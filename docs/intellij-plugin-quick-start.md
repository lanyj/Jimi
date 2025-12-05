# Jimi IntelliJ IDEA 插件快速开始指南

> 本文档提供从零开始开发Jimi IDEA插件的快速上手指南

---

## 📋 前置条件

### 开发环境要求

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 与Jimi CLI保持一致 |
| IntelliJ IDEA | 2023.1+ | 用于开发插件本身 |
| Gradle | 8.0+ | 插件构建工具 |
| Kotlin | 1.9+ | 推荐使用Kotlin开发IDEA插件 |

### Jimi CLI准备

```bash
# 确保Jimi CLI可正常运行
cd /Users/yefei.yf/QoderCLI/Jimi
mvn clean package
java -jar target/jimi-0.1.0.jar --help
```

---

## 🚀 第一步: 创建插件项目

### 1.1 使用IDEA创建Gradle插件项目

```bash
# 方式1: 使用IDEA向导
# File -> New -> Project
# 选择: Gradle -> IntelliJ Platform Plugin
# Language: Kotlin
# Build system: Gradle (Kotlin DSL)

# 方式2: 使用命令行
mkdir jimi-intellij-plugin
cd jimi-intellij-plugin
```

### 1.2 配置build.gradle.kts

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.leavesfly"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // HTTP客户端
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    
    // JSON处理
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // 测试
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

intellij {
    version.set("2023.1")
    type.set("IC") // IC = IntelliJ IDEA Community, IU = Ultimate
    plugins.set(listOf())
}

tasks {
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
    }
    
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
    
    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
```

---

## 🔧 第二步: 实现Jimi RPC客户端

### 2.1 定义数据模型

**创建 `src/main/kotlin/com/leavesfly/jimi/rpc/models/`:**

```kotlin
// JsonRpcRequest.kt
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: Map<String, Any?>
)

// JsonRpcResponse.kt
data class JsonRpcResponse(
    val jsonrpc: String,
    val id: String,
    val result: Map<String, Any?>? = null,
    val error: RpcError? = null
)

data class RpcError(
    val code: Int,
    val message: String,
    val data: Map<String, Any?>? = null
)

// InitializeResponse.kt
data class InitializeResponse(
    val sessionId: String,
    val status: String,
    val config: Map<String, Any?>? = null
)

// ExecuteResponse.kt
data class ExecuteResponse(
    val taskId: String,
    val status: String
)

// WireEvent.kt
data class WireEvent(
    val type: String,
    val data: Map<String, Any?>
)
```

### 2.2 实现RPC客户端

**创建 `JimiRpcClient.kt`:**

```kotlin
package com.leavesfly.jimi.rpc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.Closeable
import java.util.concurrent.TimeUnit

class JimiRpcClient(private val baseUrl: String) : Closeable {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val objectMapper = jacksonObjectMapper()
    private var eventSource: EventSource? = null
    
    /**
     * 初始化会话
     */
    suspend fun initialize(
        workDir: String,
        agentName: String? = null,
        model: String? = null,
        yolo: Boolean = false
    ): InitializeResponse = withContext(Dispatchers.IO) {
        val request = JsonRpcRequest(
            method = "initialize",
            params = mapOf(
                "workDir" to workDir,
                "agentName" to agentName,
                "model" to model,
                "yolo" to yolo
            )
        )
        
        call(request, InitializeResponse::class.java)
    }
    
    /**
     * 执行任务
     */
    suspend fun execute(
        sessionId: String,
        input: String
    ): ExecuteResponse = withContext(Dispatchers.IO) {
        val request = JsonRpcRequest(
            method = "execute",
            params = mapOf(
                "sessionId" to sessionId,
                "input" to input
            )
        )
        
        call(request, ExecuteResponse::class.java)
    }
    
    /**
     * 获取状态
     */
    suspend fun getStatus(sessionId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        val request = JsonRpcRequest(
            method = "getStatus",
            params = mapOf("sessionId" to sessionId)
        )
        
        val response = callRaw(request)
        response.result ?: emptyMap()
    }
    
    /**
     * 中断任务
     */
    suspend fun interrupt(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val request = JsonRpcRequest(
            method = "interrupt",
            params = mapOf("sessionId" to sessionId)
        )
        
        val response = callRaw(request)
        response.result?.get("status") == "interrupted"
    }
    
    /**
     * 关闭会话
     */
    suspend fun shutdown(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val request = JsonRpcRequest(
            method = "shutdown",
            params = mapOf("sessionId" to sessionId)
        )
        
        val response = callRaw(request)
        response.result?.get("status") == "shutdown"
    }
    
