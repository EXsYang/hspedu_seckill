package com.hspedu.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hspedu.seckill.mapper.SeckillGoodsMapper;
import com.hspedu.seckill.mapper.SeckillOrderMapper;
import com.hspedu.seckill.pojo.SeckillGoods;
import com.hspedu.seckill.pojo.SeckillOrder;
import com.hspedu.seckill.service.SeckillGoodsService;
import com.hspedu.seckill.service.SeckillOrderService;
import org.springframework.stereotype.Service;

/**
 * @author yangda
 * @create 2024-04-25-18:18
 * @description: 如果没有写对应的实现子类，springboot启动会报错
 * ,因为如果没注入对应的实现子类,在按照接口去装配 SeckillOrderService 时肯定会失败,所以启动报错
 */
@Service
public class SeckillOrderServiceImpl
        extends ServiceImpl<SeckillOrderMapper, SeckillOrder>
        implements SeckillOrderService {
}
