package in.picklerick.validation.dependency;

import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

import in.picklerick.ProcessVariables;
import in.picklerick.Setup;
import in.picklerick.constant.ProcessConstants;
import in.picklerick.dependency.DependencyGraph;
import in.picklerick.dependency.DependencyGraphHelper;
import in.picklerick.IProcessStep;
import in.picklerick.dependency.DependencyType;

/**
 * Created by ajitesh on 07/11/18.
 */

public class DependencyGraphValidator implements IProcessStep {

    @Override
    public void process(ProcessVariables processVariables) throws Exception {

        RoundEnvironment roundEnv = processVariables.getRoundEnvironment();
        Types typesUtil = processVariables.getTypesUtil();
        Map<String, Object> variableMap = processVariables.getVariables();

        Set<? extends Element> allSetupElements = roundEnv.getElementsAnnotatedWith(Setup.class);

        DependencyGraphHelper.validateSetupCyclicDependency((Set<TypeElement>)allSetupElements);

        for (Element setupElement : allSetupElements) {

            TypeElement setupTypeElement = (TypeElement) setupElement;
            DependencyGraph creationalGraph = DependencyGraphHelper.createDependencyGraph(setupTypeElement, DependencyType.CREATIONAL, roundEnv, typesUtil);
            DependencyGraph.checkIfCreationalDependencySatisfied(creationalGraph);
            DependencyGraph completeGraph = DependencyGraphHelper.createDependencyGraph(setupTypeElement, DependencyType.ALL, roundEnv, typesUtil);

            variableMap.put(ProcessConstants.GRAPH_CREATIONAL, creationalGraph);
            variableMap.put(ProcessConstants.GRAPH_COMPLETE, completeGraph);
        }
    }
}