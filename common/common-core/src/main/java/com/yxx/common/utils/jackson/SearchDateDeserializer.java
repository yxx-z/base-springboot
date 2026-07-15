package com.yxx.common.utils.jackson;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.yxx.common.annotation.jackson.SearchDate;

import java.io.IOException;
import java.util.Date;

/**
 * @author yxx
 * @since 2022/11/2 10:38
 */
public class SearchDateDeserializer extends JsonDeserializer<Date> implements ContextualDeserializer {

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
    public Date deserialize(JsonParser p, DeserializationContext context) throws IOException {
        Date date = p.readValueAs(Date.class);
        if (ObjectUtil.isNull(date)) {
            return null;
        }
        if (searchDate.startDate()) {
            return DateUtil.beginOfDay(date);
        }
        if (searchDate.endDate()) {
            return DateUtil.endOfDay(date);
        }
        return date;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            if (ObjectUtil.equal(property.getType().getRawClass(), Date.class)) {
                searchDate = property.getAnnotation(SearchDate.class);
                if (searchDate != null) {
                    return new SearchDateDeserializer(searchDate);
                }
            }
            return context.findContextualValueDeserializer(property.getType(), property);
        }
        return context.findNonContextualValueDeserializer(context.getContextualType());
    }
}
