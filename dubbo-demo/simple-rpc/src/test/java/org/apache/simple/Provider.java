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

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.simple.bean.BeanManager;
import org.apache.simple.bean.RpcBeanFactory;
import org.apache.simple.registry.ServerInfo;
import org.apache.simple.registry.ZookeeperRegistry;
import org.apache.simple.spi.RpcServiceLoader;
import org.apache.simple.transport.RpcServer;

/**
 * @program: dubbo-parent
 * @description: 提供者
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class Provider {

    public static void main(String[] args) throws Exception {
        // 创建DemoServiceImpl，并注册到BeanManager中
        RpcServiceLoader.spi();
        RpcBeanFactory.getBeanManager().registerSingleton(UserService.class.getName(),
                new UserServiceImpl());

        // 创建ZookeeperRegistry，并将Provider的地址信息封装成ServerInfo
        // 对象注册到Zookeeper
        ZookeeperRegistry<ServerInfo> discovery =
                new ZookeeperRegistry<>();
        discovery.start();
        ServerInfo serverInfo = new ServerInfo("127.0.0.1", 20880);
        discovery.registerService(
                ServiceInstance.<ServerInfo>builder().name(UserService.class.getName())
                        .payload(serverInfo).build());
        // 启动RpcServer，等待Client的请求
        RpcServer rpcServer = new RpcServer(20880);
        rpcServer.start();
        Thread.sleep(10000000L);
    }

}
