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
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @program: dubbo-parent
 * @description: cache watcher 解决watcher执行一次的问题
 *  Apache Curator 引入了 Cache 来实现对 ZooKeeper 服务端事件的监听。
 *  Cache 是 Curator 中对事件监听的包装，其对事件的监听其实可以近似看作是一个本地缓存视图和远程ZooKeeper 视图的对比过程。
 *  同时，Curator 能够自动为开发人员处理反复注册监听，从而大大简化了代码的复杂程度。
 *
 *  常用的 Cache 有三大类：
 *  NodeCache。 对一个节点进行监听，监听事件包括指定节点的增删改操作。
 *  注意哦，NodeCache 不仅可以监听数据节点的内容变更，也能监听指定节点是否存在，
 *  如果原本节点不存在，那么 Cache 就会在节点被创建后触发 NodeCacheListener，删除操作亦然。
 *
 *  PathChildrenCache。 对指定节点的一级子节点进行监听，监听事件包括子节点的增删改操作，但是不对该节点的操作监听。
 *
 *  TreeCache。 综合 NodeCache 和 PathChildrenCache 的功能，是对指定节点以及其子节点进行监听，同时还可以设置监听的深度。
 *
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-16
 */
public class CuratorCacheWatcherApiMain {

    public static void main(String[] args) throws Exception{
        // Zookeeper集群地址，多个节点地址可以用逗号分隔
        String zkAddress = "127.0.0.1:2181";
        // 重试策略，如果连接不上ZooKeeper集群，会重试三次，重试间隔会递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,3);
        // 创建Curator Client并启动，启动成功之后，就可以与Zookeeper进行交互了
        CuratorFramework client = CuratorFrameworkFactory
                .newClient(zkAddress, retryPolicy);
        client.start();

        // 创建NodeCache，监听的是"/user"这个节点
        NodeCache nodeCache = new NodeCache(client, "/user");
        //final CuratorCache nodeCache = CuratorCache.builder(client, "/user").build();
        // start()方法有个boolean类型的参数，默认是false。如果设置为true，
        // 那么NodeCache在第一次启动的时候就会立刻从ZooKeeper上读取对应节点的
        // 数据内容，并保存在Cache中。
        nodeCache.start(true);
        if (nodeCache.getCurrentData() != null) {
            System.out.println("NodeCache节点初始化数据为："
                    + new String(nodeCache.getCurrentData().getData()));
        } else {
            System.out.println("NodeCache节点数据为空");
        }

        // 添加监听器
        nodeCache.getListenable().addListener(() -> {
            String data = new String(nodeCache.getCurrentData().getData());
            System.out.println("NodeCache节点路径：" + nodeCache.getCurrentData().getPath()
                    + "，节点数据为：" + data);
        });

        // 创建PathChildrenCache实例，监听的是"user"这个节点
        PathChildrenCache childrenCache = new PathChildrenCache(client, "/user", true);
        // StartMode指定的初始化的模式
        // NORMAL:普通异步初始化
        // BUILD_INITIAL_CACHE:同步初始化
        // POST_INITIALIZED_EVENT:异步初始化，初始化之后会触发事件
        childrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        // childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        // childrenCache.start(PathChildrenCache.StartMode.NORMAL);
        List<ChildData> children = childrenCache.getCurrentData();
        System.out.println("获取子节点列表：");
        // 如果是BUILD_INITIAL_CACHE可以获取这个数据，如果不是就不行
        children.forEach(childData -> {
            System.out.println(new String(childData.getData()));
        });
        childrenCache.getListenable().addListener(((client1, event) -> {
            System.out.println(LocalDateTime.now() + "  " + event.getType());
            if (event.getType().equals(PathChildrenCacheEvent.Type.INITIALIZED)) {
                System.out.println("PathChildrenCache:子节点初始化成功...");
            } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                String path = event.getData().getPath();
                System.out.println("PathChildrenCache添加子节点:" + event.getData().getPath());
                System.out.println("PathChildrenCache子节点数据:" + new String(event.getData().getData()));
            } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                System.out.println("PathChildrenCache删除子节点:" + event.getData().getPath());
            } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
                System.out.println("PathChildrenCache修改子节点路径:" + event.getData().getPath());
                System.out.println("PathChildrenCache修改子节点数据:" + new String(event.getData().getData()));
            }
        }));

        // 创建TreeCache实例监听"user"节点
        TreeCache cache = TreeCache.newBuilder(client, "/user").setCacheData(false).build();
        cache.getListenable().addListener((c, event) -> {
            if (event.getData() != null) {
                System.out.println("TreeCache,type=" + event.getType() + " path=" + event.getData().getPath());
            } else {
                System.out.println("TreeCache,type=" + event.getType());
            }
        });
        cache.start();

        System.in.read();
    }

}
