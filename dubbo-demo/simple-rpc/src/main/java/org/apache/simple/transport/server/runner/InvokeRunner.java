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
package org.apache.simple.transport.server.runner;

import io.netty.channel.ChannelHandlerContext;
import org.apache.simple.bean.RpcBeanFactory;
import org.apache.simple.protocol.Header;
import org.apache.simple.protocol.Message;
import org.apache.simple.protocol.Request;
import org.apache.simple.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @program: dubbo-parent
 * @description: 执行异步任务处理
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class InvokeRunner implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(InvokeRunner.class);

    /**
     *  执行业务的消息
     */
    private final Message<Request> message;

    /**
     *  通道上下文用于处理网络数据发送
     */
    private final ChannelHandlerContext ctx;

    public InvokeRunner(Message<Request> message,
                        ChannelHandlerContext channelHandlerContext) {
        this.message = message;
        this.ctx = channelHandlerContext;
    }

    @Override
    public void run() {
        Response response = new Response();
        Object result = null;
        try {
            // 获取消息内容
            final Request payload = message.getPayload();
            // 获取服务名称
            final String serviceName = payload.getServiceName();
            // 这里提供BeanManager对所有业务Bean进行管理，其底层在内存中维护了
            // 一个业务Bean实例的集合。可以接入Spring等容器管理业务Bean
            final Object bean = RpcBeanFactory.getBeanManager().getBean(serviceName);
            // 通过反射调用对应方法
            final Method method = bean.getClass()
                    .getMethod(payload.getMethodName(), payload.getArgTypes());
            // 调用方法
            result = method.invoke(bean, payload.getArgs());
        }catch (Exception e){
            // TODO
            logger.info("invoke error -> "+e.getMessage());
        }finally {
            // TODO
        }

        // 创建一个header
        Header header = message.getHeader();
        header.setExtraInfo((byte) 1);

        // 设置响应结果
        response.setResult(result);
        // 将响应消息返回非客户端
        this.ctx.writeAndFlush(new Message<>(header, response));
    }

}