    /**
     * 订阅事件流
     */
    fun subscribeEvents(
        sessionId: String,
        onEvent: (WireEvent) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        val url = "$baseUrl/api/v1/events/$sessionId"
        
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()
        
        eventSource = EventSources.createFactory(httpClient)
            .newEventSource(request, object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    try {
                        val event: WireEvent = objectMapper.readValue(data)
                        onEvent(event)
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
                
                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    t?.let { onError(it) }
                }
            })
    }
    
    /**
     * 取消事件订阅
     */
    fun unsubscribeEvents() {
        eventSource?.cancel()
        eventSource = null
    }
    
    private suspend fun <T> call(
        request: JsonRpcRequest,
        responseType: Class<T>
    ): T = withContext(Dispatchers.IO) {
        val response = callRaw(request)
        
        if (response.error != null) {
            throw RpcException(response.error)
        }
        
        objectMapper.convertValue(response.result!!, responseType)
    }
    
    private fun callRaw(request: JsonRpcRequest): JsonRpcResponse {
        val body = objectMapper.writeValueAsString(request)
            .toRequestBody("application/json".toMediaType())
        
        val httpRequest = Request.Builder()
            .url("$baseUrl/api/v1/rpc")
            .post(body)
            .build()
        
        val response = httpClient.newCall(httpRequest).execute()
        
        if (!response.isSuccessful) {
            throw HttpException(response.code, response.message)
        }
        
        return objectMapper.readValue(response.body!!.string())
    }
    
    override fun close() {
        unsubscribeEvents()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }
}

// 自定义异常
class RpcException(val error: RpcError) : Exception(error.message)
class HttpException(val code: Int, message: String) : Exception("HTTP $code: $message")
```

---

## 🎨 第三步: 实现插件UI

### 3.1 创建ToolWindow

**创建 `JimiToolWindowFactory.kt`:**

```kotlin
package com.leavesfly.jimi.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class JimiToolWindowFactory : ToolWindowFactory, DumbAware {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val jimiToolWindow = JimiToolWindow(project)
        val content = ContentFactory.getInstance().createContent(
            jimiToolWindow.getContent(),
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}
```

### 3.2 创建主面板

**创建 `JimiToolWindow.kt`:**

```kotlin
package com.leavesfly.jimi.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.leavesfly.jimi.service.JimiPluginService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.*

class JimiToolWindow(private val project: Project) {
    
    private val chatPanel = ChatPanel()
    private val inputField = JBTextArea(3, 50)
    private val sendButton = JButton("发送")
    
    fun getContent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 聊天显示区域
        mainPanel.add(JBScrollPane(chatPanel), BorderLayout.CENTER)
        
        // 输入区域
        val inputPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5)
            
            add(JBScrollPane(inputField), BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }
        mainPanel.add(inputPanel, BorderLayout.SOUTH)
        
        // 绑定事件
        sendButton.addActionListener {
            val input = inputField.text.trim()
            if (input.isNotEmpty()) {
                sendMessage(input)
                inputField.text = ""
            }
        }
        
        // Enter发送, Shift+Enter换行
        inputField.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    sendButton.doClick()
                }
            }
        })
        
        return mainPanel
    }
    
    private fun sendMessage(input: String) {
        chatPanel.addUserMessage(input)
        
        val service = JimiPluginService.getInstance(project)
        
        GlobalScope.launch {
            try {
                service.executeTask(input)
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    chatPanel.addErrorMessage("执行失败: ${e.message}")
                }
            }
        }
    }
}
```

### 3.3 创建聊天面板

**创建 `ChatPanel.kt`:**

```kotlin
package com.leavesfly.jimi.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.*

class ChatPanel : JPanel() {
    
