package tamestaj.annotations;

import tamestaj.Language;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface Accept {
    Class<? extends Language<?>>[] languages();

    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    @interface This {
        Class<? extends Language<?>>[] languages();
    }
}
