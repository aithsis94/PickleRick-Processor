package in.picklerick.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import in.picklerick.Component;
import in.picklerick.ComponentType;
import in.picklerick.Inject;
import in.picklerick.NamedComponent;
import in.picklerick.NamedInject;
import in.picklerick.Setup;
import in.picklerick.constant.FileGenerationConstants;

/**
 * Created by ajitesh on 06/11/18.
 */

public final class Util {

    private static final Random random = new Random();
    private static final char CHAR_DOT = '.';

    public static String getPackageFromClassName(String className) {
        return className.substring(0, className.lastIndexOf(CHAR_DOT));
    }

    public static <T> Set<T> mergeElements(Set<? extends T>... elementListArray) {

        Set<T> allList = new HashSet<>();

        if (elementListArray != null && elementListArray.length > 0) {
            for (Set<? extends T> elementList : elementListArray) {
                for (T element : elementList) {
                    allList.add(element);
                }
            }
        }

        return allList;
    }

    public static Set<Element> methodParametersIn(Set<? extends Element> elements) {

        Set<Element> methodParameters = new HashSet<>();

        for (Element element : elements) {
            if (element.getKind() == ElementKind.PARAMETER) {
                methodParameters.add(element);
            }
        }

        return methodParameters;
    }

    public static boolean isComponent(Element element) {
        return element.getAnnotation(Component.class) != null
                || element.getAnnotation(NamedComponent.class) != null;
    }

    public static boolean isSetup(Element element) {
        return element.getAnnotation(Setup.class) != null;
    }

    public static boolean isInjectableField(VariableElement element) {
        return element.getAnnotation(Inject.class) != null
                || element.getAnnotation(NamedInject.class) != null;
    }

    public static boolean isInjectableConstructor(ExecutableElement element) {
        return element.getKind() == ElementKind.CONSTRUCTOR && element.getAnnotation(Inject.class) != null;
    }

    public static boolean isBlank(String input) {

        if (input == null)
            return true;

        input = input.trim();

        return input.length() == 0;

    }

    public static Set<VariableElement> injectableFieldsIn(List<? extends Element> elements) {

        Set<VariableElement> injectableFields = new HashSet<>();

        for (Element element : elements) {
            if (element.getKind() == ElementKind.FIELD) {
                Inject injectAnnotation = element.getAnnotation(Inject.class);
                NamedInject namedInjectAnnotation = element.getAnnotation(NamedInject.class);
                if (injectAnnotation != null || namedInjectAnnotation != null) {
                    injectableFields.add((VariableElement) element);
                }
            }
        }

        return injectableFields;
    }

    public static Set<VariableElement> injectableParametersIn(List<? extends Element> elements) {

        Set<VariableElement> injectableParams = new HashSet<>();

        for (Element element : elements) {

            if (element.getKind() == ElementKind.CONSTRUCTOR && element.getAnnotation(Inject.class) != null) {

                ExecutableElement constructorElement = (ExecutableElement) element;

                List<? extends VariableElement> params = constructorElement.getParameters();
                injectableParams.addAll(params);
            }
        }

        return injectableParams;
    }

    public static int getMinValue(int... elements) {

        int minValue = Integer.MAX_VALUE;

        for (int element : elements) {
            if (element < minValue) {
                minValue = element;
            }
        }

        return minValue;
    }

    public static int getMinValue(Collection<Integer> elements) {

        int minValue = Integer.MAX_VALUE;

        for (int element : elements) {
            if (element < minValue) {
                minValue = element;
            }
        }

        return minValue;
    }

    public static String convertToLowerCamelCase(String input) {

        if (isBlank(input))
            return input;

        char firstChar = input.charAt(0);

        if (Character.isAlphabetic(firstChar) && Character.isUpperCase(firstChar)) {
            firstChar = Character.toLowerCase(firstChar);
            input = firstChar + input.substring(1);
        }

        return input;
    }

    public static String generateProviderMethodName(String clazzName) {
        return "provide" + clazzName;
    }

    public static String generateInjectorMethodName(String clazzName){
        return FileGenerationConstants.INJECTOR_METHOD_PREFIX + clazzName;
    }

    public static String generateFieldName(String clazzName) {
        return clazzName + "SingleInstance";
    }

    public static String generateProviderInterfaceImplName(String setupName) {
        return setupName + "Provider";
    }

    public static String generateInjectorInterfaceImplName(String setupName) {
        return setupName + "Injector";
    }

    public static ExecutableElement findInjectableConstructor(TypeElement componentElement) {

        List<? extends Element> allMethodElements = componentElement.getEnclosedElements();

        for (Element element : allMethodElements) {

            if (element.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement executableElement = (ExecutableElement) element;
                if (isInjectableConstructor(executableElement)) {
                    return executableElement;
                }
            }
        }

        return null;
    }

    public static boolean isSingletonComponent(TypeElement componentTypeElement) {

        Component componentAnnotation = componentTypeElement.getAnnotation(Component.class);

        if (componentAnnotation != null) {

            ComponentType componentType = componentAnnotation.type();
            return componentType == ComponentType.SINGLETON;

        } else {
            return true;
        }

    }

    public static String generateRandomVariableName(String prefix) {
        return prefix + generateRandomInt(10000, 1000000);
    }

    public static int generateRandomInt(int min, int max) {
        return min + random.nextInt(max - min);
    }
}
