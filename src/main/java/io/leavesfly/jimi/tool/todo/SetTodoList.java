package io.leavesfly.jimi.tool.todo;

import io.leavesfly.jimi.tool.AbstractTool;
import io.leavesfly.jimi.tool.ToolResult;
import io.leavesfly.jimi.tool.ToolResultBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置待办事项列表工具
 * 用于管理和显示待办事项
 * 
 * 使用 @Scope("prototype") 使每次获取都是新实例
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SetTodoList extends AbstractTool<SetTodoList.Params> {
    
    /**
     * 待办事项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Todo {
        /**
         * 待办事项标题
         */
        private String title;
        
        /**
         * 待办事项状态：Pending, In Progress, Done
         */
        private String status;
    }
    
    /**
     * 参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
        /**
         * 待办事项列表（基础列表）
         */
        @Builder.Default
        private List<Todo> todos = new ArrayList<>();
        
        /**
         * 对现有待办的状态更新（按标题匹配）
         */
        @Builder.Default
        private List<Todo> updates = new ArrayList<>();
        
        /**
         * 需要新增的待办项（当标题不存在时添加）
         */
        @Builder.Default
        private List<Todo> adds = new ArrayList<>();
        
        /**
         * 是否移除已完成（Done）的待办
         */
        @Builder.Default
        private boolean removeCompleted = false;
    }
    
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "Pending";
        String s = status.trim();
        if (s.equalsIgnoreCase("pending")) return "Pending";
        if (s.equalsIgnoreCase("inprogress") || s.equalsIgnoreCase("in progress") || s.equalsIgnoreCase("in_progress")) return "In Progress";
        if (s.equalsIgnoreCase("done") || s.equalsIgnoreCase("completed") || s.equalsIgnoreCase("complete")) return "Done";
        return "Pending";
    }
    
    public SetTodoList() {
        super(
            "SetTodoList",
            """
            更新整份待办事项列表（Todo List）。
            
            使用场景：
            - 当一个任务包含多个子任务/里程碑时，用于拆解并追踪进度
            - 当一次请求中包含多项任务时，用于统一维护它们的状态
            
            使用约束：
            - 这是唯一的待办工具，每次操作都需要提交“完整的”待办列表
            - 请正确维护每项的标题与状态（状态仅支持：Pending、In Progress、Done）
            
            参数：
            - todos：待办事项数组，每项包含 title 与 status
            
            示例：
            {
              "todos": [
                {"title": "Research API integration", "status": "Done"},
                {"title": "Implement authentication", "status": "In Progress"},
                {"title": "Write unit tests", "status": "Pending"}
              ]
            }
            """,
            Params.class
        );
    }
    
    @Override
    public Mono<ToolResult> execute(Params params) {
        return Mono.defer(() -> {
            ToolResultBuilder trb = new ToolResultBuilder();
            
            List<Todo> list = new ArrayList<>();
            if (params != null && params.getTodos() != null) {
                for (Todo t : params.getTodos()) {
                    String title = (t != null && t.getTitle() != null) ? t.getTitle().trim() : "(未命名)";
                    String status = (t != null) ? normalizeStatus(t.getStatus()) : "Pending";
                    list.add(Todo.builder().title(title).status(status).build());
                }
            }
            
            // 更新现有待办的状态（按标题匹配）
            if (params != null && params.getUpdates() != null) {
                for (Todo upd : params.getUpdates()) {
                    if (upd == null || upd.getTitle() == null) continue;
                    String title = upd.getTitle().trim();
                    String newStatus = normalizeStatus(upd.getStatus());
                    for (Todo t : list) {
                        if (t.getTitle() != null && t.getTitle().equals(title)) {
                            t.setStatus(newStatus);
                            break;
                        }
                    }
                }
            }
            
            // 添加新的待办（仅当标题不存在时添加）
            if (params != null && params.getAdds() != null) {
                for (Todo add : params.getAdds()) {
                    if (add == null || add.getTitle() == null) continue;
                    String title = add.getTitle().trim();
                    String status = normalizeStatus(add.getStatus());
                    boolean exists = false;
                    for (Todo t : list) {
                        if (t.getTitle() != null && t.getTitle().equals(title)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        list.add(Todo.builder().title(title).status(status).build());
                    }
                }
            }
            
            // 移除完成项
            if (params != null && params.isRemoveCompleted()) {
                List<Todo> filtered = new ArrayList<>();
                for (Todo t : list) {
                    String s = normalizeStatus(t.getStatus());
                    if (!"Done".equals(s)) {
                        filtered.add(t);
                    }
                }
                list = filtered;
            }
            
            if (list.isEmpty()) {
                trb.write("暂无待办事项。\n");
                return Mono.just(trb.ok("空的待办清单", "空列表"));
            }
            
            int pending = 0, inProgress = 0, done = 0;
            for (Todo t : list) {
                String s = normalizeStatus(t.getStatus());
                switch (s) {
                    case "Pending": pending++; break;
                    case "In Progress": inProgress++; break;
                    case "Done": done++; break;
                    default: pending++; break;
                }
                trb.write(String.format("- %s [%s]\n", t.getTitle(), s));
            }
            
            String brief = String.format("共%d项：Pending %d, In Progress %d, Done %d", list.size(), pending, inProgress, done);
            log.info("Todo list applied: size={}, pending={}, inProgress={}, done={}", list.size(), pending, inProgress, done);
            
            return Mono.just(trb.ok("待办清单已更新" , brief));
        });
    }
}
