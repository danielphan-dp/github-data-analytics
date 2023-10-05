package com.githubanalytics;

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

    public Map<String, List<String>> parseAllFiles() throws IOException {
        List<Path> javaFiles = getFilesMatchingRegex(".*\\.java$");
        return parseMethodsInFiles(javaFiles);
    }

    public Map<String, List<String>> parseTestFiles() throws IOException {
        List<Path> javaTestFiles = getFilesMatchingRegex(".*\\.java$");
        return null;
    }

    public Map<String, List<String>> parseNonTestFiles() throws IOException {
        List<Path> javaNonTestFiles = getFilesMatchingRegex(".*\\.java$");
        return null;
    }

    public void saveParsingResult(Map<String, List<String>> analysisResult, Path outputPath) throws IOException {
        Map<String, List<String>> modifiedFileMethodsMap = new HashMap<>();
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

    private Map<String, List<String>> parseMethodsInFiles(List<Path> javaFiles) throws IOException {
        Map<String, List<String>> fileMethodsMap = new HashMap<>();
        JavaParser javaParser = new JavaParser();

        for (Path javaFile : javaFiles) {
            List<String> methodsList = parseMethodsInFile(javaFile, javaParser);
            fileMethodsMap.put(javaFile.toString(), methodsList);
        }

        return fileMethodsMap;
    }

    private List<String> parseMethodsInFile(Path javaFile, JavaParser javaParser) throws IOException {
        List<String> methodsList = new ArrayList<>();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);

        if (parseResult.isSuccessful()) {
            CompilationUnit cu = parseResult.getResult().get();
            cu.findAll(MethodDeclaration.class).forEach(method -> methodsList.add(method.getNameAsString()));
        } else {
            System.err.println("Error parsing " + repoDir.relativize(javaFile) + ": " + parseResult.getProblems());
        }

        return methodsList;
    }
}
