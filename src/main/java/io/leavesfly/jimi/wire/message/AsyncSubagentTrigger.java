package io.leavesfly.jimi.wire.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 异步子代理触发消息
 * Watch 模式下，当监控内容匹配到触发模式时发送
 * 
 * @author Jimi
 */
@Getter
@RequiredArgsConstructor
public class AsyncSubagentTrigger implements WireMessage {
    
    /**
     * 子代理唯一标识
     */
    private final String subagentId;
    
    /**
     * 匹配的模式
     */
    private final String matchedPattern;
    
    /**
     * 匹配的内容（触发行）
     */
    private final String matchedContent;
    
    /**
     * 触发时间戳
     */
    private final java.time.Instant triggerTime;
    
    @Override
    public String getMessageType() {
        return "AsyncSubagentTrigger";
    }
}
