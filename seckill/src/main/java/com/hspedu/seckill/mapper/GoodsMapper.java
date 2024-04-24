package com.hspedu.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hspedu.seckill.pojo.Goods;
import com.hspedu.seckill.vo.GoodsVo;

import java.util.List;

/**
 * @author yangda
 * @create 2024-04-24-14:48
 * @description:
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    //返回秒杀商品列表/信息
    List<GoodsVo> findGoodsVo();

    //获取指定商品详情-根据id
    //如果在Mapper接口中的形参位置没有任何指定 则在Mapper.xml文件中
    // 可以直接使用 #{goodsId} 取出 下面形参中传入的值
    GoodsVo findGoodsVoByGoodsId(Long goodsId);



}
