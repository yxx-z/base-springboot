package com.yxx.common.utils.jackson;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.yxx.common.annotation.jackson.SearchDate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 *
 * @author yxx
 * @since 2022/11/2 10:38
 */
public class SearchDateDeserializer extends JsonDeserializer<LocalDateTime> implements ContextualDeserializer {

    private SearchDate searchDate;

    @SuppressWarnings("unused")
    public SearchDateDeserializer() {
        super();
    }

    public SearchDateDeserializer(final SearchDate searchDate) {
        super();
        this.searchDate = searchDate;
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        LocalDateTime date = p.readValueAs(LocalDateTime.class);
        if (ObjectUtil.isNull(date)) {
            return null;
        }
        if (searchDate.startDate()) {
            return date.with(LocalTime.MIN);
        }
        if (searchDate.endDate()) {
            return date.with(LocalTime.MAX);
        }
        return date;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            if (ObjectUtil.equal(property.getType().getRawClass(), LocalDateTime.class)) {
                searchDate = property.getAnnotation(SearchDate.class);
                if (searchDate != null) {
                    return new SearchDateDeserializer(searchDate);
                }
            }
            return ctxt.findContextualValueDeserializer(property.getType(), property);
        }
        return ctxt.findNonContextualValueDeserializer(ctxt.getContextualType());
    }
}
