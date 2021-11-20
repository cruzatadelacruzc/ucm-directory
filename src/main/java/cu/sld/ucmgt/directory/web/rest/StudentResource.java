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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
    @PostMapping("/students")
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDTO) throws URISyntaxException {
        log.debug("REST request to save Student : {}", studentDTO);
        if (studentDTO.getId() != null) {
            throw new BadRequestAlertException("A new student cannot already have an ID", ENTITY_NAME, "idexists", studentDTO.getId().toString());
        }
        StudentDTO studentSaved = service.save(studentDTO);
        return ResponseEntity.created(new URI("/api/students/" + studentSaved.getId()))
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        studentSaved.getId().toString()))
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
    @PutMapping("/students")
    public ResponseEntity<StudentDTO> updateStudent(@Valid @RequestBody StudentDTO studentDTO){
        log.debug("REST request to update Student : {}", studentDTO);
        if (studentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull", "idnull");
        }

        StudentDTO studentSaved = service.save(studentDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        studentSaved.getId().toString()))
                .body(studentSaved);
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
}
