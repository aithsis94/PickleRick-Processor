package in.picklerick.dependency;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import in.picklerick.NamedComponent;
import in.picklerick.Setup;
import in.picklerick.util.Util;

/**
 * Created by ajitesh on 09/11/18.
 */

public class DependencyGraphHelper {

    public static Set<TypeElement> filterAllComponentsForSetup(TypeElement currSetupTypeElement, RoundEnvironment roundEnv, Types typesUtil) {

        Set<TypeElement> filteredComponentList = new HashSet<>();
        Set<? extends Element> allNamedComponentElements = roundEnv.getElementsAnnotatedWith(NamedComponent.class);
        Set<? extends Element> allSetups = roundEnv.getElementsAnnotatedWith(Setup.class);

        Set<TypeElement> subSetups = filterSubSetupsForSetup(currSetupTypeElement, allSetups);

        for (TypeElement setupElement : subSetups) {
            filteredComponentList.addAll(filterAllComponentsForSetup(setupElement, roundEnv, typesUtil));
        }

        Set<TypeElement> currSetupsAllComponentElements = DependencyGraphHelper.filterComponentsForSetup(currSetupTypeElement, typesUtil);
        Set<TypeElement> currSetupsAllNamedComponentElements = DependencyGraphHelper.filterNamedComponentsForSetup(currSetupTypeElement, allNamedComponentElements);
        Set<TypeElement> allCurrSetupsComponents = Util.mergeElements(currSetupsAllComponentElements, currSetupsAllNamedComponentElements);

        filteredComponentList.addAll(allCurrSetupsComponents);

        return filteredComponentList;
    }

    public static Set<TypeElement> filterSubSetupsForSetup(TypeElement currSetupTypeElement, Set<? extends Element> allSetups) {

        Set<TypeElement> filteredSubSetups = new HashSet<>();

        Setup currSetupAnnotation = currSetupTypeElement.getAnnotation(Setup.class);
        String[] allCurrSubSetupNames = currSetupAnnotation.subSetups();

        for (String currSubSetupName : allCurrSubSetupNames) {

            for (Element subSetup : allSetups) {

                Setup setupAnnotation = subSetup.getAnnotation(Setup.class);
                String setupName = setupAnnotation.name();

                if (currSubSetupName.equals(setupName)) {
                    filteredSubSetups.add((TypeElement) subSetup);
                }
            }
        }

        return filteredSubSetups;
    }

    public static Set<TypeElement> filterComponentsForSetup(TypeElement setupTypeElement, Types typesUtil) {

        Set<TypeElement> filteredComponentList = new HashSet<>();

        Setup setupAnnotation = setupTypeElement.getAnnotation(Setup.class);
        List<? extends TypeMirror> componentTypElementList = null;

        try {
            setupAnnotation.components();
        } catch (MirroredTypesException ex) {
            componentTypElementList = ex.getTypeMirrors();
        }

        for (TypeMirror componentTypeMirror : componentTypElementList) {

            TypeElement componentTypeElement = (TypeElement) typesUtil.asElement(componentTypeMirror);
            filteredComponentList.add(componentTypeElement);
        }

        return filteredComponentList;
    }

    public static Set<TypeElement> filterNamedComponentsForSetup(TypeElement setupTypeElement, Set<? extends Element> allNamedComponentElements) {

        Set<TypeElement> filteredComponentList = new HashSet<>();

        Setup setupAnnotation = setupTypeElement.getAnnotation(Setup.class);
        String[] namedComponentNames = setupAnnotation.namedComponents();

        for (String namedComponentName : namedComponentNames) {

            for (Element currentNamedComponentElement : allNamedComponentElements) {

                TypeElement currentNamedComponentTypeElement = (TypeElement) currentNamedComponentElement;
                NamedComponent namedComponentAnnotation = currentNamedComponentElement.getAnnotation(NamedComponent.class);
                String currentNameComponentName = namedComponentAnnotation.value();

                if (namedComponentName.equals(currentNameComponentName)) {
                    filteredComponentList.add(currentNamedComponentTypeElement);
                }
            }
        }

        return filteredComponentList;
    }

