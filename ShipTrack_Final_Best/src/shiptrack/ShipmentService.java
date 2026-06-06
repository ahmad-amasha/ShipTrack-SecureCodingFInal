package shiptrack;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class ShipmentService {

    private final FileDB db;
    private final AtomicInteger idCounter;

    public ShipmentService(FileDB db, Map<String, Shipment> shipments) {
        this.db = db;

        int max = 0;
        for (String key : shipments.keySet()) {
            try {
                int n = Integer.parseInt(key.replace("SHP-", ""));
                if (n > max) {
                    max = n;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        this.idCounter = new AtomicInteger(max);
    }

    public void customerCreateShipment(User customer,
                                       Map<String, Shipment> shipments,
                                       Scanner sc) {
        System.out.println("\n=== Create Shipment Request ===");

        String recipientName = readSafeRequiredText(sc, "Recipient name: ", "Recipient name", 50);
        if (recipientName == null) {
            return;
        }

        String deliveryAddress = readSafeRequiredText(sc, "Delivery address: ", "Delivery address", 100);
        if (deliveryAddress == null) {
            return;
        }

        String packageDescription = readSafeRequiredText(sc, "Package description: ", "Package description", 100);
        if (packageDescription == null) {
            return;
        }

        String id = nextId();
        Shipment shipment = new Shipment(id, customer.username, recipientName,
                deliveryAddress, packageDescription, DeliveryStatus.PENDING, "");

        shipments.put(id, shipment);
        db.saveShipments(shipments);

        MyLogger.writeToLog("CUSTOMER_ACTION: created_shipment id=" + id
                + " customer=" + customer.username);
        System.out.println("Shipment created successfully. Your Shipment ID is: " + id);
    }

    public void customerTrackShipments(User customer, Map<String, Shipment> shipments) {
        System.out.println("\n=== My Shipments ===");

        boolean found = false;
        for (Shipment shipment : shipments.values()) {
            if (shipment.customerUsername.equals(customer.username)) {
                printShipmentRow(shipment);
                found = true;
            }
        }

        if (!found) {
            System.out.println("You have no shipments yet.");
        }
    }

    public void dispatcherViewAllShipments(Map<String, Shipment> shipments) {
        System.out.println("\n=== All Shipments ===");

        if (shipments.isEmpty()) {
            System.out.println("No shipments found.");
            return;
        }

        for (Shipment shipment : shipments.values()) {
            printShipmentRow(shipment);
        }
    }

    public void dispatcherAssignDelivery(Map<String, Shipment> shipments,
                                         Map<String, User> users,
                                         Scanner sc) {
        System.out.println("\n=== Assign Delivery to Driver ===");

        System.out.print("Enter Shipment ID: ");
        String shipmentId = sc.nextLine().trim().toUpperCase();

        Shipment shipment = shipments.get(shipmentId);
        if (shipment == null) {
            System.out.println("Shipment not found.");
            return;
        }
        if (shipment.status == DeliveryStatus.DELIVERED) {
            System.out.println("Shipment is already delivered.");
            return;
        }

        System.out.println("\nAvailable Delivery Personnel:");
        boolean foundDriver = false;
        for (User user : users.values()) {
            if (user.role == Role.DELIVERY_PERSONNEL && !user.locked) {
                System.out.println("  - " + user.username + " (" + displayName(user) + ")");
                foundDriver = true;
            }
        }

        if (!foundDriver) {
            System.out.println("No delivery personnel available.");
            return;
        }

        System.out.print("Enter driver username: ");
        String driverUsername = sc.nextLine().trim();

        User driver = users.get(driverUsername);
        if (driver == null || driver.role != Role.DELIVERY_PERSONNEL) {
            System.out.println("Invalid driver username.");
            return;
        }
        if (driver.locked) {
            System.out.println("That account is locked.");
            return;
        }

        shipment.assignedDriver = driverUsername;
        db.saveShipments(shipments);

        MyLogger.writeToLog("DISPATCHER_ACTION: assigned_delivery shipmentId=" + shipmentId
                + " driver=" + driverUsername);
        System.out.println("Delivery assigned to '" + driverUsername + "'. Current status is " + shipment.status + ".");
    }

    public void dispatcherUpdateDeliveryStatus(Map<String, Shipment> shipments, Scanner sc) {
        System.out.println("\n=== Update Delivery Status ===");

        System.out.print("Enter Shipment ID: ");
        String shipmentId = sc.nextLine().trim().toUpperCase();

        Shipment shipment = shipments.get(shipmentId);
        if (shipment == null) {
            System.out.println("Shipment not found.");
            return;
        }

        System.out.println("Current status: " + shipment.status);
        System.out.println("1) PENDING");
        System.out.println("2) IN_TRANSIT");
        System.out.println("3) DELIVERED");
        System.out.print("Choose new status: ");

        DeliveryStatus newStatus = readDispatcherStatus(sc.nextLine().trim());
        if (newStatus == null) {
            System.out.println("Invalid choice.");
            return;
        }

        shipment.status = newStatus;
        db.saveShipments(shipments);

        MyLogger.writeToLog("DISPATCHER_ACTION: updated_status shipmentId=" + shipmentId
                + " newStatus=" + newStatus);
        System.out.println("Shipment status updated to " + newStatus + ".");
    }

    public void driverViewAssignedDeliveries(User driver, Map<String, Shipment> shipments) {
        System.out.println("\n=== My Assigned Deliveries ===");

        boolean found = false;
        for (Shipment shipment : shipments.values()) {
            if (driver.username.equals(shipment.assignedDriver)) {
                printShipmentRow(shipment);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No deliveries assigned to you.");
        }
    }

    public void driverUpdateDeliveryStatus(User driver,
                                           Map<String, Shipment> shipments,
                                           Scanner sc) {
        System.out.println("\n=== Update Delivery Status ===");

        System.out.print("Enter Shipment ID: ");
        String shipmentId = sc.nextLine().trim().toUpperCase();

        Shipment shipment = shipments.get(shipmentId);
        if (shipment == null) {
            System.out.println("Shipment not found.");
            return;
        }
        if (!driver.username.equals(shipment.assignedDriver)) {
            System.out.println("This shipment is not assigned to you.");
            return;
        }

        System.out.println("Current status: " + shipment.status);
        System.out.println("1) PICKED_UP");
        System.out.println("2) IN_TRANSIT");
        System.out.println("3) DELIVERED");
        System.out.print("Choose new status: ");

        DeliveryStatus newStatus = readDriverStatus(sc.nextLine().trim());
        if (newStatus == null) {
            System.out.println("Invalid choice.");
            return;
        }

        shipment.status = newStatus;
        db.saveShipments(shipments);

        MyLogger.writeToLog("DRIVER_ACTION: updated_status shipmentId=" + shipmentId
                + " driver=" + driver.username + " newStatus=" + newStatus);
        System.out.println("Status updated to " + newStatus + ".");
    }

    private String nextId() {
        return String.format("SHP-%04d", idCounter.incrementAndGet());
    }

    private DeliveryStatus readDispatcherStatus(String choice) {
        switch (choice) {
            case "1":
                return DeliveryStatus.PENDING;
            case "2":
                return DeliveryStatus.IN_TRANSIT;
            case "3":
                return DeliveryStatus.DELIVERED;
            default:
                return null;
        }
    }

    private DeliveryStatus readDriverStatus(String choice) {
        switch (choice) {
            case "1":
                return DeliveryStatus.PICKED_UP;
            case "2":
                return DeliveryStatus.IN_TRANSIT;
            case "3":
                return DeliveryStatus.DELIVERED;
            default:
                return null;
        }
    }

    private String readSafeRequiredText(Scanner sc, String prompt, String fieldName, int maxLength) {
        System.out.print(prompt);
        String value = sc.nextLine().trim();
        if (value.isEmpty() || value.length() > maxLength || value.contains("|")
                || value.contains("\n") || value.contains("\r")) {
            System.out.println(fieldName + " is required, must not contain |, and must be at most " + maxLength + " characters.");
            return null;
        }
        return value;
    }

    private void printShipmentRow(Shipment shipment) {
        System.out.printf("  [%s] Customer=%-15s | To=%-20s | Status=%-12s | Driver=%s%n",
                shipment.shipmentId,
                shipment.customerUsername,
                shipment.recipientName + " @ " + shipment.deliveryAddress,
                shipment.status,
                shipment.assignedDriver == null || shipment.assignedDriver.isEmpty() ? "(unassigned)" : shipment.assignedDriver);
    }

    private String displayName(User user) {
        return user.fullName == null || user.fullName.isEmpty() ? user.username : user.fullName;
    }
}
