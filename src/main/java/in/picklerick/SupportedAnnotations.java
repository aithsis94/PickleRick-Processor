package in.picklerick;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ajitesh on 06/11/18.
 */

public abstract class SupportedAnnotations {

    public static Set<String> getAll(){

        Set<String> annotations = new HashSet<>();

        annotations.add(Setup.class.getName());
        annotations.add(Component.class.getName());
        annotations.add(NamedComponent.class.getName());
        annotations.add(Inject.class.getName());
        annotations.add(NamedInject.class.getName());
        annotations.add(NamedValue.class.getName());

        return annotations;
    }
}
