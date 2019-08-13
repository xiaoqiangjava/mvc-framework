package com.xq.learn.model;

import java.util.HashMap;
import java.util.Map;

public enum RequestMethod
{
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

    private static final Map<String, RequestMethod> mappings = new HashMap<>(16);

    static {
        for (RequestMethod requestMethod : values())
        {
            mappings.put(requestMethod.name(), requestMethod);
        }
    }

    public static RequestMethod resolve(String method) {
        return (method != null ? mappings.get(method) : null);
    }
}
