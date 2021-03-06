1、 构建源码调试环境
```
dubbo源码官方地址: https://github.com/apache/dubbo
    1、fork 官方dubbo仓库到自己的仓库
    2、git clone git@github.com:[自己仓库名称]/dubbo.git
    3、到当前clone的目录(含有pom.xml)中，执行mvn clean install -Dmaven.test.skip=true
    4、将项目转换成idea项目 mvn idea:idea(如果需要失败可以利用 mvn idea:workspace)
    5、切换分支到目前主流分支
        git fetch   拉去所有相关的远程仓储到本地仓储
        git checkout -b 2.7.8-release  切换到release分支
```

2、 项目结构概览
```
dubbo-common :  Dubbo 的一个公共模块，其中有很多工具类以及公共逻辑，例如 Dubbo SPI 实现、时间轮实现、动态编译器等。
    org.apache.dubbo.common.compiler : 动态编译相关
    org.apache.dubbo.common.config : 配置相关
    org.apache.dubbo.common.constants : 通用常量定义(如url参数的key)
    org.apache.dubbo.common.convert : 类型转换器
    org.apache.dubbo.common.extension : SPI 核心实现
    org.apache.dubbo.common.io : IO相关实现类
    org.apache.dubbo.common.logger : 多种java日志集成
    org.apache.dubbo.common.threadlocal : ThreadLocal相关工具类
    org.apache.dubbo.common.threadpool : 线程池相关工具类
    org.apache.dubbo.common.timer : 时间轮训器工具
    org.apache.dubbo.common.utils : 通用工具类
dubbo-remoting : Dubbo 的远程通信模块，其中的子模块依赖各种开源组件实现远程通信。
    在 dubbo-remoting-api 子模块中定义该模块的抽象概念，在其他子模块中依赖其他开源组件进行实现，
    例如:
        dubbo-remoting-netty4 子模块依赖 Netty 4 实现远程通信，
        dubbo-remoting-etcd3 子模块依赖 etcd 3 实现远程通信，
        dubbo-remoting-p2p 子模块依赖 p2p 实现远程通信，
        dubbo-remoting-redis 子模块依赖 redis 实现远程通信，
        dubbo-remoting-zookeeper 通过 Apache Curator 实现与 ZooKeeper 集群的交互。
dubbo-rpc : Dubbo 中对远程调用协议进行抽象的模块，其中抽象了各种协议，依赖于 dubbo-remoting 模块的远程调用功能。
    dubbo-rpc-api 子模块是核心抽象，其他子模块是针对具体协议的实现。
    例如:
        dubbo-rpc-dubbo 子模块是对 Dubbo 协议的实现，依赖了 dubbo-remoting-netty4 等 dubbo-remoting 子模块。
        dubbo-rpc 模块的实现中只包含一对一的调用，不关心集群的相关内容。
dubbo-cluster :  Dubbo 中负责管理集群的模块，提供了负载均衡、容错、路由等一系列集群相关的功能，
    最终的目的是将多个 Provider 伪装为一个 Provider，这样 Consumer 就可以像调用一个 Provider 那样调用 Provider 集群了。
dubbo-registry : Dubbo 中负责与多种开源注册中心进行交互的模块，提供注册中心的能力。
    其中， dubbo-registry-api 子模块是顶层抽象，其他子模块是针对具体开源注册中心组件的具体实现，
    例如，dubbo-registry-zookeeper 子模块是 Dubbo 接入 ZooKeeper 的具体实现。
dubbo-monitor : Dubbo 的监控模块，主要用于统计服务调用次数、调用时间以及实现调用链跟踪的服务。
dubbo-config :  Dubbo 对外暴露的配置都是由该模块进行解析的。
    例如:
    dubbo-config-api 子模块负责处理 API 方式使用时的相关配置
    dubbo-config-spring 子模块负责处理与 Spring 集成使用时的相关配置方式。
    有了 dubbo-config 模块，用户只需要了解 Dubbo 配置的规则即可，无须了解 Dubbo 内部的细节。
dubbo-metadata : Dubbo 的元数据模块。
    dubbo-metadata 模块的实现套路也是有一个 api 子模块进行抽象，然后其他子模块进行具体实现。
dubbo-configcenter : Dubbo 的动态配置模块，主要负责外部化配置以及服务治理规则的存储与通知，提供了多个子模块用来接入多种开源的服务发现组件。
```
   
3、 解析Dubbo配置总线 URL（Uniform Resource Locator，统一资源定位符）
```
URL 本质上就是一个特殊格式的字符串。一个标准的 URL 格式可以包含如下的几个部分：
    protocol://username:password@host:port/path?key=value&key=value
    1、 protocol：URL 的协议。我们常见的就是 HTTP 协议和 HTTPS 协议，当然，还有其他协议，如 FTP 协议、SMTP 协议等。
    2、username/password：用户名/密码。 HTTP Basic Authentication 中多会使用在 URL 的协议之后直接携带用户名和密码的方式。
    3、host/port：主机/端口。在实践中一般会使用域名，而不是使用具体的 host 和 port。
    4、path：请求的路径。
    5、parameters：参数键值对。一般在 GET 请求中会将参数放到 URL 中，POST 请求会将参数放到请求体中。

Dubbo中的URL : dubbo://192.168.32.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&pid=32508&release=&side=provider&timestamp=1593253404714dubbo://172.17.32.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&pid=32508&release=&side=provider&timestamp=1593253404714
    这个 Demo Provider 注册到 ZooKeeper 上的 URL 信息，简单解析一下这个 URL 的各个部分：
    protocol：dubbo 协议。
    username/password：没有用户名和密码。
    host/port：172.17.32.91:20880。
    path：org.apache.dubbo.demo.DemoService。
    parameters：参数键值对。

源码位置: org.apache.dubbo.common.URL.URL(java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, java.lang.String, java.util.Map<java.lang.String,java.lang.String>, java.util.Map<java.lang.String,java.util.Map<java.lang.String,java.lang.String>>)
    在 dubbo-common 包中还提供了 URL 的辅助类：
        URLBuilder: 辅助构造 URL。
        URLStrParser: 将字符串解析成 URL 对象。

URL 在SPI中的应用
    Dubbo SPI 中有一个依赖 URL 的重要场景——适配器方法，是被 @Adaptive 注解标注的， URL 一个很重要的作用就是与 @Adaptive 注解一起选择合适的扩展实现类。
    例如: 在 dubbo-registry-api 模块中我们可以看到 RegistryFactory 这个接口，其中的 getRegistry() 方法上有 @Adaptive({"protocol"}) 注解，
         说明这是一个适配器方法，Dubbo 在运行时会为其动态生成相应的 “$Adaptive” 类型，如下所示:
         public class RegistryFactory$Adaptive
                       implements RegistryFactory {
             public Registry getRegistry(org.apache.dubbo.common.URL arg0) {
                 if (arg0 == null) throw new IllegalArgumentException("...");
                 org.apache.dubbo.common.URL url = arg0;
                 // 尝试获取URL的Protocol，如果Protocol为空，则使用默认值"dubbo"
                 String extName = (url.getProtocol() == null ? "dubbo" :
                      url.getProtocol());
                 if (extName == null)
                     throw new IllegalStateException("...");
                 // 根据扩展名选择相应的扩展实现，Dubbo SPI的核心原理
                 RegistryFactory extension = (RegistryFactory) ExtensionLoader
                   .getExtensionLoader(RegistryFactory.class)
                         .getExtension(extName);
                 return extension.getRegistry(arg0);
             }
         }
    我们会看到，在生成的 RegistryFactory$Adaptive 类中会自动实现 getRegistry() 方法，其中会根据 URL 的 Protocol 确定扩展名称，从而确定使用的具体扩展实现类。
    我们可以找到 RegistryProtocol 这个类，并在其 getRegistry() 方法中打一个断点， Debug 启动Demo 示例中的 Provider，得到如下图所示的内容：
        传入的值是URL中的registryUrl : service-discovery-registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&export=dubbo%3A%2F%2F192.168.0.91%3A20880%2Forg.apache.dubbo.demo.GreetingService%3Fanyhost%3Dtrue%26application%3Ddemo-provider%26bind.ip%3D192.168.0.91%26bind.port%3D20880%26deprecated%3Dfalse%26dubbo%3D2.0.2%26dynamic%3Dtrue%26generic%3Dfalse%26group%3Dgreeting%26interface%3Dorg.apache.dubbo.demo.GreetingService%26mapping-type%3Dmetadata%26mapping.type%3Dmetadata%26metadata-type%3Dremote%26methods%3Dhello%26pid%3D2417%26qos.port%3D22222%26release%3D%26revision%3D1.0.0%26side%3Dprovider%26timeout%3D5000%26timestamp%3D1600050456247%26version%3D1.0.0&id=registry1&mapping-type=metadata&mapping.type=metadata&pid=2417&qos.port=22222&registry=zookeeper&registry-type=service&timestamp=1600050456218
        通过URL获取的Registry中的registryUrl : zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.2&id=registry1&interface=org.apache.dubbo.registry.RegistryService&mapping-type=metadata&mapping.type=metadata&pid=2417&qos.port=22222&registry-type=service&timestamp=1600050456218

URL 在服务暴露中的应用
    Provider 在启动时，会将自身暴露的服务注册到 ZooKeeper 上，具体是注册哪些信息到 ZooKeeper 上呢？
    来看 ZookeeperRegistry.doRegister() 方法，在其中打个断点，然后 Debug 启动 Provider:
        url : dubbo://192.168.0.91:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-annotation-provider&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&pid=2579&release=&side=provider&timestamp=1600051163018
        toUrlPath(url) : /dubbo/org.apache.dubbo.demo.DemoService/providers/dubbo%3A%2F%2F192.168.0.91%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fanyhost%3Dtrue%26application%3Ddubbo-demo-annotation-provider%26deprecated%3Dfalse%26dubbo%3D2.0.2%26dynamic%3Dtrue%26generic%3Dfalse%26interface%3Dorg.apache.dubbo.demo.DemoService%26methods%3DsayHello%2CsayHelloAsync%26pid%3D2579%26release%3D%26side%3Dprovider%26timestamp%3D1600051163018
    传入的 URL 中包含了 Provider 的地址（192.168.0.91:20880）、暴露的接口（org.apache.dubbo.demo.DemoService）等信息， toUrlPath() 方法会根据传入的 URL 参数确定在 ZooKeeper 上创建的节点路径，还会通过 URL 中的 dynamic 参数值确定创建的 ZNode 是临时节点还是持久节点。

URl 在服务订阅中的应用
    Consumer 启动后会向注册中心进行订阅操作，并监听自己关注的 Provider。那 Consumer 是如何告诉注册中心自己关注哪些 Provider 呢？
    我们来看 ZookeeperRegistry 这个实现类，它是由上面的 ZookeeperRegistryFactory 工厂类创建的 Registry 接口实现，
    其中的 doSubscribe() 方法是订阅操作的核心实现，在方法里面打一个断点，并 Debug 启动 Demo 中 Consumer，会得到下图所示的内容：
        传入的url : consumer://192.168.0.91/org.apache.dubbo.demo.DemoService?application=dubbo-demo-annotation-consumer&category=providers,configurators,routers&dubbo=2.0.2&id=org.apache.dubbo.config.RegistryConfig#0&init=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&pid=2654&side=consumer&sticky=false&timestamp=1600051729106
        其中 Protocol 为 consumer ，表示是 Consumer 的订阅协议，其中的 category 参数表示要订阅的分类，这里要订阅 providers、configurators 以及 routers 三个分类；interface 参数表示订阅哪个服务接口，这里要订阅的是暴露 org.apache.dubbo.demo.DemoService 实现的 Provider。
        toCategoriesPath(url) : 0 = "/dubbo/org.apache.dubbo.demo.DemoService/providers"
                                1 = "/dubbo/org.apache.dubbo.demo.DemoService/configurators"
                                2 = "/dubbo/org.apache.dubbo.demo.DemoService/routers"
```
    
