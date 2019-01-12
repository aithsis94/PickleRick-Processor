package in.picklerick.codegeneration;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

import in.picklerick.IProcessStep;
import in.picklerick.ProcessVariables;
import in.picklerick.constant.FileGenerationConstants;

public class AbstractComponentInjectorGenerator implements IProcessStep {

    @Override
    public void process(ProcessVariables processVariables) throws Exception {

        Filer filer = processVariables.getFiler();

        TypeSpec.Builder consumerInjectorBuilder = TypeSpec.classBuilder(FileGenerationConstants.ABSTRACT_CLASS_NAME_COMPONENT_INJECTOR)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(ClassName.get(FileGenerationConstants.PACKAGE_NAME_BASE, FileGenerationConstants.INTERFACE_NAME_COMPONENT_INJECTOR));

        consumerInjectorBuilder.addMethod(generateConstructor());
        consumerInjectorBuilder.addField(generateProviderField());

        CodeWriter.writeToFile(filer, FileGenerationConstants.PACKAGE_NAME_BASE, consumerInjectorBuilder.build());
    }

    private static FieldSpec generateProviderField() {
        TypeName typeName = ClassName.get(FileGenerationConstants.PACKAGE_NAME_BASE, FileGenerationConstants.ABSTRACT_CLASS_NAME_COMPONENT_PROVIDER);
        return FieldSpec.builder(typeName, FileGenerationConstants.FIELD_NAME_PROVIDER_OBJECT)
                .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                .build();
    }

    private static MethodSpec generateConstructor() {

        TypeName typeName = ClassName.get(FileGenerationConstants.PACKAGE_NAME_BASE, FileGenerationConstants.ABSTRACT_CLASS_NAME_COMPONENT_PROVIDER);

        MethodSpec methodSpec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ParameterSpec.builder(typeName, FileGenerationConstants.FIELD_NAME_PROVIDER_OBJECT, Modifier.FINAL).build())
                .addStatement("this.$L = $L", FileGenerationConstants.FIELD_NAME_PROVIDER_OBJECT, FileGenerationConstants.FIELD_NAME_PROVIDER_OBJECT)
                .build();

        return methodSpec;
    }
}
