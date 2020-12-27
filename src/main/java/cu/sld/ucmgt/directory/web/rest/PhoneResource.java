package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.PhoneService;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
import cu.sld.ucmgt.directory.web.rest.errors.BadRequestAlertException;
import cu.sld.ucmgt.directory.web.rest.util.HeaderUtil;
import cu.sld.ucmgt.directory.web.rest.util.PaginationUtil;
import cu.sld.ucmgt.directory.web.rest.util.ResponseUtil;
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
public class PhoneResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final PhoneService service;
    private static final String ENTITY_NAME = "directoryPhone";

    /**
     * {@code POST  /phones} : Create a new phone.
     *
     * @param phoneDTO the phoneDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)}
     * and with body the new phoneDTO, or with status {@code 400 (Bad Request)} if the phone has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/phones")
    public ResponseEntity<PhoneDTO> createPhone(@Valid @RequestBody PhoneDTO phoneDTO) throws URISyntaxException {
        log.debug("REST request to save Phone : {}", phoneDTO);
        if (phoneDTO.getId() != null) {
            throw new BadRequestAlertException("A new phone cannot already have an ID", ENTITY_NAME, "idexists");
        }

        if (phoneDTO.getWorkPlaceId() == null && phoneDTO.getEmployeeId() == null){
            throw new BadRequestAlertException("workPlaceId or personId, both must not be null", ENTITY_NAME, "relationshipnull");
        }
        PhoneDTO phoneSaved = service.save(phoneDTO);
        return ResponseEntity.created(new URI("/api/phones/" + phoneDTO.getId()))
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        phoneSaved.getId().toString()))
                .body(phoneSaved);
    }

    /**
     * {@code GET  /phones} : get all the phone.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of phones in body.
     */
    @GetMapping("/phones")
    public ResponseEntity<List<PhoneDTO>> getAllPhones(Pageable pageable){
        log.debug("REST request to get a page of Phones");
        Page<PhoneDTO> page = service.getAllPhones(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /phones/:id} : get the "id" phone.
     *
     * @param uid the id of the employeeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the phoneDTO
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/phones/{id}")
    public ResponseEntity<PhoneDTO> getEmployee(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to get Phone : {}", uid);
        Optional<PhoneDTO> phoneFetched = service.getPhone(uid);
        return ResponseUtil.wrapOrNotFound(phoneFetched);
    }


    /**
     * {@code DELETE  /phones/:id} : delete the "id" phone.
     *
     * @param uid the id of the Phone to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/phones/{id}")
    public ResponseEntity<Void> deletePhone(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to delete Phone : {}", uid);
        service.deletePhone(uid);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                        applicationName,
                        true, ENTITY_NAME,
                        uid.toString()))
                .build();
    }

    /**
     * {@code PUT  /phones} : Updates an existing phone.
     *
     * @param phoneDTO the phoneDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated phoneDTO,
     * or with status {@code 400 (Bad Request)} if the phoneDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the phoneDTO couldn't be updated.
     */
    @PutMapping("/phones")
    public ResponseEntity<PhoneDTO> updatePhone(@Valid @RequestBody PhoneDTO phoneDTO) {
        log.debug("REST request to update Phone : {}", phoneDTO);
        if (phoneDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (phoneDTO.getWorkPlaceId() == null && phoneDTO.getEmployeeId() == null){
            throw new BadRequestAlertException("workPlaceId or personId, both must not be null", ENTITY_NAME, "relationshipnull");
        }
        PhoneDTO phoneSaved = service.save(phoneDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        phoneSaved.getId().toString()))
                .body(phoneSaved);

    }
}
