import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class JsonFileToMap {

    public static Map<String, Object> parseJsonFileToMap(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = null;

        try {
            // Specify the TypeReference for proper type safety
            map = objectMapper.readValue(new File(filePath), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            System.err.println("Error reading the JSON file: " + e.getMessage());
            e.printStackTrace();
        }

        return map;
    }

}
