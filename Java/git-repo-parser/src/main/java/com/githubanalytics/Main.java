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
            Map<String, List<String>> parsingResult = repoParser.parseRepo();
            Path outputPath = currentDir.resolve("Data/repos-parsed-code/google_gson/all_methods.json");
            repoParser.saveParsingResult(parsingResult, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}