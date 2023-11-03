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

        System.out.println("Samples in SC not in BC: " + matches.size());

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

        System.out.println("Samples in BC not in SC: " + matches.size());

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

        System.out.println("Match count: " + (matches.size()));

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
        // sourceCodeMethodExtractor.exportMethodsToJson(outputPath + "/methods_sc.json");

        // Analyze byte code
        BytecodeMethodExtractor bytecodeMethodExtractor = new BytecodeMethodExtractor();
        bytecodeMethodExtractor.analyzeDirectoryForMethods(bytecodePath);
        // bytecodeMethodExtractor.exportMethodsToJson(outputPath + "/methods_bc.json");

        // Get methods
        List<Map<String, Object>> scMethods = sourceCodeMethodExtractor.getMethods();
        List<Map<String, Object>> bcMethods = bytecodeMethodExtractor.getMethods();

        // DEBUG: The following lines can be used to get intermediate state of the outputs.
        // writeListMapToJsonFile(scMethods, outputPath + "/db_methods_sc.json");
        // writeListMapToJsonFile(bcMethods, outputPath + "/db_methods_bc.json");


        System.out.println("PRE CALCULATIONS");
        System.out.println(scMethods.size());
        System.out.println(bcMethods.size());


        System.out.println("\nPOST CALCULATIONS");

        // Compute matching
        List<Map<String, Object>> ij = inner_join(scMethods, bcMethods);
        List<Map<String, Object>> lj = left_join(scMethods, bcMethods);
        List<Map<String, Object>> rj = right_join(scMethods, bcMethods);
        
        writeListMapToJsonFile(ij, outputPath + "/mapped_methods.json");
        writeListMapToJsonFile(lj, outputPath + "/in_sc____notin_bc.json____LEFT_JOIN.json");
        writeListMapToJsonFile(rj, outputPath + "/notin_sc____in_bc.json____RIGHT_JOIN.json");

        System.out.println("Source code match rate: " + (ij.size() * 100 / scMethods.size()) + "%");

        // TODO: Check elements that are in check if there are matching in left_join and right_join sets.

        // Checks, if there are mismatches here, the equals operator is likely not correct.
        System.out.println("\nCHECKS");
        System.out.println(scMethods.size() + bcMethods.size());
        System.out.println(ij.size() * 2 + lj.size() + rj.size());
        System.out.println(scMethods.size() + bcMethods.size() == ij.size() * 2 + lj.size() + rj.size());
    }
}
