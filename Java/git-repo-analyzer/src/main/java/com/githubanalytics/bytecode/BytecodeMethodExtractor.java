package com.githubanalytics.bytecode;

import org.objectweb.asm.*;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BytecodeMethodExtractor {
    private final List<MethodIdentifier> methods = new ArrayList<>();
    private final Set<String> classNames = new HashSet<>();
    private final Map<MethodIdentifier, String> methodBytecodes = new HashMap<>();

    public Map<MethodIdentifier, String> getMethodsBytecode() {
        return this.methodBytecodes;
    }

    private class CustomClassVisitor extends ClassVisitor {
        private final String className;

        CustomClassVisitor(String className) {
            super(Opcodes.ASM9);
            this.className = className;
            classNames.add(className);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodIdentifier methodId = new MethodIdentifier(
                    className, name, convertTypesToStringList(Type.getArgumentTypes(descriptor)),
                    Type.getReturnType(descriptor).getClassName()
            );
            methods.add(methodId);
            return new CustomMethodVisitor(methodId, super.visitMethod(access, name, descriptor, signature, exceptions));
        }
    }

    private class CustomMethodVisitor extends MethodVisitor {
        private final MethodIdentifier methodId;
        private final StringBuilder bytecode = new StringBuilder();

        public CustomMethodVisitor(MethodIdentifier methodId, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodId = methodId;
        }

        @Override
        public void visitInsn(int opcode) {
            bytecode.append(opcode).append("\n");
            super.visitInsn(opcode);
        }

        @Override
        public void visitEnd() {
            methodBytecodes.put(methodId, bytecode.toString());
            super.visitEnd();
        }
    }

    private List<String> convertTypesToStringList(Type[] types) {
        List<String> typeNames = new ArrayList<>();
        for (Type type : types) {
            typeNames.add(type.getClassName());
        }
        return typeNames;
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
                    } else if (file.getName().endsWith(".class")) {
                        processClassFile(file);
                    }
                }
            }
        }
    }

    private void processClassFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            ClassReader classReader = new ClassReader(fis);
            String className = classReader.getClassName().replace('/', '.');
            classReader.accept(new CustomClassVisitor(className), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportMethodsToJson(String filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filename)) {
            List<Map<String, Object>> methodInfo = new ArrayList<>();
            for (MethodIdentifier methodId : methods) {
                Map<String, Object> methodDetails = new HashMap<>();
                methodDetails.put("className", methodId.getClassName());
                methodDetails.put("methodName", methodId.getMethodName());
                methodDetails.put("parameterTypes", methodId.getParameterTypes());
                methodDetails.put("returnType", methodId.getReturnType());

                Map<String, Object> methodEntry = new HashMap<>();
                methodEntry.put("methodIdentifier", methodDetails);
                methodEntry.put("bytecode", methodBytecodes.get(methodId));  // Moved bytecode outside methodIdentifier
                methodInfo.add(methodEntry);
            }
            gson.toJson(methodInfo, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void printAnalysisSummary() {
        long totalMethodCount = methods.size();
        long uniqueMethodCount = methods.stream().distinct().count();
        long duplicateMethodCount = totalMethodCount - uniqueMethodCount;

        System.out.println("Methods (total): " + totalMethodCount);
        System.out.println("Methods (unique): " + uniqueMethodCount);
        System.out.println("Methods (duplicated): " + duplicateMethodCount);
        System.out.println("Classes processed: " + classNames.size());
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
            System.err.println("Usage: java BytecodeMethodExtractor <root directory of class files> <output JSON file>");
            System.exit(1);
        }
        BytecodeMethodExtractor extractor = new BytecodeMethodExtractor();
        extractor.analyzeDirectoryForMethods(args[0]);
        extractor.exportMethodsToJson(args[1]);
        extractor.printAnalysisSummary();
        extractor.printDuplicateMethods();
    }
}
