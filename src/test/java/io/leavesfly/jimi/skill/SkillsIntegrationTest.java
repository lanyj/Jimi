package io.leavesfly.jimi.skill;

import io.leavesfly.jimi.engine.context.Context;
import io.leavesfly.jimi.llm.message.ContentPart;
import io.leavesfly.jimi.llm.message.Message;
import io.leavesfly.jimi.llm.message.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skills 功能集成测试
 * 
 * 验证从匹配到注入的完整流程
 */
@SpringBootTest
class SkillsIntegrationTest {
    
    @Autowired
    private SkillRegistry skillRegistry;
    
    @Autowired
    private SkillMatcher skillMatcher;
    
    @Autowired
    private SkillProvider skillProvider;
    
    private Context testContext;
    private Path tempHistoryFile;
    
    @BeforeEach
    void setUp() throws Exception {
        // 创建临时历史文件
        tempHistoryFile = Files.createTempFile("test-history", ".jsonl");
        testContext = new Context(tempHistoryFile, new com.fasterxml.jackson.databind.ObjectMapper());
    }
    
    @Test
    void testSkillRegistryInitialization() {
        // 验证SkillRegistry已正确初始化并加载了Skills
        assertNotNull(skillRegistry);
        
        // 验证内置Skills已加载
        assertTrue(skillRegistry.hasSkill("code-review"), "应该包含code-review技能");
        assertTrue(skillRegistry.hasSkill("unit-testing"), "应该包含unit-testing技能");
        
        // 验证统计信息
        var stats = skillRegistry.getStatistics();
        assertTrue((Long) stats.get("totalSkills") >= 2, "至少应该有2个技能");
    }
    
    @Test
    void testEndToEndSkillMatching() {
        // 测试完整的匹配流程
        List<ContentPart> userInput = List.of(
                TextPart.of("请帮我做代码审查，检查代码质量")
        );
        
        // 匹配Skills
        List<SkillSpec> matchedSkills = skillMatcher.matchFromInput(userInput);
        
        // 验证匹配结果
        assertFalse(matchedSkills.isEmpty(), "应该匹配到Skills");
        assertTrue(
                matchedSkills.stream().anyMatch(s -> s.getName().equals("code-review")),
                "应该匹配到code-review技能"
        );
    }
    
    @Test
    void testEndToEndSkillInjection() {
        // 测试完整的注入流程
        List<ContentPart> userInput = List.of(
                TextPart.of("帮我编写单元测试")
        );
        
        // 1. 匹配Skills
        List<SkillSpec> matchedSkills = skillMatcher.matchFromInput(userInput);
        assertFalse(matchedSkills.isEmpty(), "应该匹配到Skills");
        
        // 2. 注入Skills到Context
        StepVerifier.create(skillProvider.injectSkills(testContext, matchedSkills))
                .verifyComplete();
        
        // 3. 验证Context中的激活Skills
        List<SkillSpec> activeSkills = testContext.getActiveSkills();
        assertFalse(activeSkills.isEmpty(), "Context应该包含激活的Skills");
        
        // 4. 验证消息已添加到历史
        List<Message> history = testContext.getHistory();
        assertFalse(history.isEmpty(), "历史记录应该包含Skill消息");
        
        // 5. 验证消息内容
        Message lastMessage = history.get(history.size() - 1);
        String content = lastMessage.getTextContent();
        assertTrue(content.contains("<system>"), "应该包含系统标记");
        assertTrue(content.contains("激活的技能包"), "应该包含标题");
        assertTrue(content.contains("</system>"), "应该包含结束标记");
    }
    
    @Test
    void testSkillDeduplication() {
        // 测试Skills去重功能
        List<ContentPart> userInput = List.of(
                TextPart.of("请做代码审查")
        );
        
        // 第一次注入
        List<SkillSpec> matchedSkills1 = skillMatcher.matchFromInput(userInput);
        skillProvider.injectSkills(testContext, matchedSkills1).block();
        
        int historySize1 = testContext.getHistory().size();
        int activeSize1 = testContext.getActiveSkills().size();
        
        // 第二次注入相同的Skills
        List<SkillSpec> matchedSkills2 = skillMatcher.matchFromInput(userInput);
        skillProvider.injectSkills(testContext, matchedSkills2).block();
        
        int historySize2 = testContext.getHistory().size();
        int activeSize2 = testContext.getActiveSkills().size();
        
        // 验证：第二次应该没有注入新内容
        assertEquals(historySize1, historySize2, "历史记录不应该增加");
        assertEquals(activeSize1, activeSize2, "激活的Skills数量不应该增加");
    }
    
    @Test
    void testMultipleSkillsActivation() {
        // 测试激活多个Skills
        List<SkillSpec> skill1 = skillRegistry.findByCategory("development");
        List<SkillSpec> skill2 = skillRegistry.findByCategory("testing");
        
        // 合并所有匹配的Skills
        var allSkills = new java.util.ArrayList<SkillSpec>();
        allSkills.addAll(skill1);
        allSkills.addAll(skill2);
        
        if (!allSkills.isEmpty()) {
            // 注入多个Skills
            skillProvider.injectSkills(testContext, allSkills).block();
            
            // 验证所有Skills都已激活
            List<SkillSpec> activeSkills = testContext.getActiveSkills();
            assertTrue(activeSkills.size() <= allSkills.size());
            
            // 验证消息格式
            Message lastMessage = testContext.getHistory().get(testContext.getHistory().size() - 1);
            String content = lastMessage.getTextContent();
            
            // 应该包含分隔线（当有多个Skills时）
            if (activeSkills.size() > 1) {
                assertTrue(content.contains("---"), "多个Skills之间应该有分隔线");
            }
        }
    }
    
    @Test
    void testClearActiveSkills() {
        // 测试清除激活的Skills
        List<ContentPart> userInput = List.of(
                TextPart.of("代码审查")
        );
        
        // 先激活一些Skills
        List<SkillSpec> matchedSkills = skillMatcher.matchFromInput(userInput);
        skillProvider.injectSkills(testContext, matchedSkills).block();
        
        assertFalse(testContext.getActiveSkills().isEmpty(), "应该有激活的Skills");
        
        // 清除Skills
        skillProvider.clearSkills(testContext).block();
        
        // 验证已清除
        assertTrue(testContext.getActiveSkills().isEmpty(), "激活的Skills应该已清除");
        
        // 注意：历史记录不会被清除
        assertFalse(testContext.getHistory().isEmpty(), "历史记录应该保留");
    }
}
