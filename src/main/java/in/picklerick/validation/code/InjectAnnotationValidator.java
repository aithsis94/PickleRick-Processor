package in.picklerick.validation.code;

import in.picklerick.IProcessStep;
import in.picklerick.Inject;
import in.picklerick.ProcessVariables;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import java.util.HashSet;
import java.util.Set;

public class InjectAnnotationValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();

        Set<? extends Element> injectableElements = roundEnv.getElementsAnnotatedWith(Inject.class);

        Set<VariableElement> injectableFieldElements = new HashSet<>();

        for (Element injectableElement : injectableElements) {

            ElementKind elementKind = injectableElement.getKind();

            if (elementKind == ElementKind.FIELD) {
                injectableFieldElements.add((VariableElement) injectableElement);
            } else if (elementKind == ElementKind.CONSTRUCTOR) {
                //injectableFieldElements.add(Util.methodParametersIn(injectableElements));
                //TODO private fields
            }
        }
    }
}
