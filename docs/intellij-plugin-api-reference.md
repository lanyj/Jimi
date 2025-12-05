# Jimi RPC API 参考文档

> JSON-RPC 2.0 协议规范及API详细定义

---

## 📋 目录

1. [协议概述](#协议概述)
2. [基础规范](#基础规范)
3. [API方法](#api方法)
4. [事件流](#事件流)
5. [错误处理](#错误处理)
6. [示例代码](#示例代码)

---

## 协议概述

### 传输协议

- **基础协议**: JSON-RPC 2.0
- **传输层**: HTTP/1.1
- **内容类型**: `application/json`
- **字符编码**: UTF-8

### 端点地址

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/rpc` | POST | JSON-RPC调用入口 |
| `/api/v1/events/{sessionId}` | GET | SSE事件流 |
| `/api/v1/health` | GET | 健康检查 |

---

## 基础规范

### 请求格式

```json
{
  "jsonrpc": "2.0",
  "id": "string",
  "method": "string",
  "params": {}
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `jsonrpc` | string | ✅ | 固定值 "2.0" |
| `id` | string | ✅ | 请求唯一标识,用于匹配响应 |
| `method` | string | ✅ | 调用的方法名 |
| `params` | object | ✅ | 方法参数(可为空对象) |

### 响应格式

**成功响应:**
```json
{
  "jsonrpc": "2.0",
  "id": "string",
  "result": {}
}
```

**错误响应:**
```json
{
  "jsonrpc": "2.0",
  "id": "string",
  "error": {
    "code": -32600,
    "message": "Invalid Request",
    "data": {}
  }
}
```

---

## API方法

### 1. initialize - 初始化会话

创建新的Jimi会话并返回会话ID。

#### 请求

```http
POST /api/v1/rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-001",
  "method": "initialize",
  "params": {
    "workDir": "/path/to/project",
    "agentName": "default",
    "model": "qwen-max",
    "yolo": false,
    "mcpConfigFiles": ["/path/to/mcp.json"]
  }
}
```

#### 参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `workDir` | string | ✅ | - | 工作目录绝对路径 |
| `agentName` | string | ❌ | "default" | Agent名称 |
| `model` | string | ❌ | 配置默认模型 | LLM模型名称 |
| `yolo` | boolean | ❌ | false | 是否启用YOLO模式(自动批准) |
| `mcpConfigFiles` | string[] | ❌ | [] | MCP配置文件路径列表 |

#### 响应

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "result": {
    "sessionId": "session-2024-12-02-abc123",
    "status": "initialized",
    "config": {
      "agent": "default",
      "model": "qwen-max",
      "maxSteps": 100,
      "workDir": "/path/to/project"
    }
  }
}
```

#### 返回值字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `sessionId` | string | 会话唯一ID,后续请求需携带 |
| `status` | string | 初始化状态: "initialized" |
| `config` | object | 会话配置信息 |
| `config.agent` | string | 使用的Agent名称 |
| `config.model` | string | 使用的LLM模型 |
| `config.maxSteps` | number | 最大执行步数 |
| `config.workDir` | string | 工作目录 |

---

### 2. execute - 执行任务

提交用户输入并执行AI任务(异步执行)。

#### 请求

```http
POST /api/v1/rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-002",
  "method": "execute",
  "params": {
    "sessionId": "session-2024-12-02-abc123",
    "input": "帮我分析这个项目的架构"
  }
}
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | ✅ | 会话ID |
| `input` | string | ✅ | 用户输入文本 |

#### 响应

```json
{
  "jsonrpc": "2.0",
  "id": "req-002",
  "result": {
    "taskId": "task-456",
    "status": "running"
  }
}
```

**注意:** 此方法立即返回,实际执行进度通过SSE事件流推送。

#### 返回值字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `taskId` | string | 任务唯一ID |
| `status` | string | 任务状态: "running" \| "completed" \| "failed" |

---

### 3. getStatus - 获取引擎状态

查询当前会话的执行状态。

#### 请求

```http
POST /api/v1/rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-003",
  "method": "getStatus",
  "params": {
    "sessionId": "session-2024-12-02-abc123"
  }
}
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | ✅ | 会话ID |

#### 响应

```json
{
  "jsonrpc": "2.0",
  "id": "req-003",
  "result": {
    "currentStep": 5,
    "maxSteps": 100,
    "tokenCount": 1250,
    "maxContextSize": 32000,
    "availableTokens": 28750,
    "contextUsagePercent": 3.91,
    "checkpointCount": 3,
    "reservedTokens": 2000,
    "status": "running"
  }
}
```

#### 返回值字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `currentStep` | number | 当前执行步数 |
| `maxSteps` | number | 最大允许步数 |
| `tokenCount` | number | 已使用Token数 |
| `maxContextSize` | number | 模型上下文窗口大小 |
| `availableTokens` | number | 剩余可用Token数 |
| `contextUsagePercent` | number | 上下文使用百分比 |
| `checkpointCount` | number | 检查点数量 |
| `reservedTokens` | number | 保留Token数 |
| `status` | string | 执行状态 |

---

### 4. interrupt - 中断任务

中断当前正在执行的任务。

#### 请求

```http
POST /api/v1/rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-004",
  "method": "interrupt",
  "params": {
    "sessionId": "session-2024-12-02-abc123"
  }
}
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | ✅ | 会话ID |

#### 响应

```json
{
  "jsonrpc": "2.0",
  "id": "req-004",
  "result": {
    "status": "interrupted",
    "reason": "User requested interruption"
  }
}
```

---

### 5. shutdown - 关闭会话

关闭会话并释放资源。

#### 请求

```http
POST /api/v1/rpc
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-005",
  "method": "shutdown",
  "params": {
    "sessionId": "session-2024-12-02-abc123"
  }
}
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | ✅ | 会话ID |

#### 响应

```json
{
  "jsonrpc": "2.0",
  "id": "req-005",
  "result": {
    "status": "shutdown",
    "savedHistory": true,
    "historyFile": "/path/to/.jimi/sessions/session-abc/history.jsonl"
  }
}
```

---

## 事件流

### SSE连接

```http
GET /api/v1/events/{sessionId}
Accept: text/event-stream
```

### 事件格式

```
event: <event_type>
data: {"type":"<event_type>","data":{...}}

```

### 事件类型

#### 1. step_begin - 步骤开始

```
event: step_begin
data: {"type":"step_begin","data":{"step":1,"timestamp":"2024-12-02T10:30:00Z"}}
```

**字段说明:**
- `step`: 步骤编号(从1开始)
- `timestamp`: ISO 8601时间戳

---

#### 2. content - 内容增量

```
event: content
data: {"type":"content","data":{"text":"我来帮你","delta":true}}
```

**字段说明:**
- `text`: 文本内容
- `delta`: 是否为增量更新(true表示追加到上一条内容)

---

#### 3. tool_call - 工具调用

```
event: tool_call
data: {
  "type":"tool_call",
  "data":{
    "id":"call_123",
    "name":"read_file",
    "arguments":"{\"path\":\"/src/main.java\"}"
  }
}
```

**字段说明:**
- `id`: 工具调用唯一ID
- `name`: 工具名称
- `arguments`: JSON格式的参数字符串

---

#### 4. tool_result - 工具结果

```
event: tool_result
data: {
  "type":"tool_result",
  "data":{
    "id":"call_123",
    "result":"文件内容...",
    "error":null
  }
}
```

**字段说明:**
- `id`: 对应的工具调用ID
- `result`: 执行结果(字符串)
- `error`: 错误信息(如果执行失败)

---

#### 5. compaction_begin - 压缩开始

```
event: compaction_begin
data: {
  "type":"compaction_begin",
  "data":{
    "reason":"Context size exceeded 80%"
  }
}
```

---

#### 6. compaction_end - 压缩结束

```
event: compaction_end
data: {
  "type":"compaction_end",
  "data":{
    "savedTokens":5000,
    "beforeSize":28000,
    "afterSize":23000
  }
}
```

---

#### 7. status_update - 状态更新

```
event: status_update
data: {
  "type":"status_update",
  "data":{
    "field":"tokenCount",
    "value":1500
  }
}
```

---

#### 8. skills_activated - Skills激活

```
event: skills_activated
data: {
  "type":"skills_activated",
  "data":{
    "skills":["java-expert","spring-boot"],
    "count":2
  }
}
```

---

#### 9. step_interrupted - 步骤中断

```
event: step_interrupted
data: {
  "type":"step_interrupted",
  "data":{
    "reason":"User requested interruption"
  }
}
```

---

#### 10. done - 任务完成

```
event: done
data: {
  "type":"done",
  "data":{
    "status":"success",
    "totalSteps":5,
    "totalTokens":3200
  }
}
```

---

## 错误处理

### 标准错误码

| 错误码 | 名称 | HTTP状态 | 说明 |
|--------|------|---------|------|
| -32700 | Parse error | 500 | JSON解析失败 |
| -32600 | Invalid Request | 400 | 请求格式错误 |
| -32601 | Method not found | 404 | 方法不存在 |
| -32602 | Invalid params | 400 | 参数无效 |
| -32603 | Internal error | 500 | 内部错误 |

### 业务错误码

| 错误码 | 名称 | HTTP状态 | 说明 |
|--------|------|---------|------|
| -32000 | Session not found | 404 | 会话不存在 |
| -32001 | LLM not configured | 500 | LLM未配置 |
| -32002 | Task execution failed | 500 | 任务执行失败 |
| -32003 | Agent not found | 404 | Agent不存在 |
| -32004 | Invalid work directory | 400 | 工作目录无效 |
| -32005 | Max steps reached | 500 | 达到最大步数限制 |

### 错误响应示例

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "error": {
    "code": -32001,
    "message": "LLM not configured",
    "data": {
      "detail": "请在 ~/.jimi/config.yml 中配置LLM",
      "configPath": "/Users/yefei.yf/.jimi/config.yml",
      "docs": "https://github.com/leavesfly/jimi#configuration"
    }
  }
}
```

---

## 示例代码

### JavaScript/TypeScript

```typescript
// 使用fetch API调用
async function callJimiRpc(method: string, params: object) {
  const response = await fetch('http://localhost:9527/api/v1/rpc', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: Math.random().toString(36),
      method,
      params
    })
  });
  
  const result = await response.json();
  
  if (result.error) {
    throw new Error(`RPC Error: ${result.error.message}`);
  }
  
  return result.result;
}

// 初始化会话
const initResult = await callJimiRpc('initialize', {
  workDir: '/path/to/project',
  agentName: 'default'
});
console.log('Session ID:', initResult.sessionId);

// 执行任务
await callJimiRpc('execute', {
  sessionId: initResult.sessionId,
  input: '分析项目架构'
});

// 订阅事件流
const eventSource = new EventSource(
  `http://localhost:9527/api/v1/events/${initResult.sessionId}`
);

eventSource.addEventListener('content', (e) => {
  const data = JSON.parse(e.data);
  console.log('Content:', data.data.text);
});

eventSource.addEventListener('done', (e) => {
  console.log('Task completed');
  eventSource.close();
});
```

### Python

```python
import requests
import json
from sseclient import SSEClient

# RPC调用
def call_jimi_rpc(method, params):
    response = requests.post(
        'http://localhost:9527/api/v1/rpc',
        json={
            'jsonrpc': '2.0',
            'id': '1',
            'method': method,
            'params': params
        }
    )
    result = response.json()
    
    if 'error' in result:
        raise Exception(f"RPC Error: {result['error']['message']}")
    
    return result['result']

# 初始化
init_result = call_jimi_rpc('initialize', {
    'workDir': '/path/to/project',
    'agentName': 'default'
})
session_id = init_result['sessionId']

# 执行任务
call_jimi_rpc('execute', {
    'sessionId': session_id,
    'input': '分析项目架构'
})

# 订阅事件流
messages = SSEClient(f'http://localhost:9527/api/v1/events/{session_id}')
for msg in messages:
    if msg.event == 'content':
        data = json.loads(msg.data)
        print('Content:', data['data']['text'])
    elif msg.event == 'done':
        break
```

### Java (OkHttp)

```java
// 见快速开始指南中的JimiRpcClient实现
```

### curl命令行

```bash
# 初始化会话
curl -X POST http://localhost:9527/api/v1/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "initialize",
    "params": {
      "workDir": "/tmp/test"
    }
  }'

# 执行任务
curl -X POST http://localhost:9527/api/v1/rpc \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "2",
    "method": "execute",
    "params": {
      "sessionId": "session-abc",
      "input": "帮我分析代码"
    }
  }'

# 订阅事件流
curl -N http://localhost:9527/api/v1/events/session-abc
```

---

## 性能建议

### 1. 连接复用

- 使用HTTP Keep-Alive复用TCP连接
- SSE连接应保持打开,避免频繁重连

### 2. 超时设置

```kotlin
val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时
    .readTimeout(300, TimeUnit.SECONDS)    // 读取超时(execute可能很长)
    .writeTimeout(30, TimeUnit.SECONDS)    // 写入超时
    .build()
```

### 3. 并发控制

- 每个会话同一时间只执行一个任务
- 多会话可并发执行(服务端线程池管理)

### 4. 错误重试

- 网络错误: 指数退避重试(最多3次)
- 业务错误: 不重试,直接返回给用户

---

## 版本兼容

### 协议版本

当前版本: **v1.0.0**

### 向后兼容策略

- **MAJOR版本**: 破坏性变更,不兼容旧版本
- **MINOR版本**: 新增功能,向后兼容
- **PATCH版本**: Bug修复,完全兼容

### 版本协商

客户端在`initialize`时应发送`protocolVersion`:

```json
{
  "method": "initialize",
  "params": {
    "protocolVersion": "1.0.0",
    ...
  }
}
```

服务端在响应中返回实际支持的版本:

```json
{
  "result": {
    "protocolVersion": "1.0.0",
    ...
  }
}
```

---

**文档版本**: v1.0  
**最后更新**: 2024-12-02  
**维护者**: Jimi开发团队
