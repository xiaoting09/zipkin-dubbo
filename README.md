# zipkin-demo

zipKin测试用例

### dubbo接入方法:
1.引入dubbo与zipkin依赖 
```
    braveVersion="4.7.1"
    reporterVersion="1.1.1"
    
    compile group: 'com.alibaba', name: 'dubbo', version: '2.5.3'
    compile group: 'io.zipkin.brave', name: 'brave-spancollector-http', version:"$braveVersion"
    compile group: 'io.zipkin.brave', name: 'brave-instrumentation-http', version:"$braveVersion"
    compile group: 'io.zipkin.brave', name: 'brave-instrumentation-parent', version:"$braveVersion"
    compile group: 'io.zipkin.reporter', name: 'zipkin-sender-urlconnection', version:"1.1.1"
    (将dubbo-common打成jar添加依赖)
    
```
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
