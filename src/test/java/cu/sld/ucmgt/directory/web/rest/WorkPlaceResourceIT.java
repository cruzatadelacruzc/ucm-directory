package cu.sld.ucmgt.directory.web.rest;

import com.google.common.collect.ImmutableList;
import cu.sld.ucmgt.directory.DirectoryApp;
import cu.sld.ucmgt.directory.TestUtil;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Gender;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import cu.sld.ucmgt.directory.service.mapper.WorkPlaceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link WorkPlaceResource} REST controller.
 */

@WithMockUser
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DirectoryApp.class, TestSecurityConfiguration.class})
public class WorkPlaceResourceIT {

    private static final String UPDATE_NAME = "zxc";
    private static final String DEFAULT_NAME = "cvb";

    private static final Boolean UPDATE_ACTIVE = false;
    private static final Boolean DEFAULT_ACTIVE = true;

    private static final String UPDATE_EMAIL = "admin@mail.com";
    private static final String DEFAULT_EMAIL = "user@mail.com";

    private static final String UPDATE_DESCRIPTION = "qwerty";
    private static final String DEFAULT_DESCRIPTION = "asdfgh";

    private WorkPlace workPlace;

    @Autowired
    private WorkPlaceMapper mapper;

    @Autowired
    private PhoneMapper phoneMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private WorkPlaceSearchRepository searchRepository;

    @Autowired
    private PhoneSearchRepository phoneSearchRepository;

    @Autowired
    private EmployeeSearchRepository employeeSearchRepository;

    private static final String ENDPOINT_RESPONSE_PARAMETERS_KEY = "X-directoryApp-params";

