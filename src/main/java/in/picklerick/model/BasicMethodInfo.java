package in.picklerick.model;

import javax.lang.model.element.TypeElement;

public class BasicMethodInfo {

    private String methodName;

    private TypeElement returnType;

    public BasicMethodInfo(String methodName, TypeElement returnType) {
        this.methodName = methodName;
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public TypeElement getReturnType() {
        return returnType;
    }
}
