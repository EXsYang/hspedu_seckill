package com.hspedu.seckill.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hspedu.seckill.mapper.GoodsMapper;
import com.hspedu.seckill.mapper.OrderMapper;
import com.hspedu.seckill.pojo.*;
import com.hspedu.seckill.service.GoodsService;
import com.hspedu.seckill.service.OrderService;
import com.hspedu.seckill.service.SeckillGoodsService;
import com.hspedu.seckill.service.SeckillOrderService;
import com.hspedu.seckill.util.MD5Util;
import com.hspedu.seckill.util.UUIDUtil;
import com.hspedu.seckill.vo.GoodsVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * @author yangda
 * @create 2024-04-25-15:46
 * @description:
 */
@Service
public class OrderServiceImpl
        extends ServiceImpl<OrderMapper, Order>
        implements OrderService {

    ////使用了 static 修饰符将 AtomicInteger 变量声明为静态的，这意味着它属于类级别，而不是对象级别。
    // //在 AtomicInteger 类的使用中，new AtomicInteger(0) 用于创建一个新的 AtomicInteger 实例，
    // //其中的 0 是这个 AtomicInteger 实例的初始值。这意味着计数器开始时的数值为 0。
    // private static AtomicInteger failureCount = new AtomicInteger(0);
    public static AtomicInteger failureCount = new AtomicInteger(0);
    public static int count = failureCount.get();
    private Order order = null;
    //装配
    @Resource
    private SeckillGoodsService seckillGoodsService;
    //装配
    @Resource
    private OrderMapper orderMapper;


    //计数器
    //`AtomicInteger` 是 Java 中的一个类，属于 `java.util.concurrent.atomic` 包。
    // 它用于在多线程环境中进行原子操作，确保即使在多个线程尝试同时更新同一个变量的情况下，
    // 该变量的操作也是线程安全的。
    //在多线程程序中，简单的整数增加操作（如 `count++`）并不是原子的。这个操作实际上包含三个步骤：
    // 1. 读取 `count` 的当前值。
    // 2. 将值增加 1。
    // 3. 将新值写回 `count`。
    // 在多线程环境下，如果两个线程几乎同时执行这个操作，
    // 它们可能读取相同的初始值，都对其加 1，然后写回，
    // 结果就是 `count` 被增加了 1 而不是预期的 2，这就是典型的竞态条件（Race Condition）。
    // `AtomicInteger` 通过一种叫做 CAS（Compare-And-Swap）
    // 的机制来保证整数操作的原子性。CAS 操作包含三个操作数：
    // 内存位置（在这里是 `count` 的值）、预期原值和新值。
    // CAS 仅在内存位置的值与预期原值相匹配时，才将该位置的值更新为新值。
    // 这个过程是作为单个不可中断的操作完成的，从而保证了原子性。
    //装配
    @Resource
    private SeckillOrderService seckillOrderService;

    //failureCount 计数器显示不正确：在显示计数器值时，
    // 应确保使用 failureCount.get() 来获取当前值。
    // 如果直接通过 failureCount 引用，它不会显示正确的数值。
    @Resource
    private RedisTemplate redisTemplate;
    // public static int count2 = 0;

    // private static void updateFailed() {
    //     count = failureCount.incrementAndGet(); // 原子地将计数加 1 并获取新值
    //     if (count % 10 == 0) {
    //         System.out.println("目前已有 " + count + " 次更新失败");
    //     }
    // }

    //完成秒杀, 我们认为只要能够走到下面这个方法,就是可以进行秒杀的
    // ,即在Controller层,已经确认过秒杀商品的库存了,已经对秒杀商品的库存进行了判断
    @Transactional
    @Override
    public Order seckill(User user, GoodsVo goodsVo) {

        //查询秒杀商品的库存并减1

        //seckillGoodsService.getOne(...) 方法是 用于从数据库中查询单条记录。
        // 这个方法适用于当你期望查询结果为单个实体时使用，
        // 如获取特定条件下的唯一记录。如果查询结果有多条记录，
        // getOne 默认会返回查询结果的第一条数据。

        //下面这里的查询语句，根据goodsId到数据库查询秒杀商品的信息时,
        //在大并发的情况下,在执行减1操作之前,查询回来的数据都是同一条数据
        //即seckillGoods.getStockCount()的值都是相同的值,例如 都是10
        //然后这一堆并发请求读取到库存的值都是10,这一堆请求执行减一操作后,都是将库存设置为9
        //所以出现了订单数量和库存/卖出的商品数量不一致的问题
        //类似于redis中使用Lua脚本保证在redis中查询到的数据的操作,和减1操作原子性的问题
        //
        SeckillGoods seckillGoods = seckillGoodsService
                .getOne(new QueryWrapper<SeckillGoods>()
                        .eq("goods_id", goodsVo.getId()));


        //完成一个基本的秒杀操作[这里现在不具有原子性](因为这里是先查询再更新),后面在高并发的情况下，还会优化
        //先查询再更新
        // 这种方式，虽然看起来直观且易于理解，但在并发环境中会遇到问题：
        // 非原子性：操作不是原子的，因为它包含至少两个步骤：先查询得到数据，然后基于这个数据更新。在高并发下，多个事务可能都读取了相同的旧数据，然后基于这个数据进行更新，这就可能导致数据的不一致性（比如超卖问题）。
        // 隔离级别的影响：即使使用了较高的隔离级别（如可重复读），只有更新操作才会加锁，查询操作通常不加锁，这样就不能防止其他事务在查询和更新操作之间修改数据。

        //秒杀商品库存减1
        // seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
        //更新秒杀商品
        // boolean updateResult = seckillGoodsService.updateById(seckillGoods);


        System.out.println("hello~~ 当前请求进入到真正的秒杀方法执行update操作");

        //直接使用Mysql UPDATE更新语句保证原子性
        /**
         * 保证原子性
         * 数据库层面的操作：通过单一的 UPDATE 语句，数据库同时进行条件检查和更新操作。这意味着这两个步骤是原子的，即不可分割的。当这个语句执行时，涉及的数据行会被锁定，直到事务完成。
         * 行级锁：在 UPDATE 操作期间，MySQL（InnoDB）会自动对符合条件的行施加行级锁。这种锁保证了其他事务无法同时修改这些相同的行，直到当前事务完成，从而避免了数据冲突和一致性问题。
         * 避免数据覆盖和超卖问题
         * 减少数据覆盖：由于 UPDATE 操作是原子的，并且直接在数据库层面完成，因此避免了应用层面的多次数据回写可能导致的数据覆盖问题。
         * 有效处理超卖：特别是在秒杀等需要精确处理库存的场景中，这种方式能有效防止超卖。更新语句通过条件 gt("stock_count", 0) 确保只有在库存大于0时才会减少库存，这自然地阻止了库存变成负数的情况。
         * 提高性能和响应速度
         * 减少数据库访问：直接使用一个 UPDATE 语句减少了必须先查询再更新的两次数据库交互过程，从而降低了网络延迟和数据库加载，尤其是在高并发条件下。
         * 简化应用逻辑：应用代码更简洁，因为它不需要处理中间状态数据，或者担心多线程/多进程环境下的数据同步和锁定问题。
         * 总结来说，通过直接使用更新语句，您不仅提高了操作的原子性和数据一致性，还优化了应用的性能和响应能力。这在处理复杂并发场景时是一个非常重要的策略。
         */
        //查看Mysql当前事务隔离级别 SELECT @@tx_isolation; -> REPEATABLE-READ
        //1. Mysql在默认的事务隔离级别[REPEATABLE-READ]下
        //2. 执行update语句时,会在事务中锁定要更新的行,
        //   如果where条件带上了就会把这一行锁定,如果没有带where条件,就把当前这个表锁定
        //3. 这样可以防止其他的会话在同一行执行update或者delete

        //只有在更新成功时,返回true,否则返回false,即更新后,受影响的行数>= 1才为true
        //null != result && result >= 1;
        // 下面这个操作对应单独的一条更新语句:
        //下面这条语句，即使条件不满足，执行之后也不报错，只是影响行数为0而已
        //因此，在高并发的情况下，冲进来的请求,只要程序执行到下面这一行语句,
        // 即使不满足 UPDATE 语句的执行条件，这条语句实际上还是会被发送到数据库进行处理。
        // 数据库会检查提供的条件，判断是否有行符合条件。
        // 如果没有行符合条件（例如，stock_count 已经为0时尝试减少库存），
        // 则不会进行任何数据修改，并且 UPDATE 语句会返回一个表示影响行数的结果，这里是0行。
        // 即使不满足update语句的执行条件，也到数据库走了一圈,也会对数据库有性能开销
        // UPDATE seckill_goods SET stock_count = stock_count - 1 WHERE goods_id = ? AND stock_count > 0
        boolean update = seckillGoodsService.
                update(new UpdateWrapper<SeckillGoods>()
                        .setSql("stock_count=stock_count-1")
                        .eq("goods_id", goodsVo.getId())
                        .gt("stock_count", 0));

        // System.out.println("update=======================>" + update);

        // 获取实际影响的行数
        // count = update ? failureCount.get() : failureCount.incrementAndGet();

        // System.out.println("目前已有 " + failureCount.get() + " 次更新失败");
        //如果更新失败,说明已经没有库存了

        //这里如果到数据库更新失败则直接返回null,就不走下面的生成订单的代码了
        if (!update) {
            //说明秒杀失败,把秒杀失败的信息,记录到redis中
            //秒杀失败记录到Redis中的 k-v 的设计为:
            //seckillFail:userId:goodsId
            redisTemplate.opsForValue()
                    .set("seckillFail:" + user.getId() + ":" + goodsVo.getId(), 0);
            return null;
        }


        //测试是否到数据库走了一圈
        //结论：是，即上面的update受影响行数为0行，就返回false
        // if (!update) {
        // System.out.println("走了一下数据库，但是当前请求更新数据库失败, 仍然生成订单" +
        //         " ,返回null");
        // // updateFailed();
        // boolean update2 = seckillGoodsService.
        //         update(new UpdateWrapper<SeckillGoods>()
        //                 .setSql("seckill_price=seckill_price-1")
        //                 .eq("goods_id", goodsVo.getId()));
        // return null;
        // }else {
        //这里走了十次，即只要抢购的十次 不生成订单 是true,其他都是false
        // boolean update2 = seckillGoodsService.
        //         update(new UpdateWrapper<SeckillGoods>()
        //                 .setSql("seckill_price=seckill_price+1")
        //                 .eq("goods_id", goodsVo.getId()));
        // return null;
        // }


        // -----在尝试创建订单前检查是否已存在------
        //下面的代码是为了解决，当正常秒杀后，删除Redis中的订单相关的数据造成的
        // RabbitMQ重试次数用光后-重新排队 死循环问题
        //但是会出现库存减了但是，没有生成订单的问题，所以将其注销了
        // SeckillOrder existingOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>()
        //         .eq("user_id", user.getId())
        //         .eq("goods_id", goodsVo.getId()));
        // if (existingOrder != null) {
        //     return null;  // 如果订单已存在，直接返回或处理逻辑
        // }



        //生成普通订单
        // Order order = new Order();
        order = new Order();

        // order.setId();//自动生成的,不需要填写
        order.setUserId(user.getId());
        order.setGoodsId(goodsVo.getId());
        order.setDeliveryAddrId(0L);//交货地址 ID 暂时先设置一个初始值即可
        order.setGoodsName(goodsVo.getGoodsName());
        order.setGoodsCount(1);//购买的商品数量
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);//订单渠道设置一个初始值 pc
        order.setStatus(0);//订单状态设置一个初始值 新建未支付
        order.setCreateDate(new Date()); //订单生成时间,设置为当前时间 now
        // order.setPayDate(new Date());// 订单支付时间不确定 先不设置

        //保存order
        orderMapper.insert(order); // 插入普通订单



        //orderMapper.insert(order);  // 插入普通订单
        // seckillOrderService.save(seckillOrder);  // 插入秒杀订单
        //插入订单操作这两行代码直接涉及到数据库的插入操作，
        // 是最有可能导致自增 ID 迅速增加的地方。在高并发情况下，
        // 即使前面的库存检查通过，这些插入操作可能因为其他业务逻辑或数据库约束失败
        // （例如，用户重复购买，但由于某些原因未能在前面的逻辑中被正确拦截）。

        //重复插入尝试：并发环境下，多个进程或线程可能同时尝试插入相同的订单信息，
        // 尽管由于业务逻辑（如库存不足）或数据库约束（如唯一性约束）最终这些插入可能失败，
        // 但每次尝试都可能增加自增 ID。

        //生成秒杀商品订单
        SeckillOrder seckillOrder = new SeckillOrder();
        // seckillOrder.setId(0L); //自动生成的,不需要填写
        seckillOrder.setUserId(user.getId());
        seckillOrder.setOrderId(order.getId());//这里填写的是,普通订单order自动生成的id
        seckillOrder.setGoodsId(goodsVo.getId());

        //保存seckillOrder
        seckillOrderService.save(seckillOrder); // 插入秒杀订单

        // 将生成的秒杀订单，存入到Redis,这样在查询某个用户是否已经秒杀了这个商品时
        // ,直接到Redis中查询,起到优化效果
        // 设计秒杀订单的key => order:用户Id:商品Id
        redisTemplate.opsForValue()
                .set("order:" + user.getId() + ":" + goodsVo.getId(), seckillOrder);

        //返回普通订单信息
        return order;
    }

    //获取秒杀结果
    //前端每隔50ms轮询调用一次该方法,查询秒杀后的结果
    @Override
    public Long getSeckillResult(User user, Long goodsId) {

        //如果秒杀成功，返回订单id
        //秒杀成功后会生成对应的订单，可以到mysql/Redis中查询是否生成了对应的订单
        //以此来判断是否秒杀成功,这里到redis中查询
        //redis中保存的秒杀订单对应的 key是=>
        // "order:" + user.getId() + ":" + goodsVo.getId()
        //得到秒杀商品订单信息seckillOrder
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue()
                .get("order:" + user.getId() + ":" + goodsId);

        // ArrayList<String> list = new ArrayList<>();
        // list.add("order:" + user.getId() + ":" + goodsId);

        if (seckillOrder != null) {//秒杀成功
            //返回秒杀订单的id
            return seckillOrder.getId(); //秒杀订单的id一定是大于0的,前端可以根据这个条件判断秒杀成功
            // } else if ("0".equals(redisTemplate.opsForValue().get("seckillFail:" + user.getId() + ":" + goodsId))) {//秒杀失败
            //seckillFail:userId:goodsId
            //如果在redis中查询到 对应的.get("seckillFail:userId:goodsId") 为0 而不是nil
            //说明秒杀失败的信息已经被记录到Redis中了,即秒杀失败
            // } else if ("0".equals(redisTemplate.countExistingKeys(list))) {//秒杀失败
        } else if (redisTemplate.hasKey("seckillFail:" + user.getId() + ":" + goodsId)) {
            //redisTemplate.hasKey() 对应的redis命令就是EXISTS
            // 如果存在键 "seckillFail:" + user.getId() + ":" + goodsId，表示秒杀失败
            return -1L;
        } else {
            //秒杀排队中...
            //注意,当秒杀订单不存在同时redis中又没有秒杀失败的订单，比如redis被清空的情况下，
            //前端的轮询操作总是会返回0,然后进入死循环,会一直轮询
            return 0L;
        }

    }

    //方法: 生成秒杀路径/值【唯一的】 每个用户都有一个唯一的秒杀路径
    @Override
    public String createPath(User user, Long goodsId) {

        //给每个用户生成唯一的path
        String path = MD5Util.md5(UUIDUtil.uuid());

        //将随机生成的路径保存到Redis中,设置一个超时时间60秒,防止生成之后很久以后用户才来用
        //key的设计=>  seckillPath:userId:goodsId
        redisTemplate.opsForValue().set("seckillPath:" +
                user.getId() + ":" + goodsId, path, 60, TimeUnit.SECONDS);

        return path;
    }

    //方法: 对秒杀路径进行校验
    @Override
    public boolean checkPath(User user, Long goodsId, String path) {

        if (user == null || goodsId < 0 || !StringUtils.hasText(path)) {
            return false;
        }

        //从Redis取出该用户秒杀该商品的路径
        //存入生成的path时 key的设计=>  seckillPath:userId:goodsId
        String redisPath = (String)redisTemplate.opsForValue().get("seckillPath:" + user.getId() + ":" + goodsId);

        return path.equals(redisPath);
    }
}
