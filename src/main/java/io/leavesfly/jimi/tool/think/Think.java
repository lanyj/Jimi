package io.leavesfly.jimi.tool.think;

import io.leavesfly.jimi.tool.AbstractTool;
import io.leavesfly.jimi.tool.ToolResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Think工具
 * 用于记录Agent的思考过程，不产生实际输出
 * 
 * 使用 @Scope("prototype") 使每次获取都是新实例
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Think extends AbstractTool<Think.Params> {
    
    private static final String NAME = "Think";
    private static final String DESCRIPTION = 
            "记录思考过程。用于内部思考，不会产生可见输出。";
    
    public Think() {
        super(NAME, DESCRIPTION, Params.class);
    }
    
    @Override
    public Mono<ToolResult> execute(Params params) {
        log.debug("Think: {}", params.getThought());
        
        return Mono.just(ToolResult.ok(
                "",  // 无输出
                "思考已记录"
        ));
    }
    
    /**
     * Think工具参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
        /**
         * 思考内容
         */
        private String thought;
    }
}
