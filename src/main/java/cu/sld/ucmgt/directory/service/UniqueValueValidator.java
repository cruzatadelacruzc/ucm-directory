package cu.sld.ucmgt.directory.service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UniqueValueValidator implements ConstraintValidator<UniqueValue, Object> {

    @PersistenceContext
    private EntityManager entityManager;

    private String[] columnNames;
    /**
     * entityClass must be persistence entity(annotated with {@link javax.persistence.Entity})
     */
    private Class<?> entityClass;

    @Override
    public void initialize(UniqueValue constraintAnnotation) {
        this.columnNames = constraintAnnotation.columnNames();
        this.entityClass = constraintAnnotation.entityClass();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (this.entityClass == null || value == null) {
            return false;
        }
        // check if columnNames given exist in entityClass
        List<Field> fieldCurrentClassList = Arrays.stream(value.getClass().getDeclaredFields())
                .peek(field -> field.setAccessible(true))
                .filter(field -> Arrays.asList(columnNames).contains(field.getName()))
                .collect(Collectors.toList());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = cb.createQuery();
        Root<?> root = query.from(entityClass);
        boolean isValid = true;
        for (Field fieldEntityClass : entityClass.getDeclaredFields()) {
            fieldEntityClass.setAccessible(true);
            for (Field fieldCurrentClass : fieldCurrentClassList) {
                if (fieldCurrentClass.getName().equals(fieldEntityClass.getName()) &&
                        fieldCurrentClass.getType().equals(fieldEntityClass.getType())) {
                    try {
                        query.select(root.get(fieldEntityClass.getName())).where(cb.equal(
                                root.get(fieldEntityClass.getName()),
                                fieldCurrentClass.get(value)));
                        TypedQuery<Object> typedQuery = entityManager.createQuery(query);
                        List<Object> result = typedQuery.getResultList();
                        if (!result.isEmpty()) {
                            isValid = false;
                            /**
                             * Set the custom property path for the involved properties because, by default,
                             * the constraint violation for a class-level constraint is reported
                             * at the level of the annotated type, e.g. Employee entity.
                             */
                            constraintValidatorContext.disableDefaultConstraintViolation();
                            constraintValidatorContext
                                    .buildConstraintViolationWithTemplate(
                                            constraintValidatorContext.getDefaultConstraintMessageTemplate())
                                    .addPropertyNode(fieldEntityClass.getName())
                                    .addConstraintViolation();
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return isValid;
    }
}
