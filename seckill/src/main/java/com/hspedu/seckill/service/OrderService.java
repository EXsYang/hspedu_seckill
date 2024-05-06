package com.hspedu.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hspedu.seckill.pojo.Order;
import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.vo.GoodsVo;

/**
 * @author yangda
 * @create 2024-04-25-15:43
 * @description:
 */
public interface OrderService extends IService<Order> {

    //真正的秒杀方法
    Order seckill(User user, GoodsVo goodsVo);

    //获取秒杀结果
    Long getSeckillResult(User user,Long goodsId);

    //方法: 生成秒杀路径/值【唯一的】
    String createPath(User user,Long goodsId);

    //方法: 对秒杀路径进行校验
    boolean checkPath(User user,Long goodsId,String path);

    //方法: 验证用户输入的验证码是否正确
    boolean checkCaptcha(User user,Long goodsId,String captcha);


}
