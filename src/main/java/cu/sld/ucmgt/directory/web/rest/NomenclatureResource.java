package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.domain.NomenclatureType;
import cu.sld.ucmgt.directory.service.NomenclatureService;
import cu.sld.ucmgt.directory.service.criteria.NomenclatureCriteria;
import cu.sld.ucmgt.directory.service.dto.NomenclatureDTO;
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
public class NomenclatureResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final NomenclatureService service;
    private static final String ENTITY_NAME = "Nomenclature";

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
            throw new BadRequestAlertException("A new nomenclature cannot already have an ID", ENTITY_NAME, "idexists", nomenclatureDTO.getId().toString());
        }

        this.checkNomenclatureWithNameAndDiscriminatorExist(nomenclatureDTO);

        NomenclatureDTO nomenclatureSaved = service.create(nomenclatureDTO);
        return ResponseEntity.created(new URI("/api/nomenclatures/" + nomenclatureSaved.getId()))
                .headers(HeaderUtil.createEntityExecutedAlert(applicationName,
                        true, ENTITY_NAME,
                        nomenclatureSaved.getName()))
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
    public ResponseEntity<NomenclatureDTO> updateNomenclature(@Valid @RequestBody NomenclatureDTO nomenclatureDTO) {
        if (nomenclatureDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull", "idnull");
        }

        this.checkNomenclatureWithNameAndDiscriminatorExist(nomenclatureDTO);

        NomenclatureDTO nomenclatureSaved = service.update(nomenclatureDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName,
                        true, ENTITY_NAME,
                        nomenclatureSaved.getName()))
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
        Optional<NomenclatureDTO> nomenclatureFetched = service.getNomenclature(uid);
        return ResponseUtil.wrapOrNotFound(nomenclatureFetched);
    }

    /**
     * {@code GET  /nomenclatures} : get all the nomenclatures.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of nomenclatures in body.
     */
    @GetMapping("/nomenclatures")
    public ResponseEntity<List<NomenclatureDTO>> getAllNomenclatures(
            Pageable pageable,
            @RequestParam(name = "unpaged", required = false) boolean unpaged
    ) {
        log.debug("REST request to get a page of Nomenclature");
        Page<NomenclatureDTO> page = service.getAllNomenclatures(unpaged ? Pageable.unpaged(): pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /nomenclatures/filtered/:type/:join} : get a page of filtered nomenclature and discriminator
     *
     * @param discriminator as type nomenclature discriminator
     * @param join          union operator
     * @param criteria      search criteria to filter
     * @param pageable      the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of nomenclatures in body.
     */
    @GetMapping("/nomenclatures/filtered/{type}/{join}")
    public ResponseEntity<List<NomenclatureDTO>> getAllByDiscriminator(
            @PathVariable(name = "type") NomenclatureType discriminator,
            @ApiParam(value = "Logical operators (AND-OR) for join expressions") @PathVariable String join,
            NomenclatureCriteria criteria, Pageable pageable,
            @RequestParam(name = "unpaged", required = false) boolean unpaged) {
        if (!(join.equalsIgnoreCase("AND") || join.equalsIgnoreCase("OR"))) {
            throw new BadRequestAlertException("Wrong logical operator", ENTITY_NAME, "badoperatorjoin", join);
        }
        Page<NomenclatureDTO> page = service.getAllByStatusAndDiscriminator(
                join,
                criteria,
                discriminator,
                unpaged ? Pageable.unpaged() : pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /nomenclatures/filtered/{join}} : get all the filtered nomenclatures.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of nomenclatures in body.
     */
    @ApiOperation(value = "Filtered Nomenclatures list with pagination and logical operator join", response = List.class)
    @GetMapping("/nomenclatures/filtered/{join}")
    public ResponseEntity<List<NomenclatureDTO>> getAllFilteredNomenclatures(
            @ApiParam(value = "Logical operators (AND-OR) for join expressions")
            @PathVariable String join, NomenclatureCriteria criteria, Pageable pageable)
    {
        if (!(join.equalsIgnoreCase("AND") || join.equalsIgnoreCase("OR"))) {
            throw new BadRequestAlertException("Wrong logical operator", ENTITY_NAME, "badoperatorjoin", join);
        }
        Page<NomenclatureDTO> page = service.findByCriteria(join, criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(),
                page
        );
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    private void checkNomenclatureWithNameAndDiscriminatorExist(NomenclatureDTO nomenclatureDTO) {
        service.getNomenclatureByIdAndNameAndDiscriminator(nomenclatureDTO.getId(), nomenclatureDTO.getName(), nomenclatureDTO.getDiscriminator())
                .ifPresent( nomenclature -> {
                    throw new BadRequestAlertException("Nomenclature " + nomenclatureDTO.getName() + " of type: "
                + nomenclatureDTO.getDiscriminator() + " already used", ENTITY_NAME, "nomenclatureexists", nomenclature.getName());
                });


    }

}
