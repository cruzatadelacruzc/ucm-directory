package cu.sld.ucmgt.directory.web.rest;

import org.junit.jupiter.api.Test;
import cu.sld.ucmgt.directory.TestUtil;
import cu.sld.ucmgt.directory.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import cu.sld.ucmgt.directory.DirectoryApp;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.mapper.EmployeeMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.UUID;
import java.util.List;
import java.time.ZoneId;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.persistence.EntityManager;
import static org.hamcrest.Matchers.hasItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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

    private static final ZonedDateTime UPDATE_END_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final ZonedDateTime DEFAULT_END_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);

    private static final ZonedDateTime UPDATE_START_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final ZonedDateTime DEFAULT_START_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);

    @Autowired
    private EmployeeMapper mapper;

    private Employee employee;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        employee = new Employee();
        employee.setCI(DEFAULT_CI);
        employee.setAge(DEFAULT_AGE);
        employee.setName(DEFAULT_NAME);
        employee.setRace(DEFAULT_RACE);
        employee.setEmail(DEFAULT_EMAIL);
        employee.setGender(DEFAULT_GENDER);
        employee.setAddress(DEFAULT_ADDRESS);
        employee.setEndDate(DEFAULT_END_DATE);
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
        EmployeeDTO employeeDTO = mapper.toDto(employee);

        restMockMvc.perform(post("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isCreated());
        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeCreate + 1);
        Employee testEmployee = employees.get(employees.size() - 1);
        assertThat(testEmployee.getCI()).isEqualTo(DEFAULT_CI);
        assertThat(testEmployee.getAge()).isEqualTo(DEFAULT_AGE);
        assertThat(testEmployee.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testEmployee.getRace()).isEqualTo(DEFAULT_RACE);
        assertThat(testEmployee.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testEmployee.getGender()).isEqualTo(DEFAULT_GENDER);
        assertThat(testEmployee.getAddress()).isEqualTo(DEFAULT_ADDRESS);
        assertThat(testEmployee.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testEmployee.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testEmployee.getServiceYears()).isEqualTo(DEFAULT_SERVICE_YEAR);
        assertThat(testEmployee.getGraduateYears()).isEqualTo(DEFAULT_GRADUATE_YEAR);
        assertThat(testEmployee.getFirstLastName()).isEqualTo(DEFAULT_FIRST_LAST_NAME);
        assertThat(testEmployee.getRegisterNumber()).isEqualTo(DEFAULT_REGISTER_NUMBER);
        assertThat(testEmployee.getSecondLastName()).isEqualTo(DEFAULT_SECOND_LAST_NAME);
        assertThat(testEmployee.getProfessionalNumber()).isEqualTo(DEFAULT_PROFESSIONAL_NUMBER);
        assertThat(testEmployee.getIsGraduatedBySector()).isEqualTo(DEFAULT_IS_GRADUATE_BY_SECTOR);
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
    public void checkNameIsCanNotBlank() throws Exception {
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
    public void checkAddressIsCanNotBlank() throws Exception {
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
    public void checkRaceIsCanNotBlank() throws Exception {
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
    public void checkRegisterNumberIsCanNotBlank() throws Exception {
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
        employee.setCI("1234567891011");
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
        employee.setCI("123456789");
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
    public void checkStartDateIsNotNull() throws Exception {
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
    public void checkGraduateYearsIsCanNotLessThanZero() throws Exception {
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
    public void checkServiceYearsIsCanNotLessThanZero() throws Exception {
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
    public void checkServiceYearsIsCanNotLessThanFourteen() throws Exception {
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
    public void updateEmployee() throws Exception {
        // Initialize the database
        em.persist(employee);
        em.flush();

        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();

        // Update the Employee
        Employee updatedEmployee = em.find(Employee.class, employee.getId());
        // Disconnect from session so that the updates on updatedEmployee are not directly saved in db
        em.detach(updatedEmployee);

        updatedEmployee.setCI(UPDATE_CI);
        updatedEmployee.setAge(UPDATE_AGE);
        updatedEmployee.setName(UPDATE_NAME);
        updatedEmployee.setRace(UPDATE_RACE);
        updatedEmployee.setEmail(UPDATE_EMAIL);
        updatedEmployee.setGender(UPDATE_GENDER);
        updatedEmployee.setAddress(UPDATE_ADDRESS);
        updatedEmployee.setEndDate(UPDATE_END_DATE);
        updatedEmployee.setStartDate(UPDATE_START_DATE);
        updatedEmployee.setServiceYears(UPDATE_SERVICE_YEAR);
        updatedEmployee.setGraduateYears(UPDATE_GRADUATE_YEAR);
        updatedEmployee.setFirstLastName(UPDATE_FIRST_LAST_NAME);
        updatedEmployee.setRegisterNumber(UPDATE_REGISTER_NUMBER);
        updatedEmployee.setSecondLastName(UPDATE_SECOND_LAST_NAME);
        updatedEmployee.setProfessionalNumber(UPDATE_PROFESSIONAL_NUMBER);
        updatedEmployee.setIsGraduatedBySector(UPDATE_IS_GRADUATE_BY_SECTOR);

        EmployeeDTO employeeDTO = mapper.toDto(updatedEmployee);

        restMockMvc.perform(put("/api/employees").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(employeeDTO)))
                .andExpect(status().isOk());

        // Validate the Employee in the database
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeUpdate);
        Employee testEmployee = employees.get(employees.size() - 1);
        assertThat(testEmployee.getCI()).isEqualTo(UPDATE_CI);
        assertThat(testEmployee.getAge()).isEqualTo(UPDATE_AGE);
        assertThat(testEmployee.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testEmployee.getRace()).isEqualTo(UPDATE_RACE);
        assertThat(testEmployee.getEmail()).isEqualTo(UPDATE_EMAIL);
        assertThat(testEmployee.getGender()).isEqualTo(UPDATE_GENDER);
        assertThat(testEmployee.getAddress()).isEqualTo(UPDATE_ADDRESS);
        assertThat(testEmployee.getEndDate()).isEqualTo(UPDATE_END_DATE);
        assertThat(testEmployee.getStartDate()).isEqualTo(UPDATE_START_DATE);
        assertThat(testEmployee.getServiceYears()).isEqualTo(UPDATE_SERVICE_YEAR);
        assertThat(testEmployee.getGraduateYears()).isEqualTo(UPDATE_GRADUATE_YEAR);
        assertThat(testEmployee.getFirstLastName()).isEqualTo(UPDATE_FIRST_LAST_NAME);
        assertThat(testEmployee.getRegisterNumber()).isEqualTo(UPDATE_REGISTER_NUMBER);
        assertThat(testEmployee.getSecondLastName()).isEqualTo(UPDATE_SECOND_LAST_NAME);
        assertThat(testEmployee.getProfessionalNumber()).isEqualTo(UPDATE_PROFESSIONAL_NUMBER);
        assertThat(testEmployee.getIsGraduatedBySector()).isEqualTo(UPDATE_IS_GRADUATE_BY_SECTOR);
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
        em.flush();

        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();

        // Delete the employee
        restMockMvc.perform(delete("/api/employees/{id}", employee.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Employee> employees = TestUtil.findAll(em, Employee.class);
        assertThat(employees).hasSize(databaseSizeBeforeUpdate - 1);

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
                .andExpect(jsonPath("$.endDate").value(TestUtil.sameInstant(DEFAULT_END_DATE)))
                .andExpect(jsonPath("$.isGraduatedBySector").value(DEFAULT_IS_GRADUATE_BY_SECTOR))
                .andExpect(jsonPath("$.startDate").value(TestUtil.sameInstant(DEFAULT_START_DATE)));
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
                .andExpect(jsonPath("$.[*].gender").value(hasItem(DEFAULT_GENDER.toString())))
                .andExpect(jsonPath("$.[*].serviceYears").value(hasItem(DEFAULT_SERVICE_YEAR)))
                .andExpect(jsonPath("$.[*].graduateYears").value(hasItem(DEFAULT_GRADUATE_YEAR)))
                .andExpect(jsonPath("$.[*].firstLastName").value(hasItem(DEFAULT_FIRST_LAST_NAME)))
                .andExpect(jsonPath("$.[*].registerNumber").value(hasItem(DEFAULT_REGISTER_NUMBER)))
                .andExpect(jsonPath("$.[*].secondLastName").value(hasItem(DEFAULT_SECOND_LAST_NAME)))
                .andExpect(jsonPath("$.[*].professionalNumber").value(hasItem(DEFAULT_PROFESSIONAL_NUMBER)))
                .andExpect(jsonPath("$.[*].endDate").value(hasItem(TestUtil.sameInstant(DEFAULT_END_DATE))))
                .andExpect(jsonPath("$.[*].isGraduatedBySector").value(hasItem(DEFAULT_IS_GRADUATE_BY_SECTOR)))
                .andExpect(jsonPath("$.[*].startDate").value(hasItem(TestUtil.sameInstant(DEFAULT_START_DATE))));
    }
}
