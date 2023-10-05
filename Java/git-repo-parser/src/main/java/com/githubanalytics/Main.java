package com.githubanalytics;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // TODO: Generalize this logic to work with all repos in given directory (might need performance check).

        // Set up
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path repoDir = currentDir.resolve("Data/github-cloned-repos/google_gson");
        if (!java.nio.file.Files.exists(repoDir)) {
            System.out.println("Please specify correct path to the Git repo.");
            return;
        }

        // Parse the repo
        RepoParser repoParser = new RepoParser(repoDir);
        try {
            String targetDir = "Data/repos-parsed-code/google_gson/";
            Path outputPath = null;

            // TODO: Add functionalities to handle paths.
            Map<String, Map<String, String>> parsingAllFilesResult = repoParser.parseAllFiles();
            outputPath = currentDir.resolve(targetDir + "all_files_methods.json");
            repoParser.saveParsingResult(parsingAllFilesResult, outputPath);

            Map<String, Map<String, String>> parsingTestFilesResult = repoParser.parseTestFiles();
            outputPath = currentDir.resolve(targetDir + "test_files_methods.json");
            repoParser.saveParsingResult(parsingTestFilesResult, outputPath);

            Map<String, Map<String, String>> parsingNonTestFilesResult = repoParser.parseNonTestFiles();
            outputPath = currentDir.resolve(targetDir + "non_test_files_methods.json");
            repoParser.saveParsingResult(parsingNonTestFilesResult, outputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}