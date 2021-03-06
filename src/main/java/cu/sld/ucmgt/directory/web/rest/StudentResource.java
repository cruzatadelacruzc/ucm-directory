package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.StudentService;
import cu.sld.ucmgt.directory.service.criteria.StudentCriteria;
import cu.sld.ucmgt.directory.service.dto.StudentDTO;
import cu.sld.ucmgt.directory.web.rest.errors.BadRequestAlertException;
import cu.sld.ucmgt.directory.web.rest.util.HeaderUtil;
import cu.sld.ucmgt.directory.web.rest.util.PaginationUtil;
import cu.sld.ucmgt.directory.web.rest.util.ResponseUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudentResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final StudentService service;
    private static final String ENTITY_NAME = "Student";

    /**
     * {@code POST  /students} : Create a new student.
     *
     * @param studentDTO the studentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)}
     * and with body the new studentDTO, or with status {@code 400 (Bad Request)} if the student has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping(value = "/students", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestPart(name = "student") StudentDTO studentDTO,
                                                    @RequestPart(name = "avatar", required = false) MultipartFile avatar) throws URISyntaxException {

        if (studentDTO.getId() != null) {
            throw new BadRequestAlertException("A new student cannot already have an ID", ENTITY_NAME, "idexists", studentDTO.getId().toString());
        }

        this.checkMimeType(avatar);

        StudentDTO studentSaved = service.create(studentDTO, avatar);
        return ResponseEntity.created(new URI("/api/students/" + studentSaved.getId()))
                .headers(HeaderUtil.createEntityExecutedAlert(applicationName,
                        true, ENTITY_NAME,
                        studentSaved.getName()))
                .body(studentSaved);
    }

    /**
     * {@code PUT  /students} : Updates an existing student.
     *
     * @param studentDTO the studentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated studentDTO,
     * or with status {@code 400 (Bad Request)} if the studentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the studentDTO couldn't be updated.
     */
    @PutMapping(value = "/students", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<StudentDTO> updateStudent(@Valid @RequestPart(name = "student") StudentDTO studentDTO,
                                                    @RequestPart(name = "avatar", required = false) MultipartFile avatar){

        if (studentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull", "idnull");
        }

        this.checkMimeType(avatar);

        StudentDTO studentSaved = service.update(studentDTO, avatar);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        studentSaved.getName()))
                .body(studentSaved);
    }

    /**
     * {@code DELETE  /students/avatar/:id} : delete the avatar belong to "id" student.
     * @param studentId identifier
     * @return the {@link ResponseEntity} with status {@code 200 (NO_CONTENT)} and with body the boolean response.
     */
    @DeleteMapping("/students/avatar/{id}")
    public ResponseEntity<Boolean> deleteAvatar(@PathVariable(name = "id") UUID studentId) {
        Boolean response = service.deleteAvatar(studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * {@code DELETE  /students/:id} : delete the "id" student.
     *
     * @param uid the id of the Students to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to delete Students : {}", uid);
        service.deleteStudent(uid);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                        applicationName,
                        true, ENTITY_NAME,
                        uid.toString()))
                .build();
    }

    /**
     * {@code GET  /students/:id} : get the "id" student.
     *
     * @param uid the id of the studentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the studentDTO
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDTO> getStudent(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to get Person : {}", uid);
        Optional<StudentDTO> studentFetched = service.getStudent(uid);
        return ResponseUtil.wrapOrNotFound(studentFetched);
    }

    /**
     * {@code GET  /students} : get all the student.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of students in body.
     */
    @GetMapping("/students")
    public ResponseEntity<List<StudentDTO>> getAllStudents(Pageable pageable) {
        log.debug("REST request to get a page of Students");
        Page<StudentDTO> page = service.getAllStudents(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /students/filtered/{join}} : get all the filtered students.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of students in body.
     */
    @ApiOperation(value = "Filtered Students list with pagination and logical operator join", response = List.class)
    @GetMapping("/students/filtered/{join}")
    public ResponseEntity<List<StudentDTO>> getAllFilteredStudents(
            @ApiParam(value = "Logical operators (AND-OR) for join expressions")
            @PathVariable String join, StudentCriteria criteria, Pageable pageable)
    {
        if (!(join.equalsIgnoreCase("AND") || join.equalsIgnoreCase("OR"))) {
            throw new BadRequestAlertException("Wrong logical operator", ENTITY_NAME, "badoperatorjoin", join);
        }
        Page<StudentDTO> page = service.findByCriteria(join, criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code PATCH  /students/:id} : Partial updates given fields of an existing student, field will ignore if it is null
     *
     * @param id the id of the studentDTO to save.
     * @param studentDTO the information to update.
     * @param avatar the avatar to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated studentDTO,
     * or with status {@code 400 (Bad Request)} if the studentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the studentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the cardDTO couldn't be updated.
     */
    @PatchMapping(value = "/students/{id}")
    public ResponseEntity<StudentDTO> updatePersonalData(
            @PathVariable(value = "id", required = false) final UUID id,
            @NotNull @RequestPart(name = "student") StudentDTO studentDTO,
            @RequestPart(name = "avatar", required = false) MultipartFile avatar
    ) {
        if (studentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idnull", "idnull");
        }

        if (!Objects.equals(id, studentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid", "idnull");
        }

        this.checkMimeType(avatar);

        Boolean exists = service.exists(id);
        if (!exists) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            StudentDTO updatedStudent = service.partialUpdate(studentDTO, avatar);
            return ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                            true, ENTITY_NAME,
                            updatedStudent.getName()))
                    .body(updatedStudent);
        }
    }

    /**
     * Check if content type of avatar is JPEG of PNG
     * @param avatar {@link MultipartFile} image
     * @throws BadRequestAlertException if content type of is not JPEG or PNG
     */
    private void checkMimeType(MultipartFile avatar) {
        if (avatar != null && (!MimeTypeUtils.IMAGE_JPEG_VALUE.equalsIgnoreCase(avatar.getContentType()) &&
                !MimeTypeUtils.IMAGE_PNG_VALUE.equalsIgnoreCase(avatar.getContentType()))) {
            throw new BadRequestAlertException("ContentType not allowed ", ENTITY_NAME, "wrongtype", avatar.getOriginalFilename());
        }
    }
}
