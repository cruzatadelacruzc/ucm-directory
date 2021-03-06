package cu.sld.ucmgt.directory.service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;

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

    Set<E> toEntities(Set<D> dtos);

    List<D> toDtos(List<E> entities);

    Set<D> toDtos(Set<E> entities);

    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(D dto, @MappingTarget E entity);

}