package com.xq.learn.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.xq.learn.annotation.Autowired;
import com.xq.learn.annotation.Controller;
import com.xq.learn.annotation.RequestBody;
import com.xq.learn.annotation.RequestMapping;
import com.xq.learn.annotation.RequestParam;
import com.xq.learn.annotation.ResponseBody;
import com.xq.learn.demo.service.IDemoService;
import com.xq.learn.model.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xiaoqiang
 * @date 2019/8/14 1:34
 */
@Controller
@RequestMapping("/v1/mvc")
public class DemoController
{
    @Autowired
    private IDemoService demoService;

    @RequestMapping(value = "/resource", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> get(HttpServletRequest request, @RequestParam("name") String id)
    {
        Map<String, String> response = new HashMap<>();
        String result = demoService.get(id);
        response.put("id", result);
        return response;
    }

    @RequestMapping(value = "/resource", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> add(HttpServletRequest request, @RequestBody Object body)
    {
        Map<String, String> response = new HashMap<>();
        String result = demoService.add(JSONObject.toJSONString(body));
        response.put("body", result);
        return response;
    }

    @RequestMapping(value = "/resource", method = RequestMethod.DELETE)
    @ResponseBody
    public Map<String, String> delete(HttpServletRequest request, @RequestParam("id") String id)
    {
        Map<String, String> response = new HashMap<>();
        String result = demoService.delete(id);
        response.put("id", result);
        return response;
    }
}
