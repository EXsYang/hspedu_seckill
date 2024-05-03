package com.hspedu.seckill.controller;

import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.vo.RespBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author yangda
 * @create 2024-04-26-13:52
 * @description:
 */
@Controller
@RequestMapping("/user")
public class UserController {


    //装配
    @Resource
    private UserService userService;



    /**
     * 在使用 Thymeleaf 的 Spring Boot 应用中，
     * 控制器返回对象通常需要明确地通过 @ResponseBody 来处理 JSON 返回，
     * 或者通过添加对象到模型并返回一个视图名称来让 Thymeleaf 渲染 HTML 页面。
     * 如果返回的是对象本身而没有适当的注解或视图名称，通常会导致错误。
     */
    //返回登录用户的信息，同时接收请求携带的参数address
    // @RequestMapping("/info")
    // @ResponseBody //该注解不能少，否则会报错
    // public RespBean info(User user,String address){
    //
    //     System.out.println("address-->>" + address);
    //     return RespBean.success(user);
    // }



    @RequestMapping("/info")
    @ResponseBody //该注解不能少，否则会报错
    public RespBean info(User user){

        return RespBean.success(user);
    }


    //方法:处理更新密码
    // http://localhost:8080/user/updpwd?userTicket=0031103ce2c54b7db4903ad48e3b8c56&password=123
    @RequestMapping("/updpwd")
    @ResponseBody
    public RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response){

        RespBean respBean = userService.updatePassword(userTicket, password, request, response);

        return respBean;
    }


}
