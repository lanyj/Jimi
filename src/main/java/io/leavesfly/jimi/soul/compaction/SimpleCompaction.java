package io.leavesfly.jimi.soul.compaction;

import io.leavesfly.jimi.llm.LLM;
import io.leavesfly.jimi.llm.message.ContentPart;
import io.leavesfly.jimi.llm.message.Message;
import io.leavesfly.jimi.llm.message.MessageRole;
import io.leavesfly.jimi.llm.message.TextPart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 简单上下文压缩实现
 * 保留最近的N条用户/助手消息，压缩更早的历史
 */
@Service
@Slf4j
public class SimpleCompaction implements Compaction {
    
    /**
     * 保留的最近消息数量（用户+助手成对消息）
     */
    private static final int MAX_PRESERVED_MESSAGES = 2;
    
    /**
     * 压缩提示词模板
     */
    private static final String COMPACT_TEMPLATE = """
            请帮我总结压缩以下对话历史，保留关键信息和上下文：
            
            %s
            
            请提供一个简洁的总结，包含：
            1. 用户的主要问题和需求
            2. 已经完成的关键操作
            3. 重要的上下文信息
            
            总结应该清晰简洁，便于后续对话参考。
            """;
    
    @Override
    public Mono<List<Message>> compact(List<Message> messages, LLM llm) {
        return Mono.defer(() -> {
            List<Message> history = new ArrayList<>(messages);
            
            if (history.isEmpty()) {
                return Mono.just(history);
            }
            
            // 找到需要保留的起始位置
            int preserveStartIndex = history.size();
            int nPreserved = 0;
            
            for (int index = history.size() - 1; index >= 0; index--) {
                Message msg = history.get(index);
                if (msg.getRole() == MessageRole.USER || msg.getRole() == MessageRole.ASSISTANT) {
                    nPreserved++;
                    if (nPreserved == MAX_PRESERVED_MESSAGES) {
                        preserveStartIndex = index;
                        break;
                    }
                }
            }
            
            // 如果保留的消息不足，不进行压缩
            if (nPreserved < MAX_PRESERVED_MESSAGES) {
                log.debug("Not enough messages to compact (only {} preserved)", nPreserved);
                return Mono.just(history);
            }
            
            List<Message> toCompact = history.subList(0, preserveStartIndex);
            List<Message> toPreserve = history.subList(preserveStartIndex, history.size());
            
            if (toCompact.isEmpty()) {
                log.debug("No messages to compact");
                return Mono.just(toPreserve);
            }
            
            log.info("Compacting {} messages, preserving {} messages", toCompact.size(), toPreserve.size());
            
            // 将历史转换为文本
            StringBuilder historyText = new StringBuilder();
            for (int i = 0; i < toCompact.size(); i++) {
                Message msg = toCompact.get(i);
                historyText.append(String.format("## Message %d\n", i + 1));
                historyText.append(String.format("Role: %s\n", msg.getRole().getValue()));
                historyText.append(String.format("Content: %s\n\n", msg.getTextContent()));
            }
            
            // 构建压缩提示词
            String compactPrompt = String.format(COMPACT_TEMPLATE, historyText.toString());
            
            // 调用 LLM 进行压缩
            return llm.getChatProvider()
                .generate(
                    "You are a helpful assistant that compacts conversation context.",
                    List.of(Message.user(compactPrompt)),
                    Collections.emptyList()
                )
                .map(result -> {
                    // 记录使用统计
                    if (result.getUsage() != null) {
                        log.debug("Compaction used {} input tokens and {} output tokens",
                            result.getUsage().getPromptTokens(),
                            result.getUsage().getCompletionTokens());
                    }
                    
                    // 构建压缩后的消息
                    List<Message> compactedMessages = new ArrayList<>();
                    
                    // 添加系统消息说明
                    List<ContentPart> content = new ArrayList<>();
                    content.add(TextPart.of("Previous context has been compacted. Here is the compaction output:"));
                    
                    // 添加压缩结果
                    String compactedText = result.getMessage().getTextContent();
                    if (compactedText != null && !compactedText.isEmpty()) {
                        content.add(TextPart.of(compactedText));
                    }
                    
                    // 使用 builder 创建消息
                    Message compactedMsg = Message.builder()
                        .role(MessageRole.ASSISTANT)
                        .content(content)
                        .build();
                    
                    compactedMessages.add(compactedMsg);
                    compactedMessages.addAll(toPreserve);
                    
                    log.info("Context compacted: {} → {} messages", history.size(), compactedMessages.size());
                    
                    return compactedMessages;
                })
                .onErrorResume(e -> {
                    log.error("Compaction failed, keeping original messages", e);
                    return Mono.just(history);
                });
        });
    }
}
