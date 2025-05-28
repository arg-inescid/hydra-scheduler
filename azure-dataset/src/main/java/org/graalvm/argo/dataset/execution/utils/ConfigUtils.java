package org.graalvm.argo.dataset.execution.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigUtils {

    private static final String ENV_REGEX = "\\{\\{([A-Za-z_]+)\\}\\}";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Map<String, Benchmark> getBenchmarksConfig(String benchmarksFilename) {
        try (InputStream benchmarksFileInputStream = ConfigUtils.class.getClassLoader().getResourceAsStream(benchmarksFilename)) {
            String json = ConfigUtils.readAndReplaceEnv(benchmarksFileInputStream);
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Benchmark.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readAndReplaceEnv(InputStream inputStream) {
        // Write input stream to string.
        String json = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));

        // Regex to match {{ENV_VAR}}.
        Pattern pattern = Pattern.compile(ENV_REGEX);
        Matcher matcher = pattern.matcher(json);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String envVar = matcher.group(1);
            String envValue = System.getenv(envVar);
            matcher.appendReplacement(result, envValue != null ? envValue : "");
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
