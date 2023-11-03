package com.githubanalytics.bytecode;

import java.util.List;
import java.util.Objects;

public class MethodIdentifier {
    private final String className;
    private final String methodName;
    private final List<String> parameterTypes;
    private final String returnType;

    public MethodIdentifier(
            String className,
            String methodName,
            List<String> parameterTypes,
            String returnType) {
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

        boolean classNameMatch = Objects.equals(
                this.className.replace('$', '.'),
                that.className.replace('$', '.')
        );

        boolean methodNameMatch = Objects.equals(
                this.methodName,
                that.methodName
        );

        // TODO: More logic to add here. At the moment, only short-handed type are used.
        boolean returnTypeMatch = true;
        if (classNameMatch && methodNameMatch) {
            String[] ta1 = this.returnType.replace("$", ".").split("\\.");
            String[] ta2 = that.returnType.replace("$", ".").split("\\.");
            String rt1 = ta1[ta1.length - 1];
            String rt2 = ta2[ta2.length - 1];

            if (!Objects.equals(rt1, rt2)) {
                returnTypeMatch = false;
            }

        } else {
            returnTypeMatch = false;
        }

        // TODO: More logic to add here. At the moment, only short-handed type are used.
        boolean parameterTypesMatch = true;
        if (classNameMatch && methodNameMatch && returnTypeMatch &&
                this.parameterTypes.size() == that.parameterTypes.size()) {
            int s = this.parameterTypes.size();
            for (int i = 0; i < s; ++i) {
                String[] ta1 = this.parameterTypes.get(i).replace("$", ".").split("\\.");
                String[] ta2 = that.parameterTypes.get(i).replace("$", ".").split("\\.");
                String thisSimplifiedType = ta1[ta1.length - 1];
                String thatSimplifiedType = ta2[ta2.length - 1];
                if (!Objects.equals(thisSimplifiedType, thatSimplifiedType)) {
                    // DEBUG
//                    System.out.println("Type Not equal: " + thisSimplifiedType + " " + thatSimplifiedType);

                    parameterTypesMatch = false;
                }

                // TODO: Handle edge cases here. Compile a white list.
                // if (Objects.equals(thisSimplifiedType, "T") && Objects.equals(thatSimplifiedType, "Object")) parameterTypesMatch = true;
                // if (Objects.equals(thatSimplifiedType, "T") && Objects.equals(thisSimplifiedType, "Object")) parameterTypesMatch = true;

                // TODO: More check.
                // If everything is matched until this point. And returnType is "java.lang.Object"
                // give this case the "benefit of the doubt".
            }
        } else {
            parameterTypesMatch = false;
        }

        // EDGE CASES
        // Edge case 1
        if (classNameMatch && methodNameMatch && parameterTypesMatch)
            // At this point, benefit of the doubt can be given.
            if (Objects.equals(this.returnType, "java.lang.Object") || Objects.equals(that.returnType, "java.lang.Object")) {
                returnTypeMatch = true;
            }

        return classNameMatch &&
                methodNameMatch &&
                returnTypeMatch &&
                parameterTypesMatch;
    }

    @Override
    public String toString() {
        String params = String.join(", ", parameterTypes);
        return className + "." + methodName + "(" + params + "): " + returnType;
    }
}