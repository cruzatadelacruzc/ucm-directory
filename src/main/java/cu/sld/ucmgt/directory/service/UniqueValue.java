package cu.sld.ucmgt.directory.service;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {UniqueValueValidator.class})
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface UniqueValue {

    String[] columnNames();

    Class<?> entityClass();

    String[] includeFields() default {};

    String message() default "{validation.unique.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
