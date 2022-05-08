package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.service.filter.Filter;
import cu.sld.ucmgt.directory.service.filter.RangeFilter;
import cu.sld.ucmgt.directory.service.filter.StringFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base service for constructing and executing complex queries.
 *
 * @param <E> the type of the entity which is queried.
 */
@Transactional(readOnly = true)
public class QueryService<E> {

    /**
     * Helper function to return a specification for filtering on a single field, where equality, and null/non-null
     * conditions are supported.
     *
     * @param filter the individual attribute filter coming from the frontend.
     * @param field  the JPA static metamodel representing the field.
     * @param <X>    The type of the attribute which is filtered.
     * @return a Specification
     */
    protected <X> Specification<E> buildSpecification(Filter<X> filter, SingularAttribute<? super E, X> field) {
        return buildSpecification(filter, root -> root.get(field));
    }

    /**
     * Helper function to return a specification for filtering on a single field, where equality, and null/non-null
     * conditions are supported.
     *
     * @param filter            the individual attribute filter coming from the frontend.
     * @param metaClassFunction the function, which navigates from the current entity to a column, for which the filter applies.
     * @param <X>               The type of the attribute which is filtered.
     * @return a Specification
     */
    protected <X> Specification<E> buildSpecification(Filter<X> filter, Function<Root<E>, Expression<X>> metaClassFunction) {
        if (filter.getEquals() != null) {
            return equalsSpecification(metaClassFunction, filter.getEquals());
        } else if (filter.getIn() != null) {
            return valueIn(metaClassFunction, filter.getIn());
        } else if (filter.getNotIn() != null) {
            return valueNotIn(metaClassFunction, filter.getNotIn());
        } else if (filter.getNotEquals() != null) {
            return notEqualsSpecification(metaClassFunction, filter.getNotEquals());
        } else if (filter.getSpecified() != null) {
            return byFieldSpecified(metaClassFunction, filter.getSpecified());
        }
        return null;
    }

    /**
     * Helper function to return a specification for filtering on a {@link java.lang.String} field, where equality, containment,
     * and null/non-null conditions are supported.
     *
     * @param filter the individual attribute filter coming from the frontend.
     * @param field  the JPA static metamodel representing the field.
     * @return a Specification
     */
    protected Specification<E> buildStringSpecification(StringFilter filter, SingularAttribute<? super E, String> field) {
        return buildSpecification(filter, root -> root.get(field));
    }

    /**
     * Helper function to return a specification for filtering on a {@link java.lang.String} field, where equality, containment,
     * and null/non-null conditions are supported.
     *
     * @param filter            the individual attribute filter coming from the frontend.
     * @param metaClassFunction lambda, which based on a Root&lt;ENTITY&gt; returns Expression - basically picks a column
     * @return a Specification
     */
    protected Specification<E> buildSpecification(StringFilter filter, Function<Root<E>, Expression<String>> metaClassFunction) {
        if (filter.getEquals() != null) {
            return equalsSpecification(metaClassFunction, filter.getEquals());
        } else if (filter.getIn() != null) {
            return valueIn(metaClassFunction, filter.getIn());
        } else if (filter.getNotIn() != null) {
            return valueNotIn(metaClassFunction, filter.getNotIn());
        } else if (filter.getContains() != null) {
            return likeUpperSpecification(metaClassFunction, filter.getContains());
        } else if (filter.getDoesNotContain() != null) {
            return doesNotContainSpecification(metaClassFunction, filter.getDoesNotContain());
        } else if (filter.getNotEquals() != null) {
            return notEqualsSpecification(metaClassFunction, filter.getNotEquals());
        } else if (filter.getSpecified() != null) {
            return byFieldSpecified(metaClassFunction, filter.getSpecified());
        }
        return null;
    }


    /**
     * Helper function to return a specification for filtering on a single {@link java.lang.Comparable}, where equality, less
     * than, greater than and less-than-or-equal-to and greater-than-or-equal-to and null/non-null conditions are
     * supported.
     *
     * @param <X>    The type of the attribute which is filtered.
     * @param filter the individual attribute filter coming from the frontend.
     * @param field  the JPA static metamodel representing the field.
     * @return a Specification
     */
    protected <X extends Comparable<? super X>> Specification<E> buildRangeSpecification(RangeFilter<X> filter,
                                                                                         SingularAttribute<? super E, X> field) {
        return buildSpecification(filter, root -> root.get(field));
    }

