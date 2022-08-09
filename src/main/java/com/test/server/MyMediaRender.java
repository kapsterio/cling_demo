package com.test.server;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
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
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.lastchange.LastChangeAwareServiceManager;
import org.fourthline.cling.support.lastchange.LastChangeParser;
import org.fourthline.cling.transport.spi.DatagramProcessor;

import java.io.IOException;

public class MyMediaRender implements Runnable {
    private  LocalDevice createDevice()
            throws ValidationException, LocalServiceBindingException, IOException {
        DeviceIdentity identity =
                new DeviceIdentity(
                        UDN.uniqueSystemIdentifier("Demo Media Render")
                );

        System.out.println(identity);

        DeviceType type =
                new UDADeviceType("MediaRender", 1);

        DeviceDetails details =
                new DeviceDetails(
                        "My First Media Render",
                        new ManufacturerDetails("ACME"),
                        new ModelDetails(
                                "MediaRender2000",
                                "A demo media render",
                                "v1"
                        )
                );


        LocalService<MyAVTransportService> service =
                new AnnotationLocalServiceBinder().read(MyAVTransportService.class);

        LastChangeParser lastChangeParser = new AVTransportLastChangeParser();

        service.setManager(
                new LastChangeAwareServiceManager<MyAVTransportService>(service, lastChangeParser) {
                    @Override
                    protected MyAVTransportService createServiceInstance() throws Exception {
                        return new MyAVTransportService();
                    }
                }
        );

        LocalService<ConnectionManagerService> connectionManagerService =
                new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        connectionManagerService.setManager(new DefaultServiceManager<>(connectionManagerService,
                ConnectionManagerService.class));

        return new LocalDevice(identity, type, details, new LocalService[] {connectionManagerService, service});
    }


    public static void main(String[] args) throws Exception {
        System.out.println("file name" + System.getProperty("java.util.logging.config.file"));
        // Start a user thread that runs the UPnP stack
        Thread serverThread = new Thread(new MyMediaRender());
        serverThread.setDaemon(false);
        serverThread.start();

        System.out.println("file name" + System.getProperty("java.util.logging.config.file"));

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

            final UpnpService upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration() {
                @Override
                public DatagramProcessor getDatagramProcessor() {
                    return new MyDatagramProcessorImpl();
                }

            }, listener);

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
