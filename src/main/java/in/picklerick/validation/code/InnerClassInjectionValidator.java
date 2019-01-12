package in.picklerick.validation.code;

import in.picklerick.IProcessStep;
import in.picklerick.Inject;
import in.picklerick.NamedInject;
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

public class InnerClassInjectionValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();

        Set<? extends Element> injectElements = roundEnv.getElementsAnnotatedWith(Inject.class);
        Set<? extends Element> namedInjectElements = roundEnv.getElementsAnnotatedWith(NamedInject.class);

        Set<Element> allElement = Util.mergeElements(injectElements, namedInjectElements);

        for (Element element : allElement) {

            String elementName = element.getSimpleName().toString();

            Element enclosingElement = element.getEnclosingElement();
            if (enclosingElement == null) {
                continue;
            }

            String typeName = ((TypeElement) enclosingElement).getQualifiedName().toString();

            Element packageElement = enclosingElement.getEnclosingElement();

            if (packageElement != null
                    && packageElement.getKind() != ElementKind.PACKAGE) {
                throw new Exception(typeName + "." + elementName + " cannot be injected as it is in an inner class");
            }
        }
    }
}
