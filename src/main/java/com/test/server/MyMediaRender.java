package com.test.server;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBindingException;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.binding.xml.DeviceDescriptorBinder;
import org.fourthline.cling.binding.xml.NvaUDA10DeviceDescriptorBinderImpl;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.DLNACaps;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.lastchange.LastChangeAwareServiceManager;
import org.fourthline.cling.support.lastchange.LastChangeParser;
import org.fourthline.cling.support.renderingcontrol.AbstractAudioRenderingControl;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.DescMeta;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.transport.impl.DatagramProcessorImpl;
import org.fourthline.cling.transport.spi.DatagramProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyMediaRender implements Runnable {

    private ServiceId cdsServiceId = ServiceId.valueOf("urn:upnp-org:serviceId:ContentDirectory");
    private Service cds;
    private CountDownLatch latch = new CountDownLatch(1);
    private UpnpService upnpService;

    private LocalService<MyAVTransportService> avService;
    private MediaPlayerController mediaPlayerController;

    private  LocalDevice createDevice()
            throws ValidationException, LocalServiceBindingException, IOException {
        DeviceIdentity identity =
                new DeviceIdentity(
                        UDN.uniqueSystemIdentifier("Demo Media Render")
                );

        System.out.println(identity);

        //it does matter to specify DeviceType to `MediaRenderer`, or some apps will fail to cast (they only search for MediaRenderer)
        DeviceType type =
                new UDADeviceType("MediaRenderer", 1);

        DeviceDetails details =
                new DeviceDetails(
                        "我的小电视",
                        new ManufacturerDetails("Bilibili Inc.", "https://bilibili.com/"),
                        new ModelDetails(
                                "MediaRender2000",
                                "云视听小电视",
                                "1024",
                                "https://app.bilibili.com/"
                        ),
                        "1024",
                        null,
                        new DLNADoc[]{new DLNADoc("DMR", DLNADoc.Version.V1_5)},
                        new DLNACaps(new String[] { "av-upload", "image-upload", "audio-upload" })
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
        avService = service;

        LocalService<ConnectionManagerService> connectionManagerService =
                new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        connectionManagerService.setManager(new DefaultServiceManager<>(connectionManagerService,
                ConnectionManagerService.class));

        LocalService<MyRenderingControlService> renderingControlLocalService =
                new AnnotationLocalServiceBinder().read(MyRenderingControlService.class);
        renderingControlLocalService.setManager(
                new LastChangeAwareServiceManager<MyRenderingControlService>(renderingControlLocalService, lastChangeParser) {
                    @Override
                    protected MyRenderingControlService createServiceInstance() throws Exception {
                        return new MyRenderingControlService();
                    }
                }
        );

        return new LocalDevice(identity, type, details, new LocalService[] {connectionManagerService, service, renderingControlLocalService});
    }


    public static void main(String[] args) throws Exception {
        System.out.println("file name" + System.getProperty("java.util.logging.config.file"));
        // Start a user thread that runs the UPnP stack
        MyMediaRender mediaRender = new MyMediaRender();
        Thread serverThread = new Thread(mediaRender);
        serverThread.setDaemon(false);
        serverThread.start();

        System.out.println("file name" + System.getProperty("java.util.logging.config.file"));
        mediaRender.loop();
    }

    private void loop() throws Exception {
        latch.await();
        System.out.println("cds found .............");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        do {
            System.out.print(">");
            String input = reader.readLine();
            System.out.println("input :" + input);
            processInput(input);
        } while (true);
    }

    private void processInput(String input) {
        String[] params = input.split(" ");
        String action = params[0];
        switch (action) {
            case "browse":
                String id = params[1];
                Action browse = cds.getAction("Browse");
                ActionInvocation invocation = new ActionInvocation(browse);
                ActionCallback browseCallback = new Browse(cds, id, BrowseFlag.DIRECT_CHILDREN) {
                    @Override
                    public void received(ActionInvocation actionInvocation, DIDLContent didl) {
                        System.out.println("container size:" + didl.getContainers().size());
                        for (Container container :didl.getContainers()) {
                            System.out.println("container title: " + container.getTitle());
                            System.out.println("container id: " + container.getId());
                            System.out.println("children count" + container.getChildCount());
                        }
                        System.out.println("item size:" + didl.getItems().size());
                        for (Item item: didl.getItems()) {
                            System.out.println("title: " + item.getTitle());
                            System.out.println("id: " + item.getId());
                            System.out.println("class: " + item.getClazz().getFriendlyName());
                            System.out.println("ref id: " + item.getRefID());
                            for (Res res: item.getResources()) {
                                System.out.println("------ duration: " + res.getDuration());
                                System.out.println("------ uri:" + res.getImportUri());
                                System.out.println("------ protocol:" + res.getProtocolInfo());
                                System.out.println("------ value: " + res.getValue());
                                System.out.println("------ bitrate: " + res.getBitrate());
                                System.out.println("------ protection: " + res.getProtection());
                                System.out.println("------ resolution: " + res.getResolution());
                            }
                            for (DescMeta meta : item.getDescMetadata()) {
                                System.out.println("******* type: " + meta.getType());
                                System.out.println("******* id: " + meta.getId());
                                System.out.println("******* data: " + meta.getMetadata());
                            }

                            for (DIDLObject.Property property:item.getProperties()) {
                                System.out.println("+++++++ name: " + property.getDescriptorName());
                                System.out.println("+++++++ value: " + property.getValue());
                            }

                        }
                    }


                    @Override
                    public void updateStatus(Status status) {
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    }
                };
                upnpService.getControlPoint().execute(browseCallback);
                break;
            case "play":
                String url = params[1];
                mediaPlayerController = new MediaPlayerController(new MediaPlayerStateChangeListener() {
                    @Override
                    public void onDurationKnow(long duration) {

                    }

                    @Override
                    public void onPlay() {

                    }

                    @Override
                    public void onPause() {

                    }

                    @Override
                    public void onStop() {

                    }
                });
                mediaPlayerController.prepare(url);
                break;
            case "end":
                System.exit(0);
        }
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
                    for (RemoteService service: device.getServices()) {
                        System.out.println(service.getServiceId());
                    }
                    if ((cds = device.findService(cdsServiceId)) != null) {
                        latch.countDown();
                    }
                }

                public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
//                    System.out.println(
//                            "Remote device updated: " + device.getDisplayString()
//                    );
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

            upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration() {
                @Override
                public int getAliveIntervalMillis() {
                    return 0;
                }

                @Override
                public DatagramProcessor getDatagramProcessor() {
                    return new MyDatagramProcessorImpl();
                }


                @Override
                public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
                    return new NvaUDA10DeviceDescriptorBinderImpl();
                }

            }, listener);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                }
            });

            LocalDevice device = createDevice();
            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(device);

            scheduleFireLastChangeEvent(Executors.newSingleThreadScheduledExecutor(), device);

            upnpService.getControlPoint().search();

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }


    private void scheduleFireLastChangeEvent(ScheduledExecutorService executorService, LocalDevice device) {
        executorService.scheduleAtFixedRate(() -> {
            LocalService[] services = device.getServices();
            for (LocalService service : services) {
                if (service.getManager() instanceof  LastChangeAwareServiceManager) {
                    LastChangeAwareServiceManager manager = (LastChangeAwareServiceManager)service.getManager();
                    manager.fireLastChange();
                }
            }
        }, 0L, 1L, TimeUnit.SECONDS);
    }
}
