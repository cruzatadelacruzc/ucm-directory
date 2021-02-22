package cu.sld.ucmgt.directory.service.mapper;

import java.util.Set;

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <I> - index type parameter.
 * @param <E> - domain type parameter.
 */
public interface IndexMapper<I, E> {

    I toIndex(E entity);

    Set<I> toIndices(Set<E> entities);
}
