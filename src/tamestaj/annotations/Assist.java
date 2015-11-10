package tamestaj.annotations;

import tamestaj.Language;

import java.lang.annotation.*;

// Not implemented / supported yet!

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@interface Assist {
    Class<? extends Language<?>>[] languages();
}
