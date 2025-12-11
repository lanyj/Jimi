package io.leavesfly.jimi.wire.message;

import io.leavesfly.jimi.llm.ChatCompletionResult;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Token 使用统计消息
 * 用于在 Wire 中传递 LLM Token 使用信息
 */
@Data
@AllArgsConstructor
public class TokenUsageMessage implements WireMessage {
    
    /**
     * Token 使用统计
     */
    private ChatCompletionResult.Usage usage;
    
    @Override
    public String getMessageType() {
        return "token_usage";
    }
}
