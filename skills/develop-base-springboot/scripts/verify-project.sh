#!/usr/bin/env bash

set -euo pipefail

# 统一从仓库根目录执行，避免相对路径和 Maven reactor 选择因调用目录变化而漂移。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
cd "${PROJECT_ROOT}"

usage() {
  printf '%s\n' \
    '用法：verify-project.sh <static|compile|module|architecture|full> [模块列表]' \
    '  static        只执行静态残留和补丁格式检查' \
    '  compile       编译全部模块，不执行测试' \
    '  module LIST   测试指定 Maven 模块及其依赖，例如 common/common-security,admin' \
    '  architecture  执行共享数据库迁移和 bootstrap 架构测试' \
    '  full          执行全量 clean verify'
}

require_java_17() {
  local version
  version="$(java -version 2>&1 | sed -n '1s/.*version "\([^"]*\)".*/\1/p')"
  if [[ ! "${version}" =~ ^17([.]|$) ]]; then
    printf '错误：当前 Java 版本为 %s，本项目要求 JDK 17。\n' "${version:-未知}" >&2
    exit 2
  fi
}

static_checks() {
  git diff --check

  if command -v rg >/dev/null 2>&1; then
    if rg -n 'org\.testcontainers\.containers\.MySQLContainer' . \
      -g '*.java' -g '!target/**'; then
      printf '%s\n' '错误：发现 Testcontainers 2.x 已弃用的 MySQLContainer 包。' >&2
      exit 3
    fi

    if rg -n 'V2__add_audit_actor_snapshot|V3__strengthen_identity_and_rbac_constraints' \
      README.md admin business architecture-tests database-migrations common \
      -g '!target/**'; then
      printf '%s\n' '错误：发现已经合并进初始基线的旧迁移引用。' >&2
      exit 3
    fi
  fi

  if git ls-files 'logs/**' '*/logs/**' | grep -q .; then
    printf '%s\n' '错误：生成的日志文件不应进入 Git。' >&2
    exit 3
  fi
}

mode="${1:-}"
case "${mode}" in
  static)
    static_checks
    ;;
  compile)
    require_java_17
    ./mvnw -q -DskipTests compile
    static_checks
    ;;
  module)
    require_java_17
    modules="${2:-}"
    if [[ -z "${modules}" ]]; then
      usage >&2
      exit 1
    fi
    ./mvnw -q -pl "${modules}" -am test
    static_checks
    ;;
  architecture)
    require_java_17
    ./mvnw -q clean -pl architecture-tests -am \
      -Dtest=SharedDatabaseFlywayIntegrationTest,AdminBootstrapIntegrationTest \
      -Dsurefire.failIfNoSpecifiedTests=false test
    static_checks
    ;;
  full)
    require_java_17
    ./mvnw -q clean verify
    static_checks
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
