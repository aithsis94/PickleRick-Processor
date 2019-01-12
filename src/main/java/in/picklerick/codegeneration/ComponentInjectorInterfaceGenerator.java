package in.picklerick.codegeneration;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import in.picklerick.IProcessStep;
import in.picklerick.Inject;
import in.picklerick.NamedInject;
import in.picklerick.ProcessVariables;
import in.picklerick.constant.FileGenerationConstants;
import in.picklerick.constant.ProcessConstants;
import in.picklerick.dependency.DependencyGraphHelper;
import in.picklerick.model.BasicMethodInfo;
import in.picklerick.util.Util;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ComponentInjectorInterfaceGenerator implements IProcessStep {

    @Override
    public void process(ProcessVariables processVariables) throws Exception {

        RoundEnvironment roundEnv = processVariables.getRoundEnvironment();
        Filer filer = processVariables.getFiler();
        Map<String, Object> variables = processVariables.getVariables();

        Set<Element> injectableElements = Util.mergeElements(roundEnv.getElementsAnnotatedWith(Inject.class),
                roundEnv.getElementsAnnotatedWith(NamedInject.class));

        TypeSpec.Builder consumerInjectorBuilder = TypeSpec.interfaceBuilder(FileGenerationConstants.INTERFACE_NAME_COMPONENT_INJECTOR)
                .addModifiers(Modifier.PUBLIC);

        Set<TypeElement> pureConsumers = DependencyGraphHelper.filterAllPureConsumers(injectableElements);
        Map<String, BasicMethodInfo> consumerInjectorMethods = new HashMap<>();

        for (TypeElement pureConsumer : pureConsumers) {

            MethodSpec methodSpec = generateInjectorMethod(pureConsumer);
            consumerInjectorBuilder.addMethod(methodSpec);

            BasicMethodInfo basicMethodInfo = new BasicMethodInfo(methodSpec.name, pureConsumer);
            consumerInjectorMethods.put(basicMethodInfo.getMethodName(), basicMethodInfo);
        }

        CodeWriter.writeToFile(filer, FileGenerationConstants.PACKAGE_NAME_BASE, consumerInjectorBuilder.build());
        variables.put(ProcessConstants.COMPONENT_INJECTOR_INTERFACE_METHOD_MAP, consumerInjectorMethods);
    }


    private static MethodSpec generateInjectorMethod(TypeElement consumerElement) {

        TypeMirror consumerTypeMirror = consumerElement.asType();
        String consumerName = consumerElement.getSimpleName().toString();
        String consumerCamelCaseName = Util.convertToLowerCamelCase(consumerName);
        String methodName = Util.generateInjectorMethodName(consumerName);

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.get(consumerTypeMirror), consumerCamelCaseName, Modifier.FINAL)
                .build();
    }
}
