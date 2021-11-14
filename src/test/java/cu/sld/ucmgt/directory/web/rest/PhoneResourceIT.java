package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.DirectoryApp;
import cu.sld.ucmgt.directory.TestUtil;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import cu.sld.ucmgt.directory.domain.Employee;
import cu.sld.ucmgt.directory.domain.Gender;
import cu.sld.ucmgt.directory.domain.Phone;
import cu.sld.ucmgt.directory.domain.WorkPlace;
import cu.sld.ucmgt.directory.domain.elasticsearch.PhoneIndex;
import cu.sld.ucmgt.directory.domain.elasticsearch.WorkPlaceIndex;
import cu.sld.ucmgt.directory.repository.PhoneRepository;
import cu.sld.ucmgt.directory.repository.WorkPlaceRepository;
import cu.sld.ucmgt.directory.repository.search.PhoneSearchRepository;
import cu.sld.ucmgt.directory.repository.search.WorkPlaceSearchRepository;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.service.mapper.PhoneIndexMapper;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

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
    private static final String UPDATE_NUMBER = "78358658";
    private static final String DEFAULT_NUMBER = "78372133";

    private static final Boolean UPDATE_ACTIVE = false;
    private static final Boolean DEFAULT_ACTIVE = true;

    private static final String UPDATE_DESCRIPTION = "qwerty";
    private static final String DEFAULT_DESCRIPTION = "asdfgh";

    private Phone phone;

    @Autowired
    private PhoneMapper mapper;

    @Autowired
    private PhoneIndexMapper indexMapper;

    @Autowired
    private WorkPlaceMapper workPlaceMapper;

    @Autowired
    private WorkPlaceRepository workPlaceRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PhoneRepository repository;

    @Autowired
    private PhoneSearchRepository searchRepository;

    @Autowired
    private WorkPlaceSearchRepository workPlaceSearchRepository;

    private static final String ENDPOINT_RESPONSE_PARAMETERS_KEY = "X-directoryApp-params";

    @Autowired
    private MockMvc restMockMvc;

    @BeforeEach
    public void initTest() {
        // clear all indices
        searchRepository.deleteAll();

        phone = new Phone();
        phone.setNumber(DEFAULT_NUMBER);
        phone.setActive(DEFAULT_ACTIVE);
        phone.setDescription(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createPhoneWithWorkPlace() throws Exception {
        //Initialize database
        int databaseSizeBeforeCreate = repository.findAll().size();
        // Clear PhoneIndex and WorkPlaceIndex indices
        searchRepository.deleteAll();

        WorkPlace workPlace = getWorkPlaceWithPhones(Collections.emptySet());
        em.persist(workPlace);

        phone.setWorkPlace(workPlace);
        PhoneDTO phoneDTO = mapper.toDto(phone);

        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate + 1);
        Phone testPhone = phones.get(phones.size() - 1);
        testPhoneIsCreated(testPhone);
        assertThat(testPhone.getWorkPlace().getId()).isEqualTo(workPlace.getId());

        Iterable<PhoneIndex> phoneIndexIterable = searchRepository.findAll();
        PhoneIndex phoneIndex = phoneIndexIterable.iterator().next();
        testPhoneIndexIsCreated(phoneIndex);
    }

    @Test
    @Transactional
    public void createPhoneAndUpdateWorkPlaceIndex() throws Exception {
        // Clear PhoneIndex and WorkPlaceIndex indices
        searchRepository.deleteAll();
        workPlaceSearchRepository.deleteAll();

        WorkPlace workPlace = getWorkPlaceWithPhones(Collections.emptySet());
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(workPlace);
        MvcResult resultWorkPlace = restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String workPlaceId = resultWorkPlace.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(workPlaceId).isNotNull();

        int databaseSizeBeforeCreate = TestUtil.findAll(em, Phone.class).size();
        PhoneDTO phoneDTO = mapper.toDto(phone);
        phoneDTO.setWorkPlaceId(UUID.fromString(workPlaceId));
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        // Validate the Phone in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate + 1);
        Phone testPhone = phones.get(phones.size() - 1);
        testPhoneIsCreated(testPhone);
        assertThat(testPhone.getWorkPlace().getId()).isEqualTo(UUID.fromString(workPlaceId));

        Iterable<PhoneIndex> phoneIndexIterable = searchRepository.findAll();
        PhoneIndex phoneIndex = phoneIndexIterable.iterator().next();
        testPhoneIndexIsCreated(phoneIndex);

        Iterable<WorkPlaceIndex> workPlaceIndices = workPlaceSearchRepository.findAll();
        PhoneIndex testPhoneIndexWorkPlaceIndex = workPlaceIndices.iterator().next().getPhones().iterator().next();
        testPhoneIndexIsCreated(testPhoneIndexWorkPlaceIndex);
    }

    @Test
    @Transactional
    public void updatePhoneAndUpdateWorkPlaceIndex() throws Exception {
        // Clear PhoneIndex and WorkPlaceIndex indices
        searchRepository.deleteAll();
        workPlaceSearchRepository.deleteAll();

        WorkPlace workPlace = getWorkPlaceWithPhones(Collections.emptySet());
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(workPlace);
        MvcResult resultWorkPlace = restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String workPlaceId = resultWorkPlace.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(workPlaceId).isNotNull();

        int databaseSizeBeforeCreate = TestUtil.findAll(em, Phone.class).size();
        PhoneDTO phoneDTO = mapper.toDto(phone);
        phoneDTO.setWorkPlaceId(UUID.fromString(workPlaceId));
        MvcResult resultPhone = restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String phoneId = resultPhone.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(phoneId).isNotNull();

        // Update the Phone
        Phone updatedPhone = getUpdatedPhone(UUID.fromString(phoneId));
        int databaseSizeBeforeUpdate = TestUtil.findAll(em, Phone.class).size();
        phoneDTO = mapper.toDto(updatedPhone);
        restMockMvc.perform(put("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isOk());

        // Validate the Phone in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate + 1);
        assertThat(phones).hasSize(databaseSizeBeforeUpdate);
        Phone testPhone = phones.get(phones.size() - 1);
        testPhoneIsUpdated(testPhone);

        Iterable<PhoneIndex> phoneIndexIterable = searchRepository.findAll();
        PhoneIndex phoneIndex = phoneIndexIterable.iterator().next();
        testPhoneIndexIsUpdated(phoneIndex);

        Iterable<WorkPlaceIndex> workPlaceIndices = workPlaceSearchRepository.findAll();
        PhoneIndex testPhoneIndexWorkPlaceIndex = workPlaceIndices.iterator().next().getPhones().iterator().next();
        testPhoneIndexIsUpdated(testPhoneIndexWorkPlaceIndex);
    }

    private void testPhoneIsUpdated(Phone testPhone) {
        assertThat(testPhone.getNumber()).isEqualTo(UPDATE_NUMBER);
        assertThat(testPhone.getActive()).isEqualTo(UPDATE_ACTIVE);
        assertThat(testPhone.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
    }

    private void testPhoneIndexIsUpdated(PhoneIndex phoneIndex) {
        assertThat(phoneIndex.getNumber()).isEqualTo(UPDATE_NUMBER);
        assertThat(phoneIndex.getDescription()).isEqualTo(UPDATE_DESCRIPTION);
    }

    private void testPhoneIsCreated(Phone testPhone) {
        assertThat(testPhone.getNumber()).isEqualTo(DEFAULT_NUMBER);
        assertThat(testPhone.getActive()).isEqualTo(DEFAULT_ACTIVE);
        assertThat(testPhone.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    private void testPhoneIndexIsCreated(PhoneIndex phoneIndex) {
        assertThat(phoneIndex.getNumber()).isEqualTo(DEFAULT_NUMBER);
        assertThat(phoneIndex.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    private WorkPlace getWorkPlaceWithPhones(Set<Phone> set) {
        WorkPlace workPlace = new WorkPlace();
        workPlace.setName("TIC");
        workPlace.setActive(true);
        workPlace.setEmail("tic@mail.com");
        workPlace.setDescription("ASDASWEQWE");
        workPlace.setPhones(set);
        return workPlace;
    }

    @Test
    @Transactional
    public void createPhoneWithEmployee() throws Exception {
        //Initialize database
        int databaseSizeBeforeCreate = repository.findAll().size();

        Employee employee = getEmployee();
        em.persist(employee);

        phone.setEmployee(employee);
        PhoneDTO phoneDTO = mapper.toDto(phone);

        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate + 1);
        Phone testPhone = phones.get(phones.size() - 1);
        testPhoneIsCreated(testPhone);
    }

    private Employee getEmployee() {
        Employee employee = new Employee();
        employee.setName("Cesar");
        employee.setGender(Gender.Masculino);
        employee.setEmail("admin@mail.com");
        employee.setAge(29);
        employee.setRace("Azul");
        employee.setCi("91061721000");
        employee.setAddress("Diente y caja de muela");
        employee.setRegisterNumber("458r");
        employee.setIsGraduatedBySector(true);
        employee.setServiceYears(4);
        employee.setBossWorkPlace(true);
        employee.setStartDate(LocalDateTime.now(ZoneId.systemDefault()).withNano(0));
        return employee;
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

        // Validate the Phone in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkNumberIsCanNotLessThanOne() throws Exception {
        int databaseSizeBeforeCreate = repository.findAll().size();

        // Create the Employee, which fails.
        phone.setNumber("0");
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
    public void createPhoneWithUniqueValueConstraintViolated() throws Exception {
        //Initialize database
        Phone phone2 = new Phone();
        phone2.setActive(true);
        phone2.setNumber(DEFAULT_NUMBER);
        phone2.setDescription(DEFAULT_DESCRIPTION);
        repository.saveAndFlush(phone2);
        int databaseSizeBeforeCreate = repository.findAll().size();

        Employee employee = getEmployee();
        em.persist(employee);

        phone.setEmployee(employee);
        PhoneDTO phoneDTO = mapper.toDto(phone);

        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isBadRequest());

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
    public void deletePhoneWhitPhoneIndex() throws Exception {
        // Initialize database
        WorkPlace phoneWorkPlace = getWorkPlaceWithPhones(Collections.emptySet());
        workPlaceRepository.saveAndFlush(phoneWorkPlace);

        phone.setWorkPlace(phoneWorkPlace);
        PhoneDTO phoneDTO = mapper.toDto(phone);
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        long indexSizeAfterCreateDoc = searchRepository.count();
        int databaseSizeAfterCreate = repository.findAll().size();


        // Delete the phone
        restMockMvc.perform(delete("/api/phones/{number}", phone.getNumber()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeAfterCreate -1);
        Iterable<PhoneIndex> phoneIndexIterable = searchRepository.findAll();
        assertThat(phoneIndexIterable).hasSize((int) (indexSizeAfterCreateDoc - 1));
    }

    @Test
    public void deletePhoneAndPhoneIndexInsideWorkPlace() throws Exception {
        // clear indices
        workPlaceSearchRepository.deleteAll();
        searchRepository.deleteAll();

        Phone phone2 = new Phone();
        phone2.setActive(true);
        phone2.setNumber("21319094");
        phone2.setDescription("Carlos Manuel's Phone");

        WorkPlace phoneWorkPlace = getWorkPlaceWithPhones(Collections.emptySet());
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(phoneWorkPlace);
        MvcResult resultWorkplace = restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        String workplaceId = resultWorkplace.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(workplaceId).isNotNull();

        PhoneDTO phoneDTO = mapper.toDto(phone);
        phoneDTO.setWorkPlaceId(UUID.fromString(workplaceId));
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated());

        PhoneDTO phone2DTO = mapper.toDto(phone2);
        phone2DTO.setWorkPlaceId(UUID.fromString(workplaceId));
        restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phone2DTO)))
                .andExpect(status().isCreated());

        // Delete the phone
        restMockMvc.perform(delete("/api/phones/{number}", phone.getNumber()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(1);

        Iterable<WorkPlaceIndex> workPlaceIndexIterable = workPlaceSearchRepository.findAll();
        WorkPlaceIndex testWorkPaceIndex = workPlaceIndexIterable.iterator().next();
        assertThat(testWorkPaceIndex.getPhones()).hasSize(1);
        assertThat(testWorkPaceIndex.getPhones().iterator().next().getNumber()).isEqualTo(phone2.getNumber());
    }

    @Test
    @Transactional
    public void updatePhoneAndPhoneIndex() throws Exception {
        // Initialize the database
        WorkPlace phoneWorkPlace = getWorkPlaceWithPhones(Collections.emptySet());
        em.persist(phoneWorkPlace);
        phone.setWorkPlace(phoneWorkPlace);
        repository.saveAndFlush(phone);
        // Initialize index
        PhoneIndex index = indexMapper.toIndex(phone);
        searchRepository.save(index);

        int databaseSizeBeforeUpdate = repository.findAll().size();
        long indexSizeBeforeUpdate = searchRepository.count();

        // Update the Phone
        Phone updatedPhone = getUpdatedPhone(null);

        PhoneDTO phoneDTO = mapper.toDto(updatedPhone);
        restMockMvc.perform(put("/api/phones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isOk());

        // Validate the Phone in the database
        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeUpdate);
        Phone testPhone = phones.get(phones.size() - 1);
        testPhoneIsUpdated(testPhone);
        Iterable<PhoneIndex> phoneIndexIterable = searchRepository.findAll();
        assertThat(phoneIndexIterable).hasSize((int) indexSizeBeforeUpdate);
        PhoneIndex testPhoneIndex = phoneIndexIterable.iterator().next();
        testPhoneIndexIsUpdated(testPhoneIndex);
    }

    private Phone getUpdatedPhone(UUID phoneId) {
        UUID id = phoneId == null ? phone.getId(): phoneId;
        Phone updatedPhone = em.find(Phone.class, id);

        updatedPhone.setNumber(UPDATE_NUMBER);
        updatedPhone.setActive(UPDATE_ACTIVE);
        updatedPhone.setDescription(UPDATE_DESCRIPTION);
        return updatedPhone;
    }

    @Test
    @Transactional
    public void updateNonExistingPhone() throws Exception {
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

    @Test
    @Transactional
    public void disableStatusPhone() throws Exception {
        // Clear PhoneIndex and WorkPlaceIndex
        searchRepository.deleteAll();
        workPlaceSearchRepository.deleteAll();

        WorkPlace workPlace = getWorkPlaceWithPhones(Collections.emptySet());
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(workPlace);
        MvcResult resultWorkPlace = restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        String workPlaceId = resultWorkPlace.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(workPlaceId).isNotNull();

        PhoneDTO phoneDTO = mapper.toDto(phone);
        phoneDTO.setWorkPlaceId(UUID.fromString(workPlaceId));
        int databaseSizeBeforeCreate = repository.findAll().size();
        MvcResult resultPhone = restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String phoneId = resultPhone.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(phoneId).isNotNull();

        Map<String, Object> switchStatusPhone = new HashMap<>();
        switchStatusPhone.put("id",phoneId);
        switchStatusPhone.put("status", UPDATE_ACTIVE);
        int databaseSizeAfterUpdate = repository.findAll().size();
        restMockMvc.perform(put("/api/phones/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(switchStatusPhone)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(databaseSizeBeforeCreate + 1);
        assertThat(phones).hasSize(databaseSizeAfterUpdate);
        Phone testPhone = phones.get(phones.size() -1);
        assertThat(testPhone.getActive()).isEqualTo(UPDATE_ACTIVE);

        Iterable<PhoneIndex> phoneIndexIterable = searchRepository.findAll();
        assertThat(phoneIndexIterable).hasSize(0);

        Iterable<WorkPlaceIndex> workPlaceIndexIterable = workPlaceSearchRepository.findAll();
        assertThat(workPlaceIndexIterable).hasSize(1);
        WorkPlaceIndex testWorkPlacePhone = workPlaceIndexIterable.iterator().next();
        assertThat(testWorkPlacePhone.getPhones()).hasSize(0);
    }

    @Test
    @Transactional
    public void enableStatusPhone() throws Exception {
        // Clear PhoneIndex and WorkPlaceIndex
        searchRepository.deleteAll();
        workPlaceSearchRepository.deleteAll();

        WorkPlace workPlace = getWorkPlaceWithPhones(Collections.emptySet());
        WorkPlaceDTO workPlaceDTO = workPlaceMapper.toDto(workPlace);
        MvcResult resultWorkPlace = restMockMvc.perform(post("/api/workplaces").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(workPlaceDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        String workPlaceId = resultWorkPlace.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(workPlaceId).isNotNull();

        PhoneDTO phoneDTO = mapper.toDto(phone);
        phoneDTO.setWorkPlaceId(UUID.fromString(workPlaceId));
        MvcResult resultPhone = restMockMvc.perform(post("/api/phones").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(phoneDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        String phoneId = resultPhone.getResponse().getHeader(ENDPOINT_RESPONSE_PARAMETERS_KEY);
        assertThat(phoneId).isNotNull();

        Map<String, Object> switchStatusPhone = new HashMap<>();
        switchStatusPhone.put("id",phoneId);
        switchStatusPhone.put("status", UPDATE_ACTIVE);
        restMockMvc.perform(put("/api/phones/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(switchStatusPhone)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        switchStatusPhone.put("id",phoneId);
        switchStatusPhone.put("status", DEFAULT_ACTIVE);
        restMockMvc.perform(put("/api/phones/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(switchStatusPhone)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        List<Phone> phones = repository.findAll();
        assertThat(phones).hasSize(1);
        Phone testPhone = phones.get(phones.size() -1);
        assertThat(testPhone.getActive()).isEqualTo(DEFAULT_ACTIVE);

        Iterable<PhoneIndex> phoneIndexIterable = searchRepository.findAll();
        assertThat(phoneIndexIterable).hasSize(1);
        PhoneIndex testPhoneIndex = phoneIndexIterable.iterator().next();
        testPhoneIndexIsCreated(testPhoneIndex);

        Iterable<WorkPlaceIndex> workPlaceIndexIterable = workPlaceSearchRepository.findAll();
        assertThat(workPlaceIndexIterable).hasSize(1);
        WorkPlaceIndex testWorkPlacePhone = workPlaceIndexIterable.iterator().next();
        assertThat(testWorkPlacePhone.getPhones()).hasSize(1);
        testPhoneIndexIsCreated(testWorkPlacePhone.getPhones().iterator().next());
    }

    /**
     * Executes the search with And operator, and checks that the default entity is returned.
     */
    private void defaultPhoneShouldBeFoundWithAndOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/phones/filtered/and?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].number").value(hasItem(DEFAULT_NUMBER)))
                .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
                .andExpect(jsonPath("$.[*].id").value(hasItem(phone.getId().toString())))
                .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));
    }

    /**
     * Executes the search with And operator, and checks that the default entity is not returned.
     */
    private void defaultPhoneShouldNotBeFoundWithAndOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/phones/filtered/and?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    /**
     * Executes the search with Or operator, and checks that the default entity is returned.
     */
    private void defaultPhoneShouldBeFoundWithOrOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/phones/filtered/or?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].number").value(hasItem(DEFAULT_NUMBER)))
                .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
                .andExpect(jsonPath("$.[*].id").value(hasItem(phone.getId().toString())))
                .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));
    }

    /**
     * Executes the search with Or operator, and checks that the default entity is not returned.
     */
    private void defaultPhoneShouldNotBeFoundWithOrOperator(String filter) throws Exception {
        restMockMvc.perform(get("/api/phones/filtered/or?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    public void getWorkPlaceByIdFiltering() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        UUID id = phone.getId();

        defaultPhoneShouldBeFoundWithAndOperator("id.equals=" + id);
        defaultPhoneShouldBeFoundWithOrOperator("id.equals=" + id);

        defaultPhoneShouldNotBeFoundWithAndOperator("id.notEquals=" + id);
        defaultPhoneShouldNotBeFoundWithOrOperator("id.notEquals=" + id);


        defaultPhoneShouldBeFoundWithAndOperator("id.in=" + id + "," + UUID.randomUUID().toString());
        defaultPhoneShouldBeFoundWithOrOperator("id.in=" + id + "," + UUID.randomUUID().toString());

        defaultPhoneShouldNotBeFoundWithAndOperator("id.notIn=" + id + "," + UUID.randomUUID().toString());
        defaultPhoneShouldNotBeFoundWithOrOperator("id.notIn=" + id + "," + UUID.randomUUID().toString());

        defaultPhoneShouldBeFoundWithAndOperator("id.specified=true");
        defaultPhoneShouldBeFoundWithOrOperator("id.specified=true");

        defaultPhoneShouldNotBeFoundWithAndOperator("id.specified=false");
        defaultPhoneShouldNotBeFoundWithOrOperator("id.specified=false");
    }

    @Test
    @Transactional
    void getAllPhoneByDescriptionIsEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where description equals to DEFAULT_DESCRIPTION
        defaultPhoneShouldBeFoundWithAndOperator("description.equals=" + DEFAULT_DESCRIPTION);
        defaultPhoneShouldBeFoundWithOrOperator("description.equals=" + DEFAULT_DESCRIPTION);

        // Get all the phonesList where description equals to UPDATE_DESCRIPTION
        defaultPhoneShouldNotBeFoundWithAndOperator("description.equals=" + UPDATE_DESCRIPTION);
        defaultPhoneShouldNotBeFoundWithOrOperator("description.equals=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllPhoneByDescriptionIsNotEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where description not equals to UPDATE_DESCRIPTION
        defaultPhoneShouldBeFoundWithAndOperator("description.notEquals=" + UPDATE_DESCRIPTION);
        defaultPhoneShouldBeFoundWithOrOperator("description.notEquals=" + UPDATE_DESCRIPTION);

        // Get all the phonesList where description not equals to DEFAULT_DESCRIPTION
        defaultPhoneShouldNotBeFoundWithAndOperator("description.notEquals=" + DEFAULT_DESCRIPTION);
        defaultPhoneShouldNotBeFoundWithOrOperator("description.notEquals=" + DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllPhoneByDescriptionIsInShouldWork() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where description in UPDATE_DESCRIPTION or DEFAULT_NAME
       defaultPhoneShouldBeFoundWithAndOperator("description.in=" + DEFAULT_DESCRIPTION + "," + UPDATE_DESCRIPTION);
       defaultPhoneShouldBeFoundWithOrOperator("description.in=" + DEFAULT_DESCRIPTION + "," + UPDATE_DESCRIPTION);

        // Get all the phonesList where description equals to DEFAULT_DESCRIPTION
       defaultPhoneShouldNotBeFoundWithAndOperator("description.in=" + UPDATE_DESCRIPTION);
       defaultPhoneShouldNotBeFoundWithOrOperator("description.in=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllPhoneByDescriptionIsNullOrNotNull() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where description is not null
       defaultPhoneShouldBeFoundWithAndOperator("description.specified=true");
       defaultPhoneShouldBeFoundWithOrOperator("description.specified=true");

        // Get all the phonesList where description is null
       defaultPhoneShouldNotBeFoundWithAndOperator("description.specified=false");
       defaultPhoneShouldNotBeFoundWithOrOperator("description.specified=false");
    }

    @Test
    @Transactional
    void getAllPhoneByDescriptionContainsSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where description contains DEFAULT_DESCRIPTION
       defaultPhoneShouldBeFoundWithAndOperator("description.contains=" + DEFAULT_DESCRIPTION);
       defaultPhoneShouldBeFoundWithOrOperator("description.contains=" + DEFAULT_DESCRIPTION);

        // Get all the phonesList where description contains UPDATE_DESCRIPTION
       defaultPhoneShouldNotBeFoundWithAndOperator("description.contains=" + UPDATE_DESCRIPTION);
       defaultPhoneShouldNotBeFoundWithOrOperator("description.contains=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllPhoneByDescriptionNotContainsSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where description does not contain UPDATE_NAME
       defaultPhoneShouldNotBeFoundWithAndOperator("description.doesNotContain=" + DEFAULT_DESCRIPTION);
       defaultPhoneShouldNotBeFoundWithOrOperator("description.doesNotContain=" + DEFAULT_DESCRIPTION);

        // Get all the phonesList where description does not contain UPDATE_DESCRIPTION
       defaultPhoneShouldBeFoundWithAndOperator("description.doesNotContain=" + UPDATE_DESCRIPTION);
       defaultPhoneShouldBeFoundWithOrOperator("description.doesNotContain=" + UPDATE_DESCRIPTION);
    }

    @Test
    @Transactional
    void getAllWorPlacesByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where active equals to DEFAULT_ACTIVE
        defaultPhoneShouldBeFoundWithAndOperator("active.equals=" + DEFAULT_ACTIVE);
        defaultPhoneShouldBeFoundWithOrOperator("active.equals=" + DEFAULT_ACTIVE);

        // Get all the phonesList where active equals to UPDATE_ACTIVE
        defaultPhoneShouldNotBeFoundWithAndOperator("active.equals=" + UPDATE_ACTIVE);
        defaultPhoneShouldNotBeFoundWithOrOperator("active.equals=" + UPDATE_ACTIVE);
    }

    @Test
    @Transactional
    void getAllWorPlacesByActiveIsNotEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the workplacesList where active not equals to DEFAULT_ACTIVE
        defaultPhoneShouldNotBeFoundWithAndOperator("active.notEquals=" + DEFAULT_ACTIVE);
        defaultPhoneShouldNotBeFoundWithOrOperator("active.notEquals=" + DEFAULT_ACTIVE);

        // Get all the workplaceList where active not equals to UPDATE_ACTIVE
        defaultPhoneShouldBeFoundWithAndOperator("active.notEquals=" + UPDATE_ACTIVE);
        defaultPhoneShouldBeFoundWithOrOperator("active.notEquals=" + UPDATE_ACTIVE);
    }

    @Test
    @Transactional
    void getAllWorPlacesByNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where number equals to DEFAULT_ACTIVE
        defaultPhoneShouldBeFoundWithAndOperator("number.equals=" + DEFAULT_NUMBER);
        defaultPhoneShouldBeFoundWithOrOperator("number.equals=" + DEFAULT_NUMBER);

        // Get all the phonesList where active equals to UPDATE_ACTIVE
        defaultPhoneShouldNotBeFoundWithAndOperator("number.equals=" + UPDATE_NUMBER);
        defaultPhoneShouldNotBeFoundWithOrOperator("number.equals=" + UPDATE_NUMBER);
    }

    @Test
    @Transactional
    void getAllWorPlacesByNumberIsNotEqualToSomething() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where number not equals to DEFAULT_ACTIVE
        defaultPhoneShouldNotBeFoundWithAndOperator("number.notEquals=" + DEFAULT_NUMBER);
        defaultPhoneShouldNotBeFoundWithOrOperator("number.notEquals=" + DEFAULT_NUMBER);

        // Get all the phonesList where number not equals to UPDATE_ACTIVE
        defaultPhoneShouldBeFoundWithAndOperator("number.notEquals=" + UPDATE_NUMBER);
        defaultPhoneShouldBeFoundWithOrOperator("number.notEquals=" + UPDATE_NUMBER);
    }

    @Test
    @Transactional
    void getAllPhoneByNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where number is not null
        defaultPhoneShouldBeFoundWithAndOperator("number.specified=true");
        defaultPhoneShouldBeFoundWithOrOperator("number.specified=true");

        // Get all the phonesList where number is null
        defaultPhoneShouldNotBeFoundWithAndOperator("number.specified=false");
        defaultPhoneShouldNotBeFoundWithOrOperator("number.specified=false");
    }

    @Test
    @Transactional
    void getAllPhoneByNumberIsInShouldWork() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        // Get all the phonesList where number in DEFAULT_NUMBER or UPDATE_NUMBER
        defaultPhoneShouldBeFoundWithAndOperator("number.in=" + DEFAULT_NUMBER + "," + UPDATE_NUMBER);
        defaultPhoneShouldBeFoundWithOrOperator("number.in=" + DEFAULT_NUMBER + "," + UPDATE_NUMBER);

        // Get all the phonesList where number equals to UPDATE_NUMBER
        defaultPhoneShouldNotBeFoundWithAndOperator("number.in=" + UPDATE_NUMBER);
        defaultPhoneShouldNotBeFoundWithOrOperator("number.in=" + UPDATE_NUMBER);
    }

    @Test
    @Transactional
    void getAllPhoneByNumberIsNotInShouldWork() throws Exception {
        // Initialize the database
        repository.saveAndFlush(phone);

        defaultPhoneShouldBeFoundWithAndOperator("number.notIn=" + UPDATE_NUMBER + "," + "52132138");
        defaultPhoneShouldBeFoundWithOrOperator("number.notIn=" + UPDATE_NUMBER + "," + "52132138");
    }

    @Test
    @Transactional
    void getAllPhoneByWorkPlaceNameIsEqualToSomething() throws Exception {
        // Initialize the database
        WorkPlace workPlace = getWorkPlaceWithPhones(Collections.emptySet());
        String name = workPlace.getName();
        em.persist(workPlace);
        phone.setWorkPlace(workPlace);
        repository.saveAndFlush(phone);

        // Get all the phonesList where workPlaceName equals to name
        defaultPhoneShouldBeFoundWithAndOperator("workPlaceName.equals=" + name);
        defaultPhoneShouldBeFoundWithOrOperator("workPlaceName.equals=" + name);

        // Get all the phonesList where workPlaceName equals to wrong name
        defaultPhoneShouldNotBeFoundWithAndOperator("workPlaceName.equals=" + name.concat(" Cruzata"));
        defaultPhoneShouldNotBeFoundWithOrOperator("workPlaceName.equals=" + name.concat(" Cruzata"));
    }

    @Test
    @Transactional
    void getAllPhoneByWorkPlaceNameIsNotEqualToSomething() throws Exception {
        // Initialize the database
        WorkPlace workPlace = getWorkPlaceWithPhones(Collections.emptySet());
        String name = workPlace.getName();
        em.persist(workPlace);
        phone.setWorkPlace(workPlace);
        repository.saveAndFlush(phone);

        // Get all the phonesList where workPlaceName not equals to name
        defaultPhoneShouldBeFoundWithAndOperator("workPlaceName.notEquals=" + name.concat(" Cruzata"));
        defaultPhoneShouldBeFoundWithOrOperator("workPlaceName.notEquals=" + name.concat(" Cruzata"));

        // Get all the phonesList where workPlaceName not equals to name
        defaultPhoneShouldNotBeFoundWithAndOperator("workPlaceName.notEquals=" + name);
        defaultPhoneShouldNotBeFoundWithOrOperator("workPlaceName.notEquals=" + name);
    }

    @Test
    @Transactional
    void getAllPhoneByEmployeeNameIsEqualToSomething() throws Exception {
        // Initialize the database
        Employee employee = getEmployee();
        String name = employee.getName();
        em.persist(employee);
        phone.setEmployee(employee);
        repository.saveAndFlush(phone);

        // Get all the phonesList where employeeName equals to name
        defaultPhoneShouldBeFoundWithAndOperator("employeeName.equals=" + name);
        defaultPhoneShouldBeFoundWithOrOperator("employeeName.equals=" + name);

        // Get all the phonesList where employeeName equals to wrong name
        defaultPhoneShouldNotBeFoundWithAndOperator("employeeName.equals=" + name.concat(" Cruzata"));
        defaultPhoneShouldNotBeFoundWithOrOperator("employeeName.equals=" + name.concat(" Cruzata"));
    }

    @Test
    @Transactional
    void getAllPhoneByEmployeeNameIsNotEqualToSomething() throws Exception {
        // Initialize the database
        Employee employee = getEmployee();
        String name = employee.getName();
        em.persist(employee);
        phone.setEmployee(employee);
        repository.saveAndFlush(phone);

        // Get all the phonesList where employeeName not equals to name
        defaultPhoneShouldBeFoundWithAndOperator("employeeName.notEquals=" + name.concat(" Cruzata"));
        defaultPhoneShouldBeFoundWithOrOperator("employeeName.notEquals=" + name.concat(" Cruzata"));

        // Get all the phonesList where employeeName not equals to name
        defaultPhoneShouldNotBeFoundWithAndOperator("employeeName.notEquals=" + name);
        defaultPhoneShouldNotBeFoundWithOrOperator("employeeName.notEquals=" + name);
    }

}
