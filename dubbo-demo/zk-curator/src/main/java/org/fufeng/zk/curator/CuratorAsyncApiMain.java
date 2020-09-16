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
package org.fufeng.zk.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/**
 * @program: dubbo-parent
 * @description: background in curator
 * 引入了BackgroundCallback 这个回调接口以及 CuratorListener 这个监听器，用于处理 Background 调用之后服务端返回的结果信息。
 * BackgroundCallback 接口和 CuratorListener 监听器中接收一个 CuratorEvent 的参数，里面包含事件类型、响应码、节点路径等详细信息
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-16
 */
public class CuratorAsyncApiMain {

    public static void main(String[] args) throws Exception {
        // Zookeeper集群地址，多个节点地址可以用逗号分隔
        String zkAddress = "127.0.0.1:2181";
        // 重试策略，如果连接不上ZooKeeper集群，会重试三次，重试间隔会递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        // 创建Curator Client并启动，启动成功之后，就可以与Zookeeper进行交互了
        CuratorFramework client = CuratorFrameworkFactory
                .newClient(zkAddress, retryPolicy);
        client.start();
        // 添加CuratorListener监听器，针对不同的事件进行处理
        client.getCuratorListenable().addListener(
                (clientCallBack, event) -> {
                    switch (event.getType()) {
                        case CREATE:
                            System.out.println("CREATE:" +
                                    event.getPath());
                            break;
                        case DELETE:
                            System.out.println("DELETE:" +
                                    event.getPath());
                            break;
                        case EXISTS:
                            System.out.println("EXISTS:" +
                                    event.getPath());
                            break;
                        case GET_DATA:
                            System.out.println("GET_DATA:" +
                                    event.getPath() + ","
                                    + new String(event.getData()));
                            break;
                        case SET_DATA:
                            System.out.println("SET_DATA:" +
                                    new String(event.getData()));
                            break;
                        case CHILDREN:
                            System.out.println("CHILDREN:" +
                                    event.getPath());
                            break;
                        default:
                    }
                });
        // 注意:下面所有的操作都添加了inBackground()方法，转换为后台操作
        client.create().withMode(CreateMode.PERSISTENT)
                .inBackground().forPath("/user", "test".getBytes());
        client.checkExists().inBackground().forPath("/user");
        client.setData().inBackground().forPath("/user",
                "setData-Test".getBytes());
        client.getData().inBackground().forPath("/user");
        for (int i = 0; i < 3; i++) {
            client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .inBackground().forPath("/user/child-");
        }
        client.getChildren().inBackground().forPath("/user");
        // 添加BackgroundCallback
        client.getChildren().inBackground((clientCallBack, event) ->
                System.out.println("in background:"
                + event.getType() + "," + event.getPath())).forPath("/user");
        client.delete().deletingChildrenIfNeeded().inBackground()
                .forPath("/user");
        System.in.read();
    }

}
