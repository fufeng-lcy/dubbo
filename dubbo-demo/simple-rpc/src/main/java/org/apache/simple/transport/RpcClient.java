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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.simple.codec.RpcMessageDecoder;
import org.apache.simple.codec.RpcMessageEncoder;
import org.apache.simple.constants.RpcConstant;

/**
 * @program: dubbo-parent
 * @description: 作为RpcClient入口
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class RpcClient implements Cloneable{

    /**
     *  netty 启动实例
     */
    protected Bootstrap clientBootstrap;

    /**
     *  netty 事件循环组
     */
    protected EventLoopGroup group;

    /**
     *  主机地址
     */
    private String host;

    /**
     *  端口号
     */
    private int port;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;

        // 创建Bootstrap
        clientBootstrap = new Bootstrap();
        // 配置Bootstrap相关参数信息
        group = NettyEventLoopFactory.eventLoopGroup(
                RpcConstant.DEFAULT_IO_THREADS, "NettyClientWorker");
        // 开始配置
        clientBootstrap.group(group)
                .option(ChannelOption.TCP_NODELAY,true) // 禁用TCP nodelay算法
                .option(ChannelOption.SO_KEEPALIVE,true) // 保持常连接
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                // 指定ChannelHandler顺序
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("rpc-encoder",new RpcMessageEncoder());
                        ch.pipeline().addLast("rpc-decoder",new RpcMessageDecoder());
                        ch.pipeline().addLast("client-handler",new RpcClientHandler());
                    }
                });
    }

    /**
     *  连接到对应的IP/port
     * @return 连接回调
     */
    public ChannelFuture connect(){
        final ChannelFuture connect =
                clientBootstrap.connect(host, port);
        return connect.awaitUninterruptibly();
        //return connect;
    }

    /**
     *  关闭连接
     */
    public void close(){
        group.shutdownGracefully();
    }
}
