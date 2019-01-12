package in.picklerick;

import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Types;

public class ProcessVariables {

    private final RoundEnvironment roundEnvironment;
    private final Types typesUtil;
    private final Filer filer;

    private Map<String, Object> variables;

    public ProcessVariables(RoundEnvironment roundEnvironment, Types typesUtil, Filer filer, Map<String, Object> variables) {
        this.roundEnvironment = roundEnvironment;
        this.typesUtil = typesUtil;
        this.filer = filer;
        this.variables = variables;
    }

    public RoundEnvironment getRoundEnvironment() {
        return roundEnvironment;
    }

    public Types getTypesUtil() {
        return typesUtil;
    }

    public Filer getFiler() {
        return filer;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }
}
