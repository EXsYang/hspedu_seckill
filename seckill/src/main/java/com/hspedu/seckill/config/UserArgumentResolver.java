package com.hspedu.seckill.config;

import com.hspedu.seckill.pojo.User;
import com.hspedu.seckill.service.UserService;
import com.hspedu.seckill.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author yangda
 * @create 2024-04-23-19:20
 * @description: springboot的解析器机制
 * UserArgumentResolver 是我们自定义的一个 用户参数解析器
 * 用来解析浏览器到Controller层的方法之前，对参数
 * 根据 Cookie、request、response、业务service方法 进行解析/封装 User对象
 * 在Controller层的方法形参位置，可以直接拿到User对象的具体信息
 *
 * 注意:
 * 1. 所有的请求在到达控制层方法之前，都会经过该解析器处理，进行判断，控制层方法的形参中是否有
 *    这里需要解析的User.class 类型的参数，如果有则进行解析封装
 * 2. 需要将我们这里自定义的 UserArgumentResolver 解析器
 *    加入到WebMvcConfigurer接口的实现类WebConfig中的
 *    HandlerMethodArgumentResolver 列表中之后，该UserArgumentResolver解析器才生效！！！
 *
 */
@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    // @Autowired
    @Resource
    private UserService userService;


    //该方法 判断你当前要解析的参数类型是不是你需要的
    //如果这个方法返回 true 才会执行下面的 resolveArgument 方法
    //返回 false 不执行下面的方法
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        //获取参数的类型
        Class<?> aClass = parameter.getParameterType();
        //1. 判断aClass是不是 user 类型，如果为 true,
        //   就执行 resolveArgument，Dog Cat Person等类型都可以，根据实际情况进行封装
        //2. 这里只对User类型进行处理，其他的类型，如LoginVo类型，如果出现在Controller层的
        //   方法形参位置，则按照默认的 自定义参数封装机制 进行处理，不受影响
        return aClass == User.class;

        // 为什么 `aClass == User.class;` 选择使用 == 而非其他类型检查方法
        // 使用 == 检查参数类型是否严格为 User 类型
        // 使用 == 是适当的，因为：
        // 1. 类型标识：`==` 检查两个类对象是否指向同一个 Class 实例。
        // 2. 类对象唯一性：对于任何给定的由特定类加载器加载的类，JVM 都保证只有一个 Class 对象。
        // 3. 性能优势：使用 `==` 比较内存地址比使用 `instanceof`（还考虑子类）性能更好。
        // 4. 使用场景：此处需要确保参数类型不仅是 User 类型，而且不包括其子类。
    }

    /**
     * 如果上面的supportsParameter返回true,就执行下面的resolveArgument方法
     * 到底怎么解析，是由程序员根据业务来编写
     * 这个方法，类似拦截器，将传入的参数，取出 cookie 值，然后获取对应的 User 对象
     * 并把这个 User 对象作为参数继续传递.
     * @param parameter
     * @param mavContainer
     * @param webRequest
     * @param binderFactory
     * @return
     * @throws Exception
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer
            mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
            throws Exception {
        // HttpServletRequest request =
        //         webRequest.getNativeRequest(HttpServletRequest.class);
        // HttpServletResponse response =
        //         webRequest.getNativeResponse(HttpServletResponse.class);
        //
        // String ticket = CookieUtil.getCookieValue(request, "userTicket");
        // if (!StringUtils.hasText(ticket)) {
        //     return null;
        // }
        // //根据 cookie-ticket 到 Redis 获取 User用户信息
        // User user = userService.getUserByCookie(ticket, request, response);
        //
        // //这里返回，相当于直接返回到/封装到了Controller层的控制器方法形参位置上了，
        // //即如果Controller层的方法 如果使用到参数类型为User.class类型的参数，会直接
        // //被封装为从Redis中根据cookie等获取到的具体的user对象的信息
        // return user;


        return UserContext.getUser();
    }
}
