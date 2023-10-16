package com.githubanalytics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import java.util.List;
import java.util.ArrayList;

public class CodeTestMatcher {
    private final RepoParser repoParser;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public CodeTestMatcher(Path repoDir) {
        this.repoParser = new RepoParser(repoDir);
    }

    public List<Map<String, String>> matchCodeToTests() throws IOException {
        Map<String, Map<String, Map<String, String>>> codeMethods = repoParser.parseNonTestFiles();
        Map<String, Map<String, Map<String, String>>> testMethods = repoParser.parseTestFiles();

        List<Map<String, String>> codeTestPairs = new ArrayList<>();

        // Iterate through all code methods
        for (String codeFile : codeMethods.keySet()) {
            Map<String, Map<String, String>> codeFileMethods = codeMethods.get(codeFile);

            for (Map.Entry<String, Map<String, String>> codeMethodEntry : codeFileMethods.entrySet()) {
                String codeMethodName = codeMethodEntry.getKey();
                Map<String, String> codeMethodDetails = codeMethodEntry.getValue();
                String codeMethodDeclaration = codeMethodDetails.get("method_declaration");

                // Identify potential test method names
                String potentialTestMethodName = "test" + capitalizeFirstLetter(codeMethodName);

                for (String testFile : testMethods.keySet()) {
                    Map<String, Map<String, String>> testFileMethods = testMethods.get(testFile);

                    if (testFileMethods.containsKey(potentialTestMethodName)) {
                        Map<String, String> testMethodDetails = testFileMethods.get(potentialTestMethodName);
                        String testMethodDeclaration = testMethodDetails.get("method_declaration");

                        Map<String, String> pair = new HashMap<>();
                        pair.put("src_file", shortenPath(codeFile));
                        pair.put("src_code", codeMethodDeclaration);
                        pair.put("test_file", shortenPath(testFile));
                        pair.put("test_code", testMethodDeclaration);

                        // Additional details as per feedback
                        pair.put("src_class_name", codeMethodDetails.get("class_name"));
                        pair.put("src_method_name", codeMethodDetails.get("method_name"));
                        pair.put("src_method_signature", codeMethodDetails.get("method_signature"));

                        codeTestPairs.add(pair);
                    }
                }
            }
        }

        return codeTestPairs;
    }

    private String capitalizeFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public void saveCodeTestPairsToFile(List<Map<String, String>> codeTestPairs, Path filePath) throws IOException {
        // Convert the map to a JSON string
        String json = gson.toJson(codeTestPairs);

        // Write the JSON string to the file
        Files.write(filePath, json.getBytes());
    }

    private String shortenPath(String fullPath) {
        String unixLikePath = fullPath.replace("\\", "/");
        int repoNameIndex = unixLikePath.indexOf("google_guava");

        // Extracting the relevant path portion
        String relevantPath = (repoNameIndex != -1)
                ? unixLikePath.substring(repoNameIndex)
                : unixLikePath;

        return relevantPath;
    }

    public static void main(String[] args) {
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path repoDir = currentDir.resolve("Data/github-cloned-repos/google_guava");
        CodeTestMatcher matcher = new CodeTestMatcher(repoDir);

        try {
            List<Map<String, String>> codeTestPairs = matcher.matchCodeToTests();

            // Save to file
            Path outputPath = Paths.get("google_guava-code-test-pairs.json");
            matcher.saveCodeTestPairsToFile(codeTestPairs, outputPath);
            System.out.println("Results saved to " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
