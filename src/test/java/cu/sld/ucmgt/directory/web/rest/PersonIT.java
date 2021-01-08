package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.domain.Gender;

import java.time.LocalDate;
import java.time.ZoneId;

public class PersonIT {

    public static final Integer UPDATE_AGE = 29;
    public static final Integer DEFAULT_AGE = 28;

    public static final String UPDATE_RACE = "Negro";
    public static final String DEFAULT_RACE = "Blanco";

    public static final String DEFAULT_NAME = "CESAR";
    public static final String UPDATE_NAME = "MANUEL";

    public static final String UPDATE_CI = "86082221001";
    public static final String DEFAULT_CI = "91061721000";

    public static final Gender UPDATE_GENDER = Gender.Masculino;
    public static final Gender DEFAULT_GENDER = Gender.Femenino;

    public static final String UPDATE_EMAIL = "admin@mail.com";
    public static final String DEFAULT_EMAIL = "user@mail.com";

    public static final String UPDATE_FIRST_LAST_NAME = "ALVAREZ";
    public static final String DEFAULT_FIRST_LAST_NAME = "CRUZATA";

    public static final String UPDATE_SECOND_LAST_NAME = "CASTILLO";
    public static final String DEFAULT_SECOND_LAST_NAME = "DE LA CRUZ";

    public static final String UPDATE_ADDRESS = " FANGO AL PECHO";
    public static final String DEFAULT_ADDRESS = "DIENTE Y CAJA DE MUELAS";

    public static final LocalDate UPDATE_BIRTHDATE = LocalDate.now(ZoneId.systemDefault());
    public static final LocalDate DEFAULT_BIRTHDATE = LocalDate.ofEpochDay(0L);
}
