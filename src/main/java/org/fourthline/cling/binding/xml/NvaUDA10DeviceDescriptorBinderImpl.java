package org.fourthline.cling.binding.xml;


import static org.fourthline.cling.model.XMLUtil.appendNewElement;
import static org.fourthline.cling.model.XMLUtil.appendNewElementIfNotNull;

import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.profile.RemoteClientInfo;
import org.fourthline.cling.model.types.DLNADoc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * extends UDA10DeviceDescriptorBinderImpl not RecoveringUDA10DeviceDescriptorBinderImpl to fix cast iqy to pure java
 * client
 */
public class NvaUDA10DeviceDescriptorBinderImpl extends UDA10DeviceDescriptorBinderImpl {
    @Override
    protected void generateDevice(Namespace namespace, Device deviceModel, Document descriptor, Element rootElement, RemoteClientInfo info) {
        Element deviceElement = appendNewElement(descriptor, rootElement, Descriptor.Device.ELEMENT.device);

        appendNewElementIfNotNull(descriptor, deviceElement, Descriptor.Device.ELEMENT.deviceType, deviceModel.getType());

        DeviceDetails deviceModelDetails = deviceModel.getDetails(info);
        appendNewElementIfNotNull(
                descriptor, deviceElement, Descriptor.Device.ELEMENT.friendlyName,
                deviceModelDetails.getFriendlyName()
        );
        if (deviceModelDetails.getManufacturerDetails() != null) {
            appendNewElementIfNotNull(
                    descriptor, deviceElement, Descriptor.Device.ELEMENT.manufacturer,
                    deviceModelDetails.getManufacturerDetails().getManufacturer()
            );
            appendNewElementIfNotNull(
                    descriptor, deviceElement, Descriptor.Device.ELEMENT.manufacturerURL,
                    deviceModelDetails.getManufacturerDetails().getManufacturerURI()
            );
        }
        if (deviceModelDetails.getModelDetails() != null) {
            appendNewElementIfNotNull(
                    descriptor, deviceElement, Descriptor.Device.ELEMENT.modelDescription,
                    deviceModelDetails.getModelDetails().getModelDescription()
            );
            appendNewElementIfNotNull(
                    descriptor, deviceElement, Descriptor.Device.ELEMENT.modelName,
                    deviceModelDetails.getModelDetails().getModelName()
            );
            appendNewElementIfNotNull(
                    descriptor, deviceElement, Descriptor.Device.ELEMENT.modelNumber,
                    deviceModelDetails.getModelDetails().getModelNumber()
            );
            appendNewElementIfNotNull(
                    descriptor, deviceElement, Descriptor.Device.ELEMENT.modelURL,
                    deviceModelDetails.getModelDetails().getModelURI()
            );
        }
        appendNewElementIfNotNull(
                descriptor, deviceElement, Descriptor.Device.ELEMENT.serialNumber,
                deviceModelDetails.getSerialNumber()
        );
        appendNewElementIfNotNull(descriptor, deviceElement, Descriptor.Device.ELEMENT.UDN, deviceModel.getIdentity().getUdn());
        appendNewElementIfNotNull(
                descriptor, deviceElement, Descriptor.Device.ELEMENT.presentationURL,
                deviceModelDetails.getPresentationURI()
        );
        appendNewElementIfNotNull(
                descriptor, deviceElement, Descriptor.Device.ELEMENT.UPC,
                deviceModelDetails.getUpc()
        );

        if (deviceModelDetails.getDlnaDocs() != null) {
            for (DLNADoc dlnaDoc : deviceModelDetails.getDlnaDocs()) {
                appendNewElementIfNotNull(
                        descriptor, deviceElement, Descriptor.Device.DLNA_PREFIX + ":" + Descriptor.Device.ELEMENT.X_DLNADOC,
                        dlnaDoc, Descriptor.Device.DLNA_NAMESPACE_URI
                );
            }
        }
        appendNewElementIfNotNull(
                descriptor, deviceElement, Descriptor.Device.DLNA_PREFIX + ":" + Descriptor.Device.ELEMENT.X_DLNACAP,
                deviceModelDetails.getDlnaCaps(), Descriptor.Device.DLNA_NAMESPACE_URI
        );

        appendNewElementIfNotNull(
                descriptor, deviceElement, Descriptor.Device.SEC_PREFIX + ":" + Descriptor.Device.ELEMENT.ProductCap,
                deviceModelDetails.getSecProductCaps(), Descriptor.Device.SEC_NAMESPACE_URI
        );

        appendNewElementIfNotNull(
                descriptor, deviceElement, Descriptor.Device.SEC_PREFIX + ":" + Descriptor.Device.ELEMENT.X_ProductCap,
                deviceModelDetails.getSecProductCaps(), Descriptor.Device.SEC_NAMESPACE_URI
        );

        appendNewElement(descriptor, deviceElement, "hostVersion", "25");
        appendNewElement(descriptor, deviceElement, "ottVersion", "104600");
        appendNewElement(descriptor, deviceElement, "channelName", "master");
        appendNewElement(descriptor, deviceElement, "capability", "254");



        generateIconList(namespace, deviceModel, descriptor, deviceElement);
        generateServiceList(namespace, deviceModel, descriptor, deviceElement);
        generateDeviceList(namespace, deviceModel, descriptor, deviceElement, info);
    }
}
