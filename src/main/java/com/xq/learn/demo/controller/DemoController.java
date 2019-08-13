package com.xq.learn.demo.controller;

import com.xq.learn.annotation.Autowired;
import com.xq.learn.annotation.Controller;
import com.xq.learn.annotation.RequestBody;
import com.xq.learn.annotation.RequestMapping;
import com.xq.learn.annotation.RequestParam;
import com.xq.learn.demo.service.IDemoService;
import com.xq.learn.model.RequestMethod;

import javax.servlet.http.HttpServletRequest;

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
    public String get(HttpServletRequest request, @RequestParam("id") String id)
    {
        return null;
    }

    @RequestMapping(value = "/resource", method = RequestMethod.POST)
    public String add(HttpServletRequest request, @RequestBody Object body)
    {
        return null;
    }

    @RequestMapping(value = "/resource", method = RequestMethod.DELETE)
    public String delete(HttpServletRequest request, @RequestParam("id") String id)
    {
        return null;
    }
}
