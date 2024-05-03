package com.hspedu.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author yangda
 * @create 2024-05-03-1:27
 * @description: MQSenderMessage: 消息的生产者/发送者[秒杀消息]
 */
@Service
@Slf4j
public class MQSenderMessage {

    //装配 RabbitTemplate
    @Resource
    private RabbitTemplate rabbitTemplate;

    //方法：发送秒杀消息
    public void sendSeckillMessage(String message) {
        log.info("sendSeckillMessage 发送秒杀消息-->" + message);

        //"seckill.message" 可以和 seckillExchange交换机(Topic交换机)绑定的路由"seckill.#"匹配上
        rabbitTemplate.
                convertAndSend("seckillExchange", "seckill.message", message);

    }


}
