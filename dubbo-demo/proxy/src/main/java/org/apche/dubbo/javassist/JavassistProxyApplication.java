/*
 * The MIT License (MIT)
 * ------------------------------------------------------------------
 * Copyright © 2019 Ramostear.All Rights Reserved.
 *
 * ProjectName: dubbo-parent
 * @Author : <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @date : 2020-09-16
 * @version : 1.0.0-RELEASE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.apche.dubbo.javassist;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

/**
 * @program: dubbo-parent
 * @description: 利用javassist实现代理
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-16
 */
public class JavassistProxyApplication {

    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        ProxyFactory factory = new ProxyFactory();
        // 指定父类，ProxyFactory会动态生成继承该父类的子类
        factory.setSuperclass(UserService.class);
        // 设置过滤器，判断哪些方法调用需要被拦截
        factory.setFilter(m -> {
            if (m.getName().equals("execute")) {
                return true;
            }
            return false;
        });
        // 设置拦截处理
        /*factory.setHandler((self, thisMethod, proceed, args1) -> {
            System.out.println("前置处理");
            Object result = proceed.invoke(self, args1);
            System.out.println("执行结果:" + result);
            System.out.println("后置处理");
            return result;
        });*/

        // 创建JavassistDemo的代理类，并创建代理对象
        Class<?> c = factory.createClass();
        Object obj = c.newInstance();
        ((ProxyObject)obj).setHandler((self, thisMethod, proceed, args1) -> {
            System.out.println("前置处理");
            Object result = proceed.invoke(self, args1);
            System.out.println("执行结果:" + result);
            System.out.println("后置处理");
            return result;
        });
        UserService userService = (UserService) obj;
        // 执行execute()方法，会被拦截
        userService.execute();
        ((ProxyObject)obj).setHandler((self, thisMethod, proceed, args1) -> {
            System.out.println("invoke before");
            Object result = proceed.invoke(self, args1);
            System.out.println("invoke result :" + result);
            System.out.println("invoke after");
            return result;
        });
        // 执行execute()方法，会被拦截
        userService.execute();
        System.out.println(userService.getUserName());
    }

}
