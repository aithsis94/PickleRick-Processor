package in.picklerick.validation.code;

import in.picklerick.*;
import in.picklerick.util.Util;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

/**
 * Created by ajitesh on 07/11/18.
 */

public class PrimitiveTypeInjectionValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();
        Set<? extends Element> injectElements = roundEnv.getElementsAnnotatedWith(Inject.class);
        Set<? extends Element> namedInjectElements = roundEnv.getElementsAnnotatedWith(NamedInject.class);
        Set<? extends Element> namedValueElements = roundEnv.getElementsAnnotatedWith(NamedValue.class);

        Set<Element> allElements = Util.mergeElements(injectElements, namedInjectElements, namedValueElements);

        for (Element element : allElements) {

            if (element.getKind() == ElementKind.PARAMETER
                    || element.getKind() == ElementKind.FIELD) {

                validateElement(element);

            } else if (element.getKind() == ElementKind.CONSTRUCTOR
                    || element.getKind() == ElementKind.METHOD) {

                ExecutableElement executableElement = (ExecutableElement) element;
                List<? extends VariableElement> parameters = executableElement.getParameters();

                for (VariableElement parameter : parameters) {
                    validateElement(parameter);
                }
            }
        }
    }

    private static void validateElement(Element element) throws Exception {

        VariableElement variableElement = (VariableElement) element;
        TypeMirror typeMirror = variableElement.asType();

        if (typeMirror.getKind().isPrimitive()) {

            String elementName = element.getSimpleName().toString();
            Element enclosingElement = element.getEnclosingElement();
            String fullQualifiedName = null;

            if (enclosingElement.getKind() == ElementKind.METHOD
                    || enclosingElement.getKind() == ElementKind.CONSTRUCTOR) {

                TypeElement typeElement = (TypeElement) enclosingElement.getEnclosingElement();

                String methodName = enclosingElement.getSimpleName().toString();
                String typeName = typeElement.getQualifiedName().toString();

                fullQualifiedName = typeName + "." + methodName + "." + elementName;

            } else if (enclosingElement.getKind() == ElementKind.CLASS) {

                TypeElement typeElement = (TypeElement) enclosingElement;
                String typeName = typeElement.getQualifiedName().toString();
                fullQualifiedName = typeName + "." + elementName;
            } else {
                fullQualifiedName = elementName;
            }

            throw new Exception(fullQualifiedName + " cannot be injected as it is a primitive type");
        }
    }
}
