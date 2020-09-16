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
package org.apche.dubbo.javassist;

import javassist.*;

import java.lang.reflect.Method;

/**
 * @program: dubbo-parent
 * @description: Javassist
 * @author: <a href="https://github.com/lcy2013">MagicLuo(扶风)</a>
 * @create: 2020-09-16
 */
public class JavassistApplication {

    public static void main(String[] args) throws Exception{
        // 创建ClassPool
        ClassPool cp = ClassPool.getDefault();
        // 要生成的类名称为org.fufeng.JavassistProxy
        CtClass clazz = cp.makeClass("org.fufeng.JavassistProxy");

        StringBuffer body;
        // 创建字段，指定了字段类型、字段名称、字段所属的类
        CtField field = new CtField(cp.get("java.lang.String"),
                "prop", clazz);
        // 指定该字段使用private修饰
        field.setModifiers(Modifier.PRIVATE);

        // 设置prop字段的getter/setter方法
        clazz.addMethod(CtNewMethod.setter("getProp", field));
        clazz.addMethod(CtNewMethod.getter("setProp", field));
        // 设置prop字段的初始化值，并将prop字段添加到clazz中
        clazz.addField(field, CtField.Initializer.constant("MyName"));

        // 创建构造方法，指定了构造方法的参数类型和构造方法所属的类
        CtConstructor ctConstructor = new CtConstructor(
                new CtClass[]{}, clazz);
        // 设置方法体
        body = new StringBuffer();
        body.append("{\n prop=\"MyName\";\n}");
        ctConstructor.setBody(body.toString());
        clazz.addConstructor(ctConstructor); // 将构造方法添加到clazz中

        // 创建execute()方法，指定了方法返回值、方法名称、方法参数列表以及
        // 方法所属的类
        CtMethod ctMethod = new CtMethod(CtClass.voidType, "execute",
                new CtClass[]{}, clazz);
        // 指定该方法使用public修饰
        ctMethod.setModifiers(Modifier.PUBLIC);
        // 设置方法体
        body = new StringBuffer();
        body.append("{\n System.out.println(\"execute():\" " +
                "+ this.prop);");
        body.append("\n}");
        ctMethod.setBody(body.toString());
        // 将execute()方法添加到clazz中
        clazz.addMethod(ctMethod);
        // 将上面定义的JavassistDemo类保存到指定的目录
        clazz.writeFile("./javassist");
        // 加载clazz类，并创建对象
        Class<?> c = clazz.toClass();
        Object o = c.newInstance();
        // 调用execute()方法
        Method method = o.getClass().getMethod("execute",
                new Class[]{});
        method.invoke(o, new Object[]{});
    }

}
