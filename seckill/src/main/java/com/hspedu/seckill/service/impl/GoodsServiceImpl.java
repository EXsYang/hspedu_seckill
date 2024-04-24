package com.hspedu.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hspedu.seckill.mapper.GoodsMapper;
import com.hspedu.seckill.mapper.UserMapper;
import com.hspedu.seckill.pojo.Goods;
import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.GoodsService;
import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.vo.GoodsVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author yangda
 * @create 2024-04-24-19:11
 * @description:
 */
@Service
public class GoodsServiceImpl
        extends ServiceImpl<GoodsMapper, Goods>
        implements GoodsService {

    //装配
    @Resource
    private GoodsMapper goodsMapper;

    //返回秒杀商品列表/信息
    @Override
    public List<GoodsVo> findGoodsVo() {
        return goodsMapper.findGoodsVo();
    }

    //获取指定商品的id,返回秒杀商品的详情
    @Override
    public GoodsVo findGoodsVoByGoodsId(Long goodsId) {
        return goodsMapper.findGoodsVoByGoodsId(goodsId);
    }
}
