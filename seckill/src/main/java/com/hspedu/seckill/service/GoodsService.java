package com.hspedu.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hspedu.seckill.pojo.Goods;
import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.vo.GoodsVo;

import java.util.List;

/**
 * @author yangda
 * @create 2024-04-24-19:10
 * @description:
 */
public interface GoodsService extends IService<Goods> {

    //返回秒杀商品列表/信息
    List<GoodsVo> findGoodsVo();

    //获取指定商品详情-根据id
    GoodsVo findGoodsVoByGoodsId(Long goodsId);
}
