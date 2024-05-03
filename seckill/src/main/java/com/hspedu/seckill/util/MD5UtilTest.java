package com.hspedu.seckill.util;

import org.junit.jupiter.api.Test;

/**
 * @author yangda
 * @create 2024-04-18-21:43
 * @description: 测试 MD5Util 方法的使用
 */
public class MD5UtilTest {


    @Test
    public void f1() {

        //密码明文"12345"
        //1. 获取到密码明文 "12345" 的中间密码[即客户端加密加盐后],在网络上传输的密码
        //2. 即第一次加密加盐处理
        //3. 这个加密加盐的工作, 会在客户端/浏览器完成

        //1. 12345 就是用户输入的密码, inputPassToMidPass() 返回的是中间密码
        // , 中间密码是为了增强安全性, 防止传输的密码被网络拦截设计的
        //2. 中间密码也是前端经过 md5() 计算得到, 并通过网络发送给服务器的
        //3. 也就是说, 我们发送给服务其后端的密码是先经过加密,
        //   再通过网络发给服务器的, 并不是发送的用户原生密码
        String midPass = MD5Util.inputPassToMidPass("12345");
        // System.out.println("midPass->" + midPass);

        //中间密码midPass-> "f56a952e4bd462be0c2dc29565d90ee7" 的对应的DB的密码
        //这里对中间密码进行加密加盐时，这个salt可能不一样
        String dbPass = MD5Util.midPassToDBPass(midPass, "3cj5PnMw");
        // System.out.println("dbPass->" + dbPass);

        //密码明文 "12345" -> 得到DB中的密码
        // String inputPassToDBPass = MD5Util.inputPassToDBPass("12345", "3cj5tnMw");

        //null和"null" MD5加密后结果相同
        String inputPassToDBPass = MD5Util.inputPassToDBPass(null, "3cj5tnMw");
        String inputPassToDBPass2 = MD5Util.inputPassToDBPass("null", "3cj5tnMw");
        // String inputPassToDBPass = MD5Util.inputPassToDBPass("", "3cj5tnMw");
        System.out.println("null inputPassToDBPass->" + inputPassToDBPass);
        // null inputPassToDBPass->a3ded5f466549dcbda570c4d1026b242
        System.out.println("null inputPassToDBPass2->" + inputPassToDBPass2);
        // null inputPassToDBPass2->a3ded5f466549dcbda570c4d1026b242


    }

}
