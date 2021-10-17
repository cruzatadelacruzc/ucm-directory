package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.WorkPlaceService;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.web.rest.errors.BadRequestAlertException;
import cu.sld.ucmgt.directory.web.rest.util.HeaderUtil;
import cu.sld.ucmgt.directory.web.rest.util.PaginationUtil;
import cu.sld.ucmgt.directory.web.rest.util.ResponseUtil;
import cu.sld.ucmgt.directory.web.rest.vm.ChangeStatusVM;
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
public class WorkPlaceResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final WorkPlaceService service;
    private static final String ENTITY_NAME = "WorkPlace";

    /**
     * {@code POST  /workplaces} : Create a new workplace.
     *
     * @param workPlaceDTO the employeeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)}
     * and with body the new workplaceDTO, or with status {@code 400 (Bad Request)} if the phone has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/workplaces")
    public ResponseEntity<WorkPlaceDTO> createWorkPlace(@Valid @RequestBody WorkPlaceDTO workPlaceDTO) throws URISyntaxException {
        log.debug("REST request to save Workplace : {}", workPlaceDTO);
        if (workPlaceDTO.getId() != null) {
            throw new BadRequestAlertException("A new workplace cannot already have an ID", ENTITY_NAME, "idexists", workPlaceDTO.getId().toString());
        }
        WorkPlaceDTO workPlaceSaved = service.save(workPlaceDTO);
        return ResponseEntity.created(new URI("/api/workplaces/" + workPlaceSaved.getId()))
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        workPlaceSaved.getId().toString()))
                .body(workPlaceSaved);
    }

    /**
     * {@code PUT  /workplaces} : Updates an existing workplace.
     *
     * @param workPlaceDTO the workPlaceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated workPlaceDTO,
     * or with status {@code 400 (Bad Request)} if the employeeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the employeeDTO couldn't be updated.
     */
    @PutMapping("/workplaces")
    public ResponseEntity<WorkPlaceDTO> updateWorkPlace(@Valid @RequestBody WorkPlaceDTO workPlaceDTO) {
        log.debug("REST request to update Employee : {}", workPlaceDTO);
        if (workPlaceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull", "idnull");
        }
        WorkPlaceDTO workPlaceSaved = service.save(workPlaceDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        workPlaceSaved.getId().toString()))
                .body(workPlaceSaved);
    }

    /**
     * {@code DELETE  /workplaces/:id} : delete the "id" workplace.
     *
     * @param uid the id of the WorkPlace to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/workplaces/{id}")
    public ResponseEntity<Void> deleteWorkPlace(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to delete WorkPlace : {}", uid);
        service.deleteWorkPlace(uid);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                        applicationName,
                        true, ENTITY_NAME,
                        uid.toString()))
                .build();
    }

    /**
     * {@code GET  /workplaces/:id} : get the "id" workplace.
     *
     * @param uid the id of the workPlaceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the workPlaceDTO
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/workplaces/{id}")
    public ResponseEntity<WorkPlaceDTO> getWorkPlace(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to get WorkPlace : {}", uid);
        Optional<WorkPlaceDTO> workplaceFetched = service.getWorkPlace(uid);
        return ResponseUtil.wrapOrNotFound(workplaceFetched);
    }

    /**
     * {@code GET  /workplaces} : get all the workplace.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of employees in body.
     */
    @GetMapping("/workplaces")
    public ResponseEntity<List<WorkPlaceDTO>> getAllWorkPlaces(Pageable pageable) {
        log.debug("REST request to get a page of WorkPlace");
        Page<WorkPlaceDTO> page = service.getAllWorkPlaces(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code PUT  /workplaces/status} : Change status an existing workplace.
     *
     * @param changeStatusVM the information to change status.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body boolean result
     */
    @PutMapping("/workplaces/status")
    public ResponseEntity<Boolean> updateStatusPhone(@Valid @RequestBody ChangeStatusVM changeStatusVM) {
        log.debug("REST request to update status WorkPlace : {}", changeStatusVM);
        Boolean result = service.changeStatus(changeStatusVM.getId(), changeStatusVM.getStatus());
        return ResponseEntity.ok().body(result);
    }
}
