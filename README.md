# zipkin-demo

zipKin测试用例

### dubbo接入方法:
1.进入dubbo-common目录下将dubbo-common打成jar添加依赖 (需要本地安装gradle文件)
```
gradle clean install
```

2.在resources目录下新建META-INF/dubbo目录添加com.alibaba.dubbo.rpc.Filter文件

3.Filter文件中添加消费者,提供者Filter配置
```
traceFiltersService=com.zipkin.dubbo.DrpcServerInterceptor (提供者)

traceFiltercClient=com.zipkin.dubbo.DrpcClientInterceptor(消费者)

```

4.在config.properties，或者application.properties文件中配置
```
zipkin.serviceUrl=zipkin服务器地址
zipkin.serviceName=当前服务名称 (可选)
kafka.serviceUrl=127.0.0.1:9092 kafka服务器地址 
```

5、如果配置了kafka.serviceUrl地址,则默认使用kafkaController
