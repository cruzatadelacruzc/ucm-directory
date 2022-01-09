package cu.sld.ucmgt.directory.service.mapper;

import cu.sld.ucmgt.directory.domain.Student;
import cu.sld.ucmgt.directory.service.dto.StudentDTO;
import org.mapstruct.*;

import java.util.UUID;

/**
 * Mapper for the entity {@link Student} and its DTO {@link StudentDTO}.
 */
@Mapper(componentModel = "spring", uses = {NomenclatureMapper.class})
public interface StudentMapper extends EntityMapper<StudentDTO, Student>{

    @Mapping(source = "kindId", target = "kind")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(source = "districtId", target = "district")
    @Mapping(source = "specialtyId", target = "specialty")
    @Mapping(source = "studyCenterId", target = "studyCenter")
    Student toEntity(StudentDTO dto);

    @Mapping(source = "kind.id", target = "kindId")
    @Mapping(source = "district.id", target = "districtId")
    @Mapping(source = "specialty.id", target = "specialtyId")
    @Mapping(source = "studyCenter.id", target = "studyCenterId")
    @Mapping(source = "kind.name", target = "kindName")
    @Mapping(source = "district.name", target = "districtName")
    @Mapping(source = "specialty.name", target = "specialtyName")
    @Mapping(source = "studyCenter.name", target = "studyCenterName")
    StudentDTO toDto(Student entity);

    @Mapping(source = "kindId", target = "kind")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(source = "districtId", target = "district")
    @Mapping(source = "specialtyId", target = "specialty")
    @Mapping(source = "studyCenterId", target = "studyCenter")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(StudentDTO dto, @MappingTarget Student student);

    default Student fromId(UUID id) {
        if (id == null) {
            return null;
        }
        Student student = new Student();
        student.setId(id);
        return student;
    }
}
