package io.leavesfly.jimi.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skill加载器
 * 
 * 职责：
 * - 从文件系统扫描和加载Skills
 * - 解析SKILL.md文件（YAML Front Matter + Markdown内容）
 * - 支持从类路径和用户目录加载
 * 
 * 加载策略：
 * - 类路径优先：首先尝试从resources/skills加载内置Skills
 * - 用户目录回退：如果类路径不可用，回退到~/.jimi/skills
 * - 合并加载：两个位置的Skills都会被加载
 */
@Slf4j
@Service
public class SkillLoader {
    
    /**
     * YAML Front Matter的正则表达式
     * 匹配格式：
     * ---
     * key: value
     * ---
     */
    private static final Pattern FRONT_MATTER_PATTERN = 
        Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);
    
    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;
    
    /**
     * 获取全局Skills目录列表
     * 返回所有可能的全局Skills目录（按优先级排序）
     * 
     * @return 全局Skills目录列表
     */
    public List<Path> getGlobalSkillsDirectories() {
        List<Path> directories = new ArrayList<>();
        
        // 1. 尝试从类路径加载（resources/skills）
        try {
            URL resource = SkillLoader.class.getClassLoader().getResource("skills");
            if (resource != null) {
                Path classPathDir = Paths.get(resource.toURI());
                if (Files.exists(classPathDir) && Files.isDirectory(classPathDir)) {
                    directories.add(classPathDir);
                    log.debug("Found skills directory in classpath: {}", classPathDir);
                }
            }
        } catch (Exception e) {
            log.debug("Unable to load skills from classpath", e);
        }
        
        // 2. 用户目录（~/.jimi/skills）
        String userHome = System.getProperty("user.home");
        Path userSkillsDir = Paths.get(userHome, ".jimi", "skills");
        if (Files.exists(userSkillsDir) && Files.isDirectory(userSkillsDir)) {
            directories.add(userSkillsDir);
            log.debug("Found skills directory in user home: {}", userSkillsDir);
        }
        
        return directories;
    }
    
    /**
     * 从指定目录加载所有Skills
     * 
     * @param directory Skills根目录
     * @param scope Skill作用域
     * @return 加载的SkillSpec列表
     */
    public List<SkillSpec> loadSkillsFromDirectory(Path directory, SkillScope scope) {
        List<SkillSpec> skills = new ArrayList<>();
        
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            log.debug("Skills directory not found: {}", directory);
            return skills;
        }
        
        try {
            Files.list(directory)
                .filter(Files::isDirectory)
                .forEach(skillDir -> {
                    Path skillFile = skillDir.resolve("SKILL.md");
                    if (Files.exists(skillFile)) {
                        try {
                            SkillSpec skill = parseSkillFile(skillFile);
                            if (skill != null) {
                                // 设置作用域和资源路径
                                skill.setScope(scope);
                                skill.setSkillFilePath(skillFile);
                                
                                // 检查是否有resources目录
                                Path resourcesDir = skillDir.resolve("resources");
                                if (Files.exists(resourcesDir) && Files.isDirectory(resourcesDir)) {
                                    skill.setResourcesPath(resourcesDir);
                                }
                                
                                skills.add(skill);
                                log.debug("Loaded skill: {} from {}", skill.getName(), skillFile);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse skill file: {}", skillFile, e);
                        }
                    }
                });
        } catch (IOException e) {
            log.error("Failed to list skills directory: {}", directory, e);
        }
        
        return skills;
    }
    
    /**
     * 解析单个SKILL.md文件
     * 
     * 文件格式：
     * ---
     * name: skill-name
     * description: 描述
     * version: 1.0.0
     * category: 分类
     * triggers:
     *   - 关键词1
     *   - 关键词2
     * ---
     * 
     * Markdown内容...
     * 
     * @param skillFile SKILL.md文件路径
     * @return 解析的SkillSpec对象，解析失败返回null
     */
    public SkillSpec parseSkillFile(Path skillFile) {
        try {
            // 读取文件全文
            String fileContent = Files.readString(skillFile);
            
            // 尝试匹配YAML Front Matter
            Matcher matcher = FRONT_MATTER_PATTERN.matcher(fileContent);
            
            String yamlContent;
            String markdownContent;
            
            if (matcher.matches()) {
                // 有Front Matter
                yamlContent = matcher.group(1);
                markdownContent = matcher.group(2).trim();
            } else {
                // 没有Front Matter，使用默认值
                log.warn("SKILL.md file missing YAML Front Matter: {}", skillFile);
                yamlContent = null;
                markdownContent = fileContent.trim();
            }
            
            // 解析YAML元数据
            SkillSpec.SkillSpecBuilder builder = SkillSpec.builder();
            
            if (yamlContent != null) {
                try {
                    Map<String, Object> metadata = yamlObjectMapper.readValue(
                        yamlContent, 
                        Map.class
                    );
                    
                    // 提取字段
                    if (metadata.containsKey("name")) {
                        builder.name((String) metadata.get("name"));
                    }
                    if (metadata.containsKey("description")) {
                        builder.description((String) metadata.get("description"));
                    }
                    if (metadata.containsKey("version")) {
                        builder.version((String) metadata.get("version"));
                    }
                    if (metadata.containsKey("category")) {
                        builder.category((String) metadata.get("category"));
                    }
                    if (metadata.containsKey("triggers")) {
                        builder.triggers((List<String>) metadata.get("triggers"));
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to parse YAML Front Matter in {}, using defaults", skillFile, e);
                }
            }
            
            // 设置内容
            builder.content(markdownContent);
            
            SkillSpec skill = builder.build();
            
            // 验证必填字段
            if (skill.getName() == null || skill.getName().isEmpty()) {
                log.error("Skill name is required in: {}", skillFile);
                return null;
            }
            if (skill.getDescription() == null || skill.getDescription().isEmpty()) {
                log.warn("Skill description is missing in: {}", skillFile);
                skill.setDescription("No description");
            }
            if (skill.getContent() == null || skill.getContent().isEmpty()) {
                log.warn("Skill content is empty in: {}", skillFile);
            }
            
            return skill;
            
        } catch (IOException e) {
            log.error("Failed to read skill file: {}", skillFile, e);
            return null;
        }
    }
}
