package com.githubanalytics.bytecode_parsers.callgraph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import java.io.*;
import java.util.*;

public class CallGraph {
    private final Map<MethodIdentifier, Set<MethodIdentifier>> graph = new HashMap<>();

    static class MethodIdentifier {
        private final String className;
        private final String methodName;
        private final List<String> parameterTypes;

        MethodIdentifier(String className, String methodName, List<String> parameterTypes) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = new ArrayList<>(parameterTypes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodIdentifier that = (MethodIdentifier) o;
            return Objects.equals(className, that.className) &&
                    Objects.equals(methodName, that.methodName) &&
                    Objects.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName, parameterTypes);
        }

        @Override
        public String toString() {
            return className + "." + methodName + parameterTypes;
        }
    }

    class CustomClassVisitor extends ClassVisitor {
        private String className;

        public CustomClassVisitor(int api) {
            super(api);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name.replace('/', '.');
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new CustomMethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions), className, name, Type.getArgumentTypes(descriptor));
        }
    }

    class CustomMethodVisitor extends MethodVisitor {
        private final MethodIdentifier currentMethod;

        public CustomMethodVisitor(int api, MethodVisitor mv, String className, String methodName, Type[] argumentTypes) {
            super(api, mv);
            this.currentMethod = new MethodIdentifier(className, methodName, convertTypesToStringList(argumentTypes));
            graph.putIfAbsent(currentMethod, new HashSet<>());
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            MethodIdentifier calledMethod = new MethodIdentifier(owner.replace('/', '.'), name, convertTypesToStringList(Type.getArgumentTypes(descriptor)));
            graph.get(currentMethod).add(calledMethod);
        }

        private List<String> convertTypesToStringList(Type[] types) {
            List<String> typeNames = new ArrayList<>();
            for (Type type : types) {
                typeNames.add(type.getClassName());
            }
            return typeNames;
        }
    }

    public void generateCallGraphFromFolders(String classesDir, String testClassesDir) {
        processDirectory(new File(classesDir));
        processDirectory(new File(testClassesDir));
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
            classReader.accept(new CustomClassVisitor(Opcodes.ASM9), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dumpCallGraphToJsonFile(String filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> jsonData = new HashMap<>();
        Map<String, Set<String>> simplifiedGraph = new HashMap<>();

        Set<MethodIdentifier> allMethods = new HashSet<>();
        graph.forEach((method, calledMethods) -> {
            simplifiedGraph.put(method.toString(), convertMethodSetToStringSet(calledMethods));
            allMethods.add(method);
            allMethods.addAll(calledMethods);
        });

        List<Map<String, String>> methodsList = new ArrayList<>();
        allMethods.forEach(method -> {
            Map<String, String> methodMap = new HashMap<>();
            methodMap.put("className", method.className);
            methodMap.put("methodName", method.methodName);
            methodMap.put("parameterTypes", method.parameterTypes.toString());
            methodsList.add(methodMap);
        });

        jsonData.put("methods", methodsList);
        jsonData.put("callGraph", simplifiedGraph);
        jsonData.put("metadata", Collections.singletonMap("totalMethods", allMethods.size()));

        try (Writer writer = new FileWriter(filename)) {
            gson.toJson(jsonData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<String> convertMethodSetToStringSet(Set<MethodIdentifier> methods) {
        Set<String> result = new HashSet<>();
        for (MethodIdentifier method : methods) {
            result.add(method.toString());
        }
        return result;
    }

    public static void main(String[] args) {
        CallGraph callGraph = new CallGraph();
        callGraph.generateCallGraphFromFolders(
                "..\\Repos\\gson\\gson\\target\\classes\\",
                "..\\Repos\\gson\\gson\\target\\test-classes\\"
        );
        callGraph.dumpCallGraphToJsonFile("callGraph__google_gson.json");
    }
}