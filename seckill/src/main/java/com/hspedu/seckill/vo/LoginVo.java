package com.hspedu.seckill.vo;

import com.hspedu.seckill.validator.IsMobile;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * @author yangda
 * @create 2024-04-19-16:12
 * @description: 接收用户登录时，发送的信息 (mobile,password)
 */
@Data
public class LoginVo {

    //对LoginVo的属性值进行约束
    @NotNull
    @IsMobile //自定义校验注解
    private String mobile;

    @NotNull
    @Length(min = 32)
    private String password;

}
