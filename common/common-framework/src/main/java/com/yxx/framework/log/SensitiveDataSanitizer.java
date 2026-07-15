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
            return "<Servlet对象已忽略>";
        }
        if (value instanceof MultipartFile file) {
            return "<上传文件 name=" + file.getName() + ", size=" + file.getSize() + ">";
        }

        try {
            JsonNode node = objectMapper.valueToTree(value);
            sanitizeNode(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (IllegalArgumentException | JsonProcessingException exception) {
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
            JsonNode node = objectMapper.readTree(text);
            sanitizeNode(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (JsonProcessingException exception) {
            return truncate(TEXT_SENSITIVE_PATTERN.matcher(text).replaceAll("$1$2" + MASK));
        }
    }

    private void sanitizeNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
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
        String normalized = fieldName.toLowerCase(Locale.ROOT).replace("-", "");
        return SENSITIVE_WORDS.stream().anyMatch(normalized::contains);
    }

    private String truncate(String text) {
        if (text.length() <= MAX_LOG_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LOG_LENGTH) + "...<内容已截断>";
    }
}
