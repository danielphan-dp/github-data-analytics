package com.githubanalytics.bytecode;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;

import javax.swing.*;
import java.beans.MethodDescriptor;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodeTestMatcher {
    public static void main(String[] args) {
        // Parse repos for methods
        String sourceCodePath = "../Repos/gson";
        SourceCodeMethodExtractor sourceCodeMethodExtractor = new SourceCodeMethodExtractor();
        sourceCodeMethodExtractor.analyzeDirectoryForMethods(sourceCodePath);

        // Retrieve extracted methods
        List<Map<String, Object>> extractedMethods = sourceCodeMethodExtractor.getMethods();
        System.out.println("Number of methods: " + extractedMethods.size());

        // Split to source methods and test methods
        List<Map<String, Object>> sourceMethods = extractedMethods.stream()
                .filter(m -> !isTestMethod(m))
                .collect(Collectors.toList());
        System.out.println("Number of source methods: " + sourceMethods.size());

        List<Map<String, Object>> testMethods = extractedMethods.stream()
                .filter(CodeTestMatcher::isTestMethod)
                .collect(Collectors.toList());
        System.out.println("Number of test methods: " + testMethods.size());

        // Match code method to test methods
        matchMethods(sourceMethods, testMethods);
    }

    private static boolean isTestMethod(Map<String, Object> methodMap) {
        MethodIdentifier methodIdentifier = (MethodIdentifier) methodMap.get("methodIdentifier");
        String className = methodIdentifier.getClassName();
        String methodName = methodIdentifier.getMethodName();
        return className.contains("Test") && (methodName.contains("test") || methodName.contains("Test"));
    }

    private static void matchMethods(List<Map<String, Object>> sourceMethods, List<Map<String, Object>> testMethods) {
        int matchesCount = 0;
        List<Pair<Map<String, Object>, Map<String, Object>>> results = new ArrayList<>();

        // For each source method, find all test methods where it appears in.
        for (Map<String, Object> sourceMethod : sourceMethods) {
            MethodIdentifier sourceMethodIdentifier = (MethodIdentifier) sourceMethod.get("methodIdentifier");
            String sourceMethodClass = sourceMethodIdentifier.getClassName();
            String sourceMethodName = sourceMethodIdentifier.getMethodName();
            List<String> sourceMethodParameterTypes = sourceMethodIdentifier.getParameterTypes();
            String sourceMethodCode = (String) sourceMethod.get("sourceCode");

            for (Map<String, Object> testMethod : testMethods) {
                MethodIdentifier testMethodIdentifier = (MethodIdentifier) testMethod.get("methodIdentifier");
                String testMethodClass = testMethodIdentifier.getClassName();
                String testMethodName = testMethodIdentifier.getMethodName();
                List<String> testMethodParameterTypes = testMethodIdentifier.getParameterTypes();
                String testMethodCode = (String) testMethod.get("sourceCode");
                if (testMethodClass.contains(sourceMethodClass) && testMethodCode.contains(sourceMethodName)) {
                    // There can be overrides, check which version is matched.
                    // Count the number of params of the instance.
                    List<Map<String, Object>> calledExternalMethods = (ArrayList) testMethod.get("calledExternalMethods");
                    int paramsCount = -100;
                    for (Map<String, Object> calledExternalMethod : calledExternalMethods) {
                        if (calledExternalMethod.get("methodName").equals(sourceMethodName)) {
                            paramsCount = (int) calledExternalMethod.get("numParams");
                        }
                    }

                    if (paramsCount == sourceMethodParameterTypes.size()) {
                        System.out.println(
                                "Source method " +
                                        sourceMethodClass + "." + sourceMethodName + ": " + sourceMethodParameterTypes + " is tested in: " +
                                        testMethodClass + "." + testMethodName
                        );

                        // results.add(new Pair<>(sourceMethod, testMethod));
                        matchesCount += 1;
                    }
                }
            }
        }
        // System.out.println(results);
        System.out.println(matchesCount);
    }
}
