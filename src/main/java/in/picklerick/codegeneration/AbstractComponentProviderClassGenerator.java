package in.picklerick.codegeneration;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import in.picklerick.Component;
import in.picklerick.IProcessStep;
import in.picklerick.NamedComponent;
import in.picklerick.ProcessVariables;
import in.picklerick.constant.FileGenerationConstants;
import in.picklerick.constant.ProcessConstants;
import in.picklerick.model.BasicMethodInfo;
import in.picklerick.util.Util;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AbstractComponentProviderClassGenerator implements IProcessStep {

    @Override
    public void process(ProcessVariables processVariables) throws Exception {

        RoundEnvironment roundEnv = processVariables.getRoundEnvironment();
        Filer filer = processVariables.getFiler();
        Map<String, Object> variables = processVariables.getVariables();

        Set<? extends Element> components = roundEnv.getElementsAnnotatedWith(Component.class);
        Set<? extends Element> namedComponents = roundEnv.getElementsAnnotatedWith(NamedComponent.class);
        Set<? extends Element> allComponents = Util.mergeElements(components, namedComponents);
        Map<String, BasicMethodInfo> interfaceMethodBasicInfoSet = new HashMap<>();

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(FileGenerationConstants.ABSTRACT_CLASS_NAME_COMPONENT_PROVIDER)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

        for (Element element : allComponents) {

            TypeElement componentElement = (TypeElement) element;
            String componentSimpleName = componentElement.getSimpleName().toString();
            String providerMethodName = Util.generateProviderMethodName(componentSimpleName);

            MethodSpec defaultProviderMethodSpec = getDefaultProviderMethodSpec(componentElement);

            typeSpecBuilder.addMethod(defaultProviderMethodSpec);

            BasicMethodInfo basicMethodInfo = new BasicMethodInfo(providerMethodName, componentElement);
            interfaceMethodBasicInfoSet.put(providerMethodName, basicMethodInfo);
        }

        typeSpecBuilder.addMethod(getSetupNameProviderAbstractMethodSpec());

        TypeSpec interfaceTypeSpec = typeSpecBuilder.build();
        CodeWriter.writeToFile(filer, FileGenerationConstants.PACKAGE_NAME_BASE, interfaceTypeSpec);

        variables.put(ProcessConstants.COMPONENT_PROVIDER_CLASS_METHOD_MAP, Collections.unmodifiableMap(interfaceMethodBasicInfoSet));
    }

    private static MethodSpec getDefaultProviderMethodSpec(TypeElement componentElement) {

        String componentFullName = componentElement.getQualifiedName().toString();
        String componentSimpleName = componentElement.getSimpleName().toString();
        String providerMethodName = Util.generateProviderMethodName(componentSimpleName);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(providerMethodName)
                .addModifiers(Modifier.PUBLIC, Modifier.SYNCHRONIZED)
                .addCode("throw new $T($S + this.$L());\n", UnsupportedOperationException.class, componentFullName + " cannot be provided in ", FileGenerationConstants.METHOD_NAME_SETUP_NAME_PROVIDER )

                .returns(TypeName.get(componentElement.asType()));

        return methodBuilder.build();
    }

    private static MethodSpec getSetupNameProviderAbstractMethodSpec() {

        return MethodSpec.methodBuilder(FileGenerationConstants.METHOD_NAME_SETUP_NAME_PROVIDER)
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                .returns(String.class)
                .build();
    }
}
