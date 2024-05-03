package com.hspedu.seckill.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hspedu.seckill.pojo.Order;
import com.hspedu.seckill.pojo.SeckillMessage;
import com.hspedu.seckill.pojo.SeckillOrder;
import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.rabbitmq.MQSenderMessage;
import com.hspedu.seckill.service.GoodsService;
import com.hspedu.seckill.service.OrderService;
import com.hspedu.seckill.service.SeckillOrderService;
import com.hspedu.seckill.service.impl.OrderServiceImpl;
import com.hspedu.seckill.vo.GoodsVo;
import com.hspedu.seckill.vo.RespBean;
import com.hspedu.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * @author yangda
 * @create 2024-04-25-16:47
 * @description:
 */
@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

    //装配需要的组件/对象
    @Resource
    private GoodsService goodsService;

    @Resource
    private SeckillOrderService seckillOrderService;

    @Resource
    private OrderService orderService;

    @Resource
    private RedisTemplate redisTemplate;

    //定义map-记录秒杀商品是否还有库存[用于内存标记]
    private HashMap<Long, Boolean> entryStockMap = new HashMap<>();

    //装配消息的生产者/发送者
    @Resource
    private MQSenderMessage mqSenderMessage;


    // //方法 处理用户抢购/秒杀请求
    // //说明: 我们先完成一个V1.0版本, 后面在高并发的情况下,还要做优化
    // @RequestMapping("/doSeckill")
    // public String doSeckill(Model model, User user,Long goodsId){
    //
    //     System.out.println("---------秒杀V1.0--------------");
    //
    //     if (user == null){//用户没有登录
    //         return "login";
    //     }
    //
    //     //将user放入到model，下一个模板页可以使用
    //     model.addAttribute("user",user);
    //
    //     //获取到goodsVo
    //     GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
    //
    //     //判断库存
    //     if (goodsVo.getStockCount() < 1){//没有库存了
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //判断用户是否在复购
    //     /**
    //      * 使用连续两次 .eq 来查询用户是否复购
    //      * 在 MyBatis-Plus 中，可以使用连续两次 .eq 来构建查询条件，这是完全有效的。
    //      * 这种方式用于在查询中指定多个过滤条件。每次调用 .eq 方法都会向查询条件中
    //      * 添加一个新的等值条件（AND 关系）。这在您的案例中意味着查询将会返回所有
    //      * 同时满足 user_id 和 goods_id 条件的记录。
    //      */
    //     SeckillOrder seckillOrder = seckillOrderService.getOne(
    //                                     new QueryWrapper<SeckillOrder>()
    //                                             .eq("user_id", user.getId())
    //                                             .eq("goods_id", goodsId));
    //
    //     if (seckillOrder != null){//说明买过了
    //         model.addAttribute("errmsg",RespBeanEnum.REPEAT_ERROR.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //抢购
    //     Order order = orderService.seckill(user, goodsVo);
    //     if (order == null){
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //秒杀成功-进入到订单详情页
    //     model.addAttribute("order",order);
    //     model.addAttribute("goods",goodsVo);
    //
    //     System.out.println("---------秒杀V1.0--------------");
    //
    //     // 进入到订单详情页
    //     return "orderDetail";
    // }

    // //方法 处理用户抢购/秒杀请求
    // //说明: V2.0版本, 高并发优化 到Redis判断复购
    // @RequestMapping("/doSeckill")
    // public String doSeckill(Model model, User user, Long goodsId) {
    //
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount + " 次更新失败");
    //     //failureCount 计数器显示不正确：在显示计数器值时，应确保使用 failureCount.get() 来获取当前值。如果直接通过 failureCount 引用，它不会显示正确的数值。
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount.get() + " 次更新失败");
    //     // System.out.println("目前已有 " + OrderServiceImpl.count + " 次更新成功");
    //
    //     if (user == null) {//用户没有登录
    //         return "login";
    //     }
    //
    //     //将user放入到model，下一个模板页可以使用
    //     model.addAttribute("user", user);
    //
    //     //获取到goodsVo
    //     //- 对DB进行了一次操作
    //     GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
    //
    //     //判断库存
    //     if (goodsVo.getStockCount() < 1) {//没有库存了
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //判断用户是否在复购-这里是到DB中查询
    //     // 判断当前购买用户id和购买商品id是否已经在商品秒杀表中存在了
    //     /**
    //      * 使用连续两次 .eq 来查询用户是否复购
    //      * 在 MyBatis-Plus 中，可以使用连续两次 .eq 来构建查询条件，这是完全有效的。
    //      * 这种方式用于在查询中指定多个过滤条件。每次调用 .eq 方法都会向查询条件中
    //      * 添加一个新的等值条件（AND 关系）。这在您的案例中意味着查询将会返回所有
    //      * 同时满足 user_id 和 goods_id 条件的记录。
    //      */
    //     // SeckillOrder seckillOrder = seckillOrderService.getOne(
    //     //         new QueryWrapper<SeckillOrder>()
    //     //                 .eq("user_id", user.getId())
    //     //                 .eq("goods_id", goodsId));
    //     //
    //     // if (seckillOrder != null){//说明买过了
    //     //     model.addAttribute("errmsg",RespBeanEnum.REPEAT_ERROR.getMessage());
    //     //     return "secKillFail";//错误页面
    //     // }
    //
    //     //判断用户是否在复购-这里直接到Redis中,获取对应的秒杀订单,如果有,则说明已经抢购过了
    //     //- 对Redis进行了一次操作
    //     SeckillOrder o =
    //             (SeckillOrder) redisTemplate.opsForValue()
    //                     .get("order:" + user.getId() + ":" + goodsVo.getId());
    //     if (o != null) {//说明该用户已经抢购了该商品
    //         model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //
    //     //抢购 - 进入真正的秒杀方法
    //     Order order = orderService.seckill(user, goodsVo);
    //
    //     if (order == null) {
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //秒杀成功-进入到订单详情页
    //     model.addAttribute("order", order);
    //     model.addAttribute("goods", goodsVo);
    //
    //     // 进入到订单详情页
    //     return "orderDetail";
    // }


    // //方法 处理用户抢购/秒杀请求
    // //说明: V3.0版本, 高并发优化 Redis库存预减
    // @RequestMapping("/doSeckill")
    // public String doSeckill(Model model, User user, Long goodsId) {
    //
    //     // System.out.println("---------秒杀V3.0--------------");
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount + " 次更新失败");
    //     //failureCount 计数器显示不正确：在显示计数器值时，应确保使用 failureCount.get() 来获取当前值。如果直接通过 failureCount 引用，它不会显示正确的数值。
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount.get() + " 次更新失败");
    //     // System.out.println("目前已有 " + OrderServiceImpl.count + " 次更新成功");
    //
    //     if (user == null) {//用户没有登录
    //         return "login";
    //     }
    //
    //     //将user放入到model，下一个模板页可以使用
    //     model.addAttribute("user", user);
    //
    //     //获取到goodsVo
    //     //- 对DB进行了一次操作
    //     GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
    //
    //     //判断库存
    //     if (goodsVo.getStockCount() < 1) {//没有库存了
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //判断用户是否在复购-这里是到DB中查询
    //     // 判断当前购买用户id和购买商品id是否已经在商品秒杀表中存在了
    //     /**
    //      * 使用连续两次 .eq 来查询用户是否复购
    //      * 在 MyBatis-Plus 中，可以使用连续两次 .eq 来构建查询条件，这是完全有效的。
    //      * 这种方式用于在查询中指定多个过滤条件。每次调用 .eq 方法都会向查询条件中
    //      * 添加一个新的等值条件（AND 关系）。这在您的案例中意味着查询将会返回所有
    //      * 同时满足 user_id 和 goods_id 条件的记录。
    //      */
    //     // SeckillOrder seckillOrder = seckillOrderService.getOne(
    //     //         new QueryWrapper<SeckillOrder>()
    //     //                 .eq("user_id", user.getId())
    //     //                 .eq("goods_id", goodsId));
    //     //
    //     // if (seckillOrder != null){//说明买过了
    //     //     model.addAttribute("errmsg",RespBeanEnum.REPEAT_ERROR.getMessage());
    //     //     return "secKillFail";//错误页面
    //     // }
    //
    //     //判断用户是否在复购-这里直接到Redis中,获取对应的秒杀订单,如果有,则说明已经抢购过了
    //     //- 对Redis进行了一次操作
    //     SeckillOrder o =
    //             (SeckillOrder) redisTemplate.opsForValue()
    //                     .get("order:" + user.getId() + ":" + goodsVo.getId());
    //     if (o != null) {//说明该用户已经抢购了该商品
    //         model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //-----------------增加到Redis预减库存的逻辑start------------------------
    //     //库存预减,如果在Redis中预减库存,发现秒杀商品已经没有了,就直接返回
    //     //从而减少去执行 orderService.seckill(user, goodsVo); 真正的秒杀方法的请求的数量,
    //     //防止线程堆积,优化秒杀/高并发
    //     //Redis的decrement方法是具有原子性的！！！原子性可以保障我们优化的效果
    //     //即即使有200个请求冲到下面这行代码,也是一个一个的执行减1操作
    //     //这里返回的结果decrement是减1之后的值
    //     Long decrement = redisTemplate.opsForValue().decrement("seckillGoods:" + goodsId);
    //     if (decrement < 0){//说明当前秒杀的商品已经没有库存了
    //         //这里会判断小于0之后返回错误页面,但在这个判断之前一直在往下减,
    //         // Redis中的库存数量会变为负数(数据库中不会变为负数,最多减到0),因为冲过来的
    //         //这些请求虽然不再进入orderService.seckill(user, goodsVo);方法了,但是
    //         //还是需要走到这个判断中来并返回到错误页面的
    //         //为了好看,在将库存数量恢复成0
    //         redisTemplate.opsForValue().increment("seckillGoods:" + goodsId);
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //     //-----------------增加到Redis预减库存的逻辑end------------------------
    //
    //     // System.out.println("hello~~ 当前请求可以进入到真正的秒杀方法");
    //
    //     //抢购 - 进入真正的秒杀方法
    //     Order order = orderService.seckill(user, goodsVo);
    //
    //     if (order == null) {
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //秒杀成功-进入到订单详情页
    //     model.addAttribute("order", order);
    //     model.addAttribute("goods", goodsVo);
    //
    //     // System.out.println("---------秒杀V3.0--------------");
    //
    //     // 进入到订单详情页
    //     return "orderDetail";
    // }


    // //方法 处理用户抢购/秒杀请求
    // //说明: V4.0版本, 高并发优化 加入内存标记优化秒杀
    // //内存标记的设计思路
    // // 1.在本机jvm的map记录所有秒杀商品是否还有库存
    // // 2.在执行预减库存时，先到map去查询是否该秒杀商品有库存，
    // // 如果没有库存，则直接返回，如果有库存，则继续到Redis预减库存
    // // 3.操作本机jvm内存，快于操作Redis
    // @RequestMapping("/doSeckill")
    // public String doSeckill(Model model, User user, Long goodsId) {
    //
    //     // System.out.println("---------秒杀V3.0--------------");
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount + " 次更新失败");
    //     //failureCount 计数器显示不正确：在显示计数器值时，应确保使用 failureCount.get() 来获取当前值。如果直接通过 failureCount 引用，它不会显示正确的数值。
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount.get() + " 次更新失败");
    //     // System.out.println("目前已有 " + OrderServiceImpl.count + " 次更新成功");
    //
    //     if (user == null) {//用户没有登录
    //         return "login";
    //     }
    //
    //     //将user放入到model，下一个模板页可以使用
    //     model.addAttribute("user", user);
    //
    //     //获取到goodsVo
    //     //- 对DB进行了一次操作
    //     GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
    //
    //     //判断库存
    //     if (goodsVo.getStockCount() < 1) {//没有库存了
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //判断用户是否在复购-这里是到DB中查询
    //     // 判断当前购买用户id和购买商品id是否已经在商品秒杀表中存在了
    //     /**
    //      * 使用连续两次 .eq 来查询用户是否复购
    //      * 在 MyBatis-Plus 中，可以使用连续两次 .eq 来构建查询条件，这是完全有效的。
    //      * 这种方式用于在查询中指定多个过滤条件。每次调用 .eq 方法都会向查询条件中
    //      * 添加一个新的等值条件（AND 关系）。这在您的案例中意味着查询将会返回所有
    //      * 同时满足 user_id 和 goods_id 条件的记录。
    //      */
    //     // SeckillOrder seckillOrder = seckillOrderService.getOne(
    //     //         new QueryWrapper<SeckillOrder>()
    //     //                 .eq("user_id", user.getId())
    //     //                 .eq("goods_id", goodsId));
    //     //
    //     // if (seckillOrder != null){//说明买过了
    //     //     model.addAttribute("errmsg",RespBeanEnum.REPEAT_ERROR.getMessage());
    //     //     return "secKillFail";//错误页面
    //     // }
    //
    //     //判断用户是否在复购-这里直接到Redis中,获取对应的秒杀订单,如果有,则说明已经抢购过了
    //     //- 对Redis进行了一次操作
    //     SeckillOrder o =
    //             (SeckillOrder) redisTemplate.opsForValue()
    //                     .get("order:" + user.getId() + ":" + goodsVo.getId());
    //     if (o != null) {//说明该用户已经抢购了该商品
    //         model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //
    //     //对map进行判断[内存标记],如果商品在map中已经标记为true没有库存了，
    //     //则直接返回,无需进行Redis预减库存
    //     if (entryStockMap.get(goodsId)){
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //
    //     //-----------------增加到Redis预减库存的逻辑start------------------------
    //     //库存预减,如果在Redis中预减库存,发现秒杀商品已经没有了,就直接返回
    //     //从而减少去执行 orderService.seckill(user, goodsVo); 真正的秒杀方法的请求的数量,
    //     //防止线程堆积,优化秒杀/高并发
    //     //Redis的decrement方法是具有原子性的！！！原子性可以保障我们优化的效果
    //     //即即使有200个请求冲到下面这行代码,也是一个一个的执行减1操作
    //     //这里返回的结果decrement是减1之后的值
    //     Long decrement = redisTemplate.opsForValue().decrement("seckillGoods:" + goodsId);
    //     if (decrement < 0) {//说明当前秒杀的商品已经没有库存了
    //
    //         //[内存标记]
    //         //这里说明当前秒杀的商品已经没有库存了
    //         entryStockMap.put(goodsId,true);
    //
    //
    //         //这里会判断小于0之后返回错误页面,但在这个判断之前一直在往下减,
    //         // Redis中的库存数量会变为负数(数据库中不会变为负数,最多减到0),因为冲过来的
    //         //这些请求虽然不再进入orderService.seckill(user, goodsVo);方法了,但是
    //         //还是需要走到这个判断中来并返回到错误页面的
    //         //为了好看,在将库存数量恢复成0
    //         redisTemplate.opsForValue().increment("seckillGoods:" + goodsId);
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //     //-----------------增加到Redis预减库存的逻辑end------------------------
    //
    //     // System.out.println("hello~~ 当前请求可以进入到真正的秒杀方法");
    //
    //     //抢购 - 进入真正的秒杀方法
    //     Order order = orderService.seckill(user, goodsVo);
    //
    //     if (order == null) {
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //秒杀成功-进入到订单详情页
    //     model.addAttribute("order", order);
    //     model.addAttribute("goods", goodsVo);
    //
    //     // System.out.println("---------秒杀V3.0--------------");
    //
    //     // 进入到订单详情页
    //     return "orderDetail";
    // }


    // //方法 处理用户抢购/秒杀请求
    // //说明: V5.0版本, 高并发优化 加入 RabbitMQ消息队列,实现秒杀的异步请求
    // @RequestMapping("/doSeckill")
    // public String doSeckill(Model model, User user, Long goodsId) {
    //
    //     // System.out.println("---------秒杀V3.0--------------");
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount + " 次更新失败");
    //     //failureCount 计数器显示不正确：在显示计数器值时，应确保使用 failureCount.get() 来获取当前值。如果直接通过 failureCount 引用，它不会显示正确的数值。
    //     // System.out.println("目前已有 " + OrderServiceImpl.failureCount.get() + " 次更新失败");
    //     // System.out.println("目前已有 " + OrderServiceImpl.count + " 次更新成功");
    //
    //     if (user == null) {//用户没有登录
    //         return "login";
    //     }
    //
    //     //将user放入到model，下一个模板页可以使用
    //     model.addAttribute("user", user);
    //
    //     //获取到goodsVo
    //     //- 对DB进行了一次操作
    //     GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
    //
    //     //判断库存
    //     if (goodsVo.getStockCount() < 1) {//没有库存了
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //     //判断用户是否在复购-这里是到DB中查询
    //     // 判断当前购买用户id和购买商品id是否已经在商品秒杀表中存在了
    //     /**
    //      * 使用连续两次 .eq 来查询用户是否复购
    //      * 在 MyBatis-Plus 中，可以使用连续两次 .eq 来构建查询条件，这是完全有效的。
    //      * 这种方式用于在查询中指定多个过滤条件。每次调用 .eq 方法都会向查询条件中
    //      * 添加一个新的等值条件（AND 关系）。这在您的案例中意味着查询将会返回所有
    //      * 同时满足 user_id 和 goods_id 条件的记录。
    //      */
    //     // SeckillOrder seckillOrder = seckillOrderService.getOne(
    //     //         new QueryWrapper<SeckillOrder>()
    //     //                 .eq("user_id", user.getId())
    //     //                 .eq("goods_id", goodsId));
    //     //
    //     // if (seckillOrder != null){//说明买过了
    //     //     model.addAttribute("errmsg",RespBeanEnum.REPEAT_ERROR.getMessage());
    //     //     return "secKillFail";//错误页面
    //     // }
    //
    //     //判断用户是否在复购-这里直接到Redis中,获取对应的秒杀订单,如果有,则说明已经抢购过了
    //     //- 对Redis进行了一次操作
    //     SeckillOrder o =
    //             (SeckillOrder) redisTemplate.opsForValue()
    //                     .get("order:" + user.getId() + ":" + goodsVo.getId());
    //     if (o != null) {//说明该用户已经抢购了该商品
    //         model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //
    //     //对map进行判断[内存标记],如果商品在map中已经标记为true没有库存了，
    //     //则直接返回,无需进行Redis预减库存
    //     if (entryStockMap.get(goodsId)) {
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //
    //
    //     //-----------------增加到Redis预减库存的逻辑start------------------------
    //     //库存预减,如果在Redis中预减库存,发现秒杀商品已经没有了,就直接返回
    //     //从而减少去执行 orderService.seckill(user, goodsVo); 真正的秒杀方法的请求的数量,
    //     //防止线程堆积,优化秒杀/高并发
    //     //Redis的decrement方法是具有原子性的！！！原子性可以保障我们优化的效果
    //     //即即使有200个请求冲到下面这行代码,也是一个一个的执行减1操作
    //     //这里返回的结果decrement是减1之后的值
    //     Long decrement = redisTemplate.opsForValue().decrement("seckillGoods:" + goodsId);
    //     if (decrement < 0) {//说明当前秒杀的商品已经没有库存了
    //
    //         //[内存标记]
    //         //这里说明当前秒杀的商品已经没有库存了
    //         entryStockMap.put(goodsId, true);
    //
    //
    //         //这里会判断小于0之后返回错误页面,但在这个判断之前一直在往下减,
    //         // Redis中的库存数量会变为负数(数据库中不会变为负数,最多减到0),因为冲过来的
    //         //这些请求虽然不再进入orderService.seckill(user, goodsVo);方法了,但是
    //         //还是需要走到这个判断中来并返回到错误页面的
    //         //为了好看,在将库存数量恢复成0
    //         redisTemplate.opsForValue().increment("seckillGoods:" + goodsId);
    //         model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //         return "secKillFail";//错误页面
    //     }
    //     //-----------------增加到Redis预减库存的逻辑end------------------------
    //
    //     // System.out.println("hello~~ 当前请求可以进入到真正的秒杀方法");
    //
    //
    //     //抢购 - 进入真正的秒杀方法
    //     // Order order = orderService.seckill(user, goodsVo);
    //     // if (order == null) {
    //     //     model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
    //     //     return "secKillFail";//错误页面
    //     // }
    //     // //秒杀成功-进入到订单详情页
    //     // model.addAttribute("order", order);
    //     // model.addAttribute("goods", goodsVo);
    //     // // System.out.println("---------秒杀V3.0--------------");
    //     // // 进入到订单详情页
    //     // return "orderDetail";
    //
    //     //====V5.0 抢购，改为向消息队列发送秒杀请求,实现了秒杀异步请求========
    //     // 这里我们发送秒杀消息后,立即快速返回结果【临时结果】
    //     // 比如："秒杀进行中..."、"秒杀排队中..."
    //     // 这样请求就不用在这里阻塞了,客户端也不需要一直等待
    //     // 秒杀请求执行完seckill()后才返回结果
    //     // 客户端可以通过轮询,获取到最终结果
    //
    //     //先创建一个SeckillMessage对象
    //     SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
    //     // 将seckillMessage转为String字符串,发送到消息队列
    //     mqSenderMessage.sendSeckillMessage(JSONUtil.toJsonStr(seckillMessage));
    //
    //     model.addAttribute("errmsg", "排队中...");
    //     return "secKillFail";
    //
    //
    // }


    //方法 处理用户抢购/秒杀请求
    //说明: V6.0版本, 高并发优化 加入 秒杀安全机制，并直接返回RespBean(该对象可以携带数据)
    //通过路径变量,保证每个用户携带唯一的一个路径变量
    @RequestMapping("/{path}/doSeckill")
    @ResponseBody
    public RespBean doSeckill(@PathVariable String path,
                            Model model, User user, Long goodsId) {

        if (user == null) {//用户没有登录
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        //这里增加一个判断逻辑,校验用户携带的路径是否正确
        boolean checkPath = orderService.checkPath(user, goodsId, path);
        if (!checkPath){//校验失败
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }


        //获取到goodsVo
        //- 对DB进行了一次操作
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);

        //判断库存
        if (goodsVo.getStockCount() < 1) {//没有库存了
            return RespBean.error(RespBeanEnum.ENTRY_STOCK);//返回错误信息
        }

        //判断用户是否在复购-这里是到DB中查询
        // 判断当前购买用户id和购买商品id是否已经在商品秒杀表中存在了
        /**
         * 使用连续两次 .eq 来查询用户是否复购
         * 在 MyBatis-Plus 中，可以使用连续两次 .eq 来构建查询条件，这是完全有效的。
         * 这种方式用于在查询中指定多个过滤条件。每次调用 .eq 方法都会向查询条件中
         * 添加一个新的等值条件（AND 关系）。这在您的案例中意味着查询将会返回所有
         * 同时满足 user_id 和 goods_id 条件的记录。
         */
        // SeckillOrder seckillOrder = seckillOrderService.getOne(
        //         new QueryWrapper<SeckillOrder>()
        //                 .eq("user_id", user.getId())
        //                 .eq("goods_id", goodsId));
        //
        // if (seckillOrder != null){//说明买过了
        //     model.addAttribute("errmsg",RespBeanEnum.REPEAT_ERROR.getMessage());
        //     return "secKillFail";//错误页面
        // }

        //判断用户是否在复购-这里直接到Redis中,获取对应的秒杀订单,如果有,则说明已经抢购过了
        //- 对Redis进行了一次操作
        SeckillOrder o =
                (SeckillOrder) redisTemplate.opsForValue()
                        .get("order:" + user.getId() + ":" + goodsVo.getId());
        if (o != null) {//说明该用户已经抢购了该商品
            return RespBean.error(RespBeanEnum.REPEAT_ERROR);//返回错误信息
        }


        //对map进行判断[内存标记],如果商品在map中已经标记为true没有库存了，
        //则直接返回,无需进行Redis预减库存
        if (entryStockMap.get(goodsId)) {
            return RespBean.error(RespBeanEnum.ENTRY_STOCK);//返回错误信息
        }


        //-----------------增加到Redis预减库存的逻辑start------------------------
        //库存预减,如果在Redis中预减库存,发现秒杀商品已经没有了,就直接返回
        //从而减少去执行 orderService.seckill(user, goodsVo); 真正的秒杀方法的请求的数量,
        //防止线程堆积,优化秒杀/高并发
        //Redis的decrement方法是具有原子性的！！！原子性可以保障我们优化的效果
        //即即使有200个请求冲到下面这行代码,也是一个一个的执行减1操作
        //这里返回的结果decrement是减1之后的值
        Long decrement = redisTemplate.opsForValue().decrement("seckillGoods:" + goodsId);
        if (decrement < 0) {//说明当前秒杀的商品已经没有库存了

            //[内存标记]
            //这里说明当前秒杀的商品已经没有库存了
            entryStockMap.put(goodsId, true);


            //这里会判断小于0之后返回错误页面,但在这个判断之前一直在往下减,
            // Redis中的库存数量会变为负数(数据库中不会变为负数,最多减到0),因为冲过来的
            //这些请求虽然不再进入orderService.seckill(user, goodsVo);方法了,但是
            //还是需要走到这个判断中来并返回到错误页面的
            //为了好看,在将库存数量恢复成0
            redisTemplate.opsForValue().increment("seckillGoods:" + goodsId);

            return RespBean.error(RespBeanEnum.ENTRY_STOCK);//返回错误信息
        }
        //-----------------增加到Redis预减库存的逻辑end------------------------

        // System.out.println("hello~~ 当前请求可以进入到真正的秒杀方法");


        //抢购 - 进入真正的秒杀方法
        // Order order = orderService.seckill(user, goodsVo);
        // if (order == null) {
        //     model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
        //     return "secKillFail";//错误页面
        // }
        // //秒杀成功-进入到订单详情页
        // model.addAttribute("order", order);
        // model.addAttribute("goods", goodsVo);
        // // System.out.println("---------秒杀V3.0--------------");
        // // 进入到订单详情页
        // return "orderDetail";

        //====V5.0 抢购，改为向消息队列发送秒杀请求,实现了秒杀异步请求========
        // 这里我们发送秒杀消息后,立即快速返回结果【临时结果】
        // 比如："秒杀进行中..."、"秒杀排队中..."
        // 这样请求就不用在这里阻塞了,客户端也不需要一直等待
        // 秒杀请求执行完seckill()后才返回结果
        // 客户端可以通过轮询,获取到最终结果

        //先创建一个SeckillMessage对象
        SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
        // 将seckillMessage转为String字符串,发送到消息队列
        mqSenderMessage.sendSeckillMessage(JSONUtil.toJsonStr(seckillMessage));

        //这里需要返回一个RespBeanEnum的状态码，需要使用error方法才行，
        //success不支持,因为success方法里的状态码和状态信息,总是200和"SUCCESS"
        return RespBean.error(RespBeanEnum.SEC_KILL_WAIT);

    }

    //轮询调用该接口，判断是否秒杀成功
    @RequestMapping("/seckillResult")
    @ResponseBody
    public RespBean getSeckillResult(User user, Long goodsId) {

        System.out.println("正在轮询秒杀结果...");

        Long seckillResult = orderService.getSeckillResult(user, goodsId);

        return RespBean.success(seckillResult);


    }


    //方法: 获取秒杀路径
    @RequestMapping("/path")
    @ResponseBody
    public RespBean getPath(User user, Long goodsId) {

        if (user == null || goodsId < 0){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        //创建秒杀路径
        String path = orderService.createPath(user, goodsId);

        //将创建好的该用户 唯一的 秒杀路径返回给前端
        return RespBean.success(path);
    }


    /*
     * 老师解读
     * 1. 我们根据原生Spring 实现的一个 InitializingBean 初始化方法是根据这个接口实现的
     * 2. 该InitializingBean接口有一个方法void afterPropertiesSet() throws Exception;
     * 3. afterPropertiesSet() 在Bean的 setter方法后执行,即就是我们原来的初始化方法

        bean 的生命周期
        ● 说明: bean 对象创建是由 JVM 完成的，然后执行如下方法
        1. 执行构造器
        2. 执行 set 相关方法-(即这里会对属性进行装配/设置,如装配DAO等)
        3. 调用 bean 的初始化的方法（需要配置）
        4. 使用 bean
        5. 当容器关闭时候，调用 bean 的销毁方法（需要配置）

        原生Spring的bean 的初始化方法的实现 底层也是用的是实现了InitializingBean接口
        如果配置了后置处理器则在调用初始化方法是的调用机制如下:
     *    调用 后置处理器 postProcessBeforeInitialization()
     *    调用 bean 的初始化方法（需要配置）
     *    调用 后置处理器 postProcessAfterInitialization()
     *
     * 4. 当一个Bean实现了这个接口后,就实现afterPropertiesSet() ，这个方法就是初始化方法
     *
     *  * HspSpringApplicationContext.java
     *  * , 在创建好 Bean 实例后，判断是否需要进行初始化 【老师心得: 容器中常
     *  * 用的一个方法是，根据该类是否实现了某个接口，来判断是否要执行某个业务逻辑, 这里
     *  * 其实就是 java 基础的 接口编程 实际运用 标记接口,一个方法都没有,价值就是根据是否实现了某个标记接口，
     *  来判断是否要加入某些逻辑,比如序列化时的接口 Serializable,该接口中就一个方法都没有
     *  ,Serializable标记接口给底层使用的
     *  public interface Serializable {
     * }
     *  】
     */
    // 该方法是在SeckillController类/对象(注入到ioc容器中的bean)的所有
    // 属性都初始化后(即bean生命周期中setter方法执行完后)
    // 自动执行的，前提是该类/当前所在的类实现了InitializingBean接口
    // 在这个afterPropertiesSet()初始化方法,我们就可以将所有秒杀商品的库存量,加载到Redis
    // 当这个方法执行时，秒杀请求还没有过来,即在秒杀请求之前,这个方法就已经执行了
    @Override
    public void afterPropertiesSet() throws Exception {

        //查询所有的秒杀商品
        List<GoodsVo> list = goodsService.findGoodsVo();
        //先判断是否为空
        if (CollectionUtils.isEmpty(list)) {
            //即没有秒杀商品,直接返回
            //，如果没有秒杀商品，则直接返回，这意味着没有做任何后续操作。
            // 具体来说，这里的 return 语句会导致 afterPropertiesSet 方法的执行提前结束，不会执行任何其余的逻辑。
            return;
        }

        //代表商品还有库存,遍历list,将秒杀商品的库存量,放入到Redis中
        list.forEach(goodsVo -> {
            //秒杀商品库存量对应的key:  seckillGoods:商品id
            redisTemplate.opsForValue().set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());

            //初始化map[内存标记]
            //如果goodsId为 false,表示还有库存
            //如果goodsId为 true,表示没有库存
            entryStockMap.put(goodsVo.getId(), false);
        });

    }
}
