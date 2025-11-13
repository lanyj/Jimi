package io.leavesfly.jimi.wire;

import io.leavesfly.jimi.wire.message.WireMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Wire 消息总线实现
 * 使用 Reactor Sinks 实现消息的异步传递
 * 使用 replay().autoConnect() 确保多个订阅者共享同一个流,避免重复消息
 */
public class WireImpl implements Wire {
    
    private final Sinks.Many<WireMessage> sink;
    private final Flux<WireMessage> sharedFlux;
    
    public WireImpl() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        // 使用 share() 确保所有订阅者共享同一个上游订阅,避免重复消息
        this.sharedFlux = sink.asFlux().share();
    }
    
    @Override
    public void send(WireMessage message) {
        sink.tryEmitNext(message);
    }
    
    @Override
    public Flux<WireMessage> asFlux() {
        return sharedFlux;
    }
    
    @Override
    public void complete() {
        sink.tryEmitComplete();
    }
}
