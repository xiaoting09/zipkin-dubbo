package com.zipkin.dubbo.common;

import brave.Tracer;
import brave.Tracing;
import brave.http.HttpTracing;
import brave.propagation.SamplingFlags;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.alibaba.dubbo.rpc.RpcContext;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.urlconnection.URLConnectionSender;
import zipkin2.Endpoint;
import zipkin2.Span;

import java.util.Map;

import static brave.internal.HexCodec.lowerHexToUnsignedLong;

/**
 * Tracing获取配置
 *
 * @author 肖亭
 * @since 2017年11月04 11:30
 **/
public class TracingConfig {
    private TracingConfig() {
        getTracer();
    }

    private static Tracer tracer;

    private static HttpTracing httpTracing() {
        URLConnectionSender json = URLConnectionSender.json(PropertiesUtils.getProperty(DubboTraceConst.ZIP_CONF_URL) + "/api/v2/spans");
        AsyncReporter<Span> spanAsyncReporter = AsyncReporter.v2(json);
        Tracing myService = Tracing.newBuilder().localServiceName(getServiceName())
                .spanReporter(spanAsyncReporter)
                .build();
        return HttpTracing.newBuilder(myService).build();
    }

    private static void getTracer() {
        HttpTracing httpTracing = httpTracing();
        tracer = httpTracing.tracing().tracer();
    }

    private static TracingConfig tracingConfig;

    public static synchronized TracingConfig getSingle() {
        if (tracingConfig == null) {
            tracingConfig = new TracingConfig();
        }
        return tracingConfig;
    }


    private static String getServiceName() {
        String defaultName = RpcContext.getContext().getMethodName();
        String serviceName = PropertiesUtils.getProperty(DubboTraceConst.ZIP_CONF_NAME);
        return serviceName == null || serviceName.trim().length() == 0 ? defaultName : serviceName;
    }

    private TraceContextOrSamplingFlags extract(Map<String, String> carrier) {
        String sampledString = carrier.get(DubboTraceConst.SAMPLED_NAME);

        Boolean sampled = sampledString != null
                ? sampledString.equals("1") || sampledString.equalsIgnoreCase("true")
                : null;
        boolean debug = "1".equals(carrier.get(DubboTraceConst.FLAGS_NAME));

        String traceIdString = carrier.get(DubboTraceConst.TRACE_ID_NAME);
        if (traceIdString == null) { // return early if there's no trace ID
            return TraceContextOrSamplingFlags.create(
                    new SamplingFlags.Builder().sampled(true).debug(debug).build()
            );
        }

        TraceContext.Builder result = TraceContext.newBuilder().sampled(sampled).debug(debug);
        result.traceIdHigh(
                traceIdString.length() == 32 ? lowerHexToUnsignedLong(traceIdString, 0) : 0
        );
        result.traceId(lowerHexToUnsignedLong(traceIdString));
        String spanIdString = carrier.get(DubboTraceConst.SPAN_ID_NAME);
        if (spanIdString != null) {
            result.spanId(lowerHexToUnsignedLong(spanIdString));
        }
        String parentSpanIdString = carrier.get(DubboTraceConst.PARENT_SPAN_ID_NAME);
        if (parentSpanIdString != null) {
            result.parentId(lowerHexToUnsignedLong(parentSpanIdString));
        }
        return TraceContextOrSamplingFlags.create(result);
    }


    private brave.Span nextSpan(TraceContextOrSamplingFlags contextOrFlags) {
        TraceContext extracted = contextOrFlags.context();
        if (extracted != null) {
            // If there were trace IDs in the request and a sampling decision, honor it
            if (extracted.sampled() != null) {
                return tracer.joinSpan(contextOrFlags.context());
            }
            // Otherwise, try to make a new decision
            return tracer.joinSpan(extracted.toBuilder()
                    .sampled(SamplingFlags.SAMPLED.sampled())
                    .build());
        }

        // There was no trace in the incoming requests. However, there might be sampling flags
        SamplingFlags flags = contextOrFlags.samplingFlags();
        if (flags.sampled() == null) {
            flags = new SamplingFlags.Builder()
                    .sampled(SamplingFlags.SAMPLED.sampled())
                    .debug(flags.debug()) // should always be false if unsampled!
                    .build();
        }
        return tracer.newTrace(flags);
    }

    public brave.Span handleReceive(Map<String, String> carrier) {
        brave.Span span = nextSpan(extract(carrier));
        if (span.isNoop()) {
            return span;
        }
        String path = RpcContext.getContext().getMethodName();
        if (path != null) {
            span.tag("rpc.methodName", path);
        }
        Endpoint.Builder remoteEndpoint = Endpoint.newBuilder();
        if (parseClientAddress(remoteEndpoint)) {
            span.remoteEndpoint(remoteEndpoint.build());
        }
        return span;
    }

    public boolean parseClientAddress(Endpoint.Builder builder) {
        String ip = RpcContext.getContext().getUrl().getIp();
        if (builder.parseIp(ip)) {
            builder.port(RpcContext.getContext().getUrl().getPort());
            return true;
        }
        return false;
    }
}