4、 JDK SPI机制
```
SPI 机制原理
    SPI（Service Provider Interface）主要是被框架开发人员使用的一种技术。
    例如，使用 Java 语言访问数据库时我们会使用到 java.sql.Driver 接口，不同数据库产品底层的协议不同，提供的 java.sql.Driver 实现也不同，
      在开发 java.sql.Driver 接口时，开发人员并不清楚用户最终会使用哪个数据库，在这种情况下就可以使用 Java SPI 机制在实际运行过程中，为 java.sql.Driver 接口寻找具体的实现。

JDK SPI原理
    当服务的提供者提供了一种接口的实现之后，需要在 Classpath 下的 META-INF/services/ 目录里创建一个以服务接口命名的文件，此文件记录了该 jar 包提供的服务接口的具体实现类。
    当某个应用引入了该 jar 包且需要使用该服务时，JDK SPI 机制就可以通过查找这个 jar 包的 META-INF/services/ 中的配置文件来获得具体的实现类名，进行实现类的加载和实例化，最终使用该实现类完成业务功能。
    实例代码dubbo-demo/spi

JDK SPI源码分析
    java.util.ServiceLoader.load(java.lang.Class<S>)
    ClassLoader(Thread.currentThread().getContextClassLoader())
        -> if(null) ClassLoader(ClassLoader.getSystemClassLoader())
            -> java.util.ServiceLoader.reload
                // 用来存放ServiceLoader创建的对象
                private LinkedHashMap<String,S> providers = new LinkedHashMap<>();
                -> providers.clear() 清空缓存
                    -> lookupIterator = new LazyIterator(service, loader) 创建一个加载SPI的迭代器

    由java.util.ServiceLoader.LazyIterator.hasNextService去查询META-INF下面是否还存在接口实现
    由java.util.ServiceLoader.LazyIterator.nextService去创建该接口的实现

JDK SPI机制在JDBC中的应用
    JDK 中只定义了一个 java.sql.Driver 接口，具体的实现是由不同数据库厂商来提供的。以 MySQL 提供的 JDBC 实现包为例进行分析。
    在 mysql-connector-java-*.jar 包中的 META-INF/services 目录下，有一个 java.sql.Driver 文件中只有一行内容，如下所示：
        com.mysql.cj.jdbc.Driver
    创建连接的语句如下:
        String url = "jdbc:xxx://xxx:xxx/xxx";
        Connection conn = DriverManager.getConnection(url, username, pwd);
    DriverManager 是JDK提供的驱动管理设备具体调用如下:
        java.sql.DriverManager#ensureDriversInitialized

```
5、Dubbo SPI机制
```
SPI扩展点介绍:
    1、扩展点：通过 SPI 机制查找并加载实现的接口（又称“扩展接口”）。 Logger 接口、com.mysql.cj.jdbc.Driver 接口，都是扩展点。
    2、扩展点实现：实现了扩展接口的实现类。

    JDK SPI 在查找扩展实现类的过程中，需要遍历 SPI 配置文件中定义的所有实现类，该过程中会将这些实现类全部实例化。
如果 SPI 配置文件中定义了多个实现类，而我们只需要使用其中一个实现类时，就会生成不必要的对象。
例如，org.apache.dubbo.rpc.Protocol 接口有 InjvmProtocol、DubboProtocol、RmiProtocol、HttpProtocol、HessianProtocol、ThriftProtocol 等多个实现，如果使用 JDK SPI，就会加载全部实现类，导致资源的浪费。
Dubbo SPI 不仅解决了上述资源浪费的问题，还对 SPI 配置文件扩展和修改。

Dubbo 按照 SPI 配置文件的用途，将其分成了三类目录:
    1、META-INF/services/ 目录：该目录下的 SPI 配置文件用来兼容 JDK SPI 。
    2、META-INF/dubbo/ 目录：该目录用于存放用户自定义 SPI 配置文件。
    3、META-INF/dubbo/internal/ 目录：该目录用于存放 Dubbo 内部使用的 SPI 配置文件。
  Dubbo 将 SPI 配置文件改成了 KV 格式
    K : 扩展名称
    V : 扩展实现

Dubbo @SPI 注解
    dubbo 中某个接口被标注了@SPI，就说明这个接口是一个扩展接口，如: org.apache.dubbo.rpc.Protocol。
    @SPI 注解的 value 值指定了默认的扩展名称，
    例如，在通过 Dubbo SPI 加载 Protocol 接口实现时，如果没有明确指定扩展名，则默认会将 @SPI 注解的 value 值作为扩展名，即加载 dubbo 这个扩展名对应的 org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol 这个扩展实现类，相关的 SPI 配置文件在 dubbo-rpc-dubbo 模块中
    那 ExtensionLoader 是如何处理 @SPI 注解的呢？
        org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader
            -> org.apache.dubbo.common.extension.ExtensionLoader.withExtensionAnnotation
        ExtensionLoader 中三个核心的静态字段:
            1、strategies（LoadingStrategy[]类型）: LoadingStrategy 接口有三个实现（通过 JDK SPI 方式加载的），
                如下所示，分别对应前面介绍的三个 Dubbo SPI 配置文件所在的目录，且都继承了 Prioritized 这个优先级接口，默认优先级是
                   DubboInternalLoadingStrategy > DubboLoadingStrategy > ServicesLoadingStrategy
            2、EXTENSION_LOADERS（ConcurrentMap<Class<?>, ExtensionLoader<?>>）: Dubbo 中一个扩展接口对应一个 ExtensionLoader 实例，
                该集合缓存了全部 ExtensionLoader 实例，其中的 Key 为扩展接口，Value 为加载其扩展实现的 ExtensionLoader 实例。
            3、EXTENSION_INSTANCES（ConcurrentMap<Class<?>, Object>类型）：该集合缓存了扩展实现类与其实例对象的映射关系。
                如: Key 为 Class，Value 为 DubboProtocol 对象。
        ExtensionLoader 中的其他字段:
            1、type（Class<?>类型）：当前 ExtensionLoader 实例负责加载扩展接口。
            2、cachedDefaultName（String类型）：记录了 type 这个扩展接口上 @SPI 注解的 value 值，也就是默认扩展名。
            3、cachedNames（ConcurrentMap<Class<?>, String>类型）：缓存了该 ExtensionLoader 加载的扩展实现类与扩展名之间的映射关系。
            4、cachedClasses（Holder<Map<String, Class<?>>>类型）：缓存了该 ExtensionLoader 加载的扩展名与扩展实现类之间的映射关系。cachedNames 集合的反向关系缓存。
            5、cachedInstances（ConcurrentMap<String, Holder<Object>>类型）：缓存了该 ExtensionLoader 加载的扩展名与扩展实现对象之间的映射关系。
    ExtensionLoader 核心方法:
        1、org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader() 获取并创建指定类型的ExtensionLoader对象
        2、org.apache.dubbo.common.extension.ExtensionLoader.createExtension SPI 配置文件的查找以及相应扩展实现类的实例化，同时还实现了自动装配以及自动 Wrapper 包装等功能

@Adaptive 注解与适配器
    @Adaptive 注解用来实现 Dubbo 的适配器功能，那什么是适配器呢？
        Dubbo 中的 ExtensionFactory 接口有三个实现类: SpiExtensionFactory、SpringExtensionFactory、AdaptiveExtensionFactory
        如下所示，ExtensionFactory 接口上有 @SPI 注解，AdaptiveExtensionFactory 实现类上有 @Adaptive 注解。
            @SPI
            ExtensionFactory
                             ->  SpiExtensionFactory
                             ->  SpringExtensionFactory
                                 @Adaptive
                             ->  AdaptiveExtensionFactory
        AdaptiveExtensionFactory 不实现任何具体的功能，而是用来适配 ExtensionFactory 的 SpiExtensionFactory 和 SpringExtensionFactory 这两种实现。
        AdaptiveExtensionFactory 会根据运行时的一些状态来选择具体调用 ExtensionFactory 的哪个实现。

        @Adaptive 注解还可以加到接口方法之上，Dubbo 会动态生成适配器类。
        例如，Transporter 接口有两个被 @Adaptive 注解修饰的方法
            @Adaptive({Constants.SERVER_KEY, Constants.TRANSPORTER_KEY})
            RemotingServer bind(URL url, ChannelHandler handler) throws RemotingException;
            @Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
            Client connect(URL url, ChannelHandler handler) throws RemotingException;
        Dubbo 会生成一个 Transporter$Adaptive 适配器类，该类继承了 Transporter 接口：
            public class Transporter$Adaptive implements Transporter {
                public org.apache.dubbo.remoting.Client connect(URL arg0, ChannelHandler arg1) throws RemotingException {
                    // 必须传递URL参数
                    if (arg0 == null) throw new IllegalArgumentException("url == null");
                    URL url = arg0;
                    // 确定扩展名，优先从URL中的client参数获取，其次是transporter参数
                    // 这两个参数名称由@Adaptive注解指定，最后是@SPI注解中的默认值
                    String extName = url.getParameter("client",
                        url.getParameter("transporter", "netty"));
                    if (extName == null)
                        throw new IllegalStateException("...");
                    // 通过ExtensionLoader加载Transporter接口的指定扩展实现
                    Transporter extension = (Transporter) ExtensionLoader
                          .getExtensionLoader(Transporter.class)
                                .getExtension(extName);
                    return extension.connect(arg0, arg1);
                }
                ... // 省略bind()方法
            }
        生成 Transporter$Adaptive 这个类的逻辑位于 ExtensionLoader.createAdaptiveExtensionClass() 方法，其中涉及的 javassist 等方面的知识。
        ExtensionLoader.createExtension() 方法，其中在扫描 SPI 配置文件的时候，会调用 loadClass() 方法加载 SPI 配置文件中指定的类。
        loadClass() 方法中会识别加载扩展实现类上的 @Adaptive 注解，将该扩展实现的类型缓存到 cachedAdaptiveClass 这个实例字段上（volatile修饰）。

自动包装特性
    Dubbo 中的一个扩展接口可能有多个扩展实现类，这些扩展实现类可能会包含一些相同的逻辑，如果在每个实现类中都写一遍，那么这些重复代码就会变得很难维护。
    Dubbo 提供的自动包装特性，就可以解决这个问题。 Dubbo 将多个扩展实现类的公共逻辑，抽象到 Wrapper 类中，
    Wrapper 类与普通的扩展实现类一样，也实现了扩展接口，在获取真正的扩展实现对象时，在其外面包装一层 Wrapper 对象，可以看成一层装饰器。
     代码: ExtensionLoader.loadClass()
         ...
         if (clazz.isAnnotationPresent(Adaptive.class)) {
             cacheAdaptiveClass(clazz, overridden);
             // 在 isWrapperClass() 方法中，会判断该扩展实现类是否包含拷贝构造函数（即构造函数只有一个参数且为扩展接口类型），
             // 如果包含，则为 Wrapper 类，这就是判断 Wrapper 类的标准。
         } else if (isWrapperClass(clazz)) {
             // 将 Wrapper 类记录到 cachedWrapperClasses（Set<Class<?>>类型）这个实例字段中进行缓存。
             cacheWrapperClass(clazz);
         }
         ...

自动装配特性
    在 ExtensionLoader.createExtension() 方法中我们看到，Dubbo SPI 在拿到扩展实现类的对象（以及 Wrapper 类的对象）之后，
    还会调用 injectExtension() 方法扫描其全部 setter 方法，并根据 setter 方法的名称以及参数的类型，加载相应的扩展实现，
    然后调用相应的 setter 方法填充属性，这就实现了 Dubbo SPI 的自动装配特性。简单来说，自动装配属性就是在加载一个扩展点的时候，将其依赖的扩展点一并加载，并进行装配。
    具体方法: ExtensionLoader.injectExtension()
        injectExtension() 方法实现的自动装配依赖了 ExtensionFactory（即 objectFactory 字段），
        ExtensionFactory 有 SpringExtensionFactory 和 SpiExtensionFactory 两个真正的实现（还有一个实现是 AdaptiveExtensionFactory 是适配器）。
        1、SpringExtensionFactory: 将属性名称作为 Spring Bean 的名称，从 Spring 容器中获取 Bean
        2、SpiExtensionFactory: 根据扩展接口获取相应的适配器，没有用到属性名称


 @Activate注解与自动激活特性
    以 Dubbo 中的 Filter 为例说明自动激活特性的含义，org.apache.dubbo.rpc.Filter 接口有非常多的扩展实现类，
    在一个场景中可能需要某几个 Filter 扩展实现类协同工作，而另一个场景中可能需要另外几个实现类一起工作。
    这样，就需要一套配置来指定当前场景中哪些 Filter 实现是可用的，这就是 @Activate 注解要做的事情。
    @Activate 注解标注在扩展实现类上，有 group、value 以及 order 三个属性。
        1、group 属性：修饰的实现类是在 Provider 端被激活还是在 Consumer 端被激活。
        2、value 属性：修饰的实现类只在 URL 参数中出现指定的 key 时才会被激活。
        3、order 属性：用来确定扩展实现类的排序。
    代码处理: ExtensionLoader.loadClass()
    使用地方: ExtensionLoader.getActivateExtension()
        首先，获取默认激活的扩展集合。默认激活的扩展实现类有几个条件：
            1、在 cachedActivates 集合中存在；
            2、@Activate 注解指定的 group 属性与当前 group 匹配；
            3、扩展名没有出现在 values 中（即未在配置中明确指定，也未在配置中明确指定删除）；
            4、URL 中出现了 @Activate 注解中指定的 Key。
        然后，按照 @Activate 注解中的 order 属性对默认激活的扩展集合进行排序。
        最后，按序添加自定义扩展实现类的对象。
      举例:
        扩展名称          @Activate中的group          @Activate中的order
        filter1            Provider                     6
        filter2            Provider                     5
        filter3            Provider                     4
        filter4            Provider                     3
        filter5            Consumer                     2
        filter6            Provider                     1
        在 Provider 端调用 getActivateExtension() 方法时传入的 values 配置为 "filter3、-filter2、default、filter1"，那么根据上面的逻辑：
        得到默认激活的扩展实实现集合中有 [ filter4, filter6 ]；
        排序后为 [ filter6, filter4 ]；
        按序添加自定义扩展实例之后得到 [ filter3, filter6, filter4, filter1 ]。

```
    
