package io.leavesfly.jimi.wire.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 异步子代理完成消息
 * 当异步子代理完成执行（成功/失败/取消/超时）时发送
 * 
 * @author Jimi
 */
@Getter
@RequiredArgsConstructor
public class AsyncSubagentCompleted implements WireMessage {
    
    /**
     * 子代理唯一标识
     */
    private final String subagentId;
    
    /**
     * 执行结果或错误信息
     */
    private final String result;
    
    /**
     * 是否成功完成
     */
    private final boolean success;
    
    /**
     * 运行时长
     */
    private final Duration duration;
    
    @Override
    public String getMessageType() {
        return "AsyncSubagentCompleted";
    }
}
