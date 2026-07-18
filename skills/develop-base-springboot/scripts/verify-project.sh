#!/usr/bin/env bash

set -euo pipefail

# 统一从仓库根目录执行，避免相对路径和 Maven reactor 选择因调用目录变化而漂移。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
cd "${PROJECT_ROOT}"

readonly EXIT_USAGE=1
readonly EXIT_ENVIRONMENT=2
readonly EXIT_STATIC_CHECK=3
readonly EXIT_TEST_VERIFICATION=4
readonly MIGRATION_DIR="database-migrations/src/main/resources/db/migration/shared"

# 这些测试依赖真实容器。模块与 FQCN 是唯一清单，源码发现和报告路径都从这里派生。
readonly CONTAINER_TESTS=(
  "architecture-tests|com.yxx.architecture.SharedDatabaseFlywayIntegrationTest"
  "architecture-tests|com.yxx.architecture.AdminBootstrapIntegrationTest"
  "admin|com.yxx.admin.integration.AdminApplicationIntegrationTest"
  "business|com.yxx.business.integration.BusinessApplicationIntegrationTest"
  "common/common-cache|com.yxx.common.utils.redis.RedissonCacheIntegrationTest"
)

NORMALIZED_MODULES=""
LAST_REPORT_TESTS=0
MIGRATION_BASE_REF=""
MIGRATION_CHANGES=""

usage() {
  printf '%s\n' \
    '用法：verify-project.sh <static|compile|module|architecture|full> [模块列表]' \
    '  static        执行静态边界、补丁格式和工作区检查' \
    '  compile       编译全部模块，不执行测试' \
    '  module LIST   测试规范模块路径及其上游依赖；common 变更需把下游消费者列入 LIST' \
    '  architecture  严格执行共享数据库迁移和 bootstrap 架构测试' \
    '  full          严格执行全量 clean verify，并核对全部测试报告与容器清单'
}

fail() {
  local exit_code="$1"
  shift
  printf '错误：%s\n' "$*" >&2
  exit "${exit_code}"
}

require_command() {
  local command_name="$1"
  command -v "${command_name}" >/dev/null 2>&1 \
    || fail "${EXIT_ENVIRONMENT}" "缺少必需命令：${command_name}。"
}

validate_invocation() {
  local mode="${1:-}"
  case "${mode}" in
    static|compile|architecture|full)
      if [[ "$#" -ne 1 ]]; then
        usage >&2
        exit "${EXIT_USAGE}"
      fi
      ;;
    module)
      if [[ "$#" -ne 2 || -z "${2:-}" ]]; then
        usage >&2
        exit "${EXIT_USAGE}"
      fi
      ;;
    *)
      usage >&2
      exit "${EXIT_USAGE}"
      ;;
  esac
}

