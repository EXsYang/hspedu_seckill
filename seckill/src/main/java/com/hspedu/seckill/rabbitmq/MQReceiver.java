package com.hspedu.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * @author yangda
 * @create 2024-04-28-19:14
 * @description:
 * MQReceiver: 消息的接收者/消费者
 */
@Service
@Slf4j
public class MQReceiver {

    //方法:接收消息

    /**
     * (queues = "queue") 指定要接收哪一个队列中的消息,可以指定多个
     *
     */
    @RabbitListener(queues = "queue")
    public void receive(Object msg){
        log.info("接收到的消息-->" + msg);

    }

    /**
     * 监听队列 queue_fanout01
     * @param msg
     */
    @RabbitListener(queues = "queue_fanout01")
    public void receive1(Object msg){
        log.info("从queue_fanout01接收到的消息-->" + msg);
    }

    /**
     * 监听队列 queue_fanout02
     * @param msg
     */
    @RabbitListener(queues = "queue_fanout02")
    public void receive2(Object msg){
        log.info("从queue_fanout02接收到的消息-->" + msg);
    }


    /**
     * 监听队列 queue_direct01
     * @param msg
     */
    @RabbitListener(queues = "queue_direct01")
    public void queue_direct1(Object msg){
        log.info("从 queue_direct01 接收到的消息-->" + msg);
    }

    /**
     * 监听队列 queue_direct02
     * @param msg
     */
    @RabbitListener(queues = "queue_direct02")
    public void queue_direct2(Object msg){
        log.info("从 queue_direct02 接收到的消息-->" + msg);
    }

    /**
     * 监听队列 queue_topic01
     * @param msg
     */
    @RabbitListener(queues = "queue_topic01")
    public void queue_topic01(Object msg){
        log.info("从 queue_topic01 接收到的消息-->" + msg);
    }
    /**
     * 监听队列 queue_topic02
     * @param msg
     */
    @RabbitListener(queues = "queue_topic02")
    public void queue_topic02(Object msg){
        log.info("从 queue_topic02 接收到的消息-->" + msg);
    }

    /**
     * 监听队列 queue_header01
     * 发送时用的是Message类型,这里接收也是用Message类型
     * @param message
     */
    @RabbitListener(queues = "queue_header01")
    public void queue_header01(Message message){
        log.info("queue_header01 接收到的消息对象-->" + message);
        //Java基础 byte[] -> String 使用String的构造器即可
        log.info("queue_header01 接收到的消息内容-->" + new String(message.getBody()));
    }
    /**
     * 监听队列 queue_header02
     * 发送时用的是Message类型,这里接收也是用Message类型
     * @param message
     */
    @RabbitListener(queues = "queue_header02")
    public void queue_header02(Message message){
        log.info("queue_header02 接收到的消息对象-->" + message);
        //Java基础 byte[] -> String 使用String的构造器即可
        log.info("queue_header02 接收到的消息内容-->" + new String(message.getBody()));
    }






}
