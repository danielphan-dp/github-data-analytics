package com.githubanalytics.bytecode;

import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CallGraph {
    private final Map<MethodIdentifier, Set<MethodIdentifier>> methodRelations = new HashMap<>();
    private final Set<String> classNames = new HashSet<>();
    private final Set<String> nestedClassNames = new HashSet<>();

    private class CustomClassVisitor extends ClassVisitor {
        private final String className;

        CustomClassVisitor(String className) {
            super(Opcodes.ASM9);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodIdentifier methodId = new MethodIdentifier(
                    className,
                    name,
                    convertTypesToStringList(Type.getArgumentTypes(descriptor)),
                    Type.getReturnType(descriptor).getClassName());
            methodRelations.putIfAbsent(methodId, new HashSet<>());
            return new CustomMethodVisitor(methodId);
        }
    }

    private class CustomMethodVisitor extends MethodVisitor {
        private final MethodIdentifier currentMethod;

        CustomMethodVisitor(MethodIdentifier currentMethod) {
            super(Opcodes.ASM9);
            this.currentMethod = currentMethod;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            MethodIdentifier calledMethod = new MethodIdentifier(
                    owner.replace('/', '.'),
                    name,
                    convertTypesToStringList(Type.getArgumentTypes(descriptor)),
                    Type.getReturnType(descriptor).getClassName());
            methodRelations.get(currentMethod).add(calledMethod);
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    private List<String> convertTypesToStringList(Type[] types) {
        List<String> typeNames = new ArrayList<>();
        for (Type type : types) {
            typeNames.add(type.getClassName());
        }
        return typeNames;
    }

    public void analyzeDirectoryForCallGraph(String rootDir) {
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
            classNames.add(className);
            if (className.contains("$")) {
                nestedClassNames.add(className);
            }
            classReader.accept(new CustomClassVisitor(className), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportCallGraphToJson(String filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(filename)) {
            gson.toJson(constructJsonData(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> constructJsonData() {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("methodRelations", constructMethodRelations());
        return jsonData;
    }

    private Map<String, Set<String>> constructMethodRelations() {
        Map<String, Set<String>> relations = new HashMap<>();
        for (Map.Entry<MethodIdentifier, Set<MethodIdentifier>> entry : methodRelations.entrySet()) {
            relations.put(entry.getKey().toString(), convertMethodSetToStringSet(entry.getValue()));
        }
        return relations;
    }

    private Set<String> convertMethodSetToStringSet(Set<MethodIdentifier> methods) {
        Set<String> methodSet = new HashSet<>();
        for (MethodIdentifier method : methods) {
            methodSet.add(method.toString());
        }
        return methodSet;
    }

    public void printHeuristics() {
        System.out.println("Are all method keys unique: " + (methodRelations.keySet().stream().distinct().count() == methodRelations.size()));
        System.out.println("Number of methods: " + methodRelations.size());
        double averageSize = methodRelations.values().stream().mapToInt(Set::size).average().orElse(0.0);
        System.out.println("Average size of called methods: " + averageSize);
        System.out.println("Number of classes: " + classNames.size());
        System.out.println("Number of nested classes: " + nestedClassNames.size());
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java CallGraph <root directory of class files> <output JSON file>");
            System.exit(1);
        }

        CallGraph cg = new CallGraph();
        cg.analyzeDirectoryForCallGraph(args[0]);
        cg.exportCallGraphToJson(args[1]);
        cg.printHeuristics();
    }
}
