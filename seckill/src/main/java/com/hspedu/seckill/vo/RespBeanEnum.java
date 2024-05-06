package com.hspedu.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author yangda
 * @create 2024-04-19-15:16
 * @description: 枚举类，响应枚举类, 定义一些统一的 状态码和错误信息
 */
@Getter
@ToString
@AllArgsConstructor
// @NoArgsConstructor //这里不可以使用无参构造器，因为属性是final的，无参构造器构建对象会导致属性为默认值null,没有初始化
public enum RespBeanEnum {

    //通用
    SUCCESS(200,"SUCCESS"),
    ERROR(500,"服务端异常"),

    //登录
    LOGIN_ERROR(500210,"用户id或者密码错误"),
    MOBILE_ERROR(500211,"手机号码格式不正确"),
    BING_ERROR(500212,"参数绑定异常"),
    MOBILE_NOT_ERROR(500213,"手机号码不存在"),
    PASSWORD_UPDATE_FAIL(500214,"密码更新失败"),
    //其他我们在开发过程中，灵活增加即可

    //秒杀模块-返回的信息
    ENTRY_STOCK(500500,"库存不足"),
    REPEAT_ERROR(500501,"该商品每人限购一件"),//重复错误
    REQUEST_ILLEGAL(500502,"请求非法"),
    SESSION_ERROR(500503,"用户信息有误"),
    SEC_KILL_WAIT(500504,"排队中..."),
    CAPTCHA_ERROR(500505,"验证码错误..."),
    ACCESS_LIMIT_REACHED(500506,"访问频繁,请待会再试2..."),
    SEC_KILL_RETRY(500507,"本次抢购失败,请继续抢购...");




    private final Integer code;
    private final String message;


}
