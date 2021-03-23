package cu.sld.ucmgt.directory.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class UpdatedEmployeeIndexEvent {
    String employeeId;
    Map<String,Object> params;
}
