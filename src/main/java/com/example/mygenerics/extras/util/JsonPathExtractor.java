package io.credable.reconapi.util.pathextractor;

import io.credable.reconapi.exception.CustomException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

public class JsonPathExtractor {

    //    public static void main(String[] args) {
//        JSONObject json = new JSONObject("[{\n" +
//                "  \"name\": \"John\",\n" +
//                "  \"age\": 30,\n" +
//                "  \"cars\": [\n" +
//                "    {\n" +
//                "      \"make\": \"Ford\",\n" +
//                "      \"model\": \"Mustang\"\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"make\": \"BMW\",\n" +
//                "      \"model\": \"M3\"\n" +
//                "    }\n" +
//                "  ],\n" +
//                "  \"address\": {\n" +
//                "    \"street\": \"123 Main St\",\n" +
//                "    \"city\": \"Anytown\",\n" +
//                "    \"state\": \"CA\",\n" +
//                "    \"zip\": \"12345\"\n" +
//                "  }\n" +
//                "}]");
//        Set<String> paths = extractJsonPaths(json);
//        log.info(paths);
//    }
    public static Set<String> extract(MultipartFile file) {
        try {
            String jsonString = new String(file.getBytes(), StandardCharsets.UTF_8);
            try {
                return extractJsonPaths(new JSONArray(jsonString));
            } catch (JSONException e) {
                return extractJsonPaths(new JSONObject(jsonString));

            }
        } catch (Exception e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Unable to extract json paths from given file.");
        }
    }

    public static Set<String> extractJsonPaths(Object obj) {
        Set<String> paths = new LinkedHashSet<>();
        if (obj instanceof JSONObject jsonObj) {
            for (String key : jsonObj.keySet()) {
                Object value = jsonObj.get(key);
                Set<String> childPaths = extractJsonPaths(value);
                key = "/" + key;
                for (String childPath : childPaths) {
                    paths.add(key + childPath);
                }
                paths.add(key);
            }
        } else if (obj instanceof JSONArray jsonArray) {
            for (int i = 0; i < jsonArray.length(); i++) {
                Object value = jsonArray.get(i);
                Set<String> childPaths = extractJsonPaths(value);
                paths.addAll(childPaths);
            }
        }
        return paths;
    }
}
