package com.sengang.config;

import com.sengang.interceptor.LoginInterceptor;
import com.sengang.interceptor.RefreshInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //所有的访问都会刷新token
        registry.addInterceptor(new RefreshInterceptor(stringRedisTemplate)).addPathPatterns("/**");
        //登录拦截器
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(//排除拦截路径
                "/user/login",//登录
                "/user/code",//验证码
                "/blog/hot",//热点
                "/shop-type/**",
                "/shop/**",
                "/upload/**",
                "/voucher/**"
        );
    }
}
