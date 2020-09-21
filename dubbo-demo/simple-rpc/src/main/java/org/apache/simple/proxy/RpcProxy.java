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
package org.apache.simple.proxy;

import io.netty.channel.ChannelFuture;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.simple.constants.RpcConstant;
import org.apache.simple.protocol.Header;
import org.apache.simple.protocol.Message;
import org.apache.simple.protocol.Request;
import org.apache.simple.protocol.Response;
import org.apache.simple.registry.Registry;
import org.apache.simple.registry.ServerInfo;
import org.apache.simple.transport.client.Connection;
import org.apache.simple.transport.client.NettyResponseFuture;
import org.apache.simple.transport.client.RpcClient;
import org.apache.simple.transport.client.runner.ClientExecutor;
import org.apache.simple.transport.client.runner.ResponseFutureRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.simple.constants.RpcConstant.MAGIC;
import static org.apache.simple.constants.RpcConstant.VERSION;

/**
 * @program: dubbo-parent
 * @description: rpc 业务代理，主要针对客户端实现
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 */
public class RpcProxy implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RpcProxy.class);

    // 需要代理服务的名称
    private final String serviceName;
    // 用于与zookeeper交互，缓存功能
    private final Registry<ServerInfo> registry;
    // 缓存header
    public Map<Method, Header> headerCache = new ConcurrentHashMap<>();

    static {
        ClientExecutor.executeRpcClientResponseTimeout(new ResponseFutureRunner());
    }

    public RpcProxy(String serviceName, Registry<ServerInfo> registry) {
        this.serviceName = serviceName;
        this.registry = registry;
        try {
            registry.addServiceCache(serviceName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建代理类
     *
     * @param clazz    需要被代理的接口
     * @param registry 注册相关信息
     * @param <T>      具体接口类型
     * @return 返回接口的代理类
     */
    public static <T> T newInstance(Class<T> clazz, Registry<ServerInfo> registry) {
        // 创建代理对象
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{clazz},
                new RpcProxy(clazz.getName(), registry)
                //new RpcProxy("rcpService", registry)
        );
    }

    @Override
    public Object invoke(Object proxy,
                         Method method, Object[] args) throws Throwable {
        // 从zookeeper缓存中获取一个可用的server地址，随机或者轮训，这里可以实现一个具体的策略用于选择不同的server
        final List<ServiceInstance<ServerInfo>> serviceInstances =
                registry.queryForInstances(this.serviceName);
        // 是否没有对应的服务实例
        if (serviceInstances == null || serviceInstances.size() == 0) {
            System.out.println(Thread.currentThread()+"["+serviceName+"]:没有查询到对应的服务实例");
            return null;
        }
        final ServiceInstance<ServerInfo> serverInfoServiceInstance =
                serviceInstances.get(ThreadLocalRandom.current()
                        .nextInt(serviceInstances.size()));
        // 创建请求消息，然后调用远程方法
        String methodName = method.getName();
        // 创建请求头
        /*Header header = new Header(RpcConstant.MAGIC,
                RpcConstant.VERSION,(byte)1,null,null);*/
        Header header = headerCache.computeIfAbsent(method, head -> new Header(MAGIC, VERSION));
        Message<Request> message = new Message<>(header,
                new Request(serviceName, methodName, method.getParameterTypes(), args));
        // 获取线程数据
        Response response = remoteCall(serverInfoServiceInstance.getPayload(), message);
        if (Objects.nonNull(response)){
            return response.getResult();
        }
        return null;
    }

    private Response remoteCall(ServerInfo serverInfo, Message message) throws InterruptedException, ExecutionException, TimeoutException {
        if (serverInfo == null) {
            throw new RuntimeException("get available server error");
        }
        logger.info("client remote address -> "+serverInfo.getHost()+":"+serverInfo.getPort());
        // 创建DemoRpcClient连接指定的Server端
        RpcClient demoRpcClient = new RpcClient(
                serverInfo.getHost(), serverInfo.getPort());
        ChannelFuture channelFuture = demoRpcClient.connect()
                .awaitUninterruptibly();
        // 创建对应的Connection对象，并发送请求
        Connection connection = new Connection(channelFuture, true);
        NettyResponseFuture<Response> responseFuture =
                connection.request(message, RpcConstant.DEFAULT_TIMEOUT);
        try {
            // 等待请求对应的响应
            return responseFuture.getPromise().get(
                    RpcConstant.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        }catch (TimeoutException | ExecutionException | InterruptedException te){
            logger.info(responseFuture.getMessage().getHeader().getMessageId()+" failed:"+te.getMessage());
        }
        return null;
    }
}
