package com.hspedu.seckill.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yangda
 * @create 2024-05-06-11:45
 * @description:
 * 自定义注解，在实现springmvc底层机制时学过
 * 自定义注解@AccessLimit
 */
@Retention(RetentionPolicy.RUNTIME) //RetentionPolicy.RUNTIME:编译器将把注解记录在 class 文件中. 当运行 Java 程序时, JVM 会保留注解. 程序可以通过反射获取该注解
@Target(ElementType.METHOD) //指定自定义注解可以用来修饰那些程序元素
public @interface AccessLimit {

    int second();//时间范围
    int maxCount();//访问的最大次数
    // int maxCount = 5;//这种定义方式是不合法的

    /**
     * 在Java注解中，所谓的“属性”实际上通过无参数的方法来定义。
     * 这些方法可以定义默认值，但它们的行为与Java类中的字段或属性不同。注解中的这些方法：
     *
     * 必须没有参数。
     * 不能有throws子句。
     * 返回值必须是基本数据类型、字符串、类、注解或这些类型的数组。
     * 在使用注解时，这些方法的返回值表现为注解的属性。
     */


    boolean needLogin() default true;//是否需要登录

}
