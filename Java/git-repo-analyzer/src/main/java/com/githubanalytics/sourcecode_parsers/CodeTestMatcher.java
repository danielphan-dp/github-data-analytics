package com.githubanalytics.sourcecode_parsers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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

        for (String codeFile : codeMethods.keySet()) {
            Map<String, Map<String, String>> codeFileMethods = codeMethods.get(codeFile);
            Path codeFilePath = Paths.get(codeFile);

            for (String testFile : testMethods.keySet()) {
                Map<String, Map<String, String>> testFileMethods = testMethods.get(testFile);
                Path testFilePath = Paths.get(testFile);

                if (isMatchingPair(codeFilePath, testFilePath)) {
                    for (String codeMethodName : codeFileMethods.keySet()) {
                        Map<String, String> codeMethodDetails = codeFileMethods.get(codeMethodName);

                        String potentialTestMethodName = createPotentialTestMethodName(codeMethodName);

                        if (testFileMethods.containsKey(potentialTestMethodName)) {
                            Map<String, String> testMethodDetails = testFileMethods.get(potentialTestMethodName);
                            codeTestPairs.add(buildPair(codeFile, codeMethodDetails, testFile, testMethodDetails));
                        }
                    }
                }
            }
        }
        return codeTestPairs;
    }

    private boolean isMatchingPair(Path codeFilePath, Path testFilePath) {
        return codeFilePath.getParent().equals(testFilePath.getParent())
                && (testFilePath.getFileName().toString().startsWith(codeFilePath.getFileName().toString())
                && testFilePath.getFileName().toString().contains("Test"));
    }

    private String createPotentialTestMethodName(String codeMethodName) {
        return "test" + capitalizeFirstLetter(codeMethodName);
    }

    private Map<String, String> buildPair(String codeFile, Map<String, String> codeMethodDetails,
                                          String testFile, Map<String, String> testMethodDetails) {
        Map<String, String> pair = new HashMap<>();
        pair.put("src_file", shortenPath(codeFile));
        pair.put("src_code", codeMethodDetails.get("method_declaration"));
        pair.put("test_file", shortenPath(testFile));
        pair.put("test_code", testMethodDetails.get("method_declaration"));
        pair.put("src_class_name", codeMethodDetails.get("class_name"));
        pair.put("src_method_name", codeMethodDetails.get("method_name"));
        pair.put("src_method_signature", codeMethodDetails.get("method_signature"));
        return pair;
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public void saveCodeTestPairsToFile(List<Map<String, String>> codeTestPairs, Path filePath) throws IOException {
        String json = gson.toJson(codeTestPairs);
        Files.write(filePath, json.getBytes());
    }

    private String shortenPath(String fullPath) {
        String unixLikePath = fullPath.replace("\\", "/");
        int repoNameIndex = unixLikePath.indexOf("google_guava");
        return (repoNameIndex != -1) ? unixLikePath.substring(repoNameIndex) : unixLikePath;
    }

    public static void main(String[] args) {
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path repoDir = currentDir.resolve("../Repos/gson");
        CodeTestMatcher matcher = new CodeTestMatcher(repoDir);

        try {
            List<Map<String, String>> codeTestPairs = matcher.matchCodeToTests();
            Path outputPath = Paths.get("gson__code-test-pairs.json");
            matcher.saveCodeTestPairsToFile(codeTestPairs, outputPath);
            System.out.println("Results saved to " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
