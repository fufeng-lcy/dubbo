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
package org.apache.simple.constants;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @program: dubbo-parent
 * @description: rpc 常量定义
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public interface RpcConstant {

    /**
     *  head 头 16字节
     */
    int HEAD_SIZE = 16;

    /**
     *  魔数
     */
    short MAGIC = (short) 0xE0F1;

    /**
     *  当前RPC 版本
     */
    byte VERSION = 0;

    /**
     * 默认的IO线程池
     */
    int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);;

    /**
     *  默认超时时间 单位(ms)
     */
    long DEFAULT_TIMEOUT = 50000;

    /**
     *  连接超时
     */
    long CONNECTION_TIMEOUT = 2000;

    /**
     *  默认端口
     */
    int DEF_PORT = 20880;

    /**
     *  心跳消息代码
     */
    int HEARTBEAT_CODE = -1;

    /**
     *  心跳扩展信息
     */
    byte HEART_EXTRA_INFO = 1;

}
