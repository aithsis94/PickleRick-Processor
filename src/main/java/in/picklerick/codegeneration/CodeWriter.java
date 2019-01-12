package in.picklerick.codegeneration;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;

public final class CodeWriter {

    public static void writeToFile(Filer filer, String packageName, TypeSpec typeSpec) throws Exception {
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
        javaFile.writeTo(filer);
    }
}
