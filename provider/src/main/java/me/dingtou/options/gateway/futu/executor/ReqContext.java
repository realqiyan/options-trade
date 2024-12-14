package me.dingtou.options.gateway.futu.executor;

import com.google.protobuf.GeneratedMessageV3;

public class ReqContext {
    public final Object syncEvent = new Object();
    public GeneratedMessageV3 resp;

    ReqContext() {
    }
}
