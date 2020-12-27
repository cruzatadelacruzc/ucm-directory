package cu.sld.ucmgt.directory.service.mapper;

import java.util.List;

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <D> - DTO type parameter.
 * @param <E> - domain type parameter.
 */
public interface EntityMapper<D,E> {

    D toDto(E entity);

    E toEntity(D dto);

    List<E> toEntities(List<D> dtos);

    List<D> toDtos(List<E> entities);
}