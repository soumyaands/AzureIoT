package sands.azure.iot.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
@Document("AzureDevice2CloudDataLog")
public class D2CData {
    @JsonProperty("DeviceId")
    private String deviceId;
    @JsonProperty("RotationSpeed")
    @JsonAlias("RotationSpeed")
    private String dataField1;
    @JsonProperty("Duration")
    @JsonAlias("Duration")
    private String dataField2;
    @JsonProperty("AmountPaid")
    @JsonAlias("AmountPaid")
    private String dataField3;
//    private String deviceId;
//    private int messageId;
//    private double temperature;
//    private double humidity;
}