    private val messagesPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10)
    }
    
    init {
        layout = BorderLayout()
        add(JScrollPane(messagesPanel), BorderLayout.CENTER)
    }
    
    fun addUserMessage(text: String) {
        SwingUtilities.invokeLater {
            val messagePanel = createMessagePanel(text, isUser = true)
            messagesPanel.add(messagePanel)
            messagesPanel.revalidate()
            messagesPanel.repaint()
            scrollToBottom()
        }
    }
    
    fun addAssistantMessage(text: String) {
        SwingUtilities.invokeLater {
            val messagePanel = createMessagePanel(text, isUser = false)
            messagesPanel.add(messagePanel)
            messagesPanel.revalidate()
            messagesPanel.repaint()
            scrollToBottom()
        }
    }
    
    fun appendToLastMessage(text: String) {
        SwingUtilities.invokeLater {
            if (messagesPanel.componentCount > 0) {
                val lastPanel = messagesPanel.getComponent(messagesPanel.componentCount - 1) as JPanel
                val label = lastPanel.getComponent(0) as JLabel
                label.text = label.text + text
            }
            scrollToBottom()
        }
    }
    
    fun addToolCall(name: String, arguments: String) {
        SwingUtilities.invokeLater {
            val text = "🔧 调用工具: $name\n参数: $arguments"
            val panel = createToolPanel(text)
            messagesPanel.add(panel)
            messagesPanel.revalidate()
            messagesPanel.repaint()
            scrollToBottom()
        }
    }
    
    fun addErrorMessage(text: String) {
        SwingUtilities.invokeLater {
            val panel = createErrorPanel(text)
            messagesPanel.add(panel)
            messagesPanel.revalidate()
            messagesPanel.repaint()
            scrollToBottom()
        }
    }
    
    fun showStepBegin(step: Int) {
        SwingUtilities.invokeLater {
            val panel = createInfoPanel("📍 步骤 $step 开始")
            messagesPanel.add(panel)
            messagesPanel.revalidate()
            messagesPanel.repaint()
        }
    }
    
    private fun createMessagePanel(text: String, isUser: Boolean): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, if (isUser) 50 else 10, 5, if (isUser) 10 else 50)
            
            val label = JBLabel("<html><body style='width: 400px'>$text</body></html>")
            label.border = JBUI.Borders.empty(8)
            label.background = if (isUser) Color(220, 240, 255) else Color(240, 240, 240)
            label.isOpaque = true
            
            add(label, if (isUser) BorderLayout.EAST else BorderLayout.WEST)
        }
    }
    
    private fun createToolPanel(text: String): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, 10)
            
            val label = JBLabel("<html><body>$text</body></html>")
            label.border = JBUI.Borders.empty(5)
            label.background = Color(255, 250, 205)
            label.isOpaque = true
            
            add(label, BorderLayout.CENTER)
        }
    }
    
    private fun createErrorPanel(text: String): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, 10)
            
            val label = JBLabel("<html><body>❌ $text</body></html>")
            label.border = JBUI.Borders.empty(5)
            label.background = Color(255, 220, 220)
            label.isOpaque = true
            
            add(label, BorderLayout.CENTER)
        }
    }
    
    private fun createInfoPanel(text: String): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, 10)
            
            val label = JBLabel(text)
            label.foreground = Color.GRAY
            
            add(label, BorderLayout.CENTER)
        }
    }
    
    private fun scrollToBottom() {
        val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this) as? JScrollPane
        scrollPane?.let {
            val vertical = it.verticalScrollBar
            vertical.value = vertical.maximum
        }
    }
}
```

---

## ⚙️ 第四步: 实现核心服务

**创建 `JimiPluginService.kt`:**

```kotlin
package com.leavesfly.jimi.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.leavesfly.jimi.process.JimiProcessManager
import com.leavesfly.jimi.rpc.JimiRpcClient
import com.leavesfly.jimi.ui.ChatPanel
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class JimiPluginService(private val project: Project) : Disposable {
    
    private val processManager = JimiProcessManager()
    private var rpcClient: JimiRpcClient? = null
    private var currentSessionId: String? = null
    private var chatPanel: ChatPanel? = null
    
    companion object {
        fun getInstance(project: Project): JimiPluginService =
            project.getService(JimiPluginService::class.java)
    }
    
    fun setChatPanel(panel: ChatPanel) {
        this.chatPanel = panel
    }
    
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 查找可用端口
            val port = findAvailablePort(9527, 9537)
            
            // 2. 查找Jimi JAR
            val jimiJar = findJimiJar()
                ?: throw IllegalStateException("未找到Jimi JAR文件,请先构建Jimi项目")
            
            // 3. 启动Jimi进程
            processManager.start(jimiJar, port)
            
            // 4. 等待服务器就绪
            waitForServerReady("http://localhost:$port")
            
            // 5. 创建RPC客户端
            rpcClient = JimiRpcClient("http://localhost:$port")
            
            // 6. 初始化会话
            val response = rpcClient!!.initialize(
                workDir = project.basePath ?: System.getProperty("user.dir"),
                agentName = "default"
            )
            currentSessionId = response.sessionId
            
            // 7. 订阅事件流
            subscribeEventStream()
            
            showNotification("Jimi已启动", NotificationType.INFORMATION)
            true
            
        } catch (e: Exception) {
            showNotification("启动失败: ${e.message}", NotificationType.ERROR)
            false
        }
    }
    
    suspend fun executeTask(input: String) {
        val sessionId = currentSessionId
            ?: throw IllegalStateException("Jimi未初始化,请先启动")
        
        try {
            rpcClient!!.execute(sessionId, input)
        } catch (e: Exception) {
            showNotification("执行失败: ${e.message}", NotificationType.ERROR)
            throw e
        }
    }
    
    private fun subscribeEventStream() {
        val sessionId = currentSessionId ?: return
        val panel = chatPanel ?: return
        
        rpcClient!!.subscribeEvents(
            sessionId = sessionId,
            onEvent = { event ->
                handleWireEvent(event, panel)
            },
            onError = { error ->
                showNotification("事件流错误: ${error.message}", NotificationType.ERROR)
            }
        )
    }
    
    private fun handleWireEvent(event: WireEvent, panel: ChatPanel) {
        when (event.type) {
            "step_begin" -> {
                val step = (event.data["step"] as? Number)?.toInt() ?: 0
                panel.showStepBegin(step)
            }
            "content" -> {
                val text = event.data["text"] as? String ?: ""
                val delta = event.data["delta"] as? Boolean ?: false
                
                if (delta) {
                    panel.appendToLastMessage(text)
                } else {
                    panel.addAssistantMessage(text)
                }
            }
            "tool_call" -> {
                val name = event.data["name"] as? String ?: ""
                val args = event.data["arguments"] as? String ?: ""
                panel.addToolCall(name, args)
            }
            "done" -> {
                // 任务完成
            }
        }
    }
    
    private fun showNotification(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Jimi Notifications")
            .createNotification(content, type)
            .notify(project)
    }
    
    override fun dispose() {
        rpcClient?.close()
        processManager.stop()
    }
}

