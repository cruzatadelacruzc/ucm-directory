package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.domain.Nomenclature_;
import cu.sld.ucmgt.directory.domain.Student;
import cu.sld.ucmgt.directory.domain.Student_;
import cu.sld.ucmgt.directory.domain.elasticsearch.StudentIndex;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.repository.StudentRepository;
import cu.sld.ucmgt.directory.repository.search.StudentSearchRepository;
import cu.sld.ucmgt.directory.service.NomenclatureService.SavedNomenclatureEvent;
import cu.sld.ucmgt.directory.service.criteria.StudentCriteria;
import cu.sld.ucmgt.directory.service.dto.StudentDTO;
import cu.sld.ucmgt.directory.service.mapper.StudentIndexMapper;
import cu.sld.ucmgt.directory.service.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.JoinType;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService extends QueryService<Student>{
    private final StudentMapper mapper;
    private final StudentRepository repository;
    private final RestHighLevelClient highLevelClient;
    private static final String INDEX_NAME = "students";
    private final StudentIndexMapper studentIndexMapper;
    private final StudentSearchRepository searchRepository;
    private final NomenclatureRepository nomenclatureRepository;

    /**
     * Check if student exists
     * @param studentId of student
     * @return boolean
     */
    public Boolean exists(UUID studentId) {
        return repository.existsById(studentId);
    }

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
     * Partially update a student, only personal data
     *
     * @param studentDto the entity to update partially.
     * @return the persisted entity.
     */
    public StudentDTO partialUpdate(StudentDTO studentDto){
        Optional<Student> existingStudent =  repository.findById(studentDto.getId());
        if (existingStudent.isPresent()) {
            mapper.partialUpdate(studentDto, existingStudent.get());
            repository.save(existingStudent.get());
            StudentIndex studentIndex = studentIndexMapper.toIndex(existingStudent.get());
            searchRepository.save(studentIndex);
            return mapper.toDto(existingStudent.get());
        } else {
            return null;
        }
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

    /**
     * Add studentIds to filter of student(student with district association or specialty added above) that need
     * to be updated
     * @param savedNomenclatureEvent with saved student data
     */
    @EventListener( condition = "#savedNomenclatureEvent.getUpdatedNomenclature() != null")
    public void updateNomenclatureIntoStudentIndex(SavedNomenclatureEvent savedNomenclatureEvent) {
        log.debug("Listening SavedNomenclatureEvent event to update Nomenclature with ID {} in StudentIndex.",
                savedNomenclatureEvent.getUpdatedNomenclature().getId());
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            savedNomenclatureEvent.getCommonAssociationIds().forEach(commonAssociationIds -> boolQueryBuilder
                    .should(QueryBuilders.matchQuery("id", commonAssociationIds.toString())));
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.TIPO)){
                savedNomenclatureEvent.getUpdatedNomenclature().getStudentsKind()
                        .forEach(student -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", student.getId().toString())));
            }
            if (savedNomenclatureEvent.getUpdatedNomenclature().getDiscriminator().equals(NomenclatureType.CENTRO_ESTUDIO)){
                savedNomenclatureEvent.getUpdatedNomenclature().getStudentsStudyCenter()
                        .forEach(student -> boolQueryBuilder
                                .should(QueryBuilders.matchQuery("id", student.getId().toString())));
            }
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(INDEX_NAME)
                    .setRefresh(true)
                    .setQuery(boolQueryBuilder)
                    .setAbortOnVersionConflict(true)
                    .setScript(new Script(ScriptType.INLINE, "painless", savedNomenclatureEvent.getUpdateCode(),
                            Collections.emptyMap()));
            highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Return a {@link List} of {@link StudentDTO} which matches the criteria from the database.
     *
     * @param operator_union Logical operator to join expression: AND - OR
     * @param criteria       The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    public Page<StudentDTO> findByCriteria(String operator_union, StudentCriteria criteria, Pageable page) {
        final Specification<Student> specification = createSpecification(operator_union, criteria);
        return repository.findAll(specification, page).map(mapper::toDto);
    }

    /**
     * Function to convert {@link StudentCriteria} to a {@link Specification}
     *
     * @param operator_union Logical operator to join expression: AND - OR
     * @param criteria       The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    private Specification<Student> createSpecification(String operator_union, StudentCriteria criteria) {
        Specification<Student> specification = Specification.where(null);
        if (criteria != null) {
            if (operator_union.equalsIgnoreCase("AND")) {
                if (criteria.getId() != null) {
                    specification = specification.and(buildSpecification(criteria.getId(), Student_.id));
                }
                if (criteria.getCi() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getCi(), Student_.ci));
                }
                if (criteria.getAge() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getAge(), Student_.age));
                }
                if (criteria.getName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getName(), Student_.name));
                }
                if (criteria.getRace() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getRace(), Student_.race));
                }
                if (criteria.getEmail() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getEmail(), Student_.email));
                }
                if (criteria.getGender() != null) {
                    specification = specification.and(buildSpecification(criteria.getGender(), Student_.gender));
                }
                if (criteria.getAddress() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getAddress(), Student_.address));
                }
                if (criteria.getDistrictName() != null) {
                    specification = specification.and(buildSpecification(criteria.getDistrictName(),
                            root -> root.join(Student_.district, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getBirthdate() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getBirthdate(), Student_.birthdate));
                }
                if (criteria.getSpecialtyName() != null) {
                    specification = specification.and(buildSpecification(criteria.getSpecialtyName(),
                            root -> root.join(Student_.specialty, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getFirstLastName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getFirstLastName(), Student_.firstLastName));
                }
                if (criteria.getSecondLastName() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getSecondLastName(), Student_.secondLastName));
                }
                if (criteria.getClassRoom() !=null) {
                    specification = specification.and(buildStringSpecification(criteria.getClassRoom(), Student_.classRoom));
                }
                if (criteria.getKindName() != null) {
                    specification = specification.and(buildSpecification(criteria.getKindName(),
                            root -> root.join(Student_.kind, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getResidence() != null) {
                    specification = specification.and(buildStringSpecification(criteria.getResidence(), Student_.residence));
                }
                if (criteria.getStudyCenterName() != null) {
                    specification = specification.and(buildSpecification(criteria.getStudyCenterName(),
                            root -> root.join(Student_.studyCenter, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getUniversityYear() != null) {
                    specification = specification.and(buildRangeSpecification(criteria.getUniversityYear(), Student_.universityYear));
                }
            } else {
                if (criteria.getId() != null) {
                    specification = specification.or(buildSpecification(criteria.getId(), Student_.id));
                }
                if (criteria.getCi() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getCi(), Student_.ci));
                }
                if (criteria.getAge() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getAge(), Student_.age));
                }
                if (criteria.getName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getName(), Student_.name));
                }
                if (criteria.getRace() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getRace(), Student_.race));
                }
                if (criteria.getEmail() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getEmail(), Student_.email));
                }
                if (criteria.getGender() != null) {
                    specification = specification.or(buildSpecification(criteria.getGender(), Student_.gender));
                }
                if (criteria.getAddress() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getAddress(), Student_.address));
                }
                if (criteria.getDistrictName() != null) {
                    specification = specification.or(buildSpecification(criteria.getDistrictName(),
                            root -> root.join(Student_.district, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getBirthdate() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getBirthdate(), Student_.birthdate));
                }
                if (criteria.getSpecialtyName() != null) {
                    specification = specification.or(buildSpecification(criteria.getSpecialtyName(),
                            root -> root.join(Student_.specialty, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getFirstLastName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getFirstLastName(), Student_.firstLastName));
                }
                if (criteria.getSecondLastName() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getSecondLastName(), Student_.secondLastName));
                }
                if (criteria.getClassRoom() !=null) {
                    specification = specification.or(buildStringSpecification(criteria.getClassRoom(), Student_.classRoom));
                }
                if (criteria.getKindName() != null) {
                    specification = specification.or(buildSpecification(criteria.getKindName(),
                            root -> root.join(Student_.kind, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getResidence() != null) {
                    specification = specification.or(buildStringSpecification(criteria.getResidence(), Student_.residence));
                }
                if (criteria.getStudyCenterName() != null) {
                    specification = specification.or(buildSpecification(criteria.getStudyCenterName(),
                            root -> root.join(Student_.studyCenter, JoinType.LEFT).get(Nomenclature_.name)));
                }
                if (criteria.getUniversityYear() != null) {
                    specification = specification.or(buildRangeSpecification(criteria.getUniversityYear(), Student_.universityYear));
                }
            }
        }
        return specification;
    }
}
