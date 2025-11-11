package io.leavesfly.jimi.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skills功能演示程序
 * 
 * 展示如何使用SkillLoader和SkillRegistry
 */
public class SkillDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Jimi Skills 功能演示 ===\n");
        
        // 初始化组件
        ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        SkillLoader loader = new SkillLoader();
        SkillRegistry registry = new SkillRegistry();
        
        // 注入依赖（实际使用中由Spring自动完成）
        try {
            var loaderField = SkillLoader.class.getDeclaredField("yamlObjectMapper");
            loaderField.setAccessible(true);
            loaderField.set(loader, yamlObjectMapper);
            
            var registryField = SkillRegistry.class.getDeclaredField("skillLoader");
            registryField.setAccessible(true);
            registryField.set(registry, loader);
        } catch (Exception e) {
            System.err.println("初始化失败: " + e.getMessage());
            return;
        }
        
        // 初始化注册表（自动加载全局Skills）
        System.out.println("1. 初始化SkillRegistry...");
        registry.initialize();
        System.out.println();
        
        // 显示加载的Skills
        System.out.println("2. 已加载的Skills:");
        List<SkillSpec> allSkills = registry.getAllSkills();
        for (SkillSpec skill : allSkills) {
            System.out.printf("   - %s (v%s) - %s%n", 
                skill.getName(), 
                skill.getVersion(), 
                skill.getDescription());
            System.out.printf("     分类: %s, 作用域: %s%n", 
                skill.getCategory(), 
                skill.getScope());
            System.out.printf("     触发词: %s%n", skill.getTriggers());
            System.out.println();
        }
        
        // 按名称查找
        System.out.println("3. 按名称查找 'code-review':");
        registry.findByName("code-review").ifPresent(skill -> {
            System.out.println("   找到Skill: " + skill.getName());
            System.out.println("   内容预览: " + 
                skill.getContent().substring(0, Math.min(100, skill.getContent().length())) + "...");
        });
        System.out.println();
        
        // 按分类查找
        System.out.println("4. 按分类查找 'development':");
        List<SkillSpec> devSkills = registry.findByCategory("development");
        System.out.println("   找到 " + devSkills.size() + " 个Skills:");
        devSkills.forEach(s -> System.out.println("   - " + s.getName()));
        System.out.println();
        
        // 按触发词查找
        System.out.println("5. 按触发词查找 'test', 'junit':");
        Set<String> keywords = Set.of("test", "junit");
        List<SkillSpec> matched = registry.findByTriggers(keywords);
        System.out.println("   匹配到 " + matched.size() + " 个Skills:");
        matched.forEach(s -> System.out.println("   - " + s.getName()));
        System.out.println();
        
        // 显示统计信息
        System.out.println("6. 统计信息:");
        Map<String, Object> stats = registry.getStatistics();
        stats.forEach((key, value) -> 
            System.out.printf("   %s: %s%n", key, value));
        System.out.println();
        
        // 显示所有分类
        System.out.println("7. 所有分类:");
        registry.getAllCategories().forEach(cat -> 
            System.out.println("   - " + cat));
        System.out.println();
        
        System.out.println("=== 演示完成 ===");
    }
}
