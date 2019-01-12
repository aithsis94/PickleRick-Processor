package in.picklerick.codegeneration;

import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import in.picklerick.IProcessStep;
import in.picklerick.Inject;
import in.picklerick.NamedInject;
import in.picklerick.ProcessVariables;
import in.picklerick.Setup;
import in.picklerick.constant.FileGenerationConstants;
import in.picklerick.dependency.DependencyGraphHelper;
import in.picklerick.util.Util;

public class PickRickCodeGenerator implements IProcessStep {

    private ProcessVariables processVariables;

    @Override
    public void process(ProcessVariables processVariables) throws Exception {

        this.processVariables = processVariables;
        RoundEnvironment roundEnv = this.processVariables.getRoundEnvironment();
        Filer filer = this.processVariables.getFiler();

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(FileGenerationConstants.PICKLE_RICK_INJECTOR_NAME)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC);

        Set<Element> injectableElements = Util.mergeElements(roundEnv.getElementsAnnotatedWith(Inject.class),
                roundEnv.getElementsAnnotatedWith(NamedInject.class));

        Set<TypeElement> pureConsumers = DependencyGraphHelper.filterAllPureConsumers(injectableElements);

        for (TypeElement pureConsumer : pureConsumers) {

            MethodSpec methodSpec = generateInjectorMethod(pureConsumer);
            typeSpecBuilder.addMethod(methodSpec);
        }

        typeSpecBuilder
                .addStaticBlock(generateStaticInitCodeBlock())
                .addField(generateMapFieldSpec())
                .addField(generateSelectedSetupField())
                .addMethod(generateInitMethod())
                .addMethod(generateSetupNullCheckerMethodSpec())
                .addMethod(generateSetupSelectorMethodSpec());

        CodeWriter.writeToFile(filer, FileGenerationConstants.PACKAGE_NAME_BASE, typeSpecBuilder.build());
    }

    private MethodSpec generateInjectorMethod(TypeElement consumerElement) {

        TypeMirror consumerTypeMirror = consumerElement.asType();
        String consumerName = consumerElement.getSimpleName().toString();
        String consumerCamelCaseName = Util.convertToLowerCamelCase(consumerName);

        return MethodSpec.methodBuilder(FileGenerationConstants.PICKLE_RICK_INJECT_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .addParameter(TypeName.get(consumerTypeMirror), consumerCamelCaseName, Modifier.FINAL)
                .beginControlFlow("if($L())", FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_NULL_CHECKER_METHOD_NAME)
                .addStatement("$L.$L($L)", FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_FIELD_NAME, Util.generateInjectorMethodName(consumerName), consumerCamelCaseName)
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("throw new $T($S)", UnsupportedOperationException.class, "Setup is not selected")
                .endControlFlow()
                .build();
    }

    private FieldSpec generateMapFieldSpec() {

        TypeToken<Map<String, Object>> typeToken = new TypeToken<Map<String, Object>>() {
        };

        return FieldSpec.builder(typeToken.getType(), FileGenerationConstants.PICKLE_RICK_SETUP_PROVIDER_MAP_NAME, Modifier.PRIVATE, Modifier.STATIC).build();
    }

    private MethodSpec generateInitMethod() {

        RoundEnvironment roundEnv = this.processVariables.getRoundEnvironment();
        Set<? extends Element> setupElements = roundEnv.getElementsAnnotatedWith(Setup.class);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(FileGenerationConstants.PICKLE_RICK_INITIATOR_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        for (Element element : setupElements) {

            TypeElement setupElement = (TypeElement) element;
            Setup setupAnnotation = setupElement.getAnnotation(Setup.class);
            String setupAnnotationName = setupAnnotation.name();
            String setupQualifiedName = setupElement.getQualifiedName().toString();
            String setupPackageName = Util.getPackageFromClassName(setupQualifiedName);
            String setupSimpleName = setupElement.getSimpleName().toString();

            String setupInjectorName = Util.generateInjectorInterfaceImplName(setupSimpleName);
            TypeName setupInjectorTypeName = ClassName.get(setupPackageName, setupInjectorName);

            methodBuilder.addStatement("$L.put($S, new $T())", FileGenerationConstants.PICKLE_RICK_SETUP_PROVIDER_MAP_NAME, setupAnnotationName, setupInjectorTypeName);
        }

        return methodBuilder.build();
    }

    private CodeBlock generateStaticInitCodeBlock() {
        return CodeBlock.builder()
                .addStatement("$L = new $T<>()", FileGenerationConstants.PICKLE_RICK_SETUP_PROVIDER_MAP_NAME, HashMap.class)
                .addStatement("$L()", FileGenerationConstants.PICKLE_RICK_INITIATOR_METHOD_NAME)
                .build();
    }

    private MethodSpec generateSetupSelectorMethodSpec() {

        TypeName componentInjectorTypeName = ClassName.get(FileGenerationConstants.PACKAGE_NAME_BASE, FileGenerationConstants.INTERFACE_NAME_COMPONENT_INJECTOR);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(FileGenerationConstants.PICKLE_RICK_SETUP_SELECTOR_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String.class, FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_PARAM_NAME)
                .beginControlFlow("if($L())", FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_NULL_CHECKER_METHOD_NAME)
                .addStatement("throw new $T($S)", UnsupportedOperationException.class, "Setup is already selected")
                .endControlFlow()
                .beginControlFlow("if($L.containsKey($L))", FileGenerationConstants.PICKLE_RICK_SETUP_PROVIDER_MAP_NAME, FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_PARAM_NAME)
                .addStatement("$L = ($T)$L.get($L)", FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_FIELD_NAME, componentInjectorTypeName, FileGenerationConstants.PICKLE_RICK_SETUP_PROVIDER_MAP_NAME, FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_PARAM_NAME)
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("throw new $T($S)", UnsupportedOperationException.class, "attempt to select unknown setup")
                .endControlFlow();

        return methodBuilder.build();
    }

    private FieldSpec generateSelectedSetupField() {

        TypeName componentInjectorTypeName = ClassName.get(FileGenerationConstants.PACKAGE_NAME_BASE, FileGenerationConstants.INTERFACE_NAME_COMPONENT_INJECTOR);
        return FieldSpec.builder(componentInjectorTypeName, FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_FIELD_NAME, Modifier.STATIC, Modifier.PRIVATE).build();
    }

    private MethodSpec generateSetupNullCheckerMethodSpec() {

        return MethodSpec.methodBuilder(FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_NULL_CHECKER_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addStatement("return $L != null", FileGenerationConstants.PICKLE_RICK_SELECTED_SETUP_FIELD_NAME)
                .returns(boolean.class)
                .build();

    }
}
