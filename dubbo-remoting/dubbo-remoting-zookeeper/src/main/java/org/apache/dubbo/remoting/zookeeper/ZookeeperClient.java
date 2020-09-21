/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.zookeeper;

import org.apache.dubbo.common.URL;

import java.util.List;
import java.util.concurrent.Executor;

public interface ZookeeperClient {

    // 创建 ZNode 节点，还提供了创建临时 ZNode 节点的重载方法
    void create(String path, boolean ephemeral);

    // 删除某个路径
    void delete(String path);

    // 获取指定节点的子节点集合
    List<String> getChildren(String path);

    // 添加节点 子节点监听器
    List<String> addChildListener(String path, ChildListener listener);

    /**
     *      添加数据监听器
     * @param path:    directory. All of child of path will be listened.
     * @param listener
     */
    void addDataListener(String path, DataListener listener);

    /**
     *  添加 异步数据监听器
     * @param path:    directory. All of child of path will be listened.
     * @param listener
     * @param executor another thread
     */
    void addDataListener(String path, DataListener listener, Executor executor);

    /**
     *  删除监听器
     */
    void removeDataListener(String path, DataListener listener);

    /**
     *  删除子节点监听器
     */
    void removeChildListener(String path, ChildListener listener);

    /**
     *  添加状态监听器
     * @param listener 监听器
     */
    void addStateListener(StateListener listener);

    /**
     *  移除状态监听器
     * @param listener 监听器
     */
    void removeStateListener(StateListener listener);

    boolean isConnected();

    void close();

    URL getUrl();

    void create(String path, String content, boolean ephemeral);

    // 获取某个节点存储的内容
    String getContent(String path);

}
