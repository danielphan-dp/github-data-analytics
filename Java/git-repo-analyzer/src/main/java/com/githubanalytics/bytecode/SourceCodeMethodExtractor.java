package com.githubanalytics.bytecode;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

public class SourceCodeMethodExtractor {
    private final List<Map<String, Object>> methods = new ArrayList<>();

    static {
        ParserConfiguration configuration = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(configuration);
    }

    public List<Map<String, Object>> getMethods() {
        return this.methods;
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
                    String className = getFullyQualifiedName(n).orElse("");
                    String methodName = n.getNameAsString();
                    String returnType = eraseGenerics(n.getType());

                    List<String> paramTypes = new ArrayList<>();
                    for (Parameter param : n.getParameters()) {
                        paramTypes.add(eraseGenerics(param.getType()));
                    }

                    String methodSource = n.toString();
                    Map<String, Object> methodMap = new HashMap<>();
                    methodMap.put("methodIdentifier", new MethodIdentifier(className, methodName, paramTypes, returnType));
                    methodMap.put("sourceCode", methodSource);
                    methods.add(methodMap);
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

    private Optional<String> getFullyQualifiedName(Node node) {
        if (node instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) node;
            String className = classDecl.getNameAsString();
            Node parentNode = classDecl.getParentNode().orElse(null);
            if (parentNode instanceof ClassOrInterfaceDeclaration) {
                className = getFullyQualifiedName(parentNode).orElse("") + "$" + className;
            } else {
                Optional<PackageDeclaration> pkgDecl = classDecl.findCompilationUnit().flatMap(CompilationUnit::getPackageDeclaration);
                if (pkgDecl.isPresent()) {
                    className = pkgDecl.get().getNameAsString() + "." + className;
                }
            }
            return Optional.of(className);
        } else if (node instanceof CompilationUnit) {
            return Optional.empty(); // Reached the top level, no class found
        } else if (node != null) {
            return getFullyQualifiedName(node.getParentNode().orElse(null));
        }
        return Optional.empty();
    }

    private String eraseGenerics(Type type) {
        if (type.isClassOrInterfaceType()) {
            // Only take the raw type of the ClassOrInterfaceType
            return type.asClassOrInterfaceType().getName().getIdentifier();
        } else {
            // For other types (like arrays, primitives), just convert them to string
            return type.toString();
        }
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
        long uniqueMethodCount = methods.stream().map(Map::values).distinct().count();
        System.out.println("Methods (total): " + totalMethodCount);
        System.out.println("Methods (unique): " + uniqueMethodCount);
    }

    public void printDuplicateMethods() {
        Set<MethodIdentifier> uniqueMethods = new HashSet<>();
        List<MethodIdentifier> duplicateMethods = new ArrayList<>();

        for (Map<String, Object> methodDetail : methods) {
            MethodIdentifier method = (MethodIdentifier) methodDetail.get("methodIdentifier");
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

        String sourceCodePath = args[0];
        String outputPath = args[1];

        SourceCodeMethodExtractor extractor = new SourceCodeMethodExtractor();
        extractor.analyzeDirectoryForMethods(sourceCodePath);
        extractor.exportMethodsToJson(outputPath);
        extractor.printAnalysisSummary();
        extractor.printDuplicateMethods();
    }
}
