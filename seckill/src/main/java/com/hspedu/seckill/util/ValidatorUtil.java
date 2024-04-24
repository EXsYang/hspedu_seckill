package com.hspedu.seckill.util;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yangda
 * @create 2024-04-19-16:14
 * @description: 完成一些校验工作，比如手机号格式是否正确
 * Java基础时讲过正则表达式
 */
public class ValidatorUtil {

    //校验手机号码的正则表达式
    //13000000000 合格
    //11000000000 不合格
    private static final Pattern mobile_pattern = Pattern.compile("^[1][3-9][0-9]{9}$");

    //编写方法 校验手机号是否合法 如果满足指定的正则规则返回true，否则返回false
    public static boolean isMobile(String mobile){
        if (!StringUtils.hasText(mobile)){
            return false;
        }

        //进行正则表达式校验-Java基础时讲过
        Matcher matcher = mobile_pattern.matcher(mobile);
        // 检查整个字符串 mobile 是否匹配 返回布尔值
        return matcher.matches();
    }


    //测试一下校验方法
    @Test
    public void t1(){

        String mobile = "13300000009";

        System.out.println(isMobile(mobile)); //T


    }
}
