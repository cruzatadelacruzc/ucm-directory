package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.PhoneService;
import cu.sld.ucmgt.directory.service.dto.PhoneDTO;
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
public class PhoneResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final PhoneService service;
    private static final String ENTITY_NAME = "Phone";

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
            throw new BadRequestAlertException("A new phone cannot already have an ID", ENTITY_NAME, "idexists", phoneDTO.getId().toString());
        }

        if ((phoneDTO.getWorkPlaceId() == null && phoneDTO.getEmployeeId() == null) ||
            (phoneDTO.getWorkPlaceId() != null && phoneDTO.getEmployeeId() != null)) {
            throw new BadRequestAlertException("Phone below to WorkPlace or Employee", ENTITY_NAME, "relationshipnull", phoneDTO.getNumber().toString());
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
    public ResponseEntity<List<PhoneDTO>> getAllPhones(Pageable pageable) {
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
     * @param uid the id of the phoneDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the phoneDTO
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/phones/{id}")
    public ResponseEntity<PhoneDTO> getPhone(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to get Phone : {}", uid);
        Optional<PhoneDTO> phoneFetched = service.getPhone(uid);
        return ResponseUtil.wrapOrNotFound(phoneFetched);
    }


    /**
     * {@code DELETE  /phones/:number} : delete the "number" phone.
     *
     * @param number the id of the Phone to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/phones/{number}")
    public ResponseEntity<Void> deletePhone(@PathVariable Integer number) {
        log.debug("REST request to delete Phone : {}", number);
        service.deletePhone(number);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                        applicationName,
                        true, ENTITY_NAME,
                        number.toString()))
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
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull", "idnull");
        }
        if ((phoneDTO.getWorkPlaceId() == null && phoneDTO.getEmployeeId() == null) ||
                (phoneDTO.getWorkPlaceId() != null && phoneDTO.getEmployeeId() != null)) {
            throw new BadRequestAlertException("Phone below to WorkPlace or Employee", ENTITY_NAME, "relationshipnull", phoneDTO.getNumber().toString());
        }
        PhoneDTO phoneSaved = service.save(phoneDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        phoneSaved.getId().toString()))
                .body(phoneSaved);

    }

    /**
     * {@code PUT  /phones/status} : Change status an existing phone.
     *
     * @param changeStatusVM the information to change status.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body boolean result
     */
    @PutMapping("/phones/status")
    public ResponseEntity<Boolean> updateStatusPhone(@Valid @RequestBody ChangeStatusVM changeStatusVM) {
        log.debug("REST request to update status Phone : {}", changeStatusVM);
        Boolean result = service.changeStatus(changeStatusVM.getId(), changeStatusVM.getStatus());
        return ResponseEntity.ok().body(result);
    }
}
