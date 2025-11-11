package io.leavesfly.jimi.skill;

import io.leavesfly.jimi.engine.context.Context;
import io.leavesfly.jimi.llm.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SkillProvider 单元测试
 */
class SkillProviderTest {
    
    @Mock
    private SkillRegistry skillRegistry;
    
    @Mock
    private Context context;
    
    @InjectMocks
    private SkillProvider skillProvider;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testInjectSkills_EmptyList() {
        // 空列表应直接返回
        Mono<Void> result = skillProvider.injectSkills(context, List.of());
        
        StepVerifier.create(result)
                .verifyComplete();
        
        // 验证没有调用上下文方法
        verify(context, never()).appendMessage(any());
    }
    
    @Test
    void testInjectSkills_NullList() {
        // null 应直接返回
        Mono<Void> result = skillProvider.injectSkills(context, null);
        
        StepVerifier.create(result)
                .verifyComplete();
        
        verify(context, never()).appendMessage(any());
    }
    
    @Test
    void testInjectSkills_NewSkills() {
        // 准备测试数据
        SkillSpec skill1 = SkillSpec.builder()
                .name("skill1")
                .description("测试技能1")
                .content("内容1")
                .build();
        
        List<SkillSpec> skills = List.of(skill1);
        
        // Mock context 行为
        when(context.getActiveSkills()).thenReturn(List.of());
        when(context.appendMessage(any(Message.class))).thenReturn(Mono.empty());
        when(context.addActiveSkills(anyList())).thenReturn(Mono.empty());
        
        // 执行测试
        Mono<Void> result = skillProvider.injectSkills(context, skills);
        
        StepVerifier.create(result)
                .verifyComplete();
        
        // 验证调用了上下文方法
        verify(context).appendMessage(any(Message.class));
        verify(context).addActiveSkills(skills);
    }
    
    @Test
    void testInjectSkills_SkipAlreadyActive() {
        // 准备测试数据
        SkillSpec activeSkill = SkillSpec.builder()
                .name("active-skill")
                .description("已激活的技能")
                .content("内容")
                .build();
        
        SkillSpec newSkill = SkillSpec.builder()
                .name("new-skill")
                .description("新技能")
                .content("新内容")
                .build();
        
        List<SkillSpec> matchedSkills = List.of(activeSkill, newSkill);
        
        // Mock context：activeSkill 已激活
        when(context.getActiveSkills()).thenReturn(List.of(activeSkill));
        when(context.appendMessage(any(Message.class))).thenReturn(Mono.empty());
        when(context.addActiveSkills(anyList())).thenReturn(Mono.empty());
        
        // 执行测试
        Mono<Void> result = skillProvider.injectSkills(context, matchedSkills);
        
        StepVerifier.create(result)
                .verifyComplete();
        
        // 验证只注入了新 Skill
        ArgumentCaptor<List<SkillSpec>> captor = ArgumentCaptor.forClass(List.class);
        verify(context).addActiveSkills(captor.capture());
        
        List<SkillSpec> injectedSkills = captor.getValue();
        assertEquals(1, injectedSkills.size());
        assertEquals("new-skill", injectedSkills.get(0).getName());
    }
    
    @Test
    void testFormatSkills_SingleSkill() {
        // 准备测试数据
        SkillSpec skill = SkillSpec.builder()
                .name("test-skill")
                .description("测试技能")
                .content("# 技能内容\n\n这是测试内容")
                .build();
        
        List<SkillSpec> skills = List.of(skill);
        
        // Mock context
        when(context.getActiveSkills()).thenReturn(List.of());
        when(context.appendMessage(any(Message.class))).thenReturn(Mono.empty());
        when(context.addActiveSkills(anyList())).thenReturn(Mono.empty());
        
        // 执行测试
        skillProvider.injectSkills(context, skills).block();
        
        // 验证格式化的消息
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).appendMessage(messageCaptor.capture());
        
        Message message = messageCaptor.getValue();
        String content = message.getTextContent();
        
        // 验证包含必要的标记和内容
        assertTrue(content.contains("<system>"));
        assertTrue(content.contains("激活的技能包"));
        assertTrue(content.contains("test-skill"));
        assertTrue(content.contains("测试技能"));
        assertTrue(content.contains("技能内容"));
        assertTrue(content.contains("</system>"));
    }
    
    @Test
    void testFormatSkills_MultipleSkills() {
        // 准备测试数据
        SkillSpec skill1 = SkillSpec.builder()
                .name("skill1")
                .description("技能1")
                .content("内容1")
                .build();
        
        SkillSpec skill2 = SkillSpec.builder()
                .name("skill2")
                .description("技能2")
                .content("内容2")
                .build();
        
        List<SkillSpec> skills = List.of(skill1, skill2);
        
        // Mock context
        when(context.getActiveSkills()).thenReturn(List.of());
        when(context.appendMessage(any(Message.class))).thenReturn(Mono.empty());
        when(context.addActiveSkills(anyList())).thenReturn(Mono.empty());
        
        // 执行测试
        skillProvider.injectSkills(context, skills).block();
        
        // 验证格式化的消息
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).appendMessage(messageCaptor.capture());
        
        Message message = messageCaptor.getValue();
        String content = message.getTextContent();
        
        // 验证包含两个技能
        assertTrue(content.contains("skill1"));
        assertTrue(content.contains("skill2"));
        assertTrue(content.contains("---"));  // 分隔线
    }
    
    @Test
    void testClearSkills() {
        // Mock context
        when(context.clearActiveSkills()).thenReturn(Mono.empty());
        
        // 执行测试
        Mono<Void> result = skillProvider.clearSkills(context);
        
        StepVerifier.create(result)
                .verifyComplete();
        
        // 验证调用了清除方法
        verify(context).clearActiveSkills();
    }
    
    @Test
    void testGetActiveSkills() {
        // 准备测试数据
        SkillSpec skill = SkillSpec.builder()
                .name("active-skill")
                .build();
        
        List<SkillSpec> activeSkills = List.of(skill);
        
        // Mock context
        when(context.getActiveSkills()).thenReturn(activeSkills);
        
        // 执行测试
        List<SkillSpec> result = skillProvider.getActiveSkills(context);
        
        // 验证结果
        assertEquals(1, result.size());
        assertEquals("active-skill", result.get(0).getName());
    }
    
    @Test
    void testIsSkillActive() {
        // 准备测试数据
        SkillSpec activeSkill = SkillSpec.builder()
                .name("active-skill")
                .build();
        
        // Mock context
        when(context.getActiveSkills()).thenReturn(List.of(activeSkill));
        
        // 执行测试
        boolean isActive = skillProvider.isSkillActive(context, "active-skill");
        boolean isNotActive = skillProvider.isSkillActive(context, "other-skill");
        
        // 验证结果
        assertTrue(isActive);
        assertFalse(isNotActive);
    }
}
