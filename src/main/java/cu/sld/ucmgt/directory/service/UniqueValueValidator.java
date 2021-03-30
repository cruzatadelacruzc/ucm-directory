package cu.sld.ucmgt.directory.service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UniqueValueValidator implements ConstraintValidator<UniqueValue, Object> {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * includeFields allows check record in database equals to included fields and unique value column for searching.
     * example: when updating an entity in the database, do not trigger a UniqueValue condition
     * when comparing with the same entity to update.
     */
    private String[] includeFields;

    private String[] columnNames;
    /**
     * entityClass must be persistence entity(annotated with {@link javax.persistence.Entity})
     */
    private Class<?> entityClass;

    @Override
    public void initialize(UniqueValue constraintAnnotation) {
        this.columnNames = constraintAnnotation.columnNames();
        this.entityClass = constraintAnnotation.entityClass();
        this.includeFields = constraintAnnotation.includeFields();
    }

    @Override
    public boolean isValid(Object currentClass, ConstraintValidatorContext constraintValidatorContext) {
        if (this.entityClass == null || currentClass == null) {
            return false;
        }

        if (columnNames.length > 0 ) {
            // check if given columnNames exist in entityClass
            List<Field> fieldCurrentClassList = getFields(currentClass, columnNames);

            List<Field> includeFieldsList = new ArrayList<>();
            if (includeFields.length > 0 ) {
                includeFieldsList = getFields(currentClass, includeFields);
            }

            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object> query = cb.createQuery();
            Root<?> root = query.from(entityClass);
            boolean isValid = true;
            try {
                List<Predicate> predicates = new ArrayList<>();
                // check if given includeFields exist in entityClass and create predicates
                for (Field includeField : includeFieldsList) {
                    for (Field fieldEntityClass : entityClass.getDeclaredFields()) {
                        if (includeField.getName().equals(fieldEntityClass.getName()) && includeField.getType().equals(fieldEntityClass.getType())) {
                            predicates.add(cb.notEqual(root.get(fieldEntityClass.getName()), includeField.get(currentClass)));
                        }
                    }
                }

                for (Field fieldEntityClass : entityClass.getDeclaredFields()) {
                    fieldEntityClass.setAccessible(true);
                    for (Field fieldCurrentClass : fieldCurrentClassList) {
                        if (fieldCurrentClass.getName().equals(fieldEntityClass.getName()) && fieldCurrentClass.getType().equals(fieldEntityClass.getType())) {
                            predicates.add(cb.equal(root.get(fieldEntityClass.getName()), fieldCurrentClass.get(currentClass)));
                            query.select(root.get(fieldEntityClass.getName())).where(predicates.toArray(new Predicate[0]));
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

                        }
                    }
                }
                return isValid;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private List<Field> getFields(Object object, String[] stringFields) {
       List<Field> fields = Arrays.stream(object.getClass().getDeclaredFields())
                .peek(field -> field.setAccessible(true))
                .filter(field -> Arrays.asList(stringFields).contains(field.getName()))
                .collect(Collectors.toList());
       if (fields.isEmpty()){
           throw new IllegalArgumentException("Field(s) not exist in " + object.getClass().getName());
       }
       return fields;
    }
}
