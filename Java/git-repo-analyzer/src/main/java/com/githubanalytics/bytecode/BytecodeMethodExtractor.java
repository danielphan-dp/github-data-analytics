package com.githubanalytics.bytecode;

import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BytecodeMethodExtractor {
    private final List<Map<String, Object>> methods = new ArrayList<>();

    private class CustomClassVisitor extends ClassVisitor {
        private final String className;

        CustomClassVisitor(String className) {
            super(Opcodes.ASM9);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            Map<String, Object> methodMap = new HashMap<>();
            methodMap.put("methodIdentifier", new MethodIdentifier(
                    className, name, convertTypesToStringList(Type.getArgumentTypes(descriptor)),
                    Type.getReturnType(descriptor).getClassName()
            ));
            methods.add(methodMap);
            return new CustomMethodVisitor(methodMap, super.visitMethod(access, name, descriptor, signature, exceptions));
        }
    }

    public List<Map<String, Object>> getMethods() {
        return this.methods;
    }

    private class CustomMethodVisitor extends MethodVisitor {
        private final Map<String, Object> methodMap;
        private final StringBuilder bytecode = new StringBuilder();

        public CustomMethodVisitor(Map<String, Object> methodMap, MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
            this.methodMap = methodMap;
        }

        @Override
        public void visitInsn(int opcode) {
            bytecode.append(opcode).append("\n");
            super.visitInsn(opcode);
        }

        @Override
        public void visitEnd() {
            methodMap.put("bytecode", bytecode.toString());
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
            gson.toJson(methods, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printAnalysisSummary() {
        long totalMethodCount = methods.size();
        long uniqueMethodCount = methods.stream().map(m -> m.get("methodIdentifier")).distinct().count();
        long duplicateMethodCount = totalMethodCount - uniqueMethodCount;

        System.out.println("Methods (total): " + totalMethodCount);
        System.out.println("Methods (unique): " + uniqueMethodCount);
        System.out.println("Methods (duplicated): " + duplicateMethodCount);
    }

    public void printDuplicateMethods() {
        Set<Object> uniqueMethodIdentifiers = new HashSet<>();
        List<Object> duplicateMethods = new ArrayList<>();

        for (Map<String, Object> methodDetail : methods) {
            Object methodIdentifier = methodDetail.get("methodIdentifier");
            if (!uniqueMethodIdentifiers.add(methodIdentifier)) {
                duplicateMethods.add(methodIdentifier);
            }
        }

        if (duplicateMethods.isEmpty()) {
            System.out.println("No duplicates found.");
        } else {
            System.out.println("Duplicate methods found:");
            for (Object duplicate : duplicateMethods) {
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