    public static DependencyGraph createDependencyGraph(TypeElement setupElement, DependencyType dependencyType, RoundEnvironment roundEnv, Types typesUtil) throws Exception {
        Set<TypeElement> allSetupComponents = DependencyGraphHelper.filterAllComponentsForSetup(setupElement, roundEnv, typesUtil);
        return DependencyGraphHelper.createDependencyGraph(allSetupComponents, dependencyType, typesUtil);
    }

    public static DependencyGraph createDependencyGraph(Set<TypeElement> allComponents, DependencyType dependencyType, Types types) throws Exception {

        Set<Node> rootNodeSet = new HashSet<>();
        Map<String, Node> nameToNodeMap = new HashMap<>();

        for (TypeElement currComponent : allComponents) {

            String currComponentFullName = currComponent.getQualifiedName().toString();

            Node currNode = new Node(currComponent);
            rootNodeSet.add(currNode);

            nameToNodeMap.put(currComponentFullName, currNode);
        }

        for (Node currNode : rootNodeSet) {

            Set<TypeElement> dependencyTypeElements = DependencyResolver.resolveDependenciesForType(currNode.getTypeElement(), allComponents, dependencyType, types);

            for (TypeElement dependencyTypeElement : dependencyTypeElements) {
                String dependencyName = dependencyTypeElement.getQualifiedName().toString();
                Node dependencyNode = nameToNodeMap.get(dependencyName);
                currNode.addDependency(dependencyNode);
            }
        }

        return new DependencyGraph(rootNodeSet);
    }

    public static void validateSetupCyclicDependency(Set<TypeElement> allSetups) throws Exception {

        Set<Node> rootNodeSet = new HashSet<>();

        {
            Map<String, Node> nameToNodeMap = new HashMap<>();

            for (TypeElement currSetup : allSetups) {

                String currSetupFullName = currSetup.getQualifiedName().toString();

                Node currNode = new Node(currSetup);
                rootNodeSet.add(currNode);

                nameToNodeMap.put(currSetupFullName, currNode);
            }

            for (Node currNode : rootNodeSet) {

                Set<TypeElement> dependencyTypeElements = DependencyGraphHelper.filterSubSetupsForSetup(currNode.getTypeElement(), allSetups);

                for (TypeElement dependencyTypeElement : dependencyTypeElements) {
                    String dependencyName = dependencyTypeElement.getQualifiedName().toString();
                    Node dependencyNode = nameToNodeMap.get(dependencyName);
                    currNode.addDependency(dependencyNode);
                }
            }
        }

        DependencyGraph dependencyGraph = new DependencyGraph(rootNodeSet);
        DependencyGraph.checkIfCreationalDependencySatisfied(dependencyGraph);
    }


    public static Set<TypeElement> filterAllConsumers(Collection<Element> allElementCollection){

        Set<TypeElement> allConsumerList = new HashSet<>();

        for (Element element : allElementCollection) {

            ElementKind elementKind = element.getKind();

            if (elementKind == ElementKind.FIELD) {

                if (Util.isInjectableField((VariableElement) element)) {
                    TypeElement enclosingTypeElement = (TypeElement) element.getEnclosingElement();
                    allConsumerList.add(enclosingTypeElement);
                }

            } else if (elementKind == ElementKind.PARAMETER) {

                ExecutableElement enclosingMethodElement = (ExecutableElement) element.getEnclosingElement();
                if (Util.isInjectableConstructor(enclosingMethodElement)) {
                    TypeElement enclosingTypeElement = (TypeElement) enclosingMethodElement.getEnclosingElement();
                    allConsumerList.add(enclosingTypeElement);
                }

            } else if (elementKind == ElementKind.CONSTRUCTOR) {

                if (Util.isInjectableConstructor((ExecutableElement) element)) {
                    TypeElement enclosingTypeElement = (TypeElement) element.getEnclosingElement();
                    allConsumerList.add(enclosingTypeElement);
                }
            }
        }

        return allConsumerList;
    }

    public static Set<TypeElement> filterAllPureConsumers(Collection<Element> allElementCollection) {

        Set<TypeElement> allConsumers = filterAllConsumers(allElementCollection);
        Set<TypeElement> pureConsumers = new HashSet<>();

        for(TypeElement consumer : allConsumers){

            if(!Util.isComponent(consumer) && !Util.isSetup(consumer)){
                pureConsumers.add(consumer);
            }
        }

        return pureConsumers;
    }
}
