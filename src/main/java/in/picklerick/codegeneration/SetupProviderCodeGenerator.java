package in.picklerick.codegeneration;

import com.squareup.javapoet.*;
import in.picklerick.IProcessStep;
import in.picklerick.ProcessVariables;
import in.picklerick.Setup;
import in.picklerick.constant.FileGenerationConstants;
import in.picklerick.dependency.DependencyGraphHelper;
import in.picklerick.dependency.DependencyResolver;
import in.picklerick.dependency.DependencyType;
import in.picklerick.util.Util;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetupProviderCodeGenerator implements IProcessStep {

    private ProcessVariables processVariables;

    @Override
    public void process(ProcessVariables processVariables) throws Exception {

        this.processVariables = processVariables;

        RoundEnvironment roundEnv = this.processVariables.getRoundEnvironment();
        Filer filer = this.processVariables.getFiler();

        Set<? extends Element> setupsElements = roundEnv.getElementsAnnotatedWith(Setup.class);

        for (Element element : setupsElements) {

            TypeElement setupElement = (TypeElement) element;
            String setupQualifiedName = setupElement.getQualifiedName().toString();
            String packageName = Util.getPackageFromClassName(setupQualifiedName);

            TypeSpec setupTypeSpec = createSetupProviderTypeSpec(setupElement);
            CodeWriter.writeToFile(filer, packageName, setupTypeSpec);
        }
    }

    private TypeSpec createSetupProviderTypeSpec(TypeElement setupElement) throws Exception {

        String setupSimpleName = setupElement.getSimpleName().toString();
        String setupGeneratorSimpleName = Util.generateProviderInterfaceImplName(setupSimpleName);
        Setup setupAnnotation = setupElement.getAnnotation(Setup.class);
        String setupAnnotationName = setupAnnotation.name();
        String setupNameWithAnnotatedName = setupSimpleName + "(" + setupAnnotationName + ")";

        RoundEnvironment roundEnv = this.processVariables.getRoundEnvironment();
        Types typesUtil = this.processVariables.getTypesUtil();

        Set<TypeElement> allComponentsInEnv = DependencyGraphHelper.filterAllComponentsForSetup(setupElement, roundEnv, typesUtil);

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(setupGeneratorSimpleName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ClassName.get(FileGenerationConstants.PACKAGE_NAME_BASE, FileGenerationConstants.ABSTRACT_CLASS_NAME_COMPONENT_PROVIDER));

        Set<TypeElement> singletonComponents = filterSingletonComponents(allComponentsInEnv);
        for (TypeElement singletonComponent : singletonComponents) {

            FieldSpec fieldSpec = generateFieldSpec(singletonComponent);
            typeSpecBuilder.addField(fieldSpec);
        }

        for (TypeElement componentElement : allComponentsInEnv) {

            MethodSpec componentProviderMethodSpec = generateProviderImplMethodSpec(componentElement, allComponentsInEnv, typesUtil);
            typeSpecBuilder.addMethod(componentProviderMethodSpec);
        }

        MethodSpec setupNameProviderMethodSpec = generateGetSetupNameProviderMethodSpec(setupNameWithAnnotatedName);
        typeSpecBuilder.addMethod(setupNameProviderMethodSpec);

        return typeSpecBuilder.build();
    }

    private static MethodSpec generateProviderImplMethodSpec(TypeElement componentElement, Set<TypeElement> allComponentsInEnv, Types typesUtil) throws Exception {

        Set<TypeElement> allCreationalDependencies = DependencyResolver.resolveDependenciesForType(componentElement, allComponentsInEnv, DependencyType.CREATIONAL, typesUtil);
        Set<TypeElement> nonCreationalDependencies = DependencyResolver.resolveDependenciesForType(componentElement, allComponentsInEnv, DependencyType.NON_CREATIONAL, typesUtil);

        String componentName = componentElement.getSimpleName().toString();
        String methodName = Util.generateProviderMethodName(componentName);

        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.SYNCHRONIZED)
                .addCode(generateCodeBlockForProvider(componentElement, allCreationalDependencies, nonCreationalDependencies, typesUtil))
                .returns(TypeName.get(componentElement.asType()))
                .build();
    }

    private static CodeBlock generateCodeBlockForProvider(TypeElement componentElement, Set<TypeElement> allCreationalDependencies, Set<TypeElement> nonCreationalDependencies, Types typesUtil) throws Exception {

        String componentQualifiedName = componentElement.getQualifiedName().toString();
        String componentPackageName = Util.getPackageFromClassName(componentQualifiedName);
        String componentSimpleName = componentElement.getSimpleName().toString();
        ClassName componentClassName = ClassName.get(componentPackageName, componentSimpleName);
        String componentCamelCaseName = Util.convertToLowerCamelCase(componentSimpleName);

        String setterClassSimpleName = componentSimpleName + FileGenerationConstants.CLASS_NAME_SETTER_SUFFIX;
        TypeName setterTypeName = ClassName.get(componentPackageName, setterClassSimpleName);

        String fieldName = Util.generateFieldName(componentCamelCaseName);
        String currVariableName = Util.generateRandomVariableName(Util.convertToLowerCamelCase(componentCamelCaseName));
        boolean isSingletonComponent = Util.isSingletonComponent(componentElement);

        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        codeBlockBuilder.add("\n");

        if (isSingletonComponent) {

            codeBlockBuilder.beginControlFlow("if(this.$L != null)", fieldName);
            codeBlockBuilder.addStatement("return this.$L", fieldName);
            codeBlockBuilder.endControlFlow();

            codeBlockBuilder.beginControlFlow("else");
        }

        List<String> variableNameList = new ArrayList<>();
        ExecutableElement constructor = Util.findInjectableConstructor(componentElement);

        if (constructor != null) {

            List<? extends VariableElement> paramElementList = constructor.getParameters();

            for (VariableElement paramElement : paramElementList) {

                TypeElement dependency = DependencyResolver.resolveDependencyForVariable(paramElement, allCreationalDependencies, typesUtil);
                String dependencyQualifiedName = dependency.getQualifiedName().toString();
                String dependencyPackageName = Util.getPackageFromClassName(dependencyQualifiedName);
                String dependencySimpleName = dependency.getSimpleName().toString();
                ClassName dependencyClassName = ClassName.get(dependencyPackageName, dependencySimpleName);
                String variableName = Util.generateRandomVariableName(Util.convertToLowerCamelCase(dependencySimpleName));

                codeBlockBuilder.add("$T $L = this.$L();", dependencyClassName, variableName, Util.generateProviderMethodName(dependencySimpleName));
                codeBlockBuilder.add("\n");
                variableNameList.add(variableName);
            }
        }

        codeBlockBuilder.add("$T $L = new $T($L);", componentClassName, currVariableName, componentClassName, String.join(", ", variableNameList));

        if (nonCreationalDependencies.size() > 0) {

            List<? extends Element> enclosedElements = componentElement.getEnclosedElements();
            Set<VariableElement> injectableFields = Util.injectableFieldsIn(enclosedElements);

            for (VariableElement injectableField : injectableFields) {

                TypeElement dependency = DependencyResolver.resolveDependencyForVariable(injectableField, nonCreationalDependencies, typesUtil);
                String dependencySimpleName = dependency.getSimpleName().toString();

                codeBlockBuilder.add("\n");
                //codeBlockBuilder.add("$L.$L = this.$L();", currVariableName, injectableFieldName, Util.generateProviderMethodName(dependencySimpleName));
                codeBlockBuilder.add("$T.$L($L, this.$L());", setterTypeName, FileGenerationConstants.METHOD_NAME_SETTER, currVariableName, Util.generateProviderMethodName(dependencySimpleName));
                codeBlockBuilder.add("\n");
            }
        }

        if (isSingletonComponent) {
            codeBlockBuilder.add("\n");
            codeBlockBuilder.add("this.$L = $L;", fieldName, currVariableName);
            codeBlockBuilder.add("\n");
        }

        codeBlockBuilder.add("return $L;", currVariableName);
        codeBlockBuilder.add("\n");

        if (isSingletonComponent) {
            codeBlockBuilder.endControlFlow();
            codeBlockBuilder.add("\n");
        }

        return codeBlockBuilder.build();
    }

    private static FieldSpec generateFieldSpec(TypeElement typeElement) {

        TypeMirror typeMirror = typeElement.asType();
        TypeName typeName = TypeName.get(typeMirror);
        String componentSimpleName = typeElement.getSimpleName().toString();
        String fieldName = Util.convertToLowerCamelCase(componentSimpleName);
        fieldName = Util.generateFieldName(fieldName);

        return FieldSpec.builder(typeName, fieldName, Modifier.PRIVATE).build();
    }

    private static Set<TypeElement> filterSingletonComponents(Set<TypeElement> allComponentsInEnv) {

        Set<TypeElement> singletonComponents = new HashSet<>();

        for (TypeElement componentTypeElement : allComponentsInEnv) {

            if (Util.isSingletonComponent(componentTypeElement)) {
                singletonComponents.add(componentTypeElement);
            }
        }

        return singletonComponents;
    }

    private static MethodSpec generateGetSetupNameProviderMethodSpec(String setupNameWithAnnotatedName) {

        return MethodSpec.methodBuilder(FileGenerationConstants.METHOD_NAME_SETUP_NAME_PROVIDER)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addCode("return $S;", setupNameWithAnnotatedName)
                .returns(String.class)
                .build();
    }
}
