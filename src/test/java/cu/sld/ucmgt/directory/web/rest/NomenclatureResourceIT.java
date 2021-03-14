package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.DirectoryApp;
import cu.sld.ucmgt.directory.TestUtil;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import cu.sld.ucmgt.directory.domain.*;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.StudentIndex;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import cu.sld.ucmgt.directory.service.dto.StudentDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import cu.sld.ucmgt.directory.service.mapper.NomenclatureMapper;
import cu.sld.ucmgt.directory.service.mapper.StudentMapper;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link NomenclatureResource} REST controller.
 */
@WithMockUser
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DirectoryApp.class, TestSecurityConfiguration.class})
public class NomenclatureResourceIT {

    private static final String UPDATE_NAME = "La Habana";
    private static final String DEFAULT_NAME = "Marianao";

    private static final Boolean UPDATE_ACTIVE = false;
    private static final Boolean DEFAULT_ACTIVE = true;

    private static final String UPDATE_DESCRIPTION = "provincia";
    private static final String DEFAULT_DESCRIPTION = "municipio";

    private static final NomenclatureType DEFAULT_DISCRIMINATOR = NomenclatureType.DISTRITO;

    private Nomenclature nomenclature;

    private Nomenclature nomenclatureParentDistrict;

    @Autowired
    private NomenclatureMapper mapper;

    @Autowired
    private NomenclatureRepository repository;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        elasticsearchOperations.deleteIndex(StudentIndex.class);
        elasticsearchOperations.deleteIndex(EmployeeIndex.class);
        nomenclatureParentDistrict = new Nomenclature();
        nomenclatureParentDistrict.setName(UPDATE_NAME);
        nomenclatureParentDistrict.setActive(DEFAULT_ACTIVE);
        nomenclatureParentDistrict.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureParentDistrict.setDescription(DEFAULT_DESCRIPTION);
        repository.save(nomenclatureParentDistrict);

