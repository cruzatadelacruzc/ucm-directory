package cu.sld.ucmgt.directory.web.rest.errors;

import java.net.URI;

public final class ErrorConstants {

    public static final String ERR_VALIDATION = "error:validation";
    public static final String ERR_CONCURRENCY_FAILURE = "error:concurrencyFailure";
    private static final String PROBLEM_BASE_URL = "http://localhost:8080/problem";
    static final URI DEFAULT_TYPE = URI.create(PROBLEM_BASE_URL + "/problem-with-message");
    static final URI CONSTRAINT_VIOLATION_TYPE = URI.create(PROBLEM_BASE_URL + "/constraint-violation");

    private ErrorConstants() {
    }
}
