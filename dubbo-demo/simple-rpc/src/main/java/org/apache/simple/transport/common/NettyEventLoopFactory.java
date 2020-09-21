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
package org.apache.simple.transport.common;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

/**
 * @program: dubbo-parent
 * @description: NettyEventLoop 工厂
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class NettyEventLoopFactory {

    /**
     * 创建一个自定义的EventLoopGroup
     *
     * @param threads 线程数量
     * @param threadFactoryName  事件组名称
     * @return 事件循环组
     */
    public static EventLoopGroup eventLoopGroup(int threads, String threadFactoryName) {
        ThreadFactory threadFactory = new DefaultThreadFactory(threadFactoryName, true);
        return shouldEpoll() ? new EpollEventLoopGroup(threads, threadFactory) :
                new NioEventLoopGroup(threads, threadFactory);
    }

    /**
     *  根据不同的操作系统选择不同的客户端连接通道
     * @return 连接通道的class
     */
    public static Class<? extends SocketChannel> socketChannelClass() {
        return shouldEpoll() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    /**
     *  根据不同的操作系统选择不同的服务端连接通道
     * @return 连接通道的class
     */
    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return shouldEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    /**
     *  是否启用epoll io模型
     * @return 是否启用
     */
    private static boolean shouldEpoll() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }
}
