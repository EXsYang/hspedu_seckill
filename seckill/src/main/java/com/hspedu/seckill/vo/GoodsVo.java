package com.hspedu.seckill.vo;

import com.hspedu.seckill.pojo.Goods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author yangda
 * @create 2024-04-24-14:43
 * @description:
 * GoodsVo对应显示在秒杀页面商品列表的信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsVo extends Goods {

    private BigDecimal seckillPrice;

    private Integer stockCount;

    private Date startDate;

    private Date endDate;

    //如果后面有需求可以再增加...
}
