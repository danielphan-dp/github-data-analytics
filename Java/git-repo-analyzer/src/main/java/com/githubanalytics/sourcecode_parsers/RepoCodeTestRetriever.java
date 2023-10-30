package com.githubanalytics.sourcecode_parsers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RepoCodeTestRetriever {

    private final Path repoPath;

    public RepoCodeTestRetriever(String repoPathString) {
        this.repoPath = Paths.get(repoPathString);
    }

    private boolean isValidTest(MethodDeclaration method) {
        return method.getAnnotationByName("Test").isPresent()
                && !method.getNameAsString().matches("test\\d+")
                && method.getParameters().isEmpty()
                && method.getType().asString().equals("void");
    }

    private Set<MethodDeclaration> locateMethodsUnderTest(MethodDeclaration testMethod, List<MethodDeclaration> allMethods) {

//        System.out.println(testMethod);
//        System.out.println(allMethods);


        List<MethodDeclaration> orderedMethods = new ArrayList<>();

        Map<String, List<MethodDeclaration>> methodMap = new HashMap<>();
        for (MethodDeclaration md : allMethods) {
            methodMap
                    .computeIfAbsent(md.getNameAsString(), k -> new ArrayList<>())
                    .add(md);
        }

        testMethod
                .getBody()
                .ifPresent(body -> {
                    // Traverse all child nodes of the body, not just direct method calls
                    for (Node childNode : body.getChildNodes()) {
                        Queue<Node> nodes = new LinkedList<>();
                        nodes.add(childNode);

                        while (!nodes.isEmpty()) {
                            Node currentNode = nodes.poll();
                            if (currentNode instanceof MethodCallExpr) {
                                MethodCallExpr methodCall = (MethodCallExpr) currentNode;
                                String methodName = methodCall.getNameAsString();

                                if (methodMap.containsKey(methodName)) {
                                    for (MethodDeclaration methodDecl : methodMap.get(methodName)) {
                                        if (!orderedMethods.contains(methodDecl)) {
                                            orderedMethods.add(methodDecl);
                                        }
                                    }
                                }
                            }
                            // Add all child nodes of the current node to the queue
                            nodes.addAll(currentNode.getChildNodes());
                        }
                    }
                });

        return new LinkedHashSet<>(orderedMethods);
    }

    private void processFilesInRepository(FileProcessor processor) throws IOException {
        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            processor.process(path);
                        } catch (Exception e) {
                            System.err.println("Error processing file: " + path);
                            e.printStackTrace();
                            // Continue with the next file
                        }
                    });
        }
    }

    private CompilationUnit parseCompilationUnit(Path path) {
        try {
            return new JavaParser().parse(path).getResult().orElse(null);
        } catch (Exception | StackOverflowError e) {  // Catching StackOverflowError is generally not recommended
            System.err.println("Error or StackOverflowError parsing file: " + path);
            e.printStackTrace();
            return null;
        }
    }

    public int countValidTestMethods() throws IOException {
        AtomicInteger count = new AtomicInteger();
        processFilesInRepository(path -> {
            CompilationUnit cu = parseCompilationUnit(path);
            if (cu != null) {
                cu.findAll(MethodDeclaration.class).stream()
                        .filter(this::isValidTest)
                        .forEach(method -> count.getAndIncrement());
            }
        });
        return count.get();
    }

    public void saveTestMethodsToJSON(Path outputPath) throws IOException {
        JsonObject rootObject = new JsonObject();
        JsonArray filesArray = new JsonArray();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        rootObject.addProperty("repositoryPath", repoPath.toString());

        processFilesInRepository(path -> {
            CompilationUnit cu = parseCompilationUnit(path);
            if (cu != null) {
                JsonObject fileDetails = createFileTestDetails(cu, path);
                if (fileDetails != null) {
                    filesArray.add(fileDetails);
                }
            }
        });

        rootObject.add("files", filesArray);

        try (FileWriter file = new FileWriter(outputPath.toFile())) {
            file.write(gson.toJson(rootObject));
            System.out.println("Successfully saved test methods to " + outputPath);
        }
    }

    private JsonObject createFileTestDetails(CompilationUnit compilationUnit, Path path) {
        JsonObject fileObject = new JsonObject();
        JsonArray testMethodsArray = new JsonArray();
        fileObject.addProperty("fileName", path.getFileName().toString());

        compilationUnit
                .findAll(MethodDeclaration.class, Node.TreeTraversal.BREADTHFIRST)
                .forEach(method -> {
                    if (isValidTest(method)) {
                        JsonObject testMethodObject = new JsonObject();
                        testMethodObject.addProperty("testMethodName", method.getNameAsString());

                        Set<MethodDeclaration> methodsUnderTest = locateMethodsUnderTest(
                                method,
                                compilationUnit
                                        .findAll(MethodDeclaration.class)
                        );

                        JsonArray methodsUnderTestArray = createMethodsUnderTestArray(methodsUnderTest);

                        if (!methodsUnderTestArray.isEmpty()) {
                            testMethodObject.add("methodsUnderTest", methodsUnderTestArray);
                        } else {
                            testMethodObject.addProperty("methodUnderTest", "Not Found");
                        }
                        testMethodsArray.add(testMethodObject);
                    }
                });

        if (!testMethodsArray.isEmpty()) {
            fileObject.add("testMethods", testMethodsArray);
            return fileObject;
        }
        return null;
    }

    private JsonArray createMethodsUnderTestArray(Set<MethodDeclaration> methodsUnderTest) {
        JsonArray methodsArray = new JsonArray();
        methodsUnderTest.forEach(md -> {
            JsonObject methodObject = new JsonObject();
            methodObject.addProperty("methodName", md.getNameAsString());
            methodObject.addProperty("methodCode", md.toString());
            methodsArray.add(methodObject);
        });
        return methodsArray;
    }

    @FunctionalInterface
    private interface FileProcessor {
        void process(Path path);
    }
}