6、Dubbo中的定时任务
```
    JDK 提供的 java.util.Timer 和 DelayedQueue 等工具类，可以帮助我们实现简单的定时任务管理，其底层实现使用的是堆这种数据结构，存取操作的复杂度都是 O(nlog(n))，
无法支持大量的定时任务。在定时任务量比较大、性能要求比较高的场景中，为了将定时任务的存取操作以及取消操作的时间复杂度降为 O(1)，一般会使用时间轮的方式。

时间轮是一种高效的、批量管理定时任务的调度模型。
    时间轮一般会实现成一个环形结构，类似一个时钟，分为很多槽，一个槽代表一个时间间隔，每个槽使用双向链表存储定时任务；
    指针周期性地跳动，跳动到一个槽位，就执行该槽位的定时任务。

需要注意的是，单层时间轮的容量和精度都是有限的，对于精度要求特别高、时间跨度特别大或是海量定时任务需要调度的场景，通常会使用多级时间轮以及持久化存储与时间轮结合的方案。

dubbo实现timer位置: dubbo-common 模块的 org.apache.dubbo.common.timer 包
    核心接口:
        1、org.apache.dubbo.common.timer.Timer
        2、org.apache.dubbo.common.timer.TimerTask
        3、org.apache.dubbo.common.timer.Timeout
    具体实现类:
        HashedWheelTimer
    分析时间轮指针一次转动的全流程(HashedWheelTimer$Worker.run()):
        1、时间轮指针转动，时间轮周期开始。
        2、清理用户主动取消的定时任务，这些定时任务在用户取消时，会记录到 cancelledTimeouts 队列中。在每次指针转动的时候，时间轮都会清理该队列。
        3、将缓存在 timeouts 队列中的定时任务转移到时间轮中对应的槽中。
        4、根据当前指针定位对应槽，处理该槽位的双向链表中的定时任务。
        5、检测时间轮的状态。如果时间轮处于运行状态，则循环执行上述步骤，不断执行定时任务。
            如果时间轮处于停止状态，则执行下面的步骤获取到未被执行的定时任务并加入 unprocessedTimeouts 队列：遍历时间轮中每个槽位，
            并调用 clearTimeouts() 方法；对 timeouts 队列中未被加入槽中循环调用 poll()。
        6、最后再次清理 cancelledTimeouts 队列中用户主动取消的定时任务。

dubbo中如何使用定时任务?
        在 Dubbo 中，时间轮并不直接用于周期性操作，而是只向时间轮提交执行单次的定时任务，在上一次任务执行完成的时候，调用 newTimeout() 方法再次提交当前任务，
    这样就会在下个周期执行该任务。即使在任务执行过程中出现了 GC、I/O 阻塞等情况，导致任务延迟或卡住，也不会有同样的任务源源不断地提交进来，导致任务堆积。
    Dubbo 中对时间轮的应用主要体现在如下两个方面：
        1、失败重试， 例如，Provider 向注册中心进行注册失败时的重试操作，或是 Consumer 向注册中心订阅时的失败重试等。
        2、周期性定时任务， 例如，定期发送心跳请求，请求超时的处理，或是网络连接断开后的重连机制。

```
        
7、dubbo注册模块（dubbo-registry）
```
Dubbo 目前支持 Consul、etcd、Nacos、ZooKeeper、Redis 等多种开源组件作为注册中心，并且在 Dubbo 源码也有相应的接入模块。

Dubbo 官方推荐使用 ZooKeeper 作为注册中心: ZkClient,Apache Curator 是实践中最常用的 ZooKeeper 客户端，dubbo 使用Apache Curator作为客户端。

Apache ZooKeeper 是一个针对分布式系统的、可靠的、可扩展的协调服务，它通常作为统一命名服务、统一配置管理、注册中心（分布式集群管理）、分布式锁服务、Leader 选举服务等角色出现。
很多分布式系统都依赖与 ZooKeeper 集群实现分布式系统间的协调调度，例如：Dubbo、HDFS 2.x、HBase、Kafka 等。ZooKeeper 已经成为现代分布式系统的标配。

Apache Curator 是 Apache 基金会提供的一款 ZooKeeper 客户端，它提供了一套易用性和可读性非常强的 Fluent 风格的客户端 API ，可以帮助我们快速搭建稳定可靠的 ZooKeeper 客户端程序。
```

8、动态代理机制 示例代码(dubbo-demo/proxy)
```
动态代理机制在 Java 中有着广泛的应用，例如，Spring AOP、MyBatis、Hibernate 等常用的开源框架，都使用到了动态代理机制。
当然，Dubbo 中也使用到了动态代理，在开发简易版 RPC 框架的时候，可以参考 Dubbo 使用动态代理机制来屏蔽底层的网络传输以及服务发现的相关实现。

代理模式: 是23种设计模式中常用的

静态代理
    interface Subject [method operation()]
                        -> class RealSubject
                        -> class Proxy [ hold(RealSubject) ]
    Subject 是程序中的业务逻辑接口，RealSubject 是实现了 Subject 接口的真正业务类，Proxy 是实现了 Subject 接口的代理类，
    封装了一个 RealSubject 引用。在程序中不会直接调用 RealSubject 对象的方法，而是使用 Proxy 对象实现相关功能。

    Proxy.operation() 方法的实现会调用其中封装的 RealSubject 对象的 operation() 方法，执行真正的业务逻辑。
    代理的作用不仅仅是正常地完成业务逻辑，还会在业务逻辑前后添加一些代理逻辑，也就是说，
    Proxy.operation() 方法会在 RealSubject.operation() 方法调用前后进行一些预处理以及一些后置处理。这就是我们常说的“代理模式”。

    使用代理模式可以控制程序对 RealSubject 对象的访问，如果发现异常的访问，可以直接限流或是返回，
    也可以在执行业务处理的前后进行相关的预处理和后置处理，帮助上层调用方屏蔽底层的细节。
    例如，在 RPC 框架中，代理可以完成序列化、网络 I/O 操作、负载均衡、故障恢复以及服务发现等一系列操作，而上层调用方只感知到了一次本地调用。

    代理模式还可以用于实现延迟加载的功能。我们知道查询数据库是一个耗时的操作，而有些时候查询到的数据也并没有真正被程序使用。
    延迟加载功能就可以有效地避免这种浪费，系统访问数据库时，首先可以得到一个代理对象，此时并没有执行任何数据库查询操作，代理对象中自然也没有真正的数据；
    当系统真正需要使用数据时，再调用代理对象完成数据库查询并返回数据。常见 ORM 框架（例如，MyBatis、 Hibernate）中的延迟加载的原理大致也是如此。


JDK动态代理
    核心类执行流程:
        1、java.lang.reflect.InvocationHandler 代理类拦截处理器
        2、java.lang.reflect.Proxy
            -> java.lang.reflect.Proxy.newProxyInstance 创建proxy实例
                -> java.lang.reflect.Proxy.getProxyClass0 通过WeakCache获取proxy class
                    -> java.lang.reflect.WeakCache.get 先通过缓存拿class subKeyFactory.apply(key, parameter);
                        -> java.lang.reflect.Proxy.ProxyClassFactory.apply 创建真正的class文件和instance
                            -> sun.misc.ProxyGenerator.generateProxyClass
                                需要添加JVM参数信息 -Dsun.misc.ProxyGenerator.saveGeneratedFiles=true将生成的代理clss输出到磁盘
                                具体输出的位置是 当前Project/com/sun/proxy/$Proxy0.class
    原理: JDK 动态代理的实现原理是动态创建代理类并通过指定类加载器进行加载，在创建代理对象时将InvocationHandler对象作为构造参数传入。
         当调用代理对象时，会调用 InvocationHandler.invoke() 方法，从而执行代理逻辑，并最终调用真正业务对象的相应方法。

CGlib动态代理
    CGLib（Code Generation Library）是一个基于 ASM 的字节码生成库，它允许我们在运行时对字节码进行修改和动态生成。
    CGLib 采用字节码技术实现动态代理功能，其底层原理是通过字节码技术为目标类生成一个子类，并在该子类中采用方法拦截的方式拦截所有父类方法的调用，从而实现代理的功能。

    因为 CGLib 使用生成子类的方式实现动态代理，所以无法代理 final 关键字修饰的方法（因为final 方法是不能够被重写的）。
    这样的话，CGLib 与 JDK 动态代理之间可以相互补充：在目标类实现接口时，使用 JDK 动态代理创建代理对象；
    当目标类没有实现接口时，使用 CGLib 实现动态代理的功能。在 Spring、MyBatis 等多种开源框架中，都可以看到JDK动态代理与 CGLib 结合使用的场景。

    CGLib 的实现有两个重要的成员组成:
        1、Enhancer：指定要代理的目标对象以及实际处理代理逻辑的对象，最终通过调用 create() 方法得到代理对象，对这个对象所有的非 final 方法的调用都会转发给 MethodInterceptor 进行处理。
        2、MethodInterceptor：动态代理对象的方法调用都会转发到intercept方法进行增强。

Javassist
    Javassist 是一个开源的生成 Java 字节码的类库，其主要优点在于简单、快速，直接使用Javassist 提供的 Java API 就能动态修改类的结构，或是动态生成类。
    Javassist 也可以实现动态代理功能，底层的原理也是通过创建目标类的子类的方式实现的(dubbo-demo/proxy/src/main/java/org/apche/dubbo/javassist)。

Javassist 的性能也比较好，是 Dubbo 默认的代理生成方式。

```
    