    /**
     * Helper function to return a specification for filtering on a single {@link java.lang.Comparable}, where equality, less
     * than, greater than and less-than-or-equal-to and greater-than-or-equal-to and null/non-null conditions are
     * supported.
     *
     * @param <X>               The type of the attribute which is filtered.
     * @param filter            the individual attribute filter coming from the frontend.
     * @param metaClassFunction the function, which navigates from the current entity to a column, for which the filter applies.
     * @return a Specification
     */
    protected <X extends Comparable<? super X>> Specification<E> buildSpecification(RangeFilter<X> filter,
                                                                                    Function<Root<E>, Expression<X>> metaClassFunction) {
        if (filter.getEquals() != null) {
            return equalsSpecification(metaClassFunction, filter.getEquals());
        } else if (filter.getIn() != null) {
            return valueIn(metaClassFunction, filter.getIn());
        }

        Specification<E> result = Specification.where(null);
        if (filter.getSpecified() != null) {
            result = result.and(byFieldSpecified(metaClassFunction, filter.getSpecified()));
        }
        if (filter.getNotEquals() != null) {
            result = result.and(notEqualsSpecification(metaClassFunction, filter.getNotEquals()));
        }
        if (filter.getNotIn() != null) {
            result = result.and(valueNotIn(metaClassFunction, filter.getNotIn()));
        }
        if (filter.getGreaterThan() != null) {
            result = result.and(greaterThan(metaClassFunction, filter.getGreaterThan()));
        }
        if (filter.getGreaterThanOrEqual() != null) {
            result = result.and(greaterThanOrEqualTo(metaClassFunction, filter.getGreaterThanOrEqual()));
        }
        if (filter.getLessThan() != null) {
            result = result.and(lessThan(metaClassFunction, filter.getLessThan()));
        }
        if (filter.getLessThanOrEqual() != null) {
            result = result.and(lessThanOrEqualTo(metaClassFunction, filter.getLessThanOrEqual()));
        }
        return result;
    }


    /**
     * Helper function to return a specification for filtering on one-to-many or many-to-many reference.Where equality, less
     * than, greater than and less-than-or-equal-to and greater-than-or-equal-to and null/non-null conditions are
     * supported. Usage:
     * <pre>
     *   Specification&lt;Employee&gt; specByEmployeeId = buildReferringEntitySpecification(criteria.getEmployeeId(),
     * Project_.employees, Employee_.id);
     *   Specification&lt;Employee&gt; specByEmployeeName = buildReferringEntitySpecification(criteria.getEmployeeName(),
     * Project_.project, Project_.name);
     * </pre>
     *
     * @param <X>        The type of the attribute which is filtered.
     * @param filter     the filter object which contains a value, which needs to match or a flag if emptiness is
     *                   checked.
     * @param reference  the attribute of the static metamodel for the referring entity.
     * @param valueField the attribute of the static metamodel of the referred entity, where the equality should be
     *                   checked.
     * @param <OTHER>    The type of the referenced entity.
     * @return a Specification
     */
    protected <OTHER, X extends Comparable<? super X>> Specification<E> buildReferringEntitySpecification(final RangeFilter<X> filter,
                                                                                                               final SetAttribute<E, OTHER> reference,
                                                                                                               final SingularAttribute<OTHER, X> valueField) {
        return buildReferringEntitySpecification(filter, root -> root.join(reference), entity -> entity.get(valueField));
    }

