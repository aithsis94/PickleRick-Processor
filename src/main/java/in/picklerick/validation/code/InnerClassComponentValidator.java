package in.picklerick.validation.code;

import in.picklerick.Component;
import in.picklerick.IProcessStep;
import in.picklerick.NamedComponent;
import in.picklerick.ProcessVariables;
import in.picklerick.util.Util;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by ajitesh on 07/11/18.
 */

public class InnerClassComponentValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();

        Set<? extends Element> componentElements = roundEnv.getElementsAnnotatedWith(Component.class);
        Set<? extends Element> namedComponentElements = roundEnv.getElementsAnnotatedWith(NamedComponent.class);

        Set<Element> allComponents = Util.mergeElements(componentElements, namedComponentElements);

        for (Element component : allComponents) {

            TypeElement typeElement = (TypeElement) component;
            String typeFullName = typeElement.getQualifiedName().toString();

            Element enclosingElement = typeElement.getEnclosingElement();
            if (enclosingElement == null) {
                throw new Exception(typeFullName + " cannot be component because it is not in a package");
            }

            if (enclosingElement.getKind() != ElementKind.PACKAGE) {
                throw new Exception(typeFullName + " cannot be component because it is a inner class");
            }
        }
    }
}
