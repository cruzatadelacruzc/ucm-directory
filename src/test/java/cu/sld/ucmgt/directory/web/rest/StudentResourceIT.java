package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.DirectoryApp;
import cu.sld.ucmgt.directory.TestUtil;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Nomenclature;
import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.domain.Student;
import cu.sld.ucmgt.directory.service.dto.StudentDTO;
import cu.sld.ucmgt.directory.service.mapper.StudentMapper;
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
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for the {@link StudentResource} REST controller.
 */
@SpringBootTest(classes = {DirectoryApp.class, TestSecurityConfiguration.class})
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
public class StudentResourceIT extends PersonIT {

    private static final String UPDATE_CLASSROOM = "35A";
    private static final String DEFAULT_CLASSROOM = "34B";

    private static final Integer UPDATE_UNIVERSITY_YEAR = 3;
    private static final Integer DEFAULT_UNIVERSITY_YEAR = 2;

    private static final String UPDATE_RESIDENCE = "2A";
    private static final String DEFAULT_RESIDENCE = "3T";

    @Autowired
    private StudentMapper mapper;

    private Student student;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        student = new Student();
        student.setCi(DEFAULT_CI);
        student.setAge(DEFAULT_AGE);
        student.setName(DEFAULT_NAME);
        student.setRace(DEFAULT_RACE);
        student.setEmail(DEFAULT_EMAIL);
        student.setGender(DEFAULT_GENDER);
        student.setAddress(DEFAULT_ADDRESS);
        student.setBirthdate(DEFAULT_BIRTHDATE);
        student.setResidence(DEFAULT_RESIDENCE);
        student.setClassRoom(DEFAULT_CLASSROOM);
        student.setFirstLastName(DEFAULT_FIRST_LAST_NAME);
        student.setUniversityYear(DEFAULT_UNIVERSITY_YEAR);
        student.setSecondLastName(DEFAULT_SECOND_LAST_NAME);
    }

    @Test
    @Transactional
    public void createStudent() throws Exception {
        Nomenclature district = new Nomenclature();
        district.setName("Yateras");
        district.setActive(true);
        district.setDescription("Municipio de residencia");
        district.setDiscriminator(NomenclatureType.DISTRITO);
        em.persist(district);
        em.flush();
        student.setDistrict(district);

        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isCreated());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate + 1);
        Student testStudent = students.get(students.size() -1 );
        assertThat(testStudent.getCi()).isEqualTo(DEFAULT_CI);
        assertThat(testStudent.getAge()).isEqualTo(DEFAULT_AGE);
        assertThat(testStudent.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testStudent.getRace()).isEqualTo(DEFAULT_RACE);
        assertThat(testStudent.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testStudent.getGender()).isEqualTo(DEFAULT_GENDER);
        assertThat(testStudent.getAddress()).isEqualTo(DEFAULT_ADDRESS);
        assertThat(testStudent.getResidence()).isEqualTo(DEFAULT_RESIDENCE);
        assertThat(testStudent.getBirthdate()).isEqualTo(DEFAULT_BIRTHDATE);
        assertThat(testStudent.getClassRoom()).isEqualTo(DEFAULT_CLASSROOM);
        assertThat(testStudent.getFirstLastName()).isEqualTo(DEFAULT_FIRST_LAST_NAME);
        assertThat(testStudent.getUniversityYear()).isEqualTo(DEFAULT_UNIVERSITY_YEAR);
        assertThat(testStudent.getSecondLastName()).isEqualTo(DEFAULT_SECOND_LAST_NAME);
    }

    @Test
    @Transactional
    public void createStudentWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Phone with an existing ID
        student.setId(UUID.randomUUID());
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkNameIsCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setName("");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());
        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkEmailIsMalformed() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setEmail("peepmailcom");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkAddressIsCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setAddress("");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkClassRoomIsCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setClassRoom("");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkResidenceIsCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setClassRoom("");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkRaceIsCanNotBlank() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setRace("");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkCIWithDigitsQuantityGreaterThanElevenIsIncorrect() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setCi("1234567891011");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkCIWithDigitsQuantityLessThanElevenIsIncorrect() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setCi("123456789");
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkUniversityYearsIsCanNotLessThanOne() throws Exception {
        int databaseSizeBeforeCreate = TestUtil.findAll(em, Student.class).size();

        // Create the Student, which fails.
        student.setUniversityYear(0);
        StudentDTO studentDTO = mapper.toDto(student);

        restMockMvc.perform(post("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void updateStudent() throws Exception {
        // Initialize the database
        em.persist(student);
        em.flush();

        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Student.class).size();

        // Update the Student
        Student updatedStudent = em.find(Student.class, student.getId());
        // Disconnect from session so that the updates on updatedStudent are not directly saved in db
        em.detach(updatedStudent);

        updatedStudent.setCi(UPDATE_CI);
        updatedStudent.setAge(UPDATE_AGE);
        updatedStudent.setName(UPDATE_NAME);
        updatedStudent.setRace(UPDATE_RACE);
        updatedStudent.setEmail(UPDATE_EMAIL);
        updatedStudent.setGender(UPDATE_GENDER);
        updatedStudent.setAddress(UPDATE_ADDRESS);
        updatedStudent.setResidence(UPDATE_RESIDENCE);
        updatedStudent.setBirthdate(UPDATE_BIRTHDATE);
        updatedStudent.setClassRoom(UPDATE_CLASSROOM);
        updatedStudent.setFirstLastName(UPDATE_FIRST_LAST_NAME);
        updatedStudent.setUniversityYear(UPDATE_UNIVERSITY_YEAR);
        updatedStudent.setSecondLastName(UPDATE_SECOND_LAST_NAME);

        StudentDTO studentDTO = mapper.toDto(updatedStudent);

        restMockMvc.perform(put("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isOk());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeUpdate);
        Student testStudent = students.get(students.size() -1);
        assertThat(testStudent.getCi()).isEqualTo(UPDATE_CI);
        assertThat(testStudent.getAge()).isEqualTo(UPDATE_AGE);
        assertThat(testStudent.getName()).isEqualTo(UPDATE_NAME);
        assertThat(testStudent.getRace()).isEqualTo(UPDATE_RACE);
        assertThat(testStudent.getEmail()).isEqualTo(UPDATE_EMAIL);
        assertThat(testStudent.getGender()).isEqualTo(UPDATE_GENDER);
        assertThat(testStudent.getBirthdate()).isEqualTo(UPDATE_BIRTHDATE);
        assertThat(testStudent.getAddress()).isEqualTo(UPDATE_ADDRESS);
        assertThat(testStudent.getResidence()).isEqualTo(UPDATE_RESIDENCE);
        assertThat(testStudent.getClassRoom()).isEqualTo(UPDATE_CLASSROOM);
        assertThat(testStudent.getFirstLastName()).isEqualTo(UPDATE_FIRST_LAST_NAME);
        assertThat(testStudent.getUniversityYear()).isEqualTo(UPDATE_UNIVERSITY_YEAR);
        assertThat(testStudent.getSecondLastName()).isEqualTo(UPDATE_SECOND_LAST_NAME);
    }

    @Test
    @Transactional
    public void updateNonExistingEmployee() throws Exception {
        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Employee.class).size();

        // Create the Student
        StudentDTO studentDTO = mapper.toDto(student);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMockMvc.perform(put("/api/students").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(studentDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Student in the database
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteStudent() throws Exception {
        // Initialize the database
        em.persist(student);
        em.flush();

        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Student.class).size();

        // Delete the student
        restMockMvc.perform(delete("/api/students/{id}", student.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Student> students = TestUtil.findAll(em, Student.class);
        assertThat(students).hasSize(databaseSizeBeforeUpdate - 1);

    }

    @Test
    @Transactional
    public void getStudent() throws Exception {
        // Initialize the database
        em.persist(student);
        em.flush();

        restMockMvc.perform(get("/api/students/{id}", student.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(student.getId().toString()))
                .andExpect(jsonPath("$.ci").value(DEFAULT_CI))
                .andExpect(jsonPath("$.age").value(DEFAULT_AGE))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
                .andExpect(jsonPath("$.race").value(DEFAULT_RACE))
                .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
                .andExpect(jsonPath("$.address").value(DEFAULT_ADDRESS))
                .andExpect(jsonPath("$.birthdate").value(DEFAULT_BIRTHDATE.toString()))
                .andExpect(jsonPath("$.gender").value(DEFAULT_GENDER.toString()))
                .andExpect(jsonPath("$.firstLastName").value(DEFAULT_FIRST_LAST_NAME))
                .andExpect(jsonPath("$.secondLastName").value(DEFAULT_SECOND_LAST_NAME));
    }

    @Test
    @Transactional
    public void getNonExistingStudent() throws Exception {
        // Get the student
        restMockMvc.perform(get("/api/students/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void getAllStudents() throws Exception {
        // Initialize the database
        em.persist(student);
        em.flush();

        // Get all the students
        restMockMvc.perform(get("/api/students?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(student.getId().toString())))
                .andExpect(jsonPath("$.[*].ci").value(hasItem(DEFAULT_CI)))
                .andExpect(jsonPath("$.[*].age").value(hasItem(DEFAULT_AGE)))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
                .andExpect(jsonPath("$.[*].race").value(hasItem(DEFAULT_RACE)))
                .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
                .andExpect(jsonPath("$.[*].birthdate").value(hasItem(DEFAULT_BIRTHDATE.toString())))
                .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
                .andExpect(jsonPath("$.[*].gender").value(hasItem(DEFAULT_GENDER.toString())))
                .andExpect(jsonPath("$.[*].firstLastName").value(hasItem(DEFAULT_FIRST_LAST_NAME)))
                .andExpect(jsonPath("$.[*].secondLastName").value(hasItem(DEFAULT_SECOND_LAST_NAME)));
    }
}
