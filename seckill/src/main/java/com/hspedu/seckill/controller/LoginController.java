package com.hspedu.seckill.controller;

import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.vo.LoginVo;
import com.hspedu.seckill.vo.RespBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * @author yangda
 * @create 2024-04-19-20:58
 * @description:
 */
@Controller //注意这里要使用这个注解，不要使用@RestController,因为这里需要返回HTML页面,而不是json数据
@RequestMapping("/login")
@Slf4j
public class LoginController {

    //装配
    @Resource
    private UserService userService;


    /**
     * 编写方法，可以进入到登录界面
     * http://localhost:8080/login/toLogin
     */
    @RequestMapping("/toLogin")
    public String toLogin() {

        return "login"; //默认请求转发到templates/login.html
    }


    /**
     * 在 Spring Boot 中，决定响应类型（即是返回 HTML 页面还是 JSON 数据）
     * 的关键是 @ResponseBody 注解的使用，以及控制器是否被标记
     * 为 @RestController（等同于在每个方法上使用 @Controller 和 @ResponseBody）。
     *
     * 经由 http://localhost:8080/login/toLogin 所在的html页面中的 ajax请求 转发到该控制器方法
     *
     * /login/doLogin
     * @param loginVo
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/doLogin")
    @ResponseBody //加上该注解，就不会被解析为视图了。即不会按照templates的视图，而是返回json数据
    // public RespBean doLogin(LoginVo loginVo,
    // 启用校验规则
    // public RespBean doLogin(@Valid LoginVo loginVo,
    public RespBean doLogin(@Validated LoginVo loginVo,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        // log.info("{}", loginVo);

        return userService.doLogin(loginVo,request,response); //默认请求转发到templates/login.html
    }


}
