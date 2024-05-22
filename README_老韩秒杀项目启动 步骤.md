启动秒杀项目的完整视频可以参考老韩seckill项目视频下部分1-62视频



# 0 秒杀项目的application.yml文件如下:

~~~yaml
spring:
  thymeleaf:
    #关闭缓存
    cache: false
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/seckill?useUnicode=true&characterEncoding=utf-8&useSSL=true
    username: root
    #本地windows主机mysql5.7的mysql密码是hsp
    #password: hsp
    #本地windows主机mysql8的mysql密码是1234
    password: 1234
    # 数据库连接池
    hikari:
      #连接池名
      pool-name: Hsp_Hikari_Poll
      #最小空闲连接
      minimum-idle: 5
      #空闲连接存活最大时间，默认 60000(10 分钟)
      idle-timeout: 60000
      # 最大连接数，默认是 10
      maximum-pool-size: 10
      #从连接池返回来的连接自动提交
      auto-commit: true
      #连接最大存活时间。0 表示永久存活，默认 180000（30 分钟）
      max-lifetime: 180000
      #连接超时时间，默认 30000（30 秒）
      connection-timeout: 30000
      #测试连接是否可用的查询语句
      connection-test-query: select 1

  #配置Redis
  redis:
    host: 192.168.198.135
    port: 6379
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        #最大连接数 默认是8 CPU内核*2 即可
        max-active: 8
        #最大的连接等待时间 默认是-1即一直等待
        max-wait: 10000ms
        #最大空闲连接数  默认是8
        max-idle: 200
        #最小空闲连接数  默认是0
        min-idle: 5
 #配置rabbitMQ
  rabbitmq:
    #rabbitmq的ip
    host: 192.168.198.135
    username: guest
    password: guest
    #虚拟主机
    virtual-host: /
    #端口
    port: 5672
    listener:
      simple:
        #消费者的最小数量
        concurrency: 10
        #消费者的最大数量
        max-concurrency: 10
        #限制消费者,每次只能处理一条消息,处理完才能继续下一条消息
        prefetch: 1
        #启动时，是否默认启动容器,默认true
        auto-startup: true
        #被拒绝后，重新进入队列
        default-requeue-rejected: true
    template:
      retry:
        #启用重试机制,默认false
        enabled: true
        #设置初始化的重试时间间隔
        initial-interval: 1000ms
        #重试最大次数,默认是3
        max-attempts: 3
        #重试最大时间间隔，默认是10s
        max-interval: 10000ms
        #重试时间间隔的乘数
        #比如配置是2 ：第1次重试等待 等 1s, 第2次等 2s,第3次等 4s..
        #比如配置是1 ：第1次等 1s, 第2次等 1s,第3次等 1s..
        multiplier: 1




