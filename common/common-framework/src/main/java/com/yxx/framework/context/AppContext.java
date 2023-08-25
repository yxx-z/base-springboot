package com.yxx.framework.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.io.Serial;
import java.io.Serializable;

public class AppContext implements Serializable {
    public static final String KEY_TRACE_ID = "Trace-Id";
    @Serial
    private static final long serialVersionUID = -979220111440953115L;

    private String traceId;

    private static final TransmittableThreadLocal<AppContext> LOCAL = new TransmittableThreadLocal<>() {
        @Override
        protected AppContext initialValue() {
            return new AppContext();
        }
    };

    public static AppContext getContext() {
        return LOCAL.get();
    }

    public static void setContext(AppContext context) {
        LOCAL.set(context);
    }

    public static void removeContext() {
        LOCAL.remove();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
