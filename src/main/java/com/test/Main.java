package com.test;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.io.IOException;

/**
 * Runs a simple UPnP discovery procedure.
 */
public class Main implements Runnable {

    private LocalDevice createDevice()
            throws ValidationException, LocalServiceBindingException, IOException {

        DeviceIdentity identity =
                new DeviceIdentity(
                        UDN.uniqueSystemIdentifier("Demo Binary Light")
                );

        System.out.println(identity);

        DeviceType type =
                new UDADeviceType("BinaryLight", 1);

        DeviceDetails details =
                new DeviceDetails(
                        "Friendly Binary Light",
                        new ManufacturerDetails("ACME"),
                        new ModelDetails(
                                "BinLight2000",
                                "A demo light with on/off switch.",
                                "v1"
                        )
                );

//        Icon icon =
//                new Icon(
//                        "image/png", 48, 48, 8,
//                        getClass().getResource("icon.png")
//                );

        LocalService<SwitchPower> switchPowerService =
                new AnnotationLocalServiceBinder().read(SwitchPower.class);

        switchPowerService.setManager(
                new DefaultServiceManager(switchPowerService, SwitchPower.class)
        );

        return new LocalDevice(identity, type, details, switchPowerService);

    /* Several services can be bound to the same device:
    return new LocalDevice(
            identity, type, details, icon,
            new LocalService[] {switchPowerService, myOtherService}
    );
    */

    }


    public static void main(String[] args) throws Exception {
        // Start a user thread that runs the UPnP stack
        Thread serverThread = new Thread(new Main());
        serverThread.setDaemon(false);
        serverThread.start();
    }

    public void run() {
        try {

            RegistryListener listener = new RegistryListener() {

                public void remoteDeviceDiscoveryStarted(Registry registry,
                                                         RemoteDevice device) {
                    System.out.println(
                            "Discovery started: " + device.getDisplayString()
                    );
                }

                public void remoteDeviceDiscoveryFailed(Registry registry,
                                                        RemoteDevice device,
                                                        Exception ex) {
                    System.out.println(
                            "Discovery failed: " + device.getDisplayString() + " => " + ex
                    );
                }

                public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                    System.out.println(
                            "Remote device available: " + device.getDisplayString()
                    );
                }

                public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                    System.out.println(
                            "Remote device updated: " + device.getDisplayString()
                    );
                }

                public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                    System.out.println(
                            "Remote device removed: " + device.getDisplayString()
                    );
                }

                public void localDeviceAdded(Registry registry, LocalDevice device) {
                    System.out.println(
                            "Local device added: " + device.getDisplayString()
                    );
                }

                public void localDeviceRemoved(Registry registry, LocalDevice device) {
                    System.out.println(
                            "Local device removed: " + device.getDisplayString()
                    );
                }

                public void beforeShutdown(Registry registry) {
                    System.out.println(
                            "Before shutdown, the registry has devices: "
                                    + registry.getDevices().size()
                    );
                }

                public void afterShutdown() {
                    System.out.println("Shutdown of registry complete!");

                }
            };

            final UpnpService upnpService = new UpnpServiceImpl(listener);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                }
            });

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(
                    createDevice()
            );

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}