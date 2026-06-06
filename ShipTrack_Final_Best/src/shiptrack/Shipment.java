package shiptrack;

public class Shipment {

    public String shipmentId;
    public String customerUsername;
    public String recipientName;
    public String deliveryAddress;
    public String packageDescription;
    public DeliveryStatus status;
    public String assignedDriver;

    public Shipment(String shipmentId,
                    String customerUsername,
                    String recipientName,
                    String deliveryAddress,
                    String packageDescription,
                    DeliveryStatus status,
                    String assignedDriver) {
        this.shipmentId = shipmentId;
        this.customerUsername = customerUsername;
        this.recipientName = recipientName;
        this.deliveryAddress = deliveryAddress;
        this.packageDescription = packageDescription;
        this.status = status;
        this.assignedDriver = assignedDriver;
    }
}
