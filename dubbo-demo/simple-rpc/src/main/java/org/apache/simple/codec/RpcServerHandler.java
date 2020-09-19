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
package org.apache.simple.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.simple.invoke.InvokeRunner;
import org.apache.simple.protocol.Message;
import org.apache.simple.protocol.Request;
import org.apache.simple.utils.CheckUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @program: dubbo-parent
 * @description: Rpc 服务器业务处理
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<Message<Request>> {

    // 业务线程池
    static Executor executor = Executors.newCachedThreadPool();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                Message<Request> message) throws Exception {
        // 获取消息头中的扩展信息
        final byte extraInfo = message.getHeader().getExtraInfo();
        // 判断是否是心跳消息，如果是心跳消息直接返回
        if (CheckUtil.isHeartBeat(extraInfo)){
            channelHandlerContext.writeAndFlush(message);
            return;
        }
        // 如果不是心跳消息就直接封装成一个Runnable交给业务线程执行
        executor.execute(new InvokeRunner(message,channelHandlerContext));
    }
}
