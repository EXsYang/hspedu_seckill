package com.hspedu.seckill.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author yangda
 * @create 2024-04-18-21:24
 * @description: 工具类，根据前面密码设计方案提供相应的方法
 */
public class MD5Util {

    //准备一个salt 加盐。 转换大小写快捷键 ctrl + shift + u
    private static final String SALT = "Dz7Oyf9b";

    /**
     * 最基本的md5方法
     * @param src
     * @return
     */
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    //加密加盐, 完成的任务就是 md5(password明文+salt1)
    public static String inputPassToMidPass(String inputPass) {
        System.out.println("SALT.charAt(0)-->" + SALT.charAt(0));//c
        System.out.println("SALT.charAt(6)-->" + SALT.charAt(6));//T
        String str = SALT.charAt(0) + inputPass + SALT.charAt(6);
        return md5(str);
    }

    //加密加盐, 完成的任务就是 把 (MidPass + salt2)转成DB中的密码
    // 相当于是 md5(md5(password明文+salt1)+salt2)
    public static String midPassToDBPass(String midPass, String salt) {
        System.out.println("salt.charAt(1)-->" + SALT.charAt(1));//L
        System.out.println("salt.charAt(5)-->" + SALT.charAt(5));//m
        String str = salt.charAt(1) + midPass + salt.charAt(5);
        return md5(str);
    }

    /**
     * 编写方法，可以将password明文，直接转成DB中的密码
     * @param inputPass: password明文
     * @param salt: 第二次要加的盐，每个用户的第二把盐都不一样，需要到DB中获取相应的字段
     * @return
     */
    public static String inputPassToDBPass(String inputPass,String salt){
        String midPass = inputPassToMidPass(inputPass);
        String dbPass = midPassToDBPass(midPass, salt);
        return dbPass;
    }

}
