package com.hspedu.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hspedu.seckill.pojo.User;

/**
 * @author yangda
 * @create 2024-04-18-22:52
 * @description:
 * 继承 BaseMapper<T> 的接口可以直接使用 MyBatis Plus 提供的各种 CRUD 方法，
 * 从而简化了数据访问层的代码。
 */
public interface UserMapper extends BaseMapper<User> {
}
