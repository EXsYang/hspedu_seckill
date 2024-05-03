package com.hspedu.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author yangda
 * @create 2024-04-28-19:08
 * @description:
 * MQSender: 消息的发送者/生产者
 */
@Service
@Slf4j
public class MQSender {


    //装配RabbitTemplate -> 操作 RabbitMQ
    @Resource
    private RabbitTemplate rabbitTemplate;


    //方法:发送消息
    public void send(Object msg){
        log.info("发送消息-->" + msg);
        //"queue" 指定要发送到的队列的名字
        //convert:转换;转化;转换
        rabbitTemplate.convertAndSend("queue",msg);
    }

    //方法: 发送消息到交换机 fanoutExchange
    public void sendFanout(Object msg){
        log.info("sendFanout 发送消息-->" + msg);
        //"fanoutExchange" 填入的是要发送消息到交换机的 交换机的名称
        //传入空串"" 忽略路由
        rabbitTemplate.convertAndSend("fanoutExchange","",msg);
    }


    //方法: 发送消息到direct交换机,同时指定路由queue.red
    public void sendDirect1(Object msg){
        log.info("sendDirect1 发送消息-->" + msg);
        //"fanoutExchange" 填入的是要发送消息到交换机的 交换机的名称
        //在第二个参数指定路由routingKey-> queue.red
        rabbitTemplate.convertAndSend("directExchange","queue.red",msg);
    }
    //方法: 发送消息到direct交换机,同时指定路由queue.green
    public void sendDirect2(Object msg){
        log.info("sendDirect2 发送消息-->" + msg);
        //"fanoutExchange" 填入的是要发送消息到交换机的 交换机的名称
        //在第二个参数指定路由routingKey-> queue.green
        rabbitTemplate.convertAndSend("directExchange","queue.green",msg);
    }


    //方法: 发送消息到topic交换机,同时指定路由 queue.red.message
    public void sendTopic3(Object msg){
        log.info("sendTopic3 发送消息-->" + msg);
        //"topicExchange" 填入的是要发送消息到交换机的 交换机的名称
        //在第二个参数指定路由routingKey-> queue.red.message
        rabbitTemplate.convertAndSend("topicExchange","queue.red.message",msg);
    }

    //方法: 发送消息到topic交换机,同时指定路由 queue.red.message
    public void sendTopic4(Object msg){
        log.info("sendTopic4 发送消息-->" + msg);
        //"topicExchange" 填入的是要发送消息到交换机的 交换机的名称
        //在第二个参数指定路由routingKey-> green.queue.green.message
        rabbitTemplate.convertAndSend("topicExchange","green.queue.green.message",msg);
    }

    //方法: 发送消息到topic交换机,同时携带/指定 匹配的k-v
    public void sendHeader01(String msg){
        log.info("sendHeader01 发送消息-->" + msg);
        //创建消息属性MessageProperties
        MessageProperties properties = new MessageProperties();
        properties.setHeader("color","red");
        properties.setHeader("speed","fast");
        //创建Message对象【包含了发送的消息本身和消息属性】
        //这里是使用byte数组来接收的,因此需要将String类型转为byte数组,这里是要求,按着来即可
        Message message = new Message(msg.getBytes(), properties);
        //发送消息到交换机
        rabbitTemplate.convertAndSend("headersExchange","",message);

    }
    //方法: 发送消息到topic交换机,同时携带/指定 匹配的k-v
    public void sendHeader02(String msg){
        log.info("sendHeader02 发送消息-->" + msg);
        //创建消息属性MessageProperties
        MessageProperties properties = new MessageProperties();
        properties.setHeader("color","red");
        properties.setHeader("speed","normal");
        //创建Message对象【包含了发送的消息本身和消息属性】
        //这里是使用byte数组来接收的,因此需要将String类型转为byte数组,这里是要求,按着来即可
        Message message = new Message(msg.getBytes(), properties);
        //发送消息到交换机
        rabbitTemplate.convertAndSend("headersExchange","",message);

    }



}
