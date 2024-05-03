package com.hspedu.seckill.rabbitmq;

import cn.hutool.json.JSONUtil;
import com.hspedu.seckill.pojo.SeckillMessage;
import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.GoodsService;
import com.hspedu.seckill.service.OrderService;
import com.hspedu.seckill.vo.GoodsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author yangda
 * @create 2024-05-03-1:33
 * @description:
 *  MQReceiverMessage: 秒杀消息的消费者/接收者,在这里进行真正的秒杀/下单操作
 *  调用orderService.seckill(user,goodsVo);方法
 */
@Service
@Slf4j
public class MQReceiverMessage {

    //装配需要的组件/对象
    //构建goodsVo需要用到
    @Resource
    private GoodsService goodsService;

    //装配OrderService用来在MQReceiverMessage中调用真正的秒杀方法seckill()
    @Resource
    private OrderService orderService;

    //接收消息,并完成下单
    @RabbitListener(queues = "seckillQueue")
    public void queue(String message) {

        //
        log.info("RabbitMQ接收者/消费者 MQReceiverMessage 接收到的消息是-->" + message);

        //这里我们从队列中取出的是String类型的数据
        //但是我们需要的是SeckillMessage类型,因此这里需要一个工具类
        //JSONUtil 在hutool依赖
        // 使用工具类JSONUtil 将String类型转为指定类型(SeckillMessage)
        // JSONUtil.toBean(String jsonString, Class<T> beanClass)
        SeckillMessage seckillMessage =
                JSONUtil.toBean(message, SeckillMessage.class);

        //得到秒杀的用户对象
        User user = seckillMessage.getUser();
        //秒杀的商品id
        Long goodsId = seckillMessage.getGoodsId();


        //通过商品id得到对应的goodsVo
        // GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);

        //进行真正的秒杀/下单操作
        // orderService.seckill(user,goodsVo);


        //增加日志和确认逻辑
        try {
            //通过商品id得到对应的goodsVo
            GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
            //进行真正的秒杀/下单操作
            orderService.seckill(user, goodsVo);
            // 显式确认消息，具体方法依赖于使用的消息队列客户端库
            // message.acknowledge();
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            // 可能需要进行消息重试逻辑，具体策略依赖于业务需求
            // 重新发送消息或记录失败情况
        }



    }


}
