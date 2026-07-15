package com.yxx.common.utils.jackson;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.yxx.common.annotation.jackson.QueryDateBoundary;

import java.io.IOException;
import java.util.Date;

/**
 * 查询日期边界反序列化器。
 *
 * <p>根据字段上的 {@link QueryDateBoundary} 将输入日期转换为当天开始或结束时间。</p>
 */
public class QueryDateBoundaryDeserializer extends JsonDeserializer<Date>
        implements ContextualDeserializer {

    private final QueryDateBoundary dateBoundary;

    /** Jackson 反射创建实例时使用。 */
    public QueryDateBoundaryDeserializer() {
        this(null);
    }

    private QueryDateBoundaryDeserializer(QueryDateBoundary dateBoundary) {
        this.dateBoundary = dateBoundary;
    }

    @Override
    public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        Date date = parser.readValueAs(Date.class);
        if (date == null || dateBoundary == null) {
            return date;
        }
        return dateBoundary.value() == QueryDateBoundary.Boundary.START_OF_DAY
                ? DateUtil.beginOfDay(date)
                : DateUtil.endOfDay(date);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return context.findNonContextualValueDeserializer(context.getContextualType());
        }
        QueryDateBoundary annotation = property.getAnnotation(QueryDateBoundary.class);
        if (annotation != null && Date.class.equals(property.getType().getRawClass())) {
            return new QueryDateBoundaryDeserializer(annotation);
        }
        return context.findContextualValueDeserializer(property.getType(), property);
    }
}
