package com.githubanalytics.bytecode_parsers.callgraph;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

public class SourceCodeMethodExtractor {
    private final List<MethodIdentifier> methods = new ArrayList<>();
    private final Set<String> classNames = new HashSet<>();

    static {
        // Configure JavaParser to use Java 17 (Working for google/gson)
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(configuration);
    }

    public void analyzeDirectoryForMethods(String rootDir) {
        processDirectory(new File(rootDir));
    }

    private void processDirectory(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        processDirectory(file);
                    } else if (file.getName().endsWith(".java")) {
                        processJavaFile(file);
                    }
                }
            }
        }
    }

    private void processJavaFile(File file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    super.visit(n, arg);
                    String className = getClassName(n);
                    String methodName = n.getNameAsString();
                    String returnType = n.getType().asString();
                    List<String> paramTypes = new ArrayList<>();
                    for (Parameter param : n.getParameters()) {
                        paramTypes.add(param.getType().asString());
                    }
                    MethodIdentifier methodId = new MethodIdentifier(className, methodName, paramTypes, returnType);
                    methods.add(methodId);
                    classNames.add(className);
                }
            }, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getClassName(Node node) {
        if (node instanceof ClassOrInterfaceDeclaration) {
            return ((ClassOrInterfaceDeclaration) node).getFullyQualifiedName().orElse("");
        } else if (node.getParentNode().isPresent()) {
            return getClassName(node.getParentNode().get());
        }
        return "";
    }

    public void exportMethodsToJson(String filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filename)) {
            gson.toJson(methods, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printAnalysisSummary() {
        long totalMethodCount = methods.size();
        long uniqueMethodCount = methods.stream().distinct().count();
        long duplicateMethodCount = totalMethodCount - uniqueMethodCount;
        long totalParamCount = methods.stream().mapToInt(method -> method.getParameterTypes().size()).sum();
        double avgParamPerMethod = methods.isEmpty() ? 0 : (double) totalParamCount / totalMethodCount;
        double avgMethodsPerClass = classNames.isEmpty() ? 0 : (double) totalMethodCount / classNames.size();

        System.out.println("Methods (total): " + totalMethodCount);
        System.out.println("Methods (unique): " + uniqueMethodCount);
        System.out.println("Methods (duplicated): " + duplicateMethodCount);
        System.out.println("Classes processed: " + classNames.size());
        System.out.println("Average parameters per method: " + avgParamPerMethod);
        System.out.println("Average methods per class: " + avgMethodsPerClass);
    }

    public void printDuplicateMethods() {
        Set<MethodIdentifier> uniqueMethods = new HashSet<>();
        List<MethodIdentifier> duplicateMethods = new ArrayList<>();

        for (MethodIdentifier method : methods) {
            if (!uniqueMethods.add(method)) {
                duplicateMethods.add(method);
            }
        }

        if (duplicateMethods.isEmpty()) {
            System.out.println("No duplicates found.");
        } else {
            System.out.println("Duplicate methods found:");
            for (MethodIdentifier duplicate : duplicateMethods) {
                System.out.println(duplicate);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java SourceCodeMethodExtractor <root directory of Java files> <output JSON file>");
            System.exit(1);
        }

        SourceCodeMethodExtractor extractor = new SourceCodeMethodExtractor();
        extractor.analyzeDirectoryForMethods(args[0]);
        extractor.exportMethodsToJson(args[1]);
        extractor.printAnalysisSummary();
        extractor.printDuplicateMethods();
    }
}
