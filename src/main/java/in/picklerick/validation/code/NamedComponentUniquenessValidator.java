package in.picklerick.validation.code;

import in.picklerick.IProcessStep;
import in.picklerick.NamedComponent;
import in.picklerick.ProcessVariables;
import in.picklerick.util.Util;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ajitesh on 07/11/18.
 */

public class NamedComponentUniquenessValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();

        Set<? extends Element> namedComponentElements = roundEnv.getElementsAnnotatedWith(NamedComponent.class);

        Set<String> names = new HashSet<>();

        for (Element element : namedComponentElements) {

            TypeElement typeElement = (TypeElement) element;
            String typeFullName = typeElement.getQualifiedName().toString();

            NamedComponent namedComponentAnnotation = typeElement.getAnnotation(NamedComponent.class);
            String componentName = namedComponentAnnotation.value();

            if (Util.isBlank(componentName)) {
                throw new Exception(typeFullName + " NamedComponent has invalid name");
            }

            if (names.contains(componentName)) {
                throw new Exception("Duplicate NamedComponent with name " + componentName);
            } else {
                names.add(componentName);
            }
        }
    }
}
