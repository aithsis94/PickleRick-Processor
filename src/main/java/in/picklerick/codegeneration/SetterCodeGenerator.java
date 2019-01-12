package in.picklerick.codegeneration;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import in.picklerick.IProcessStep;
import in.picklerick.Inject;
import in.picklerick.NamedInject;
import in.picklerick.ProcessVariables;
import in.picklerick.constant.FileGenerationConstants;
import in.picklerick.dependency.DependencyGraphHelper;
import in.picklerick.util.Util;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetterCodeGenerator implements IProcessStep {

    @Override
    public void process(ProcessVariables processVariables) throws Exception {

        Filer filer = processVariables.getFiler();
        RoundEnvironment roundEnv = processVariables.getRoundEnvironment();
        Set<Element> injectableElements = Util.mergeElements(roundEnv.getElementsAnnotatedWith(Inject.class), roundEnv.getElementsAnnotatedWith(NamedInject.class));
        Set<TypeElement> consumers = DependencyGraphHelper.filterAllConsumers(injectableElements);

        for (TypeElement consumer : consumers) {

            String consumerFullName = consumer.getQualifiedName().toString();
            String packageName = Util.getPackageFromClassName(consumerFullName);

            TypeSpec setterTypeSpec = generateSetterClassTypeSpec(consumer);

            CodeWriter.writeToFile(filer, packageName, setterTypeSpec);
        }
    }

    private static TypeSpec generateSetterClassTypeSpec(TypeElement consumerElement) {

        String consumerName = consumerElement.getSimpleName().toString();
        String setterClassName = consumerName + FileGenerationConstants.CLASS_NAME_SETTER_SUFFIX;

        Set<MethodSpec> setterMethods = generateSetterMethods(consumerElement);

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(setterClassName).addModifiers(Modifier.FINAL, Modifier.PUBLIC);

        for (MethodSpec setterMethodSpec : setterMethods) {
            typeSpecBuilder.addMethod(setterMethodSpec);
        }

        typeSpecBuilder.addMethod(generatePrivateConstructorMethodSpec());

        return typeSpecBuilder.build();
    }

    private static Set<MethodSpec> generateSetterMethods(TypeElement consumerElement) {

        String consumerName = consumerElement.getSimpleName().toString();
        String objectParamName = Util.convertToLowerCamelCase(consumerName);
        List<? extends Element> allEnclosedElements = consumerElement.getEnclosedElements();
        Set<VariableElement> injectableFields = Util.injectableFieldsIn(allEnclosedElements);

        Set<MethodSpec> setterMethodSpecs = new HashSet<>();

        for (VariableElement injectableField : injectableFields) {

            String fieldName = injectableField.getSimpleName().toString();

            ParameterSpec objectParameterSpec = ParameterSpec.builder(TypeName.get(consumerElement.asType()), objectParamName, Modifier.FINAL).build();
            ParameterSpec valueParameterSpec = ParameterSpec.builder(TypeName.get(injectableField.asType()), fieldName, Modifier.FINAL).build();

            MethodSpec setterMethodSpec = MethodSpec.methodBuilder(FileGenerationConstants.METHOD_NAME_SETTER)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(objectParameterSpec)
                    .addParameter(valueParameterSpec)
                    .addCode("$L.$L = $L;\n", objectParamName, fieldName, fieldName)
                    .build();

            setterMethodSpecs.add(setterMethodSpec);
        }

        return setterMethodSpecs;
    }

    private static MethodSpec generatePrivateConstructorMethodSpec() {

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();
    }
}
