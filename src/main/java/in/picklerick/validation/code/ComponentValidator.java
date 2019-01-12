package in.picklerick.validation.code;

import in.picklerick.*;
import in.picklerick.util.Util;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Created by ajitesh on 07/11/18.
 */

public class ComponentValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();

        Set<? extends Element> componentElements = roundEnv.getElementsAnnotatedWith(Component.class);
        Set<? extends Element> namedComponentElements = roundEnv.getElementsAnnotatedWith(NamedComponent.class);

        Set<Element> allComponents = Util.mergeElements(componentElements, namedComponentElements);

        for (Element element : allComponents) {
            TypeElement typeElement = (TypeElement) element;
            checkIfComponentIsValid(typeElement);
        }
    }

    private static void checkIfComponentIsValid(TypeElement typeElement) throws Exception {

        String typeFullName = typeElement.getQualifiedName().toString();

        if (typeElement.getKind() != ElementKind.CLASS) {
            throw new Exception(typeFullName + " is not a class");
        }

        Set<Modifier> modifiers = typeElement.getModifiers();

        if (!modifiers.contains(Modifier.PUBLIC)) {
            throw new Exception(typeFullName + " should be public to be marked as component");
        }

        if (!modifiers.contains(Modifier.FINAL)) {
            throw new Exception(typeFullName + " be should be final to be marked as component");
        }

        if (modifiers.contains(Modifier.ABSTRACT)) {
            throw new Exception(typeFullName + " cannot be abstract to be marked as component");
        }

        if (modifiers.contains(Modifier.STATIC)) {
            throw new Exception(typeFullName + " cannot be static to be marked as component");
        }

        List<? extends TypeParameterElement> typeParams = typeElement.getTypeParameters();
        if (typeParams.size() > 0) {
            throw new Exception(typeFullName + " cannot be a generic type to be marked as component");
        }

        Component componentAnnotation = typeElement.getAnnotation(Component.class);
        NamedComponent namedComponentAnnotation = typeElement.getAnnotation(NamedComponent.class);

        if (componentAnnotation != null && namedComponentAnnotation != null) {
            throw new Exception(typeFullName + " cannot be both Component and NamedComponent");
        }

        checkIfTypeHasValidInjectableFields(typeElement);
        checkIfComponentHasValidConstructors(typeElement);
    }

    private static void checkIfTypeHasValidInjectableFields(TypeElement typeElement) throws Exception {

        String typeFullName = typeElement.getQualifiedName().toString();

        List<VariableElement> fields = ElementFilter.fieldsIn(typeElement.getEnclosedElements());

        for (VariableElement field : fields) {

            String fieldName = field.getSimpleName().toString();
            String fieldFullName = typeFullName + "." + fieldName;

            boolean isInjectable = field.getAnnotation(Inject.class) != null
                    || field.getAnnotation(NamedInject.class) != null;

            if (!isInjectable)
                continue;

            Set<Modifier> modifiers = field.getModifiers();

            for (Modifier modifier : modifiers) {

                boolean isInvalid = true;
                String error = null;

                if (modifier == Modifier.ABSTRACT) {
                    error = "cannot be abstract";
                } else if (modifier == Modifier.FINAL) {
                    error = "cannot be final";
                } else if (modifier == Modifier.PRIVATE) {
                    error = "cannot be private";
                } else if (modifier == Modifier.PROTECTED) {
                    error = "cannot be protected";
                } else if (modifier == Modifier.STATIC) {
                    error = "cannot be static";
                } else {
                    isInvalid = false;
                }

                if (isInvalid) {
                    throw new Exception(fieldFullName + " " + error);
                }
            }
        }
    }

    private static void checkIfComponentHasValidConstructors(TypeElement typeElement) throws Exception {

        String typeFullName = typeElement.getQualifiedName().toString();
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(typeElement.getEnclosedElements());

        int injectableConstructorCount = 0;
        boolean hasDefaultConstructor = false;

        for (ExecutableElement constructor : constructors) {

            Annotation injectAnnotation = constructor.getAnnotation(Inject.class);

            if (injectAnnotation != null) {
                injectableConstructorCount++;
            }

            if (constructor.getParameters().size() == 0) {
                hasDefaultConstructor = true;
            }
        }

        if (injectableConstructorCount > 1) {
            throw new Exception(typeFullName + " cannot contain more than one injectable constructor");
        }

        if (!hasDefaultConstructor && injectableConstructorCount == 0) {
            throw new Exception(typeFullName + " doesn't have a default constructor or an injectable constructor");
        }
    }
}
