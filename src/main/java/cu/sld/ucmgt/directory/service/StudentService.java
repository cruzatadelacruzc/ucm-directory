package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.Student;
import cu.sld.ucmgt.directory.domain.elasticsearch.StudentIndex;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.repository.StudentRepository;
import cu.sld.ucmgt.directory.repository.search.StudentSearchRepository;
import cu.sld.ucmgt.directory.service.dto.StudentDTO;
import cu.sld.ucmgt.directory.service.mapper.StudentIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentMapper mapper;
    private final StudentRepository repository;
    private final StudentIndexMapper studentIndexMapper;
    private final StudentSearchRepository searchRepository;
    private final NomenclatureRepository nomenclatureRepository;

    /**
     * Save a student.
     *
     * @param studentDTO the entity to save.
     * @return the persisted entity.
     */
    public StudentDTO save(StudentDTO studentDTO) {
        log.debug("Request to save Student : {}", studentDTO);
        Student student = mapper.toEntity(studentDTO);
        repository.save(student);
        if (student.getDistrict() != null) {
            nomenclatureRepository.findById(student.getDistrict().getId()).ifPresent(student::setDistrict);
        }
        if (student.getSpecialty() != null) {
            nomenclatureRepository.findById(student.getSpecialty().getId()).ifPresent(student::setSpecialty);
        }
        if (student.getStudyCenter() != null) {
            nomenclatureRepository.findById(student.getStudyCenter().getId()).ifPresent(student::setStudyCenter);
        }
        if (student.getKind() != null) {
            nomenclatureRepository.findById(student.getKind().getId()).ifPresent(student::setKind);
        }
        StudentIndex studentIndex = studentIndexMapper.toIndex(student);
        searchRepository.save(studentIndex);
        return mapper.toDto(student);
    }

    /**
     * Delete the student by id.
     *
     * @param uid the id of the entity.
     */
    public void deleteStudent(UUID uid) {
        log.debug("Request to delete Student : {}", uid);
        repository.deleteById(uid);
        searchRepository.deleteById(uid);
    }

    /**
     * Get one student by uid.
     *
     * @param uid the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<StudentDTO> getStudent(UUID uid) {
        log.debug("Request to get Student : {}", uid);
        return repository.findById(uid).map(mapper::toDto);
    }

    /**
     * Get all the Students.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<StudentDTO> getAllStudents(Pageable pageable) {
        log.debug("Request to get all Students");
        return repository.findAll(pageable).map(mapper::toDto);
    }
}
