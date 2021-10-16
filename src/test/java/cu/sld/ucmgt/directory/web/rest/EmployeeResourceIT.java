package cu.sld.ucmgt.directory.web.rest;

import com.google.common.collect.ImmutableList;
import cu.sld.ucmgt.directory.DirectoryApp;
import cu.sld.ucmgt.directory.TestUtil;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import cu.sld.ucmgt.directory.domain.*;
import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import cu.sld.ucmgt.directory.repository.search.EmployeeSearchRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeIndexMapper;
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
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link EmployeeResource} REST controller.
 */
@SpringBootTest(classes = {DirectoryApp.class, TestSecurityConfiguration.class})
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
public class EmployeeResourceIT extends PersonIT {

    private static final Integer UPDATE_SERVICE_YEAR = 5;
    private static final Integer DEFAULT_SERVICE_YEAR = 4;

    private static final Integer UPDATE_GRADUATE_YEAR = 29;
    private static final Integer DEFAULT_GRADUATE_YEAR = 28;

    private static final String UPDATE_REGISTER_NUMBER = "8631A";
    private static final String DEFAULT_REGISTER_NUMBER = "8631B";

    private static final String UPDATE_PROFESSIONAL_NUMBER = "5464T";
    private static final String DEFAULT_PROFESSIONAL_NUMBER = "76897D";

    private static final Boolean DEFAULT_IS_GRADUATE_BY_SECTOR = true;
    private static final Boolean UPDATE_IS_GRADUATE_BY_SECTOR = false;

    private static final LocalDateTime UPDATE_END_DATE = LocalDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final LocalDateTime DEFAULT_END_DATE = LocalDateTime.ofInstant(Instant.ofEpochMilli(1L), ZoneOffset.UTC);

    private static final LocalDateTime UPDATE_START_DATE = LocalDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final LocalDateTime DEFAULT_START_DATE = LocalDateTime.ofInstant(Instant.ofEpochMilli(1L), ZoneOffset.UTC);

    private static final String ENDPOINT_RESPONSE_PARAMETERS_KEY = "X-directoryApp-params";

    @Autowired
    private EmployeeMapper mapper;

    private Employee employee;

    @Autowired
    private PhoneMapper phoneMapper;

    @Autowired
    private EmployeeIndexMapper indexMapper;

    @Autowired
    private WorkPlaceMapper workPlaceMapper;

    @Autowired
    private PhoneSearchRepository phoneSearchRepository;

    @Autowired
    private EmployeeSearchRepository employeeSearchRepository;

