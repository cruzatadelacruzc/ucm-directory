package cu.sld.ucmgt.directory.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BadRequestAlertException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String errorKey;

    private final String params;

    public BadRequestAlertException(String defaultMessage, String entityName, String errorKey, String param) {
        this(ErrorConstants.DEFAULT_TYPE, defaultMessage, entityName, errorKey, param);
    }

    public BadRequestAlertException(URI type, String defaultMessage, String entityName, String errorKey, String params) {
        super(type, defaultMessage, Status.BAD_REQUEST, null, null, null, getAlertParameters(params, errorKey));
        this.entityName = entityName;
        this.errorKey = errorKey;
        this.params = params;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public String getParams() {
        return params;
    }

    private static Map<String, Object> getAlertParameters(String params, String errorKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("message", "error:" + errorKey);
        parameters.put("params", params);
        return parameters;
    }
}
