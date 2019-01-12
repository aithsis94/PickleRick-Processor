package in.picklerick.codegeneration;

import com.squareup.javapoet.*;
import in.picklerick.*;
import in.picklerick.constant.FileGenerationConstants;
import in.picklerick.dependency.DependencyGraphHelper;
import in.picklerick.dependency.DependencyResolver;
import in.picklerick.util.Util;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SetupInjectorCodeGenerator implements IProcessStep {

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

            TypeSpec setupTypeSpec = createSetupTypeSpec(setupElement);
            CodeWriter.writeToFile(filer, packageName, setupTypeSpec);
        }
    }

    private TypeSpec createSetupTypeSpec(TypeElement setupElement) {

        String setupSimpleName = setupElement.getSimpleName().toString();
        String setupQualifiedName = setupElement.getQualifiedName().toString();
        String setupPackageName = Util.getPackageFromClassName(setupQualifiedName);
        String setupGeneratorSimpleName = Util.generateInjectorInterfaceImplName(setupSimpleName);
        Setup setupAnnotation = setupElement.getAnnotation(Setup.class);
        String setupAnnotationName = setupAnnotation.name();
        String setupNameWithAnnotatedName = setupSimpleName + "(" + setupAnnotationName + ")";

        RoundEnvironment roundEnv = this.processVariables.getRoundEnvironment();
        Types typesUtil = this.processVariables.getTypesUtil();

        Set<TypeElement> allComponentsInEnv = DependencyGraphHelper.filterAllComponentsForSetup(setupElement, roundEnv, typesUtil);
        Set<Element> injectableElements = Util.mergeElements(roundEnv.getElementsAnnotatedWith(Inject.class), roundEnv.getElementsAnnotatedWith(NamedInject.class));
        Set<TypeElement> pureConsumers = DependencyGraphHelper.filterAllPureConsumers(injectableElements);

        TypeSpec.Builder consumerInjectorBuilder = TypeSpec.classBuilder(setupGeneratorSimpleName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ClassName.get(FileGenerationConstants.PACKAGE_NAME_BASE, FileGenerationConstants.ABSTRACT_CLASS_NAME_COMPONENT_INJECTOR));

        for (TypeElement consumerElement : pureConsumers) {

            TypeMirror consumerTypeMirror = consumerElement.asType();
            String consumerName = consumerElement.getSimpleName().toString();
            String consumerFullName = consumerElement.getQualifiedName().toString();
            String consumerPackageName = Util.getPackageFromClassName(consumerFullName);
            String consumerCamelCaseName = Util.convertToLowerCamelCase(consumerName);
            String setterClassSimpleName = consumerName + FileGenerationConstants.CLASS_NAME_SETTER_SUFFIX;
            TypeName setterTypeName = ClassName.get(consumerPackageName, setterClassSimpleName);
            String methodName = Util.generateInjectorMethodName(consumerName);
            TypeName consumerTypeName = TypeName.get(consumerTypeMirror);

            MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(methodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(consumerTypeName, consumerCamelCaseName, Modifier.FINAL);


            Set<VariableElement> injectableFields = Util.injectableFieldsIn(consumerElement.getEnclosedElements());

            boolean canBeSatisfiedInThisEnv = true;
            List<CodeBlock> codeBlocks = new ArrayList<>();
            codeBlocks.add(CodeBlock.builder().add("\n").build());

            for (VariableElement injectableElement : injectableFields) {

                try {

                    TypeElement dependency = DependencyResolver.resolveDependencyForVariable(injectableElement, allComponentsInEnv, typesUtil);
                    String dependencyName = dependency.getSimpleName().toString();
                    String providerMethodName = Util.generateProviderMethodName(dependencyName);


                    CodeBlock codeBlock = CodeBlock.builder()
                            //.add("$L.$L = this.$L.$L();", consumerCamelCaseName, fieldName, FileGenerationConstants.FIELD_NAME_PROVIDER_OBJECT, providerMethodName)
                            .add("$T.$L($L, this.$L.$L());", setterTypeName, FileGenerationConstants.METHOD_NAME_SETTER, consumerCamelCaseName, FileGenerationConstants.FIELD_NAME_PROVIDER_OBJECT, providerMethodName)
                            .add("\n")
                            .build();

                    codeBlocks.add(codeBlock);

                } catch (Exception ex) {
                    canBeSatisfiedInThisEnv = false;
                    break;
                }
            }

            if (canBeSatisfiedInThisEnv) {
                for (CodeBlock codeBlock : codeBlocks) {
                    methodSpecBuilder.addCode(codeBlock);
                }
            } else {
                methodSpecBuilder.addCode("throw new $T($S);", UnsupportedOperationException.class, consumerFullName + " cannot be satisfied in " + setupNameWithAnnotatedName);
                methodSpecBuilder.addCode(CodeBlock.builder().add("\n").build());
            }

            consumerInjectorBuilder.addMethod(methodSpecBuilder.build());
        }

        TypeName providerTypeName = ClassName.get(setupPackageName, Util.generateProviderInterfaceImplName(setupSimpleName));

        MethodSpec constructorSpec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super(new $T())", providerTypeName)
                .build();

        consumerInjectorBuilder.addMethod(constructorSpec);
        return consumerInjectorBuilder.build();
    }
}
