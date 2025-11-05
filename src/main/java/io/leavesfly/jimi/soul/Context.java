package io.leavesfly.jimi.soul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.leavesfly.jimi.llm.message.Message;
import io.leavesfly.jimi.llm.message.MessageRole;
import io.leavesfly.jimi.llm.message.TextPart;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 上下文管理器
 * 负责维护对话历史、Token 计数和检查点机制
 * 
 * 功能特性：
 * 1. 消息历史管理（追加、查询、清空）
 * 2. Token 计数追踪
 * 3. 检查点机制（创建、回退）
 * 4. 文件持久化（JSONL 格式）
 * 5. 上下文恢复
 * 
 * 创建方式：
 * - 使用静态工厂方法 {@link #create(Path, ObjectMapper)} 确保完整初始化
 * - 或使用构造函数 + 手动调用 {@link #restore()}（不推荐）
 */
@Slf4j
public class Context {
    
    private final Path fileBackend;
    private final ObjectMapper objectMapper;
    
    /**
     * 消息历史列表（只读视图对外暴露）
     */
    private final List<Message> history;
    
    /**
     * Token 计数
     */
    private int tokenCount;
    
    /**
     * 下一个检查点 ID（从 0 开始递增）
     */
    private int nextCheckpointId;
    
    public Context(Path fileBackend, ObjectMapper objectMapper) {
        this.fileBackend = fileBackend;
        this.objectMapper = objectMapper;
        this.history = new ArrayList<>();
        this.tokenCount = 0;
        this.nextCheckpointId = 0;
    }
    
    /**
     * 静态工厂方法：创建并恢复 Context
     * 确保 Context 实例完整初始化（推荐使用）
     * 
     * @param fileBackend 文件后端路径
     * @param objectMapper JSON 序列化工具
     * @return 完整初始化的 Context 实例
     */
    public static Mono<Context> create(Path fileBackend, ObjectMapper objectMapper) {
        return Mono.fromCallable(() -> {
            Context context = new Context(fileBackend, objectMapper);
            // 同步执行 restore 逻辑
            context.restoreSync();
            return context;
        });
    }
    
    /**
     * 从文件后端恢复上下文（异步版本）
     * 
     * @return 是否成功恢复（true 表示恢复了数据，false 表示文件不存在或为空）
     */
    public Mono<Boolean> restore() {
        return Mono.fromCallable(this::restoreSync);
    }
    
    /**
     * 从文件后端恢复上下文（同步版本）
     * 由静态工厂方法内部使用
     */
    private boolean restoreSync() {
        try {
            log.debug("Restoring context from file: {}", fileBackend);
            
            if (!history.isEmpty()) {
                log.error("The context storage is already modified");
                throw new IllegalStateException("The context storage is already modified");
            }
            
            if (!Files.exists(fileBackend)) {
                log.debug("No context file found, skipping restoration");
                return false;
            }
            
            if (Files.size(fileBackend) == 0) {
                log.debug("Empty context file, skipping restoration");
                return false;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(fileBackend)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    ObjectNode lineJson = objectMapper.readValue(line, ObjectNode.class);
                    String role = lineJson.get("role").asText();
                    
                    // 处理特殊的元数据行
                    if ("_usage".equals(role)) {
                        this.tokenCount = lineJson.get("token_count").asInt();
                        continue;
                    }
                    
                    if ("_checkpoint".equals(role)) {
                        this.nextCheckpointId = lineJson.get("id").asInt() + 1;
                        continue;
                    }
                    
                    // 解析为普通消息
                    Message message = objectMapper.readValue(line, Message.class);
                    history.add(message);
                }
                
                log.info("Restored context: {} messages, {} tokens, {} checkpoints", 
                        history.size(), tokenCount, nextCheckpointId);
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to restore context from file", e);
            throw new RuntimeException("Failed to restore context", e);
        }
    }
    
    /**
     * 追加单条或多条消息到上下文
     * 
     * @param messages 单条消息或消息列表
     */
    public Mono<Void> appendMessage(Object messages) {
        return Mono.fromRunnable(() -> {
            log.debug("Appending message(s) to context: {}", messages);
            
            List<Message> messageList;
            if (messages instanceof Message) {
                messageList = Collections.singletonList((Message) messages);
            } else if (messages instanceof List) {
                messageList = (List<Message>) messages;
            } else {
                throw new IllegalArgumentException("Messages must be Message or List<Message>");
            }
            
            history.addAll(messageList);
            
            // 持久化到文件
            try (BufferedWriter writer = Files.newBufferedWriter(fileBackend, 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                for (Message message : messageList) {
                    String json = objectMapper.writeValueAsString(message);
                    writer.write(json);
                    writer.newLine();
                }
            } catch (IOException e) {
                log.error("Failed to persist messages to file", e);
                throw new RuntimeException("Failed to persist messages", e);
            }
        });
    }
    
    /**
     * 更新 Token 计数并持久化
     * 
     * @param count 新的 token 计数
     */
    public Mono<Void> updateTokenCount(int count) {
        return Mono.fromRunnable(() -> {
            log.debug("Updating token count in context: {}", count);
            this.tokenCount = count;
            
            // 持久化 token 计数
            try (BufferedWriter writer = Files.newBufferedWriter(fileBackend,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                ObjectNode usageNode = objectMapper.createObjectNode();
                usageNode.put("role", "_usage");
                usageNode.put("token_count", count);
                writer.write(objectMapper.writeValueAsString(usageNode));
                writer.newLine();
            } catch (IOException e) {
                log.error("Failed to persist token count", e);
                throw new RuntimeException("Failed to persist token count", e);
            }
        });
    }
    
    /**
     * 创建检查点
     * 
     * @param addUserMessage 是否添加用户可见的检查点消息
     * @return 检查点 ID
     */
    public Mono<Integer> checkpoint(boolean addUserMessage) {
        return Mono.fromCallable(() -> {
            int checkpointId = nextCheckpointId++;
            log.debug("Checkpointing, ID: {}", checkpointId);
            
            // 持久化检查点
            try (BufferedWriter writer = Files.newBufferedWriter(fileBackend,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                ObjectNode checkpointNode = objectMapper.createObjectNode();
                checkpointNode.put("role", "_checkpoint");
                checkpointNode.put("id", checkpointId);
                writer.write(objectMapper.writeValueAsString(checkpointNode));
                writer.newLine();
            } catch (IOException e) {
                log.error("Failed to persist checkpoint", e);
                throw new RuntimeException("Failed to persist checkpoint", e);
            }
            
            // 可选：添加用户可见的检查点消息
            if (addUserMessage) {
                Message checkpointMessage = Message.builder()
                        .role(MessageRole.USER)
                        .content(List.of(TextPart.of("<system>CHECKPOINT " + checkpointId + "</system>")))
                        .build();
                appendMessage(checkpointMessage).block();
            }
            
            return checkpointId;
        });
    }
    
    /**
     * 回退到指定检查点
     * 回退后，指定检查点及之后的所有内容将被移除
     * 原文件会被轮转保存
     * 
     * @param checkpointId 检查点 ID（0 是第一个检查点）
     */
    public Mono<Void> revertTo(int checkpointId) {
        return Mono.fromRunnable(() -> {
            log.debug("Reverting checkpoint, ID: {}", checkpointId);
            
            if (checkpointId >= nextCheckpointId) {
                log.error("Checkpoint {} does not exist", checkpointId);
                throw new IllegalArgumentException("Checkpoint " + checkpointId + " does not exist");
            }
            
            try {
                // 轮转历史文件
                Path rotatedPath = getNextRotationPath();
                Files.move(fileBackend, rotatedPath, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Rotated history file: {}", rotatedPath);
                
                // 清空内存状态
                history.clear();
                tokenCount = 0;
                nextCheckpointId = 0;
                
                // 从轮转文件恢复到指定检查点
                try (BufferedReader reader = Files.newBufferedReader(rotatedPath);
                     BufferedWriter writer = Files.newBufferedWriter(fileBackend,
                             StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }
                        
                        ObjectNode lineJson = objectMapper.readValue(line, ObjectNode.class);
                        String role = lineJson.get("role").asText();
                        
                        // 遇到目标检查点时停止
                        if ("_checkpoint".equals(role) && lineJson.get("id").asInt() == checkpointId) {
                            break;
                        }
                        
                        // 写入新文件
                        writer.write(line);
                        writer.newLine();
                        
                        // 恢复到内存
                        if ("_usage".equals(role)) {
                            this.tokenCount = lineJson.get("token_count").asInt();
                        } else if ("_checkpoint".equals(role)) {
                            this.nextCheckpointId = lineJson.get("id").asInt() + 1;
                        } else {
                            Message message = objectMapper.readValue(line, Message.class);
                            history.add(message);
                        }
                    }
                }
                
                log.info("Reverted to checkpoint {}: {} messages, {} tokens", 
                        checkpointId, history.size(), tokenCount);
            } catch (IOException e) {
                log.error("Failed to revert to checkpoint", e);
                throw new RuntimeException("Failed to revert to checkpoint", e);
            }
        });
    }
    
    /**
     * 获取下一个可用的轮转文件路径
     */
    private Path getNextRotationPath() throws IOException {
        Path parent = fileBackend.getParent();
        String baseName = fileBackend.getFileName().toString();
        
        for (int i = 1; i < 1000; i++) {
            Path rotatedPath = parent.resolve(baseName + "." + i);
            if (!Files.exists(rotatedPath)) {
                return rotatedPath;
            }
        }
        
        throw new IOException("No available rotation path found");
    }
    
    /**
     * 获取消息历史（只读视图）
     */
    public List<Message> getHistory() {
        return Collections.unmodifiableList(history);
    }
    
    /**
     * 获取 Token 计数
     */
    public int getTokenCount() {
        return tokenCount;
    }
    
    /**
     * 获取检查点数量
     */
    public int getnCheckpoints() {
        return nextCheckpointId;
    }
}