9、dubbo网络通信利器 - netty
```
为什么使用netty而不使用java自身的NIO/AIO？
问题:
1、Java NIO 的 API 非常复杂
   要写出成熟可用的 Java NIO 代码，需要熟练掌握 JDK 中的 Selector、ServerSocketChannel、SocketChannel、ByteBuffer 等组件，
   还要理解其中一些反人类的设计以及底层原理，这对新手来说是非常不友好的。

2、如果直接使用 Java NIO 进行开发，难度和开发量会非常大
   我们需要自己补齐很多可靠性方面的实现，例如，网络波动导致的连接重连、半包读写等。
   这就会导致一些本末倒置的情况出现：核心业务逻辑比较简单，但补齐其他公共能力的代码非常多，开发耗时比较长。
   这时就需要一个统一的 NIO 框架来封装这些公共能力了。

3、JDK 自身的 Bug
   其中比较出名的就要属 Epoll Bug 了，这个 Bug 会导致 Selector 空轮询，CPU 使用率达到 100%，这样就会导致业务逻辑无法执行，降低服务性能。

4、Linux平台没有更好的真正异步IO实现
优势:
Netty 在 JDK 自带的 NIO API 基础之上进行了封装，解决了 JDK 自身的一些问题，具备如下优点:
    1、入门简单，使用方便，文档齐全，无其他依赖，只依赖 JDK 就够了
    2、高性能，高吞吐，低延迟，资源消耗少
    3、灵活的线程模型，支持阻塞和非阻塞的I/O 模型
    4、代码质量高，目前主流版本基本没有 Bug

I/O 模型

同步阻塞模型
   同步: 用户空间线程需要等待内核处理的过程叫做同步
   阻塞: 用户空间处理内核空间的东西是串行执行，一个没有处理完成其他就需要阻塞

1. 传统阻塞 I/O模型
在传统阻塞型 I/O 模型（即我们常说的 BIO）中，每个请求都需要独立的线程完成读数据、业务处理以及写回数据的完整操作。
用户空间                        内核空间
  user      ->(read)            kernel
   |                              |
阻塞 |                              |
   |        (read)<-              |
              同步
 Socket(1) -（read/write）- Thread(1)

 2. I/O 多路复用模型
 针对传统的阻塞 I/O 模型的缺点，I/O 复用的模型在性能方面有不小的提升。
 I/O 复用模型中的多个连接会共用一个 Selector 对象，由 Selector 感知连接的读写事件，
 而此时的线程数并不需要和连接数一致，只需要很少的线程定期从 Selector 上查询连接的读写状态即可，
 无须大量线程阻塞等待连接。当某个连接有新的数据可以处理时，操作系统会通知线程，线程从阻塞状态返回，
 开始进行读写操作以及后续的业务逻辑处理,如下:
 用户空间                        内核空间
   user      ->(read)            kernel
    |          <-                  |
非阻塞 |        os 事件通知            |
    |        (read)<-              |
               同步
 Socket(n) -（read/write）- selector(1)
 Socket(n) -（read/write）- selector(1) - Thread(m)

Netty 线程模型设计
服务器程序在读取到二进制数据之后，首先需要通过编解码，得到程序逻辑可以理解的消息，然后将消息传入业务逻辑进行处理，
并产生相应的结果，返回给客户端。编解码逻辑、消息派发逻辑、业务处理逻辑以及返回响应的逻辑，是放到一个线程里面串行执行，
还是分配到不同的线程中执行，会对程序的性能产生很大的影响。所以，优秀的线程模型对一个高性能网络库来说是至关重要的。

Netty 采用了 Reactor 线程模型的设计。
Reactor 模式，也被称为 Dispatcher 模式，核心原理是 Selector 负责监听 I/O 事件，在监听到 I/O 事件之后，分发（Dispatch）给相关线程进行处理。

单Reactor 单线程
Reactor 对象监听客户端请求事件，收到事件后通过 Dispatch 进行分发。如果是连接建立的事件，
则由 Acceptor 通过 Accept 处理连接请求，然后创建一个 Handler 对象处理连接建立之后的业务请求。
如果不是连接建立的事件，而是数据的读写事件，则 Reactor 会将事件分发对应的 Handler 来处理，
由这里唯一的线程调用 Handler 对象来完成读取数据、业务处理、发送响应的完整流程。
当然，该过程中也可能会出现连接不可读或不可写等情况，该单线程会去执行其他 Handler 的逻辑，而不是阻塞等待。

单 Reactor 单线程的优点就是：线程模型简单，没有引入多线程，自然也就没有多线程并发和竞争的问题。

缺点也非常明显，那就是性能瓶颈问题，一个线程只能跑在一个 CPU 上，能处理的连接数是有限的，无法完全发挥多核 CPU 的优势。
一旦某个业务逻辑耗时较长，这唯一的线程就会卡在上面，无法处理其他连接的请求，程序进入假死的状态，可用性也就降低了。
正是由于这种限制，一般只会在客户端使用这种线程模型。

单Reactor 多线程
在单 Reactor 多线程的架构中，Reactor 监控到客户端请求之后，如果连接建立的请求，
则由Acceptor 通过 accept 处理，然后创建一个 Handler 对象处理连接建立之后的业务请求。
如果不是连接建立请求，则 Reactor 会将事件分发给调用连接对应的 Handler 来处理。
到此为止，该流程与单 Reactor 单线程的模型基本一致，唯一的区别就是执行 Handler 逻辑的线程隶属于一个线程池。

很明显，单 Reactor 多线程的模型可以充分利用多核 CPU 的处理能力，提高整个系统的吞吐量，
但引入多线程模型就要考虑线程并发、数据共享、线程调度等问题。
在这个模型中，只有一个线程来处理 Reactor 监听到的所有 I/O 事件，
其中就包括连接建立事件以及读写事件，当连接数不断增大的时候，这个唯一的 Reactor 线程也会遇到瓶颈。

主从Reactor 多线程
为了解决单 Reactor 多线程模型中的问题，我们可以引入多个 Reactor。
其中，Reactor 主线程负责通过 Acceptor 对象处理 MainReactor 监听到的连接建立事件，
当Acceptor 完成网络连接的建立之后，MainReactor 会将建立好的连接分配给 SubReactor 进行后续监听。

当一个连接被分配到一个 SubReactor 之上时，会由 SubReactor 负责监听该连接上的读写事件。
当有新的读事件（OP_READ）发生时，Reactor 子线程就会调用对应的 Handler 读取数据，
然后分发给 Worker 线程池中的线程进行处理并返回结果。
待处理结束之后，Handler 会根据处理结果调用 send 将响应返回给客户端，当然此时连接要有可写事件（OP_WRITE）才能发送数据。

主从 Reactor 多线程的设计模式解决了单一 Reactor 的瓶颈。
主从 Reactor 职责明确，主 Reactor 只负责监听连接建立事件，SubReactor只负责监听读写事件。
整个主从 Reactor 多线程架构充分利用了多核 CPU 的优势，可以支持扩展，而且与具体的业务逻辑充分解耦，复用性高。
但不足的地方是，在交互上略显复杂，需要一定的编程门槛。

netty线程模型（通过配置支持上面的三种线程模型）
Netty 抽象出两组线程池：BossGroup 专门用于接收客户端的连接，WorkerGroup 专门用于网络的读写。
    BossGroup 和 WorkerGroup 类型都是 NioEventLoopGroup，相当于一个事件循环组，
    其中包含多个事件循环 ，每一个事件循环是 NioEventLoop。

NioEventLoop 表示一个不断循环的、执行处理任务的线程，每个 NioEventLoop 都有一个Selector 对象与之对应，
用于监听绑定在其上的连接，这些连接上的事件由 Selector 对应的这条线程处理。
每个 NioEventLoopGroup 可以含有多个 NioEventLoop，也就是多个线程。

每个 Boss NioEventLoop 会监听 Selector 上连接建立的 accept 事件，
然后处理 accept 事件与客户端建立网络连接，生成相应的 NioSocketChannel 对象，
一个 NioSocketChannel 就表示一条网络连接。
之后会将 NioSocketChannel 注册到某个 Worker NioEventLoop 上的 Selector 中。

每个 Worker NioEventLoop 会监听对应 Selector 上的 read/write 事件，
当监听到 read/write 事件的时候，会通过 Pipeline 进行处理。
一个 Pipeline 与一个 Channel 绑定，在 Pipeline 上可以添加多个 ChannelHandler，
每个 ChannelHandler 中都可以包含一定的逻辑，例如编解码等。
Pipeline 在处理请求的时候，会按照我们指定的顺序调用 ChannelHandler。

netty 相关组件
1、Channel
    Channel 是 Netty 对网络连接的抽象，核心功能是执行网络 I/O 操作。
    不同协议、不同阻塞类型的连接对应不同的 Channel 类型。
    我们一般用的都是 NIO 的 Channel，下面是一些常用的 NIO Channel 类型:
        1、NioSocketChannel：对应异步的 TCP Socket 连接。
        2、NioServerSocketChannel：对应异步的服务器端 TCP Socket 连接。
        3、NioDatagramChannel：对应异步的 UDP 连接。

    上述异步 Channel 主要提供了异步的网络 I/O 操作，例如：建立连接、读写操作等。
    异步调用意味着任何 I/O 调用都将立即返回，并且不保证在调用返回时所请求的 I/O 操作已完成。
    I/O 操作返回的是一个 ChannelFuture 对象，无论 I/O 操作是否成功，
    Channel 都可以通过监听器通知调用方，我们通过向 ChannelFuture 上注册监听器来监听 I/O 操作的结果。

    Netty 也支持同步 I/O 操作，但在实践中几乎不使用。
    绝大多数情况下，我们使用的是 Netty 中异步 I/O 操作。
    虽然立即返回一个 ChannelFuture 对象，但不能立刻知晓 I/O 操作是否成功，
    这时我们就需要向 ChannelFuture 中注册一个监听器，当操作执行成功或失败时，监听器会自动触发注册的监听事件。

    另外，Channel 还提供了检测当前网络连接状态等功能，这些可以帮助我们实现网络异常断开后自动重连的功能。

2、Selector
    Selector 是对多路复用器的抽象，也是 Java NIO 的核心基础组件之一。
    Netty 就是基于 Selector 对象实现 I/O 多路复用的，
    在 Selector 内部，会通过系统调用不断地查询这些注册在其上的 Channel 是否有已就绪的 I/O 事件，
    例如，可读事件（OP_READ）、可写事件（OP_WRITE）或是网络连接事件（OP_ACCEPT）等，
    而无须使用用户线程进行轮询。这样，我们就可以用一个线程监听多个 Channel 上发生的事件。

3、ChannelPipeline & ChannelHandler
    提到 Pipeline，你可能最先想到的是 Linux 命令中的管道，它可以实现将一条命令的输出作为另一条命令的输入。
    Netty 中的 ChannelPipeline 也可以实现类似的功能：
    ChannelPipeline 会将一个 ChannelHandler 处理后的数据作为下一个 ChannelHandler 的输入。

    Netty 中定义了两种事件类型：入站（Inbound）事件和出站（Outbound）事件。
    这两种事件就像 Linux 管道中的数据一样，在 ChannelPipeline 中传递，事件之中也可能会附加数据。
    ChannelPipeline 之上可以注册多个 ChannelHandler（ChannelInboundHandler 或 ChannelOutboundHandler），
    我们在 ChannelHandler 注册的时候决定处理 I/O 事件的顺序，这就是典型的责任链模式。

                channelPipeline 流程
    socket.read   -> handler1 -> handler2 ->  ...
                                                    处理业务
    socket.write  <- handler4 <- handler3 <-  ...

    实例如下:
        ChannelPipeline p = socketChannel.pipeline();
        p.addLast("1", new InboundHandlerA());
        p.addLast("2", new InboundHandlerB());
        p.addLast("3", new OutboundHandlerA());
        p.addLast("4", new OutboundHandlerB());
        p.addLast("5", new InboundOutboundHandlerX());
    处理顺序如下:
     inbound流程      1 -> 2 -> 5
     outbound流程     5 -> 4 -> 3

     入站（Inbound）事件一般由 I/O 线程触发。
     举个例子，我们自定义了一种消息协议，一条完整的消息是由消息头和消息体两部分组成，其中消息头会含有消息类型、控制位、数据长度等元数据，
     消息体则包含了真正传输的数据。在面对一块较大的数据时，客户端一般会将数据切分成多条消息发送，服务端接收到数据后，一般会先进行解码和缓存，
     待收集到长度足够的字节数据，组装成有固定含义的消息之后，才会传递给下一个 ChannelInboundHandler 进行后续处理。

     在 Netty 中就提供了很多 Encoder 的实现用来解码读取到的数据，Encoder 会处理多次 channelRead() 事件，等拿到有意义的数据之后，
     才会触发一次下一个 ChannelInboundHandler 的 channelRead() 方法。

     出站（Outbound）事件与入站（Inbound）事件相反，一般是由用户触发的。

     ChannelHandler 接口中并没有定义方法来处理事件，而是由其子类进行处理的，
     ChannelInboundHandler 拦截并处理入站事件，ChannelOutboundHandler 拦截并处理出站事件。

     Netty 提供的 ChannelInboundHandlerAdapter 和 ChannelOutboundHandlerAdapter 主要是帮助完成事件流转功能的，即自动调用传递事件的相应方法。
     这样，我们在自定义 ChannelHandler 实现类的时候，就可以直接继承相应的 Adapter 类，并覆盖需要的事件处理方法，其他不关心的事件方法直接使用默认实现即可，从而提高开发效率。

     ChannelHandler 中的很多方法都需要一个 ChannelHandlerContext 类型的参数，
     ChannelHandlerContext 抽象的是 ChannelHandler 之间的关系以及 ChannelHandler 与ChannelPipeline 之间的关系。
     ChannelPipeline 中的事件传播主要依赖于ChannelHandlerContext 实现，
     在 ChannelHandlerContext 中维护了 ChannelHandler 之间的关系，
     所以我们可以从 ChannelHandlerContext 中得到当前 ChannelHandler 的后继节点，从而将事件传播到后续的 ChannelHandler。

     ChannelHandlerContext 继承了 AttributeMap，所以提供了 attr() 方法设置和删除一些状态属性信息，
     我们可将业务逻辑中所需使用的状态属性值存入到 ChannelHandlerContext 中，然后这些属性就可以随它传播了。
     Channel 中也维护了一个 AttributeMap，与 ChannelHandlerContext 中的 AttributeMap，从 Netty 4.1 开始，都是作用于整个 ChannelPipeline。

     一个 Channel 对应一个 ChannelPipeline，一个 ChannelHandlerContext 对应一个ChannelHandler。

                                                        [(ChannelPipeline)]
     channel -> [        ChannelHandler1         ,           ChannelHandler2        ,        ChannelHandler3        , ...]
                                |                                   |
                       ChannelHandlerContext1   ->         ChannelHandlerContext2   ->      ChannelHandlerContext3 ->
    需要注意的是，如果要在 ChannelHandler 中执行耗时较长的逻辑，例如，操作 DB 、进行网络或磁盘 I/O 等操作，一般会在注册到 ChannelPipeline 的同时，指定一个线程池异步执行 ChannelHandler 中的操作。

4、NioEventLoop
    一个 EventLoop 对象由一个永远都不会改变的线程驱动，同时一个 NioEventLoop 包含了一个 Selector 对象，可以支持多个 Channel 注册在其上，
    该 NioEventLoop 可以同时服务多个 Channel，每个 Channel 只能与一个 NioEventLoop 绑定，这样就实现了线程与 Channel 之间的关联。

    我们知道，Channel 中的 I/O 操作是由 ChannelPipeline 中注册的 ChannelHandler 进行处理的，
    而 ChannelHandler 的逻辑都是由相应 NioEventLoop 关联的那个线程执行的。

    除了与一个线程绑定之外，NioEventLoop 中还维护了两个任务队列：
    普通任务队列: 用户产生的普通任务可以提交到该队列中暂存，NioEventLoop 发现该队列中的任务后会立即执行。
        这是一个多生产者、单消费者的队列，Netty 使用该队列将外部用户线程产生的任务收集到一起，并在 Reactor 线程内部用单线程的方式串行执行队列中的任务。
        例如，外部非 I/O 线程调用了 Channel 的 write() 方法，Netty 会将其封装成一个任务放入 TaskQueue 队列中，
        这样，所有的 I/O 操作都会在 I/O 线程中串行执行。
        io.netty.util.concurrent.SingleThreadEventExecutor.taskQueue
    定时任务队列: 当用户在非 I/O 线程产生定时操作时，Netty 将用户的定时操作封装成定时任务，并将其放入该定时任务队列中等待相应 NioEventLoop 串行执行。
        io.netty.channel.SingleThreadEventLoop.tailTasks

    NioEventLoop 主要做三件事：监听 I/O 事件、执行普通任务以及执行定时任务。
    NioEventLoop 到底分配多少时间在不同类型的任务上，是可以配置的。
    另外，为了防止 NioEventLoop 长时间阻塞在一个任务上，一般会将耗时的操作提交到其他业务线程池处理。

5、NioEventLoopGroup
    NioEventLoopGroup 表示的是一组 NioEventLoop。
    Netty 为了能更充分地利用多核 CPU 资源，一般会有多个 NioEventLoop 同时工作，至于多少线程可由用户决定，
    Netty 会根据实际上的处理器核数计算一个默认值，具体计算公式是：CPU 的核心数 * 2，当然我们也可以根据实际情况手动调整。

    当一个 Channel 创建之后，Netty 会调用 NioEventLoopGroup 提供的 next() 方法，按照一定规则获取其中一个 NioEventLoop 实例，
    并将 Channel 注册到该 NioEventLoop 实例，之后，就由该 NioEventLoop 来处理 Channel 上的事件。
    EventLoopGroup、EventLoop 以及 Channel 三者的关联关系如下:
                                          ->    channel
                     ->     EventLoop     ...
                                          ->    channel
    EventLoopGroup   ...
                                          ->    channel
                     ->     EventLoop     ...
                                          ->    channel

    在 Netty 服务器端中，会有 BossEventLoopGroup 和 WorkerEventLoopGroup 两个 NioEventLoopGroup。
    通常一个服务端口只需要一个ServerSocketChannel，对应一个 Selector 和一个 NioEventLoop 线程。

    BossEventLoop 负责接收客户端的连接事件，即 OP_ACCEPT 事件，然后将创建的 NioSocketChannel 交给 WorkerEventLoopGroup；
    WorkerEventLoopGroup 会由 next() 方法选择其中一个 NioEventLoopGroup，并将这个 NioSocketChannel 注册到其维护的 Selector 并对其后续的I/O事件进行处理。
                        Channel Event
                            |                                         -> (next) EventLoop
    BossEventLoop -> (next) EventLoop  ->     WorkerEventLoopGroup    -> (next) EventLoop
                                                                      -> (next) EventLoop

    BossEventLoopGroup 通常是一个单线程的 EventLoop(因为doBind执行执行一次，也只会使用到其中一个EventLoop)，EventLoop 维护着一个 Selector 对象，其上注册了一个 ServerSocketChannel，
    BoosEventLoop 会不断轮询 Selector 监听连接事件，在发生连接事件时，通过 accept 操作与客户端创建连接，创建 SocketChannel 对象。
    然后将 accept 操作得到的 SocketChannel 交给 WorkerEventLoopGroup，在Reactor 模式中 WorkerEventLoopGroup 中会维护多个 EventLoop，
    而每个 EventLoop 都会监听分配给它的 SocketChannel 上发生的 I/O 事件，并将这些具体的事件分发给业务线程池处理。
    BossEventLoopGroup 是一个单线程的代码分析: bind发生一次，也只会获取一次EventLoop
        io.netty.bootstrap.AbstractBootstrap.bind() ->
            io.netty.bootstrap.AbstractBootstrap.doBind ->
                io.netty.bootstrap.AbstractBootstrap.initAndRegister ->
                    io.netty.bootstrap.AbstractBootstrapConfig.group

6、ByteBuf
  在进行跨进程远程交互的时候，我们需要以字节的形式发送和接收数据，发送端和接收端都需要一个高效的数据容器来缓存字节数据，ByteBuf 就扮演了这样一个数据容器的角色。

  ByteBuf 类似于一个字节数组，其中维护了一个读索引和一个写索引，分别用来控制对 ByteBuf 中数据的读写操作，两者符合下面的不等式：
    0 <= readerIndex <= writerIndex <= capacity

  Netty 中主要分为以下三大类 ByteBuf:
      1、Heap Buffer（堆缓冲区）: 这是最常用的一种 ByteBuf，它将数据存储在 JVM 的堆空间，其底层实现是在 JVM 堆内分配一个数组，
                                实现数据的存储。堆缓冲区可以快速分配，当不使用时也可以由 GC 轻松释放。
                                它还提供了直接访问底层数组的方法，通过 ByteBuf.array() 来获取底层存储数据的 byte[] 。

      2、Direct Buffer（直接缓冲区）: 直接缓冲区会使用堆外内存存储数据，不会占用 JVM 堆的空间，使用时应该考虑应用程序要使用的最大内存容量以及如何及时释放。
                                    直接缓冲区在使用 Socket 传递数据时性能很好，当然，它也是有缺点的，因为没有了 JVM GC 的管理，
                                    在分配内存空间和释放内存时，比堆缓冲区更复杂，Netty 主要使用内存池来解决这样的问题，这也是 Netty 使用内存池的原因之一。

      3、Composite Buffer（复合缓冲区）: 我们可以创建多个不同的 ByteBuf，然后提供一个这些 ByteBuf 组合的视图，也就是 CompositeByteBuf。
                                        它就像一个列表，可以动态添加和删除其中的 ByteBuf。

7、内存管理
  Netty 使用 ByteBuf 对象作为数据容器，进行 I/O 读写操作，其实 Netty 的内存管理也是围绕着ByteBuf 对象高效地分配和释放。
  从内存管理角度来看，ByteBuf 可分为 Unpooled 和 Pooled 两类。

  Unpooled，是指非池化的内存管理方式。每次分配时直接调用系统 API 向操作系统申请 ByteBuf，在使用完成之后，通过系统调用进行释放。
    Unpooled 将内存管理完全交给系统，不做任何特殊处理，使用起来比较方便，对于申请和释放操作不频繁、操作成本比较低的 ByteBuf 来说，是比较好的选择。

  Pooled，是指池化的内存管理方式。该方式会预先申请一块大内存形成内存池，在需要申请 ByteBuf 空间的时候，会将内存池中一部分合理的空间封装成 ByteBuf 给服务使用，
    使用完成后回收到内存池中。 DirectByteBuf 底层使用的堆外内存管理比较复杂，池化技术很好地解决了这一问题。

  如何高效分配和释放内存、如何减少内存碎片以及在多线程环境下如何减少锁竞争这三个方面？ 关于 Netty 提供的 ByteBuf 池化技术

      Netty 首先会向系统申请一整块连续内存，称为 Chunk（默认大小为 16 MB），这一块连续的内存通过 PoolChunk 对象进行封装。
      之后，Netty 将 Chunk 空间进一步拆分为 Page，每个 Chunk 默认包含 2048 个 Page，每个 Page 的大小为 8 KB。
          在同一个 Chunk 中，Netty 将 Page 按照不同粒度进行分层管理。
          从下向上数第 1 层中每个分组的大小为 1 * PageSize，一共有 2048 个分组；
          第 2 层中每个分组大小为 2 * PageSize，一共有 1024 个组；
          第 3 层中每个分组大小为 4 * PageSize，一共有 512 个组；
          依次类推，直至最顶层。

   1. 内存分配&释放
       当服务向内存池请求内存时，Netty 会将请求分配的内存数向上取整到最接近的分组大小，然后在该分组的相应层级中从左至右寻找空闲分组。
       例如，服务请求分配 3 * PageSize 的内存，向上取整得到的分组大小为 4 * PageSize，在该层分组中找到完全空闲的一组内存进行分配即可
       当分组大小 4 * PageSize 的内存分配出去后，为了方便下次内存分配，分组被标记为全部已使用，向上更粗粒度的内存分组被标记为部分已使用。
       Netty 使用完全平衡树的结构实现了上述算法，这个完全平衡树底层是基于一个 byte 数组构建的

   2. 大对象&小对象的处理
       当申请分配的对象是超过 Chunk 容量的大型对象，Netty 就不再使用池化管理方式了，在每次请求分配内存时单独创建特殊的非池化 PoolChunk 对象进行管理，当对象内存释放时整个PoolChunk 内存释放。

       如果需要一定数量空间远小于 PageSize 的 ByteBuf 对象，例如，创建 256 Byte 的 ByteBuf，按照上述算法，就需要为每个小 ByteBuf 对象分配一个 Page，
       这就出现了很多内存碎片。Netty 通过再将 Page 细分的方式，解决这个问题。Netty 将请求的空间大小向上取最近的 16 的倍数（或 2 的幂），规整后小于 PageSize 的小 Buffer 可分为两类。

       微型对象：规整后的大小为 16 的整倍数，如 16、32、48、……、496，一共 31 种大小。

       小型对象：规整后的大小为 2 的幂，如 512、1024、2048、4096，一共 4 种大小。

       Netty 的实现会先从 PoolChunk 中申请空闲 Page，同一个 Page 分为相同大小的小 Buffer 进行存储；
       这些 Page 用 PoolSubpage 对象进行封装，PoolSubpage 内部会记录它自己能分配的小 Buffer 的规格大小、可用内存数量，
       并通过 bitmap 的方式记录各个小内存的使用情况。虽然这种方案不能完美消灭内存碎片，但是很大程度上还是减少了内存浪费。

       为了解决单个 PoolChunk 容量有限的问题，Netty 将多个 PoolChunk 组成链表一起管理，然后用 PoolChunkList 对象持有链表的 head。
       Netty 通过 PoolArena 管理 PoolChunkList 以及 PoolSubpage。
       PoolArena 内部持有 6 个 PoolChunkList，各个 PoolChunkList 持有的 PoolChunk 的使用率区间有所不同。

   3、 并发处理
     内存分配释放不可避免地会遇到多线程并发场景，PoolChunk 的完全平衡树标记以及 PoolSubpage 的 bitmap 标记都是多线程不安全的，都是需要加锁同步的。
     为了减少线程间的竞争，Netty 会提前创建多个 PoolArena（默认数量为 2 * CPU 核心数），当线程首次请求池化内存分配，会找被最少线程持有的 PoolArena，
     并保存线程局部变量 PoolThreadCache 中，实现线程与 PoolArena 的关联绑定。

     Netty 还提供了延迟释放的功能，来提升并发性能。当内存释放时，PoolArena 并没有马上释放，而是先尝试将该内存关联的 PoolChunk 和 Chunk 中的偏移位置等信息存入 ThreadLocal 的固定大小缓存队列中，
     如果该缓存队列满了，则马上释放内存。当有新的分配请求时，PoolArena 会优先访问线程本地的缓存队列，查询是否有缓存可用，如果有，则直接分配，提高分配效率。

```
    
