package com.hspedu.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author yangda
 * @create 2024-04-18-15:57
 * @description:
 */
@SpringBootApplication
@MapperScan("com.hspedu.seckill.mapper")
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class,args);
        System.out.println("SeckillApplication started...");
    }
}
