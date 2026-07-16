package com.yxx.framework.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 日志敏感数据脱敏器。
 *
 * <p>所有请求参数和审计参数进入日志前必须经过本组件处理。脱敏规则以字段语义为准，
 * 覆盖密码、Token、密钥、签名及授权头等常见敏感信息。</p>
 */
@Component
public class SensitiveDataSanitizer {

    private static final String MASK = "******";
    private static final int MAX_LOG_LENGTH = 4096;
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "password", "passwd", "pwd", "token", "authorization", "secret",
            "privatekey", "private_key", "accesskey", "access_key", "sign", "credential");
    private static final Pattern TEXT_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd|token|authorization|secret|private[_-]?key|access[_-]?key|sign)"
                    + "(\\s*[=:]\\s*|\\\"\\s*:\\s*\\\")([^,;&\\s\\\"]+)");

    private final ObjectMapper objectMapper;

    public SensitiveDataSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 脱敏任意方法参数。
     *
     * @param value 原始参数
     * @return 可安全写入日志的文本
     */
    public String sanitize(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse) {
            // Servlet 对象结构庞大且可能间接暴露 Header、Session，禁止序列化进日志。
            return "<Servlet对象已忽略>";
        }
        if (value instanceof MultipartFile file) {
            // 文件只记录字段名与大小，不读取文件名或内容，降低隐私和内存风险。
            return "<上传文件 name=" + file.getName() + ", size=" + file.getSize() + ">";
        }

        try {
            // 优先转为 JSON 树按字段名递归脱敏，能覆盖嵌套 DTO、集合和 Map。
            JsonNode node = objectMapper.valueToTree(value);
            sanitizeNode(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            // 不可序列化对象退化为文本规则处理，日志能力不能反向中断业务流程。
            return truncate(sanitizeText(String.valueOf(value)));
        }
    }

    /**
     * 批量脱敏方法参数。
     *
     * @param arguments 方法参数数组
     * @return 脱敏后的参数文本
     */
    public String sanitizeArguments(Object[] arguments) {
        // 每个参数独立处理，单个复杂对象不会影响其他参数的脱敏结果。
        return Arrays.stream(arguments)
                .map(this::sanitize)
                .toList()
                .toString();
    }

    /**
     * 脱敏已经被读取为文本的 JSON 或查询参数。
     *
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public String sanitizeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        try {
            // 合法 JSON 仍使用结构化字段脱敏，避免正则遗漏嵌套字段。
            JsonNode node = objectMapper.readTree(text);
            sanitizeNode(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (JsonProcessingException exception) {
            // 查询串、异常消息等非 JSON 文本使用保守正则替换常见敏感键值。
            return truncate(TEXT_SENSITIVE_PATTERN.matcher(text).replaceAll("$1$2" + MASK));
        }
    }

    private void sanitizeNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            // 命中敏感字段后整体替换值；非敏感字段继续递归检查其子节点。
            objectNode.properties().forEach(entry -> {
                if (isSensitive(entry.getKey())) {
                    objectNode.put(entry.getKey(), MASK);
                } else {
                    sanitizeNode(entry.getValue());
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::sanitizeNode);
        }
    }

    private boolean isSensitive(String fieldName) {
        // 字段名忽略大小写和连字符差异，并允许 passwordHash 等组合命名被识别。
        String normalized = fieldName.toLowerCase(Locale.ROOT).replace("-", "");
        return SENSITIVE_WORDS.stream().anyMatch(normalized::contains);
    }

    private String truncate(String text) {
        if (text.length() <= MAX_LOG_LENGTH) {
            return text;
        }
        // 限制单条参数日志大小，避免异常大请求放大磁盘与日志平台压力。
        return text.substring(0, MAX_LOG_LENGTH) + "...<内容已截断>";
    }
}