10、自定义一个RPC(dubbo-demo/simple-rpc)
```
RPC 框架的基石部分——远程调用，简易版 RPC 框架一次远程调用的核心流程是这样的：
    1、Client 首先会调用本地的代理，也就是 Proxy。

    2、Client 端 Proxy 会按照协议（Protocol），将调用中传入的数据序列化成字节流。

    3、之后 Client 会通过网络，将字节数据发送到 Server 端。

    4、Server 端接收到字节数据之后，会按照协议进行反序列化，得到相应的请求信息。

    5、Server 端 Proxy 会根据序列化后的请求信息，调用相应的业务逻辑。

    6、Server 端业务逻辑的返回值，也会按照上述逻辑返回给 Client 端。

包文件功能:
    1、protocol：简易版 RPC 框架的自定义协议。

    2、serialization：提供了自定义协议对应的序列化、反序列化的相关工具类。

    3、codec：提供了自定义协议对应的编码器和解码器。

    4、transport：基于 Netty 提供了底层网络通信的功能，其中会使用到 codec 包中定义编码器和解码器，以及 serialization 包中的序列化器和反序列化器。

    5、registry：基于 ZooKeeper 和 Curator 实现了简易版本的注册中心功能。

    6、proxy：使用 JDK 动态代理实现了一层代理。

自定义协议
    当前已经有很多成熟的协议了，例如 HTTP、HTTPS 等，那为什么我们还要自定义 RPC 协议呢？

    从功能角度考虑，HTTP 协议在 1.X 时代，只支持半双工传输模式，虽然支持长连接，但是不支持服务端主动推送数据。
    从效率角度来看，在一次简单的远程调用中，只需要传递方法名和加个简单的参数，此时，HTTP 请求中大部分数据都被 HTTP Header 占据，真正的有效负载非常少，效率就比较低。

    当然，HTTP 协议也有自己的优势，例如，天然穿透防火墙，大量的框架和开源软件支持 HTTP 接口，
    而且配合 REST 规范使用也是很便捷的，所以有很多 RPC 框架直接使用 HTTP 协议，
    尤其是在 HTTP 2.0 之后，如 gRPC、Spring Cloud 等。

    协议帧如下:
     short        byte                            byte                      long               int             n - byte
     magic       version                        extraInfo                 messageId            size           message body
     魔数         版本号         0         1～2       3～4      5～6
                            消息类型    序列化方式   压缩方式    请求类型        消息ID              消息长度          消息体内容
                          (请求/响应)                     (正常请求/心跳请求)
    在 simple-RPC 的消息头中，包含了整个 RPC 消息的一些控制信息，例如，版本号、魔数、消息类型、附加信息、消息 ID 以及消息体的长度，
    在附加信息（extraInfo）中，按位进行划分，分别定义消息的类型、序列化方式、压缩方式以及请求类型。当然，也可以自己扩充 simple-RPC 协议，实现更加复杂的功能。

    消息帧定义:
        public class Header {
             private short magic; // 魔数
             private byte version; // 协议版本
             private byte extraInfo; // 附加信息
             private Long messageId; // 消息ID
             private Integer size; // 消息体长度
             ... // 省略getter/setter方法
        }

    请求消息和响应消息定义:
        public class Request implements Serializable {
             private String serviceName; // 请求的Service类名
             private String methodName; // 请求的方法名称
             private Class[] argTypes; // 请求方法的参数类型
             private Object[] args; // 请求方法的参数
                ... // 省略getter/setter方法
        }
        public class Response implements Serializable {
             private int code = 0; // 响应的错误码，正常响应为0，非0表示异常响应
             private String errMsg; // 异常信息
             private Object result; // 响应结果
             ... // 省略getter/setter方法
        }

序列化实现
    Request 和 Response 对象是要进行序列化的，需要实现 Serializable 接口。
    为了让这两个类的对象能够在 Client 和 Server 之间跨进程传输，需要进行序列化和反序列化操作，这里定义一个 Serialization 接口，统一完成序列化相关的操作。
    定义序列化接口
        public interface Serialization {
             <T> byte[] serialize(T obj)throws IOException;
             <T> T deSerialize(byte[] data, Class<T> clz)throws IOException;
        }
    可以自定义实现各种序列化协议

    在有的场景中，请求或响应传输的数据比较大，直接传输比较消耗带宽，所以一般会采用压缩后再发送的方式。
    simple-RPC 消息头中的 extraInfo 字段中，就包含了标识消息体压缩方式的 bit 位。
    这里我们定义一个 Compressor 接口抽象所有压缩算法。
        public interface Compressor {
             byte[] compress(byte[] array) throws IOException;
             byte[] unCompress(byte[] array) throws IOException;
        }

编解码实现

     Netty 核心概念是Netty 每个 Channel 绑定一个 ChannelPipeline，并依赖 ChannelPipeline 中添加的 ChannelHandler 处理接收到（或要发送）的数据，其中就包括字节到消息（以及消息到字节）的转换。
     Netty 中提供了 ByteToMessageDecoder、 MessageToByteEncoder、MessageToMessageEncoder、MessageToMessageDecoder 等抽象类来实现 Message 与 ByteBuf 之间的转换以及 Message 之间的转换。

     可以看到对很多已有协议的序列化和反序列化都是基于上述抽象类实现的，
     例如，HttpServerCodec 中通过依赖 HttpServerRequestDecoder 和 HttpServerResponseEncoder 来实现 HTTP 请求的解码和 HTTP 响应的编码。
     HttpServerRequestDecoder 继承自 ByteToMessageDecoder，实现了 ByteBuf 到 HTTP 请求之间的转换；
     HttpServerResponseEncoder 继承自 MessageToMessageEncoder，实现 HTTP 响应到其他消息的转换（其中包括转换成 ByteBuf 的能力）。

     本例中使用ByteToMessageDecoder、MessageToMessageEncoder
     request :  byte -> message
     response : message -> byte

```

