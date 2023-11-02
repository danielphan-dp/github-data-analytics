package com.githubanalytics.bytecode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SourceCodeToBytecodeMapper {
    public void mapMethodsAndExportToJson(List<Map<String, Object>> sourceMethods, Map<MethodIdentifier, String> bytecodeMap, String outputPath) throws IOException {
        Map<MethodIdentifier, String> sourceCodeMap = new HashMap<>();

        // Extracting source code
        for (Map<String, Object> methodMap : sourceMethods) {
            MethodIdentifier id = (MethodIdentifier) methodMap.get("methodIdentifier");
            String sourceCode = (String) methodMap.get("sourceCode");
            sourceCodeMap.put(id, sourceCode);
        }

        // Sets for categorized methods
        Set<MethodIdentifier> exclusiveToBytecode = new HashSet<>(bytecodeMap.keySet());
        Set<MethodIdentifier> exclusiveToSource = new HashSet<>(sourceCodeMap.keySet());
        Set<MethodIdentifier> commonMethods = new HashSet<>();

        // Categorize methods
        for (MethodIdentifier id : sourceCodeMap.keySet()) {
            if (bytecodeMap.containsKey(id)) {
                commonMethods.add(id);
                exclusiveToBytecode.remove(id);
                exclusiveToSource.remove(id);
            }
        }

        // Creating JSON objects for each category
        List<Map<String, Object>> common = commonMethods.stream()
                .map(id -> createMethodMap(id, bytecodeMap.get(id), sourceCodeMap.get(id)))
                .collect(Collectors.toList());

        List<Map<String, Object>> bytecodeExclusive = exclusiveToBytecode.stream()
                .map(id -> createMethodMap(id, bytecodeMap.get(id), null))
                .collect(Collectors.toList());

        List<Map<String, Object>> sourceCodeExclusive = exclusiveToSource.stream()
                .map(id -> createMethodMap(id, null, sourceCodeMap.get(id)))
                .collect(Collectors.toList());

        // Structuring the final JSON output
        Map<String, List<Map<String, Object>>> categorizedMethods = new HashMap<>();
        categorizedMethods.put("common", common);
        categorizedMethods.put("bytecodeExclusive", bytecodeExclusive);
        categorizedMethods.put("sourceCodeExclusive", sourceCodeExclusive);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(categorizedMethods);

        Path filePath = Paths.get(outputPath, "methodMappings.json");
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(jsonOutput);
        }
    }

    private Map<String, Object> createMethodMap(MethodIdentifier id, String bytecode, String sourceCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("methodIdentifier", id);
        map.put("bytecode", bytecode != null ? bytecode : JsonNull.INSTANCE);
        map.put("sourceCode", sourceCode != null ? sourceCode : JsonNull.INSTANCE);
        return map;
    }




    public static void main(String[] args) throws IOException {
        String sourceCodePath = "../Repos/gson";
        String bytecodePath = "../Repos/gson";
        String outputPath = "./data";

        SourceCodeMethodExtractor sourceExtractor = new SourceCodeMethodExtractor();
        sourceExtractor.analyzeDirectoryForMethods(sourceCodePath);

        BytecodeMethodExtractor bytecodeExtractor = new BytecodeMethodExtractor();
        bytecodeExtractor.analyzeDirectoryForMethods(bytecodePath);

        SourceCodeToBytecodeMapper mapper = new SourceCodeToBytecodeMapper();

        mapper.mapMethodsAndExportToJson(sourceExtractor.getMethodsSource(), bytecodeExtractor.getMethodsBytecode(), outputPath);
    }
}
