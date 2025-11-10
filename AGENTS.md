# Jimi 项目技术文档

## 1. 项目概述

### 项目类型
Jimi 是一个基于 Java 17 和 Spring Boot 3 构建的智能 CLI 代理，专为软件开发任务和终端操作而设计。

### 主要功能
- **智能开发助手**：通过集成大语言模型（LLM）辅助软件开发工作流程
- **交互式 Shell**：提供交互式命令行界面
- **批处理模式**：支持单次命令执行模式
- **多工具集成**：集成了文件操作、代码分析、Shell 执行、Web 搜索等多种工具
- **Agent 系统**：通过主 Agent 和多个 Subagent 实现任务的智能化处理

### 核心特性
1. **模块化架构**：核心模块清晰分离，易于扩展和维护
2. **响应式编程**：基于 Project Reactor 实现异步非阻塞操作
3. **智能协作**：支持主 Agent 和多个 Subagent 协同工作
4. **安全可控**：内置审批机制，危险操作需用户确认
5. **协议集成**：支持 MCP（Model Context Protocol）和 ACP（Agent Client Protocol）

## 2. 项目结构

```
jimi/
├── doc/                          # 文档目录
│   ├── QUICKSTART.md            # 快速入门
│   ├── RUNNING.md               # 运行指南
│   └── MCP_LOCAL_IMPLEMENTATION.md  # MCP 实现文档
│
├── script/                       # 脚本目录
│   ├── Makefile                 # Make 构建脚本
│   ├── deploy.sh                # 部署脚本
│   └── jimi.bat                 # Windows 启动脚本
│
├── src/
│   ├── main/
│   │   ├── java/io/leavesfly/jimi/
│   │   │   ├── agent/           # Agent 系统
│   │   │   ├── cli/             # 命令行入口
│   │   │   ├── command/         # 元命令系统
│   │   │   ├── config/          # 配置系统
│   │   │   ├── exception/       # 异常定义
│   │   │   ├── llm/             # LLM 通信层
│   │   │   ├── session/         # 会话管理
│   │   │   ├── soul/            # 智能核心
│   │   │   ├── tool/            # 工具系统
│   │   │   ├── ui/              # 用户界面
│   │   │   ├── wire/            # 消息传输
│   │   │   ├── JimiApplication.java  # 主启动类
│   │   │   └── JimiFactory.java      # 工厂类
│   │   │
│   │   └── resources/
│   │       ├── agents/          # Agent 配置
│   │       │   ├── default/     # 默认 Agent
│   │       │   ├── build/       # 构建 Agent
│   │       │   ├── test/        # 测试 Agent
│   │       │   ├── debug/       # 调试 Agent
│   │       │   ├── research/    # 研究 Agent
│   │       │   └── SUBAGENTS_USAGE.md
│   │       ├── application.yml  # Spring Boot 配置
│   │       ├── config-template.json  # 配置模板
│   │       └── providers-config-examples.yaml
│   │
│   └── test/                     # 测试代码
│
├── pom.xml                       # Maven 配置
└── README.md                     # 项目说明
```

## 3. 技术栈

### 编程语言和版本
- **Java 17+**

### 核心框架
- **Spring Boot 3.2.5**
- **Project Reactor**：响应式编程支持
- **Picocli 4.7.6**：命令行参数解析
- **JLine 3.25.1**：终端交互

### 主要依赖
- **Jackson 2.16.2**：JSON 处理
- **SnakeYAML 2.2**：YAML 处理
- **Apache Commons Exec 1.4.0**：进程执行
- **Jsoup 1.17.2**：HTML 解析
- **Java Diff Utils 4.12**：差异比较
- **Caffeine 3.1.8**：缓存
- **Lombok 1.18.32**：代码简化
- **SLF4J 2.0.12 + Logback 1.4.14**：日志

### 测试框架
- **JUnit 5.10.2**
- **Mockito 5.11.0**

### 构建工具
- **Maven 3.9+**

## 4. 构建和运行

### 环境要求
- Java 17 或更高版本
- Maven 3.9+（仅构建时需要）
- LLM API Key（Moonshot/OpenAI/DeepSeek/Qwen）

### 构建命令
```bash
# 使用 Maven 构建
mvn clean package

# 或使用 Makefile（推荐）
make build
```
### 运行命令
### 运行命令
1. 查看版本: ./jimi --version
2. 显示帮助: ./jimi --help
3. 启动交互式 Shell: ./jimi -w /path/to/your/project
4. Execute single command: ./jimi -w /path/to/your/project -c Analyze_project_structure
5. Continue previous session: ./jimi -w /path/to/your/project -C

### System Installation
1. Install to ~/.local/bin: ./script/deploy.sh
2. Or use Makefile: make install

### Configuration
Configuration files are located in ~/.config/jimi/ directory:
- config.json: Main configuration file
- providers.json: LLM provider configuration

## 5. 开发规范

### 代码组织
- 遵循标准 Maven 项目结构
- 按功能模块划分包结构
- 核心模块包括：agent、cli、command、config、llm、session、soul、tool、ui、wire

### 命名规范
- 类名使用 PascalCase
- 方法名和变量名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 包名使用小写字母

### 注释风格
- 使用 JavaDoc 格式注释
- 类、方法、字段都需要适当的文档说明
- 复杂逻辑需要行内注释解释

### 设计模式
- Builder 模式：用于构建复杂对象
- 工厂模式：用于创建工具和 Agent 实例
- 策略模式：用于不同的 LLM 提供商实现
- 响应式编程：使用 Project Reactor 处理异步操作

## 6. 测试

### 测试框架
- JUnit 5 作为主要测试框架
- Mockito 用于模拟对象

### 运行命令
- 运行所有测试: make test
- 运行特定测试: mvn test -Dtest=ConfigLoaderTest
- 生成测试覆盖率报告: mvn clean verify jacoco:report

### 覆盖率要求
项目未明确指定测试覆盖率要求，但建议:
- 核心功能模块测试覆盖率应达到 80% 以上
- 工具类应有完整的单元测试
