package in.picklerick.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import in.picklerick.NamedComponent;
import in.picklerick.NamedInject;
import in.picklerick.NamedValue;
import in.picklerick.util.Util;

public class DependencyResolver {

    public static Set<TypeElement> resolveDependenciesForType(TypeElement dependantTypeElement, Set<TypeElement> allComponents, DependencyType dependencyType, Types typesUtil) throws Exception {

        Set<TypeElement> dependencies = new HashSet<>();
        Set<String> uniqueNames = new HashSet<>();

        List<? extends Element> allEnclosedElements = dependantTypeElement.getEnclosedElements();

        Set<VariableElement> allInjectableElements = null;

        if (dependencyType == DependencyType.CREATIONAL) {
            allInjectableElements = Util.injectableParametersIn(allEnclosedElements);
        }else if(dependencyType == DependencyType.NON_CREATIONAL){
            allInjectableElements =  Util.injectableFieldsIn(allEnclosedElements);
        } else {
            Set<VariableElement> injectableConstructorParams = Util.injectableParametersIn(allEnclosedElements);
            Set<VariableElement> injectableFields = Util.injectableFieldsIn(allEnclosedElements);
            allInjectableElements = Util.mergeElements(injectableFields, injectableConstructorParams);
        }

        for (VariableElement injectableElement : allInjectableElements) {

            TypeElement dependencyTypeElement = resolveDependencyForVariable(injectableElement, allComponents, typesUtil);

            String fullName = dependencyTypeElement.getQualifiedName().toString();

            if (!uniqueNames.contains(fullName)) {
                uniqueNames.add(fullName);
                dependencies.add(dependencyTypeElement);
            }
        }

        return dependencies;
    }

    public static TypeElement resolveDependencyForVariable(VariableElement injectableElement, Set<TypeElement> allComponents, Types typesUtil) throws Exception {

        String injectableElementSimpleName = injectableElement.getSimpleName().toString();
        String injectableElementFullName = null;
        Element enclosingElement = injectableElement.getEnclosingElement();
        TypeElement injectableElementEnclosingTypeElement = null;

        if (enclosingElement.getKind() == ElementKind.CONSTRUCTOR) {
            injectableElementEnclosingTypeElement = (TypeElement) enclosingElement.getEnclosingElement();
            injectableElementFullName = injectableElementEnclosingTypeElement.getQualifiedName() + "." + enclosingElement.getSimpleName() + "." + injectableElementSimpleName;
        } else if (enclosingElement.getKind() == ElementKind.CLASS) {
            injectableElementEnclosingTypeElement = (TypeElement) enclosingElement;
            injectableElementFullName = injectableElementEnclosingTypeElement.getQualifiedName() + "." + injectableElementSimpleName;
        }

        TypeMirror injectableElementEnclosingTypeMirror = injectableElementEnclosingTypeElement.asType();

        TypeMirror injectableElementTypeMirror = injectableElement.asType();
        NamedInject namedInjectAnnotation = injectableElement.getAnnotation(NamedInject.class);
        NamedValue namedValueAnnotation = injectableElement.getAnnotation(NamedValue.class);

        if (namedInjectAnnotation != null || namedValueAnnotation != null) {

            String requiredComponentName = null;

            if(namedInjectAnnotation != null){
                requiredComponentName = namedInjectAnnotation.value();
            }else if(namedValueAnnotation != null){
                requiredComponentName = namedValueAnnotation.value();
            }

            int numOfNamedComponentsFound = 0;
            TypeElement matchedNamedComponent = null;

            for (TypeElement currTypeElement : allComponents) {

                String currTypeElementFullName = currTypeElement.getQualifiedName().toString();
                NamedComponent namedComponentAnnotation = currTypeElement.getAnnotation(NamedComponent.class);

                if (namedComponentAnnotation != null) {
                    String currComponentName = namedComponentAnnotation.value();
                    if (currComponentName.equals(requiredComponentName)) {

                        TypeMirror currComponentTypeMirror = currTypeElement.asType();

                        if (!typesUtil.isAssignable(currComponentTypeMirror, injectableElementTypeMirror)) {
                            throw new Exception(currTypeElementFullName + " is not assignable to " + injectableElementFullName);
                        }

                        if (typesUtil.isSameType(injectableElementEnclosingTypeMirror, currComponentTypeMirror)) {
                            throw new Exception(injectableElementFullName + " cannot be injected as it is referencing the enclosing type");
                        }

                        matchedNamedComponent = currTypeElement;
                        numOfNamedComponentsFound++;
                    }
                }
            }

            if (numOfNamedComponentsFound == 0) {
                throw new Exception(injectableElementFullName + " is not satisfied as no NamedComponent is found with name " + requiredComponentName);
            } else if (numOfNamedComponentsFound > 1) {
                throw new Exception(injectableElementFullName + " have multiple matches with name " + requiredComponentName);
            } else {
                return matchedNamedComponent;
            }

        } else {

            if (typesUtil.isAssignable(injectableElementEnclosingTypeMirror, injectableElementTypeMirror)) {
                throw new Exception(injectableElementFullName + " cannot injected because it is referencing the enclosing type");
            }

            List<TypeElement> allComponentsList = new ArrayList<>(allComponents);
            Map<Integer, TypeElement> matchRankToTypeElementMap = new HashMap<>();

            for (TypeElement testTypeElement : allComponentsList) {

                TypeMirror testTypeMirror = testTypeElement.asType();
                String testTypeElementFullName = testTypeElement.getQualifiedName().toString();

                int matchRank = getDependencyMatchRankBasedOnHierarchy(injectableElementTypeMirror, testTypeMirror, 0, typesUtil);

                if (matchRank != Integer.MAX_VALUE) {

                    if (matchRankToTypeElementMap.containsKey(matchRank)) {
                        String conflictingTypeFullName = matchRankToTypeElementMap.get(matchRank).getQualifiedName().toString();
                        throw new Exception(conflictingTypeFullName + " and " + testTypeElementFullName + " types are conflicting to be injected into " + injectableElementFullName);
                    } else {
                        matchRankToTypeElementMap.put(matchRank, testTypeElement);
                    }
                }
            }

            if (matchRankToTypeElementMap.size() == 0) {
                throw new Exception(injectableElementFullName + " cannot be injected as there are no matching Component(s)");
            } else {
                int bestMatchRank = Util.getMinValue(matchRankToTypeElementMap.keySet());
                return matchRankToTypeElementMap.get(bestMatchRank);
            }
        }
    }

