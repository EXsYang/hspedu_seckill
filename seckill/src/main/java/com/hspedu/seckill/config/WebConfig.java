package com.hspedu.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author yangda
 * @create 2024-04-23-19:42
 * @description:
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    //装配
    @Resource
    private UserArgumentResolver userArgumentResolver;

    //装配自定义拦截器
    @Resource
    private AccessLimitInterceptor accessLimitInterceptor;


    //注册自定义拦截器，这样才能生效
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessLimitInterceptor);
    }

    //静态资源加载
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    // 将我们自定义的 UserArgumentResolver 解析器加入到HandlerMethodArgumentResolver 列表
    // 这样我们自定义的 UserArgumentResolver 解析器 才生效
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
    }
}