11、dubbo register 核心接口(dubbo-registry/dubbo-registry-api)
```
Dubbo 一般使用 Node 这个接口来抽象节点的概念，Node不仅可以表示 Provider 和 Consumer 节点，还可以表示注册中心节点。

RegistryService 接口抽象了注册服务的基本行为。

Registry 接口继承了 RegistryService 接口和 Node 接口。

RegistryFactory 接口是 Registry 的工厂接口，负责创建 Registry 对象，其中 @SPI 注解指定了默认的扩展名为 dubbo，@Adaptive 注解表示会生成适配器类并根据 URL 参数中的 protocol 参数值选择相应的实现。
    EtcdRegistryFactory
    ConsulRegistryFactory
    ZookeeperRegistryFactory
    NacosRegistryFactory
    DubboRegistryFactory
    ...

RegistryFactoryWrapper 是 RegistryFactory 接口的 Wrapper 类，它在底层 RegistryFactory 创建的 Registry 对象外层封装了一个 ListenerRegistryWrapper ，
ListenerRegistryWrapper 中维护了一个 RegistryServiceListener 集合，会将 register()、subscribe() 等事件通知到 RegistryServiceListener 监听器。

AbstractRegistryFactory 是一个实现了 RegistryFactory 接口的抽象类，提供了规范 URL 的操作以及缓存 Registry 对象的公共能力。
其中，缓存 Registry 对象是使用 HashMap<String, Registry> 集合实现的（REGISTRIES 静态字段）。
在规范 URL 的实现逻辑中，AbstractRegistryFactory 会将 RegistryService 的类名设置为 URL path 和 interface 参数，同时删除 export 和 refer 参数。

AbstractRegistry 实现了 Registry 接口，虽然 AbstractRegistry 本身在内存中实现了注册数据的读写功能，也没有什么抽象方法，
但它依然被标记成了抽象类，从Registry 继承关系图中可以看出，Registry 接口的所有实现类都继承了 AbstractRegistry。
    为了减轻注册中心组件的压力，AbstractRegistry 会把当前节点订阅的 URL 信息缓存到本地的 Properties 文件中(File file): org.apache.dubbo.registry.support.AbstractRegistry.AbstractRegistry

1、本地缓存 (org.apache.dubbo.registry.support.AbstractRegistry.notify(URL, NotifyListener, List<URL>))
    Dubbo 在微服务架构中解决了各个服务间协作的难题；作为 Provider 和 Consumer 的底层依赖，它会与服务一起打包部署。
    dubbo-registry 也仅仅是其中一个依赖包，负责完成与 ZooKeeper、etcd、Consul 等服务发现组件的交互。

    当 Provider 端暴露的 URL 发生变化时，ZooKeeper 等服务发现组件会通知 Consumer 端的 Registry 组件，Registry 组件会调用 notify() 方法，
    被通知的 Consumer 能匹配到所有 Provider 的 URL 列表并写入 properties 集合中。

2、注册、订阅
    AbstractRegistry 实现了 Registry 接口，它实现的 registry() 方法会将当前节点要注册的 URL 缓存到 registered 集合，
    而 unregistry() 方法会从 registered 集合删除指定的 URL，例如当前节点下线的时候。

    subscribe() 方法会将当前节点作为 Consumer 的 URL 以及相关的 NotifyListener 记录到 subscribed 集合，
    unsubscribe() 方法会将当前节点的 URL 以及关联的 NotifyListener 从 subscribed 集合删除。

3、回复与销毁
    AbstractRegistry 中还有另外两个需要关注的方法：recover() 方法和destroy() 方法。

    在 Provider 因为网络问题与注册中心断开连接之后，会进行重连，重新连接成功之后，会调用 recover() 方法将 registered 集合中的全部 URL 重新走一遍 register() 方法，恢复注册数据。
    同样，recover() 方法也会将 subscribed 集合中的 URL 重新走一遍 subscribe() 方法，恢复订阅监听器。

    在当前节点下线的时候，会调用 Node.destroy() 方法释放底层资源。
    AbstractRegistry 实现的 destroy() 方法会调用 unregister() 方法和 unsubscribe() 方法将当前节点注册的 URL 以及订阅的监听全部清理掉，
    其中不会清理非动态注册的 URL（即 dynamic 参数明确指定为 false）。

```
    
12、服务可靠性如何保障 - (FailbackRegistry)
```
为了保证服务的可靠性，重试机制就变得必不可少了。
"重试机制"就是在请求失败时，客户端重新发起一个一模一样的请求，尝试调用相同或不同的服务端，完成相应的业务操作。
能够使用重试机制的业务接口得是"幂等"的，也就是无论请求发送多少次，得到的结果都是一样的，例如查询操作。

dubbo-registry 将重试机制的相关实现放到了 AbstractRegistry 的子类—— FailbackRegistry 中。
接入 ZooKeeper、etcd 等开源服务发现组件的 Registry 实现，都继承了 FailbackRegistry，也就都拥有了失败重试的能力。

org.apache.dubbo.registry.support.FailbackRegistry.register

重试任务 org.apache.dubbo.registry.retry.FailedRegisteredTask 继承至 AbstractRetryTask

AbstractRetryTask 中维护了当前任务关联的 URL、当前重试的次数等信息，在其 run() 方法中，
会根据重试 URL 中指定的重试次数（retry.times 参数，默认值为 3）、任务是否被取消以及时间轮的状态，决定此次任务的 doRetry() 方法是否正常执行

AbstractRegistry 的实现类——FailbackRegistry 的核心实现，它主要是在 AbstractRegistry 的基础上，提供了重试机制。
具体方法就是通过时间轮，在 register()/ unregister()、subscribe()/ unsubscribe() 等核心方法失败时，添加重试定时任务，实现重试机制，同时也添加了相应的定时任务清理逻辑。
```
   
13、zookeeper - dubbo如何使用
```
dubbo 应用的常用目录
    root                    /dubbo
    service        /org.apache.service
    Type      /providers     /consumers   /routes  /configurations
    URL   /consumer://10.20.153.10/org.apache.dubbo.foo.BarService?version=1.0.0&application=kylin
root : 默认是 dubbo
service : 服务接口的全路径名称
Type : URL 的分类，一共有四种分类，分别是：providers（服务提供者列表）、consumers（服务消费者列表）、routes（路由规则列表）和 configurations（配置规则列表）
URL : Provider URL 、Consumer URL 、Routes URL 和 Configurations URL

ZookeeperRegistryFactory
     RegistryFactory 这个工厂接口以及其子类 AbstractRegistryFactory，AbstractRegistryFactory 仅仅是提供了缓存 Registry 对象的功能，并未真正实现 Registry 的创建，具体的创建逻辑是由子类完成的。
     在 dubbo-registry-zookeeper 模块中的 SPI 配置文件(META-INF/dubbo/internal)中，指定了RegistryFactory 的实现类 —— ZookeeperRegistryFactory。

ZookeeperTransporter
    dubbo-remoting-zookeeper 模块是 dubbo-remoting 模块的子模块，但它并不依赖 dubbo-remoting 中的其他模块，是相对独立的。

    简单来说，dubbo-remoting-zookeeper 模块是在 Apache Curator 的基础上封装了一套 Zookeeper 客户端，将与 Zookeeper 的交互融合到 Dubbo 的体系之中。

    dubbo-remoting-zookeeper 模块中有两个核心接口：ZookeeperTransporter 接口和 ZookeeperClient 接口。

    ZookeeperTransporter 只负责一件事情，那就是创建 ZookeeperClient 对象。

ZookeeperClient
    ZookeeperClient 接口是 Dubbo 封装的 Zookeeper 客户端，该接口定义了大量的方法，都是用来与 Zookeeper 进行交互的。

    抽象类实现 - AbstractZookeeperClient 提供能力如下:
        1、缓存当前 ZookeeperClient 实例创建的持久 ZNode 节点

        2、管理当前 ZookeeperClient 实例添加的各类监听器

        3、管理当前 ZookeeperClient 的运行状态

    dubbo-remoting-zookeeper 对外提供了 StateListener、DataListener 和 ChildListener 三种类型的监听器
        StateListener：主要负责监听 Dubbo 与 Zookeeper 集群的连接状态，
                包括 SESSION_LOST、CONNECTED、RECONNECTED、SUSPENDED 和 NEW_SESSION_CREATED。

        DataListener：主要监听某个节点存储的数据变化。

        ChildListener：主要监听某个 ZNode 节点下的子节点变化。

     AbstractZookeeperClient 中维护了 stateListeners、listeners 以及 childListeners 三个集合，分别管理上述三种类型的监听器。

ZookeeperRegistry

```

