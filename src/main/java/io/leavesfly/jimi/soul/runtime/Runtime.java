package io.leavesfly.jimi.soul.runtime;

import io.leavesfly.jimi.config.JimiConfig;
import io.leavesfly.jimi.llm.LLM;
import io.leavesfly.jimi.session.Session;
import io.leavesfly.jimi.soul.approval.Approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

/**
 * Runtime 运行时上下文
 * 包含 Agent 运行所需的所有全局状态和服务
 * 
 * 功能特性：
 * 1. 配置管理（JimiConfig）
 * 2. LLM 实例（LLM）
 * 3. 会话信息（Session）
 * 4. 内置参数（BuiltinSystemPromptArgs）
 * 5. 审批服务（Approval）
 * 
 * 设计理念：
 * - Runtime 是不可变的数据容器，由 JimiFactory 负责创建和组装
 * - 不包含创建逻辑，符合单一职责原则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Runtime {

    /**
     * 全局配置
     */
    private JimiConfig config;

    /**
     * LLM 实例
     */
    private LLM llm;

    /**
     * 会话信息
     */
    private Session session;

    /**
     * 内置系统提示词参数
     */
    private BuiltinSystemPromptArgs builtinArgs;


    /**
     * 审批机制
     */
    private Approval approval;

    /**
     * 获取工作目录
     */
    public Path getWorkDir() {
        return session.getWorkDir();
    }

    /**
     * 获取会话 ID
     */
    public String getSessionId() {
        return session.getId();
    }

    /**
     * 检查是否为YOLO模式
     */
    public boolean isYoloMode() {
        return approval != null && approval.isYolo();
    }
}
