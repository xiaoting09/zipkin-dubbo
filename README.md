# zipkin-demo

zipKin测试用例

### dubbo接入方法:
1.将dubbo-common打成jar添加依赖

2.在resources目录下新建META-INF/dubbo目录添加com.alibaba.dubbo.rpc.Filter文件

3.Filter文件中添加消费者,提供者Filter配置
```
traceFiltersService=com.zipkin.dubbo.DrpcServerInterceptor (提供者)

traceFiltercClient=com.zipkin.dubbo.DrpcClientInterceptor(消费者)

```

4.在config.properties，或者application.properties文件中配置
```
zipkin.serviceUrl（zipkin服务器地址）
zipkin.serviceName(当前服务名称)
```
