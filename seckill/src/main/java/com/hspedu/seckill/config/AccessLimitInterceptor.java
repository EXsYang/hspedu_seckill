package com.hspedu.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.util.CookieUtil;
import com.hspedu.seckill.vo.RespBean;
import com.hspedu.seckill.vo.RespBeanEnum;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * @author yangda
 * @create 2024-05-06-12:07
 * @description: AccessLimitInterceptor: 自定义拦截器
 *
 *
 *  //需要到WebConfig配置类中注册自定义拦截器，这样才能生效！！！
 *     @Override
 *     public void addInterceptors(InterceptorRegistry registry) {
 *         registry.addInterceptor(accessLimitInterceptor);
 *     }
 *
 *  执行顺序为:
 *  前端请求->自定义拦截器AccessLimitInterceptor的preHandle()->自定义参数解析器->再执行到目标方法
 */
@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    //装配需要的组件/对象
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;


    //preHandle()方法在执行到目标方法之前执行
    //这个方法完成1. 得到user对象，并放入到ThreadLocal 2. 去处理@AccessLimit自定义注解
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //判断要去执行的目标方法的运行类型是否为 import org.springframework.web.method.HandlerMethod;
        //即判断是否是一个控制器方法 精确性：明确说明HandlerMethod代表的是Spring MVC控制器中的方法。
        /**
         * 此处的目的是判断拦截到的处理器（handler）是否为一个控制器方法。
         * HandlerMethod 类型表示该处理器是Spring MVC控制器中的一个具体方法。
         * 如果handler是HandlerMethod的实例，那么我们可以安全地转换它并访问关于
         * 控制器方法的详细信息，如方法名、所在的控制器类、参数类型等。
         *
         * 注意事项：
         * - 如果请求是针对静态资源或直接通过Servlet处理（而不经过Spring MVC的映射），
         *   那么handler可能是ResourceHttpRequestHandler等其他类型，此时不能将其视为
         *   控制器方法。
         * - 只有当handler是HandlerMethod实例时，我们才能执行如权限验证、日志记录或
         *   特定业务逻辑处理等操作，因为这些需要访问控制器方法的元数据或属性。
         */
        if (handler instanceof HandlerMethod) {
            //这里我们就先获取到登录的user
            User user = getUser(request, response);
            //将user对象放入到ThreadLocal中【注意这里其实是放入到当前线程的threadLocals属性ThreadLocalMap中了】
            //每个线程（处理一个请求）都在其自己的`ThreadLocalMap`中操作数据，保证了数据的隔离性。
            //`ThreadLocal`利用每个线程持有自己的`ThreadLocalMap`来实现数据隔离。
            // 虽然`ThreadLocal`变量是静态的（即类级别的，所有实例共享），
            // 每个线程通过`ThreadLocal`实例存取的数据实际上存储在它自己的`ThreadLocalMap`中。
            // 这保证了即使多个线程访问同一个`ThreadLocal`对象，
            // 它们也只能访问各自独立存储的数据。

            // 放入的user是有可能为null的,即没有登录时为null
            UserContext.setUser(user);

            //处理自定义注解@AccessLimit
            //把handler 转成 HandlerMethod
            HandlerMethod hm = (HandlerMethod) handler;
            //使用HandlerMethod对象hm获取到控制器目标方法上的@AccessLimit注解信息
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            //进行判断
            if (accessLimit == null) { //如果目标方法没有@AccessLimit注解，说明该接口不需要处理限流防刷
                return true;//放行,继续执行 放行后这里是先走到自定义参数解析器-再执行到目标方法
            }
            //走到这里说明要访问的目标方法上有@AccessLimit注解,需要进行限流防刷
            //解析注解上设置的值
            int second = accessLimit.second();//获取到时间范围
            int maxCount = accessLimit.maxCount();//获取到最大的访问次数
            boolean needLogin = accessLimit.needLogin();//获取是否需要登录，才能访问该接口
            if (needLogin) { //说明用户必须登录才能访问目标方法/接口
                //进行下一步判断，判断用户是否登录
                if (user == null) {//说明没有登录
                    //给前端返回一个用户信息错误的提示信息...
                    render(response,RespBeanEnum.SESSION_ERROR);
                    return false;//返回
                }
            }

            //限流计数器的逻辑
            String uri = request.getRequestURI();
            String key = uri + ":" + user.getId();
            ValueOperations valueOperations = redisTemplate.opsForValue();
            //得到5s内访问次数该"/seckill/path"方法的次数 count
            Integer count = (Integer) valueOperations.get(key);
            if (count == null) {// 说明还没有这个key,说明是在second秒内第一次访问该接口,就初始化,值为1,超时/过期时间为5s
                valueOperations.set(key, 1, second, TimeUnit.SECONDS);
            } else if (count < maxCount) { //说明是正常访问,将该key的值加1
                valueOperations.increment(key);
            } else { //count >= 5  说明用户在刷接口
                //返回一个频繁访问的提示..一会单独处理
                render(response,RespBeanEnum.ACCESS_LIMIT_REACHED);
                return false;//不往下走了，返回
            }

        }


        return true;
    }


    //单独编写方法，得到登录的user对象-根据cookie中的userTicket
    private User getUser(HttpServletRequest request, HttpServletResponse response) {

        String userTicket = CookieUtil.getCookieValue(request, "userTicket");
        if (!StringUtils.hasText(userTicket)) {
            return null;//说明该用户没有登录,直接返回一个null
        }

        return userService.getUserByCookie(userTicket, request, response);

    }


    //方法: 构建返回对象，以流的形式返回
    //结论： "以流的形式给前端返回数据，响应体中返回json字符串形式" 与
    // "@ResponseBody 加 RespBean 的形式给前端返回数据，
    // 响应体中返回json字符串形式“ 前端接收到的形式是相同的，
    // 都是在响应体中json格式接收到数据的!!!
    private void render(HttpServletResponse response,RespBeanEnum respBeanEnum) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter out = response.getWriter();
        //构建RespBean
        RespBean error = RespBean.error(respBeanEnum);
        //把RespBean转成 json 字符串-使用工具类 jackson
        //然后以流的形式返回给前端
        out.write(new ObjectMapper().writeValueAsString(error));
        out.flush();
        out.close();


    }



}
