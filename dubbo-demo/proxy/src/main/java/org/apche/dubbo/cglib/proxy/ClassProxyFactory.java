/*
 * The MIT License (MIT)
 * ------------------------------------------------------------------
 * Copyright © 2019 Ramostear.All Rights Reserved.
 *
 * ProjectName: dubbo-parent
 * @Author : <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @date : 2020-09-16
 * @version : 1.0.0-RELEASE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.apche.dubbo.cglib.proxy;

import net.sf.cglib.proxy.Enhancer;

/**
 * @program: dubbo-parent
 * @description: CGlib 代理类生成工厂
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-16
 */
public class ClassProxyFactory {

    // 创建一个生成器对象
    private final Enhancer enhancer = new Enhancer();

    /**
     *  生成某个实例类的代理对象
     * @param target 代理对象
     * @param <T> 某个具体的类
     * @return 代理后的类
     */
    public <T> T newProxy(Class<T> target){
        if (target.isInterface()){
            throw new RuntimeException("not support interface");
        }
        // 设置需要代理的类对象
        enhancer.setSuperclass(target);
        // 设置回调的拦截器CallBack
        enhancer.setCallback(new ClassMethodInterceptor());
        // 通过ASM字节码编辑技术创建生成的子类
        return (T)enhancer.create();
    }

}