    private static int getDependencyMatchRankBasedOnHierarchy(TypeMirror subjectTypeMirror, TypeMirror testTypeMirror, int currRank, Types typesUtil) {

        if (!typesUtil.isAssignable(testTypeMirror, subjectTypeMirror)) {
            return Integer.MAX_VALUE;
        }

        if (!typesUtil.isSameType(subjectTypeMirror, testTypeMirror)) {

            List<? extends TypeMirror> immediateSuperTypeMirrors = new ArrayList<>(typesUtil.directSupertypes(testTypeMirror));

            for (int i = 0; i < immediateSuperTypeMirrors.size(); i++) {
                TypeMirror immediateSuperTypeMirror = immediateSuperTypeMirrors.get(0);
                if (immediateSuperTypeMirror.toString().equals("java.lang.Object")) {
                    immediateSuperTypeMirrors.remove(immediateSuperTypeMirror);
                    i--;
                }
            }

            if (immediateSuperTypeMirrors.size() == 0) {
                return Integer.MAX_VALUE;
            }

            currRank++;

            int[] childRankList = new int[immediateSuperTypeMirrors.size()];
            for (int i = 0; i < immediateSuperTypeMirrors.size(); i++) {
                childRankList[i] = getDependencyMatchRankBasedOnHierarchy(subjectTypeMirror, immediateSuperTypeMirrors.get(i), currRank, typesUtil);
            }

            currRank = Util.getMinValue(childRankList);
        }

        return currRank;
    }
}
//
//    int numOfNamedComponentsFound = 0;
//    TypeElement matchedNamedComponent = null;
//    String requiredComponentName = namedInjectAnnotation.value();
//
//            for (TypeElement currTypeElement : allComponents) {
//
//                    String currTypeElementFullName = currTypeElement.getQualifiedName().toString();
//                    NamedComponent namedComponentAnnotation = currTypeElement.getAnnotation(NamedComponent.class);
//
//        if (namedComponentAnnotation != null) {
//        String currComponentName = namedComponentAnnotation.value();
//        if (currComponentName.equals(requiredComponentName)) {
//
//        TypeMirror currComponentTypeMirror = currTypeElement.asType();
//
//        if (!typesUtil.isAssignable(currComponentTypeMirror, injectableElementTypeMirror)) {
//        throw new Exception(currTypeElementFullName + " is not assignable to " + injectableElementFullName);
//        }
//
//        if (typesUtil.isSameType(injectableElementTypeMirror, injectableElementTypeMirror)) {
//        throw new Exception(injectableElementFullName + " cannot be injected as it is referencing it's enclosing class");
//        }
//
//        matchedNamedComponent = currTypeElement;
//        numOfNamedComponentsFound++;
//        }
//        }
//        }
//
//        if (numOfNamedComponentsFound == 0) {
//        throw new Exception(injectableElementFullName + " is not satisfied as no NamedComponent is found with name " + requiredComponentName);
//        } else if (numOfNamedComponentsFound > 1) {
//        throw new Exception(injectableElementFullName + " have multiple matches with name " + requiredComponentName);
//        } else {
//        return matchedNamedComponent;
//        }
