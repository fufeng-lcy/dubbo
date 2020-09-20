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
package org.apache.simple.transport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.simple.codec.RpcMessageDecoder;
import org.apache.simple.codec.RpcMessageEncoder;
import org.apache.simple.constants.RpcConstant;


/**
 * @program: dubbo-parent
 * @description: 作为RpcServer 的入口
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class RpcServer {

    /**
     * netty boss循环组
     */
    private EventLoopGroup bossGroup;

    /**
     * netty worker循环组
     */
    private EventLoopGroup workerGroup;

    /**
     * 服务器启动类
     */
    private ServerBootstrap serverBootstrap;

    /**
     * 通道信息
     */
    private Channel channel;

    /**
     * 端口信息
     */
    protected int port;

    public RpcServer(int port) {
        this.port = port;
        // 创建boss和worker两个EventLoopGroup，注意一些小细节，
        // workerGroup 是按照中的线程数是按照 CPU 核数计算得到的，
        bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "boos");
        workerGroup = NettyEventLoopFactory.eventLoopGroup(
                RpcConstant.DEFAULT_IO_THREADS, "worker");
        serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("rpc-encoder", new RpcMessageEncoder());
                        ch.pipeline().addLast("rpc-decoder", new RpcMessageDecoder());
                        ch.pipeline().addLast("server-handler", new RpcServerHandler());
                    }
                });
    }

    /**
     *  启动rpc服务
     * @return 启动
     * @throws InterruptedException 异常
     */
    public ChannelFuture start() throws InterruptedException {
        ChannelFuture channelFuture = serverBootstrap.bind(port);
        channel = channelFuture.channel();
        channel.closeFuture();
        return channelFuture;
    }

    /**
     *  等待
     * @throws InterruptedException 中断异常
     */
    public void startAndWait() throws InterruptedException {
        try {
            channel.closeFuture().await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    /**
     *  关闭服务
     * @throws InterruptedException 中断
     */
    public void shutdown() throws InterruptedException {
        channel.close().sync();
        if (bossGroup != null)
            bossGroup.shutdownGracefully().awaitUninterruptibly(15000);
        if (workerGroup != null)
            workerGroup.shutdownGracefully().awaitUninterruptibly(15000);
    }
}
