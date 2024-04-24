package com.hspedu.seckill.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author yangda
 * @create 2024-04-20-16:25
 * @description: 自定义的校验注解
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {IsMobileValidator.class})
public @interface IsMobile {

    // message 属性，该属性提供了默认的错误消息。当验证失败时，这个消息会被使用。
    String message() default "手机号码格式错误";

    boolean required() default true;//默认必须填写手机号码
    Class<?>[] groups() default { };//默认参数
    Class<? extends Payload>[] payload() default { };//默认参数
}
