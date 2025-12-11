package io.leavesfly.jimi.command.impl;

import io.leavesfly.jimi.command.CommandContext;
import io.leavesfly.jimi.command.CommandHandler;
import io.leavesfly.jimi.config.ThemeConfig;
import org.springframework.stereotype.Component;

/**
 * 主题切换命令
 * 支持切换预设主题：default, dark, light, minimal, matrix
 */
@Component
public class ThemeCommand implements CommandHandler {

    @Override
    public String getName() {
        return "theme";
    }

    @Override
    public String getDescription() {
        return "切换UI主题";
    }

    @Override
    public String getUsage() {
        return "/theme [name]  - 切换主题 (default, dark, light, minimal, matrix)";
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        // 如果没有参数，显示当前主题和可用主题
        if (args.length == 0) {
            String currentTheme = context.getSoul().getRuntime().getConfig().getShellUI().getThemeName();
            context.getOutputFormatter().printInfo(
                "当前主题: " + currentTheme + "\n" +
                "可用主题:\n" +
                "  - default: 默认主题 (绿色提示符)\n" +
                "  - dark: 暗色主题 (青色/品红)\n" +
                "  - light: 亮色主题 (蓝色系)\n" +
                "  - minimal: 极简主题 (黑白)\n" +
                "  - matrix: 终端绿主题 (黑客风格)\n" +
                "\n用法: /theme <name>"
            );
            return;
        }
        
        // 获取主题名称
        String themeName = args[0].toLowerCase();
        
        // 验证主题名称
        if (!isValidTheme(themeName)) {
            context.getOutputFormatter().printError(
                "无效的主题名称: " + themeName + "\n" +
                "可用主题: default, dark, light, minimal, matrix"
            );
            return;
        }
        
        // 获取新主题
        ThemeConfig newTheme = ThemeConfig.getPresetTheme(themeName);
        
        // 更新配置（直接修改配置对象）
        context.getSoul().getRuntime().getConfig().getShellUI().setThemeName(themeName);
        context.getSoul().getRuntime().getConfig().getShellUI().setTheme(newTheme);
        
        // 更新OutputFormatter的主题（立即生效）
        context.getOutputFormatter().setTheme(newTheme);
        
        context.getOutputFormatter().printSuccess("🎨 主题已切换到: " + themeName + " （部分样式将在下次输入时生效）");
    }
    
    private boolean isValidTheme(String themeName) {
        return themeName.equals("default") ||
               themeName.equals("dark") ||
               themeName.equals("light") ||
               themeName.equals("minimal") ||
               themeName.equals("matrix");
    }
}
