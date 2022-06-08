package sands.azure.iot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import sands.azure.iot.entities.D2CData;
import sands.azure.iot.entities.DeviceInformation;
import sands.azure.iot.service.DeviceService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@RestController
public class DeviceController {
    @Autowired
    private DeviceService deviceService;

    @PostMapping("/register-devices/{deviceCount}")
    public ResponseEntity registerDevices(@PathVariable Integer deviceCount, @RequestBody Map<String,String> deviceSeries){
        if(deviceSeries.size()==1){
           return ResponseEntity.status(200).body(deviceService.registerDevice(deviceSeries.get("DeviceSeries"),deviceCount));
        } else {
            return ResponseEntity.status(400).body("Please Provide Valid Body Field");
        }
    }
    @Async
    @GetMapping("/show-registered-devices")
    public ResponseEntity showRegisteredDevices(){
        return ResponseEntity.status(200).body(deviceService.showAllDevices());
    }

    @GetMapping("/show-device-by-id/{deviceId}")
    public ResponseEntity findDeviceById(@PathVariable String deviceId){
        return ResponseEntity.status(200).body(deviceService.showDevice(deviceId));
    }

    @DeleteMapping("/remove-device/{deviceId}")
    public String removeDeviceById(@PathVariable String deviceId){
        if(deviceService.deleteDevice(deviceId)){
            return "device Successfully Removed";
        }else{
            return "Undable To Remove Device Some Exception Occurred please check database and azure portal";
        }
    }
    @Async
    @PostMapping("/d2c/{deviceId}")
    public Callable<String> d2cSendMessage(@PathVariable String deviceId, @RequestBody D2CData d2CData){
        Callable<String> asyncTask = () -> {
            try {
                d2CData.setDeviceId(deviceId);
                deviceService.sendD2CMessageServiceHTTPS(d2CData);
                return "Message Sent";
            } catch (Exception e){
                return e.getLocalizedMessage();
            }
        };
        return asyncTask;
    }
}