    @Autowired
    private WorkPlaceRepository repository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        workPlace = new WorkPlace();
        workPlace.setName(DEFAULT_NAME);
        workPlace.setActive(DEFAULT_ACTIVE);
        workPlace.setEmail(DEFAULT_EMAIL);
        workPlace.setDescription(DEFAULT_DESCRIPTION);

    }

    @Test
    @Transactional
    public void createWorkPlace() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        Employee employee = getEmployeeObj();
        em.persist(employee);

        WorkPlaceDTO workPlaceDTO = mapper.toDto(workPlace);
        workPlaceDTO.setEmployees(Collections.singleton(employee.getId()));

        restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated());


        // Validate the WorkPlace in the database
        List<WorkPlace> workPlaceList = repository.findAll();
        assertThat(workPlaceList).hasSize(databaseSizeBeforeCreate + 1);
        WorkPlace testWorkPlace = workPlaceList.get(workPlaceList.size() - 1);
        assertThat(testWorkPlace.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testWorkPlace.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testWorkPlace.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    private Employee getEmployeeObj() {
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
        return employee;
    }

    @Test
    @Transactional
    public void createWorkPlaceWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Employee with an existing ID
        workPlace.setId(UUID.randomUUID());
        WorkPlaceDTO workPlaceDTO = mapper.toDto(workPlace);

        restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isBadRequest());


        // Validate the WorkPlace in the database
        List<WorkPlace> workPlaceList = repository.findAll();
        assertThat(workPlaceList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkNameIsCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the WorkPlace, which fails.
        workPlace.setName("");
        WorkPlaceDTO workPlaceDTO = mapper.toDto(workPlace);

        restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isBadRequest());
        // Validate the WorkPlace in the database
        List<WorkPlace> workPlaceList = repository.findAll();
        assertThat(workPlaceList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void updateWorkPlaceAndWorkPlaceIndex() throws Exception {
        // clear WorkPlaceIndex
            searchRepository.deleteAll();
        WorkPlaceDTO workPlaceDTO = mapper.toDto(workPlace);
        MvcResult resultWorkplace = restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        String workplaceId = resultWorkplace.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(workplaceId).isNotNull();

        int databaseSizeBeforeUpdate = repository.findAll().size();
        WorkPlace updatedWorkPlace =  repository.findById(UUID.fromString(workplaceId)).get();
        updatedWorkPlace.setActive(true);
        updatedWorkPlace.setName(UPDATE_NAME);
        updatedWorkPlace.setEmail(UPDATE_EMAIL);
        updatedWorkPlace.setDescription(UPDATE_DESCRIPTION);
        workPlaceDTO = mapper.toDto(updatedWorkPlace);
        restMockMvc.perform(put("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isOk());

        // Validate the Employee in the database
        List<WorkPlace> workPlaceList = repository.findAll();
        assertThat(workPlaceList).hasSize(databaseSizeBeforeUpdate);
        WorkPlace testWorkPlace = workPlaceList.get(workPlaceList.size() - 1);
        assertThat(testWorkPlace.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testWorkPlace.getEmail()).isEqualTo(UPDATE_EMAIL);
        assertThat(testWorkPlace.getActive()).isEqualTo(true);
        assertThat(testWorkPlace.getDescription()).isEqualTo(UPDATE_DESCRIPTION);

        Iterable<WorkPlaceIndex> workPlaceIndexIterable = searchRepository.findAll();
        assertThat(workPlaceIndexIterable).hasSize(databaseSizeBeforeUpdate);
        WorkPlaceIndex testWorkPlaceIndex = workPlaceIndexIterable.iterator().next();
        assertThat(testWorkPlaceIndex.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testWorkPlaceIndex.getEmail()).isEqualTo(UPDATE_EMAIL);
        assertThat(testWorkPlaceIndex.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
    }

    private WorkPlace updateWorkPlaceObj(WorkPlace workPlace) {
        // Update the WorkPlace
        WorkPlace updatedWorkPlace = repository.findById(workPlace.getId()).get();
        // Disconnect from session so that the updates on updatedWorkPlace are not directly saved in db
        em.detach(updatedWorkPlace);

        updatedWorkPlace.setName(UPDATE_NAME);
        updatedWorkPlace.setActive(UPDATE_ACTIVE);
        updatedWorkPlace.setEmail(UPDATE_EMAIL);
        updatedWorkPlace.setDescription(UPDATE_DESCRIPTION);
        return updatedWorkPlace;
    }

    @Test
    @Transactional
    public void updateSavedWorkPlaceWithPhones() throws Exception {
        // Initialize the database
        phoneSearchRepository.deleteAll();
        repository.saveAndFlush(workPlace);

        Phone phone = createPhoneOfWorkPlace(workPlace);

        // To save phone with a workplace in elasticsearch
        PhoneDTO phoneDTO = phoneMapper.toDto(phone);
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        WorkPlace updatedWorkPlace = updateWorkPlaceObj(workPlace);

        // to update workplace belong to phoneDTO
        WorkPlaceDTO workPlaceDTO = mapper.toDto(updatedWorkPlace);
        restMockMvc.perform(put("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isOk());

        List<PhoneIndex> phonesElasticSearch = phoneSearchRepository.findAllByWorkPlace_Id(workPlace.getId());
        PhoneIndex testPhoneWorkPlaceES = phonesElasticSearch.get(phonesElasticSearch.size() - 1);
        assertThat(testPhoneWorkPlaceES.getWorkPlace().getName()).isEqualTo(UPDATE_NAME);
        assertThat(testPhoneWorkPlaceES.getWorkPlace().getEmail()).isEqualTo(UPDATE_EMAIL);
        assertThat(testPhoneWorkPlaceES.getWorkPlace().getDescription()).isEqualTo(UPDATE_DESCRIPTION);
    }

    private Phone createPhoneOfWorkPlace(WorkPlace workPlace) {
        Phone phone = new Phone();
        phone.setNumber(21382103);
        phone.setActive(true);
        phone.setDescription("Maximo's cell");
        phone.setWorkPlace(workPlace);
        return phone;
    }

    @Test
    @Transactional
    public void updateSavedWorkPlaceWithEmployees() throws Exception {
        // Initialize the database
        employeeSearchRepository.deleteAll();
        repository.saveAndFlush(workPlace);

        Employee employee = getEmployeeObj();
        employee.setWorkPlace(workPlace);

        // To save employee with a workplace in elasticsearch
        EmployeeDTO employeeDTO = employeeMapper.toDto(employee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        WorkPlace updatedWorkPlace = updateWorkPlaceObj(workPlace);

        // to update workplace belong to employeeDTO
        WorkPlaceDTO workPlaceDTO = mapper.toDto(updatedWorkPlace);
        restMockMvc.perform(put("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isOk());


        Iterable<EmployeeIndex> employeesElasticSearch = employeeSearchRepository.findAll();
        List<EmployeeIndex> indexList = ImmutableList.copyOf(employeesElasticSearch);
        EmployeeIndex testEmployeeWorkPlaceElasticSearch = indexList.get(indexList.size() - 1);
        assertThat(testEmployeeWorkPlaceElasticSearch.getWorkPlace().getName()).isEqualTo(UPDATE_NAME);
    }

    @Test
    @Transactional
    public void updateNonExistingWorkPlace() throws Exception {
        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Create the WorkPlace
        WorkPlaceDTO workPlaceDTO = mapper.toDto(workPlace);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMockMvc.perform(put("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<WorkPlace> workPlaceList = repository.findAll();
        assertThat(workPlaceList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    public void deleteWorkPlaceInEmployeeIndexAndPhoneIndex() throws Exception {
        // Initialize the database
        phoneSearchRepository.deleteAll();
        employeeSearchRepository.deleteAll();
        repository.saveAndFlush(workPlace);
        int databaseSizeBeforeUpdate = repository.findAll().size();


        Phone workplacePhone = createPhoneOfWorkPlace(workPlace);
        PhoneDTO phoneDTO = phoneMapper.toDto(workplacePhone);
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        Employee workPlaceEmployee = getEmployeeObj();
        workPlaceEmployee.setWorkPlace(workPlace);
        EmployeeDTO employeeDTO = employeeMapper.toDto(workPlaceEmployee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        // Delete the workplace
        restMockMvc.perform(delete("/api/workplaces/{id}", workPlace.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<WorkPlace> workPlaces = TestUtil.findAll(em, WorkPlace.class);
        assertThat(workPlaces).hasSize(databaseSizeBeforeUpdate - 1);

        Iterable<PhoneIndex> phoneIndexList = phoneSearchRepository.findAll();
        assertThat(phoneIndexList).hasSize(0);

        Iterable<EmployeeIndex> employeeIndexList = employeeSearchRepository.findAll();
        EmployeeIndex testEmployeeIndex = employeeIndexList.iterator().next();
        assertThat(testEmployeeIndex.getWorkPlace()).isNull();
    }

    @Test
    public void deleteWorkPlaceAndWorkPlaceIndexAndEmployeeAndEmployeeIndex() throws Exception {
        // Clear EmployeeIndex and WorkPlaceIndex
        searchRepository.deleteAll();
        employeeSearchRepository.deleteAll();

        WorkPlaceDTO workPlaceDTO = mapper.toDto(workPlace);
        restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated());
        List<WorkPlace> databaseSizeAfterCreatedWorkPlace = TestUtil.findAll(em, WorkPlace.class);

        Employee workPlaceEmployee = getEmployeeObj();
        WorkPlace fetchedWorkPlace = databaseSizeAfterCreatedWorkPlace.get(databaseSizeAfterCreatedWorkPlace.size() -1);
        workPlaceEmployee.setWorkPlace(fetchedWorkPlace);
        EmployeeDTO employeeDTO = employeeMapper.toDto(workPlaceEmployee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        // Delete the workplace
        restMockMvc.perform(delete("/api/workplaces/{id}", fetchedWorkPlace.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<WorkPlace> workPlaces = TestUtil.findAll(em, WorkPlace.class);
        assertThat(workPlaces).hasSize(databaseSizeAfterCreatedWorkPlace.size() - 1);

        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        Employee testEmployee = employees.get(employees.size() -1);
        assertThat(testEmployee.getWorkPlace()).isNull();

        Iterable<EmployeeIndex> employeeIndexIterable = employeeSearchRepository.findAll();
        Iterable<WorkPlaceIndex> workPlaceIndexIterable = searchRepository.findAll();
        EmployeeIndex testEmployeeIndex = employeeIndexIterable.iterator().next();
        assertThat(workPlaceIndexIterable).hasSize(0);
        assertThat(testEmployeeIndex.getWorkPlace()).isNull();
    }

    @Test
    @Transactional
    public void getWorkPlace() throws Exception {
        // Initialize the database
        repository.saveAndFlush(workPlace);

        restMockMvc.perform(get("/api/workplaces/{id}", workPlace.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(workPlace.getId().toString()))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE))
                .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    @Transactional
    public void getNonExistingWorkPlace() throws Exception {
        // Get the workplace
        restMockMvc.perform(get("/api/workplaces/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void getAllWorkPlaces() throws Exception {
        // Initialize the database
        repository.saveAndFlush(workPlace);

        restMockMvc.perform(get("/api/workplaces?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(workPlace.getId().toString()))
                .andExpect(jsonPath("$.[*].name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.[*].active").value(DEFAULT_ACTIVE))
                .andExpect(jsonPath("$.[*].description").value(DEFAULT_DESCRIPTION));
    }
}
