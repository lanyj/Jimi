package io.leavesfly.jimi.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shell UI 主题配置
 * 定义各种UI元素的颜色方案
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeConfig {
    
    /**
     * 主题名称
     */
    @JsonProperty("name")
    @Builder.Default
    private String name = "default";
    
    /**
     * 主提示符颜色（就绪状态）
     */
    @JsonProperty("prompt_color")
    @Builder.Default
    private String promptColor = "green";
    
    /**
     * 思考状态颜色
     */
    @JsonProperty("thinking_color")
    @Builder.Default
    private String thinkingColor = "yellow";
    
    /**
     * 错误状态颜色
     */
    @JsonProperty("error_color")
    @Builder.Default
    private String errorColor = "red";
    
    /**
     * 成功消息颜色
     */
    @JsonProperty("success_color")
    @Builder.Default
    private String successColor = "green";
    
    /**
     * 状态消息颜色
     */
    @JsonProperty("status_color")
    @Builder.Default
    private String statusColor = "yellow";
    
    /**
     * 信息消息颜色
     */
    @JsonProperty("info_color")
    @Builder.Default
    private String infoColor = "blue";
    
    /**
     * 助手输出颜色
     */
    @JsonProperty("assistant_color")
    @Builder.Default
    private String assistantColor = "yellow";
    
    /**
     * 推理内容颜色
     */
    @JsonProperty("reasoning_color")
    @Builder.Default
    private String reasoningColor = "white";
    
    /**
     * Token统计颜色
     */
    @JsonProperty("token_color")
    @Builder.Default
    private String tokenColor = "cyan";
    
    /**
     * 快捷提示颜色
     */
    @JsonProperty("hint_color")
    @Builder.Default
    private String hintColor = "blue";
    
    /**
     * Banner颜色
     */
    @JsonProperty("banner_color")
    @Builder.Default
    private String bannerColor = "cyan";
    
    /**
     * 是否使用粗体提示符
     */
    @JsonProperty("bold_prompt")
    @Builder.Default
    private boolean boldPrompt = true;
    
    /**
     * 是否使用斜体推理
     */
    @JsonProperty("italic_reasoning")
    @Builder.Default
    private boolean italicReasoning = true;
    
    /**
     * 预设主题：默认
     */
    public static ThemeConfig defaultTheme() {
        return ThemeConfig.builder()
                .name("default")
                .promptColor("green")
                .thinkingColor("yellow")
                .errorColor("red")
                .successColor("green")
                .statusColor("yellow")
                .infoColor("blue")
                .assistantColor("yellow")
                .reasoningColor("white")
                .tokenColor("cyan")
                .hintColor("blue")
                .bannerColor("cyan")
                .boldPrompt(true)
                .italicReasoning(true)
                .build();
    }
    
    /**
     * 预设主题：暗色
     */
    public static ThemeConfig darkTheme() {
        return ThemeConfig.builder()
                .name("dark")
                .promptColor("cyan")
                .thinkingColor("magenta")
                .errorColor("red")
                .successColor("green")
                .statusColor("cyan")
                .infoColor("blue")
                .assistantColor("white")
                .reasoningColor("bright_black")
                .tokenColor("magenta")
                .hintColor("cyan")
                .bannerColor("magenta")
                .boldPrompt(true)
                .italicReasoning(true)
                .build();
    }
    
    /**
     * 预设主题：亮色
     */
    public static ThemeConfig lightTheme() {
        return ThemeConfig.builder()
                .name("light")
                .promptColor("blue")
                .thinkingColor("magenta")
                .errorColor("red")
                .successColor("green")
                .statusColor("blue")
                .infoColor("cyan")
                .assistantColor("black")
                .reasoningColor("bright_black")
                .tokenColor("blue")
                .hintColor("magenta")
                .bannerColor("blue")
                .boldPrompt(false)
                .italicReasoning(false)
                .build();
    }
    
    /**
     * 预设主题：极简
     */
    public static ThemeConfig minimalTheme() {
        return ThemeConfig.builder()
                .name("minimal")
                .promptColor("white")
                .thinkingColor("white")
                .errorColor("red")
                .successColor("white")
                .statusColor("white")
                .infoColor("white")
                .assistantColor("white")
                .reasoningColor("bright_black")
                .tokenColor("bright_black")
                .hintColor("bright_black")
                .bannerColor("white")
                .boldPrompt(false)
                .italicReasoning(true)
                .build();
    }
    
    /**
     * 预设主题：终端绿
     */
    public static ThemeConfig matrixTheme() {
        return ThemeConfig.builder()
                .name("matrix")
                .promptColor("green")
                .thinkingColor("green")
                .errorColor("red")
                .successColor("bright_green")
                .statusColor("green")
                .infoColor("green")
                .assistantColor("bright_green")
                .reasoningColor("green")
                .tokenColor("bright_green")
                .hintColor("green")
                .bannerColor("bright_green")
                .boldPrompt(true)
                .italicReasoning(false)
                .build();
    }
    
    /**
     * 根据主题名称获取预设主题
     */
    public static ThemeConfig getPresetTheme(String themeName) {
        if (themeName == null) {
            return defaultTheme();
        }
        
        return switch (themeName.toLowerCase()) {
            case "dark" -> darkTheme();
            case "light" -> lightTheme();
            case "minimal" -> minimalTheme();
            case "matrix" -> matrixTheme();
            default -> defaultTheme();
        };
    }
    
    /**
     * 验证颜色值
     */
    public void validate() {
        String[] validColors = {
            "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
            "bright_black", "bright_red", "bright_green", "bright_yellow",
            "bright_blue", "bright_magenta", "bright_cyan", "bright_white"
        };
        
        validateColor(promptColor, "prompt_color", validColors);
        validateColor(thinkingColor, "thinking_color", validColors);
        validateColor(errorColor, "error_color", validColors);
        validateColor(successColor, "success_color", validColors);
        validateColor(statusColor, "status_color", validColors);
        validateColor(infoColor, "info_color", validColors);
        validateColor(assistantColor, "assistant_color", validColors);
        validateColor(reasoningColor, "reasoning_color", validColors);
        validateColor(tokenColor, "token_color", validColors);
        validateColor(hintColor, "hint_color", validColors);
        validateColor(bannerColor, "banner_color", validColors);
    }
    
    private void validateColor(String color, String fieldName, String[] validColors) {
        if (color == null || color.isEmpty()) {
            throw new IllegalStateException(
                String.format("%s cannot be null or empty", fieldName)
            );
        }
        
        for (String validColor : validColors) {
            if (validColor.equals(color)) {
                return;
            }
        }
        
        throw new IllegalStateException(
            String.format("Invalid color '%s' for %s. Must be one of: %s", 
                color, fieldName, String.join(", ", validColors))
        );
    }
}
