package com.githubanalytics.bytecode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.checkerframework.checker.units.qual.A;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class SourceCodeToBytecodeMapper {
    public static List<Map<String, Object>> left_join(List<Map<String, Object>> scMethods, List<Map<String, Object>> bcMethods) {
        // TODO: Optimize this.

        // Methods in sc but not in bc.

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> scm : scMethods) {
            // Check for not in list.
            boolean notInBc = true;

            for (Map<String, Object> bcm : bcMethods) {
                MethodIdentifier scmId = (MethodIdentifier) scm.get("methodIdentifier");
                MethodIdentifier bcmId = (MethodIdentifier) bcm.get("methodIdentifier");
                if (scmId.equals(bcmId)) {
                    notInBc = false;
                    break;
                }
            }

            if (notInBc) {
                matches.add(scm);
            }
        }

        System.out.println(matches.size());

        return matches;
    }

    public static List<Map<String, Object>> right_join(List<Map<String, Object>> scMethods, List<Map<String, Object>> bcMethods) {
        // TODO: Optimize this.

        // Methods in bc but not in sc.

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> bcm : bcMethods) {
            // Check for not in list.
            boolean notInSc = true;

            for (Map<String, Object> scm : scMethods) {
                MethodIdentifier scmId = (MethodIdentifier) scm.get("methodIdentifier");
                MethodIdentifier bcmId = (MethodIdentifier) bcm.get("methodIdentifier");
                if (scmId.equals(bcmId)) {
                    notInSc = false;
                    break;
                }
            }

            if (notInSc) {
                matches.add(bcm);
            }
        }

        System.out.println(matches.size());

        return matches;
    }

    public static List<Map<String, Object>> inner_join(List<Map<String, Object>> scMethods, List<Map<String, Object>> bcMethods) {
        // TODO: Optimize this.

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> scm : scMethods) {
            for (Map<String, Object> bcm : bcMethods) {
                MethodIdentifier scmId = (MethodIdentifier) scm.get("methodIdentifier");
                MethodIdentifier bcmId = (MethodIdentifier) bcm.get("methodIdentifier");
                if (scmId.equals(bcmId)) {
                    // Create a new entry.
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("methodIdentifier", bcmId);
                    entry.put("sourceCode", scm.get("sourceCode"));
                    entry.put("bytecode", bcm.get("bytecode"));

                    // Add the entry to result.
                    matches.add(entry);

                    // Break (does not need this is keys are unique).
                    break;
                }
            }
        }

        System.out.println(matches.size());

        return matches;
    }
    public static void writeListMapToJsonFile(List<Map<String, Object>> list, String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String sourceCodePath = "../Repos/gson";
        String bytecodePath = "../Repos/gson";
        String outputPath = "./data";

        // Analyze source code
        SourceCodeMethodExtractor sourceCodeMethodExtractor = new SourceCodeMethodExtractor();
        sourceCodeMethodExtractor.analyzeDirectoryForMethods(sourceCodePath);
//        sourceCodeMethodExtractor.exportMethodsToJson(outputPath + "/methods_sc.json");

        // Analyze byte code
        BytecodeMethodExtractor bytecodeMethodExtractor = new BytecodeMethodExtractor();
        bytecodeMethodExtractor.analyzeDirectoryForMethods(bytecodePath);
//        bytecodeMethodExtractor.exportMethodsToJson(outputPath + "/methods_bc.json");

        // Get methods
        List<Map<String, Object>> scMethods = sourceCodeMethodExtractor.getMethods();
        List<Map<String, Object>> bcMethods = bytecodeMethodExtractor.getMethods();

        // DEBUG: The following lines can be used to get intermediate state of the outputs.
        // writeListMapToJsonFile(scMethods, outputPath + "/db_methods_sc.json");
        // writeListMapToJsonFile(bcMethods, outputPath + "/db_methods_bc.json");

        // Compute matching
        writeListMapToJsonFile(inner_join(scMethods, bcMethods), outputPath + "/mapped_methods.json");
        writeListMapToJsonFile(left_join(scMethods, bcMethods), outputPath + "/in_sc____not_in_bc.json____LEFT_JOIN.json");
        writeListMapToJsonFile(right_join(scMethods, bcMethods), outputPath + "/notin_sc____in_bc.json____RIGHT_JOIN.json");
    }
}
