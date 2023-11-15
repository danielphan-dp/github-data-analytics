package com.githubanalytics.bytecode;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.*;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

public class SourceCodeMethodExtractor {
    private final List<Map<String, Object>> methods = new ArrayList<>();

    public SourceCodeMethodExtractor() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());

        // Add a JavaParserTypeSolver if you have the source code of the libraries you use
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("../Repos/gson")));

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver))
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        StaticJavaParser.setConfiguration(parserConfiguration);
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
            cu.removeComment();
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    // When resolving types, if error occurred when processing an entry. Simply log it out.
                    try {
                        n.removeComment();
                        super.visit(n, arg);

                        // Retrieve class name.
                        String className = n.findAncestor(ClassOrInterfaceDeclaration.class)
                                .flatMap(node -> getFullyQualifiedName(node))
                                .orElse("");

                        // Retrieve method name.
                        String methodName = n.getNameAsString();

                        // Retrieve return type.
                        String returnType;
                        try {
                            returnType = getQualifiedName(n.getType());
                        } catch (UnsolvedSymbolException | IllegalArgumentException e) {
                            System.err.println("Failed to resolve return type for method " + methodName + ", using raw type.");
                            returnType = n.getType().asString();
                        }

                        // Retrieve parameter types.
                        List<String> paramTypes = new ArrayList<>();
                        for (Parameter param : n.getParameters()) {
                            try {
                                paramTypes.add(getQualifiedName(param.getType()));
                            } catch (UnsolvedSymbolException | IllegalArgumentException e) {
                                System.err.println("Failed to resolve type for parameter " + param.getName() + " in method " + methodName + ", using raw type.");
                                paramTypes.add(param.getType().asString()); // Use the raw type as a fallback.
                            }
                        }

                        // Build the entry.
                        MethodIdentifier methodIdentifier = new MethodIdentifier(className, methodName, paramTypes, returnType);
                        String sourceCode = n.toString(new PrettyPrinterConfiguration().setPrintComments(false));
                        Map<String, Object> methodMap = new HashMap<>();
                        methodMap.put("methodIdentifier", methodIdentifier);
                        methodMap.put("sourceCode", sourceCode);

                        // Add the entry to the collection.
                        methods.add(methodMap);

                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping method due to exception: " + e.getMessage());
                    }
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

    private Optional<String> getFullyQualifiedName(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getFullyQualifiedName(); // This method already returns Optional<String>
    }

    private String getQualifiedName(Type type) {
        try {
            return type.resolve().asReferenceType().getQualifiedName();
        } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
            return type.toString();
        }
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
