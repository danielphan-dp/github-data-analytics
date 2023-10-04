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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Create path to the sample dir, and make sure that it is correct.
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path repoDir = currentDir.resolve("Data/github-cloned-repos/google_gson");
        if (Files.exists(repoDir)) {
            System.out.println("Repo google_gson exists, ready for processing.");
        } else {
            System.out.println("Please specify correct path to the Git repo.");
            return;
        }

        final List<Path> javaFiles = new ArrayList<>();

        // Initialize a file Visitor.
        FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // TODO: More parsing logic can be added here.
                if (file.toString().endsWith(".java")) {
                    javaFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        };

        // Traverse the repo.
        try {
            Files.walkFileTree(repoDir, fileVisitor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Print out some information.
        System.out.println("Java Files Count: " + javaFiles.size());

        // At this point, we already have list of all Java files. Scan for the functions list in each file and store
        // them in hash map.
        Map<String, List<String>> fileMethodsMap = new HashMap<>();
        for (Path javaFile : javaFiles) {
            JavaParser javaParser = new JavaParser();

            ParseResult<CompilationUnit> parseResult = null;
            try {
                parseResult = javaParser.parse(javaFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (parseResult.isSuccessful()) {
                CompilationUnit cu = parseResult.getResult().get();

                // List to store methods of the current file.
                List<String> methodsList = new ArrayList<>();

                // Visit and print the methods names.
                // TODO: More parsing/filtering logic can be added here.
                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    methodsList.add(method.getNameAsString());
                });

                // Add to the map.
                fileMethodsMap.put(javaFile.toString(), methodsList);
            } else {
                System.err.println("Error parsing " + repoDir.relativize(javaFile) + ": " + parseResult.getProblems());
            }
        }

        // At this point, we have
        System.out.println(fileMethodsMap.size());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, List<String>> modifiedFileMethodsMap = new HashMap<>();
        fileMethodsMap.forEach((filePath, methods) -> {
            Path relativePath = repoDir.relativize(Paths.get(filePath));
            modifiedFileMethodsMap.put("google_json" + "\\" + relativePath.toString(), methods);
        });

        String jsonOutput = gson.toJson(modifiedFileMethodsMap);
        Path outputFileDir = currentDir.resolve("Data/repos-parsed-code/google_gson");
        Path outputFileName = Paths.get("all_methods.json");

        // Create the path if there is no such path.
        try {
            if (!Files.exists(outputFileDir)) {
                Files.createDirectories(outputFileDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (FileWriter writer = new FileWriter(outputFileDir + "/" + outputFileName)) {
            writer.write(jsonOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}