14、序列化
```
java 原生序列化注意
    1、transient 关键字，它的作用就是：在对象序列化过程中忽略被其修饰的成员属性变量。
        一般情况下，它可以用来修饰一些非数据型的字段以及一些可以通过其他字段计算得到的值。
        通过合理地使用 transient 关键字，可以降低序列化后的数据量，提高网络传输效率。

    2、生成一个序列号 serialVersionUID，这个序列号不是必需的，但还是建议你生成。
        serialVersionUID 的字面含义是序列化的版本号，只有序列化和反序列化的 serialVersionUID 都相同的情况下，才能够成功地反序列化。
        如果类中没有定义 serialVersionUID，那么 JDK 也会随机生成一个 serialVersionUID。
        如果在某些场景中，你希望不同版本的类序列化和反序列化相互兼容，那就需要定义相同的 serialVersionUID。

    3、根据需求决定是否要重写 writeObject()/readObject() 方法，实现自定义序列化。

    4、调用 java.io.ObjectOutputStream 的 writeObject()/readObject() 进行序列化与反序列化。

常见序列化算法
    1、Apache Avro 是一种与编程语言无关的序列化格式。
    Avro 依赖于用户自定义的 Schema，在进行序列化数据的时候，无须多余的开销，就可以快速完成序列化，并且生成的序列化数据也较小。
    当进行反序列化的时候，需要获取到写入数据时用到的 Schema。在 Kafka、Hadoop 以及 Dubbo 中都可以使用 Avro 作为序列化方案。

    2、FastJson 是阿里开源的 JSON 解析库，可以解析 JSON 格式的字符串。
    它支持将 Java 对象序列化为 JSON 字符串，反过来从 JSON 字符串也可以反序列化为 Java 对象。
    FastJson 是 Java 程序员常用到的类库之一，正如其名，“快”是其主要卖点。
    从官方的测试结果来看，FastJson 确实是最快的，比 Jackson 快 20% 左右，但是近几年 FastJson 的安全漏洞比较多，所以你在选择版本的时候，还是需要谨慎一些。

    3、Fst（全称是 fast-serialization）是一款高性能 Java 对象序列化工具包，100% 兼容 JDK 原生环境，
    序列化速度大概是JDK 原生序列化的 4~10 倍，序列化后的数据大小是 JDK 原生序列化大小的 1/3 左右。目前，Fst 已经更新到 3.x 版本，支持 JDK 14。

    4、Kryo 是一个高效的 Java 序列化/反序列化库，目前 Twitter、Yahoo、Apache 等都在使用该序列化技术，特别是 Spark、Hive 等大数据领域用得较多。
    Kryo 提供了一套快速、高效和易用的序列化 API。无论是数据库存储，还是网络传输，都可以使用 Kryo 完成 Java 对象的序列化。
    Kryo 还可以执行自动深拷贝和浅拷贝，支持环形引用。Kryo 的特点是 API 代码简单，序列化速度快，并且序列化之后得到的数据比较小。
    另外，Kryo 还提供了 NIO 的网络通信库——KryoNet。

    5、Hessian2 序列化是一种支持动态类型、跨语言的序列化协议，Java 对象序列化的二进制流可以被其他语言使用。
    Hessian2 序列化之后的数据可以进行自描述，不会像 Avro 那样依赖外部的 Schema 描述文件或者接口定义。
    Hessian2 可以用一个字节表示常用的基础类型，这极大缩短了序列化之后的二进制流。
    需要注意的是，在 Dubbo 中使用的 Hessian2 序列化并不是原生的 Hessian2 序列化，而是阿里修改过的 Hessian Lite，它是 Dubbo 默认使用的序列化方式。
    其序列化之后的二进制流大小大约是 Java 序列化的 50%，序列化耗时大约是 Java 序列化的 30%，反序列化耗时大约是 Java 反序列化的 20%。

    6、Protobuf（Google Protocol Buffers）是 Google 公司开发的一套灵活、高效、自动化的、用于对结构化数据进行序列化的协议。
    但相比于常用的 JSON 格式，Protobuf 有更高的转化效率，时间效率和空间效率都是 JSON 的 5 倍左右。
    Protobuf 可用于通信协议、数据存储等领域，它本身是语言无关、平台无关、可扩展的序列化结构数据格式。
    目前 Protobuf提供了 C++、Java、Python、Go 等多种语言的 API，gRPC 底层就是使用 Protobuf 实现的序列化。

dubbo-serialization
    Dubbo 为了支持多种序列化算法，单独抽象了一层 Serialize 层，在整个 Dubbo 架构中处于最底层，对应的模块是 dubbo-serialization 模块。

    dubbo-serialization-api 模块中定义了 Dubbo 序列化层的核心接口，其中最核心的是 Serialization 这个接口，
    它是一个扩展接口，被 @SPI 接口修饰，默认扩展实现是 Hessian2Serialization。

    org.apache.dubbo.common.serialize.hessian2.Hessian2Serialization
        -> org.apache.dubbo.common.serialize.hessian2.Hessian2ObjectInput
        -> org.apache.dubbo.common.serialize.hessian2.Hessian2ObjectOutput

dubbo-remoting
    抽象层 -> dubbo-remoting-api
        1、buffer 包：定义了缓冲区相关的接口、抽象类以及实现类。缓冲区在NIO框架中是一个不可或缺的角色，在各个 NIO 框架中都有自己的缓冲区实现。这里的 buffer 包在更高的层面，抽象了各个 NIO 框架的缓冲区，同时也提供了一些基础实现。

        2、exchange 包：抽象了 Request 和 Response 两个概念，并为其添加很多特性。这是整个远程调用非常核心的部分。

        3、transport 包：对网络传输层的抽象，但它只负责抽象单向消息的传输，即请求消息由 Client 端发出，Server 端接收；响应消息由 Server 端发出，Client端接收。有很多网络库可以实现网络传输的功能，例如 Netty、Grizzly 等， transport 包是在这些网络库上层的一层抽象。

        4、其他接口：Endpoint、Channel、Transporter、Dispatcher 等顶层接口放到了org.apache.dubbo.remoting 这个包，这些接口是 Dubbo Remoting 的核心接口。

    传输层核心接口
        在 Dubbo 中会抽象出一个“端点（Endpoint）”的概念，我们可以通过一个 ip 和 port 唯一确定一个端点，两个端点之间会创建 TCP 连接，可以双向传输数据。
        Dubbo 将 Endpoint 之间的 TCP 连接抽象为通道（Channel），将发起请求的 Endpoint 抽象为客户端（Client），将接收请求的 Endpoint 抽象为服务端（Server）。

    dubbo 如何利用buffer？

        抽象接口: org.apache.dubbo.remoting.buffer.ChannelBuffer
               (
                ChannelBuffer 接口的设计与 Netty4 中 ByteBuf 抽象类的设计基本一致，也有 readerIndex 和 writerIndex 指针的概念。
                // 获取工厂的方法
                ChannelBufferFactory factory();
               )
            抽象实现: org.apache.dubbo.remoting.buffer.AbstractChannelBuffer
                具体实现: org.apache.dubbo.remoting.buffer.ByteBufferBackedChannelBuffer
                         org.apache.dubbo.remoting.buffer.DynamicChannelBuffer
                         org.apache.dubbo.remoting.buffer.HeapChannelBuffer

            具体实现: org.apache.dubbo.remoting.transport.netty4.NettyBackedChannelBuffer
                     org.apache.dubbo.remoting.transport.netty.NettyBackedChannelBuffer

    Stream 相关

        org.apache.dubbo.remoting.buffer.ChannelBufferInputStream

        org.apache.dubbo.remoting.buffer.ChannelBufferOutputStream

        org.apache.dubbo.remoting.buffer.ChannelBuffers
            dynamicBuffer() 方法：创建 DynamicChannelBuffer 对象，初始化大小由第一个参数指定，默认为 256。
            buffer() 方法：创建指定大小的 HeapChannelBuffer 对象。
            wrappedBuffer() 方法：将传入的 byte[] 数字封装成 HeapChannelBuffer 对象。
            directBuffer() 方法：创建 ByteBufferBackedChannelBuffer 对象，需要注意的是，底层的 ByteBuffer 使用的堆外内存，需要特别关注堆外内存的管理。
            equals() 方法：用于比较两个 ChannelBuffer 是否相同，其中会逐个比较两个 ChannelBuffer 中的前 7 个可读字节，只有两者完全一致，才算两个 ChannelBuffer 相同
            compare() 方法：用于比较两个 ChannelBuffer 的大小，会逐个比较两个 ChannelBuffer 中的全部可读字节。

    AbstractPeer 抽象类

    AbstractEndpoint 抽象类

    AbstractServer 抽象类

    ExecutorRepository 接口  ->  DefaultExecutorRepository 实现 (ThreadPool[spi]) -> EagerThreadPoolExecutor(TaskQueue) -> org.apache.dubbo.remoting.transport.AbstractServer

    dubbo 整个服务端方法执行流程如下:
    org.apache.dubbo.remoting.transport.netty4.NettyServer doOpen() 开启服务端TCP监听
        -> org.apache.dubbo.remoting.transport.netty4.NettyServerHandler channelRead() 读取解码后的message
            -> org.apache.dubbo.remoting.transport.dispatcher.all.AllChannelHandler received() 处理回复客户端的消息
                -> org.apache.dubbo.remoting.transport.dispatcher.ChannelEventRunnable run()开启异步(通过线程池执行)处理回复消息
                    -> org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol received() 通过不同的协议去处理回复消息，这里选择Dubbo协议分析
                        -> org.apache.dubbo.rpc.proxy.AbstractProxyInvoker(JavassistProxyFactory、JdkProxyFactory) invoke() 通过执行器处理具体方法

```

14、AbstractPeer 、 AbstractEndpoint 、 AbstractServer

1、AbstractPeer 这个抽象类，它同时实现了 Endpoint 接口和 ChannelHandler 接口。

Netty 中也有 ChannelHandler、Channel 等接口，但无特殊说明的情况下，这里的接口指的都是 Dubbo 中定义的接口。

AbstractPeer 中有四个字段：
    一个是表示该端点自身的 URL 类型的字段，还有两个 Boolean 类型的字段（closing 和 closed）用来记录当前端点的状态，这三个字段都与 Endpoint 接口相关，
    第四个字段指向了一个 ChannelHandler 对象，AbstractPeer 对 ChannelHandler 接口的所有实现，都是委托给了这个 ChannelHandler 对象。

结论：AbstractChannel、AbstractServer、AbstractClient 都是要关联一个 ChannelHandler 对象的。

2、AbstractEndpoint 抽象类 继承了 AbstractPeer 这个抽象类，AbstractEndpoint 中维护了一个 Codec2 对象（codec 字段）和两个超时时间（timeout 字段和 connectTimeout 字段），在 AbstractEndpoint 的构造方法中会根据传入的 URL 初始化这三个字段：

public AbstractEndpoint(URL url, ChannelHandler handler) {
    // 调用父类AbstractPeer的构造方法
    super(url, handler);
    // 根据URL中的codec参数值，确定此处具体的Codec2实现类
    this.codec = getChannelCodec(url);
    // 根据URL中的timeout参数确定timeout字段的值，默认1000
    this.timeout = url.getPositiveParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
    // 根据URL中的connect.timeout参数确定connectTimeout字段的值，默认3000
    this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
}

Codec2 接口的时候提到它是一个 SPI 扩展点，这里的 AbstractEndpoint.getChannelCodec() 方法就是基于 Dubbo SPI 选择其扩展实现的，具体实现如下：
protected static Codec2 getChannelCodec(URL url) {
    // 根据URL的codec参数获取扩展名
    String codecName = url.getParameter(Constants.CODEC_KEY, "telnet");
    if (ExtensionLoader.getExtensionLoader(Codec2.class).hasExtension(codecName)) { // 通过ExtensionLoader加载并实例化Codec2的具体扩展实现
      return ExtensionLoader.getExtensionLoader(Codec2.class).getExtension(codecName);
    } else { // Codec2接口不存在相应的扩展名，就尝试从Codec这个老接口的扩展名中查找，目前Codec接口已经废弃了，所以省略这部分逻辑
    }
}

AbstractEndpoint 还实现了 Resetable 接口（只有一个 reset() 方法需要实现），虽然 AbstractEndpoint 中的 reset() 方法比较长，但是逻辑非常简单，就是根据传入的 URL 参数重置 AbstractEndpoint 的三个字段，下面是重置 codec 字段的代码片段，还是调用 getChannelCodec() 方法实现的：
public void reset(URL url) {
  // 检测当前AbstractEndpoint是否已经关闭
  // 省略重置timeout、connectTimeout两个字段的逻辑
  try {
    if (url.hasParameter(Constants.CODEC_KEY)) {
      this.codec = getChannelCodec(url);
    }
  } catch (Throwable t) {
    logger.error(t.getMessage(), t);
  }
}

3、Server 继承路线分析

AbstractServer 和 AbstractClient 都实现了 AbstractEndpoint 抽象类。

先看 AbstractServer 的实现，AbstractServer 在继承了 AbstractEndpoint 的同时，还实现了 RemotingServer 接口，如下图所示：

![AbstractServer类继承](image/AbstractServer.png)

AbstractServer 是对服务端的抽象，实现了服务端的公共逻辑。

AbstractServer 的核心字段有下面几个：
```text
localAddress、bindAddress（InetSocketAddress 类型）：分别对应该 Server 的本地地址和绑定的地址，都是从 URL 中的参数中获取，bindAddress 默认值与 localAddress 一致。

accepts（int 类型）：该 Server 能接收的最大连接数，从 URL 的 accepts 参数中获取，默认值为 0，表示没有限制。

executorRepository（ExecutorRepository 类型）：负责管理线程池。

executor（ExecutorService 类型）：当前 Server 关联的线程池，由上面的 ExecutorRepository 创建并管理。
```

在 AbstractServer 的构造方法中会根据传入的 URL初始化上述字段，并调用 doOpen() 这个抽象方法完成该 Server 的启动，具体实现如下：
```
public AbstractServer(URL url, ChannelHandler handler) throws RemotingException {
    // 调用父类的构造方法
    super(url, handler);
    // 根据传入的URL初始化localAddress和bindAddress
    localAddress = getUrl().toInetSocketAddress();

    String bindIp = getUrl().getParameter(Constants.BIND_IP_KEY, getUrl().getHost());
    int bindPort = getUrl().getParameter(Constants.BIND_PORT_KEY, getUrl().getPort());
    if (url.getParameter(ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(bindIp)) {
        bindIp = ANYHOST_VALUE;
    }
    bindAddress = new InetSocketAddress(bindIp, bindPort);
    // 初始化accepts等字段
    this.accepts = url.getParameter(ACCEPTS_KEY, DEFAULT_ACCEPTS);
    this.idleTimeout = url.getParameter(IDLE_TIMEOUT_KEY, DEFAULT_IDLE_TIMEOUT);
    try {
        // 调用doOpen()这个抽象方法，启动该Server
        doOpen();
        if (logger.isInfoEnabled()) {
            logger.info("Start " + getClass().getSimpleName() + " bind " + getBindAddress() + ", export " + getLocalAddress());
        }
    } catch (Throwable t) {
        throw new RemotingException(url.toInetSocketAddress(), null, "Failed to bind " + getClass().getSimpleName()
                + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
    }
    // 获取该Server关联的线程池
    executor = executorRepository.createExecutorIfAbsent(url);
}
```

