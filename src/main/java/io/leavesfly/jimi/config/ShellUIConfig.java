package io.leavesfly.jimi.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shell UI 配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShellUIConfig {
    
    /**
     * 是否显示进度指示器（旋转动画）
     */
    @JsonProperty("show-spinner")
    @Builder.Default
    private boolean showSpinner = true;
    
    /**
     * 是否显示 Token 消耗统计
     */
    @JsonProperty("show-token-usage")
    @Builder.Default
    private boolean showTokenUsage = true;
    
    /**
     * 工具调用显示模式
     * - full: 完整模式，显示所有工具调用细节
     * - compact: 紧凑模式，只显示工具名称和关键信息
     * - minimal: 最小模式，只显示工具调用数量
     */
    @JsonProperty("tool-display-mode")
    @Builder.Default
    private String toolDisplayMode = "full";
    
    /**
     * 工具参数截断长度（紧凑模式下）
     */
    @JsonProperty("tool-args-truncate-length")
    @Builder.Default
    private int toolArgsTruncateLength = 100;
    
    /**
     * 旋转动画类型
     * - dots: 点状动画 ⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏
     * - arrows: 箭头动画 ←↖↑↗→↘↓↙
     * - circles: 圆圈动画 ◐◓◑◒
     */
    @JsonProperty("spinner-type")
    @Builder.Default
    private String spinnerType = "dots";
    
    /**
     * 旋转动画更新间隔（毫秒）
     */
    @JsonProperty("spinner-interval-ms")
    @Builder.Default
    private int spinnerIntervalMs = 100;
    
    /**
     * 提示符显示样式
     * - simple: 简洁模式，只显示图标和名称
     * - normal: 标准模式，显示图标、名称和状态
     * - rich: 丰富模式，显示图标、名称、状态、消息数和Token数
     */
    @JsonProperty("prompt-style")  // 使用 kebab-case 与 YAML 配置一致
    @Builder.Default
    private String promptStyle = "normal";
    
    /**
     * 是否在提示符中显示时间
     */
    @JsonProperty("show-time-in-prompt")  // 使用 kebab-case 与 YAML 配置一致
    @Builder.Default
    private boolean showTimeInPrompt = false;
    
    /**
     * 是否在提示符中显示上下文统计（消息数/Token数）
     */
    @JsonProperty("show-context-stats")  // 使用 kebab-case 与 YAML 配置一致
    @Builder.Default
    private boolean showContextStats = true;
    
    /**
     * 是否显示快捷操作提示
     */
    @JsonProperty("show-shortcuts-hint")
    @Builder.Default
    private boolean showShortcutsHint = true;
    
    /**
     * 快捷提示显示频率
     * - always: 总是显示
     * - first_time: 仅首次显示
     * - periodic: 定期显示（每N次交互）
     */
    @JsonProperty("shortcuts-hint-frequency")
    @Builder.Default
    private String shortcutsHintFrequency = "first_time";
    
    /**
     * 定期显示的间隔（交互次数）
     */
    @JsonProperty("shortcuts-hint-interval")
    @Builder.Default
    private int shortcutsHintInterval = 10;
    
    /**
     * 主题配置
     */
    @JsonProperty("theme")
    @Builder.Default
    private ThemeConfig theme = ThemeConfig.defaultTheme();
    
    /**
     * 主题名称（用于选择预设主题）
     * 可选值: default, dark, light, minimal, matrix
     */
    @JsonProperty("theme-name")
    @Builder.Default
    private String themeName = "default";
    
    // ==================== 通知配置 ====================
    
    /**
     * 是否启用桌面通知（异步任务完成/触发时）
     */
    @JsonProperty("enable-desktop-notification")
    @Builder.Default
    private boolean enableDesktopNotification = true;
    
    /**
     * 是否启用终端提示音（Bell）
     */
    @JsonProperty("enable-notification-sound")
    @Builder.Default
    private boolean enableNotificationSound = true;
    
    /**
     * 检查配置有效性
     */
    public void validate() {
        // 验证工具显示模式
        if (!toolDisplayMode.equals("full") && !toolDisplayMode.equals("compact") && !toolDisplayMode.equals("minimal")) {
            throw new IllegalStateException(
                String.format("Invalid tool_display_mode: '%s'. Must be one of: full, compact, minimal", toolDisplayMode)
            );
        }
        
        // 验证旋转动画类型
        if (!spinnerType.equals("dots") && !spinnerType.equals("arrows") && !spinnerType.equals("circles")) {
            throw new IllegalStateException(
                String.format("Invalid spinner_type: '%s'. Must be one of: dots, arrows, circles", spinnerType)
            );
        }
        
        // 验证提示符样式
        if (!promptStyle.equals("simple") && !promptStyle.equals("normal") && !promptStyle.equals("rich")) {
            throw new IllegalStateException(
                String.format("Invalid prompt_style: '%s'. Must be one of: simple, normal, rich", promptStyle)
            );
        }
        
        // 验证快捷提示频率
        if (!shortcutsHintFrequency.equals("always") && 
            !shortcutsHintFrequency.equals("first_time") && 
            !shortcutsHintFrequency.equals("periodic")) {
            throw new IllegalStateException(
                String.format("Invalid shortcuts_hint_frequency: '%s'. Must be one of: always, first_time, periodic", 
                    shortcutsHintFrequency)
            );
        }
        
        // 验证快捷提示间隔
        if (shortcutsHintInterval < 1) {
            throw new IllegalStateException(
                String.format("shortcuts_hint_interval must be at least 1, got: %d", shortcutsHintInterval)
            );
        }
        
        // 验证参数截断长度
        if (toolArgsTruncateLength < 10) {
            throw new IllegalStateException(
                String.format("tool_args_truncate_length must be at least 10, got: %d", toolArgsTruncateLength)
            );
        }
        
        // 验证动画间隔
        if (spinnerIntervalMs < 50 || spinnerIntervalMs > 1000) {
            throw new IllegalStateException(
                String.format("spinner_interval_ms must be between 50 and 1000, got: %d", spinnerIntervalMs)
            );
        }
        
        // 验证主题配置
        if (theme != null) {
            theme.validate();
        }
    }
}
