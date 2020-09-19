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
package org.apache.simple.registry;

import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.details.ServiceCacheImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: dubbo-parent
 * @description: 基于zookeeper的服务注册
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 * @see ServiceCacheImpl serviceCache 实现
 */
public class ZookeeperRegistry<T> implements Registry<T>{

    /**
     *  服务监听实例
     */
    private Map<String, List<ServiceInstanceListener<T>>> listeners =
            Maps.newConcurrentMap();

    /**
     *  实例序列化器
     */
    private InstanceSerializer serializer =
            new JsonInstanceSerializer<>(ServerInfo.class);
    /**
     *  服务发现
     */
    private ServiceDiscovery<T> serviceDiscovery;
    /**
     *  服务缓存
     */
    private ServiceCache<T> serviceCache;
    /**
     *  zk默认地址
     */
    private String address = "127.0.0.1:2181";

    /**
     *  默认的zk根目录
     */
    private String root = "/rpc";

    public void start() throws Exception {
        // 初始化CuratorFramework
        CuratorFramework client = CuratorFrameworkFactory
                .newClient(address, new ExponentialBackoffRetry(1000, 3));
        // 启动Curator客户端
        client.start();
        //client.createContainers(root);

        // 初始化ServiceDiscovery
        serviceDiscovery = ServiceDiscoveryBuilder
                .builder(ServerInfo.class)
                .client(client).basePath(root)
                .serializer(serializer)
                .build();

        // 创建ServiceCache，监Zookeeper相应节点的变化，也方便后续的读取
        serviceCache = serviceDiscovery.serviceCacheBuilder()
                .name(root)
                .build();

        // 阻塞当前线程，等待连接成
        client.blockUntilConnected();

        // 启动ServiceDiscovery
        serviceDiscovery.start();
        // 启动ServiceCache
        serviceCache.start();
    }
    @Override
    public void registerService(ServiceInstance<T> service)
            throws Exception {
        serviceDiscovery.registerService(service);
    }
    @Override
    public void unregisterService(ServiceInstance service)
            throws Exception {
        serviceDiscovery.unregisterService(service);
    }
    @Override
    public List<ServiceInstance<T>> queryForInstances(
            String name) throws Exception {
        // 直接根据name进行过滤ServiceCache中的缓存数据
        return serviceCache.getInstances().stream()
                .filter(svn -> svn.getName().equals(name))
                .collect(Collectors.toList());
        //return new ArrayList<>(serviceDiscovery.queryForInstances(name));
    }

}
