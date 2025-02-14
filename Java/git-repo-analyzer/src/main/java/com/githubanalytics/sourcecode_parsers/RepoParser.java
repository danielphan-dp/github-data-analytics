package com.githubanalytics.sourcecode_parsers;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class RepoParser {
    private final Path repoDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public RepoParser(Path repoDir) {
        this.repoDir = repoDir;
    }

    public Map<String, Map<String, Map<String, String>>> parseAllFiles() throws IOException {
        List<Path> javaFiles = getFilesMatchingRegex(".*\\.java$");
        return parseMethodsInFiles(javaFiles);
    }

    public Map<String, Map<String, Map<String, String>>> parseTestFiles() throws IOException {
        List<Path> javaTestFiles = getFilesMatchingRegex(".*\\.java$");
        return parseMethodsInFiles(javaTestFiles);
    }

    public Map<String, Map<String, Map<String, String>>> parseNonTestFiles() throws IOException {
        List<Path> javaNonTestFiles = getFilesMatchingRegex(".*\\.java$");
        javaNonTestFiles.removeIf(file -> file.toString().matches(".*Test.*\\.java$"));
        return parseMethodsInFiles(javaNonTestFiles);
    }

    public void saveParsingResult(Map<String, Map<String, String>> analysisResult, Path outputPath) throws IOException {
        Map<String, Map<String, String>> modifiedFileMethodsMap = new HashMap<>();
        analysisResult.forEach((filePath, methods) -> {
            Path relativePath = repoDir.relativize(Paths.get(filePath));
            modifiedFileMethodsMap.put("google_json" + "\\" + relativePath.toString(), methods);
        });

        String jsonOutput = gson.toJson(modifiedFileMethodsMap);
        Path outputDir = outputPath.getParent();
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            writer.write(jsonOutput);
        }
    }

    private List<Path> getFilesMatchingRegex(String regex) throws IOException {
        final List<Path> matchingFiles = new ArrayList<>();
        final Pattern pattern = Pattern.compile(regex);

        FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Matcher matcher = pattern.matcher(file.toString());
                if (matcher.find()) {
                    matchingFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree(repoDir, fileVisitor);

        return matchingFiles;
    }

    private Map<String, Map<String, Map<String, String>>> parseMethodsInFiles(List<Path> javaFiles) throws IOException {
        Map<String, Map<String, Map<String, String>>> fileMethodsMap = new HashMap<>();
        JavaParser javaParser = new JavaParser();

        for (Path javaFile : javaFiles) {
            Map<String, Map<String, String>> methodsMap = parseMethodsInFile(javaFile, javaParser);
            fileMethodsMap.put(javaFile.toString(), methodsMap);
        }

        return fileMethodsMap;
    }

    private Map<String, Map<String, String>> parseMethodsInFile(Path javaFile, JavaParser javaParser) throws IOException {
        // TODO: Add more logic here to capture more information.

        Map<String, Map<String, String>> methodsMap = new HashMap<>();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);

        if (parseResult.isSuccessful()) {
            CompilationUnit compilationUnit = parseResult.getResult().get();
            compilationUnit
                    .findAll(MethodDeclaration.class)
                    .stream()
                    .filter(method -> method.getAnnotationByName("Test").isPresent())
                    .forEach(method -> {
                        Map<String, String> methodDetails = new HashMap<>();
                        methodDetails.put("method_name", method.getNameAsString());
                        methodDetails.put("method_signature", method.getSignature().asString());
                        methodDetails.put("method_declaration", method.toString());

                        // If method is inside a class, capture class name
                        methodDetails.put("class_name", method.findAncestor(CompilationUnit.class).flatMap(CompilationUnit::getPrimaryTypeName).orElse("UnknownClass"));

                        methodsMap.put(method.getNameAsString(), methodDetails);
                    });
        } else {
            System.err.println("Error parsing " + repoDir + "/" + repoDir.relativize(javaFile) + ": " + parseResult.getProblems());
        }

        return methodsMap;
    }

    public static void main(String[] args) throws IOException {
        Path repoPath = Paths.get("Data/github-cloned-repos/google_guava");
        RepoParser repoParser = new RepoParser(repoPath);

        Map<String, Map<String, Map<String, String>>> allFilesData = repoParser.parseAllFiles();

        List<Map<String, String>> outputList = new ArrayList<>();

        allFilesData.forEach((filename, methodsData) -> {
            methodsData.forEach((methodName, methodDetails) -> {
                if (methodDetails.containsKey("method_name") && methodDetails.containsKey("class_name")) {
                    Map<String, String> outputFileData = new HashMap<>();
                    outputFileData.put("filename", filename);
                    outputFileData.put("test", methodName);
                    outputList.add(outputFileData);
                }
            });
        });

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(outputList);

        System.out.println(outputList.size());
        System.out.println(jsonOutput);
    }
}