        nomenclature = new Nomenclature();
        nomenclature.setName(DEFAULT_NAME);
        nomenclature.setActive(DEFAULT_ACTIVE);
        nomenclature.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclature.setDescription(DEFAULT_DESCRIPTION);
        nomenclature.setParentDistrict(nomenclatureParentDistrict);
    }

    @Test
    @Transactional
    public void createNomenclature() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();
        nomenclature.setParentDistrict(null);
        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);

        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isCreated());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate + 1);
        // databaseSizeBeforeCreate = 1; position of the last stored nomenclature
        Nomenclature testNomenclature = nomenclatures.get(databaseSizeBeforeCreate);
        assertThat(testNomenclature.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testNomenclature.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testNomenclature.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
    }

    @Test
    @Transactional
    public void createNomenclatureChildDistrictAndNomenclatureParentDistrictAndNomenclatureWithCargoDiscriminator() throws Exception {
        // Initialize database
        Nomenclature otherNomenclature = new Nomenclature();
        otherNomenclature.setName(DEFAULT_NAME);
        otherNomenclature.setActive(UPDATE_ACTIVE);
        otherNomenclature.setDiscriminator(NomenclatureType.CARGO);
        otherNomenclature.setDescription(UPDATE_DESCRIPTION);
        repository.saveAndFlush(otherNomenclature);

        int databaseSizeBeforeCreate = repository.findAll().size();

        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);
        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isCreated());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate + 1);
        // databaseSizeBeforeCreate = 2; position of the last stored nomenclature
        Nomenclature testNomenclature = nomenclatures.get(databaseSizeBeforeCreate);
        assertThat(testNomenclature.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testNomenclature.getParentDistrict()).isEqualTo(nomenclatureParentDistrict);
        assertThat(testNomenclature.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testNomenclature.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
    }

    @Test
    @Transactional
    public void createNomenclatureChildDistrictAndNomenclatureParentDistrict() throws Exception {

        int databaseSizeBeforeCreate = repository.findAll().size();
        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);

        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isCreated());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate + 1);
        // databaseSizeBeforeCreate = 1; position of the last stored nomenclature
        Nomenclature testNomenclature = nomenclatures.get(databaseSizeBeforeCreate);
        assertThat(testNomenclature.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testNomenclature.getParentDistrict()).isEqualTo(nomenclatureParentDistrict);
        assertThat(testNomenclature.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testNomenclature.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
    }

    @Test
    @Transactional
    public void createNomenclatureWithExistingNameAndDiscriminator() throws Exception {
        Nomenclature nomenclatureChild = new Nomenclature();
        nomenclatureChild.setName(DEFAULT_NAME);
        nomenclatureChild.setActive(UPDATE_ACTIVE);
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(UPDATE_DESCRIPTION);
        repository.saveAndFlush(nomenclatureChild);

        int databaseSizeBeforeCreate = repository.findAll().size();
        nomenclature.setParentDistrict(null);

        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);
        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isBadRequest());

        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createNomenclatureWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Nomenclature with an existing ID
        nomenclature.setId(UUID.randomUUID());
        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);

        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkNameIsCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Nomenclature, which fails.
        nomenclature.setName("");
        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);

        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createNomenclatureChildDistrictWithExistingNomenclatureChildDistrictName() throws Exception {
        // Initialize database
        Nomenclature nomenclatureChild = new Nomenclature();
        nomenclatureChild.setName(DEFAULT_NAME);
        nomenclatureChild.setActive(UPDATE_ACTIVE);
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(UPDATE_DESCRIPTION);
        nomenclatureChild.setParentDistrict(nomenclatureParentDistrict);
        repository.saveAndFlush(nomenclatureChild);

        int databaseSizeBeforeCreate = repository.findAll().size();

        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);
        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void updateNomenclature() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Update the Nomenclature
        Nomenclature updatedNomenclature = repository.findById(nomenclature.getId()).get();
        // Disconnect from session so that the updates on updatedNomenclature are not directly saved in db
        em.detach(updatedNomenclature);

        updatedNomenclature.setName(UPDATE_NAME);
        updatedNomenclature.setActive(UPDATE_ACTIVE);
        updatedNomenclature.setDescription(UPDATE_DESCRIPTION);

        NomenclatureDTO nomenclatureDTO = mapper.toDto(updatedNomenclature);
        restMockMvc.perform(put("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isOk());
        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate);
        Nomenclature testNomenclature = nomenclatures.get(nomenclatures.size() - 1);
        assertThat(testNomenclature.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testNomenclature.getActive()).isEqualTo(UPDATE_ACTIVE);
        assertThat(testNomenclature.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
    }


    @Test
    @Transactional
    public void updateSavedNomenclatureWithEmployeeIndex() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // To save Nomenclature with a EmployeeIndex
        Employee employee = getEmployee();
        EmployeeDTO employeeDTO = employeeMapper.toDto(employee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        // Update the Nomenclature
        Nomenclature updatedNomenclature = repository.findById(nomenclature.getId()).get();
        // Disconnect from session so that the updates on updatedNomenclature are not directly saved in db
        em.detach(updatedNomenclature);

        updatedNomenclature.setName(UPDATE_NAME);
        updatedNomenclature.setActive(true);
        updatedNomenclature.setDescription(UPDATE_DESCRIPTION);

        NomenclatureDTO nomenclatureDTO = mapper.toDto(updatedNomenclature);
        restMockMvc.perform(put("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isOk());

        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery()).build();
        List<EmployeeIndex> employees = elasticsearchOperations.queryForList(query, EmployeeIndex.class);
        EmployeeIndex testEmployeeIndexNomenclature = employees.get(employees.size() - 1);
        assertThat(testEmployeeIndexNomenclature.getDistrict()).isEqualTo(UPDATE_NAME);
    }

    private Employee getEmployee() {
        Employee employee = new Employee();
        employee.setName("Cesar");
        employee.setGender(Gender.Masculino);
        employee.setEmail("admin@mail.com");
        employee.setAge(29);
        employee.setServiceYears(5);
        employee.setRegisterNumber("asdasdqweqw");
        employee.setRace("Azul");
        employee.setGraduateYears(2015);
        employee.setCi("91061721000");
        employee.setAddress("Diente y caja de muela");
        employee.setStartDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC));
        employee.setDistrict(nomenclature);
        return employee;
    }

    @Test
    @Transactional
    public void updateSavedNomenclatureWithStudentIndex() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        Student student = getStudent();

        // To save Nomenclature with a StudentIndex
        StudentDTO studentDTO = studentMapper.toDto(student);
        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isCreated());

        // Update the Nomenclature
        Nomenclature updatedNomenclature = repository.findById(nomenclature.getId()).get();
        // Disconnect from session so that the updates on updatedNomenclature are not directly saved in db
        em.detach(updatedNomenclature);

        updatedNomenclature.setName(UPDATE_NAME);
        updatedNomenclature.setActive(true);
        updatedNomenclature.setDescription(UPDATE_DESCRIPTION);

        NomenclatureDTO nomenclatureDTO = mapper.toDto(updatedNomenclature);
        restMockMvc.perform(put("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isOk());

        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery()).build();
        List<StudentIndex> studentIndices = elasticsearchOperations.queryForList(query, StudentIndex.class);
        StudentIndex testStudentIndexNomenclature = studentIndices.get(studentIndices.size() - 1);
        assertThat(testStudentIndexNomenclature.getDistrict()).isEqualTo(UPDATE_NAME);
    }

    @Test
    @Transactional
    public void updateParentDistrictDisableWithEmployeeIndexAndStudentIndex() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);
        int databaseSizeBeforeUpdate = repository.findAll().size();

        // To save Nomenclature with a StudentIndex
        Student student = getStudent();
        StudentDTO studentDTO = studentMapper.toDto(student);
        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isCreated());

        // To save Nomenclature with a EmployeeIndex
        Employee employee = getEmployee();
        EmployeeDTO employeeDTO = employeeMapper.toDto(employee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        // Update the Nomenclature
        Nomenclature updatedNomenclature = repository.findById(nomenclatureParentDistrict.getId()).get();
        // Disconnect from session so that the updates on updatedNomenclature are not directly saved in db
        em.detach(updatedNomenclature);

        updatedNomenclature.setName(DEFAULT_NAME);
        updatedNomenclature.setActive(false);
        updatedNomenclature.setDescription(UPDATE_DESCRIPTION);

        NomenclatureDTO nomenclatureDTO = mapper.toDto(updatedNomenclature);
        restMockMvc.perform(put("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isOk());

        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate);
        assertThat(nomenclatures.get(0).getActive()).isFalse();
        assertThat(nomenclatures.get(1).getActive()).isFalse();
        SearchQuery query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).build();
        List<StudentIndex> studentIndices = elasticsearchOperations.queryForList(query, StudentIndex.class);
        List<EmployeeIndex> employeeIndices = elasticsearchOperations.queryForList(query, EmployeeIndex.class);
        StudentIndex testStudentIndexNomenclature = studentIndices.get(studentIndices.size() - 1);
        StudentIndex testEmployeeIndexNomenclature = studentIndices.get(employeeIndices.size() - 1);
        assertThat(testStudentIndexNomenclature.getDistrict()).isNull();
        assertThat(testStudentIndexNomenclature.getDistrict()).isNull();
        assertThat(testEmployeeIndexNomenclature.getParentDistrict()).isNull();
        assertThat(testStudentIndexNomenclature.getParentDistrict()).isNull();
    }

    private Student getStudent() {
        Student student = new Student();
        student.setName("Cesar");
        student.setGender(Gender.Masculino);
        student.setEmail("admin@mail.com");
        student.setAge(29);
        student.setRace("Azul");
        student.setCi("91061721000");
        student.setAddress("Diente y caja de muela");
        student.setBirthdate(LocalDate.now());
        student.setDistrict(nomenclature);
        student.setClassRoom("3504");
        student.setResidence("5301");
        return student;
    }

    @Test
    @Transactional
    public void updateNonExistingNomenclature() throws Exception {
        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Create the Nomenclature
        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMockMvc.perform(put("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    public void deleteNomenclature() throws Exception {
        // Initialize the database
        nomenclature.setParentDistrict(null);
        repository.saveAndFlush(nomenclature);
        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Clear EmployeeIndex and StudentIndex indices
        elasticsearchOperations.deleteIndex(StudentIndex.class);
        elasticsearchOperations.deleteIndex(EmployeeIndex.class);

        Employee employee = getEmployee();
        // To save Nomenclature with a EmployeeIndex
        EmployeeDTO employeeDTO = employeeMapper.toDto(employee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        Student student = getStudent();
        // To save Nomenclature with a StudentIndex
        StudentDTO studentDTO = studentMapper.toDto(student);
        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isCreated());


        // Delete the nomenclature
        restMockMvc.perform(delete("/api/nomenclatures/{id}", nomenclature.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate - 1);
        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery()).build();
        List<StudentIndex> studentIndices = elasticsearchOperations.queryForList(query, StudentIndex.class);
        List<EmployeeIndex> employeeIndices = elasticsearchOperations.queryForList(query, EmployeeIndex.class);
        EmployeeIndex testEmployeeIndex = employeeIndices.get(employeeIndices.size() -1);
        assertThat(testEmployeeIndex.getDistrict()).isNull();
        StudentIndex testStudentIndex = studentIndices.get(studentIndices.size() -1);
        assertThat(testStudentIndex.getDistrict()).isNull();
    }

    @Test
    public void deleteOnlyNomenclatureChildDistrict() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);
        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Delete the nomenclature
        restMockMvc.perform(delete("/api/nomenclatures/{id}", nomenclature.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate - 1);
    }

    @Test
    public void deleteNomenclatureParentDistrictAndChild() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);
        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Delete the nomenclature
        restMockMvc.perform(delete("/api/nomenclatures/{id}", nomenclatureParentDistrict.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate - 2);
    }

    @Test
    @Transactional
    public void getNomenclature() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        restMockMvc.perform(get("/api/nomenclatures/{id}", nomenclature.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(nomenclature.getId().toString()))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE))
                .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
                .andExpect(jsonPath("$.discriminator").value(DEFAULT_DISCRIMINATOR.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingNomenclature() throws Exception {
        // Get the nomenclature
        restMockMvc.perform(get("/api/nomenclatures/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void getAllNomenclatures() throws Exception {
        // Initialize the database
        repository.deleteAll();
        Nomenclature nomenclatureChild = new Nomenclature();
        nomenclatureChild.setName(DEFAULT_NAME);
        nomenclatureChild.setActive(DEFAULT_ACTIVE);
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(DEFAULT_DESCRIPTION);
        repository.saveAndFlush(nomenclatureChild);

        restMockMvc.perform(get("/api/nomenclatures?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(nomenclatureChild.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.[*].active").value(DEFAULT_ACTIVE))
                .andExpect(jsonPath("$.[*].discriminator").value(DEFAULT_DISCRIMINATOR.toString()))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    @Transactional
    public void getDistrictChildrenByParent() throws Exception{
        // Initialize the database
        Nomenclature districtParent = new Nomenclature();
        districtParent.setName(UPDATE_NAME);
        districtParent.setActive(UPDATE_ACTIVE);
        districtParent.setDescription(UPDATE_DESCRIPTION);
        districtParent.setDiscriminator(NomenclatureType.DISTRITO);
        em.persist(districtParent);

        nomenclature.setParentDistrict(districtParent);
        repository.saveAndFlush(nomenclature);

        restMockMvc.perform(get("/api/nomenclatures/childrenbyparentid/{id}?sort=id,desc",districtParent.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(nomenclature.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.[*].active").value(DEFAULT_ACTIVE))
                .andExpect(jsonPath("$.[*].discriminator").value(DEFAULT_DISCRIMINATOR.toString()))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION));
    }

}
