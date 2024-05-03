package com.hspedu.seckill.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangda
 * @create 2024-04-29-17:09
 * @description: RabbitMQHeadersConfig: 配置类， 创建/配置 队列和交换机,并完成绑定
 */
@Configuration
public class RabbitMQHeadersConfig {

    //定义队列和交换机的名称
    private static final String QUEUE01 = "queue_header01";
    private static final String QUEUE02 = "queue_header02";
    private static final String EXCHANGE = "headersExchange";

    // 创建/配置 队列和交换机
    @Bean
    public Queue queue_header01(){
        return new Queue(QUEUE01);
    }
    @Bean
    public Queue queue_header02(){
        return new Queue(QUEUE02);
    }

    @Bean
    public HeadersExchange headersExchange(){
        return new HeadersExchange(EXCHANGE);
    }

    //完成队列到交换机的绑定，同时声明要匹配的k-v, 和要以什么方式来匹配(all/any)
    @Bean
    public Binding binding_header01(){
        //先定义/声明 k-v, 因为可以有多个,所以将其放入到map中
        //这里的k-v是什么,是由程序员根据业务指定的
        Map<String, Object> map = new HashMap<>();
        map.put("color","red");
        map.put("speed","low");
        //这里执行queue_header01()方法时,会将注入的对象从容器中取回来放入到这里的.bind()方法中
        //为什么执行 queue_header01() 方法时，会将注入的对象从容器中取回来放入到这里的 .bind() 方法中？
        /**
         * 默认情况下，通过 `@Configuration` 和 `@Bean` 注解注入的对象是单例的。
         * 这意味着 Spring 容器在启动时会创建这些对象的唯一实例，
         * 并且在整个应用程序的生命周期内都会重用这些实例。
         * 在这段代码中，使用了 Spring Framework 的依赖注入（DI）特性来管理和配置应用中的对象。具体来说：
         *
         * 在定义队列和交换机时，通过 @Bean 注解声明了这些组件，并将其作为 Spring 容器管理的 beans。
         * 当调用 queue_header01() 方法时，如果 Spring 容器已经创建了该方法所返回的 Queue 实例并将其存储在容器中，容器会直接返回已存在的实例，而不是每次都创建新的实例。
         * 在绑定队列到交换机时，通过从 Spring 容器中获取队列的实例，确保了队列实例的单一性和一致性，从而避免了不必要的对象创建并提高了效率和一致性。
         * 这种依赖注入的方式允许整个应用中的不同部分共享和复用相同的对象实例，同时保持配置的集中管理和易于变更的特性。
         */


        //在这个方法中，使用的是 .whereAny(map) 方法，
        // 意味着只要消息的头部包含 map 中的任意一个键值对（key-value pair），
        // 就可以与 queue_header01 队列匹配成功。
        //指定的匹配方式是 any，即只要消息头部包含这里指定的 map 中
        // 的任意一组 k-v 即可匹配上这里的交换机绑定的队列。”
        return BindingBuilder.bind(queue_header01())
                .to(headersExchange()).whereAny(map).match();

    }

    //完成队列到交换机的绑定，同时声明要匹配的k-v, 和要以什么方式来匹配(all/any)
    @Bean
    public Binding binding_header02(){
        //先定义/声明 k-v, 因为可以有多个,所以将其放入到map中
        //这里的k-v是什么,是由程序员根据业务指定的
        Map<String, Object> map = new HashMap<>();
        map.put("color","red");
        map.put("speed","fast");
        //这里执行queue_header01()方法时,会将注入的对象从容器中取回来放入到这里的.bind()方法中
        //使用的是 .whereAll(map) 方法，这意味着消息的头部必须包含所有 map 中的键值对，才能与 queue_header02 队列匹配成功。
        // 指定的匹配方式是 all，即需要消息头部完全包含这里指定的 map 中的
        // 所有 k-v 组才能匹配上这里的交换机绑定的队列。”
        return BindingBuilder.bind(queue_header02())
                .to(headersExchange()).whereAll(map).match();

    }


}
