import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

public class DebugTest {
    public static void main(String[] args) throws Exception {
        ObjectMapper om = new ObjectMapper();
        String input = "\"{\\\"command\\\": \\\"mvn -version\\\", \\\"timeout\\\": 10}\\\"null\\\"null";
        
        System.out.println("Input: [" + input + "]");
        System.out.println("Length: " + input.length());
        
        try {
            JsonParser parser = om.getFactory().createParser(input);
            JsonNode node = om.readTree(parser);
            System.out.println("Parsed successfully: " + node);
            System.out.println("Next token: " + parser.nextToken());
            
            if (node.isTextual()) {
                String textValue = node.asText();
                System.out.println("Text value: [" + textValue + "]");
                System.out.println("Text value starts with {: " + textValue.trim().startsWith("{"));
                System.out.println("Text value starts with [: " + textValue.trim().startsWith("["));
            }
        } catch (Exception e) {
            System.out.println("Parse failed: " + e.getMessage());
        }
    }
}
