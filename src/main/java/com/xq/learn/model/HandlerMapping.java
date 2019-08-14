package com.xq.learn.model;

import java.lang.reflect.Method;

/**
 * @author xiaoqiang
 * @date 2019/8/14 3:43
 */
public class HandlerMapping
{
    private Method method;

    private RequestMethod[] requestMethod;

    public Method getMethod()
    {
        return method;
    }

    public void setMethod(Method method)
    {
        this.method = method;
    }

    public RequestMethod[] getRequestMethod()
    {
        return requestMethod;
    }

    public void setRequestMethod(RequestMethod[] requestMethod)
    {
        this.requestMethod = requestMethod;
    }
}
