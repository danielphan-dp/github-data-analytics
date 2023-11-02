package com.githubanalytics.bytecode_parsers.callgraph;

import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BytecodeMethodExtractor {
    private final List<MethodIdentifier> methods = new ArrayList<>();

    private final Set<String> classNames = new HashSet<>();

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
                    className,
                    name,
                    convertTypesToStringList(Type.getArgumentTypes(descriptor)),
                    Type.getReturnType(descriptor).getClassName()
            );
            methods.add(methodId);
            return super.visitMethod(access, name, descriptor, signature, exceptions);
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
                methodInfo.add(methodEntry);
            }
            gson.toJson(methodInfo, writer);
        } catch (IOException e) {
            e.printStackTrace();
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
    }
}
