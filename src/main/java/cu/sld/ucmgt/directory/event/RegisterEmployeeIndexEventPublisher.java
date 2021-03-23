package cu.sld.ucmgt.directory.event;

import cu.sld.ucmgt.directory.domain.elasticsearch.EmployeeIndex;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class RegisterEmployeeIndexEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Save a new SavedEmployeeIndexEvent event
     *
     * @param employeeId    identifier of the saved {@link EmployeeIndex}
     * @param employeeIndex information to save a {@link EmployeeIndex}
     */
    public void publishSavedEmployeeIndexEvent(String employeeId, @NotNull Map<String, Object> employeeIndex) {
        log.debug("Request to publish a UpdatedEmployeeIndexEvent with ID {}", employeeId);
        final UpdatedEmployeeIndexEvent employeeIndexEvent = new UpdatedEmployeeIndexEvent(employeeId, employeeIndex);
        eventPublisher.publishEvent(employeeIndexEvent);
    }
}
