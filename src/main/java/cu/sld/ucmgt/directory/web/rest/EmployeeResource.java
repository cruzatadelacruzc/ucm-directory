package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.EmployeeService;
import cu.sld.ucmgt.directory.service.criteria.EmployeeCriteria;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
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
public class EmployeeResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final EmployeeService service;
    private static final String ENTITY_NAME = "Employee";

    /**
     * {@code POST  /employees} : Create a new employee.
     *
     * @param employeeDTO the employeeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)}
     * and with body the new employeeDTO, or with status {@code 400 (Bad Request)} if the phone has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/employees")
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) throws URISyntaxException {
        log.debug("REST request to save Employee : {}", employeeDTO);
        if (employeeDTO.getId() != null) {
            throw new BadRequestAlertException("A new employee cannot already have an ID", ENTITY_NAME, "idexists", employeeDTO.getId().toString());
        }

        if (employeeDTO.getEndDate() != null && employeeDTO.getStartDate().isAfter(employeeDTO.getEndDate())) {
            throw new BadRequestAlertException("End Date greater than Start Date", ENTITY_NAME, "enddategt", employeeDTO.getEndDate().toString());
        }
        EmployeeDTO employeeSaved = service.create(employeeDTO);
        return ResponseEntity.created(new URI("/api/employees/" + employeeSaved.getId()))
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        employeeSaved.getId().toString()))
                .body(employeeSaved);
    }

    /**
     * {@code PUT  /employees} : Updates an existing employee.
     *
     * @param employeeDTO the employeeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated employeeDTO,
     * or with status {@code 400 (Bad Request)} if the employeeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the employeeDTO couldn't be updated.
     */
    @PutMapping("/employees")
    public ResponseEntity<EmployeeDTO> updateEmployee(@Valid @RequestBody EmployeeDTO employeeDTO){
        log.debug("REST request to update Employee : {}", employeeDTO);
        if (employeeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull", "idnull");
        }

        if (employeeDTO.getEndDate() != null && employeeDTO.getStartDate().isAfter(employeeDTO.getEndDate())) {
            throw new BadRequestAlertException("End Date greater than Start Date", ENTITY_NAME, "enddategt",employeeDTO.getEndDate().toString());
        }

        EmployeeDTO employeeSaved = service.update(employeeDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        employeeSaved.getId().toString()))
                .body(employeeSaved);
    }

    /**
     * {@code DELETE  /employees/:id} : delete the "id" employee.
     *
     * @param uid the id of the Employee to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to delete Employee : {}", uid);
        service.deleteEmployee(uid);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                        applicationName,
                        true, ENTITY_NAME,
                        uid.toString()))
                .build();
    }

    /**
     * {@code GET  /employees/:id} : get the "id" employee.
     *
     * @param uid the id of the employeeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the employeeDTO
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> getEmployee(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to get Person : {}", uid);
        Optional<EmployeeDTO> employeeFetched = service.getEmployee(uid);
        return ResponseUtil.wrapOrNotFound(employeeFetched);
    }

    /**
     * {@code GET  /employees} : get all the employee.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of employees in body.
     */
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees(Pageable pageable) {
        log.debug("REST request to get a page of Employees");
        Page<EmployeeDTO> page = service.getAllEmployees(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /employees/filtered/{join}} : get all the filtered employees.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of employees in body.
     */
    @ApiOperation(value = "Filtered Employees list with pagination and logical operator join", response = List.class)
    @GetMapping("/employees/filtered/{join}")
    public ResponseEntity<List<EmployeeDTO>> getAllFilteredEmployees(
            @ApiParam(value = "Logical operators (AND-OR) for join expressions")
            @PathVariable String join, EmployeeCriteria criteria, Pageable pageable)
    {
        if (!(join.equalsIgnoreCase("AND") || join.equalsIgnoreCase("OR"))) {
            throw new BadRequestAlertException("Wrong logical operator", ENTITY_NAME, "badoperatorjoin", join);
        }
        Page<EmployeeDTO> page = service.findByCriteria(join, criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

}