    @Autowired
    private WorkPlaceSearchRepository workPlaceSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        employee = new Employee();
        employee.setCi(DEFAULT_CI);
        employee.setAge(DEFAULT_AGE);
        employee.setName(DEFAULT_NAME);
        employee.setRace(DEFAULT_RACE);
        employee.setEmail(DEFAULT_EMAIL);
        employee.setGender(DEFAULT_GENDER);
        employee.setAddress(DEFAULT_ADDRESS);
        employee.setEndDate(DEFAULT_END_DATE);
        employee.setBirthdate(DEFAULT_BIRTHDATE);
        employee.setStartDate(DEFAULT_START_DATE);
        employee.setServiceYears(DEFAULT_SERVICE_YEAR);
        employee.setGraduateYears(DEFAULT_GRADUATE_YEAR);
        employee.setFirstLastName(DEFAULT_FIRST_LAST_NAME);
        employee.setRegisterNumber(DEFAULT_REGISTER_NUMBER);
        employee.setSecondLastName(DEFAULT_SECOND_LAST_NAME);
        employee.setProfessionalNumber(DEFAULT_PROFESSIONAL_NUMBER);
        employee.setIsGraduatedBySector(DEFAULT_IS_GRADUATE_BY_SECTOR);
    }

    @Test
    @Transactional
    public void createEmployee() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();
        Nomenclature district = new Nomenclature();
        district.setName("Guantanamo");
        district.setDescription("State");
        district.setDiscriminator(NomenclatureType.DISTRITO);
        em.persist(district);
        employee.setDistrict(district);
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());
        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate + 1);
        Employee testEmployee = employees.get(employees.size() - 1);
        testEmployeeIsCreated(testEmployee);
        assertThat(testEmployee.getDistrict().getId()).isEqualTo(district.getId());

        Iterable<EmployeeIndex> employeeIndices = employeeSearchRepository.findAll();
        EmployeeIndex testEmployeeIndex = employeeIndices.iterator().next();
        testEmployeeIndexIsCreated(testEmployeeIndex);
    }

    private void testEmployeeIsCreated(Employee testEmployee) {
        assertThat(testEmployee.getCi()).isEqualTo(DEFAULT_CI);
        assertThat(testEmployee.getAge()).isEqualTo(DEFAULT_AGE);
        assertThat(testEmployee.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testEmployee.getRace()).isEqualTo(DEFAULT_RACE);
        assertThat(testEmployee.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testEmployee.getGender()).isEqualTo(DEFAULT_GENDER);
        assertThat(testEmployee.getAddress()).isEqualTo(DEFAULT_ADDRESS);
        assertThat(testEmployee.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testEmployee.getBirthdate()).isEqualTo(DEFAULT_BIRTHDATE);
        assertThat(testEmployee.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testEmployee.getServiceYears()).isEqualTo(DEFAULT_SERVICE_YEAR);
        assertThat(testEmployee.getGraduateYears()).isEqualTo(DEFAULT_GRADUATE_YEAR);
        assertThat(testEmployee.getFirstLastName()).isEqualTo(DEFAULT_FIRST_LAST_NAME);
        assertThat(testEmployee.getRegisterNumber()).isEqualTo(DEFAULT_REGISTER_NUMBER);
        assertThat(testEmployee.getSecondLastName()).isEqualTo(DEFAULT_SECOND_LAST_NAME);
        assertThat(testEmployee.getProfessionalNumber()).isEqualTo(DEFAULT_PROFESSIONAL_NUMBER);
        assertThat(testEmployee.getIsGraduatedBySector()).isEqualTo(DEFAULT_IS_GRADUATE_BY_SECTOR);
    }

    private void testEmployeeIndexIsCreated(EmployeeIndex testEmployeeIndex) {
        assertThat(testEmployeeIndex.getCi()).isEqualTo(DEFAULT_CI);
        assertThat(testEmployeeIndex.getAge()).isEqualTo(DEFAULT_AGE);
        assertThat(testEmployeeIndex.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testEmployeeIndex.getRace()).isEqualTo(DEFAULT_RACE);
        assertThat(testEmployeeIndex.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testEmployeeIndex.getGender()).isEqualTo(DEFAULT_GENDER);
        assertThat(testEmployeeIndex.getAddress()).isEqualTo(DEFAULT_ADDRESS);
        assertThat(testEmployeeIndex.getBirthdate()).isEqualTo(DEFAULT_BIRTHDATE);
        assertThat(testEmployeeIndex.getFirstLastName()).isEqualTo(DEFAULT_FIRST_LAST_NAME);
        assertThat(testEmployeeIndex.getRegisterNumber()).isEqualTo(DEFAULT_REGISTER_NUMBER);
        assertThat(testEmployeeIndex.getSecondLastName()).isEqualTo(DEFAULT_SECOND_LAST_NAME);
        assertThat(testEmployeeIndex.getProfessionalNumber()).isEqualTo(DEFAULT_PROFESSIONAL_NUMBER);
    }

    @Test
    @Transactional
    void createEmployeeAndUpdateWorkPlaceIndex() throws Exception {

        // Clear EmployeeIndex, WorkPlaceIndex and PhoneIndex indices
        employeeSearchRepository.deleteAll();
        workPlaceSearchRepository.deleteAll();

        WorkPlace workPlace = createWorkPlaceOfEmployee(Collections.emptySet());

        // To save workplace with a employee in elasticsearch
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(workPlace);
        MvcResult resultWorkPlace = restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String workPlaceId = resultWorkPlace.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(workPlaceId).isNotNull();

        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();
        EmployeeDTO employeeDTO = mapper.toDto(employee);
        employeeDTO.setWorkPlaceId(UUID.fromString(workPlaceId));
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate + 1);
        Employee testEmployee = employees.get(employees.size() - 1);
        testEmployeeIsCreated(testEmployee);

        Iterable<EmployeeIndex> employeeIndices = employeeSearchRepository.findAll();
        EmployeeIndex testEmployeeIndex = employeeIndices.iterator().next();
        testEmployeeIndexIsCreated(testEmployeeIndex);

        Iterable<WorkPlaceIndex> workPlaceIndices = workPlaceSearchRepository.findAll();
        EmployeeIndex testEmployeeIndexWorkPlaceIndex = workPlaceIndices.iterator().next().getEmployees().iterator().next();
        testEmployeeIndexIsCreated(testEmployeeIndexWorkPlaceIndex);
    }

    @Test
    @Transactional
    public void updateEmployeeAndUpdatePhoneIndex() throws Exception {
        // Clear EmployeeIndex, WorkPlaceIndex and PhoneIndex indices
        employeeSearchRepository.deleteAll();
        phoneSearchRepository.deleteAll();

        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();
        EmployeeDTO employeeDTO = mapper.toDto(employee);
        MvcResult resultEmployee = restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate + 1);
        String employeeId = resultEmployee.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(employeeId).isNotNull();

        // To save phone with a employee in elasticsearch
        Phone phone = createPhoneOfEmployee(null);
        PhoneDTO phoneDTO = phoneMapper.toDto(phone);
        phoneDTO.setEmployeeId(UUID.fromString(employeeId));
        databaseSizeBeforeCreate = TestUtil.findAll(em, Phone.class).size();
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());
        // Validate the Phone in the database
        List<Phone> phones = TestUtil.findAll(em, Phone.class);
        assertThat(phones).hasSize(databaseSizeBeforeCreate + 1);

        Employee updatedEmployee = updateEmployeeObj(UUID.fromString(employeeId));
        // to update employee belong to employeeDTO
        employeeDTO = mapper.toDto(updatedEmployee);
       int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();
        restMockMvc.perform(put("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isOk());

        // Validate the Employee in the database
        List<Employee> employeeList = TestUtil.findAll(em, Employee.class);
        assertThat(employeeList).hasSize(databaseSizeBeforeUpdate);
        Employee testEmployee = employees.get(employees.size() - 1);
        testUpdatedEmployee(testEmployee);

        List<PhoneIndex> phonesElasticSearch = phoneSearchRepository.findAllByEmployee_Id(UUID.fromString(employeeId));
        EmployeeIndex testEmployeeIndexPhoneIndex = phonesElasticSearch.get(phonesElasticSearch.size() - 1).getEmployee();
        testEmployeeIndexIsUpdated(testEmployeeIndexPhoneIndex);
    }

    @Test
    @Transactional
    public void createEmployeeWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee with an existing ID
        employee.setId(UUID.randomUUID());
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkEndDateCanNotBeGreaterStartDate() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();
        // create employee with endDate more than startDate, which fails.
        employee.setEndDate(LocalDateTime.now(ZoneId.systemDefault()));
        employee.setStartDate(LocalDateTime.now(ZoneId.systemDefault()).plusDays(2L));

        EmployeeDTO employeeDTO = mapper.toDto(employee);
        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkNameCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setName("");
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkEmailIsMalformed() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setEmail("peepmailcom");
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkAddressCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setAddress("");
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkRaceCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setRace("");
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkRegisterNumberCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setRegisterNumber("");
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkCIWithDigitsQuantityGreaterThanElevenIsIncorrect() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setCi("1234567891011");
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkCIWithDigitsQuantityLessThanElevenIsIncorrect() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setCi("123456789");
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkStartDateCanNotNull() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setStartDate(null);
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkGraduateYearsCanNotLessThanZero() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setGraduateYears(-1);
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkServiceYearsCanNotLessThanZero() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setServiceYears(-1);
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkServiceYearsCanNotLessThanFourteen() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee, which fails.
        employee.setAge(13);
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void updateEmployeeAndEmployeeIndex() throws Exception {
        // clear EmployeeIndex
        employeeSearchRepository.deleteAll();

        EmployeeDTO employeeDTO = mapper.toDto(employee);

        MvcResult resultEmployee = restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String employeeId = resultEmployee.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(employeeId).isNotNull();
        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();

        // Update the Employee
        Employee updatedEmployee = updateEmployeeObj(UUID.fromString(employeeId));
        employeeDTO = mapper.toDto(updatedEmployee);
        restMockMvc.perform(put("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isOk());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeUpdate);
        Employee testEmployee = employees.get(employees.size() - 1);
        testUpdatedEmployee(testEmployee);

        Iterable<EmployeeIndex> employeeIndexList = employeeSearchRepository.findAll();
        assertThat(employeeIndexList).hasSize(databaseSizeBeforeUpdate);
        EmployeeIndex testEmployeeIndex = employeeIndexList.iterator().next();
        testEmployeeIndexIsUpdated(testEmployeeIndex);
    }

    private void testUpdatedEmployee(Employee testEmployee) {
        assertThat(testEmployee.getCi()).isEqualTo(UPDATE_CI);
        assertThat(testEmployee.getAge()).isEqualTo(UPDATE_AGE);
        assertThat(testEmployee.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testEmployee.getRace()).isEqualTo(UPDATE_RACE);
        assertThat(testEmployee.getEmail()).isEqualTo(UPDATE_EMAIL);
        assertThat(testEmployee.getGender()).isEqualTo(UPDATE_GENDER);
        assertThat(testEmployee.getAddress()).isEqualTo(UPDATE_ADDRESS);
        assertThat(testEmployee.getEndDate()).isEqualTo(UPDATE_END_DATE);
        assertThat(testEmployee.getBirthdate()).isEqualTo(UPDATE_BIRTHDATE);
        assertThat(testEmployee.getStartDate()).isEqualTo(UPDATE_START_DATE);
        assertThat(testEmployee.getServiceYears()).isEqualTo(UPDATE_SERVICE_YEAR);
        assertThat(testEmployee.getGraduateYears()).isEqualTo(UPDATE_GRADUATE_YEAR);
        assertThat(testEmployee.getFirstLastName()).isEqualTo(UPDATE_FIRST_LAST_NAME);
        assertThat(testEmployee.getRegisterNumber()).isEqualTo(UPDATE_REGISTER_NUMBER);
        assertThat(testEmployee.getSecondLastName()).isEqualTo(UPDATE_SECOND_LAST_NAME);
        assertThat(testEmployee.getProfessionalNumber()).isEqualTo(UPDATE_PROFESSIONAL_NUMBER);
        assertThat(testEmployee.getIsGraduatedBySector()).isEqualTo(UPDATE_IS_GRADUATE_BY_SECTOR);
    }

    private Employee updateEmployeeObj(UUID employeeId) {
        UUID id = employeeId == null ? employee.getId(): employeeId;
        Employee updatedEmployee = em.find(Employee.class, id);
        // Disconnect from session so that the updates on updatedEmployee are not directly saved in db
        em.detach(updatedEmployee);

        updatedEmployee.setCi(UPDATE_CI);
        updatedEmployee.setAge(UPDATE_AGE);
        updatedEmployee.setName(UPDATE_NAME);
        updatedEmployee.setRace(UPDATE_RACE);
        updatedEmployee.setEmail(UPDATE_EMAIL);
        updatedEmployee.setGender(UPDATE_GENDER);
        updatedEmployee.setAddress(UPDATE_ADDRESS);
        updatedEmployee.setEndDate(UPDATE_END_DATE);
        updatedEmployee.setBirthdate(UPDATE_BIRTHDATE);
        updatedEmployee.setStartDate(UPDATE_START_DATE);
        updatedEmployee.setServiceYears(UPDATE_SERVICE_YEAR);
        updatedEmployee.setGraduateYears(UPDATE_GRADUATE_YEAR);
        updatedEmployee.setFirstLastName(UPDATE_FIRST_LAST_NAME);
        updatedEmployee.setRegisterNumber(UPDATE_REGISTER_NUMBER);
        updatedEmployee.setSecondLastName(UPDATE_SECOND_LAST_NAME);
        updatedEmployee.setProfessionalNumber(UPDATE_PROFESSIONAL_NUMBER);
        updatedEmployee.setIsGraduatedBySector(UPDATE_IS_GRADUATE_BY_SECTOR);
        return updatedEmployee;
    }

    @Test
    @Transactional
    public void updateSavedEmployeeInsidePhonesIndex() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();
        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();

        // Clear EmployeeIndex and PhoneIndex indices
        employeeSearchRepository.deleteAll();
        phoneSearchRepository.deleteAll();


        Phone phone = createPhoneOfEmployee(employee);

        // To save phone with a employee in elasticsearch
        PhoneDTO phoneDTO = phoneMapper.toDto(phone);
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        // Update the Employee
        Employee updatedEmployee = updateEmployeeObj(null);

        // to update employee belong to employeeDTO
        EmployeeDTO employeeDTO = mapper.toDto(updatedEmployee);
        restMockMvc.perform(put("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isOk());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeUpdate);
        Employee testEmployee = employees.get(employees.size() - 1);
        testUpdatedEmployee(testEmployee);

        List<PhoneIndex> phonesElasticSearch = phoneSearchRepository.findAllByEmployee_Id(employee.getId());
        EmployeeIndex testEmployeeIndexPhoneIndex = phonesElasticSearch.get(phonesElasticSearch.size() - 1).getEmployee();
        testEmployeeIndexIsUpdated(testEmployeeIndexPhoneIndex);
    }

    private Phone createPhoneOfEmployee(Employee employee) {
        Phone phone = new Phone();
        phone.setNumber(21382103);
        phone.setActive(true);
        phone.setDescription("Cesar's cell");
        phone.setEmployee(employee);
        return phone;
    }

    private void testEmployeeIndexIsUpdated(EmployeeIndex testEmployeeIndex) {
        assertThat(testEmployeeIndex.getCi()).isEqualTo(UPDATE_CI);
        assertThat(testEmployeeIndex.getAge()).isEqualTo(UPDATE_AGE);
        assertThat(testEmployeeIndex.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testEmployeeIndex.getRace()).isEqualTo(UPDATE_RACE);
        assertThat(testEmployeeIndex.getEmail()).isEqualTo(UPDATE_EMAIL);
        assertThat(testEmployeeIndex.getGender()).isEqualTo(UPDATE_GENDER);
        assertThat(testEmployeeIndex.getAddress()).isEqualTo(UPDATE_ADDRESS);
        assertThat(testEmployeeIndex.getBirthdate()).isEqualTo(UPDATE_BIRTHDATE);
        assertThat(testEmployeeIndex.getFirstLastName()).isEqualTo(UPDATE_FIRST_LAST_NAME);
        assertThat(testEmployeeIndex.getRegisterNumber()).isEqualTo(UPDATE_REGISTER_NUMBER);
        assertThat(testEmployeeIndex.getSecondLastName()).isEqualTo(UPDATE_SECOND_LAST_NAME);
        assertThat(testEmployeeIndex.getProfessionalNumber()).isEqualTo(UPDATE_PROFESSIONAL_NUMBER);
    }

    @Test
    @Transactional
    public void updateSavedEmployeeInsideWorkPlaceIndex() throws Exception {
        // Clear EmployeeIndex and PhoneIndex indices
        employeeSearchRepository.deleteAll();
        workPlaceSearchRepository.deleteAll();

        // Initialize the database and Index
        em.persist(employee);
        em.flush();
        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();

        EmployeeIndex employeeIndex = indexMapper.toIndex(employee);
        employeeSearchRepository.save(employeeIndex);

        WorkPlace workPlace = createWorkPlaceOfEmployee(Collections.singleton(employee));

        // To save workplace with a employee in elasticsearch
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(workPlace);
        restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated());

        // Update the Employee
        Employee updatedEmployee = updateEmployeeObj(null);

        // to update employee belong to employeeDTO
        EmployeeDTO employeeDTO = mapper.toDto(updatedEmployee);
        restMockMvc.perform(put("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isOk());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeUpdate);
        Employee testEmployee = employees.get(employees.size() - 1);
        testUpdatedEmployee(testEmployee);

        Iterable<WorkPlaceIndex> workPlaceIndices = workPlaceSearchRepository.findAll();
        EmployeeIndex testEmployeeIndexWorkPlaceIndex = workPlaceIndices.iterator().next().getEmployees().iterator().next();
        testEmployeeIndexIsUpdated(testEmployeeIndexWorkPlaceIndex);
    }

    private WorkPlace createWorkPlaceOfEmployee(Set<Employee> employeeSet) {
        WorkPlace workPlace = new WorkPlace();
        workPlace.setName("TIC");
        workPlace.setEmail("tic@infomed.sld.cu");
        workPlace.setActive(true);
        workPlace.setDescription("Departamento de las TIC");
        workPlace.setEmployees(employeeSet);
        return workPlace;
    }

    @Test
    @Transactional
    public void updateNonExistingEmployee() throws Exception {
        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();

        // Create the Employee
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMockMvc.perform(put("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteEmployee() throws Exception {
        // Initialize the database
        em.persist(employee);
        Phone employeePhone = createPhoneOfEmployee(employee);
        employee.setPhones(Collections.singleton(employeePhone));
        em.flush();
        // Initialize Index
        EmployeeIndex employeeIndex = indexMapper.toIndex(employee);
        employeeSearchRepository.save(employeeIndex);

        int databaseSizeBeforeDelete = TestUtil.findAll(em, Employee.class).size();
        long indexCountBeforeDelete = employeeSearchRepository.count();

        // Delete the employee
        restMockMvc.perform(delete("/api/employees/{id}", employee.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database and index contains one less item
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeDelete - 1);
        Iterable<EmployeeIndex> employeeIndexIterable = employeeSearchRepository.findAll();
        assertThat(employeeIndexIterable).hasSize((int) (indexCountBeforeDelete - 1));
    }

    @Test
    @Transactional
    public void deleteEmployeeInsidePhoneIndex() throws Exception {
        // Clear EmployeeIndex and PhoneIndex indices
        phoneSearchRepository.deleteAll();
        employeeSearchRepository.deleteAll();

        // Initialize the database and index
        em.persist(employee);
        em.flush();
        EmployeeIndex employeeIndex = indexMapper.toIndex(employee);
        employeeSearchRepository.save(employeeIndex);

        // To save phone with a employee in elasticsearch
        Phone phone = createPhoneOfEmployee(employee);
        PhoneDTO phoneDTO = phoneMapper.toDto(phone);
        MvcResult resultPhone = restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        String phoneId = resultPhone.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(phoneId).isNotNull();

        Phone phone1 = em.find(Phone.class, UUID.fromString(phoneId));
        employee.setPhones(Collections.singleton(phone1));

        // Delete the employee
        restMockMvc.perform(delete("/api/employees/{id}", employee.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        List<PhoneIndex> phonesElasticSearch = phoneSearchRepository.findAllByEmployee_Id(employee.getId());
        assertThat(phonesElasticSearch).hasSize(0);

    }

    @Test
    @Transactional
    public void deleteEmployeeInsideWorkplacesIndex() throws Exception {
        //clear WorkplaceIndex
        workPlaceSearchRepository.deleteAll();

        // Initialize the database
        em.persist(employee);
        Employee employee2 = new Employee();
        employee2.setCi(UPDATE_CI);
        employee2.setAge(UPDATE_AGE);
        employee2.setName(UPDATE_NAME);
        employee2.setRace(UPDATE_RACE);
        employee2.setEmail(UPDATE_EMAIL);
        employee2.setGender(UPDATE_GENDER);
        employee2.setAddress(UPDATE_ADDRESS);
        employee2.setEndDate(UPDATE_END_DATE);
        employee2.setBirthdate(UPDATE_BIRTHDATE);
        employee2.setStartDate(UPDATE_START_DATE);
        employee2.setServiceYears(UPDATE_SERVICE_YEAR);
        employee2.setGraduateYears(UPDATE_GRADUATE_YEAR);
        employee2.setFirstLastName(UPDATE_FIRST_LAST_NAME);
        employee2.setRegisterNumber(UPDATE_REGISTER_NUMBER);
        employee2.setSecondLastName(UPDATE_SECOND_LAST_NAME);
        employee2.setProfessionalNumber(UPDATE_PROFESSIONAL_NUMBER);
        employee2.setIsGraduatedBySector(UPDATE_IS_GRADUATE_BY_SECTOR);
        em.persist(employee2);

        em.flush();

        Set<Employee> employeeSet = new HashSet<>();
        employeeSet.add(employee);
        employeeSet.add(employee2);
        WorkPlace workPlace = createWorkPlaceOfEmployee(employeeSet);
        // Initialize index
        EmployeeIndex employeeIndex = indexMapper.toIndex(employee);
        EmployeeIndex employeeIndex2 = indexMapper.toIndex(employee2);
        employeeSearchRepository.save(employeeIndex);
        employeeSearchRepository.save(employeeIndex2);

        // To save workplace with a employee in elasticsearch
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(workPlace);
        restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated());

        // Delete the employee
        restMockMvc.perform(delete("/api/employees/{id}", employee.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Iterable<WorkPlaceIndex> workPlaceIndices = workPlaceSearchRepository.findAll();
        List<WorkPlaceIndex> workPlaceIndexList = ImmutableList.copyOf(workPlaceIndices);
        WorkPlaceIndex testEmployeeWorkPlaceIndex = workPlaceIndexList.get(workPlaceIndexList.size() - 1);
        assertThat(testEmployeeWorkPlaceIndex.getEmployees()).hasSize(1);
    }


    @Test
    @Transactional
    public void getEmployee() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        restMockMvc.perform(get("/api/employees/{id}", employee.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(employee.getId().toString()))
                .andExpect(jsonPath("$.ci").value(DEFAULT_CI))
                .andExpect(jsonPath("$.age").value(DEFAULT_AGE))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.race").value(DEFAULT_RACE))
                .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
                .andExpect(jsonPath("$.address").value(DEFAULT_ADDRESS))
                .andExpect(jsonPath("$.gender").value(DEFAULT_GENDER.toString()))
                .andExpect(jsonPath("$.serviceYears").value(DEFAULT_SERVICE_YEAR))
                .andExpect(jsonPath("$.graduateYears").value(DEFAULT_GRADUATE_YEAR))
                .andExpect(jsonPath("$.firstLastName").value(DEFAULT_FIRST_LAST_NAME))
                .andExpect(jsonPath("$.registerNumber").value(DEFAULT_REGISTER_NUMBER))
                .andExpect(jsonPath("$.secondLastName").value(DEFAULT_SECOND_LAST_NAME))
                .andExpect(jsonPath("$.professionalNumber").value(DEFAULT_PROFESSIONAL_NUMBER))
                .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
                .andExpect(jsonPath("$.birthdate").value(DEFAULT_BIRTHDATE.toString()))
                .andExpect(jsonPath("$.isGraduatedBySector").value(DEFAULT_IS_GRADUATE_BY_SECTOR))
                .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingEmployee() throws Exception {
        // Get the employee
        restMockMvc.perform(get("/api/employees/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void getAllEmployees() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        // Get all the employees
        restMockMvc.perform(get("/api/employees?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(employee.getId().toString())))
                .andExpect(jsonPath("$.[*].ci").value(hasItem(DEFAULT_CI)))
                .andExpect(jsonPath("$.[*].age").value(hasItem(DEFAULT_AGE)))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
                .andExpect(jsonPath("$.[*].race").value(hasItem(DEFAULT_RACE)))
                .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
                .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
                .andExpect(jsonPath("$.[*].birthdate").value(hasItem(DEFAULT_BIRTHDATE.toString())))
                .andExpect(jsonPath("$.[*].gender").value(hasItem(DEFAULT_GENDER.toString())))
                .andExpect(jsonPath("$.[*].serviceYears").value(hasItem(DEFAULT_SERVICE_YEAR)))
                .andExpect(jsonPath("$.[*].graduateYears").value(hasItem(DEFAULT_GRADUATE_YEAR)))
                .andExpect(jsonPath("$.[*].firstLastName").value(hasItem(DEFAULT_FIRST_LAST_NAME)))
                .andExpect(jsonPath("$.[*].registerNumber").value(hasItem(DEFAULT_REGISTER_NUMBER)))
                .andExpect(jsonPath("$.[*].secondLastName").value(hasItem(DEFAULT_SECOND_LAST_NAME)))
                .andExpect(jsonPath("$.[*].professionalNumber").value(hasItem(DEFAULT_PROFESSIONAL_NUMBER)))
                .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
                .andExpect(jsonPath("$.[*].isGraduatedBySector").value(hasItem(DEFAULT_IS_GRADUATE_BY_SECTOR)))
                .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())));
    }

    /**
     * Executes the search with And operator and checks that the default entity is returned.
     */
    private void defaultEmployeeShouldBeFoundWithAndOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/employees/filtered/and?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(employee.getId().toString())))
                .andExpect(jsonPath("$.[*].ci").value(hasItem(DEFAULT_CI)))
                .andExpect(jsonPath("$.[*].age").value(hasItem(DEFAULT_AGE)))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
                .andExpect(jsonPath("$.[*].race").value(hasItem(DEFAULT_RACE)))
                .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
                .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
                .andExpect(jsonPath("$.[*].gender").value(hasItem(DEFAULT_GENDER.toString())))
                .andExpect(jsonPath("$.[*].serviceYears").value(hasItem(DEFAULT_SERVICE_YEAR)))
                .andExpect(jsonPath("$.[*].graduateYears").value(hasItem(DEFAULT_GRADUATE_YEAR)))
                .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
                .andExpect(jsonPath("$.[*].firstLastName").value(hasItem(DEFAULT_FIRST_LAST_NAME)))
                .andExpect(jsonPath("$.[*].birthdate").value(hasItem(DEFAULT_BIRTHDATE.toString())))
                .andExpect(jsonPath("$.[*].registerNumber").value(hasItem(DEFAULT_REGISTER_NUMBER)))
                .andExpect(jsonPath("$.[*].secondLastName").value(hasItem(DEFAULT_SECOND_LAST_NAME)))
                .andExpect(jsonPath("$.[*].professionalNumber").value(hasItem(DEFAULT_PROFESSIONAL_NUMBER)))
                .andExpect(jsonPath("$.[*].isGraduatedBySector").value(hasItem(DEFAULT_IS_GRADUATE_BY_SECTOR)))
                .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())));
    }

    /**
     * Executes the search with And operator and checks that the default entity is not returned.
     */
    private void defaultEmployeeShouldNotBeFoundWithAndOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/employees/filtered/and?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    /**
     * Executes the search with Or operator and checks that the default entity is returned.
     */
    private void defaultNomenclatureShouldBeFoundWithOrOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/employees/filtered/or?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*].id").value(hasItem(employee.getId().toString())))
                .andExpect(jsonPath("$.[*].ci").value(hasItem(DEFAULT_CI)))
                .andExpect(jsonPath("$.[*].age").value(hasItem(DEFAULT_AGE)))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
                .andExpect(jsonPath("$.[*].race").value(hasItem(DEFAULT_RACE)))
                .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
                .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
                .andExpect(jsonPath("$.[*].gender").value(hasItem(DEFAULT_GENDER.toString())))
                .andExpect(jsonPath("$.[*].serviceYears").value(hasItem(DEFAULT_SERVICE_YEAR)))
                .andExpect(jsonPath("$.[*].graduateYears").value(hasItem(DEFAULT_GRADUATE_YEAR)))
                .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
                .andExpect(jsonPath("$.[*].firstLastName").value(hasItem(DEFAULT_FIRST_LAST_NAME)))
                .andExpect(jsonPath("$.[*].birthdate").value(hasItem(DEFAULT_BIRTHDATE.toString())))
                .andExpect(jsonPath("$.[*].registerNumber").value(hasItem(DEFAULT_REGISTER_NUMBER)))
                .andExpect(jsonPath("$.[*].secondLastName").value(hasItem(DEFAULT_SECOND_LAST_NAME)))
                .andExpect(jsonPath("$.[*].professionalNumber").value(hasItem(DEFAULT_PROFESSIONAL_NUMBER)))
                .andExpect(jsonPath("$.[*].isGraduatedBySector").value(hasItem(DEFAULT_IS_GRADUATE_BY_SECTOR)))
                .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())));
    }

    /**
     * Executes the search with Or operator and checks that the default entity is not returned.
     */
    private void defaultEmployeeShouldNotBeFoundWithOrOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/employees/filtered/or?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    public void getEmployeesByIdFiltering() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        UUID id = employee.getId();

        defaultEmployeeShouldBeFoundWithAndOperator("id.equals=" + id);
        defaultNomenclatureShouldBeFoundWithOrOperator("id.equals=" + id);

        defaultEmployeeShouldNotBeFoundWithAndOperator("id.notEquals=" + id);
        defaultEmployeeShouldNotBeFoundWithOrOperator("id.notEquals=" + id);


        defaultEmployeeShouldBeFoundWithAndOperator("id.in=" + id + "," + UUID.randomUUID().toString());
        defaultNomenclatureShouldBeFoundWithOrOperator("id.in=" + id + "," + UUID.randomUUID().toString());

        defaultEmployeeShouldNotBeFoundWithAndOperator("id.notIn=" + id + "," + UUID.randomUUID().toString());
        defaultEmployeeShouldNotBeFoundWithOrOperator("id.notIn=" + id + "," + UUID.randomUUID().toString());

        defaultEmployeeShouldBeFoundWithAndOperator("id.specified=true");
        defaultNomenclatureShouldBeFoundWithOrOperator("id.specified=true");

        defaultEmployeeShouldNotBeFoundWithAndOperator("id.specified=false");
        defaultEmployeeShouldNotBeFoundWithOrOperator("id.specified=false");
    }

    @Test
    @Transactional
    void getAllEmployeesByBirthdateIsEqualToSomething() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        // Get all the employeeList where birthdate equals to UPDATE_BIRTHDATE
        defaultEmployeeShouldBeFoundWithAndOperator("birthdate.equals=" + DEFAULT_BIRTHDATE);
        defaultNomenclatureShouldBeFoundWithOrOperator("birthdate.equals=" + DEFAULT_BIRTHDATE);

        // Get all the employeeList where birthdate equals to DEFAULT_BIRTHDATE
        defaultEmployeeShouldNotBeFoundWithAndOperator("birthdate.equals=" + UPDATE_BIRTHDATE);
        defaultEmployeeShouldNotBeFoundWithOrOperator("birthdate.equals=" + UPDATE_BIRTHDATE);
    }

    @Test
    @Transactional
    void getAllEmployeesByBirthdateIsNotEqualToSomething() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        // Get all the employeeList where birthdate not equals to UPDATE_BIRTHDATE
        defaultEmployeeShouldNotBeFoundWithAndOperator("birthdate.notEquals=" + DEFAULT_BIRTHDATE);
        defaultEmployeeShouldNotBeFoundWithOrOperator("birthdate.notEquals=" + DEFAULT_BIRTHDATE);

        // Get all the employeeList where birthdate not equals to DEFAULT_BIRTHDATE
        defaultEmployeeShouldBeFoundWithAndOperator("birthdate.notEquals=" + UPDATE_BIRTHDATE);
        defaultNomenclatureShouldBeFoundWithOrOperator("birthdate.notEquals=" + UPDATE_BIRTHDATE);
    }

    @Test
    @Transactional
    void getAllEmployeesByBirthdateIsIsInShouldWork() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        // Get all the employeeList where name not equals to UPDATE_BIRTHDATE
        defaultEmployeeShouldNotBeFoundWithAndOperator("birthdate.in=" + UPDATE_BIRTHDATE + "," + LocalDate.now());
        defaultEmployeeShouldNotBeFoundWithOrOperator("birthdate.in=" + UPDATE_BIRTHDATE + "," + LocalDate.now());

        // Get all the employeeList where name not equals to DEFAULT_BIRTHDATE
        defaultEmployeeShouldBeFoundWithAndOperator("birthdate.in=" + UPDATE_BIRTHDATE + "," + DEFAULT_BIRTHDATE);
        defaultNomenclatureShouldBeFoundWithOrOperator("birthdate.in=" + UPDATE_BIRTHDATE + "," + DEFAULT_BIRTHDATE);
    }

    @Test
    @Transactional
    void getAllEmployeesByBirthdateIsGreaterThanShouldWork() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        // Get all the employeeList where birthdate not equals to UPDATE_BIRTHDATE
        defaultEmployeeShouldNotBeFoundWithAndOperator("birthdate.greaterThan=" + UPDATE_BIRTHDATE);
        defaultEmployeeShouldNotBeFoundWithOrOperator("birthdate.greaterThan=" + UPDATE_BIRTHDATE);

        // Get all the employeeList where birthdate not equals to DEFAULT_BIRTHDATE
        defaultEmployeeShouldBeFoundWithAndOperator("birthdate.greaterThan=" + DEFAULT_BIRTHDATE.minusDays(1L));
        defaultNomenclatureShouldBeFoundWithOrOperator("birthdate.greaterThan=" + DEFAULT_BIRTHDATE.minusDays(1L));
    }

    @Test
    @Transactional
    void getAllEmployeesByGenderIsGreaterThanShouldWork() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        // Get all the employeeList where gender not equals to UPDATE_GENDER
        defaultEmployeeShouldNotBeFoundWithAndOperator("gender.equals=" + UPDATE_GENDER);
        defaultEmployeeShouldNotBeFoundWithOrOperator("gender.equals=" + UPDATE_GENDER);

        // Get all the employeeList where gender not equals to DEFAULT_GENDER
        defaultEmployeeShouldBeFoundWithAndOperator("gender.equals=" + DEFAULT_GENDER);
        defaultNomenclatureShouldBeFoundWithOrOperator("gender.equals=" + DEFAULT_GENDER);
    }

    @Test
    @Transactional
    void searchAllEmployeesByCIAndNameAndWorkPlaceAndSpecialtyAndRegisterNumberIsIsGreaterThanShouldWork() throws Exception {
        // Initialize the database
        Nomenclature specialty =  new Nomenclature();
        specialty.setName("Medicine");
        specialty.setDiscriminator(NomenclatureType.ESPECIALIDAD);
        specialty.setDescription("Health profession");
        em.persist(specialty);
        employee.setSpecialty(specialty);

        WorkPlace tic = new WorkPlace();
        tic.setName("TIC");
        tic.setActive(true);
        tic.setDescription("Technology of Communication and Information");
        tic.setEmail("tic@mail.com");
        em.persist(tic);
        tic.addEmployee(employee);

        em.persist(employee);
        em.flush();
        String filter = "ci.contains=paramName&registerNumber.contains=paramName&name.contains=paramName" +
                "&specialtyName.contains=paramName&workPlaceName.contains=paramName&page=0&size=20&sort=name,asc";
        // Get all the employeeList where ci, registerNumber, name,specialtyName and workPlaceName not equals to UPDATE_NAME
        defaultEmployeeShouldNotBeFoundWithOrOperator(filter.replaceAll("paramName", UPDATE_NAME));

        // Get all the employeeList where name not equals to DEFAULT_NAME
        defaultNomenclatureShouldBeFoundWithOrOperator(filter.replaceAll("paramName", DEFAULT_NAME));
    }
}
