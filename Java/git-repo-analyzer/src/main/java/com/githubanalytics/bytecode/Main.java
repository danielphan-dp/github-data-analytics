package com.githubanalytics.bytecode;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String sourceCodePath = "../Repos/gson";
        String bytecodePath = "../Repos/gson";
        String outputPath = "./data";

        SourceCodeMethodExtractor sourceExtractor = new SourceCodeMethodExtractor();
        sourceExtractor.analyzeDirectoryForMethods(sourceCodePath);

        BytecodeMethodExtractor bytecodeExtractor = new BytecodeMethodExtractor();
        bytecodeExtractor.analyzeDirectoryForMethods(bytecodePath);

        SourceCodeToBytecodeMapper mapper = new SourceCodeToBytecodeMapper();

        mapper.mapMethodsAndExportToJson(sourceExtractor.getMethodsSource(), bytecodeExtractor.getMethodsBytecode(), outputPath);
    }
}
