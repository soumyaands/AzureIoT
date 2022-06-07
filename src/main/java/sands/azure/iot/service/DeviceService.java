package sands.azure.iot.service;

import org.springframework.http.ResponseEntity;
import sands.azure.iot.entities.D2CData;
import sands.azure.iot.entities.DeviceInformation;

import java.util.List;
import java.util.Map;

/**
 * @author Soumya Mitra
 */
public interface DeviceService {
    /**
     * Register n-device with naming prefix.
     * @param deviceNamePrefix prefix of naming logic.
     * @param numberOfDevices number of devices with given naming prefix to configure.
     * @return List of Information for the Devices Created.
     */
    List<DeviceInformation> registerDevice(String deviceNamePrefix,Integer numberOfDevices);

    /**
     * Show Messages in Service Bus if Any Pending.
     * @return List of Messages in Service Bus
     */
    List<Object> showServiceBusMessages();

    /**
     * Show List of all created devices Information.
     * @return List Information of available devices
     */
    List<DeviceInformation> showAllDevices();

    /**
     * Show Device Information for given Device ID.
     * @param deviceId Device ID to query with.
     * @return Device Information.
     */
    DeviceInformation showDevice(String deviceId);

    /**
     * Remove a non-functional Device.
     * @param deviceId Device ID of non-functional device.
     * @return Weather Deleted or not.
     */
    Boolean deleteDevice(String deviceId);

    /**
     * Send Data from Device to Cloud.
     * @param data Data to send along with its deviceId.
     * @return If data sent or not.
     */
    Boolean sendD2CMessageServiceHTTPS(D2CData data);
}
