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

import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.apache.simple.constants.RpcConstant;
import org.apache.simple.protocol.Header;
import org.apache.simple.protocol.Message;
import org.apache.simple.protocol.Request;
import org.apache.simple.protocol.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: dubbo-parent
 * @description: 连接处理
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class Connection implements Closeable {

    /**
     * 定义一个消息ID生成
     */
    private static AtomicLong ID_GENERATOR = new AtomicLong(0);

    /**
     * 定义消息ID，消息回调相关信息
     *  这里如果没有响应可能会存在内存溢出情况，需要处理
     *  TODO 内存溢出问题 定时删除
     */
    public static Map<Long, NettyResponseFuture<Response>> IN_FLIGHT_REQUEST_MAP =
            new HashMap<>();

    /**
     * netty 通道回调
     */
    private ChannelFuture channelFuture;

    /**
     * 是否连接
     */
    private AtomicBoolean isConnected = new AtomicBoolean();

    public Connection() {
        this.isConnected.set(false);
        this.channelFuture = null;
    }

    public Connection(ChannelFuture channelFuture, boolean isConnected) {
        this.channelFuture = channelFuture;
        this.isConnected.set(isConnected);
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public void setChannelFuture(ChannelFuture channelFuture) {
        this.channelFuture = channelFuture;
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected.set(isConnected);
    }

    public NettyResponseFuture<Response> request(Message<Request> message, long timeout) {
        // 生成本次请求的消息ID
        long msgId = ID_GENERATOR.incrementAndGet();
        // 设置消息ID
        message.getHeader().setMessageId(msgId);
        // 创建和消息相关的Future
        NettyResponseFuture responseFuture = new NettyResponseFuture<>(System.currentTimeMillis(),
                timeout, message, channelFuture.channel(), new DefaultPromise(new DefaultEventLoop()));
        // 将消息ID和关联的Future存入到IN_FLIGHT_REQUEST_MAP中
        IN_FLIGHT_REQUEST_MAP.put(msgId, responseFuture);

        try {
            // 发送消息
            channelFuture.channel().writeAndFlush(message);
        } catch (Exception e) {
            // 发送异常删除对应的future
            IN_FLIGHT_REQUEST_MAP.remove(msgId);
            throw e;
        }
        return responseFuture;
    }

    public boolean ping() {
        Header heartBeatHeader = new Header(RpcConstant.MAGIC, RpcConstant.VERSION);
        heartBeatHeader.setExtraInfo(RpcConstant.HEART_EXTRA_INFO);
        Message message = new Message(heartBeatHeader, null);
        NettyResponseFuture<Response> request = request(message, RpcConstant.DEFAULT_TIMEOUT);
        try {
            Promise<Response> await = request.getPromise().await();
            return await.get().getCode() == RpcConstant.HEARTBEAT_CODE;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        this.channelFuture.channel().close();
    }
}