#mybatis-plus 配置
mybatis-plus:
  #配置 mapper.xml 映射文件
  mapper-locations: classpath*:/mapper/*Mapper.xml
  #配置 mybatis 数据返回类型别名
  type-aliases-package: com.hspedu.seckill.pojo
#mybatis sql 打印
#logging:
#  level:
#    com.hspedu.seckill.mapper: debug
~~~















# 1 创建并连接本地数据库 这里使用的是mysql8

## 1.1 先要保证本地mysql8服务正在运行

![image-20240522121314909](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522121314909.png)



## 1.2 检查windows主机本地使用的MySQL版本



![image-20240522103611154](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522103611154.png)



**系统环境变量即是所有用户公用的变量，用户变量是和当前登录windows的用户关联的变量 **

MYSQL_HOME系统变量的值如下:

~~~
MYSQL_HOME    D:\atguigu_program\mysql-8.4.0\mysql-8.0.37-winx64
~~~





![image-20240522103820394](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522103820394.png)



系统环境变量Path的值如下:

~~~
%JAVA_HOME%\bin;%JRE_HOME%\bin;C:\Program Files\Common Files\Oracle\Java\javapath;D:\setupruanjian\app\oracle\product\11.2.0\server\bin;C:\Program Files (x86)\Intel\iCLS Client\;C:\Program Files\Intel\iCLS Client\;C:\Windows\system32;C:\Windows;C:\Windows\System32\Wbem;C:\Windows\System32\WindowsPowerShell\v1.0\;C:\Program Files (x86)\Intel\Intel(R) Management Engine Components\DAL;C:\Program Files\Intel\Intel(R) Management Engine Components\DAL;C:\Program Files (x86)\Intel\Intel(R) Management Engine Components\IPT;C:\Program Files\Intel\Intel(R) Management Engine Components\IPT;C:\Program Files (x86)\NVIDIA Corporation\PhysX\Common;C:\Program Files\Intel\WiFi\bin\;C:\Program Files\Common Files\Intel\WirelessCommon\;C:\WINDOWS\system32;C:\WINDOWS;C:\WINDOWS\System32\Wbem;C:\WINDOWS\System32\WindowsPowerShell\v1.0\;C:\WINDOWS\System32\OpenSSH\;C:\Program Files\NVIDIA Corporation\NVIDIA NvDLISR;C:\Program Files\dotnet\;D:\atguigu_program\mysql-8.0.37\mysql-8.0.37-winx64\bin;D:\hspmysql\mysql-5.7.25-winx64\bin;C:\Program Files (x86)\ZeroTier\One\;C:\Program Files\Cloudflare\Cloudflare WARP\;D:\Java_developer_tools\Git\Git\cmd;D:\Java_developer_tools\Must_learn_must_know_technology\Linux\Xshell 7\;D:\Java_developer_tools\Must_learn_must_know_technology\Linux\Xftp 7\;D:\Java_developer_tools\Git\Git LFS;D:\Java_developer_tools\DistributedMicroservicesProject\program\Vagrant\bin;D:\Java_developer_tools\DistributedMicroservicesProject\program\nodeJs10163\;D:\Java_developer_tools\TortoiseSVN\bin
~~~



其中关键点为mysql相关的两条配置,发现在系统变量Path中，mysql8的bin目录在前,mysql5.7的bin目录在后,

，因此此时**mysql8生效**,mysql5.7没有生效

~~~
D:\atguigu_program\mysql-8.0.37\mysql-8.0.37-winx64\bin;D:\hspmysql\mysql-5.7.25-winx64\bin;
~~~



![image-20240522104218154](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522104218154.png)



使用Navicat连接本地的mysql8服务

![image-20240522104436285](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522104436285.png)



**注意此时在application.yml文件中对应的数据库连接的密码也要做修改，修改为1234**



![image-20240522104535868](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522104535868.png)





# 2 启动Linux中的Redis服务

用到的指令

~~~
[root@hspEdu100 ~]# redis-server /hspredis/redis.conf 
[root@hspEdu100 ~]# ps -aux |grep redis
root      78289  0.2  0.1 164972  3500 ?        Ssl  11:00   0:00 redis-server *:6379
root      78356  0.0  0.0 112724   988 pts/1    S+   11:01   0:00 grep --color=auto redis
[root@hspEdu100 ~]# redis-cli
127.0.0.1:6379> keys *

~~~



Linux虚拟机地址是 192.168.198.135 ，使用的是VMware虚拟机软件

Redis服务和RabbitMQ服务都需要在VMware虚拟机软件 IP为 192.168.198.135的虚拟机上启动

1

![image-20240522105910417](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522105910417.png)

2

![image-20240522110013005](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522110013005.png)



3

![image-20240522105718400](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522105718400.png)



4 

![image-20240522110328658](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522110328658.png)

5 检查防火墙是否打开了该端口

![image-20240522110424460](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522110424460.png)

6 检查windows主机和linux虚拟机网络是否畅通

![image-20240522110721177](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522110721177.png)



![image-20240522110643937](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522110643937.png)



# 3 查看RabbitMQ的启动状态

命令：

~~~
[root@hspEdu100 ~]# /sbin/service rabbitmq-server status

[root@hspEdu100 ~]# /sbin/service rabbitmq-server stop

[root@hspEdu100 ~]# /sbin/service rabbitmq-server start

~~~



![image-20240522111100373](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522111100373.png)



![image-20240522111554559](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522111554559.png)



# 4 打开Redis Desktop方便观察

![image-20240522111838313](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522111838313.png)





# 5 打开jmeter压测工具

双击运行

![image-20240522111920410](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522111920410.png)



文件所在位置

~~~
D:\Java_developer_tools\DistributedMicroservicesProject\program\Jmeter-5.4.3\apache-jmeter-5.4.3\apache-jmeter-5.4.3\bin
~~~



![image-20240522111951770](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522111951770.png)



1

![image-20240522112149658](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522112149658.png)

2

![image-20240522112236645](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522112236645.png)

3 将jmeter配置文件直接拖拽到Test Plan的位置即可复用之前配置好的配置

![image-20240522112832656](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522112832656.png)



# 6 通过启动类启动项目



![image-20240522112606203](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522112606203.png)



# 7 压测秒杀接口 `/seckill/doSeckill`



![image-20240522114423451](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522114423451.png)



在后端的Controller中需要打开对应的接口方法  `/seckill/doSeckill`

1

![image-20240522114646580](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522114646580.png)

2

![image-20240522114526830](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522114526830.png)



使用UserController工具类生成2000个测试用户并将用户信息保存到Redis中

![image-20240522114818274](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522114818274.png)



进行压测

1

![image-20240522114852519](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522114852519.png)

2

![image-20240522114932170](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522114932170.png)

3 观察聚合报告中的吞吐量 QPS 

![image-20240522114954850](https://raw.githubusercontent.com/EXsYang/PicGo-images-hosting/main/images/image-20240522114954850.png)





# 8 seckill秒杀项目使用到的建库建表相关的sql脚本如下:

~~~mysql
#创建数据库seckill
DROP DATABASE IF EXISTS `seckill`;
CREATE DATABASE `seckill`;
USE `seckill`;

#1. 创建表 seckill_user

DROP TABLE IF EXISTS `seckill_user`;

CREATE TABLE `seckill_user` (
`id` BIGINT(20) NOT NULL COMMENT '用户 ID, 设为主键, 唯一 手机号', 
`nickname` VARCHAR(255) NOT NULL DEFAULT '', 
`password` VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'MD5(MD5(pass 明文+固定salt)+salt)', 
`salt` VARCHAR(10) NOT NULL DEFAULT '',
`head` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '头像', 
`register_date` DATETIME DEFAULT NULL COMMENT '注册时间', 
`last_login_date` DATETIME DEFAULT NULL COMMENT '最后一次登录时间', 
`login_count` INT(11) DEFAULT '0' COMMENT '登录次数',
PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

SELECT * FROM `seckill_user`;

INSERT INTO `seckill_user` (id, nickname, PASSWORD, salt, head, register_date, login_count)
VALUES (13300000000, 'tom', '2ef6dd46e8d4ec44f09f4033a59bfbf0', '3cj5tnMw', 'tx', NOW(), 0);

INSERT INTO `seckill_user` (id, nickname, PASSWORD, salt, head, register_date, login_count)
VALUES (13300000001, 'jack', '2ef6dd46e8d4ec44f09f4033a59bfbf0', '3cj5tnMw', 'tx', NOW(), 0);

DELETE FROM `seckill_user` WHERE id >= 13300000100;


SELECT * FROM `seckill_user` WHERE id >= 13300000890;



DROP TABLE IF EXISTS `t_goods`;
# 秒杀商品表，存放的是所有的商品
CREATE TABLE `t_goods` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '商品id', 
`goods_name` VARCHAR(16) NOT NULL DEFAULT '', 
`goods_title` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '商品标题', 
`goods_img` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '商品图片', 
`goods_detail` LONGTEXT NOT NULL COMMENT '商品详情',
`goods_price` DECIMAL(10,2) DEFAULT '0.00' COMMENT '商品价格', 
`goods_stock` INT(11) DEFAULT '0' COMMENT '商品库存', 
PRIMARY KEY (`id`)
) ENGINE=INNODB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;




INSERT INTO `t_goods` VALUES ('1', '整体厨房设计-套件', '整体厨房设计-套件', '/imgs/kitchen.jpg', '整体厨房设计-套件', '15266.00', '100');
INSERT INTO `t_goods` VALUES ('2', '学习书桌-套件', '学习书桌-套件', '/imgs/desk.jpg', '学习书桌-套件', '5690.00', '100');


DROP TABLE IF EXISTS `t_seckill_goods`;

CREATE TABLE `t_seckill_goods` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀商品id', 
`goods_id` BIGINT(20) DEFAULT 0 COMMENT '该秒杀商品对应t_goods表的id', 
`seckill_price` DECIMAL(10,2) DEFAULT '0.00' COMMENT '秒杀价', 
`stock_count` INT(10) DEFAULT 0 COMMENT '秒杀商品库存', 
`start_date` DATETIME DEFAULT NULL COMMENT '秒杀开始时间', 
`end_date` DATETIME DEFAULT NULL COMMENT '秒杀结束时间', 
PRIMARY KEY (`id`)
) ENGINE=INNODB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;


INSERT INTO `t_seckill_goods` VALUES ('1', '1', '5266.00', '0', '2022-11-18 19:36:00', '2022-11-19 09:00:00');
INSERT INTO `t_seckill_goods` VALUES ('2', '2', '690.00', '10', '2022-11-18 08:00:00', '2022-11-19 09:00:00');


SELECT * FROM `t_seckill_goods`;
#下面这条语句，即使条件不满足，执行之后也不报错，只是影响行数为0而已
UPDATE t_seckill_goods SET stock_count = stock_count - 1 WHERE goods_id = 1 AND stock_count > 0;


#编写sql,可以返回秒杀商品列表/信息
 -- 左外连接，左侧的表完全显示

SELECT g.id, g.goods_name, 
	g.goods_title, 
	g.goods_img,
	g.goods_detail, 
	g.goods_price, 
	g.goods_stock, 
	sg.seckill_price, 
	sg.stock_count, 
	sg.start_date, 
	sg.end_date
	FROM 
	t_goods g LEFT JOIN t_seckill_goods AS sg -- 左外连接，左侧的表完全显示
	ON g.id = sg.goods_id; -- ON 后面写条件，相当于where


-- 获取指定商品详情-根据id
 SELECT
            g.id, g.goods_name,
            g.goods_title,
            g.goods_img,
            g.goods_detail,
            g.goods_price,
            g.goods_stock,
            sg.seckill_price,
            sg.stock_count,
            sg.start_date,
            sg.end_date
            FROM
            t_goods g LEFT JOIN t_seckill_goods AS sg -- 左外连接，左侧的表完全显示
            ON g.id = sg.goods_id -- ON 后面写条件，相当于where
            WHERE g.id =1;




-- ----------------------------
-- Table structure for t_order 普通订单表,记录订单完整信息
-- ---------------------------- DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT, 
`user_id` BIGINT(20) NOT NULL DEFAULT 0, 
`goods_id` BIGINT(20) NOT NULL DEFAULT 0, 
`delivery_addr_id` BIGINT(20) NOT NULL DEFAULT 0, 
`goods_name` VARCHAR(16) NOT NULL DEFAULT '', 
`goods_count` INT(11) NOT NULL DEFAULT '0', 
`goods_price` DECIMAL(10,2) NOT NULL DEFAULT '0.00', 
`order_channel` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '订单渠道 1pc，2Android，3ios', 
`status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '订单状态：0 新建未支付 1 已支付 2 已发货 3 已收货 4 已退款 5 已完成',
`create_date` DATETIME DEFAULT NULL, `pay_date` DATETIME DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=INNODB AUTO_INCREMENT=600 DEFAULT CHARSET=utf8mb4;
-- ----------------------------
-- Table structure for t_seckill_order 秒杀订单表,记录某用户 id,秒杀的商品 id,及其订单 id
-- ---------------------------- DROP TABLE IF EXISTS `t_seckill_order`;
CREATE TABLE `t_seckill_order` (
`id` BIGINT(20) NOT NULL AUTO_INCREMENT, 
`user_id` BIGINT(20) NOT NULL DEFAULT 0, 
`order_id` BIGINT(20) NOT NULL DEFAULT 0, 
`goods_id` BIGINT(20) NOT NULL DEFAULT 0,
PRIMARY KEY (`id`),
UNIQUE KEY `seckill_uid_gid` (`user_id`,`goods_id`) USING BTREE COMMENT ' 用户 id，商
品 id 的唯一索引，解决同一个用户多次抢购' ) ENGINE=INNODB AUTO_INCREMENT=300 DEFAULT CHARSET=utf8mb4;

SELECT * FROM `t_seckill_order`;

#在MySQL5.7中查看Mysql当前事务隔离级别
SELECT @@tx_isolation;

#在MySQL8中查看Mysql当前事务隔离级别
SELECT @@transaction_isolation;

#REPEATABLE-READ


~~~

