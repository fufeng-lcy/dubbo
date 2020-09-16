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
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;

/**
 * @program: dubbo-parent
 * @description: watcher API
 *  Watcher 监听机制是 ZooKeeper 中非常重要的特性，可以监听某个节点上发生的特定事件，
 *  例如，监听节点数据变更、节点删除、子节点状态变更等事件。
 *  当相应事件发生时，ZooKeeper 会产生一个 Watcher 事件，并且发送到客户端。
 *  通过 Watcher 机制，就可以使用 ZooKeeper 实现分布式锁、集群管理等功能。
 *
 *  在 Curator 客户端中，我们可以使用 usingWatcher() 方法添加 Watcher。
 *
 *  watcher 只会执行一次，只能通过执行完成后进行再次注册watcher来做这个重复监听事件。
 *
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-16
 */
public class CuratorWatcherApiMain {

    public static void main(String[] args) throws Exception {
        // Zookeeper集群地址，多个节点地址可以用逗号分隔
        String zkAddress = "127.0.0.1:2181";
        // 重试策略，如果连接不上ZooKeeper集群，会重试三次，重试间隔会递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,3);
        // 创建Curator Client并启动，启动成功之后，就可以与Zookeeper进行交互了
        CuratorFramework client = CuratorFrameworkFactory
                .newClient(zkAddress, retryPolicy);
        client.start();
        try {
            client.create().withMode(CreateMode.PERSISTENT)
                    .forPath("/user", "test".getBytes());
        } catch (Exception e) {
        }
        // 这里通过usingWatcher()方法添加一个Watcher
        List<String> children = client.getChildren().usingWatcher(
                (CuratorWatcher) event -> {
                    System.out.println(event.getType() + "," + event.getPath());
                    addWatcher(client,"/user");
                }).forPath("/user");
        System.out.println(children);
        final byte[] path = client.getData().usingWatcher((CuratorWatcher) event -> {
            System.out.println(event.getType() + "," + event.getPath());
            addWatcherForData(client, "/user");
        }).forPath("/user");
        System.out.println(new String(path));
        System.in.read();
    }

    public static void addWatcher(CuratorFramework client,String path) throws Exception {
        client.getChildren().usingWatcher(
                (CuratorWatcher) event -> {
                    System.out.println(event.getType() + "," + event.getPath());
                    addWatcher(client,path);
                }).forPath(path);
    }

    public static void addWatcherForData(CuratorFramework client,String path) throws Exception {
        client.getData().usingWatcher(
                (CuratorWatcher) event -> {
                    System.out.println(event.getType() + "," + event.getPath());
                    addWatcherForData(client,path);
                }).forPath(path);
    }

}
