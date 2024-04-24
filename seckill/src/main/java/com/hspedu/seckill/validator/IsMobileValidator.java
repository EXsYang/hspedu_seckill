package com.hspedu.seckill.validator;

import com.hspedu.seckill.util.ValidatorUtil;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author yangda
 * @create 2024-04-20-16:27
 * @description: 注意要想让校验规则，或是自定义注解和自定义校验器生效，
 * 需要在Controller层，封装前端参数时，加上 @Valid注解！！！校验框架才会生效！！
 *
 * IsMobileValidator自定义的校验器 在分布式项目 hspliving中讲过
 *
 * ConstraintValidator<> 自定义校验器需要实现的接口
 * Constraint: 限制;约束;限定;严管
 *
 * 1. IsMobileValidator 是真正的校验器，即校验的逻辑是写在这里的
 * 2. IsMobileValidator 需要实现接口 ConstraintValidator<A extends Annotation, T>
 * 3. <IsMobile,String> 表示该校验器是针对 @IsMobile 注解 传入的 String 类型的数据进行校验
 */
//我们自拟定注解 IsMobile 的校验规则, 可以根据业务需求增加相应的校验规则
public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {
    private boolean required = false;

    /*
        constraintAnnotation 就是标注在对象属性上的@IsMobile注解
        通过该注解可以获取到IsMobile注解的各个属性传入的值
    */
    @Override
    public void initialize(IsMobile constraintAnnotation) {
        //初始化
        required = constraintAnnotation.required();
    }

    /**
     * // 这里是判断最终校验结果的方法
     * @param value   就是将来在前端的表单中传入的数据，即要校验的字段 前端表单字段值 封装到 有 IsMobile 注解标注 的对象属性的值
     * @param context  ConstraintValidatorContext 是一个强大的工具，它不仅允许验证器自定义错误报告，还能通过各种方法精确控制验证过程中的错误反馈。
     * @return 如果返回true校验成功-通过，如果返回false就是校验失败！-没有通过
     * 如果 isValid 方法返回 false（即验证失败），
     * 则验证框架（如 Spring 的 Validation）会自动使用 @IsMobile 注解中
     * 定义的 message 属性值作为错误消息返回给前端。
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        //必填
        if (required) {
            //isValid(String value,)的形参value 就是前端填写的手机号
            return ValidatorUtil.isMobile(value);
        } else {//非必填
            if (!StringUtils.hasText(value)) {
                return true;
            } else {
                return ValidatorUtil.isMobile(value);
            }
        }
    }
}
