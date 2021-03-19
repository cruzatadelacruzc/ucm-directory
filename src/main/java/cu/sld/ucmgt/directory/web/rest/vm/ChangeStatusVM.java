package cu.sld.ucmgt.directory.web.rest.vm;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class ChangeStatusVM {

    @NotNull
    private UUID id;

    private Boolean status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
