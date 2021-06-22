package com.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import java.util.HashMap;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    //Spring Ioc环境配置
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] {RootConfig.class};
    }
    //DispatcherServlet环境配置
    @Override
    protected Class<?>[] getServletConfigClasses() {
        //加载java配置类
        return new Class<?>[] {WebConfig.class};
    }
    //DispatcherServlet拦截请求配置
    @Override
    protected String[] getServletMappings() {
        return new String[] {"*.do"};
    }
}
