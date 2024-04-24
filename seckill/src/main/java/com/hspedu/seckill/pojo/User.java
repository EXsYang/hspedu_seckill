package com.hspedu.seckill.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yangda
 * @create 2024-04-18-21:58
 * @description:
 */
@Data
@TableName("seckill_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 用户 ID,手机号码
     * 只有当插入对象ID 为空，才自动填充
     * 分配ID (主键类型为number或string）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 昵称
     */
    private String nickname;
    /**
     * MD5(MD5(pass 明文+固定 salt1)+salt2)
     */
    private String password;

    /**
     * 盐, 每个用户可以相同，也可以不同
     */
    private String salt;
    /**
     * 头像
     */
    private String head;
    /**
     * 注册时间
     */
    private Date registerDate;
    /**
     * 韩顺平 Java 工程师
     * 最后一次登录时间
     */
    private Date lastLoginDate;
    /**
     * 登录次数
     */
    private Integer loginCount;

}
