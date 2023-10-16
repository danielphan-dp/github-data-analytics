package com.githubanalytics;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;

public class Main {
    private static List<String> listDir(String dir) {
        File folder = new File(dir);
        List<String> dirList = new ArrayList<>();
        if (folder.isDirectory()) {
            for (File file : Objects.requireNonNull(
                    folder.listFiles(file -> file.isDirectory() && !file.isHidden()))
            ) {
                if (file.isDirectory()) {
                    dirList.add(file.getName());
                }
            }
        }
        return dirList;
    }

    public static void main(String[] args) {
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path repoDir = currentDir.resolve("Data/github-cloned-repos");
        List<String> repos = listDir(String.valueOf(repoDir));

        repos = Arrays.asList("google_guava");

        // At the moment, temporarily filter out repos causing errors by the JavaParser lib.
        repos.removeIf(repo -> repo.equals("checkstyle_checkstyle"));

        System.out.println(repos.size());
        System.out.println(repos);

//        for (String repo : repos) {
//            Path repoPath = currentDir.resolve("Data/github-cloned-repos/" + repo);
//            RepoParser repoParser = new RepoParser(repoPath);
//            try {
//                String outputDir = "Data/repos-parsed-code/" + repo + "/";
//                Path outputPath = null;
//                Map<String, Map<String, String>> parsingResult = null;
//
//                parsingResult = repoParser.parseAllFiles();
//                outputPath = currentDir.resolve(outputDir + "all_files_methods.json");
//                repoParser.saveParsingResult(parsingResult, outputPath);
//
//                parsingResult = repoParser.parseTestFiles();
//                outputPath = currentDir.resolve(outputDir + "test_files_methods.json");
//                repoParser.saveParsingResult(parsingResult, outputPath);
//
//                parsingResult = repoParser.parseNonTestFiles();
//                outputPath = currentDir.resolve(outputDir + "non_test_files_methods.json");
//                repoParser.saveParsingResult(parsingResult, outputPath);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }
}