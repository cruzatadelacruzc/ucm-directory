package cu.sld.ucmgt.directory.web.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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

    private static final String UPDATE_DESCRIPTION = "provincia";
    private static final String DEFAULT_DESCRIPTION = "municipio";

    private static final NomenclatureType DEFAULT_DISCRIMINATOR = NomenclatureType.DISTRITO;
    private static final NomenclatureType UPDATE_DISCRIMINATOR = NomenclatureType.CARGO;

    private Nomenclature nomenclature;

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

    private ObjectMapper objectMapper;

    @BeforeEach
    public void initTest() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // never do that shit, cause a lot of trouble
        elasticsearchOperations.deleteIndex(StudentIndex.class);
        elasticsearchOperations.deleteIndex(EmployeeIndex.class);

        nomenclature = new Nomenclature();
        nomenclature.setName(DEFAULT_NAME);
        nomenclature.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclature.setDescription(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createNomenclature() throws Exception {
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
        assertThat(testNomenclature.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
    }

    @Test
    @Transactional
    public void createNomenclatureWithCargoDiscriminator() throws Exception {
        // Initialize database
        Nomenclature otherNomenclature = new Nomenclature();
        otherNomenclature.setName(DEFAULT_NAME);
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
        assertThat(testNomenclature.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
    }

    @Test
    @Transactional
    public void createNomenclatureWithExistingNameAndDiscriminator() throws Exception {
        Nomenclature nomenclatureChild = new Nomenclature();
        nomenclatureChild.setName(DEFAULT_NAME);
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(UPDATE_DESCRIPTION);
        repository.saveAndFlush(nomenclatureChild);

        int databaseSizeBeforeCreate = repository.findAll().size();

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
    public void updateNomenclature() throws Exception {
        // Initialize the database
        Nomenclature parentNomenclatureToUpdate = new Nomenclature();
        parentNomenclatureToUpdate.setName("Pepe");
        repository.save(parentNomenclatureToUpdate);
        repository.saveAndFlush(nomenclature);

        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Update the Nomenclature
        Nomenclature updatedNomenclature = repository.findById(nomenclature.getId()).get();
        // Disconnect from session so that the updates on updatedNomenclature are not directly saved in db
        em.detach(updatedNomenclature);

        updatedNomenclature.setName(UPDATE_NAME);
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
        assertThat(testNomenclature.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
    }


    @Test
    @Transactional
    public void updateNomenclatureWithEmployeeIndex() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        int databaseSizeBeforeUpdate = repository.findAll().size();

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
        assertThat(testNomenclature.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);

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
    public void updateNomenclatureWithStudentIndex() throws Exception {
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

    private Student getStudent() {
        Student student = new Student();
        student.setName("Cesar");
        student.setGender(Gender.Masculino);
        student.setEmail("admin@mail.com");
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
    @Transactional
    public void updateNomenclatureAndInsideStudentIndexAndEmployeeIndexWithDiscriminatorSpecialty() throws Exception {
        // Initialize the database
        nomenclature.setDiscriminator(NomenclatureType.ESPECIALIDAD);
        repository.saveAndFlush(nomenclature);
        int databaseSizeBeforeUpdate = repository.findAll().size();

        Employee employee = getEmployee();
        employee.setSpecialty(nomenclature);
        // To save Nomenclature with a EmployeeIndex
        EmployeeDTO employeeDTO = employeeMapper.toDto(employee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        Student student = getStudent();
        student.setSpecialty(nomenclature);
        // To save Nomenclature with a StudentIndex
        StudentDTO studentDTO = studentMapper.toDto(student);
        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isCreated());

        // Update the Nomenclature
        Nomenclature updatedNomenclature = repository.findById(nomenclature.getId()).get();
        updatedNomenclature.setName(UPDATE_NAME);
        updatedNomenclature.setDescription(UPDATE_DESCRIPTION);
        NomenclatureDTO nomenclatureDTO = mapper.toDto(updatedNomenclature);
        restMockMvc.perform(put("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isOk());

        // Validate the database contains one less item
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate);
        Nomenclature testNomenclature = nomenclatures.get(nomenclatures.size() - 1);
        assertThat(testNomenclature.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testNomenclature.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery()).build();
        List<StudentIndex> studentIndices = elasticsearchOperations.queryForList(query, StudentIndex.class);
        List<EmployeeIndex> employeeIndices = elasticsearchOperations.queryForList(query, EmployeeIndex.class);
        EmployeeIndex testEmployeeIndex = employeeIndices.get(employeeIndices.size() -1);
        assertThat(testEmployeeIndex.getSpecialty()).isEqualTo(UPDATE_NAME);
        StudentIndex testStudentIndex = studentIndices.get(studentIndices.size() -1);
        assertThat(testStudentIndex.getSpecialty()).isEqualTo(UPDATE_NAME);
    }

    @Test
    public void deleteNomenclature() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);
        int databaseSizeBeforeUpdate = repository.findAll().size();

        Employee employee = getEmployee();
        // To save Nomenclature with a EmployeeIndex
        EmployeeDTO employeeDTO = employeeMapper.toDto(employee);
        MvcResult resultEmployee = restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String employeeId = objectMapper.readTree(resultEmployee.getResponse().getContentAsByteArray()).get("id").asText();
        assertThat(employeeId).isNotNull();

        Student student = getStudent();
        // To save Nomenclature with a StudentIndex
        StudentDTO studentDTO = studentMapper.toDto(student);
        MvcResult resultStudent = restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String studentId = objectMapper.readTree(resultStudent.getResponse().getContentAsByteArray()).get("id").asText();
        assertThat(studentId).isNotNull();

        // Delete the nomenclature
        restMockMvc.perform(delete("/api/nomenclatures/{id}", nomenclature.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database that the item does not exist
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeUpdate - 1);
        // Validate that StudentIndex and EmployeeIndex contains the district attribute with null value
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
    @Transactional
    public void getNomenclature() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        restMockMvc.perform(get("/api/nomenclatures/{id}", nomenclature.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(nomenclature.getId().toString()))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
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
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(DEFAULT_DESCRIPTION);
        repository.saveAndFlush(nomenclatureChild);

        restMockMvc.perform(get("/api/nomenclatures?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(nomenclatureChild.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.[*].discriminator").value(DEFAULT_DISCRIMINATOR.toString()))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    @Transactional
    public void getAllNomenclaturesUnPaged() throws Exception {
        // Initialize the database
        repository.deleteAll();
        Nomenclature nomenclatureChild = new Nomenclature();
        nomenclatureChild.setName(DEFAULT_NAME);
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(DEFAULT_DESCRIPTION);
        repository.saveAndFlush(nomenclatureChild);

        MvcResult resultNomenclature = restMockMvc.perform(get("/api/nomenclatures?unpaged=true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(nomenclatureChild.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.[*].discriminator").value(DEFAULT_DISCRIMINATOR.toString()))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION))
                .andReturn();

        assertThat(resultNomenclature.getResponse().getHeader("X-Pageable")).isEqualTo("false");
    }

    @Test
    @Transactional
    public void getFilteredNomenclatureByDiscriminatorAndDiscriminatorOrName() throws Exception {
        // Initialize the database
        Nomenclature nomenclatureChild = new Nomenclature();
        nomenclatureChild.setName(DEFAULT_NAME.concat("pepe"));
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(DEFAULT_DESCRIPTION);
        repository.saveAndFlush(nomenclatureChild);
        String filter = "name.contains=%s&description.contains=%s".replaceAll("%s", DEFAULT_NAME.concat("pepe"));
        restMockMvc.perform(get(String.format("/api/nomenclatures/filtered/%s/OR?%s&sort=name,desc", DEFAULT_DISCRIMINATOR, filter)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(nomenclatureChild.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME.concat("pepe")))
                .andExpect(jsonPath("$.[*].discriminator").value(DEFAULT_DISCRIMINATOR.toString()))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    @Transactional
    public void getFilteredNomenclatureByDiscriminatorAndDiscriminatorOrNameUnpaged() throws Exception {
        // Initialize the database
        repository.deleteAll();
        Nomenclature nomenclatureChild = new Nomenclature();
        nomenclatureChild.setName(DEFAULT_NAME);
        nomenclatureChild.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureChild.setDescription(DEFAULT_DESCRIPTION);
        repository.saveAndFlush(nomenclatureChild);

        String filter = "name.contains=%s&description.contains=%s".replaceAll("%s", DEFAULT_NAME);
        MvcResult resultNomenclature = restMockMvc.perform(get(String.format("/api/nomenclatures/filtered/%s/OR?%s&sort=name,desc&unpaged=true", DEFAULT_DISCRIMINATOR, filter)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(nomenclatureChild.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.[*].discriminator").value(DEFAULT_DISCRIMINATOR.toString()))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION))
                .andReturn();

        assertThat(resultNomenclature.getResponse().getHeader("X-Pageable")).isEqualTo("false");
    }

    /**
     * Executes the search with And operator, and checks that the default entity is returned.
     */
    private void defaultNomenclatureShouldBeFoundWithAndOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/nomenclatures/filtered/and?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(nomenclature.getId().toString())))
                .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
                .andExpect(jsonPath("$.[*].discriminator").value(hasItem(DEFAULT_DISCRIMINATOR.toString())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)));
    }

    /**
     * Executes the search with And operator, and checks that the default entity is not returned.
     */
    private void defaultNomenclatureShouldNotBeFoundWithAndOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/nomenclatures/filtered/and?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    /**
     * Executes the search with Or operator, and checks that the default entity is returned.
     */
    private void defaultNomenclatureShouldBeFoundWithOrOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/nomenclatures/filtered/or?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(nomenclature.getId().toString())))
                .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
                .andExpect(jsonPath("$.[*].discriminator").value(hasItem(DEFAULT_DISCRIMINATOR.toString())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)));
    }

    /**
     * Executes the search with Or operator, and checks that the default entity is not returned.
     */
    private void defaultNomenclatureShouldNotBeFoundWithOrOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/nomenclatures/filtered/or?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    public void getNomenclaturesByIdFiltering() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        UUID id = nomenclature.getId();

        defaultNomenclatureShouldBeFoundWithAndOperator("id.equals=" + id);
        defaultNomenclatureShouldBeFoundWithOrOperator("id.equals=" + id);

        defaultNomenclatureShouldNotBeFoundWithAndOperator("id.notEquals=" + id);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("id.notEquals=" + id);


        defaultNomenclatureShouldBeFoundWithAndOperator("id.in=" + id + "," + UUID.randomUUID().toString());
        defaultNomenclatureShouldBeFoundWithOrOperator("id.in=" + id + "," + UUID.randomUUID().toString());

        defaultNomenclatureShouldNotBeFoundWithAndOperator("id.notIn=" + id + "," + UUID.randomUUID().toString());
        defaultNomenclatureShouldNotBeFoundWithOrOperator("id.notIn=" + id + "," + UUID.randomUUID().toString());

        defaultNomenclatureShouldBeFoundWithAndOperator("id.specified=true");
        defaultNomenclatureShouldBeFoundWithOrOperator("id.specified=true");

        defaultNomenclatureShouldNotBeFoundWithAndOperator("id.specified=false");
        defaultNomenclatureShouldNotBeFoundWithOrOperator("id.specified=false");
    }

    @Test
    @Transactional
    void getAllNomenclaturesByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where name equals to DEFAULT_NAME
        defaultNomenclatureShouldBeFoundWithAndOperator("name.equals=" + DEFAULT_NAME);
        defaultNomenclatureShouldBeFoundWithOrOperator("name.equals=" + DEFAULT_NAME);

        // Get all the nomenclatureList where name equals to UPDATE_NAME
        defaultNomenclatureShouldNotBeFoundWithAndOperator("name.equals=" + UPDATE_NAME);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("name.equals=" + UPDATE_NAME);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByNameIsNotEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where name not equals to DEFAULT_NAME
        defaultNomenclatureShouldNotBeFoundWithAndOperator("name.notEquals=" + DEFAULT_NAME);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("name.notEquals=" + DEFAULT_NAME);

        // Get all the nomenclatureList where name not equals to UPDATE_NAME
        defaultNomenclatureShouldBeFoundWithAndOperator("name.notEquals=" + UPDATE_NAME);
        defaultNomenclatureShouldBeFoundWithOrOperator("name.notEquals=" + UPDATE_NAME);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByNameIsInShouldWork() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where name in UPDATED_NAME or DEFAULT_NAME
        defaultNomenclatureShouldBeFoundWithAndOperator("name.in=" + DEFAULT_NAME + "," + UPDATE_NAME);
        defaultNomenclatureShouldBeFoundWithOrOperator("name.in=" + DEFAULT_NAME + "," + UPDATE_NAME);

        // Get all the nomenclatureList where name equals to UPDATE_NAME
        defaultNomenclatureShouldNotBeFoundWithAndOperator("name.in=" + UPDATE_NAME);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("name.in=" + UPDATE_NAME);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where name is not null
        defaultNomenclatureShouldBeFoundWithAndOperator("name.specified=true");
        defaultNomenclatureShouldBeFoundWithOrOperator("name.specified=true");

        // Get all the nomenclatureList where name is null
        defaultNomenclatureShouldNotBeFoundWithAndOperator("name.specified=false");
        defaultNomenclatureShouldNotBeFoundWithOrOperator("name.specified=false");
    }

    @Test
    @Transactional
    void getAllNomenclaturesByNameContainsSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where name contains DEFAULT_NAME
        defaultNomenclatureShouldBeFoundWithAndOperator("name.contains=" + DEFAULT_NAME);
        defaultNomenclatureShouldBeFoundWithOrOperator("name.contains=" + DEFAULT_NAME);

        // Get all the nomenclatureList where name contains UPDATE_NAME
        defaultNomenclatureShouldNotBeFoundWithAndOperator("name.contains=" + UPDATE_NAME);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("name.contains=" + UPDATE_NAME);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByNameNotContainsSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where name does not contain DEFAULT_NAME
        defaultNomenclatureShouldNotBeFoundWithAndOperator("name.doesNotContain=" + DEFAULT_NAME);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("name.doesNotContain=" + DEFAULT_NAME);

        // Get all the nomenclatureList where name does not contain UPDATE_NAME
        defaultNomenclatureShouldBeFoundWithAndOperator("name.doesNotContain=" + UPDATE_NAME);
        defaultNomenclatureShouldBeFoundWithOrOperator("name.doesNotContain=" + UPDATE_NAME);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDescriptionIsEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where description equals to DEFAULT_DESCRIPTION
        defaultNomenclatureShouldBeFoundWithAndOperator("description.equals=" + DEFAULT_DESCRIPTION);
        defaultNomenclatureShouldBeFoundWithOrOperator("description.equals=" + DEFAULT_DESCRIPTION);

        // Get all the nomenclatureList where description equals to UPDATE_DESCRIPTION
        defaultNomenclatureShouldNotBeFoundWithAndOperator("description.equals=" + UPDATE_DESCRIPTION);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("description.equals=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDescriptionIsNotEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where description not equals to UPDATE_DESCRIPTION
        defaultNomenclatureShouldBeFoundWithAndOperator("description.notEquals=" + UPDATE_DESCRIPTION);
        defaultNomenclatureShouldBeFoundWithOrOperator("description.notEquals=" + UPDATE_DESCRIPTION);

        // Get all the nomenclatureList where description not equals to DEFAULT_DESCRIPTION
        defaultNomenclatureShouldNotBeFoundWithAndOperator("description.notEquals=" + DEFAULT_DESCRIPTION);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("description.notEquals=" + DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDescriptionIsInShouldWork() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where description in UPDATE_DESCRIPTION or DEFAULT_NAME
        defaultNomenclatureShouldBeFoundWithAndOperator("description.in=" + DEFAULT_DESCRIPTION + "," + UPDATE_DESCRIPTION);
        defaultNomenclatureShouldBeFoundWithOrOperator("description.in=" + DEFAULT_DESCRIPTION + "," + UPDATE_DESCRIPTION);

        // Get all the nomenclatureList where description equals to DEFAULT_DESCRIPTION
        defaultNomenclatureShouldNotBeFoundWithAndOperator("description.in=" + UPDATE_DESCRIPTION);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("description.in=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDescriptionIsNullOrNotNull() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where description is not null
        defaultNomenclatureShouldBeFoundWithAndOperator("description.specified=true");
        defaultNomenclatureShouldBeFoundWithOrOperator("description.specified=true");

        // Get all the nomenclatureList where description is null
        defaultNomenclatureShouldNotBeFoundWithAndOperator("description.specified=false");
        defaultNomenclatureShouldNotBeFoundWithOrOperator("description.specified=false");
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDescriptionContainsSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where description contains DEFAULT_DESCRIPTION
        defaultNomenclatureShouldBeFoundWithAndOperator("description.contains=" + DEFAULT_DESCRIPTION);
        defaultNomenclatureShouldBeFoundWithOrOperator("description.contains=" + DEFAULT_DESCRIPTION);

        // Get all the nomenclatureList where description contains UPDATE_DESCRIPTION
        defaultNomenclatureShouldNotBeFoundWithAndOperator("description.contains=" + UPDATE_DESCRIPTION);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("description.contains=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDescriptionNotContainsSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where description does not contain UPDATE_NAME
        defaultNomenclatureShouldNotBeFoundWithAndOperator("description.doesNotContain=" + DEFAULT_DESCRIPTION);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("description.doesNotContain=" + DEFAULT_DESCRIPTION);

        // Get all the nomenclatureList where description does not contain UPDATE_DESCRIPTION
        defaultNomenclatureShouldBeFoundWithAndOperator("description.doesNotContain=" + UPDATE_DESCRIPTION);
        defaultNomenclatureShouldBeFoundWithOrOperator("description.doesNotContain=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDiscriminatorIsEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where discriminator equals to DEFAULT_DISCRIMINATOR
        defaultNomenclatureShouldBeFoundWithAndOperator("discriminator.equals=" + DEFAULT_DISCRIMINATOR);
        defaultNomenclatureShouldBeFoundWithOrOperator("discriminator.equals=" + DEFAULT_DISCRIMINATOR);

        // Get all the nomenclatureList where discriminator equals to UPDATE_DISCRIMINATOR
        defaultNomenclatureShouldNotBeFoundWithAndOperator("discriminator.equals=" + UPDATE_DISCRIMINATOR);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("discriminator.equals=" + UPDATE_DISCRIMINATOR);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDiscriminatorIsNotEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where discriminator not equals to DEFAULT_DISCRIMINATOR
        defaultNomenclatureShouldNotBeFoundWithAndOperator("discriminator.notEquals=" + DEFAULT_DISCRIMINATOR);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("discriminator.notEquals=" + DEFAULT_DISCRIMINATOR);

        // Get all the nomenclatureList where discriminator not equals to UPDATE_DISCRIMINATOR
        defaultNomenclatureShouldBeFoundWithAndOperator("discriminator.notEquals=" + UPDATE_DISCRIMINATOR);
        defaultNomenclatureShouldBeFoundWithOrOperator("discriminator.notEquals=" + UPDATE_DISCRIMINATOR);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDiscriminatorIsInShouldWork() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where discriminator in UPDATED_NAME or DEFAULT_NAME
        defaultNomenclatureShouldBeFoundWithAndOperator("discriminator.in=" + DEFAULT_DISCRIMINATOR + "," + UPDATE_DISCRIMINATOR);
        defaultNomenclatureShouldBeFoundWithOrOperator("discriminator.in=" + DEFAULT_DISCRIMINATOR + "," + UPDATE_DISCRIMINATOR);

        // Get all the nomenclatureList where discriminator equals to DEFAULT_NAME
        defaultNomenclatureShouldNotBeFoundWithAndOperator("discriminator.in=" + UPDATE_DISCRIMINATOR);
        defaultNomenclatureShouldNotBeFoundWithOrOperator("discriminator.in=" + UPDATE_DISCRIMINATOR);
    }

    @Test
    @Transactional
    void getAllNomenclaturesByDiscriminatorIsNullOrNotNull() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);

        // Get all the nomenclatureList where discriminator is not null
        defaultNomenclatureShouldBeFoundWithAndOperator("discriminator.specified=true");
        defaultNomenclatureShouldBeFoundWithOrOperator("discriminator.specified=true");

        // Get all the nomenclatureList where discriminator is null
        defaultNomenclatureShouldNotBeFoundWithAndOperator("discriminator.specified=false");
        defaultNomenclatureShouldNotBeFoundWithOrOperator("discriminator.specified=false");
    }

}
