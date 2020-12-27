package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Student;
import cu.sld.ucmgt.directory.service.dto.StudentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

/**
 * Mapper for the entity {@link Student} and its DTO {@link StudentDTO}.
 */
@Mapper(componentModel = "spring", uses = {NomenclatureMapper.class})
public interface StudentMapper extends EntityMapper<StudentDTO, Student>{

    @Mapping(source = "districtId", target = "district")
    Student toEntity(StudentDTO dto);

    @Mapping(source = "district.id", target = "districtId")
    StudentDTO toDto(Student entity);

    default Student fromId(UUID id) {
        if (id == null) {
            return null;
        }
        Student student = new Student();
        student.setId(id);
        return student;
    }
}
