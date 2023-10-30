package com.githubanalytics;

import com.github.javaparser.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.githubanalytics.sourcecode_parsers.RepoCodeTestRetriever;

import java.io.File;
import java.nio.file.*;

public class Main {
    private static void configureJavaParser() {
        TypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(new File("C:\\Users\\Duy Phan\\OneDrive\\Research (AI&ML for Software)\\GitHub-Data-Analytics\\Data\\github-cloned-repos\\google_gson"))
        );

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        StaticJavaParser.setConfiguration(parserConfiguration);
    }

    public static void main(String[] args) {
        // Configure JavaParser with symbol resolution
        configureJavaParser();

        // Define the base paths
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path repoBaseDir = currentDir.resolve("Data/github-cloned-repos");
        Path outputBaseDir = currentDir.resolve("Data/code-test-pairs");

        System.out.println(repoBaseDir);

        // Specific file path for testing
        String repoPath = "C:\\Users\\Duy Phan\\OneDrive\\Research (AI&ML for Software)\\GitHub-Data-Analytics\\Data\\github-cloned-repos\\google_gson";

        try {
            RepoCodeTestRetriever retriever = new RepoCodeTestRetriever(repoPath);
            Path outputFilePath = outputBaseDir.resolve(new File(repoPath).getName() + ".json");
            retriever.saveTestMethodsToJSON(outputFilePath);
        } catch (Exception e) {
            System.err.println("Error processing file: " + repoPath);
            e.printStackTrace();
        }
    }
}
