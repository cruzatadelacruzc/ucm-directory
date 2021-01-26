package cu.sld.ucmgt.directory.web.rest;

import com.google.common.collect.Lists;
import cu.sld.ucmgt.directory.DirectoryApp;
import cu.sld.ucmgt.directory.TestUtil;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Gender;
import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.repository.NomenclatureRepository;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import cu.sld.ucmgt.directory.service.mapper.NomenclatureMapper;
import org.apache.commons.collections.IteratorUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for the {@link NomenclatureResource} REST controller.
 */
@WithMockUser
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DirectoryApp.class, TestSecurityConfiguration.class})
public class NomenclatureResourceIT {

    private static final String UPDATE_NAME = "zxc";
    private static final String DEFAULT_NAME = "cvb";

    private static final Boolean UPDATE_ACTIVE = false;
    private static final Boolean DEFAULT_ACTIVE = true;

    private static final String UPDATE_DESCRIPTION = "qwerty";
    private static final String DEFAULT_DESCRIPTION = "asdfgh";

    private static final NomenclatureType DEFAULT_DISCRIMINATOR = NomenclatureType.CARGO;

    private Nomenclature nomenclature;

    @Autowired
    private NomenclatureMapper mapper;

    @Autowired
    private NomenclatureRepository repository;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        nomenclature = new Nomenclature();
        nomenclature.setName(DEFAULT_NAME);
        nomenclature.setActive(DEFAULT_ACTIVE);
        nomenclature.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclature.setDescription(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createNomenclature() throws Exception {
        Nomenclature nomenclatureParent = new Nomenclature();
        nomenclatureParent.setName(UPDATE_NAME);
        nomenclatureParent.setActive(UPDATE_ACTIVE);
        nomenclatureParent.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureParent.setDescription(UPDATE_DESCRIPTION);
        repository.saveAndFlush(nomenclatureParent);
        nomenclature.setParent(nomenclatureParent);

        int databaseSizeBeforeCreate = repository.findAll().size();
        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclature);

        restMockMvc.perform(post("/api/nomenclatures").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(nomenclatureDTO)))
                .andExpect(status().isCreated());

        // Validate the Nomenclature in the database
        List<Nomenclature> nomenclatures = repository.findAll();
        assertThat(nomenclatures).hasSize(databaseSizeBeforeCreate + 1);
        Nomenclature testNomenclature = nomenclatures.get(nomenclatures.size() - 1);
        assertThat(testNomenclature.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testNomenclature.getParent().getId()).isEqualTo(nomenclatureParent.getId());
        assertThat(testNomenclature.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testNomenclature.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testNomenclature.getDiscriminator()).isEqualTo(DEFAULT_DISCRIMINATOR);
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
    public void createNomenclatureWithExistingName() throws Exception {
        repository.saveAndFlush(nomenclature);

        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Nomenclature with an existing Name
        Nomenclature nomenclatureCopy = new Nomenclature();
        nomenclatureCopy.setName(DEFAULT_NAME);
        nomenclatureCopy.setActive(DEFAULT_ACTIVE);
        nomenclatureCopy.setDiscriminator(DEFAULT_DISCRIMINATOR);
        nomenclatureCopy.setDescription(DEFAULT_DESCRIPTION);
        NomenclatureDTO nomenclatureDTO = mapper.toDto(nomenclatureCopy);

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
    public void updateSavedNomenclatureWithEmployees() throws Exception {
        // Initialize the database
        repository.saveAndFlush(nomenclature);
        elasticsearchOperations.deleteIndex("employee");

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
        employee.setCharge(nomenclature);


        // To save nomenclature with a employee in elasticsearch
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
        List<Employee> employees = elasticsearchOperations.queryForList(query, Employee.class);
        Employee testEmployeeNomenclatureElasticSearch = employees.get(employees.size() - 1);
        assertThat(testEmployeeNomenclatureElasticSearch.getCharge().getName()).isEqualTo(UPDATE_NAME);
        assertThat(testEmployeeNomenclatureElasticSearch.getCharge().getDescription()).isEqualTo(UPDATE_DESCRIPTION);
        assertThat(testEmployeeNomenclatureElasticSearch.getCharge().getActive()).isEqualTo(true);

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
    public void deleteNomenclature() throws Exception {
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
        repository.saveAndFlush(nomenclature);

        restMockMvc.perform(get("/api/nomenclatures?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(nomenclature.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.[*].active").value(DEFAULT_ACTIVE))
                .andExpect(jsonPath("$.[*].discriminator").value(DEFAULT_DISCRIMINATOR.toString()))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION));
    }

}
