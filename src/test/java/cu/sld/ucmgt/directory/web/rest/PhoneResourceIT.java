package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.domain.*;
import org.junit.jupiter.api.Test;
import cu.sld.ucmgt.directory.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import cu.sld.ucmgt.directory.DirectoryApp;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import org.springframework.test.web.servlet.MockMvc;
import cu.sld.ucmgt.directory.service.mapper.PhoneMapper;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import javax.persistence.EntityManager;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link PhoneResource} REST controller.
 */
@SpringBootTest(classes = {DirectoryApp.class, TestSecurityConfiguration.class})
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
public class PhoneResourceIT {
    private static final Integer UPDATE_NUMBER = 78358658;
    private static final Integer DEFAULT_NUMBER = 78372133;

    private static final Boolean UPDATE_ACTIVE = false;
    private static final Boolean DEFAULT_ACTIVE = true;

    private static final String UPDATE_DESCRIPTION = "qwerty";
    private static final String DEFAULT_DESCRIPTION = "asdfgh";

    private Phone phone;

    @Autowired
    private PhoneMapper mapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private PhoneRepository repository;

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        WorkPlace workPlace = new WorkPlace();
        workPlace.setName("TIC");
        workPlace.setActive(true);
        workPlace.setEmail("tic@mail.com");
        workPlace.setDescription("ASDASWEQWE");
        em.persist(workPlace);

        Employee employee = new Employee();
        employee.setName("Cesar");
        employee.setGender(Gender.Masculino);
        employee.setEmail("admin@mail.com");
        employee.setAge(29);
        employee.setRace("Azul");
        employee.setCI("91061721000");
        employee.setAddress("Diente y caja de muela");
        employee.setRegisterNumber("458r");
        employee.setIsGraduatedBySector(true);
        employee.setServiceYears(4);
        employee.setBossWorkPlace(true);
        employee.setStartDate(ZonedDateTime.now(ZoneId.systemDefault()).withNano(0));
        em.persist(employee);

        phone = new Phone();
        phone.setNumber(DEFAULT_NUMBER);
        phone.setActive(DEFAULT_ACTIVE);
        phone.setWorkPlace(workPlace);
        phone.setEmployee(employee);
        phone.setDescription(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createPhone() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();
        PhoneDTO phoneDTO = mapper.toDto(phone);

        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate + 1);
        Phone testPhone = phones.get(phones.size() - 1);
        assertThat(testPhone.getNumber()).isEqualTo(DEFAULT_NUMBER);
        assertThat(testPhone.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testPhone.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createPhoneWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Phone with an existing ID
        phone.setId(UUID.randomUUID());
        PhoneDTO phoneDTO = mapper.toDto(phone);

        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createPhoneWithWorkPlaceAndPersonNull() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Phone with an existing ID
        phone.setWorkPlace(null);
        phone.setEmployee(null);
        PhoneDTO phoneDTO = mapper.toDto(phone);

        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkNumberIsCanNotLessThanOne() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Employee, which fails.
        phone.setNumber(0);
        PhoneDTO phoneDTO = mapper.toDto(phone);

        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Employee in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllPhones() throws Exception {
        repository.save(phone);

        // Get all the phones
        restMockMvc.perform(get("/api/phones?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(phone.getId().toString())))
                .andExpect(jsonPath("$.[*].number").value(hasItem(DEFAULT_NUMBER)))
                .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
                .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));
    }

    @Test
    @Transactional
    public void getPhone() throws Exception {
        // Initialize the database
        repository.save(phone);

        restMockMvc.perform(get("/api/phones/{id}", phone.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(phone.getId().toString()))
                .andExpect(jsonPath("$.number").value(phone.getNumber()))
                .andExpect(jsonPath("$.active").value(phone.getActive()))
                .andExpect(jsonPath("$.description").value(phone.getDescription()));
    }

    @Test
    @Transactional
    public void getNonExistingPhone() throws Exception {
        // Get the phone
        restMockMvc.perform(get("/api/phones/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void deletePhone() throws Exception {
        // Initialize the database
        repository.save(phone);

        int databaseSizeBeforeCreate = repository.findAll().size();

        // Delete the employee
        restMockMvc.perform(delete("/api/phones/{id}", phone.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate - 1);
    }

    @Test
    @Transactional
    public void updateEmployee() throws Exception {
        // Initialize the database
        repository.save(phone);

        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Update the Phone
        Phone updatedPhone = em.find(Phone.class, phone.getId());
        // Disconnect from session so that the updates on updatedEmployee are not directly saved in db
        em.detach(updatedPhone);

        updatedPhone.setNumber(UPDATE_NUMBER);
        updatedPhone.setActive(UPDATE_ACTIVE);
        updatedPhone.setDescription(UPDATE_DESCRIPTION);

        PhoneDTO phoneDTO = mapper.toDto(updatedPhone);
        restMockMvc.perform(put("/api/phones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isOk());

        // Validate the Phone in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeUpdate);
        Phone testPhone = phones.get(phones.size() - 1);
        assertThat(testPhone.getNumber()).isEqualTo(UPDATE_NUMBER);
        assertThat(testPhone.getActive()).isEqualTo(UPDATE_ACTIVE);
        assertThat(testPhone.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    public void updateNonExistingEmployee() throws Exception {
        int databaseSizeBeforeUpdate = repository.findAll().size();

        // Create the Phone
        PhoneDTO phoneDTO = mapper.toDto(phone);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMockMvc.perform(put("/api/phones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Phone in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeUpdate);
    }


}
