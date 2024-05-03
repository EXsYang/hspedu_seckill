package com.hspedu.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yangda
 * @create 2024-04-28-18:59
 * @description: 配置类,可以创建队列,交换机...
 */
@Configuration
public class RabbitMQConfig {

    //定义队列名
    private static final String QUEUE = "queue";

    //--fanout--
    private static final String QUEUE1 = "queue_fanout01"; //队列的名称
    private static final String QUEUE2 = "queue_fanout02"; //队列的名称
    private static final String EXCHANGE = "fanoutExchange"; //交换机的名称


    //--direct--
    private static final String QUEUE_DIRECT1 = "queue_direct01"; //队列的名称
    private static final String QUEUE_DIRECT2 = "queue_direct02"; //队列的名称
    private static final String EXCHANGE_DIRECT = "directExchange"; //交换机的名称

    //路由
    private static final String ROUTING_KEY01 = "queue.red";
    private static final String ROUTING_KEY02 = "queue.green";



    //创建队列
    /**
     * 1. 配置队列
     * 2. 队列名QUEUE(queue)
     * 3. true: 表示持久化
     * 3. durable: 队列是否持久化  durable英文意思就是持久化的意思
     * 队列在默认情况下是放到内存的,rabbitmq重启就丢失了,如果希望重启后
     * ,队列数据还能使用，就需要持久化,Erlang自带Mnesia数据库,当rabbitmq重启后,会读取该数据库
     * 从而进行恢复数据
     */
    @Bean
    public Queue queue(){
        return new Queue(QUEUE,true);
    }


    /**
     * 创建队列 QUEUE1(queue_fanout01)
     * 如果没有指定第二个参数durable,默认就是true,支持持久化的
     * durable:默认就是true
     */
    @Bean
    public Queue queue1(){
        return new Queue(QUEUE1);
    }

    /**
     * 创建队列 QUEUE2(queue_fanout02)
     */
    @Bean
    public Queue queue2(){
        return new Queue(QUEUE2);
    }

    /**
     * 创建/配置 交换机 EXCHANGE(fanoutExchange)
     */
    @Bean
    public FanoutExchange exchange(){
        return new FanoutExchange(EXCHANGE);
    }

    /**
     * 将队列 QUEUE1(queue_fanout01) 绑定到交换机 EXCHANGE(fanoutExchange)
     */
    @Bean
    public Binding binding01(){
        return BindingBuilder.bind(queue1()).to(exchange());
    }

    /**
     * 将队列 QUEUE2(queue_fanout02) 绑定到交换机 EXCHANGE(fanoutExchange)
     */
    @Bean
    public Binding binding02(){
        return BindingBuilder.bind(queue2()).to(exchange());
    }


    //--direct--

    /**
     * 创建/配置队列 QUEUE_DIRECT1(queue_direct01)
     * @return
     */
    @Bean
    public Queue queue_direct1(){
        return new Queue(QUEUE_DIRECT1);
    }
    /**
     * 创建/配置队列 QUEUE_DIRECT2(queue_direct02)
     * @return
     */
    @Bean
    public Queue queue_direct2(){
        return new Queue(QUEUE_DIRECT2);
    }


    /**
     * 创建/配置 交换机 EXCHANGE_DIRECT(directExchange)
     */
    @Bean
    public DirectExchange exchange_direct(){
        return new DirectExchange(EXCHANGE_DIRECT);
    }

    /**
     * 1. 将队列 queue_direct1QUEUE_DIRECT1(queue_direct01)
     *    绑定到交换机 exchange_direct() EXCHANGE_DIRECT(directExchange)
     * 2. 同时声明了/关联路由 ROUTING_KEY01(queue.red)
     * (1) 队列: queue_direct1()
     * (2) 交换机: exchange_direct()
     * (3) 路由: ROUTING_KEY01
     */
    @Bean
    public Binding binding_direct1(){
        return BindingBuilder
                .bind(queue_direct1()).to(exchange_direct()).with(ROUTING_KEY01);
    }

    @Bean
    public Binding binding_direct2(){
        return BindingBuilder
                .bind(queue_direct2()).to(exchange_direct()).with(ROUTING_KEY02);
    }


}
