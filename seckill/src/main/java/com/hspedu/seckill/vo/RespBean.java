package com.hspedu.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yangda
 * @create 2024-04-19-15:32
 * @description: 用于给前端返回 状态码和错误信息 和一些数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespBean {

    //这里使用基本数据类型，传进来的如果是Integer类型，则会先自动拆箱为int,然后再自动类型提升为long类型
    private long code;

    private String message;

    // 要给前端携带的数据
    private Object obj;

    //成功后-同时携带数据
    public static RespBean success(Object data){
        //这里使用基本数据类型，传进来的如果是RespBeanEnum.SUCCESS.getCode()【Integer类型】，则会先自动拆箱为int,然后再自动类型提升为long类型
        return new RespBean(RespBeanEnum.SUCCESS.getCode(),RespBeanEnum.SUCCESS.getMessage(),data);
    }


    //成功后-不携带数据
    public static RespBean success(){
        //这里使用基本数据类型，传进来的如果是RespBeanEnum.SUCCESS.getCode()【Integer类型】，则会先自动拆箱为int,然后再自动类型提升为long类型
        return new RespBean(RespBeanEnum.SUCCESS.getCode(),RespBeanEnum.SUCCESS.getMessage(),null);
    }


    //失败后 各有不同-返回失败信息时，同时携带数据
    public static RespBean error(RespBeanEnum respBeanEnum,Object data){
        //这里使用基本数据类型，传进来的如果是respBeanEnum.getCode()【Integer类型】，则会先自动拆箱为int,然后再自动类型提升为long类型
        return new RespBean(respBeanEnum.getCode(),respBeanEnum.getMessage(),data);
    }

    //失败后 各有不同-返回失败信息时，不携带数据
    public static RespBean error(RespBeanEnum respBeanEnum){
        //这里使用基本数据类型，传进来的如果是respBeanEnum.getCode()【Integer类型】，则会先自动拆箱为int,然后再自动类型提升为long类型
        return new RespBean(respBeanEnum.getCode(),respBeanEnum.getMessage(),null);
    }






}