ExecutorRepository

在继续分析 AbstractServer 的具体实现类之前，我们先来了解一下 ExecutorRepository 这个接口。

ExecutorRepository 负责创建并管理 Dubbo 中的线程池，该接口虽然是个 SPI 扩展点，但是只有一个默认实现—— DefaultExecutorRepository，在该默认实现中维护了一个 ConcurrentMap<String, ConcurrentMap<Integer, ExecutorService>> 集合（data 字段）缓存已有的线程池，第一层 Key 值表示线程池属于 Provider 端还是 Consumer 端，第二层 Key 值表示线程池关联服务的端口。

DefaultExecutorRepository.createExecutorIfAbsent() 方法会根据 URL 参数创建相应的线程池并缓存在合适的位置，具体实现如下：
```
public synchronized ExecutorService createExecutorIfAbsent(URL url) {
  // 根据URL中的side参数值决定第一层key
  String componentKey = EXECUTOR_SERVICE_COMPONENT_KEY;
  if (CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(SIDE_KEY))) {
    componentKey = CONSUMER_SIDE; 
  }
  Map<Integer, ExecutorService> executors = data.computeIfAbsent(componentKey, k -> new ConcurrentHashMap<>());
  //根据URL中的port值确定第二层key
  Integer portKey = url.getPort();
  ExecutorService executor = executors.computeIfAbsent(portKey, k -> createExecutor(url));
  // 如果缓存中相应的线程池已关闭，则同样需要调用createExecutor()方法
  // 创建新的线程池，并替换掉缓存中已关闭的线程持，这里省略这段逻辑
  return executor;
}
```

在 createExecutor() 方法中，会通过 Dubbo SPI 查找 ThreadPool 接口的扩展实现，并调用其 getExecutor() 方法创建线程池，ThreadPool 接口被 @SPI 注解修饰，默认使用 FixedThreadPool 实现，但是 ThreadPool 接口中的 getExecutor() 方法被 @Adaptive 注解修饰，动态生成的适配器类会优先根据 URL 中的 threadpool 参数选择 ThreadPool 的扩展实现。

ThreadPool 接口的实现类如下图所示：

![ThreadPool 接口的实现类](image/ThreadPool.png)

不同实现会根据 URL 参数创建不同特性的线程池，这里以CacheThreadPool为例进行分析：
```
public Executor getExecutor(URL url) {
  String name = url.getParameter(THREAD_NAME_KEY, DEFAULT_THREAD_NAME);
  // 核心线程数量
  int cores = url.getParameter(CORE_THREADS_KEY, DEFAULT_CORE_THREADS);
  // 最大线程数量
  int threads = url.getParameter(THREADS_KEY, Integer.MAX_VALUE);
  // 缓冲队列的最大长度
  int queues = url.getParameter(QUEUES_KEY, DEFAULT_QUEUES);
  // 非核心线程的最大空闲时长，当非核心线程空闲时间超过该值时，会被回收
  int alive = url.getParameter(ALIVE_KEY, DEFAULT_ALIVE);
  // 下面就是依赖JDK的ThreadPoolExecutor创建指定特性的线程池并返回
  return new ThreadPoolExecutor(cores, threads, alive, TimeUnit.MILLISECONDS,
      queues == 0 ? new SynchronousQueue<Runnable>() :
          (queues < 0 ? new LinkedBlockingQueue<Runnable>()
              : new LinkedBlockingQueue<Runnable>(queues)),
      new NamedInternalThreadFactory(name, true), new AbortPolicyWithReport(name, url));
}
```

LimitedThreadPool：与 CacheThreadPool 一样，可以指定核心线程数、最大线程数以及缓冲队列长度，区别在于，LimitedThreadPool 创建的线程池的非核心线程不会被回收。

FixedThreadPool：核心线程数和最大线程数一致，且不会被回收。

EagerThreadPool：创建的线程池是 EagerThreadPoolExecutor（继承了 JDK 提供的 ThreadPoolExecutor），使用的队列是 TaskQueue（继承了LinkedBlockingQueue），该线程池与 ThreadPoolExecutor 不同的是：在线程数没有达到最大线程数的前提下，EagerThreadPoolExecutor 会优先创建线程来执行任务，而不是放到缓冲队列中；当线程数达到最大值时，EagerThreadPoolExecutor 会将任务放入缓冲队列，等待空闲线程。
EagerThreadPoolExecutor 覆盖了 ThreadPoolExecutor 中的两个方法：execute() 方法和 afterExecute() 方法，具体实现如下，我们可以看到其中维护了一个 submittedTaskCount 字段（AtomicInteger 类型），用来记录当前在线程池中的任务总数（正在线程中执行的任务数+队列中等待的任务数）
```
public void execute(Runnable command) {
  // 任务提交之前，递增submittedTaskCount
  submittedTaskCount.incrementAndGet(); 
  try {
    super.execute(command); // 提交任务
  } catch (RejectedExecutionException rx) {
    final TaskQueue queue = (TaskQueue) super.getQueue();
    try {
      // 任务被拒绝之后，会尝试再次放入队列中缓存，等待空闲线程执行
      if (!queue.retryOffer(command, 0, TimeUnit.MILLISECONDS)) {
        // 再次入队被拒绝，则队列已满，无法执行任务
        //递减submittedTaskCount
        submittedTaskCount.decrementAndGet();
        throw new RejectedExecutionException("Queue capacity is full.", rx);
      }
    } catch (InterruptedException x) {
      // 再次入队列异常，递减submittedTaskCount
      submittedTaskCount.decrementAndGet();
      throw new RejectedExecutionException(x);
    }
  } catch (Throwable t) { // 任务提交异常，递减submittedTaskCount
    submittedTaskCount.decrementAndGet();
    throw t;
  }
}
protected void afterExecute(Runnable r, Throwable t) {
  // 任务指定结束，递减submittedTaskCount
  submittedTaskCount.decrementAndGet(); 
}
```
看到这里，你可能会有些疑惑：没有看到优先创建线程执行任务的逻辑，其实重点在关联的 TaskQueue 实现中，它覆盖了 LinkedBlockingQueue.offer() 方法，会判断线程池的 submittedTaskCount 值是否已经达到最大线程数，如果未超过，则会返回 false，迫使线程池创建新线程来执行任务。

示例代码如下：
```
public boolean offer(Runnable runnable) {
  //获取当前线程池中的活跃线程数
  int currentPoolThreadSize = executor.getPoolSize();
  //当前有线程空闲，直接将任务提交到队列中，空闲线程会直接从中获取任务执行
  if (executor.getSubmittedTaskCount() < currentPoolThreadSize) {
    return super.offer(runnable);
  }
  // 当前没有空闲线程，但是还可以创建新线程，则返回false，迫使线程池创建
  // 新线程来执行任务
  if (currentPoolThreadSize < executor.getMaximumPoolSize()) {
    return false;
  }
  // 当前线程数已经达到上限，只能放到队列中缓存了
  return super.offer(runnable);
}
```

AbortPolicyWithReport ，它继承了 ThreadPoolExecutor.AbortPolicy，覆盖的 rejectedExecution 方法中会输出包含线程池相关信息的 WARN 级别日志，然后进行 dumpJStack() 方法，最后才会抛出RejectedExecutionException 异常。

基于 Netty 4 实现的 NettyServer，继承来自AbstractServer，实现了 doOpen() 方法和 doClose() 方法，这里重点看 doOpen() 方法，如下所示：
```
protected void doOpen() throws Throwable {
    // 创建ServerBootstrap
    bootstrap = new ServerBootstrap();

    // 创建boss EventLoopGroup
    bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "NettyServerBoss");
    // 创建worker EventLoopGroup
    workerGroup = NettyEventLoopFactory.eventLoopGroup(
            getUrl().getPositiveParameter(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
            "NettyServerWorker");

    // 创建NettyServerHandler，它是一个Netty中的ChannelHandler实现，
    // 不是Dubbo Remoting层的ChannelHandler接口的实现
    final NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
    // 获取当前NettyServer创建的所有Channel，这里的channels集合中的
    // Channel不是Netty中的Channel对象，而是Dubbo Remoting层的Channel对象
    channels = nettyServerHandler.getChannels();

    // 初始化ServerBootstrap，指定boss和worker EventLoopGroup
    bootstrap.group(bossGroup, workerGroup)
            .channel(NettyEventLoopFactory.serverSocketChannelClass())
            .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
            .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // 连接空闲超时时间
                    // FIXME: should we use getTimeout()?
                    int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
                    // NettyCodecAdapter中会创建Decoder和Encoder
                    NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                    if (getUrl().getParameter(SSL_ENABLED_KEY, false)) {
                        ch.pipeline().addLast("negotiation",
                                SslHandlerInitializer.sslServerHandler(getUrl(), nettyServerHandler));
                    }
                    ch.pipeline()
                            // 注册Decoder和Encoder
                            .addLast("decoder", adapter.getDecoder())
                            .addLast("encoder", adapter.getEncoder())
                            // 注册IdleStateHandler
                            .addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                            // 注册NettyServerHandler
                            .addLast("handler", nettyServerHandler);
                }
            });
    // bind
    // 绑定指定的地址和端口
    ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
    // 等待bind操作完成
    channelFuture.syncUninterruptibly();
    channel = channelFuture.channel();
}
```

NettyServer 实现的 doOpen() 基本流程类似：初始化 ServerBootstrap、创建 Boss EventLoopGroup 和 Worker EventLoopGroup、创建 ChannelInitializer 指定如何初始化 Channel 上的 ChannelHandler 等一系列 Netty 使用的标准化流程。

四个核心 ChannelHandler

首先是decoder 和 encoder，它们都是 NettyCodecAdapter 的内部类，分别继承了 Netty 中的 ByteToMessageDecoder 和 MessageToByteEncoder，可以关联到AbstractEndpoint 抽象类中的 codec 字段（Codec2 类型），InternalDecoder 和 InternalEncoder 会将真正的编解码功能委托给 NettyServer 关联的这个 Codec2 对象去处理，这里以 InternalDecoder 为例进行分析：
```
private class InternalDecoder extends ByteToMessageDecoder {
  protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) throws Exception {
   // 将ByteBuf封装成统一的ChannelBuffer
    ChannelBuffer message = new NettyBackedChannelBuffer(input);
   //拿到关联的Channel
    NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    do {
       //记录当前readerIndex的位置
      int saveReaderIndex = message.readerIndex();
     //委托给Codec2进行解码
      Object msg = codec.decode(channel, message);
     //当前接收到的数据不足一个消息的长度，会返回NEED_MORE_INPUT，
     //这里会重置readerIndex，继续等待接收更多的数据
      if (msg == Codec2.DecodeResult.NEED_MORE_INPUT) {
        message.readerIndex(saveReaderIndex);
        break;
      } else {
        if (msg != null) { // 将读取到的消息传递给后面的Handler处理
          out.add(msg);
        }
      }
    } while (message.readable());
  }
}
```

IdleStateHandler，它是 Netty 提供的一个工具型 ChannelHandler，用于定时心跳请求的功能或是自动关闭长时间空闲连接的功能。它的原理到底是怎样的呢？在 IdleStateHandler 中通过 lastReadTime、lastWriteTime 等几个字段，记录了最近一次读/写事件的时间，IdleStateHandler 初始化的时候，会创建一个定时任务，定时检测当前时间与最后一次读/写时间的差值。如果超过我们设置的阈值（也就是上面 NettyServer 中设置的 idleTimeout），就会触发 IdleStateEvent 事件，并传递给后续的 ChannelHandler 进行处理，后续 ChannelHandler 的 userEventTriggered() 方法会根据接收到的 IdleStateEvent 事件，决定是关闭长时间空闲的连接，还是发送心跳探活。

NettyServerHandler，它继承了 ChannelDuplexHandler，这是 Netty 提供的一个同时处理 Inbound 数据和 Outbound 数据的 ChannelHandler。

在 NettyServerHandler 中有 channels 和 handler 两个核心字段：
```
channels（Map<String,Channel>集合）：记录了当前 Server 创建的所有 Channel，连接创建（触发 channelActive() 方法）、连接断开（触发 channelInactive()方法）会操作 channels 集合进行相应的增删。

handler（ChannelHandler 类型）：NettyServerHandler 内几乎所有方法都会触发该 Dubbo ChannelHandler 对象。

在 NettyServer 创建 NettyServerHandler 的时候，可以看到下面的这行代码：
final NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
第二个参数传入的是 NettyServer 这个对象，它的最顶层父类 AbstractPeer 实现了 ChannelHandler，并且将所有的方法委托给其中封装的 ChannelHandler 对象。
```

从 AbstractPeer 开始往下，一路继承下来，NettyServer 拥有了 Endpoint、ChannelHandler 以及RemotingServer多个接口的能力，关联了一个 ChannelHandler 对象以及 Codec2 对象，并最终将数据委托给这两个对象进行处理，所以，上层调用方只需要实现 ChannelHandler 和 Codec2 这两个接口就可以了。










