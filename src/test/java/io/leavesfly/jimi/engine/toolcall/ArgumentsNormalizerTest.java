package io.leavesfly.jimi.engine.toolcall;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArgumentsNormalizer 测试类
 */
class ArgumentsNormalizerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("测试你提供的实际原始字符串")
    void testYourActualString() {
        // 你提供的原始字符串字面量（Java代码中的写法）
        String input = "\"{\\\"command\\\": \\\"ls -la /Users/yefei.yf/Jimi/tank\\\", \\\"timeout\\\": 10}\"null";
        
        // 这个字符串的实际值是：
        // "{\"command\": \"ls -la /Users/yefei.yf/Jimi/tank\", \"timeout\": 10}"null
        
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        
        System.out.println("===============================");
        System.out.println("原始输入: " + input);
        System.out.println("处理结果: " + result);
        System.out.println("结果是否以{开头: " + result.startsWith("{"));
        System.out.println("结果是否以[开头: " + result.startsWith("["));
        System.out.println("===============================");
        
        // 验证不会被错误地转换为数组（这是之前的bug）
        assertFalse(result.startsWith("["), "不应该被转换为数组格式");
        
        // 验证结果是有效的JSON对象
        assertTrue(result.startsWith("{"), "应该是JSON对象格式");
        assertTrue(result.endsWith("}"), "应该以}结尾");
        
        // 验证可以被解析
        assertDoesNotThrow(() -> objectMapper.readTree(result));
        
        // 验证包含正确的内容
        assertTrue(result.contains("command"), "应该包含command字段");
        assertTrue(result.contains("timeout"), "应该包含timeout字段");
    }

    @Test
    @DisplayName("测试移除单个null后缀")
    void testRemoveSingleNullSuffix() {
        String input = "{\"command\": \"ls -la\", \"timeout\": 10}null";
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        assertEquals("{\"command\": \"ls -la\", \"timeout\": 10}", result);
    }

    @Test
    @DisplayName("测试移除多个null后缀")
    void testRemoveMultipleNullSuffix() {
        String input = "{\"command\": \"ls -la\", \"timeout\": 10}nullnull";
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        assertEquals("{\"command\": \"ls -la\", \"timeout\": 10}", result);
    }

    @Test
    @DisplayName("测试双重转义JSON带单个null后缀")
    void testDoubleEscapedJsonWithSingleNull() {
        String input = "\"{\\\"command\\\": \\\"ls -la\\\", \\\"timeout\\\": 10}\"null";
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        assertEquals("{\"command\": \"ls -la\", \"timeout\": 10}", result);
    }

    @Test
    @DisplayName("测试双重转义JSON带多个null后缀")
    void testDoubleEscapedJsonWithMultipleNull() {
        String input = "\"{\\\"command\\\": \\\"ls -la\\\", \\\"timeout\\\": 10}\"nullnull";
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        assertEquals("{\"command\": \"ls -la\", \"timeout\": 10}", result);
    }

    @Test
    @DisplayName("测试实际字面值：双层引号包裹+null后缀")
    void testActualLiteralWithQuotedNull() {
        // 字面值："\"{\\\"
        String input = "\"{\\\"command\\\": \\\"ls -la /Users/yefei.yf/Jimi/tank\\\"}\"null";
        
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        
        System.out.println("======= 实际字面值测试 =======");
        System.out.println("原始输入: " + input);
        System.out.println("处理结果: " + result);
        System.out.println("结果是否以{开头: " + result.startsWith("{"));
        System.out.println("结果是否以[开头: " + result.startsWith("["));
        System.out.println("=============================");
        
        // 验证不会被转换为数组
        assertFalse(result.startsWith("["), "不应该被转换为数组格式");
        
        // 验证结果是有效的JSON对象
        assertTrue(result.startsWith("{"), "应该是JSON对象格式");
        assertTrue(result.endsWith("}"), "应该以}结尾");
        
        // 验证可以被解析
        assertDoesNotThrow(() -> objectMapper.readTree(result));
        
        // 验证包含正确的内容
        assertTrue(result.contains("command"), "应该包含command字段");
    }

    @Test
    @DisplayName("测试包含未转义引号的JSON+双重null后缀")
    void testJsonWithUnescapedQuotesAndDoubleNull() {
        // 字面值："{\"command\": \"find ... -name \"*.java\" ...\", \"timeout\": 10}null"null
        // 注意：这里的 \"*.java\" 是未转义的引号
        String input = "{\"command\": \"find /Users/yefei.yf/Jimi/tank -type f -name \"*.java\" | head -20\", \"timeout\": 10}null";
        
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        
        System.out.println("======= 未转义引号+双重null测试 =======");
        System.out.println("原始输入: " + input);
        System.out.println("处理结果: " + result);
        System.out.println("结果是否以{开头: " + result.startsWith("{"));
        System.out.println("结果是否以}结尾: " + result.endsWith("}"));
        System.out.println("=============================");
        
        // 验证不会被转换为数组
        assertFalse(result.startsWith("["), "不应该被转换为数组格式");
        
        // 验证结果是有效的JSON对象
        assertTrue(result.startsWith("{"), "应该是JSON对象格式");
        assertTrue(result.endsWith("}"), "应该以}结尾");
        
        // 验证可以被解析
        assertDoesNotThrow(() -> objectMapper.readTree(result));
        
        // 验证包含正确的内容
        assertTrue(result.contains("command"), "应该包含command字段");
        assertTrue(result.contains("*.java"), "应该包含*.java");
    }

    @Test
    @DisplayName("测试引号包裹的JSON+null后缀(实际日志场景)")
    void testQuotedJsonWithNullSuffix() {
        // 模拟实际日志中的场景:"{...}null"
        String input = "\"{\\\"command\\\": \\\"ls -la /Users/yefei.yf/Jimi/tank\\\"}null\"";
        
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        
        System.out.println("======= 引号包裹+null后缀测试 =======");
        System.out.println("原始输入: " + input);
        System.out.println("处理结果: " + result);
        System.out.println("结果是否以{开头: " + result.startsWith("{"));
        System.out.println("结果是否以}结尾: " + result.endsWith("}"));
        System.out.println("=============================");
        
        // 验证不会被转换为数组
        assertFalse(result.startsWith("["), "不应该被转换为数组格式");
        
        // 验证结果是有效的JSON对象
        assertTrue(result.startsWith("{"), "应该是JSON对象格式");
        assertTrue(result.endsWith("}"), "应该以}结尾");
        
        // 验证可以被解析
        assertDoesNotThrow(() -> objectMapper.readTree(result));
        
        // 验证包含正确的内容
        assertTrue(result.contains("command"), "应该包含command字段");
    }

    @Test
    @DisplayName("测试双重null后缀场景 - 你提到的实际案例")
    void testDoubleNullSuffixLiteral() {
        // 字面值: "{\"pattern\": \"*.java\", \"directory\": \"/Users/yefei.yf/Jimi/tank\", \"includeDirs\": false}null"null
        String input = "\"{\\\"pattern\\\": \\\"*.java\\\", \\\"directory\\\": \\\"/Users/yefei.yf/Jimi/tank\\\", \\\"includeDirs\\\": false}null\"null";
        
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        
        System.out.println("======= 双重null后缀测试 =======");
        System.out.println("原始输入: " + input);
        System.out.println("处理结果: " + result);
        System.out.println("结果是否以{开头: " + result.startsWith("{"));
        System.out.println("结果是否以}结尾: " + result.endsWith("}"));
        System.out.println("=============================");
        
        // 验证不会被转换为数组
        assertFalse(result.startsWith("["), "不应该被转换为数组格式");
        
        // 验证结果是有效的JSON对象
        assertTrue(result.startsWith("{"), "应该是JSON对象格式");
        assertTrue(result.endsWith("}"), "应该以}结尾");
        
        // 验证可以被解析
        assertDoesNotThrow(() -> objectMapper.readTree(result));
        
        // 验证包含正确的字段
        assertTrue(result.contains("pattern"), "应该包含pattern字段");
        assertTrue(result.contains("*.java"), "应该包含*.java");
        assertTrue(result.contains("directory"), "应该包含directory字段");
        assertTrue(result.contains("includeDirs"), "应该包含includeDirs字段");
    }

    @Test
    @DisplayName("测试真实值场景 - 三层嵌套null后缀")
    void testRealValueTripleNestedNullSuffix() {
        // 这是真实的字符串值，不是Java字面值
        // 真实值: "{\"command\": \"mvn -version\", \"timeout\": 10}"null"null
        String input = "\"{\\\"command\\\": \\\"mvn -version\\\", \\\"timeout\\\": 10}\\\"null\"null";
        
        String result = ArgumentsNormalizer.normalizeToValidJson(input, objectMapper);
        
        System.out.println("======= 真实值三层嵌套null后缀测试 =======");
        System.out.println("原始输入: " + input);
        System.out.println("处理结果: " + result);
        System.out.println("结果是否以{开头: " + result.startsWith("{"));
        System.out.println("结果是否以[开头: " + result.startsWith("["));
        System.out.println("结果是否以}结尾: " + result.endsWith("}"));
        System.out.println("=============================");
        
        // 验证不会被转换为数组
        assertFalse(result.startsWith("["), "不应该被转换为数组格式");
        
        // 验证结果是有效的JSON对象
        assertTrue(result.startsWith("{"), "应该是JSON对象格式");
        assertTrue(result.endsWith("}"), "应该以}结尾");
        
        // 验证可以被解析
        assertDoesNotThrow(() -> objectMapper.readTree(result));
        
        // 验证包含正确的字段
        assertTrue(result.contains("command"), "应该包含command字段");
        assertTrue(result.contains("mvn -version"), "应该包含mvn -version");
        assertTrue(result.contains("timeout"), "应该包含timeout字段");
    }
}
