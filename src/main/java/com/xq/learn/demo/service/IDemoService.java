package com.xq.learn.demo.service;

/**
 * @author xiaoqiang
 * @date 2019/8/14 1:34
 */
public interface IDemoService
{
    String get(String id);

    String add(String body);

    String delete(String id);
}