normalize_module_list() {
  local raw_modules="$1"
  local module normalized=""
  local -a requested_modules
  [[ "${raw_modules}" != ,* && "${raw_modules}" != *, \
     && "${raw_modules}" != *,,* ]] \
    || fail "${EXIT_USAGE}" "模块列表不能包含空项。"
  IFS=',' read -r -a requested_modules <<<"${raw_modules}"

  for module in "${requested_modules[@]}"; do
    # 只接受可核对的仓库相对路径，避免 Maven 别名绕过报告与容器门禁。
    module="${module#"${module%%[![:space:]]*}"}"
    module="${module%"${module##*[![:space:]]}"}"
    module="${module#./}"
    module="${module%/}"
    [[ -n "${module}" && "${module}" != "." && "${module}" != /* ]] \
      || fail "${EXIT_USAGE}" "模块列表包含无效路径：${module:-空值}。"
    [[ "${module}" =~ ^[[:alnum:]_.\/-]+$ \
       && "${module}" != */ \
       && "${module}" != *//* \
       && ! "${module}" =~ (^|/)\.{1,2}(/|$) ]] \
      || fail "${EXIT_USAGE}" "模块路径包含不允许的字符或越界段：${module}。"
    [[ -f "${module}/pom.xml" ]] \
      || fail "${EXIT_USAGE}" "模块路径不存在或缺少 pom.xml：${module}。"
    [[ ",${normalized}," != *",${module},"* ]] \
      || fail "${EXIT_USAGE}" "模块列表包含重复项：${module}。"
    normalized="${normalized:+${normalized},}${module}"
  done

  NORMALIZED_MODULES="${normalized}"
}

module_selected() {
  local module="$1"
  [[ ",${NORMALIZED_MODULES}," == *",${module},"* ]]
}

container_report_path() {
  local module="$1"
  local fqcn="$2"
  printf '%s/target/surefire-reports/TEST-%s.xml\n' "${module}" "${fqcn}"
}

container_source_path() {
  local module="$1"
  local fqcn="$2"
  printf '%s/src/test/java/%s.java\n' "${module}" "${fqcn//.//}"
}

container_class_path() {
  local module="$1"
  local fqcn="$2"
  printf '%s/target/test-classes/%s.class\n' "${module}" "${fqcn//.//}"
}

clear_surefire_reports() {
  local report_paths report_path
  report_paths="$(find . \( -type f -o -type l \) \
    -path '*/target/surefire-reports/TEST-*.xml' -print)" \
    || fail "${EXIT_ENVIRONMENT}" "扫描旧 Surefire 报告失败。"
  [[ -n "${report_paths}" ]] || return 0

  while IFS= read -r report_path; do
    [[ -n "${report_path}" ]] || continue
    rm -f -- "${report_path}" \
      || fail "${EXIT_ENVIRONMENT}" "无法清理旧测试报告：${report_path}。"
  done <<<"${report_paths}"
}

clear_container_test_classes() {
  local entry module fqcn class_path
  for entry in "${CONTAINER_TESTS[@]}"; do
    IFS='|' read -r module fqcn <<<"${entry}"
    class_path="$(container_class_path "${module}" "${fqcn}")"
    rm -f -- "${class_path}" \
      || fail "${EXIT_ENVIRONMENT}" "无法清理容器测试类：${class_path}。"
  done
}

resolve_migration_base_ref() {
  local base_ref="${VERIFY_BASE_REF:-}"
  MIGRATION_BASE_REF=""
  if [[ -n "${base_ref}" ]]; then
    git rev-parse --verify --quiet "${base_ref}^{commit}" >/dev/null \
      || fail "${EXIT_STATIC_CHECK}" "VERIFY_BASE_REF 不是有效提交：${base_ref}。"
    MIGRATION_BASE_REF="${base_ref}"
    return
  fi

  base_ref="$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null || true)"
  if [[ -n "${base_ref}" ]] \
      && git rev-parse --verify --quiet "${base_ref}^{commit}" >/dev/null; then
    MIGRATION_BASE_REF="${base_ref}"
  fi
}

resolve_java_command() {
  local executable_name="$1"
  if [[ -n "${JAVA_HOME:-}" ]]; then
    printf '%s/bin/%s\n' "${JAVA_HOME}" "${executable_name}"
    return
  fi
  command -v "${executable_name}" 2>/dev/null || true
}

require_build_environment() {
  require_command find
  require_command rm
  require_command sed
  [[ -x "./mvnw" ]] || fail "${EXIT_ENVIRONMENT}" "仓库 Maven Wrapper 不存在或不可执行。"

  local expected_java_version java_command javac_command java_version javac_version
  expected_java_version="$(sed -n \
    's:.*<java.version>\([^<]*\)</java.version>.*:\1:p' pom.xml | sed -n '1p')"
  [[ -n "${expected_java_version}" ]] \
    || fail "${EXIT_ENVIRONMENT}" "无法从根 pom.xml 读取 java.version。"

  java_command="$(resolve_java_command java)"
  javac_command="$(resolve_java_command javac)"
  [[ -n "${java_command}" && -x "${java_command}" ]] \
    || fail "${EXIT_ENVIRONMENT}" "未找到可执行的 java；请配置有效的 JAVA_HOME 或 PATH。"
  [[ -n "${javac_command}" && -x "${javac_command}" ]] \
    || fail "${EXIT_ENVIRONMENT}" "未找到可执行的 javac；本项目需要完整 JDK。"

  java_version="$("${java_command}" -version 2>&1 \
    | sed -n '1s/.*version "\([^"]*\)".*/\1/p' || true)"
  javac_version="$("${javac_command}" -version 2>&1 \
    | sed -n '1s/^javac[[:space:]]*//p' || true)"
  [[ "${java_version}" =~ ^${expected_java_version}([.]|$) ]] \
    || fail "${EXIT_ENVIRONMENT}" \
      "当前 java 版本为 ${java_version:-未知}，pom.xml 要求 ${expected_java_version}。"
  [[ "${javac_version}" =~ ^${expected_java_version}([.]|$) ]] \
    || fail "${EXIT_ENVIRONMENT}" \
      "当前 javac 版本为 ${javac_version:-未知}，pom.xml 要求 ${expected_java_version}。"
}

verify_container_test_manifest() {
  local entry module fqcn source_path expected_sources="" actual_sources rg_status
  for entry in "${CONTAINER_TESTS[@]}"; do
    IFS='|' read -r module fqcn <<<"${entry}"
    source_path="$(container_source_path "${module}" "${fqcn}")"
    if [[ -n "${expected_sources}" ]]; then
      expected_sources+=$'\n'
    fi
    expected_sources+="${source_path}"
  done
  expected_sources="$(printf '%s\n' "${expected_sources}" | sort)"

  if actual_sources="$(rg -l '^[[:space:]]*@Testcontainers([[:space:](]|$)' \
      admin business common architecture-tests \
      -g '*.java' -g '!**/target/**')"; then
    actual_sources="$(printf '%s\n' "${actual_sources}" | sort)"
  else
    rg_status=$?
    (( rg_status == 1 )) \
      || fail "${EXIT_STATIC_CHECK}" "扫描 Testcontainers 测试清单失败。"
    actual_sources=""
  fi

  if [[ "${actual_sources}" != "${expected_sources}" ]]; then
    printf '脚本声明的容器测试源码：\n%s\n' "${expected_sources}" >&2
    printf '仓库实际发现的容器测试源码：\n%s\n' "${actual_sources:-（无）}" >&2
    fail "${EXIT_STATIC_CHECK}" \
      "容器测试清单与源码不一致；请同步 CONTAINER_TESTS 后再验证。"
  fi
}

collect_migration_changes() {
  local base_ref merge_base comparison_ref="HEAD"
  local branch_changes="" index_changes worktree_changes changes="" changes_part
  resolve_migration_base_ref
  base_ref="${MIGRATION_BASE_REF}"
  if [[ -n "${base_ref}" ]]; then
    merge_base="$(git merge-base HEAD "${base_ref}")" \
      || fail "${EXIT_STATIC_CHECK}" "无法计算 HEAD 与 ${base_ref} 的共同基准。"
    comparison_ref="${merge_base}"
    branch_changes="$(git diff --name-status --diff-filter=MDRTCU \
      "${merge_base}" HEAD -- "${MIGRATION_DIR}")" \
      || fail "${EXIT_STATIC_CHECK}" "无法检查分支中的迁移变更。"
  else
    case "${CI:-}" in
      ""|0|false|FALSE|no|NO)
        printf '%s\n' \
          '提示：未找到迁移比较基准；如需覆盖分支提交历史，请设置 VERIFY_BASE_REF。' >&2
        ;;
      *)
        fail "${EXIT_STATIC_CHECK}" \
          "CI 环境缺少迁移比较基准；请设置 VERIFY_BASE_REF 或提供 origin/HEAD。"
        ;;
    esac
  fi

  # 分别把 HEAD、索引和工作区快照与共同基准比较，既能发现被后续还原遮蔽的历史修改，
  # 又会把分支新增迁移持续识别为 A，从而允许其在进入基准前增补或修正。
  index_changes="$(git diff --cached --name-status --diff-filter=MDRTCU \
    "${comparison_ref}" -- "${MIGRATION_DIR}")" \
    || fail "${EXIT_STATIC_CHECK}" "无法检查索引中的迁移变更。"
  worktree_changes="$(git diff --name-status --diff-filter=MDRTCU \
    "${comparison_ref}" -- "${MIGRATION_DIR}")" \
    || fail "${EXIT_STATIC_CHECK}" "无法检查工作区中的迁移变更。"

  for changes_part in "${branch_changes}" "${index_changes}" "${worktree_changes}"; do
    if [[ -n "${changes_part}" ]]; then
      if [[ -n "${changes}" ]]; then
        changes+=$'\n'
      fi
      changes+="${changes_part}"
    fi
  done
  if [[ -n "${changes}" ]]; then
    MIGRATION_CHANGES="$(printf '%s\n' "${changes}" | sort -u)" \
      || fail "${EXIT_STATIC_CHECK}" "整理迁移变更清单失败。"
  else
    MIGRATION_CHANGES=""
  fi
}

static_checks() {
  require_command git
  require_command rg
  require_command sort

  git diff --check \
    || fail "${EXIT_STATIC_CHECK}" "未暂存补丁存在空白或冲突标记问题。"
  git diff --cached --check \
    || fail "${EXIT_STATIC_CHECK}" "已暂存补丁存在空白或冲突标记问题。"

  local deprecated_imports migration_changes tracked_logs rg_status
  if deprecated_imports="$(rg -n 'org\.testcontainers\.containers\.MySQLContainer' . \
      -g '*.java' -g '!**/target/**')"; then
    printf '%s\n' "${deprecated_imports}" >&2
    fail "${EXIT_STATIC_CHECK}" "发现 Testcontainers 2.x 已弃用的 MySQLContainer 包。"
  else
    rg_status=$?
    (( rg_status == 1 )) \
      || fail "${EXIT_STATIC_CHECK}" "扫描弃用 Testcontainers 导入失败。"
  fi

  verify_container_test_manifest

  # 基准分支已有迁移不可变；新迁移在进入基准前仍可正常增补和修正。
  collect_migration_changes
  migration_changes="${MIGRATION_CHANGES}"
  if [[ -n "${migration_changes}" ]]; then
    printf '%s\n' "${migration_changes}" >&2
    fail "${EXIT_STATIC_CHECK}" \
      "基准分支已有 Flyway 迁移被修改、删除或重命名；请新增更高版本迁移。"
  fi

  tracked_logs="$(git ls-files 'logs/**' '*/logs/**')"
  if [[ -n "${tracked_logs}" ]]; then
    printf '%s\n' "${tracked_logs}" >&2
    fail "${EXIT_STATIC_CHECK}" "生成的日志文件不应进入 Git。"
  fi
}

require_container_runtime() {
  if command -v docker >/dev/null 2>&1; then
    docker info >/dev/null 2>&1 \
      || fail "${EXIT_TEST_VERIFICATION}" \
        "Docker 当前不可用或无访问权限，容器集成测试无法完成。"
    return
  fi
  printf '%s\n' \
    '提示：未找到 docker CLI，将由 Testcontainers 和测试报告门禁判断容器是否可用。' >&2
}

require_selected_container_runtime() {
  local entry module fqcn
  for entry in "${CONTAINER_TESTS[@]}"; do
    IFS='|' read -r module fqcn <<<"${entry}"
    if module_selected "${module}"; then
      require_container_runtime
      return
    fi
  done
  return 0
}

require_generated_report() {
  local report_path="$1"
  local suite_name="$2"
  [[ -f "${report_path}" && ! -L "${report_path}" ]] \
    || fail "${EXIT_TEST_VERIFICATION}" "缺少测试报告：${suite_name}。"
}

assert_surefire_report() {
  local report_path="$1"
  local suite_name="$2"
  require_generated_report "${report_path}" "${suite_name}"

  local header tests skipped failures errors
  local tests_pattern='tests="([0-9]+)"'
  local skipped_pattern='skipped="([0-9]+)"'
  local failures_pattern='failures="([0-9]+)"'
  local errors_pattern='errors="([0-9]+)"'
  header="$(rg -m1 '<testsuite([[:space:]>])' "${report_path}" || true)"
  [[ -n "${header}" ]] \
    || fail "${EXIT_TEST_VERIFICATION}" "无法解析测试报告：${suite_name}。"

  [[ "${header}" =~ ${tests_pattern} ]] \
    || fail "${EXIT_TEST_VERIFICATION}" "测试报告缺少 tests 属性：${suite_name}。"
  tests="${BASH_REMATCH[1]}"
  [[ "${header}" =~ ${skipped_pattern} ]] \
    || fail "${EXIT_TEST_VERIFICATION}" "测试报告缺少 skipped 属性：${suite_name}。"
  skipped="${BASH_REMATCH[1]}"
  [[ "${header}" =~ ${failures_pattern} ]] \
    || fail "${EXIT_TEST_VERIFICATION}" "测试报告缺少 failures 属性：${suite_name}。"
  failures="${BASH_REMATCH[1]}"
  [[ "${header}" =~ ${errors_pattern} ]] \
    || fail "${EXIT_TEST_VERIFICATION}" "测试报告缺少 errors 属性：${suite_name}。"
  errors="${BASH_REMATCH[1]}"

  LAST_REPORT_TESTS="${tests}"

  (( tests > 0 )) \
    || fail "${EXIT_TEST_VERIFICATION}" "${suite_name} 没有执行任何测试。"
  (( skipped == 0 )) \
    || fail "${EXIT_TEST_VERIFICATION}" "${suite_name} 有 ${skipped} 个测试被跳过。"
  (( failures == 0 && errors == 0 )) \
    || fail "${EXIT_TEST_VERIFICATION}" \
      "${suite_name} 存在 failures=${failures}, errors=${errors}。"

  printf '关键测试已执行：%s tests=%s skipped=%s failures=%s errors=%s\n' \
    "${suite_name}" "${tests}" "${skipped}" "${failures}" "${errors}"
}

assert_generated_upstream_container_reports() {
  local entry module fqcn class_path report_path suite_name
  for entry in "${CONTAINER_TESTS[@]}"; do
    IFS='|' read -r module fqcn <<<"${entry}"
    if module_selected "${module}"; then
      continue
    fi
    class_path="$(container_class_path "${module}" "${fqcn}")"
    [[ -f "${class_path}" ]] || continue
    report_path="$(container_report_path "${module}" "${fqcn}")"
    suite_name="${fqcn##*.}"
    assert_surefire_report "${report_path}" "${suite_name}"
  done
}

assert_selected_module_reports() {
  local module reports_dir report_paths report_path suite_name
  local total_tests=0 report_count=0
  local module_tests module_report_count
  local -a selected_modules
  IFS=',' read -r -a selected_modules <<<"${NORMALIZED_MODULES}"

  for module in "${selected_modules[@]}"; do
    module_tests=0
    module_report_count=0
    reports_dir="${module}/target/surefire-reports"
    report_paths=""
    if [[ -d "${reports_dir}" ]]; then
      report_paths="$(find "${reports_dir}" -type f -name 'TEST-*.xml' -print)" \
        || fail "${EXIT_TEST_VERIFICATION}" "扫描模块测试报告失败：${module}。"
    fi
    if [[ -n "${report_paths}" ]]; then
      while IFS= read -r report_path; do
        [[ -n "${report_path}" ]] || continue
        suite_name="${report_path##*/}"
        assert_surefire_report "${report_path}" "${suite_name}"
        module_tests=$((module_tests + LAST_REPORT_TESTS))
        module_report_count=$((module_report_count + 1))
      done <<<"${report_paths}"
    fi

    (( module_tests > 0 )) \
      || fail "${EXIT_TEST_VERIFICATION}" \
        "所选模块 ${module} 未执行任何测试；无测试模块请改用 compile，或选择能覆盖它的有测试消费者。"
    total_tests=$((total_tests + module_tests))
    report_count=$((report_count + module_report_count))
  done

  printf '所选模块报告汇总：reports=%s tests=%s\n' "${report_count}" "${total_tests}"
}

require_selected_container_reports() {
  local entry module fqcn report_path suite_name
  for entry in "${CONTAINER_TESTS[@]}"; do
    IFS='|' read -r module fqcn <<<"${entry}"
    module_selected "${module}" || continue
    report_path="$(container_report_path "${module}" "${fqcn}")"
    suite_name="${fqcn##*.}"
    require_generated_report "${report_path}" "${suite_name}"
  done
}

assert_module_reports() {
  assert_generated_upstream_container_reports
  assert_selected_module_reports
  require_selected_container_reports
}

assert_architecture_reports() {
  local entry module fqcn report_path suite_name
  for entry in "${CONTAINER_TESTS[@]}"; do
    IFS='|' read -r module fqcn <<<"${entry}"
    [[ "${module}" == "architecture-tests" ]] || continue
    report_path="$(container_report_path "${module}" "${fqcn}")"
    suite_name="${fqcn##*.}"
    assert_surefire_report "${report_path}" "${suite_name}"
  done
}

require_full_test_source_reports() {
  local source_paths source_path module relative_class fqcn reports_dir matching_reports
  source_paths="$(find . -type f -path '*/src/test/java/*.java' \
    \( -name 'Test*.java' -o -name '*Test.java' -o -name '*Tests.java' \
       -o -name '*TestCase.java' \) -print)" \
    || fail "${EXIT_TEST_VERIFICATION}" "扫描全量测试源码失败。"
  [[ -n "${source_paths}" ]] \
    || fail "${EXIT_TEST_VERIFICATION}" "仓库中没有发现符合 Surefire 命名规则的测试源码。"

  while IFS= read -r source_path; do
    [[ -n "${source_path}" ]] || continue
    module="${source_path%%/src/test/java/*}"
    relative_class="${source_path#*/src/test/java/}"
    fqcn="${relative_class%.java}"
    fqcn="${fqcn//\//.}"
    reports_dir="${module}/target/surefire-reports"
    matching_reports=""
    if [[ -d "${reports_dir}" ]]; then
      matching_reports="$(find "${reports_dir}" -type f \
        \( -name "TEST-${fqcn}.xml" -o -name "TEST-${fqcn}\$*.xml" \) -print)" \
        || fail "${EXIT_TEST_VERIFICATION}" "扫描测试源码对应报告失败：${source_path}。"
    fi
    [[ -n "${matching_reports}" ]] \
      || fail "${EXIT_TEST_VERIFICATION}" \
        "测试源码未生成 Surefire 报告：${source_path}。"
  done <<<"${source_paths}"
}

assert_full_reports() {
  local report_paths report_path suite_name entry module fqcn
  local total_tests=0 report_count=0

  require_full_test_source_reports
  report_paths="$(find . -type f \
    -path '*/target/surefire-reports/TEST-*.xml' -print)" \
    || fail "${EXIT_TEST_VERIFICATION}" "扫描全量测试报告失败。"
  while IFS= read -r report_path; do
    [[ -n "${report_path}" ]] || continue
    suite_name="${report_path##*/}"
    assert_surefire_report "${report_path}" "${suite_name}"
    total_tests=$((total_tests + LAST_REPORT_TESTS))
    report_count=$((report_count + 1))
  done <<<"${report_paths}"

  (( total_tests > 0 )) \
    || fail "${EXIT_TEST_VERIFICATION}" "全量构建没有执行任何测试。"

  for entry in "${CONTAINER_TESTS[@]}"; do
    IFS='|' read -r module fqcn <<<"${entry}"
    report_path="$(container_report_path "${module}" "${fqcn}")"
    require_generated_report "${report_path}" "${fqcn##*.}"
  done
  printf '全量报告汇总：reports=%s tests=%s\n' "${report_count}" "${total_tests}"
}

validate_invocation "$@"
mode="$1"
modules="${2:-}"

case "${mode}" in
  static)
    static_checks
    ;;
  compile)
    static_checks
    require_build_environment
    ./mvnw -q -DskipTests compile
    static_checks
    ;;
  module)
    normalize_module_list "${modules}"
    static_checks
    require_build_environment
    require_selected_container_runtime
    clear_surefire_reports
    clear_container_test_classes
    ./mvnw -q -pl "${NORMALIZED_MODULES}" -am test
    assert_module_reports
    static_checks
    ;;
  architecture)
    static_checks
    require_build_environment
    require_container_runtime
    clear_surefire_reports
    ./mvnw -q clean -pl architecture-tests -am \
      -Dtest=SharedDatabaseFlywayIntegrationTest,AdminBootstrapIntegrationTest \
      -Dsurefire.failIfNoSpecifiedTests=false test
    assert_architecture_reports
    static_checks
    ;;
  full)
    static_checks
    require_build_environment
    require_container_runtime
    clear_surefire_reports
    ./mvnw -q clean verify
    assert_full_reports
    static_checks
    ;;
esac
