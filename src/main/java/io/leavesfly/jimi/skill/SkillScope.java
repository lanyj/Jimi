package io.leavesfly.jimi.skill;

/**
 * Skill作用域枚举
 * 
 * 定义Skill的生效范围：
 * - GLOBAL: 全局作用域，对所有项目生效
 * - PROJECT: 项目作用域，仅对当前项目生效
 */
public enum SkillScope {
    
    /**
     * 全局作用域
     * 从 ~/.jimi/skills/ 或 resources/skills/ 加载
     */
    GLOBAL,
    
    /**
     * 项目作用域
     * 从项目根目录 .jimi/skills/ 加载
     */
    PROJECT
}
