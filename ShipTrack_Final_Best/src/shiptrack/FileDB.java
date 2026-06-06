package shiptrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileDB {

    private final String usersPath;
    private final String shipmentsPath;
    private final String configPath;

    public FileDB(String usersPath, String shipmentsPath, String configPath) {
        this.usersPath = usersPath;
        this.shipmentsPath = shipmentsPath;
        this.configPath = configPath;
    }

    public Map<String, User> loadUsers() {
        Map<String, User> users = new LinkedHashMap<>();
        File file = new File(usersPath);
        if (!file.exists()) {
            return users;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] p = line.split("\\|", -1);
                if (p.length < 6) {
                    continue;
                }

                String username = p[0];
                Role role = Role.valueOf(p[1]);
                String saltB64 = p[2];
                String hashB64 = p[3];
                boolean locked = Boolean.parseBoolean(p[4]);
                int failedAttempts = Integer.parseInt(p[5]);
                String fullName = p.length >= 7 ? p[6] : "";
                String idNumber = p.length >= 8 ? p[7] : "";
                String contactNo = p.length >= 9 ? p[8] : "";

                users.put(username, new User(username, role, saltB64, hashB64,
                        locked, failedAttempts, fullName, idNumber, contactNo));
            }
        } catch (Exception e) {
            System.out.println("Error reading users file: " + e.getMessage());
        }

        return users;
    }

    public void saveUsers(Map<String, User> users) {
        File file = new File(usersPath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, false))) {
            for (User u : users.values()) {
                pw.println(safe(u.username) + "|"
                        + u.role + "|"
                        + safe(u.saltBase64) + "|"
                        + safe(u.hashBase64) + "|"
                        + u.locked + "|"
                        + u.failedAttempts + "|"
                        + safe(u.fullName) + "|"
                        + safe(u.idNumber) + "|"
                        + safe(u.contactNo));
            }
        } catch (IOException e) {
            System.out.println("Error saving users file: " + e.getMessage());
        }
    }

    public Map<String, Shipment> loadShipments() {
        Map<String, Shipment> shipments = new LinkedHashMap<>();
        File file = new File(shipmentsPath);
        if (!file.exists()) {
            return shipments;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] p = line.split("\\|", -1);
                if (p.length < 7) {
                    continue;
                }

                String shipmentId = p[0];
                String customerUsername = p[1];
                String recipientName = p[2];
                String deliveryAddress = p[3];
                String packageDescription = p[4];
                DeliveryStatus status = DeliveryStatus.valueOf(p[5]);
                String assignedDriver = p[6];

                shipments.put(shipmentId, new Shipment(shipmentId, customerUsername,
                        recipientName, deliveryAddress, packageDescription, status, assignedDriver));
            }
        } catch (Exception e) {
            System.out.println("Error reading shipments file: " + e.getMessage());
        }

        return shipments;
    }

    public void saveShipments(Map<String, Shipment> shipments) {
        File file = new File(shipmentsPath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, false))) {
            for (Shipment s : shipments.values()) {
                pw.println(safe(s.shipmentId) + "|"
                        + safe(s.customerUsername) + "|"
                        + safe(s.recipientName) + "|"
                        + safe(s.deliveryAddress) + "|"
                        + safe(s.packageDescription) + "|"
                        + s.status + "|"
                        + safe(s.assignedDriver));
            }
        } catch (IOException e) {
            System.out.println("Error saving shipments file: " + e.getMessage());
        }
    }

    public PasswordPolicy loadPolicy() {
        File file = new File(configPath);
        if (!file.exists()) {
            return new PasswordPolicy();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null || line.trim().isEmpty()) {
                return new PasswordPolicy();
            }

            String[] p = line.split("\\|", -1);
            if (p.length < 6) {
                return new PasswordPolicy();
            }

            return new PasswordPolicy(
                    Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]),
                    Integer.parseInt(p[5])
            );
        } catch (Exception e) {
            System.out.println("Error reading config file: " + e.getMessage());
            return new PasswordPolicy();
        }
    }

    public void savePolicy(PasswordPolicy policy) {
        policy.normalize();
        File file = new File(configPath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, false))) {
            pw.println(policy.minLength + "|"
                    + policy.minUppercase + "|"
                    + policy.minLowercase + "|"
                    + policy.minDigits + "|"
                    + policy.minSpecial + "|"
                    + policy.maxLoginAttempts);
        } catch (IOException e) {
            System.out.println("Error saving config file: " + e.getMessage());
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }
}
