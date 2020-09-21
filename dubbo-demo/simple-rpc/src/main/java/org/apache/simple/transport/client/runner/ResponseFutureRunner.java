/*
 * The MIT License (MIT)
 * ------------------------------------------------------------------
 * Copyright © 2019 Ramostear.All Rights Reserved.
 *
 * ProjectName: dubbo-parent
 * @Author : <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @date : 2020-09-21
 * @version : 1.0.0-RELEASE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.apache.simple.transport.client.runner;

import org.apache.simple.transport.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: dubbo-parent
 * @description: 去除客户端响应超时处理
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-21
 */
public class ResponseFutureRunner implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResponseFutureRunner.class);

    @Override
    public void run() {
        logger.info("start clear timeout message");
        Connection.IN_FLIGHT_REQUEST_MAP.keySet()
                .stream()
                .filter(msgId ->
                        System.currentTimeMillis() - Connection.IN_FLIGHT_REQUEST_MAP.get(msgId).getTimestamp()
                                > Connection.IN_FLIGHT_REQUEST_MAP.get(msgId).getTimeout()
                ).forEach(msgId -> {
            logger.info(msgId + "-> timeout，remove msgId " + msgId);
            // 取消future
            Connection.IN_FLIGHT_REQUEST_MAP.get(msgId).getPromise().cancel(true);
            // 关闭通道
            Connection.IN_FLIGHT_REQUEST_MAP.get(msgId).getChannel().closeFuture();
            Connection.IN_FLIGHT_REQUEST_MAP.get(msgId).getChannel().close();
            Connection.IN_FLIGHT_REQUEST_MAP.remove(msgId);
        });
        logger.info("clear timeout message end");
    }

}
