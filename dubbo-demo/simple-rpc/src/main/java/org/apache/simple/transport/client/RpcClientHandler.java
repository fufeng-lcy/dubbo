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
package org.apache.simple.transport.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.simple.constants.RpcConstant;
import org.apache.simple.protocol.Message;
import org.apache.simple.protocol.Response;
import org.apache.simple.utils.CheckUtil;

/**
 * @program: dubbo-parent
 * @description: Rpc 客户端实现
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<Message<Response>> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                Message<Response> message) throws Exception {
        // 获取客户端响应的Future
        final NettyResponseFuture<Response> responseFuture =
                Connection.IN_FLIGHT_REQUEST_MAP.remove(message.getHeader().getMessageId());
        // 获取响应消息
        Response payload = message.getPayload();
        // 心跳检测处理
        if (payload == null
                && CheckUtil.isHeartBeat(message.getHeader().getExtraInfo())){
            payload = new Response();
            payload.setCode(RpcConstant.HEARTBEAT_CODE);
        }
        // 设置future监听成功的消息内容
        responseFuture.getPromise().setSuccess(payload);
    }
}
