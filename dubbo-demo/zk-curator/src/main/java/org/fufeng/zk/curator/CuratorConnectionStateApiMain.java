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

/**
 * @program: dubbo-parent
 * @description: 连接状态API Listener
 * Curator 还提供了监听连接状态的监听器——ConnectionStateListener，
 * 它主要是处理 Curator 客户端和 ZooKeeper 服务器间连接的异常情况，例如， 短暂或者长时间断开连接。
 * <p>
 * 短暂断开连接时，ZooKeeper 客户端会检测到与服务端的连接已经断开，但是服务端维护的客户端 Session 尚未过期，
 * 之后客户端和服务端重新建立了连接；当客户端重新连接后，由于 Session 没有过期，ZooKeeper 能够保证连接恢复后保持正常服务。
 * <p>
 * 长时间断开连接时，Session 已过期，与先前 Session 相关的 Watcher 和临时节点都会丢失。
 * 当 Curator 重新创建了与 ZooKeeper 的连接时，会获取到 Session 过期的相关异常，
 * Curator 会销毁老 Session，并且创建一个新的 Session。
 * 由于老 Session 关联的数据不存在了，在 ConnectionStateListener 监听到 LOST 事件时，
 * 就可以依靠本地存储的数据恢复 Session 了。
 * <p>
 * 这里 Session 指的是 ZooKeeper 服务器与客户端的会话。
 * 客户端启动的时候会与服务器建立一个 TCP 连接，从第一次连接建立开始，客户端会话的生命周期也开始了。
 * 客户端能够通过心跳检测与服务器保持有效的会话，也能够向 ZooKeeper 服务器发送请求并接受响应，
 * 同时还能够通过该连接接收来自服务器的 Watch 事件通知。
 * <p>
 * 我们可以设置客户端会话的超时时间（sessionTimeout），当服务器压力太大、网络故障或是客户端主动断开连接等原因导致连接断开时，
 * 只要客户端在 sessionTimeout 规定的时间内能够重新连接到 ZooKeeper 集群中任意一个实例，那么之前创建的会话仍然有效。
 * ZooKeeper 通过 sessionID 唯一标识 Session，所以在 ZooKeeper 集群中，sessionID 需要保证全局唯一。
 * 由于 ZooKeeper 会将 Session 信息存放到硬盘中，即使节点重启，之前未过期的 Session 仍然会存在。
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-16
 */
public class CuratorConnectionStateApiMain {

    public static void main(String[] args) throws Exception {
        // Zookeeper集群地址，多个节点地址可以用逗号分隔
        String zkAddress = "127.0.0.1:2181";
        // 重试策略，如果连接不上ZooKeeper集群，会重试三次，重试间隔会递增
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        // 创建Curator Client并启动，启动成功之后，就可以与Zookeeper进行交互了
        CuratorFramework client = CuratorFrameworkFactory
                .newClient(zkAddress, retryPolicy);
        client.start();
        // 添加ConnectionStateListener监听器
        client.getConnectionStateListenable().addListener(
                (clientCallBack, newState) -> {
                    // 这里我们可以针对不同的连接状态进行特殊的处理
                    switch (newState) {
                        case CONNECTED:
                            // 第一次成功连接到ZooKeeper之后会进入该状态。
                            // 对于每个CuratorFramework对象，此状态仅出现一次
                            break;
                        case SUSPENDED: //   ZooKeeper的连接丢失
                            break;
                        case RECONNECTED: // 丢失的连接被重新建立
                            break;
                        case LOST:
                            // 当Curator认为会话已经过期时，则进入此状态
                            break;
                        case READ_ONLY: // 连接进入只读模式
                            break;
                    }
                });
    }

}
