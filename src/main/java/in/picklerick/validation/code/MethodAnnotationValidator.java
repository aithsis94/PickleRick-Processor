package in.picklerick.validation.code;

import in.picklerick.*;
import in.picklerick.util.Util;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Created by ajitesh on 07/11/18.
 */

public class MethodAnnotationValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();

        Set<? extends Element> injectElements = roundEnv.getElementsAnnotatedWith(Inject.class);
        Set<? extends Element> namedInjectElements = roundEnv.getElementsAnnotatedWith(NamedInject.class);
        Set<? extends Element> namedValueElements = roundEnv.getElementsAnnotatedWith(NamedValue.class);

        Set<Element> allElements = Util.mergeElements(injectElements, namedInjectElements, namedValueElements);

        {
            Set<ExecutableElement> constructors = ElementFilter.constructorsIn(allElements);

            for (ExecutableElement constructor : constructors) {

                TypeElement typeElement = (TypeElement) constructor.getEnclosingElement();
                String typeFullName = typeElement.getQualifiedName().toString();

                if (!Util.isComponent(typeElement)) {
                    throw new Exception(typeFullName + " is not a component and cannot have injectable constructor");
                }
            }
        }

        {

            Set<Element> methodParameters = Util.methodParametersIn(allElements);

            for (Element methodParameter : methodParameters) {

                ExecutableElement methodElement = (ExecutableElement) methodParameter.getEnclosingElement();
                TypeElement typeElement = (TypeElement) methodElement.getEnclosingElement();

                String methodName = methodElement.getSimpleName().toString();
                String typeFullName = typeElement.getQualifiedName().toString();
                String methodFullName = typeFullName + "." + methodName + "()";

                Annotation namedValueAnnotation = methodParameter.getAnnotation(NamedValue.class);
                if (namedValueAnnotation != null) {

                    String error = null;
                    boolean isInvalid = false;

                    if (methodElement.getKind() != ElementKind.CONSTRUCTOR) {
                        error = " is not a constructor and should not contain injectable parameters";
                        isInvalid = true;
                    } else if (!Util.isComponent(typeElement)) {
                        error = " is not a component's constructor and should not contain injectable parameters";
                        isInvalid = true;
                    }

                    if (isInvalid) {
                        error = methodFullName + error;
                        throw new Exception(error);
                    }
                }
            }
        }
    }
}
