package sands.azure.iot.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import sands.azure.iot.entities.DeviceInformation;

import java.util.List;

@Repository
public interface DeviceInformationRepository extends MongoRepository<DeviceInformation,String> {
    List<DeviceInformation> findByDeviceType(String deviceType);

    DeviceInformation findByDeviceId(String deviceId);
}
