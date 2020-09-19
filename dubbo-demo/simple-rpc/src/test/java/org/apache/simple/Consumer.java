/*
 * The MIT License (MIT)
 * ------------------------------------------------------------------
 * Copyright © 2019 Ramostear.All Rights Reserved.
 *
 * ProjectName: dubbo-parent
 * @Author : <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @date : 2020-09-18
 * @version : 1.0.0-RELEASE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.apache.simple;

import org.apache.simple.proxy.RpcProxy;
import org.apache.simple.registry.ServerInfo;
import org.apache.simple.registry.ZookeeperRegistry;

/**
 * @program: dubbo-parent
 * @description: 消费者
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class Consumer {

    public static void main(String[] args) throws Exception {
        // 创建ZookeeperRegistr对象
        ZookeeperRegistry<ServerInfo> discovery = new ZookeeperRegistry<>();
        // 订阅服务
        discovery.start();
        // 创建代理对象，通过代理调用远端Server
        UserService userService = RpcProxy.newInstance(UserService.class, discovery);
        // 调用sayHello()方法，并输出结果
        String result = userService.users("fufeng");
        System.out.println(result);
        Thread.sleep(10000000L);
    }

}
