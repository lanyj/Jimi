package io.leavesfly.jimi.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill集成测试
 * 验证从类路径加载内置Skills的功能
 */
class SkillIntegrationTest {
    
    @Test
    void loadBuiltinSkills_shouldLoadFromClasspath() {
        // Arrange
        ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        SkillLoader loader = new SkillLoader();
        
        // 注入依赖
        try {
            var field = SkillLoader.class.getDeclaredField("yamlObjectMapper");
            field.setAccessible(true);
            field.set(loader, yamlObjectMapper);
        } catch (Exception e) {
            fail("Failed to inject dependency: " + e.getMessage());
        }
        
        // Act
        List<Path> directories = loader.getGlobalSkillsDirectories();
        
        // Assert
        assertNotNull(directories);
        assertFalse(directories.isEmpty(), "Should find at least one skills directory");
        
        // 从找到的目录加载Skills
        int totalSkills = 0;
        for (Path dir : directories) {
            List<SkillSpec> skills = loader.loadSkillsFromDirectory(dir, SkillScope.GLOBAL);
            totalSkills += skills.size();
            
            // 验证加载的Skills
            for (SkillSpec skill : skills) {
                assertNotNull(skill.getName(), "Skill name should not be null");
                assertNotNull(skill.getDescription(), "Skill description should not be null");
                assertNotNull(skill.getContent(), "Skill content should not be null");
                assertEquals(SkillScope.GLOBAL, skill.getScope());
                
                System.out.println("Loaded skill: " + skill.getName() + 
                    " (category: " + skill.getCategory() + 
                    ", triggers: " + skill.getTriggers().size() + ")");
            }
        }
        
        // 应该至少加载到我们创建的两个内置Skills
        assertTrue(totalSkills >= 2, 
            "Should load at least 2 builtin skills (code-review, unit-testing)");
    }
    
    @Test
    void skillRegistry_shouldAutoLoadSkills() {
        // Arrange
        ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        SkillLoader loader = new SkillLoader();
        SkillRegistry registry = new SkillRegistry();
        
        // 注入依赖
        try {
            var loaderField = SkillLoader.class.getDeclaredField("yamlObjectMapper");
            loaderField.setAccessible(true);
            loaderField.set(loader, yamlObjectMapper);
            
            var registryField = SkillRegistry.class.getDeclaredField("skillLoader");
            registryField.setAccessible(true);
            registryField.set(registry, loader);
        } catch (Exception e) {
            fail("Failed to inject dependencies: " + e.getMessage());
        }
        
        // Act
        registry.initialize();
        
        // Assert
        List<SkillSpec> allSkills = registry.getAllSkills();
        assertFalse(allSkills.isEmpty(), "Registry should contain skills after initialization");
        
        // 验证内置Skills
        assertTrue(registry.hasSkill("code-review"), "Should have code-review skill");
        assertTrue(registry.hasSkill("unit-testing"), "Should have unit-testing skill");
        
        // 验证code-review skill的内容
        registry.findByName("code-review").ifPresent(skill -> {
            assertEquals("code-review", skill.getName());
            assertEquals("development", skill.getCategory());
            assertTrue(skill.getTriggers().contains("code review"));
            assertTrue(skill.getTriggers().contains("代码审查"));
            assertTrue(skill.getContent().contains("代码审查技能包"));
        });
        
        // 验证unit-testing skill的内容
        registry.findByName("unit-testing").ifPresent(skill -> {
            assertEquals("unit-testing", skill.getName());
            assertEquals("testing", skill.getCategory());
            assertTrue(skill.getTriggers().contains("unit test"));
            assertTrue(skill.getTriggers().contains("单元测试"));
            assertTrue(skill.getContent().contains("单元测试技能包"));
        });
        
        // 打印统计信息
        System.out.println("Registry statistics: " + registry.getStatistics());
    }
    
    @Test
    void findByTriggers_shouldMatchBuiltinSkills() {
        // Arrange
        ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        SkillLoader loader = new SkillLoader();
        SkillRegistry registry = new SkillRegistry();
        
        try {
            var loaderField = SkillLoader.class.getDeclaredField("yamlObjectMapper");
            loaderField.setAccessible(true);
            loaderField.set(loader, yamlObjectMapper);
            
            var registryField = SkillRegistry.class.getDeclaredField("skillLoader");
            registryField.setAccessible(true);
            registryField.set(registry, loader);
        } catch (Exception e) {
            fail("Failed to inject dependencies: " + e.getMessage());
        }
        
        registry.initialize();
        
        // Act & Assert
        // 测试触发code-review
        List<SkillSpec> codeReviewMatches = registry.findByTriggers(
            java.util.Set.of("code", "review")
        );
        assertTrue(codeReviewMatches.stream()
            .anyMatch(s -> "code-review".equals(s.getName())),
            "Should match code-review skill with 'code review' keywords");
        
        // 测试触发unit-testing
        List<SkillSpec> testMatches = registry.findByTriggers(
            java.util.Set.of("junit", "test")
        );
        assertTrue(testMatches.stream()
            .anyMatch(s -> "unit-testing".equals(s.getName())),
            "Should match unit-testing skill with 'junit test' keywords");
    }
}