    /**
     * Helper function to return a specification for filtering on one-to-many or many-to-many reference.Where equality, less
     * than, greater than and less-than-or-equal-to and greater-than-or-equal-to and null/non-null conditions are
     * supported. Usage:
     * <pre><code>
     *   Specification&lt;Employee&gt; specByEmployeeId = buildReferringEntitySpecification(
     *          criteria.getEmployeId(),
     *          root -&gt; root.get(Project_.company).join(Company_.employees),
     *          entity -&gt; entity.get(Employee_.id));
     *   Specification&lt;Employee&gt; specByProjectName = buildReferringEntitySpecification(
     *          criteria.getProjectName(),
     *          root -&gt; root.get(Project_.project)
     *          entity -&gt; entity.get(Project_.name));
     * </code>
     * </pre>
     *
     * @param <X>              The type of the attribute which is filtered.
     * @param filter           the filter object which contains a value, which needs to match or a flag if emptiness is
     *                         checked.
     * @param functionToEntity the function, which joins he current entity to the entity set, on which the filtering is applied.
     * @param entityToColumn   the function, which of the static metamodel of the referred entity, where the equality should be
     *                         checked.
     * @param <OTHER>          The type of the referenced entity.
     * @param <MISC>           The type of the entity which is the last before the OTHER in the chain.
     * @return a Specification
     */
    protected <OTHER, MISC, X extends Comparable<? super X>> Specification<E> buildReferringEntitySpecification(
            final RangeFilter<X> filter,
            Function<Root<E>, SetJoin<MISC, OTHER>> functionToEntity,
            Function<SetJoin<MISC, OTHER>, Expression<X>> entityToColumn
    )
    {

        Function<Root<E>, Expression<X>> fused = functionToEntity.andThen(entityToColumn);
        if (filter.getEquals() != null) {
            return equalsSpecification(fused, filter.getEquals());
        } else if (filter.getIn() != null) {
            return valueIn(fused, filter.getIn());
        }
        Specification<E> result = Specification.where(null);
        if (filter.getSpecified() != null) {
            // Interestingly, 'functionToEntity' doesn't work, we need the longer lambda formula
            result = result.and(byFieldSpecified(root -> functionToEntity.apply(root), filter.getSpecified()));
        }
        if (filter.getNotEquals() != null) {
            result = result.and(notEqualsSpecification(fused, filter.getNotEquals()));
        }
        if (filter.getNotIn() != null) {
            result = result.and(valueNotIn(fused, filter.getNotIn()));
        }
        if (filter.getGreaterThan() != null) {
            result = result.and(greaterThan(fused, filter.getGreaterThan()));
        }
        if (filter.getGreaterThanOrEqual() != null) {
            result = result.and(greaterThanOrEqualTo(fused, filter.getGreaterThanOrEqual()));
        }
        if (filter.getLessThan() != null) {
            result = result.and(lessThan(fused, filter.getLessThan()));
        }
        if (filter.getLessThanOrEqual() != null) {
            result = result.and(lessThanOrEqualTo(fused, filter.getLessThanOrEqual()));
        }
        return result;
    }

    /**
     * Helper function to return a specification for filtering on single field casted as {@link java.lang.String}, where equality, containment,
     * and null/non-null conditions are supported.
     *
     * @param filter            the individual attribute filter coming from the frontend.
     * @return a Specification
     */
    protected <X> Specification<E> buildAsStringSpecification(StringFilter filter, SingularAttribute<? super E, X> field) {
        if (filter.getEquals() != null) {
            return equalsSpecification(root -> root.get(field).as(String.class), filter.getEquals());
        } else if (filter.getIn() != null) {
            return valueIn(root -> root.get(field).as(String.class), filter.getIn());
        } else if (filter.getNotIn() != null) {
            return valueNotIn(root -> root.get(field).as(String.class), filter.getNotIn());
        } else if (filter.getContains() != null) {
            return likeUpperSpecification(root -> root.get(field).as(String.class), filter.getContains());
        } else if (filter.getDoesNotContain() != null) {
            return doesNotContainSpecification(root -> root.get(field).as(String.class), filter.getDoesNotContain());
        } else if (filter.getNotEquals() != null) {
            return notEqualsSpecification(root -> root.get(field).as(String.class), filter.getNotEquals());
        } else if (filter.getSpecified() != null) {
            return byFieldSpecified(root -> root.get(field).as(String.class), filter.getSpecified());
        }
        return null;
    }


    /**
     * Generic method, which based on a Root&lt;ENTITY&gt;
     * returns an Expression which type is the same as the given 'value' type.
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value             the actual value to filter for.
     * @param <X>              The type of the attribute which is filtered.
     * @return a Specification.
     */
    protected <X> Specification<E> equalsSpecification(Function<Root<E>, Expression<X>> metaClassFunction, final X value) {
        return (root, query, builder) -> builder.equal(metaClassFunction.apply(root), value);
    }

    /**
     * <p>valueIn.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param values a {@link java.util.Collection} object.
     * @param <X> a X object.
     * @return a {@link Specification} object.
     */
    protected <X> Specification<E> valueIn(Function<Root<E>, Expression<X>> metaClassFunction,
                                                final Collection<X> values) {
        return (root, query, builder) -> {
            In<X> in = builder.in(metaClassFunction.apply(root));
            for (X value : values) {
                in = in.value(value);
            }
            return in;
        };
    }

    /**
     * <p>likeUpperSpecification.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value a {@link java.lang.String} object.
     * @return a {@link Specification} object.
     */
    protected Specification<E> likeUpperSpecification(Function<Root<E>, Expression<String>> metaClassFunction, final String value) {
        return (root, query, builder) -> {
            List<String> tokens = getTokensWithCollection(value);
            return builder.or(tokens.stream()
                    .map(token -> builder.like(builder.upper(metaClassFunction.apply(root)), wrapLikeQuery(token)))
                    .toArray(Predicate[]::new));
        };
    }

