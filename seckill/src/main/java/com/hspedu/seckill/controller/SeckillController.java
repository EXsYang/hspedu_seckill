package com.hspedu.seckill.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hspedu.seckill.config.AccessLimit;
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
import com.ramostear.captcha.HappyCaptcha;
import com.ramostear.captcha.common.Fonts;
import com.ramostear.captcha.support.CaptchaStyle;
import com.ramostear.captcha.support.CaptchaType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    //装配RedisScript
    @Resource
    private RedisScript<Long> script;


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
    // @RequestMapping("/{path}/doSeckill")
    // @ResponseBody
    // public RespBean doSeckill(@PathVariable String path,
    //                           Model model, User user, Long goodsId) {
    //
    //     if (user == null) {//用户没有登录
    //         return RespBean.error(RespBeanEnum.SESSION_ERROR);
    //     }
    //
    //     //这里增加一个判断逻辑,校验用户携带的路径是否正确
    //     boolean checkPath = orderService.checkPath(user, goodsId, path);
    //     if (!checkPath) {//校验失败
    //         return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
    //     }
    //
    //
    //     //获取到goodsVo
    //     //- 对DB进行了一次操作
    //     GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
    //
    //     //判断库存
    //     if (goodsVo.getStockCount() < 1) {//没有库存了
    //         return RespBean.error(RespBeanEnum.ENTRY_STOCK);//返回错误信息
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
    //         return RespBean.error(RespBeanEnum.REPEAT_ERROR);//返回错误信息
    //     }
    //
    //
    //     //对map进行判断[内存标记],如果商品在map中已经标记为true没有库存了，
    //     //则直接返回,无需进行Redis预减库存
    //     if (entryStockMap.get(goodsId)) {
    //         return RespBean.error(RespBeanEnum.ENTRY_STOCK);//返回错误信息
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
    //
    //         return RespBean.error(RespBeanEnum.ENTRY_STOCK);//返回错误信息
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
    //     //这里需要返回一个RespBeanEnum的状态码，需要使用error方法才行，
    //     //success不支持,因为success方法里的状态码和状态信息,总是200和"SUCCESS"
    //     return RespBean.error(RespBeanEnum.SEC_KILL_WAIT);
    //
    // }




    // //方法 处理用户抢购/秒杀请求
    // //说明: V7.0版本, 是在V5.0的基础上修改的 加入Redis分布式锁
    @RequestMapping("/doSeckill")
    public String doSeckill(Model model, User user, Long goodsId) {

        // System.out.println("---------秒杀V3.0--------------");
        // System.out.println("目前已有 " + OrderServiceImpl.failureCount + " 次更新失败");
        //failureCount 计数器显示不正确：在显示计数器值时，应确保使用 failureCount.get() 来获取当前值。如果直接通过 failureCount 引用，它不会显示正确的数值。
        // System.out.println("目前已有 " + OrderServiceImpl.failureCount.get() + " 次更新失败");
        // System.out.println("目前已有 " + OrderServiceImpl.count + " 次更新成功");

        if (user == null) {//用户没有登录
            return "login";
        }

        //将user放入到model，下一个模板页可以使用
        model.addAttribute("user", user);

        //获取到goodsVo
        //- 对DB进行了一次操作
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);

        //判断库存
        if (goodsVo.getStockCount() < 1) {//没有库存了
            model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
            return "secKillFail";//错误页面
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
            model.addAttribute("errmsg", RespBeanEnum.REPEAT_ERROR.getMessage());
            return "secKillFail";//错误页面
        }


        //对map进行判断[内存标记],如果商品在map中已经标记为true没有库存了，
        //则直接返回,无需进行Redis预减库存
        if (entryStockMap.get(goodsId)) {
            model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
            return "secKillFail";//错误页面
        }


        //-----------------增加到Redis预减库存的逻辑start------------------------
        //库存预减,如果在Redis中预减库存,发现秒杀商品已经没有了,就直接返回
        //从而减少去执行 orderService.seckill(user, goodsVo); 真正的秒杀方法的请求的数量,
        //防止线程堆积,优化秒杀/高并发
        //Redis的decrement方法是具有原子性的！！！原子性可以保障我们优化的效果
        //即即使有200个请求冲到下面这行代码,也是一个一个的执行减1操作
        //这里返回的结果decrement是减1之后的值
        // Long decrement = redisTemplate.opsForValue().decrement("seckillGoods:" + goodsId);
        // if (decrement < 0) {//说明当前秒杀的商品已经没有库存了
        //     //[内存标记]
        //     //这里说明当前秒杀的商品已经没有库存了
        //     entryStockMap.put(goodsId, true);
        //
        //     //这里会判断小于0之后返回错误页面,但在这个判断之前一直在往下减,
        //     // Redis中的库存数量会变为负数(数据库中不会变为负数,最多减到0),因为冲过来的
        //     //这些请求虽然不再进入orderService.seckill(user, goodsVo);方法了,但是
        //     //还是需要走到这个判断中来并返回到错误页面的
        //     //为了好看,在将库存数量恢复成0
        //     redisTemplate.opsForValue().increment("seckillGoods:" + goodsId);
        //     model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
        //     return "secKillFail";//错误页面
        // }
        //-----------------增加到Redis预减库存的逻辑end------------------------

        //=====使用Redis分布式锁====
        //老师说明
        //1. 对于当前项目而言，使用redisTemplate.opsForValue().decrement() 就可以控制抢购,因为该方法具有原子性和隔离性
        //2. 考虑到如果有比较多的操作，需要保证隔离性，也就是说，不是简单的-1,而是有多个操作
        //   这样就需要扩大隔离性的范围，部分操作还需要原子性, 我们给小伙伴演示一下Redis分布式锁的使用
        //3. 我们看看以前是如何使用Redis分布式锁的

        //1 获取锁，setnx
        //得到一个 uuid 值，作为锁的值
        String uuid = UUID.randomUUID().toString();

        //获取锁 这里是指向同一个Redis的
        Boolean lock =
                redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        //2 获取锁成功
        if (lock) {

            //准备删除锁脚本-使用 lua 脚本来释放锁, 控制删除原子性 【这里注销掉是因为将Lua脚本放到了lock.lua文件中,
            // 并将操作Lua脚本执行的DefaultRedisScript对象注入到ioc容器中了,提高通用性和可扩展性】
            //String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            //DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            //redisScript.setScriptText(script);
            //redisScript.setResultType(Long.class);

            //写自己的业务-就可以有多个操作了
            Long decrement = redisTemplate.opsForValue().decrement("seckillGoods:" + goodsId);
            if (decrement < 0) {//说明这个商品已经没有库存
                //说明当前秒杀的商品，已经没有库存
                entryStockMap.put(goodsId, true);
                //恢复库存为0
                redisTemplate.opsForValue().increment("seckillGoods:" + goodsId);

                //分布式锁可以保证隔离性,即到释放锁之前的 多个操作 是一个请求一个请求执行的,
                //只有拿到Redis分布式锁的请求才可以执行上面加锁的这段代码

                //使用 Lua脚本 释放分布式锁.-lua为什么使用redis+lua脚本释放锁前面讲过
                // 第一个参数是 script 脚本 ，第二个需要判断的 key，第三个就是 key 所对应的值
                // 老 韩 解 读 Arrays.asList("lock") 会 传 递 给 script 的 KEYS[1] , uuid 会 传 递 给 ARGV[1] , 其它的小伙伴应该很容易理解
                redisTemplate.execute(script, Arrays.asList("lock"), uuid);

                model.addAttribute("errmsg", RespBeanEnum.ENTRY_STOCK.getMessage());
                return "secKillFail";//错误页面
            }

            //说明这个商品有库存可以进行抢购,也需要将这个分布式锁释放掉,因为后面的请求还在等着拿到这把锁继续抢呢
            //在秒杀逻辑中，无论最终的操作结果如何，只要你之前成功获取了锁，最后都应该释放这个锁。
            // 这包括decrement >= 0的情况，即使这表示还有库存可以继续进行秒杀操作。
            /**
             * 这里是为什么需要在decrement >= 0时释放锁的几个主要原因：
             *
             * 1. 维护锁的生命周期和完整性
             * 获取锁的目的是为了在操作共享资源（如库存数量）时保护这些资源不受并发操作的影响。当一个线程（或请求）完成其操作后，无论其结果如何，都应该释放锁，以便其他线程（或请求）可以获取锁来进行自己的操作。这是使用锁的基本协议。
             *
             * 2. 防止死锁和资源阻塞
             * 如果不释放锁，那么其他等待这个锁的线程将无法继续执行，可能导致系统效率低下，甚至出现死锁的情况。释放锁是确保资源可以被适时地、公平地访问的关键。
             *
             * 3. 保证数据的一致性和完整性
             * 在decrement >= 0时释放锁，意味着即使当前线程的操作表明还有库存，但此操作已完成对共享资源的所有修改。保持锁直到操作完全结束，可以确保这一过程的原子性和一致性，避免在操作过程中发生数据竞争或状态不一致的情况。
             *
             * 4. 提高系统的响应性和吞吐量
             * 及时释放锁可以让其他正在等待的操作尽快执行，从而提高系统的响应性和并发处理能力。这对于秒杀这种高并发场景尤为重要，因为延迟释放锁可能导致大量用户的请求被不必要地阻塞。
             */
            //使用 Lua脚本script 释放分布式锁
            // 第一个参数是 script 脚本 ，第二个需要判断的 key，第三个就是 key 所对应的值
            // 老 韩 解 读 Arrays.asList("lock") 会 传 递 给 script 的 KEYS[1] , uuid 会 传 递 给 ARGV[1] , 其它的小伙伴应该很容易理解
            redisTemplate.execute(script, Arrays.asList("lock"), uuid);

        } else {
            //3 获取锁失败,返回个信息[本次抢购失败，请再次抢购...]
            model.addAttribute("errmsg", RespBeanEnum.SEC_KILL_RETRY.getMessage());
            return "secKillFail";//错误页面
        }



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

        model.addAttribute("errmsg", "排队中...");
        return "secKillFail";


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
    /**
     * 解读:@AccessLimit(second = 5,maxCount = 5,needLogin = true)
     * 1. 使用自定义注解的方式完成对用户的限流防刷-通用性和灵活性提高了
     * 2. second = 5,maxCount = 5 说明是在5秒内可以访问的最大次数是5次
     * 3. needLogin = true 表示用户是否需要登录才能访问该接口
     */
    @AccessLimit(second = 5,maxCount = 5,needLogin = true)
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request) {
        //常规校验
        if (user == null || goodsId < 0 || !StringUtils.hasText(captcha)) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        //增加业务逻辑: 加入Redis计数器, 完成对用户的限流防刷
        //比如: 在5s内访问的次数超过了五次, 我们就认为是刷接口
        //这里先把代码写在方法中, 后面使用自定义注解提高使用的通用性
        //uri 就是 localhost:8080/seckill/path 中的 '/seckill/path'
        // String uri = request.getRequestURI();
        // ValueOperations valueOperations = redisTemplate.opsForValue();
        // //定义一个key => uri:userId
        // String key = uri + ":" + user.getId();
        // //得到5s内访问次数该"/seckill/path"方法的次数 count
        // Integer count = (Integer)valueOperations.get(key);
        // if (count == null){// 说明还没有这个key,说明是在5s内第一次访问该接口,就初始化,值为1,超时/过期时间为5s
        //     valueOperations.set(key,1,5,TimeUnit.SECONDS);
        // }else if (count < 5){ //说明是正常访问,将该key的值加1
        //     valueOperations.increment(key);
        // }else { //count >= 5  说明用户在刷接口
        //     return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
        // }


        //增加一个业务逻辑-校验用户输入的验证码是否正确
        boolean check = orderService.checkCaptcha(user, goodsId, captcha);
        if (!check) {
            //如果校验没有通过，直接返回错误信息
            return RespBean.error(RespBeanEnum.CAPTCHA_ERROR);
        }

        //如果用户输入的验证码,就创建秒杀路径,并保存到Redis中,并返回给前端
        String path = orderService.createPath(user, goodsId);

        //将创建好的该用户 唯一的 秒杀路径返回给前端
        return RespBean.success(path);
    }


    //生成验证码-happyCaptcha
    @RequestMapping("/captcha")
    public void happyCaptcha(HttpServletRequest request,
                             HttpServletResponse response,
                             User user, Long goodsId) {

        System.out.println("请求来了... goodsId=" + goodsId);
        //生成验证码并以流的方式输出给客户端了
        // <img src="/seckill/captcha?goodsId=1"/>
        // ,相当于直接输出到了img这个标签中并显示出来了

        //后端生成图像：
        // 后端接收到请求验证码的API调用。
        // 生成验证码图像并将其以二进制流的形式发送，同时设置合适的 Content-Type。
        // 前端显示图像：
        // <img> 标签的 src 属性设置为指向验证码生成的URL。
        // 浏览器请求该URL，接收响应并在 <img> 标签中显示图像。

        //注意该验证码,默认就保存到session中了，session中保存时 key默认是 happy-captcha
        HappyCaptcha.require(request, response)
                .style(CaptchaStyle.ANIM)               //设置展现样式为动画
                .type(CaptchaType.NUMBER)               //设置验证码内容为数字
                .length(6)                              //设置字符长度为 6
                .width(220)                             //设置动画宽度为 220
                .height(80)                             //设置动画高度为 80
                .font(Fonts.getInstance().zhFont())     //设置汉字的字体
                .build().finish();                      //生成并输出验证码

        //把验证码的值保存到Redis中[考虑分布式,解决session不能共享问题],设置了验证码的失效时间100s
        //key => captcha:userId:goodsId
        redisTemplate.opsForValue().set("captcha:" + user.getId() + ":"
                + goodsId, (String) request.getSession().getAttribute("happy-captcha"), 100, TimeUnit.SECONDS);


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
