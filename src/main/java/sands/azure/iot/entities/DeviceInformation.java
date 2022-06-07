package sands.azure.iot.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Document("AzureDevices")
public class DeviceInformation {
    private ObjectId _id;
    private String deviceId;
    private Integer deviceNumber;
    private String deviceType;
    private String azureiot_createdate;
    private String azureiot_uuid;
    private String azuredocdb_createdate;
    private Boolean nonFunctional;
}