    /**
     * <p>doesNotContainSpecification.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value a {@link java.lang.String} object.
     * @return a {@link Specification} object.
     */
    protected Specification<E> doesNotContainSpecification(Function<Root<E>, Expression<String>> metaClassFunction, final String value) {
        return (root, query, builder) -> {
            List<String> tokens = getTokensWithCollection(value);
            return builder.not(builder.or(tokens.stream()
                                .map(token -> builder.like(builder.upper(metaClassFunction.apply(root)), wrapLikeQuery(token)))
                                .toArray(Predicate[]::new)));
        };
    }

    /**
     * <p>valueNotIn.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param values a {@link java.util.Collection} object.
     * @param <X> The type of the attribute which is filtered.
     * @return a {@link Specification} object.
     */
    protected <X> Specification<E> valueNotIn(Function<Root<E>, Expression<X>> metaClassFunction,
                                                   final Collection<X> values) {
        return (root, query, builder) -> {
            In<X> in = builder.in(metaClassFunction.apply(root));
            for (X value : values) {
                in = in.value(value);
            }
            return builder.not(in);
        };
    }

    /**
     * Generic method, which based on a Root&lt;ENTITY&gt; returns an Expression which type is the same as the given 'value' type.
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value             the actual value to exclude for.
     * @param <X>              The type of the attribute which is filtered.
     * @return a {@link Specification} object.
     */
    protected <X> Specification<E> notEqualsSpecification(Function<Root<E>, Expression<X>> metaClassFunction, final X value) {
        return (root, query, builder) -> builder.not(builder.equal(metaClassFunction.apply(root), value));
    }

    /**
     * <p>byFieldSpecified.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param specified a boolean.
     * @param <X>  the type of the attribute which is filtered.
     * @return a {@link Specification} object.
     */
    protected <X> Specification<E> byFieldSpecified(Function<Root<E>, Expression<X>> metaClassFunction,
                                                         final boolean specified) {
        return specified ?
                (root, query, builder) -> builder.isNotNull(metaClassFunction.apply(root)) :
                (root, query, builder) -> builder.isNull(metaClassFunction.apply(root));
    }

    /**
     * <p>greaterThan.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value a X object.
     * @param <X> the type of the attribute which is filtered.
     * @return a {@link Specification} object.
     */
    protected <X extends Comparable<? super X>> Specification<E> greaterThan(Function<Root<E>, Expression<X>> metaClassFunction,
                                                                                  final X value) {
        return (root, query, builder) -> builder.greaterThan(metaClassFunction.apply(root), value);
    }

    /**
     * <p>greaterThanOrEqualTo.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value a X object.
     * @param <X> the type of the attribute which is filtered.
     * @return a {@link Specification} object.
     */
    protected <X extends Comparable<? super X>> Specification<E> greaterThanOrEqualTo(Function<Root<E>, Expression<X>> metaClassFunction,
                                                                                           final X value) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(metaClassFunction.apply(root), value);
    }

    /**
     * <p>lessThanOrEqualTo.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value a X object.
     * @param <X> the type of the attribute which is filtered.
     * @return a {@link Specification} object.
     */
    protected <X extends Comparable<? super X>> Specification<E> lessThanOrEqualTo(Function<Root<E>, Expression<X>> metaClassFunction,
                                                                                        final X value) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(metaClassFunction.apply(root), value);
    }

    /**
     * <p>lessThan.</p>
     *
     * @param metaClassFunction function which returns the column which is used for filtering.
     * @param value a X object.
     * @param <X> the type of the attribute which is filtered.
     * @return a {@link Specification} object.
     */
    protected <X extends Comparable<? super X>> Specification<E> lessThan(Function<Root<E>, Expression<X>> metaClassFunction,
                                                                               final X value) {
        return (root, query, builder) -> builder.lessThan(metaClassFunction.apply(root), value);
    }

    /**
     * <p>wrapLikeQuery.</p>
     *
     * @param txt a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    protected String wrapLikeQuery(String txt) {
        return "%" + txt.toUpperCase() + '%';
    }

    protected List<String> getTokensWithCollection(String str) {
        return getTokensWithCollection(str, null);
    }

    protected List<String> getTokensWithCollection(String str, String delim) {
        return Collections.list(null == delim ? new StringTokenizer(str): new StringTokenizer(str, delim)).stream()
                .map(token -> (String) token)
                .collect(Collectors.toList());
    }
}
