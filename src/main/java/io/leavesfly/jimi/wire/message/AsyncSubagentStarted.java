package io.leavesfly.jimi.wire.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * 异步子代理启动消息
 * 当异步子代理启动时发送到 Wire
 * 
 * @author Jimi
 */
@Getter
@RequiredArgsConstructor
public class AsyncSubagentStarted implements WireMessage {
    
    /**
     * 子代理唯一标识
     */
    private final String subagentId;
    
    /**
     * 子代理名称
     */
    private final String subagentName;
    
    /**
     * 运行模式
     */
    private final String mode;
    
    /**
     * 启动时间
     */
    private final Instant startTime;
    
    @Override
    public String getMessageType() {
        return "AsyncSubagentStarted";
    }
}
