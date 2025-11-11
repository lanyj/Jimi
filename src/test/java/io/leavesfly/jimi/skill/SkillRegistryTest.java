package io.leavesfly.jimi.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SkillRegistry单元测试
 */
class SkillRegistryTest {
    
    @Mock
    private SkillLoader skillLoader;
    
    private SkillRegistry skillRegistry;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        skillRegistry = new SkillRegistry();
        
        // 注入Mock的SkillLoader
        try {
            var field = SkillRegistry.class.getDeclaredField("skillLoader");
            field.setAccessible(true);
            field.set(skillRegistry, skillLoader);
        } catch (Exception e) {
            fail("Failed to inject skillLoader: " + e.getMessage());
        }
    }
    
    @Test
    void register_newSkill_shouldRegisterSuccessfully() {
        // Arrange
        SkillSpec skill = SkillSpec.builder()
            .name("test-skill")
            .description("测试技能")
            .version("1.0.0")
            .category("testing")
            .triggers(Arrays.asList("test", "testing"))
            .content("Test content")
            .scope(SkillScope.GLOBAL)
            .build();
        
        // Act
        skillRegistry.register(skill);
        
        // Assert
        assertTrue(skillRegistry.hasSkill("test-skill"));
        Optional<SkillSpec> found = skillRegistry.findByName("test-skill");
        assertTrue(found.isPresent());
        assertEquals("test-skill", found.get().getName());
    }
    
    @Test
    void register_duplicateName_shouldOverride() {
        // Arrange
        SkillSpec skill1 = SkillSpec.builder()
            .name("skill")
            .description("第一版")
            .scope(SkillScope.GLOBAL)
            .build();
        
        SkillSpec skill2 = SkillSpec.builder()
            .name("skill")
            .description("第二版")
            .scope(SkillScope.PROJECT)
            .build();
        
        // Act
        skillRegistry.register(skill1);
        skillRegistry.register(skill2);
        
        // Assert
        Optional<SkillSpec> found = skillRegistry.findByName("skill");
        assertTrue(found.isPresent());
        assertEquals("第二版", found.get().getDescription());
        assertEquals(SkillScope.PROJECT, found.get().getScope());
    }
    
    @Test
    void findByCategory_shouldReturnSkillsInCategory() {
        // Arrange
        SkillSpec skill1 = SkillSpec.builder()
            .name("skill1")
            .description("技能1")
            .category("development")
            .scope(SkillScope.GLOBAL)
            .build();
        
        SkillSpec skill2 = SkillSpec.builder()
            .name("skill2")
            .description("技能2")
            .category("development")
            .scope(SkillScope.GLOBAL)
            .build();
        
        SkillSpec skill3 = SkillSpec.builder()
            .name("skill3")
            .description("技能3")
            .category("testing")
            .scope(SkillScope.GLOBAL)
            .build();
        
        skillRegistry.register(skill1);
        skillRegistry.register(skill2);
        skillRegistry.register(skill3);
        
        // Act
        List<SkillSpec> devSkills = skillRegistry.findByCategory("development");
        List<SkillSpec> testSkills = skillRegistry.findByCategory("testing");
        
        // Assert
        assertEquals(2, devSkills.size());
        assertEquals(1, testSkills.size());
        assertTrue(devSkills.stream().allMatch(s -> "development".equals(s.getCategory())));
    }
    
    @Test
    void findByTriggers_shouldReturnMatchingSkills() {
        // Arrange
        SkillSpec skill1 = SkillSpec.builder()
            .name("skill1")
            .description("技能1")
            .triggers(Arrays.asList("java", "code"))
            .scope(SkillScope.GLOBAL)
            .build();
        
        SkillSpec skill2 = SkillSpec.builder()
            .name("skill2")
            .description("技能2")
            .triggers(Arrays.asList("test", "junit"))
            .scope(SkillScope.GLOBAL)
            .build();
        
        skillRegistry.register(skill1);
        skillRegistry.register(skill2);
        
        // Act
        Set<String> keywords = new HashSet<>(Arrays.asList("java", "junit"));
        List<SkillSpec> matched = skillRegistry.findByTriggers(keywords);
        
        // Assert
        assertEquals(2, matched.size());
        assertTrue(matched.stream().anyMatch(s -> "skill1".equals(s.getName())));
        assertTrue(matched.stream().anyMatch(s -> "skill2".equals(s.getName())));
    }
    
    @Test
    void findByTriggers_partialMatch_shouldReturnMatches() {
        // Arrange
        SkillSpec skill = SkillSpec.builder()
            .name("code-review")
            .description("代码审查")
            .triggers(Arrays.asList("code review", "review code"))
            .scope(SkillScope.GLOBAL)
            .build();
        
        skillRegistry.register(skill);
        
        // Act
        Set<String> keywords = new HashSet<>(Arrays.asList("code"));
        List<SkillSpec> matched = skillRegistry.findByTriggers(keywords);
        
        // Assert
        assertFalse(matched.isEmpty());
        assertTrue(matched.stream().anyMatch(s -> "code-review".equals(s.getName())));
    }
    
    @Test
    void getAllSkills_shouldReturnAllRegisteredSkills() {
        // Arrange
        skillRegistry.register(createSkill("skill1"));
        skillRegistry.register(createSkill("skill2"));
        skillRegistry.register(createSkill("skill3"));
        
        // Act
        List<SkillSpec> allSkills = skillRegistry.getAllSkills();
        
        // Assert
        assertEquals(3, allSkills.size());
    }
    
    @Test
    void getStatistics_shouldReturnCorrectStats() {
        // Arrange
        SkillSpec globalSkill = SkillSpec.builder()
            .name("global")
            .description("全局技能")
            .category("development")
            .triggers(Arrays.asList("global"))
            .scope(SkillScope.GLOBAL)
            .build();
        
        SkillSpec projectSkill = SkillSpec.builder()
            .name("project")
            .description("项目技能")
            .category("testing")
            .triggers(Arrays.asList("project"))
            .scope(SkillScope.PROJECT)
            .build();
        
        skillRegistry.register(globalSkill);
        skillRegistry.register(projectSkill);
        
        // Act
        Map<String, Object> stats = skillRegistry.getStatistics();
        
        // Assert
        assertEquals(2, stats.get("totalSkills"));
        assertEquals(2, stats.get("categories"));
        assertEquals(1L, stats.get("globalSkills"));
        assertEquals(1L, stats.get("projectSkills"));
    }
    
    @Test
    void initialize_shouldLoadGlobalSkills() {
        // Arrange
        when(skillLoader.getGlobalSkillsDirectories())
            .thenReturn(Arrays.asList(Paths.get("/mock/path")));
        
        when(skillLoader.loadSkillsFromDirectory(any(), eq(SkillScope.GLOBAL)))
            .thenReturn(Arrays.asList(
                createSkill("skill1"),
                createSkill("skill2")
            ));
        
        // Act
        skillRegistry.initialize();
        
        // Assert
        assertEquals(2, skillRegistry.getAllSkills().size());
        verify(skillLoader, times(1)).getGlobalSkillsDirectories();
    }
    
    @Test
    void loadProjectSkills_shouldLoadFromProjectDirectory() {
        // Arrange
        when(skillLoader.loadSkillsFromDirectory(any(), eq(SkillScope.PROJECT)))
            .thenReturn(Arrays.asList(createSkill("project-skill")));
        
        // Act
        skillRegistry.loadProjectSkills(Paths.get("/mock/project/.jimi/skills"));
        
        // Assert
        assertTrue(skillRegistry.hasSkill("project-skill"));
        verify(skillLoader, times(1)).loadSkillsFromDirectory(any(), eq(SkillScope.PROJECT));
    }
    
    // 辅助方法
    private SkillSpec createSkill(String name) {
        return SkillSpec.builder()
            .name(name)
            .description("Description for " + name)
            .scope(SkillScope.GLOBAL)
            .build();
    }
}
