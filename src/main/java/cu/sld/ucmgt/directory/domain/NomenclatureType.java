package cu.sld.ucmgt.directory.domain;

public enum NomenclatureType {
    CATEGORIA("CATEGORY"),
    CARGO("CHARGE"),
    DISTRITO("DISTRICT"),
    PROFESION("PROFESSION"),
    GRADO_CIENTIFICO("SCIENTIFIC_DEGREE"),
    CATEGORIA_DOCENTE("TEACHING_CATEGORY"),
    ESPECIALIDAD("SPECIALTY"),
    TIPO("KIND"),
    CENTRO_ESTUDIO("STUDY_CENTER");

    private final String shortCode;

    NomenclatureType(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}
