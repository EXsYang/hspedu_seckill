package com.hspedu.seckill.config;

import com.hspedu.seckill.pojo.User;

/**
 * @author yangda
 * @create 2024-05-06-12:02
 * @description:
 * UserContext 工具类，向ThreadLocal 存入或者取出User对象
 */
public class UserContext {

    //每个线程都有自己的ThreadLocal,把共享的数据放到这里,保证线程安全
    private static ThreadLocal<User> userHolder =
            new ThreadLocal<>();

    public static void setUser(User user){
        userHolder.set(user);
    }

    public static User getUser(){
        return userHolder.get();
    }

}
