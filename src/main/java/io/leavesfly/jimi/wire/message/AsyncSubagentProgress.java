package io.leavesfly.jimi.wire.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 异步子代理进度消息
 * 用于报告异步子代理的执行进度
 * 
 * @author Jimi
 */
@Getter
@RequiredArgsConstructor
public class AsyncSubagentProgress implements WireMessage {
    
    /**
     * 子代理唯一标识
     */
    private final String subagentId;
    
    /**
     * 进度信息描述
     */
    private final String progressInfo;
    
    /**
     * 当前步骤编号
     */
    private final int stepNumber;
    
    @Override
    public String getMessageType() {
        return "AsyncSubagentProgress";
    }
}
