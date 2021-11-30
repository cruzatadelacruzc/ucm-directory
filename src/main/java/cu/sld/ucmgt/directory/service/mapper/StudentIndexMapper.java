package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Student;
import cu.sld.ucmgt.directory.domain.elasticsearch.StudentIndex;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the index {@link StudentIndex} and its entity {@link Student}.
 */
@Mapper(componentModel = "spring")
public interface StudentIndexMapper extends IndexMapper<StudentIndex, Student> {

    @Mapping(source = "district.name", target = "district")
    @Mapping(source = "specialty.name", target = "specialty")
    StudentIndex toIndex(Student student);
}
