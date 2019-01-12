package in.picklerick.validation.code;

import in.picklerick.*;
import in.picklerick.util.Util;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Created by ajitesh on 08/11/18.
 */

public class SetupValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables variables) throws Exception {

        RoundEnvironment roundEnv = variables.getRoundEnvironment();
        Types typesUtil = variables.getTypesUtil();

        Set<? extends Element> setupElements = roundEnv.getElementsAnnotatedWith(Setup.class);
        Set<? extends Element> namedComponentElements = roundEnv.getElementsAnnotatedWith(NamedComponent.class);

        for (Element currSetupElement : setupElements) {

            TypeElement typeElement = (TypeElement) currSetupElement;
            String typeFullName = typeElement.getQualifiedName().toString();

            {
                if (typeElement.getKind() != ElementKind.INTERFACE) {
                    throw new Exception(typeFullName + " should be a interface to be marked as Setup");
                }

                boolean isDeclaredAsComponent =
                        typeElement.getAnnotation(Component.class) != null
                                || typeElement.getAnnotation(NamedComponent.class) != null;

                if (isDeclaredAsComponent) {
                    throw new Exception(typeFullName + " can be a Setup or a Component, but not both");
                }
            }

            Setup currSetupAnnotation = currSetupElement.getAnnotation(Setup.class);
            String currSetupName = currSetupAnnotation.name();
            String[] currSetupsNamedComponentNames = currSetupAnnotation.namedComponents();
            String[] currSubSetups = currSetupAnnotation.subSetups();
            List<? extends TypeMirror> currSetupsComponentTypElementList = null;

            try {
                currSetupAnnotation.components();
            } catch (MirroredTypesException ex) {
                currSetupsComponentTypElementList = ex.getTypeMirrors();
            }

            if (Util.isBlank(currSetupName)) {
                throw new Exception(typeFullName + " Setup has invalid name");
            }

            typeFullName = typeFullName + "(" + currSetupName + ")";

            if ((currSetupsComponentTypElementList == null || currSetupsComponentTypElementList.size() == 0) && currSetupsNamedComponentNames.length == 0 && currSubSetups.length == 0) {
                throw new Exception(typeFullName + " should contain atleast one SubSetup/Component/NamedComponent");
            }

            {
                for (Element element : setupElements) {

                    if (!element.equals(currSetupElement)) {

                        Setup setupAnnotation = element.getAnnotation(Setup.class);
                        String setupName = setupAnnotation.name();

                        if (currSetupName.equals(setupName)) {
                            throw new Exception("Multiple setups are defined with same name " + setupName);
                        }
                    }
                }
            }

            for (String currSubSetupName : currSubSetups) {

                if (currSubSetupName.equals(currSetupName)) {
                    throw new Exception(typeFullName + " cannot be its own sub setup");
                }

                boolean matchFound = false;

                for (Element subSetupElement : setupElements) {

                    Setup setupAnnotation = subSetupElement.getAnnotation(Setup.class);
                    String setupName = setupAnnotation.name();

                    if (setupName.equals(currSubSetupName)) {
                        matchFound = true;
                        break;
                    }
                }

                if (!matchFound) {
                    throw new Exception(typeFullName + " Setup contains unknown SubSetup " + currSubSetupName);
                }
            }

            {
                for (TypeMirror componentTypeMirror : currSetupsComponentTypElementList) {

                    TypeElement componentTypeElement = (TypeElement) typesUtil.asElement(componentTypeMirror);

                    String componentName = componentTypeElement.getQualifiedName().toString();

                    Annotation componentAnnotation = componentTypeElement.getAnnotation(Component.class);

                    if (componentAnnotation == null) {
                        throw new Exception(typeFullName + " Setup contains invalid Component " + componentName);
                    }
                }
            }

            {
                for (String namedComponentName : currSetupsNamedComponentNames) {

                    boolean namedComponentFound = false;

                    for (Element namedComponentElement : namedComponentElements) {

                        TypeElement namedComponentTypeElement = (TypeElement) namedComponentElement;
                        NamedComponent namedComponentAnnotation = namedComponentTypeElement.getAnnotation(NamedComponent.class);
                        String currentComponentName = namedComponentAnnotation.value();

                        if (currentComponentName.equalsIgnoreCase(namedComponentName)) {
                            namedComponentFound = true;
                            break;
                        }
                    }

                    if (!namedComponentFound) {
                        throw new Exception(typeFullName + " Setup contains unknown NamedComponent " + namedComponentName);
                    }
                }
            }
        }
    }
}
