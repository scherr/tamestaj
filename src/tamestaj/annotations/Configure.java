package tamestaj.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Configure {
    boolean hasRestrictedAcceptAccessibility() default false;
    boolean hasRestrictedStageAccessibility() default false;
    boolean hasRestrictedSuppressAccessibility() default false;
}
