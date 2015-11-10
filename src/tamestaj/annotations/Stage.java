package tamestaj.annotations;

import tamestaj.Language;
import tamestaj.StaticInfo;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Stage {
    Class<? extends Language<?>> language();
    boolean isStrict() default false;
    StaticInfo.Element[] staticInfoElements() default {};
}
