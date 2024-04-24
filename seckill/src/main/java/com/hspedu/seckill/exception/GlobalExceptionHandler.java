package com.hspedu.seckill.exception;

import com.hspedu.seckill.vo.RespBean;
import com.hspedu.seckill.vo.RespBeanEnum;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.swing.*;
import javax.validation.Valid;

/**
 * @author yangda
 * @create 2024-04-20-18:23
 * @description:
 * GlobalExceptionHandler 这里是springboot的全局异常处理器，在springboot中讲过
 *
 * 注解 @RestControllerAdvice 是一个用于全局异常处理的注解，
 * 它是 @ControllerAdvice 和 @ResponseBody 注解的组合体。
 *
 * 注解@ControllerAdvice: 提供全局异常处理、全局数据绑定、全局数据预处理的功能。
 * 它可以指定作用的目标（特定控制器或更广泛的包名）。
 * 注解@ResponseBody: 表示方法的返回值应直接绑定到Web响应体，
 * 并通过消息转换器转换为例如JSON或XML格式。
 *
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    //处理所有的异常
    @ExceptionHandler(Exception.class)
    public RespBean ExceptionHandler(Exception e) {
        //如果是全局异常，正常处理
        if (e instanceof GlobalException) {//全局异常的处理
            GlobalException ex = (GlobalException) e;
            return RespBean.error(ex.getRespBeanEnum());

        } else if (e instanceof BindException) { //绑定异常的处理
            // BindException: 处理表单绑定时发生的异常（例如，Spring MVC 中使用 @Valid 注解验证时）。这里将错误信息提取出来，并且构造一个具体的错误响应返回给前端。
            //如果是绑定异常 ：由于我们自定义的注解只会在控制台打印错误信息，想让该信息传给前端。
            //需要获取该异常 BindException，进行打印
            BindException ex = (BindException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BING_ERROR);
            respBean.setMessage(" 参数校验异常~：" + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
            return respBean;
        }
        //既不是全局异常也不是绑定异常，是其他的异常，就统一的给前端返回一个错误信息， RespBean.error(RespBeanEnum.ERROR);
        //这里之所以可以直接给前端返回一个 RespBean类型的对象 数据，
        // 是因为 @ResponseBody注解 可以将信息 通过消息转换器转换为例如JSON或XML格式 直接返回给前端
        return RespBean.error(RespBeanEnum.ERROR);
    }
}
