package tamestaj.annotations;

import tamestaj.Language;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE })
public @interface Suppress {
    Class<? extends Language<?>>[] languages();
}
