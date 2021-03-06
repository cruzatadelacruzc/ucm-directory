package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.WorkPlaceService;
import cu.sld.ucmgt.directory.service.criteria.WorkPlaceCriteria;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
import cu.sld.ucmgt.directory.web.rest.errors.BadRequestAlertException;
import cu.sld.ucmgt.directory.web.rest.util.HeaderUtil;
import cu.sld.ucmgt.directory.web.rest.util.PaginationUtil;
import cu.sld.ucmgt.directory.web.rest.util.ResponseUtil;
import cu.sld.ucmgt.directory.web.rest.vm.ChangeStatusVM;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    @PostMapping(value = "/workplaces",  consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<WorkPlaceDTO> createWorkPlace(@Valid @RequestPart(name = "workplace") WorkPlaceDTO workPlaceDTO,
                                                        @RequestPart(name = "avatar", required = false) MultipartFile avatar)
            throws URISyntaxException {

        if (workPlaceDTO.getId() != null) {
            throw new BadRequestAlertException("A new workplace cannot already have an ID", ENTITY_NAME, "idexists", workPlaceDTO.getId().toString());
        }

        this.checkMimeType(avatar);

        WorkPlaceDTO workPlaceSaved = service.create(workPlaceDTO, avatar);
        return ResponseEntity.created(new URI("/api/workplaces/" + workPlaceSaved.getId()))
                .headers(HeaderUtil.createEntityExecutedAlert(applicationName,
                        true, ENTITY_NAME,
                        workPlaceSaved.getName()))
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
    @PutMapping(value = "/workplaces",  consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<WorkPlaceDTO> updateWorkPlace(@Valid @RequestPart(name = "workplace") WorkPlaceDTO workPlaceDTO,
                                                        @RequestPart(name = "avatar", required = false) MultipartFile avatar) {
        if (workPlaceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull", "idnull");
        }

        this.checkMimeType(avatar);

        WorkPlaceDTO workPlaceSaved = service.update(workPlaceDTO, avatar);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        workPlaceSaved.getName()))
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
    public ResponseEntity<Boolean> updateStatusWorkPlace(@Valid @RequestBody ChangeStatusVM changeStatusVM) {
        log.debug("REST request to update status WorkPlace : {}", changeStatusVM);
        Boolean result = service.changeStatus(changeStatusVM.getId(), changeStatusVM.getStatus());
        return ResponseEntity.ok().body(result);
    }

    /**
     * {@code GET  /workplaces/filtered/{join}} : get all the filtered workplaces.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of workplaces in body.
     */
    @ApiOperation(value = "Filtered WorkPlaces list with pagination and logical operator join", response = List.class)
    @GetMapping("/workplaces/filtered/{join}")
    public ResponseEntity<List<WorkPlaceDTO>> getAllFilteredWorkPlaces(
            @ApiParam(value = "Logical operators (AND-OR) for join expressions")
            @PathVariable String join, WorkPlaceCriteria criteria, Pageable pageable)
    {
        if (!(join.equalsIgnoreCase("AND") || join.equalsIgnoreCase("OR"))) {
            throw new BadRequestAlertException("Wrong logical operator", ENTITY_NAME, "badoperatorjoin", join);
        }
        Page<WorkPlaceDTO> page = service.findByCriteria(join, criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code DELETE  /workplaces/avatar/:id} : delete the avatar belong to "id" workplace.
     * @param workplaceId identifier
     * @return the {@link ResponseEntity} with status {@code 200 (NO_CONTENT)} and with body the boolean response.
     */
    @DeleteMapping("/workplaces/avatar/{id}")
    public ResponseEntity<Boolean> deleteAvatar(@PathVariable(name = "id") UUID workplaceId) {
        Boolean response = service.deleteAvatar(workplaceId);
        return ResponseEntity.ok(response);
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
