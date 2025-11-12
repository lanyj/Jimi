package io.leavesfly.jimi.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作目录元数据
 * 存储某个工作目录的会话信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkDirMetadata {

    /**
     * 工作目录路径
     */
    @JsonProperty("path")
    private String path;

    /**
     * 最后使用的会话 ID
     */
    @JsonProperty("last_session_id")
    private String lastSessionId;

    /**
     * 历史会话 ID 列表
     */
    @JsonProperty("session_ids")
    @Builder.Default
    private List<String> sessionIds = new ArrayList<>();

    /**
     * 获取会话存储目录
     */
    public Path getSessionsDir() {
        // 使用工作目录路径的哈希值作为子目录名 在user.home统一管理
        String dirHash = Integer.toHexString(path.hashCode());
        return Paths.get(System.getProperty("user.home"), ".jimi", "sessions", dirHash);
    }
}
