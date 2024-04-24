package com.hspedu.seckill.controller;

import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.GoodsService;
import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.vo.GoodsVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

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

    /**
     * 验证用户是否登录，如果登录过，则请求转发到商品列表页，否则返回登录页面
     * 注解 @CookieValue和 Model 在springbootweb时讲过
     * <p>
     * http://localhost:8080/goods/toList
     *
     */
    @RequestMapping("/toList")
    // public String toList(HttpSession session,
    //                      @CookieValue("userTicket") String ticket,
    //                      Model model
    // public String toList(@CookieValue("userTicket") String ticket,
    //                      Model model,
    //                      HttpServletRequest request,
    //                      HttpServletResponse response) {
    // 获取浏览器传递的 cookie 值，进行参数解析，直接转成 User 对象，继续传递
    // 通过WebMvcConfigurer来解析参数，得到User对象，简化操作
    public String toList(Model model,User user) {

        //没有session，即没有登录过
        // if (session == null){
        //     return "login";
        // }

        //没有票据，即没有登录过
        // if (!StringUtils.hasText(ticket)) {
        //     return "login";
        // }

        //获取session中保存的用户
        // User user = (User)session.getAttribute(ticket);
        //如果session中没有 该ticket对应的 用户，说明没有登录过

        //根据Cookie中的userTicket到Redis中 去获取 保存的用户user
        // User user = userService.getUserByCookie(ticket, request, response);

        if (user == null) {
            return "login";
        }


        //登录过, 将user信息放入到model,携带到下一个模板使用
        model.addAttribute("user", user);
        //将商品列表信息，放入到model,携带到下一个模板使用
        model.addAttribute("goodsList", goodsService.findGoodsVo());

        return "goodsList";
    }



    //方法: 进入到商品详情页面-根据goodsId
    // User user 是通过我们自定义的参数解析器处理返回到下面的方法形参上的
    //@PathVariable Long goodsId 路径变量，是用户点击详情时，携带过来的
    @RequestMapping("/toDetail/{goodsId}")
    public String toDetail(Model model,
                           User user,
                           @PathVariable Long goodsId){

        //判断user
        if (user == null){ //说明没有登录
            return "login";
        }
        //将user放入model
        model.addAttribute("user",user);

        //通过goodsId获取指定的秒杀商品的信息
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        //将查询到的goodsVo放入到model,携带给下一个模板页使用
        model.addAttribute("goods",goodsVo);

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
        if (nowDate.before(startDate)){
            //秒杀状态 0:秒杀未开始 不需要修改
            //得到还有多少秒开始秒杀
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        }else if (nowDate.after(endDate)){
            //nowDate 在 endDate 之后, 表示秒杀已结束
            secKillStatus = 2;
            remainSeconds = -1;
        }else {
            //秒杀进行中
            secKillStatus = 1;
            remainSeconds = 0;
        }

        //将 secKillStatus 和 remainSeconds 放入到model,携带给模板页使用
        model.addAttribute("secKillStatus",secKillStatus);
        model.addAttribute("remainSeconds",remainSeconds);

        return "goodsDetail";
    }



}
