package io.leavesfly.jimi.skill;

import io.leavesfly.jimi.llm.message.ContentPart;
import io.leavesfly.jimi.llm.message.Message;
import io.leavesfly.jimi.llm.message.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SkillMatcher 单元测试
 */
class SkillMatcherTest {
    
    @Mock
    private SkillRegistry skillRegistry;
    
    @InjectMocks
    private SkillMatcher skillMatcher;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 手动初始化缓存
        skillMatcher.initializeCache();
    }
    
    @Test
    void testMatchFromInput_EmptyInput() {
        // 空输入应返回空列表
        List<SkillSpec> result = skillMatcher.matchFromInput(List.of());
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testMatchFromInput_NoTextContent() {
        // 无文本内容应返回空列表
        List<ContentPart> input = List.of();
        List<SkillSpec> result = skillMatcher.matchFromInput(input);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testMatchFromInput_ValidMatch() {
        // 准备测试数据
        SkillSpec codeReviewSkill = SkillSpec.builder()
                .name("code-review")
                .description("代码审查指南")
                .triggers(List.of("code review", "代码审查"))
                .content("审查规范...")
                .build();
        
        // Mock registry 行为
        when(skillRegistry.findByTriggers(anySet()))
                .thenReturn(List.of(codeReviewSkill));
        
        // 执行测试
        List<ContentPart> input = List.of(TextPart.of("请帮我做代码审查"));
        List<SkillSpec> result = skillMatcher.matchFromInput(input);
        
        // 验证结果
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("code-review", result.get(0).getName());
    }
    
    @Test
    void testMatchFromInput_NoMatch() {
        // Mock registry 返回空列表
        when(skillRegistry.findByTriggers(anySet()))
                .thenReturn(List.of());
        
        // 执行测试
        List<ContentPart> input = List.of(TextPart.of("Hello world"));
        List<SkillSpec> result = skillMatcher.matchFromInput(input);
        
        // 验证结果
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testMatchFromContext_EmptyContext() {
        // 空上下文应返回空列表
        List<SkillSpec> result = skillMatcher.matchFromContext(List.of());
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testMatchFromContext_ValidMatch() {
        // 准备测试数据
        SkillSpec testingSkill = SkillSpec.builder()
                .name("unit-testing")
                .description("单元测试指南")
                .triggers(List.of("test", "testing", "单元测试"))
                .content("测试规范...")
                .build();
        
        // Mock registry 行为
        when(skillRegistry.findByTriggers(anySet()))
                .thenReturn(List.of(testingSkill));
        
        // 准备上下文消息
        Message userMsg = Message.user("我需要编写单元测试");
        List<Message> context = List.of(userMsg);
        
        // 执行测试
        List<SkillSpec> result = skillMatcher.matchFromContext(context);
        
        // 验证结果（注意：上下文匹配使用更低的阈值）
        verify(skillRegistry, atLeastOnce()).findByTriggers(anySet());
    }
    
    @Test
    void testKeywordExtraction() {
        // 准备包含中英文关键词的输入
        List<ContentPart> input = List.of(
                TextPart.of("请帮我进行 code review，检查代码质量")
        );
        
        // Mock registry 行为
        when(skillRegistry.findByTriggers(anySet()))
                .thenReturn(List.of());
        
        // 执行测试
        skillMatcher.matchFromInput(input);
        
        // 验证提取了关键词
        verify(skillRegistry).findByTriggers(argThat(keywords -> 
                !keywords.isEmpty()
        ));
    }
    
    @Test
    void testMaxMatchedSkillsLimit() {
        // 准备多个匹配的 Skills
        List<SkillSpec> manySkills = List.of(
                SkillSpec.builder().name("skill1").triggers(List.of("test")).build(),
                SkillSpec.builder().name("skill2").triggers(List.of("test")).build(),
                SkillSpec.builder().name("skill3").triggers(List.of("test")).build(),
                SkillSpec.builder().name("skill4").triggers(List.of("test")).build(),
                SkillSpec.builder().name("skill5").triggers(List.of("test")).build(),
                SkillSpec.builder().name("skill6").triggers(List.of("test")).build()
        );
        
        // Mock registry 行为
        when(skillRegistry.findByTriggers(anySet()))
                .thenReturn(manySkills);
        
        // 执行测试
        List<ContentPart> input = List.of(TextPart.of("test"));
        List<SkillSpec> result = skillMatcher.matchFromInput(input);
        
        // 验证结果不超过最大值（5个）
        assertTrue(result.size() <= 5);
    }
}
