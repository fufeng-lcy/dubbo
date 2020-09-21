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
import org.apache.simple.bean.RpcBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: dubbo-parent
 * @description: 基于zookeeper的服务注册
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-18
 * @see ServiceCacheImpl serviceCache 实现
 */
public class ZookeeperRegistry<T> implements Registry<T> {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    /**
     * 服务监听实例
     *  key(String) -> 服务注册名称
     *  value(ServiceInstanceListener) -> 服务实例监听器
     */
    private final Map<String, List<ServiceInstanceListener<T>>> listeners =
            Maps.newConcurrentMap();

    /**
     * 实例序列化器
     */
    private final InstanceSerializer serializer =
            new JsonInstanceSerializer<>(ServerInfo.class);
    /**
     * 服务发现
     */
    private ServiceDiscovery<T> serviceDiscovery;
    /**
     * 服务缓存
     */
    private Map<String, ServiceCache<T>> serviceCache;
    /**
     * zk默认地址
     */
    private final String address = "127.0.0.1:2181";

    /**
     * 默认的zk根目录
     */
    private final String root = "/rpc";

    public void start() throws Exception {
        start(address);
    }

    public void start(String address) throws Exception {
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

        // 如果是服务端发布服务
        if (Objects.nonNull(RpcBeanFactory.getBeanManager())) {
            // 获取所有的Bean
            final Map<String, Object> beans =
                    RpcBeanFactory.getBeanManager().getBeans();
            if (beans.size() > 0) {
                if (Objects.isNull(serviceCache)) {
                    synchronized (root) {
                        if (Objects.isNull(serviceCache)) {
                            serviceCache = new ConcurrentHashMap<>();
                        }
                    }
                }
                for (Map.Entry<String, Object> entry : beans.entrySet()) {
                    final ServiceCache<T> sc = serviceDiscovery.serviceCacheBuilder()
                            .name("/" + entry.getKey()).build();
                    // 创建ServiceCache，监Zookeeper相应节点的变化，也方便后续的读取
                    this.serviceCache.put(entry.getKey(), sc);
                    // 启动ServiceCache
                    sc.start();
                }
            } else {
                logger.info("没有需要发布的服务");
            }
        }

        // 阻塞当前线程，等待连接成
        client.blockUntilConnected();

        // 启动ServiceDiscovery
        serviceDiscovery.start();
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
        /*return serviceCache.getInstances().stream()
                .filter(svn -> svn.getName().equals(name))
                .collect(Collectors.toList());*/
        //return new ArrayList<>(serviceDiscovery.queryForInstances(name));
        return serviceCache.get(name).getInstances();
    }

    /**
     * 消费端增加服务缓存
     *
     * @param serviceName 服务名称
     */
    public void addServiceCache(String serviceName) throws Exception {
        final ServiceCache<T> sc = serviceDiscovery.serviceCacheBuilder()
                .name("/" + serviceName).build();
        if (Objects.isNull(serviceCache)) {
            synchronized (root) {
                if (Objects.isNull(serviceCache)) {
                    serviceCache = new ConcurrentHashMap<>();
                }
            }
        }
        // 创建ServiceCache，监Zookeeper相应节点的变化，也方便后续的读取
        this.serviceCache.put(serviceName, sc);
        // 启动ServiceCache
        sc.start();
    }

}
