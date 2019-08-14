package com.xq.learn.demo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.xq.learn.annotation.Service;
import com.xq.learn.demo.service.IDemoService;

import java.util.Map;

/**
 * @author xiaoqiang
 * @date 2019/8/14 1:34
 */
@Service
public class DemoServiceImpl implements IDemoService
{
    @Override
    public String get(String id)
    {
        return id;
    }

    @Override
    public String add(String body)
    {
        Map<String, String> map = JSONObject.parseObject(body, Map.class);
        return map.get("id");
    }

    @Override
    public String delete(String id)
    {
        return id;
    }
}
