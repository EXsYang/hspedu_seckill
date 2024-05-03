package com.hspedu.seckill.controller;

import com.hspedu.seckill.rabbitmq.MQSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @author yangda
 * @create 2024-04-28-19:18
 * @description:
 */
@Controller
public class RabbitMQHandler {

    //装配MQSender
    @Resource
    private MQSender mqSender;


    //方法:调用消息生产者,发送消息
    @RequestMapping("/mq")
    @ResponseBody
    public void mq(){
        mqSender.send("hello hsp!!");
    }


    //方法:调用消息生产者,发送消息到交换机 fanoutExchange
    @RequestMapping("/mq/fanout")
    @ResponseBody
    public void fanout(){
        mqSender.sendFanout("hello jack!!");
    }


    //方法:调用消息生产者,发送消息到交换机 directExchange
    @RequestMapping("/mq/direct01")
    @ResponseBody
    public void direct01(){
        mqSender.sendDirect1("hello smith!!");
    }

    //方法:调用消息生产者,发送消息到交换机 directExchange
    @RequestMapping("/mq/direct02")
    @ResponseBody
    public void direct02(){
        mqSender.sendDirect2("hello tomcat!!");
    }

    //方法:调用消息生产者,发送消息到交换机 topicExchange
    @RequestMapping("/mq/topic01")
    @ResponseBody
    public void topic01(){
        mqSender.sendTopic3("hello red!!");
    }

    //方法:调用消息生产者,发送消息到交换机 topicExchange
    @RequestMapping("/mq/topic02")
    @ResponseBody
    public void topic02(){
        mqSender.sendTopic4("hello green!!");
    }


    //方法:调用消息生产者,发送消息到交换机 headersExchange
    @RequestMapping("/mq/header01")
    @ResponseBody
    public void header01(){
        //希望两个队列都接收到 queue_header01 queue_header02
        mqSender.sendHeader01("hello ABC!!");
    }
    //方法:调用消息生产者,发送消息到交换机 headersExchange
    @RequestMapping("/mq/header02")
    @ResponseBody
    public void header02(){
        //希望只有第1个队列接收到 queue_header01
        mqSender.sendHeader02("hello HSP!!");
    }

}
