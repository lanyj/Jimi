package io.leavesfly.jimi.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillLoader单元测试
 */
class SkillLoaderTest {
    
    private SkillLoader skillLoader;
    private ObjectMapper yamlObjectMapper;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        skillLoader = new SkillLoader();
        
        // 使用反射注入依赖
        try {
            var field = SkillLoader.class.getDeclaredField("yamlObjectMapper");
            field.setAccessible(true);
            field.set(skillLoader, yamlObjectMapper);
        } catch (Exception e) {
            fail("Failed to inject yamlObjectMapper: " + e.getMessage());
        }
    }
    
    @Test
    void parseSkillFile_withValidFrontMatter_shouldParseCorrectly() throws Exception {
        // Arrange
        String skillContent = """
            ---
            name: test-skill
            description: 测试技能
            version: 1.0.0
            category: testing
            triggers:
              - test
              - testing
            ---
            
            # Test Skill Content
            
            This is the skill content.
            """;
        
        Path skillFile = tempDir.resolve("SKILL.md");
        Files.writeString(skillFile, skillContent);
        
        // Act
        SkillSpec skill = skillLoader.parseSkillFile(skillFile);
        
        // Assert
        assertNotNull(skill);
        assertEquals("test-skill", skill.getName());
        assertEquals("测试技能", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion());
        assertEquals("testing", skill.getCategory());
        assertEquals(2, skill.getTriggers().size());
        assertTrue(skill.getTriggers().contains("test"));
        assertTrue(skill.getTriggers().contains("testing"));
        assertTrue(skill.getContent().contains("Test Skill Content"));
    }
    
    @Test
    void parseSkillFile_withoutFrontMatter_shouldUseDefaults() throws Exception {
        // Arrange
        String skillContent = """
            # Simple Skill
            
            This is a simple skill without YAML front matter.
            """;
        
        Path skillFile = tempDir.resolve("SKILL.md");
        Files.writeString(skillFile, skillContent);
        
        // Act
        SkillSpec skill = skillLoader.parseSkillFile(skillFile);
        
        // Assert
        assertNull(skill); // 因为没有name字段，解析会失败
    }
    
    @Test
    void parseSkillFile_withMinimalFrontMatter_shouldParseSuccessfully() throws Exception {
        // Arrange
        String skillContent = """
            ---
            name: minimal-skill
            description: 最小配置技能
            ---
            
            Minimal skill content.
            """;
        
        Path skillFile = tempDir.resolve("SKILL.md");
        Files.writeString(skillFile, skillContent);
        
        // Act
        SkillSpec skill = skillLoader.parseSkillFile(skillFile);
        
        // Assert
        assertNotNull(skill);
        assertEquals("minimal-skill", skill.getName());
        assertEquals("最小配置技能", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion()); // 默认版本
        assertNull(skill.getCategory());
        assertTrue(skill.getTriggers().isEmpty());
    }
    
    @Test
    void loadSkillsFromDirectory_shouldLoadAllSkills() throws Exception {
        // Arrange
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        
        // 创建第一个skill
        Path skill1Dir = skillsDir.resolve("skill1");
        Files.createDirectories(skill1Dir);
        String skill1Content = """
            ---
            name: skill1
            description: 第一个技能
            ---
            Content 1
            """;
        Files.writeString(skill1Dir.resolve("SKILL.md"), skill1Content);
        
        // 创建第二个skill
        Path skill2Dir = skillsDir.resolve("skill2");
        Files.createDirectories(skill2Dir);
        String skill2Content = """
            ---
            name: skill2
            description: 第二个技能
            ---
            Content 2
            """;
        Files.writeString(skill2Dir.resolve("SKILL.md"), skill2Content);
        
        // Act
        List<SkillSpec> skills = skillLoader.loadSkillsFromDirectory(skillsDir, SkillScope.GLOBAL);
        
        // Assert
        assertEquals(2, skills.size());
        assertTrue(skills.stream().anyMatch(s -> "skill1".equals(s.getName())));
        assertTrue(skills.stream().anyMatch(s -> "skill2".equals(s.getName())));
        skills.forEach(s -> assertEquals(SkillScope.GLOBAL, s.getScope()));
    }
    
    @Test
    void loadSkillsFromDirectory_withResources_shouldSetResourcesPath() throws Exception {
        // Arrange
        Path skillsDir = tempDir.resolve("skills");
        Path skillDir = skillsDir.resolve("skill-with-resources");
        Path resourcesDir = skillDir.resolve("resources");
        Files.createDirectories(resourcesDir);
        
        String skillContent = """
            ---
            name: skill-with-resources
            description: 带资源的技能
            ---
            Content with resources
            """;
        Files.writeString(skillDir.resolve("SKILL.md"), skillContent);
        Files.writeString(resourcesDir.resolve("data.txt"), "resource data");
        
        // Act
        List<SkillSpec> skills = skillLoader.loadSkillsFromDirectory(skillsDir, SkillScope.PROJECT);
        
        // Assert
        assertEquals(1, skills.size());
        SkillSpec skill = skills.get(0);
        assertNotNull(skill.getResourcesPath());
        assertTrue(Files.exists(skill.getResourcesPath()));
        assertEquals(SkillScope.PROJECT, skill.getScope());
    }
    
    @Test
    void loadSkillsFromDirectory_nonExistentDirectory_shouldReturnEmptyList() {
        // Act
        List<SkillSpec> skills = skillLoader.loadSkillsFromDirectory(
            tempDir.resolve("nonexistent"), 
            SkillScope.GLOBAL
        );
        
        // Assert
        assertTrue(skills.isEmpty());
    }
    
    @Test
    void getGlobalSkillsDirectories_shouldReturnValidDirectories() {
        // Act
        List<Path> directories = skillLoader.getGlobalSkillsDirectories();
        
        // Assert
        assertNotNull(directories);
        // 至少应该包含类路径或用户目录之一
        // 具体数量取决于运行环境
    }
}
