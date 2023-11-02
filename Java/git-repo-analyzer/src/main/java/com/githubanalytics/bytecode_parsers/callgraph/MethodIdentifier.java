package com.githubanalytics.bytecode_parsers.callgraph;

import java.util.List;
import java.util.Objects;

public class MethodIdentifier {
    private final String className;
    private final String methodName;
    private final List<String> parameterTypes;
    private final String returnType;

    public MethodIdentifier(String className, String methodName, List<String> parameterTypes, String returnType) {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, parameterTypes, returnType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodIdentifier)) return false;
        MethodIdentifier that = (MethodIdentifier) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(parameterTypes, that.parameterTypes) &&
                Objects.equals(returnType, that.returnType);
    }

    @Override
    public String toString() {
        String params = String.join(", ", parameterTypes);
        return className + "." + methodName + "(" + params + "): " + returnType;
    }
}