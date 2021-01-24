package cu.sld.ucmgt.directory.domain;

public enum NomenclatureType {
    CATEGORIA("category"),
    CARGO("charge"),
    DISTRITO("district"),
    PROFESION("profession"),
    GRADO_CIENTIFICO("scientificDegree"),
    CATEGORIA_DOCENTE("teachingCategory"),
    ESPECIALIDAD("specialty");

    private final String shortCode;

    NomenclatureType(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}
