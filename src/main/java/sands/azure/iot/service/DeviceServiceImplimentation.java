package sands.azure.iot.service;

import com.google.gson.Gson;
import com.microsoft.azure.iot.service.exceptions.IotHubException;
import com.microsoft.azure.iot.service.sdk.Device;
import com.microsoft.azure.iot.service.sdk.DeviceStatus;
import com.microsoft.azure.iot.service.sdk.RegistryManager;
import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sands.azure.iot.entities.D2CData;
import sands.azure.iot.entities.DeviceInformation;
import sands.azure.iot.repository.DeviceInformationRepository;
import sands.azure.iot.utilities.Constants;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
public class DeviceServiceImplimentation implements DeviceService {
    private static final long DEVICE_TO_CLOUD_MESSAGE_TIMEOUT = 2000;
    private static final List<String> failedMessageListOnClose = new ArrayList<>(); // List of messages that failed on close
    private static RegistryManager registryManager = null;
    private Configuration configuration = null;
    private ServiceBusContract serviceBusContract = null;
    @Autowired
    private DeviceInformationRepository deviceInformationRepository;

    @PostConstruct
    void init() {
        try {
            registryManager = RegistryManager.createFromConnectionString(Constants.REGISTRATION_CONNECTION_STRING);
            configuration = ServiceBusConfiguration.configureWithSASAuthentication(Constants.SBUSNAME, "RootManagerSharedAccessKey", Constants.SBUSKEY, ".servicebus.windows.net");
            serviceBusContract = ServiceBusService.create(configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DeviceInformation> registerDevice(String deviceNamePrefix, Integer numberOfDevices) {
        Integer existingDevices = deviceSeriesExists(deviceNamePrefix);
        List<DeviceInformation> deviceInformations = new ArrayList<>();
        {
            for (int devicePos = 0 + existingDevices; devicePos < numberOfDevices + existingDevices; devicePos++) {
                boolean result = false, result1 = false, result2 = false;
                try {
                    Map<String, Object> deviceObject = new HashMap<>();
                    deviceObject.put("deviceId", deviceNamePrefix + "_" + devicePos);
                    deviceObject.put("deviceNumber", devicePos);
                    deviceObject.put("deviceType", deviceNamePrefix);
//                    Gson gson = new Gson();
//                    JsonObject deviceJsonObject = gson.fromJson(deviceObject, JsonObject.class);
                    result1 = azureIoTDeviceRegistration(deviceObject);
                    result2 = persistDeviceModel(deviceObject, deviceInformations);
                    if (result1 && result2) result = true;
                    else throw new RuntimeException("SomeHorribleThingHappened");
                } catch (Exception e) {

                }
            }
        }
        return deviceInformations;
    }

    private Integer deviceSeriesExists(String deviceNamePrefix) {
        List<DeviceInformation> deviceInformations = deviceInformationRepository.findByDeviceType(deviceNamePrefix);
        if (deviceInformations != null) return deviceInformations.size();
        else return 0;
    }

    private boolean persistDeviceModel(Map<String, Object> deviceJsonObject, List<DeviceInformation> deviceInformations) {
        boolean result = false;
        deviceJsonObject.put("azuredocdb_createdate", Instant.now().toString());
        DeviceInformation deviceInformation = new DeviceInformation();
        deviceInformation.setDeviceId(deviceJsonObject.get("deviceId").toString());
        deviceInformation.setDeviceType(deviceJsonObject.get("deviceType").toString());
        deviceInformation.setAzuredocdb_createdate(deviceJsonObject.get("azuredocdb_createdate").toString());
        deviceInformation.setAzureiot_uuid(deviceJsonObject.get("azureiot_uuid").toString());
        deviceInformation.setAzureiot_createdate(deviceJsonObject.get("azureiot_createdate").toString());
        deviceInformation.setDeviceNumber((Integer) deviceJsonObject.get("deviceNumber"));
        deviceInformation.setNonFunctional(false);
        deviceInformations.add(deviceInformation);
        deviceInformationRepository.insert(deviceInformation);
        result = true;
        return result;
    }

    private boolean azureIoTDeviceRegistration(Map<String, Object> deviceJsonObject) {
        boolean result = false;
        String deviceId = deviceJsonObject.get("deviceId").toString();
        String azureIoTDeviceUUID = deviceNameLogic(deviceId);
        deviceJsonObject.put("azureiot_createdate", Instant.now().toString());
        deviceJsonObject.put("azureiot_uuid", azureIoTDeviceUUID);
        try {
            Device device = Device.createFromId(azureIoTDeviceUUID, DeviceStatus.Enabled, null);
            device = registryManager.addDevice(device);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String deviceNameLogic(String deviceId) {
        return UUID.randomUUID().toString();
    }

    @Override
    public List<Object> showServiceBusMessages() {
        return null;
    }

    @Override
    public List<DeviceInformation> showAllDevices() {
        return deviceInformationRepository.findAll();
    }

    @Override
    public DeviceInformation showDevice(String deviceId) {
        try {
            Device data = registryManager.getDevice(deviceInformationRepository.findByDeviceId(deviceId).getAzureiot_uuid());
            System.out.println(data.getPrimaryKey());
            System.out.println(data.getDeviceId());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IotHubException e) {
            e.printStackTrace();
        }
        return deviceInformationRepository.findByDeviceId(deviceId);
    }


    @Override
    public Boolean deleteDevice(String deviceId) {
        try {
            DeviceInformation deviceInformation = showDevice(deviceId);
            registryManager.removeDeviceAsync(deviceInformation.getAzureiot_uuid());
            deviceInformation.setNonFunctional(true);
            deviceInformationRepository.save(deviceInformation);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IotHubException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public Boolean sendD2CMessageServiceHTTPS(D2CData data) {
        try {
            String deviceIdAzure = deviceInformationRepository.findByDeviceId(data.getDeviceId()).getAzureiot_uuid();
            Device device = registryManager.getDevice(deviceIdAzure);
            String connectionString = Constants.DEVICE_CONNECTION_STRING.replace("{DEVICE_ID}", deviceIdAzure).replace("{PRIMARY_KEY}", device.getPrimaryKey());
            //IOT HUB HTTPS CLIENT CREATION
            DeviceClient deviceClient = new DeviceClient(connectionString, IotHubClientProtocol.HTTPS);
            {
                MessageCallback callback = new MessageCallback();
                Counter counter = new Counter(0);
                deviceClient.setMessageCallback(callback, counter);
            }
            deviceClient.setConnectionStatusChangeCallback(new IotHubConnectionStatusChangeCallbackLogger(), new Object());
            deviceClient.open(false);
            Gson gson = new Gson();
            String jsonData = gson.toJson(data);
            Message message = new Message(jsonData);
            message.setContentType("application/json");
            message.setMessageId(UUID.randomUUID().toString());
            message.setExpiryTime(DEVICE_TO_CLOUD_MESSAGE_TIMEOUT);
            EventCallback eventCallback = new EventCallback();
            deviceClient.sendEventAsync(message, eventCallback, message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IotHubException e) {
            e.printStackTrace();
        } catch (IotHubClientException e) {
            e.printStackTrace();
        }
        try{
Thread.sleep(DEVICE_TO_CLOUD_MESSAGE_TIMEOUT);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
e.printStackTrace();
        }
        return null;
    }

    protected static class MessageCallback implements com.microsoft.azure.sdk.iot.device.MessageCallback {
        public IotHubMessageResult onCloudToDeviceMessageReceived(Message msg, Object context) {
            Counter counter = (Counter) context;
            System.out.println("Received message " + counter.toString() + " with content: " + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));
            for (MessageProperty messageProperty : msg.getProperties()) {
                System.out.println(messageProperty.getName() + " : " + messageProperty.getValue());
            }

            int switchVal = counter.get() % 3;
            IotHubMessageResult res;
            switch (switchVal) {
                case 0:
                    res = IotHubMessageResult.COMPLETE;
                    break;
                case 1:
                    res = IotHubMessageResult.ABANDON;
                    break;
                case 2:
                    res = IotHubMessageResult.REJECT;
                    break;
                default:
                    // should never happen.
                    throw new IllegalStateException("Invalid message result specified.");
            }

            System.out.println("Responding to message " + counter + " with " + res.name());

            counter.increment();

            return res;
        }
    }

    protected static class Counter {
        protected int num;

        public Counter(int num) {
            this.num = num;
        }

        public int get() {
            return this.num;
        }

        public void increment() {
            this.num++;
        }

        @Override
        public String toString() {
            return Integer.toString(this.num);
        }
    }

    protected static class IotHubConnectionStatusChangeCallbackLogger implements IotHubConnectionStatusChangeCallback {
        @Override
        public void onStatusChanged(ConnectionStatusChangeContext connectionStatusChangeContext) {
            IotHubConnectionStatus status = connectionStatusChangeContext.getNewStatus();
            IotHubConnectionStatusChangeReason statusChangeReason = connectionStatusChangeContext.getNewStatusReason();
            Throwable throwable = connectionStatusChangeContext.getCause();

            System.out.println();
            System.out.println("CONNECTION STATUS UPDATE: " + status);
            System.out.println("CONNECTION STATUS REASON: " + statusChangeReason);
            System.out.println("CONNECTION STATUS THROWABLE: " + (throwable == null ? "null" : throwable.getMessage()));
            System.out.println();

            if (throwable != null) {
                throwable.printStackTrace();
            }

            if (status == IotHubConnectionStatus.DISCONNECTED) {
                System.out.println("The connection was lost, and is not being re-established." + " Look at provided exception for how to resolve this issue." + " Cannot send messages until this issue is resolved, and you manually re-open the device client");
            } else if (status == IotHubConnectionStatus.DISCONNECTED_RETRYING) {
                System.out.println("The connection was lost, but is being re-established." + " Can still send messages, but they won't be sent until the connection is re-established");
            } else if (status == IotHubConnectionStatus.CONNECTED) {
                System.out.println("The connection was successfully established. Can send messages.");
            }
        }
    }

    protected static class EventCallback implements MessageSentCallback {
        public void onMessageSent(Message sentMessage, IotHubClientException exception, Object context) {
            Message msg = (Message) context;
            IotHubStatusCode status = exception == null ? IotHubStatusCode.OK : exception.getStatusCode();
            System.out.println("IoT Hub responded to message " + msg.getMessageId() + " with status " + status.name());
            if (status == IotHubStatusCode.MESSAGE_CANCELLED_ONCLOSE) {
                failedMessageListOnClose.add(msg.getMessageId());
            }
        }
    }
}
