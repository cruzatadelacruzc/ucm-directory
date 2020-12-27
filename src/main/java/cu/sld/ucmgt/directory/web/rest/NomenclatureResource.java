package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.NomenclatureService;
import cu.sld.ucmgt.directory.service.dto.EmployeeDTO;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
import cu.sld.ucmgt.directory.service.dto.WorkPlaceDTO;
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
public class NomenclatureResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final NomenclatureService service;
    private static final String ENTITY_NAME = "directoryNomenclature";

    /**
     * {@code POST  /nomenclatures} : Create a new nomenclature.
     *
     * @param nomenclatureDTO the nomenclatureDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)}
     * and with body the new nomenclatureDTO, or with status {@code 400 (Bad Request)} if the phone has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/nomenclatures")
    public ResponseEntity<NomenclatureDTO> createNomenclature(@Valid @RequestBody NomenclatureDTO nomenclatureDTO) throws URISyntaxException {
        log.debug("REST request to create Nomenclature : {}", nomenclatureDTO);
        if (nomenclatureDTO.getId() != null) {
            throw new BadRequestAlertException("A new nomenclature cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (service.findNomenclatureByNameAndDiscriminator(nomenclatureDTO.getName(), nomenclatureDTO.getDiscriminator()).isPresent()) {
            throw new BadRequestAlertException("Nomenclature name: "
                    + nomenclatureDTO.getName() +
                    " of type: "
                    + nomenclatureDTO.getDiscriminator() +" already used",
                    ENTITY_NAME, "idexists");
        }
        NomenclatureDTO nomenclatureSaved = service.save(nomenclatureDTO);
        return ResponseEntity.created(new URI("/api/nomenclatures/" + nomenclatureSaved.getId()))
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        nomenclatureSaved.getId().toString()))
                .body(nomenclatureSaved);
    }

    /**
     * {@code PUT  /nomenclatures} : Updates an existing nomenclature.
     *
     * @param nomenclatureDTO the nomenclatureDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nomenclatureDTO,
     * or with status {@code 400 (Bad Request)} if the nomenclatureDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the employeeDTO couldn't be updated.
     */
    @PutMapping("/nomenclatures")
    public ResponseEntity<NomenclatureDTO> updateNomenclature(@Valid @RequestBody NomenclatureDTO nomenclatureDTO){
        log.debug("REST request to update Nomenclature : {}", nomenclatureDTO);
        if (nomenclatureDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        NomenclatureDTO nomenclatureSaved = service.save(nomenclatureDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        nomenclatureSaved.getId().toString()))
                .body(nomenclatureSaved);
    }

    /**
     * {@code DELETE  /nomenclatures/:id} : delete the "id" nomenclature.
     *
     * @param uid the id of the Nomenclature to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/nomenclatures/{id}")
    public ResponseEntity<Void> deleteNomenclature(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to delete Nomenclature : {}", uid);
        service.deleteNomenclature(uid);
        return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(
                        applicationName,
                        true, ENTITY_NAME,
                        uid.toString()))
                .build();
    }

    /**
     * {@code GET  /nomenclatures/:id} : get the "id" nomenclature.
     *
     * @param uid the id of the Nomenclature to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the workPlaceDTO
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/nomenclatures/{id}")
    public ResponseEntity<NomenclatureDTO> getNomenclature(@PathVariable(name = "id") UUID uid) {
        log.debug("REST request to get Nomenclatures : {}", uid);
        Optional<NomenclatureDTO> nomenclatureFetched = service.getNomenclatures(uid);
        return ResponseUtil.wrapOrNotFound(nomenclatureFetched);
    }

    /**
     * {@code GET  /nomenclatures} : get all the workplace.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of nomenclatures in body.
     */
    @GetMapping("/nomenclatures")
    public ResponseEntity<List<NomenclatureDTO>> getAllNomenclatures(Pageable pageable) {
        log.debug("REST request to get a page of Nomenclature");
        Page<NomenclatureDTO> page = service.getAllNomenclatures(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

}
