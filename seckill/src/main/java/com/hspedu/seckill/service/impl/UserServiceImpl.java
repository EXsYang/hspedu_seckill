package com.hspedu.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hspedu.seckill.exception.GlobalException;
import com.hspedu.seckill.mapper.UserMapper;
import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.util.CookieUtil;
import com.hspedu.seckill.util.MD5Util;
import com.hspedu.seckill.util.UUIDUtil;
import com.hspedu.seckill.util.ValidatorUtil;
import com.hspedu.seckill.vo.LoginVo;
import com.hspedu.seckill.vo.RespBean;
import com.hspedu.seckill.vo.RespBeanEnum;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author yangda
 * @create 2024-04-19-16:41
 * @description: 1.传统方式:在实现类中直接进行 implements UserService
 * 2.在mybatis-plus中，我们开发Service实现类，需要继承 ServiceImpl
 * 3.我们观察看到 ServiceImpl类实现 IService接口
 * 4.UserService 接口它继承了IService接口
 * 5.这里UserServiceImpl 就可以认为是实现了UserService接口，这样UserServiceImpl
 * 就可以使用IService接口方法，也可以理解成可以使用UserService接口方法或父接口IService里的方法
 * 6.如果UserService接口中，声明了其他的方法/自定义方法，那么我们依然需要在UserServiceImpl类
 * 中进行实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    //装配Mapper
    @Resource
    private UserMapper userMapper;

    //装配RedisTemplate，操作Redis 注意RedisTemplate<K, V> 中可以指定泛型，如RedisTemplate<String,User>
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 用户登录校验
     * /login/doLogin
     * @param loginVo
     * @param request
     * @param response
     * @return
     */
    @Override
    public RespBean doLogin(LoginVo loginVo,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        //接收到mobile/id和password[midPass]
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();

        //判断手机号和密码是否为空
        // if (!StringUtils.hasText(mobile) || !StringUtils.hasText(password)){
        //     return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        // }

        //校验手机号码是否合格
        // if (!ValidatorUtil.isMobile(mobile)){
        //     return RespBean.error(RespBeanEnum.MOBILE_ERROR);
        // }

        //查询DB，看看用户是否存在
        User user = userMapper.selectById(mobile);
        // System.out.println("DB根据手机号mobile取回来的user->" + user);
        if (null == user) { //说明用户不存在
            //方式1: 在这里可以直接返回一个RespBean对象, 然后经由控制层再返回给前端
            // return RespBean.error(RespBeanEnum.LOGIN_ERROR);

            //方式2:
            // 抛出一个全局异常，交给全局异常处理器统一进行处理(由全局异常处理器返回数据给前端)
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }

        //如果用户存在则比较密码！
        //注意，我们从loginVo取出的密码是中间密码(即客户端经过一次加密加盐处理的密码)
        if (!MD5Util.midPassToDBPass(password, user.getSalt()).equals(user.getPassword())) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        //用户登录成功

        //给每个用户生成票据 ticket -是唯一的
        String ticket = UUIDUtil.uuid();

        //将登录成功的用户保存到Session
        // HttpSession session = request.getSession();
        // System.out.println("sid=" + session.getId());
        // sid=86b96f73-ce72-4cc0-a653-f116db905301 这里还是正常的session格式
        // ，之后会被Spring Session接管，
        // 原本应该在Cookie中的字段名从JSESSIONID变成了SESSION，
        // 同时其值位数也变多了形式如:
        // SESSION:"ODZiOTZmNzMtY2U3Mi00Y2MwLWE2NTMtZjExNmRiOTA1MzAx"
        // session.setAttribute(ticket,user);
        // request.getSession().setAttribute(ticket,user);

        //为了实现分布式Session，把登录用户存放到Redis
        // System.out.println("使用的redisTemplate的hashCode=" + redisTemplate.hashCode());


        // 定义一个带有类型参数的 RedisTemplate 实例，确保类型安全
        // String 类型用于键，User 类型用于值
        // RedisTemplate<String, User> redisTemplate = new RedisTemplate<>();

        // ... 在这里配置 redisTemplate 的序列化器和其他相关设置

        // 使用 opsForValue() 方法时，我们已经知道键和值的类型
        // 这样可以避免编译时类型检查的警告，并确保类型安全
        // redisTemplate.opsForValue().set("user:" + ticket, user);


        redisTemplate.opsForValue().set("user:" + ticket, user);


        //将登录成功用户的票据 ticket 保存到cookie
        CookieUtil.setCookie(request, response, "userTicket", ticket);

        return RespBean.success(ticket);
    }

    /**
     * 根据Cookie-ticket 到Redis中获取用户信息
     *
     * @param userTicket
     * @param request
     * @param response
     * @return
     */
    @Override
    public User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response) {

        //如果携带过来的userTicket 中没有值，返回一个null
        if (!StringUtils.hasText(userTicket)) {
            return null;
        }

        //根据缓存中的 userTicket 到Redis中获取对应的 User对象
        User user = (User) redisTemplate.opsForValue().get("user:"+userTicket);

        //如果用户不为null,就重新设置Cookie,刷新,这里根据业务需求调整
        if (user != null) {
            CookieUtil.setCookie(request,response,"userTicket",userTicket);
        }

        // Redis中没有该 userTicket 对应的 User对象，这里不需要写
        // ，如果Redis中的user为null，直接返回user即可
        // if (user == null) {
        //     return null;
        // }

        return user;

    }

    //更新用户的密码
    @Override
    public RespBean updatePassword(String userTicket,
                                   String password,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        User user = getUserByCookie(userTicket, request, response);
        if (user == null){
            //该用户不存在,抛出异常
            throw new GlobalException(RespBeanEnum.MOBILE_NOT_ERROR);
        }

        //设置新密码
        user.setPassword(MD5Util.inputPassToDBPass(password,user.getSalt()));

        int i = userMapper.updateById(user);
        if (i == 1){ //更新成功
            //删除该用户在redis中的数据/对象
            redisTemplate.delete("user:" + userTicket);
            return RespBean.success();
        }

        //密码更新失败
        return RespBean.error(RespBeanEnum.PASSWORD_UPDATE_FAIL);
    }


}
