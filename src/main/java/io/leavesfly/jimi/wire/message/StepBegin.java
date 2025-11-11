package io.leavesfly.jimi.wire.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 步骤开始消息
 */
@Data
@NoArgsConstructor
public class StepBegin implements WireMessage {
    
    /**
     * 步骤编号
     */
    private int stepNumber;
    
    /**
     * 是否为子Agent的步骤（默认false，即主Agent）
     */
    private boolean isSubagent;
    
    /**
     * 子Agent的名称（仅当isSubagent=true时有值）
     */
    private String agentName;
    
    /**
     * 主Agent构造函数
     */
    public StepBegin(int stepNumber) {
        this.stepNumber = stepNumber;
        this.isSubagent = false;
        this.agentName = null;
    }
    
    /**
     * 子Agent构造函数
     */
    public StepBegin(int stepNumber, boolean isSubagent) {
        this.stepNumber = stepNumber;
        this.isSubagent = isSubagent;
        this.agentName = null;
    }
    
    /**
     * 完整构造函数（包含Agent名称）
     */
    public StepBegin(int stepNumber, boolean isSubagent, String agentName) {
        this.stepNumber = stepNumber;
        this.isSubagent = isSubagent;
        this.agentName = agentName;
    }
    
    @Override
    public String getMessageType() {
        return "StepBegin";
    }
}
