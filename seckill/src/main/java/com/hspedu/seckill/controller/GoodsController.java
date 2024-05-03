package com.hspedu.seckill.controller;

import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.GoodsService;
import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.vo.GoodsVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author yangda
 * @create 2024-04-21-20:38
 * @description:
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {

    //装配userService
    @Resource
    private UserService userService;

    //装配
    @Resource
    private GoodsService goodsService;

    //装配
    @Resource
    private RedisTemplate redisTemplate;

    //手动进行渲染需要的模板解析器-可以将thymeleaf模板页面中需要的变量进行替换
    @Resource
    private ThymeleafViewResolver thymeleafViewResolver;


    /**
     * 进入到商品列表页-到DB查询
     * 验证用户是否登录，如果登录过，则请求转发到商品列表页，否则返回登录页面
     * 注解 @CookieValue和 Model 在springbootweb时讲过
     * <p>
     * http://localhost:8080/goods/toList
     *
     */
    // @RequestMapping("/toList")
    // // public String toList(HttpSession session,
    // //                      @CookieValue("userTicket") String ticket,
    // //                      Model model
    // // public String toList(@CookieValue("userTicket") String ticket,
    // //                      Model model,
    // //                      HttpServletRequest request,
    // //                      HttpServletResponse response) {
    // // 获取浏览器传递的 cookie 值，进行参数解析，直接转成 User 对象，继续传递
    // // 通过WebMvcConfigurer来解析参数，得到User对象，简化操作
    // public String toList(Model model,User user) {
    //
    //     //没有session，即没有登录过
    //     // if (session == null){
    //     //     return "login";
    //     // }
    //
    //     //没有票据，即没有登录过
    //     // if (!StringUtils.hasText(ticket)) {
    //     //     return "login";
    //     // }
    //
    //     //获取session中保存的用户
    //     // User user = (User)session.getAttribute(ticket);
    //     //如果session中没有 该ticket对应的 用户，说明没有登录过
    //
    //     //根据Cookie中的userTicket到Redis中 去获取 保存的用户user
    //     // User user = userService.getUserByCookie(ticket, request, response);
    //
    //     if (user == null) {//用户没有成功登录
    //         return "login";
    //     }
    //
    //
    //     //登录过, 将user信息放入到model,携带到下一个模板使用
    //     model.addAttribute("user", user);
    //     //将商品列表信息，放入到model,携带到下一个模板使用
    //     model.addAttribute("goodsList", goodsService.findGoodsVo());
    //
    //     return "goodsList";
    // }
    //

    /**
     * 进入到商品列表页-使用Redis优化，到Redis查询,将商品列表页缓存到Redis
     * <p>
     * http://localhost:8080/goods/toList
     *
     * produces指定返回的字符串的数据格式为"text/html;charset=utf-8"，
     * sponseBody表示直接将数据写入到响应体中返回
     * 这两注解组合使用,可以直接将html模板页面以String格式 "text/html;charset=utf-8" 返回给前端
     */
    @RequestMapping(value = "/toList",produces = "text/html;charset=utf-8")
    @ResponseBody//不能少，否则会出问题
    public String toList(Model model, User user,
                         HttpServletRequest request,
                         HttpServletResponse response) {

        if (user == null) { //用户没有成功登录
            // 下面这两个返回语句在这里都变成了返回字符串到前端了
            // ，因为这里使用的注解的影响，使其没有被解析为thymeleaf模板的模板名称
            // return "login";
            // return "redirect:/login";

            //返回登录页面的 HTML 内容
            // 如果您想通过 AJAX 调用这些服务，
            // 并需要在用户未登录时加载登录页面，
            // 可以在服务器端渲染登录页面的 HTML 并返回它。
            // 例如，您可以使用 ThymeleafViewResolver 来获取登录页面的 HTML 字符串，
            // 然后返回这个字符串。这需要您确保能够在这种情况下正确处理 HTML 内容：
            WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale());
            String html = thymeleafViewResolver.getTemplateEngine().process("login", webContext);
            return html;
            // Local variable 'html' is redundant 局部变量“html”是多余的
        }

        //先到Redis获取页面-如果有，直接返回页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if (StringUtils.hasText(html)) {
            //如果html有内容就直接返回
            return html;
        }

        //如果html没有内容继续往下走

        //登录过, 将user信息放入到model,携带到下一个模板使用
        model.addAttribute("user", user);
        //将商品列表信息，放入到model,携带到下一个模板使用
        //这里会到DB获取商品列表需要的数据
        model.addAttribute("goodsList", goodsService.findGoodsVo());

        //如果从redis没有获取到html页面，就手动渲染页面，并存入到redis
        //model.asMap() 就是取出前面放入到model中的数据"user","goodsList", 进行渲染需要
        //这里是一个常规的用法,直接拿来使用即可,获取web的上下文,用model中的数据,渲染html模板
        WebContext webContext =
                new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());

        //process:处理方法
        //使用thymeleaf引擎处理一个模板页,模板页的名称 名为"goodsList"，
        //模板页中构建页面需要的数据/内容是从webContext中获取的
        //上面从redis中获取的html页面如果没有内容,就在这里进行赋值
        //"goodsList"的名称不可以乱写,是已经存在的,
        // 或者你需要对其进行手动渲染的thymeleaf模板的名称!!!
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext);
        if (StringUtils.hasText(html)) {
            //如果此时html模板有内容,就说明渲染成功了
            //将页面保存到redis,设置每60s更新一次,该页面60s失效,redis会清除该页面
            //因为也有可能会更新这个商品列表页面,对redis中的缓存页面进行更新
            valueOperations.set("goodsList", html, 60, TimeUnit.SECONDS);
        }

        return html;
    }


    // //方法: 进入到商品详情页面-根据goodsId-到DB查询
    // // User user 是通过我们自定义的参数解析器处理返回到下面的方法形参上的
    // //@PathVariable Long goodsId 路径变量，是用户点击详情时，携带过来的
    // @RequestMapping("/toDetail/{goodsId}")
    // public String toDetail(Model model,
    //                        User user,
    //                        @PathVariable Long goodsId) {
    //
    //     //判断user
    //     if (user == null) { //说明没有登录
    //         return "login";
    //     }
    //     //将user放入model
    //     model.addAttribute("user", user);
    //
    //     //通过goodsId获取指定的秒杀商品的信息
    //     GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
    //     //将查询到的goodsVo放入到model,携带给下一个模板页使用
    //     model.addAttribute("goods", goodsVo);
    //
    //     //说明:返回秒杀商品详情时，同时返回该商品的秒杀状态和秒杀的剩余时间
    //     //为了配合前端展示秒杀商品的状态 - 这里依然有一个业务设计
    //     //1. 变量 secKillStatus 秒杀状态 0:秒杀未开始, 1: 秒杀进行中, 2: 秒杀已经结束
    //     //2. 变量 remainSeconds 剩余秒数: >0: 表示还有多久开始秒杀: 0: 秒杀进行中, -1: 表示秒杀已经结束
    //
    //     //秒杀开始时间
    //     Date startDate = goodsVo.getStartDate();
    //     //秒杀结束时间
    //     Date endDate = goodsVo.getEndDate();
    //     //当前时间
    //     Date nowDate = new Date();
    //
    //     //秒杀状态
    //     int secKillStatus = 0;
    //     //剩余秒数
    //     int remainSeconds = 0;
    //
    //     //如果nowDate 在 startDate 之前，说明还没有开始秒杀
    //     if (nowDate.before(startDate)) {
    //         //秒杀状态 0:秒杀未开始 不需要修改
    //         //得到还有多少秒开始秒杀
    //         remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
    //     } else if (nowDate.after(endDate)) {
    //         //nowDate 在 endDate 之后, 表示秒杀已结束
    //         secKillStatus = 2;
    //         remainSeconds = -1;
    //     } else {
    //         //秒杀进行中
    //         secKillStatus = 1;
    //         remainSeconds = 0;
    //     }
    //
    //     //将 secKillStatus 和 remainSeconds 放入到model,携带给模板页使用
    //     model.addAttribute("secKillStatus", secKillStatus);
    //     model.addAttribute("remainSeconds", remainSeconds);
    //
    //     return "goodsDetail";
    // }



    //方法: 进入到商品详情页面-根据goodsId-使用Redis优化，到Redis查询
    // User user 是通过我们自定义的参数解析器处理返回到下面的方法形参上的
    //@PathVariable Long goodsId 路径变量，是用户点击详情时，携带过来的
    /*
     * produces指定返回的字符串的数据格式为"text/html;charset=utf-8"，
     * sponseBody表示直接将数据写入到响应体中返回
     * 这两注解组合使用,可以直接将html模板页面以String格式 "text/html;charset=utf-8" 返回给前端
     */
    @RequestMapping(value = "/toDetail/{goodsId}",produces = "text/html;charset=utf-8")
    @ResponseBody//不能少，否则会出问题
    public String toDetail(Model model,
                           User user,
                           @PathVariable Long goodsId,
                           HttpServletRequest request,
                           HttpServletResponse response) {

        //判断user
        if (user == null) { //说明没有登录
            // 下面这两个返回语句在这里都变成了返回字符串到前端了
            // ，因为这里使用的注解的影响，使其没有被解析为thymeleaf模板的模板名称
            // return "login";
            // return "redirect:/login";

            //返回登录页面的 HTML 内容
            // 如果您想通过 AJAX 调用这些服务，
            // 并需要在用户未登录时加载登录页面，
            // 可以在服务器端渲染登录页面的 HTML 并返回它。
            // 例如，您可以使用 ThymeleafViewResolver 来获取登录页面的 HTML 字符串，
            // 然后返回这个字符串。这需要您确保能够在这种情况下正确处理 HTML 内容：
            WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale());
            String html = thymeleafViewResolver.getTemplateEngine().process("login", webContext);
            return html;
            // Local variable 'html' is redundant 局部变量“html”是多余的
        }

        //先到Redis获取页面-如果有，直接返回页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
        if (StringUtils.hasText(html)) {
            //如果html有内容就直接返回
            return html;
        }


        //将user放入model
        model.addAttribute("user", user);

        //通过goodsId获取指定的秒杀商品的信息
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        //将查询到的goodsVo放入到model,携带给下一个模板页使用
        model.addAttribute("goods", goodsVo);

        //说明:返回秒杀商品详情时，同时返回该商品的秒杀状态和秒杀的剩余时间
        //为了配合前端展示秒杀商品的状态 - 这里依然有一个业务设计
        //1. 变量 secKillStatus 秒杀状态 0:秒杀未开始, 1: 秒杀进行中, 2: 秒杀已经结束
        //2. 变量 remainSeconds 剩余秒数: >0: 表示还有多久开始秒杀: 0: 秒杀进行中, -1: 表示秒杀已经结束

        //秒杀开始时间
        Date startDate = goodsVo.getStartDate();
        //秒杀结束时间
        Date endDate = goodsVo.getEndDate();
        //当前时间
        Date nowDate = new Date();

        //秒杀状态
        int secKillStatus = 0;
        //剩余秒数
        int remainSeconds = 0;

        //如果nowDate 在 startDate 之前，说明还没有开始秒杀
        if (nowDate.before(startDate)) {
            //秒杀状态 0:秒杀未开始 不需要修改
            //得到还有多少秒开始秒杀
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            //nowDate 在 endDate 之后, 表示秒杀已结束
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀进行中
            secKillStatus = 1;
            remainSeconds = 0;
        }

        //将 secKillStatus 和 remainSeconds 放入到model,携带给模板页使用
        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("remainSeconds", remainSeconds);


        //如果从redis没有获取到html页面，就手动渲染页面，并存入到redis
        //model.asMap() 就是取出前面放入到model中的数据"user","goodsList", 进行渲染需要
        //这里是一个常规的用法,直接拿来使用即可,获取web的上下文,用model中的数据,渲染html模板
        WebContext webContext =
                new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());

        //process:处理方法
        //使用thymeleaf引擎处理一个模板页,模板页的名称 名为"goodsDetail" ，
        //模板页中构建页面需要的数据/内容是从webContext中获取的
        //上面从redis中获取的html页面如果没有内容,就在这里进行赋值
        //"goodsDetail" 的名称不可以乱写,是已经存在的,
        // 或者你需要对其进行手动渲染的thymeleaf模板的名称!!!
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);
        if (StringUtils.hasText(html)) {
            //如果此时html模板有内容,就说明渲染成功了
            //将页面保存到redis,设置每60s更新一次,该页面60s失效,redis会清除该页面
            //因为也有可能会更新这个商品列表页面,对redis中的缓存页面进行更新
            valueOperations.set("goodsDetail:"+goodsId, html, 60, TimeUnit.SECONDS);
        }

        return html;
    }


}