// 工具函数
private fun findAvailablePort(start: Int, end: Int): Int {
    for (port in start..end) {
        try {
            java.net.ServerSocket(port).close()
            return port
        } catch (e: Exception) {
            continue
        }
    }
    throw IllegalStateException("未找到可用端口")
}

private suspend fun waitForServerReady(baseUrl: String, timeout: Duration = Duration.ofSeconds(10)) {
    val endTime = System.currentTimeMillis() + timeout.toMillis()
    
    while (System.currentTimeMillis() < endTime) {
        try {
            java.net.URL("$baseUrl/api/v1/health").openConnection().connect()
            return
        } catch (e: Exception) {
            delay(500)
        }
    }
    
    throw TimeoutException("等待Jimi服务器就绪超时")
}

private fun findJimiJar(): java.nio.file.Path? {
    // 在常见位置查找Jimi JAR
    val locations = listOf(
        "/Users/yefei.yf/QoderCLI/Jimi/target/jimi-0.1.0.jar",
        "../Jimi/target/jimi-0.1.0.jar",
        "~/jimi/jimi-0.1.0.jar"
    )
    
    return locations
        .map { java.nio.file.Paths.get(it.replace("~", System.getProperty("user.home"))) }
        .firstOrNull { java.nio.file.Files.exists(it) }
}
```

---

## 🏃 第五步: 运行和调试

### 5.1 启动Jimi RPC Server

```bash
cd /Users/yefei.yf/QoderCLI/Jimi
mvn clean package
java -jar target/jimi-0.1.0.jar --server --port 9527
```

### 5.2 运行插件

```bash
cd jimi-intellij-plugin
./gradlew runIde
```

### 5.3 测试流程

1. 新的IDEA窗口打开后,点击右侧工具栏的"Jimi"
2. 在输入框输入: "帮我分析这个项目"
3. 点击"发送"按钮
4. 观察聊天面板实时显示Jimi的响应

---

## 🐛 常见问题

### Q1: 找不到Jimi JAR文件

**解决:**
```bash
cd /Users/yefei.yf/QoderCLI/Jimi
mvn clean package -DskipTests
ls -lh target/jimi-0.1.0.jar
```

### Q2: 端口被占用

**解决:**
```bash
# 查找占用端口的进程
lsof -i :9527

# 杀死进程
kill -9 <PID>
```

### Q3: 事件流无响应

**检查:**
1. Jimi Server是否正常运行
2. 查看IDEA日志: Help -> Show Log in Finder
3. 使用curl测试SSE:
```bash
curl -N http://localhost:9527/api/v1/events/<sessionId>
```

---

## 📚 下一步

- [ ] 阅读完整技术方案: [intellij-plugin-integration-plan.md](./intellij-plugin-integration-plan.md)
- [ ] 查看API参考: [intellij-plugin-api-reference.md](./intellij-plugin-api-reference.md)
- [ ] 贡献代码: 提交PR到 https://github.com/leavesfly/jimi-intellij-plugin

---

**祝你开发顺利! 🎉**
