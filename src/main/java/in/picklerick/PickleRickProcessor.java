package in.picklerick;

import com.google.auto.service.AutoService;
import in.picklerick.codegeneration.*;
import in.picklerick.util.Util;
import in.picklerick.validation.code.*;
import in.picklerick.validation.dependency.DependencyGraphValidator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by ajitesh on 06/11/18.
 */

@AutoService(Processor.class)
public class PickleRickProcessor extends AbstractProcessor {

    private Messager messager;
    private Types typesUtil;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {

        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.typesUtil = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.size() == 0) {
            return true;
        }

        ProcessVariables processVariables = new ProcessVariables(roundEnv, this.typesUtil, this.filer, new HashMap<>());

        try {

            MultiStepProcessor codeValidator = new MultiStepProcessor();

            codeValidator
                    .add(new MethodAnnotationValidator())
                    .add(new ComponentValidator())
                    .add(new PrimitiveTypeInjectionValidator())
                    .add(new InnerClassComponentValidator())
                    .add(new InnerClassInjectionValidator())
                    .add(new NamedComponentUniquenessValidator())
                    .add(new SetupValidator())
                    .add(new DependencyGraphValidator())
                    .process(processVariables);

            MultiStepProcessor codeGenerator = new MultiStepProcessor();

            codeGenerator
                    .add(new SetterCodeGenerator())
                    .add(new AbstractComponentProviderClassGenerator())
                    .add(new SetupProviderCodeGenerator())
                    .add(new ComponentInjectorInterfaceGenerator())
                    .add(new AbstractComponentInjectorGenerator())
                    .add(new SetupInjectorCodeGenerator())
                    .add(new PickRickCodeGenerator())
                    .process(processVariables);

        } catch (Exception ex) {

            String error = ex.getMessage();
            if (Util.isBlank(error)) {
                error = ex.toString();
            }

            error = "PickleRick: " + error;

            messager.printMessage(Diagnostic.Kind.ERROR, error);
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SupportedAnnotations.getAll();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}

//        messager.printMessage(Diagnostic.Kind.NOTE, "PickleRick started processing");

//            try {
//                JavaFile javaFile = JavaFile.builder(component.getPackageName(), component.getTypeSpec()).build();
//                javaFile.writeTo(filer);
//            }catch (Exception ex){
//                messager.printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
//            }


//    Set<AnnotatedComponent> allComponents = new HashSet<>();
//
//        for (Element element : componentElements) {
//
//                TypeElement componentElement = (TypeElement) element;
//                AnnotatedComponent component = new AnnotatedComponent(componentElement);
//                this.generatedComponentMap.put(component.getActualFullName(), component);
//                }
//
//                for (Element element : namedComponentElements) {
//
//                TypeElement componentElement = (TypeElement) element;
//                AnnotatedComponent component = new AnnotatedComponent(componentElement);
//                this.generatedComponentMap.put(component.getActualFullName(), component);
//                }
//
//                allComponents = new HashSet<AnnotatedComponent>(this.generatedComponentMap.values());