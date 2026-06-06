package shiptrack;

import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String usersPath = "data/users.txt";
        String shipmentsPath = "data/shipments.txt";
        String configPath = "data/config.txt";

        FileDB db = new FileDB(usersPath, shipmentsPath, configPath);
        AuthService auth = new AuthService(db);

        Map<String, User> users = db.loadUsers();
        Map<String, Shipment> shipments = db.loadShipments();
        ShipmentService shipSvc = new ShipmentService(db, shipments);

        Scanner sc = new Scanner(System.in);
        MyLogger.writeToLog("SYSTEM: ShipTrack application started");

        auth.ensureAdminExists(users, sc);

        while (true) {
            System.out.println("\n==============================");
            System.out.println("   ShipTrack - Delivery System");
            System.out.println("==============================");
            System.out.println("1) Register (Customer)");
            System.out.println("2) Login");
            System.out.println("3) Exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            if (choice.equals("1")) {
                users = db.loadUsers();
                auth.registerCustomer(users, sc);
            } else if (choice.equals("2")) {
                users = db.loadUsers();
                shipments = db.loadShipments();
                auth.reloadPolicy();

                User loggedIn = auth.login(users, sc);
                if (loggedIn != null) {
                    runRoleMenu(loggedIn, auth, shipSvc, users, shipments, db, sc);
                }
            } else if (choice.equals("3")) {
                MyLogger.writeToLog("SYSTEM: ShipTrack application closed");
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("Invalid option. Please choose 1, 2, or 3.");
            }
        }

        sc.close();
    }

    private static void runRoleMenu(User user,
                                    AuthService auth,
                                    ShipmentService shipSvc,
                                    Map<String, User> users,
                                    Map<String, Shipment> shipments,
                                    FileDB db,
                                    Scanner sc) {
        switch (user.role) {
            case ADMIN:
                adminMenu(user, auth, users, db, sc);
                break;
            case DISPATCHER:
                dispatcherMenu(user, auth, shipSvc, users, shipments, db, sc);
                break;
            case DELIVERY_PERSONNEL:
                driverMenu(user, auth, shipSvc, users, shipments, db, sc);
                break;
            case CUSTOMER:
                customerMenu(user, auth, shipSvc, users, shipments, db, sc);
                break;
            default:
                System.out.println("Unknown role. Logging out.");
        }
    }

    private static void adminMenu(User user,
                                  AuthService auth,
                                  Map<String, User> users,
                                  FileDB db,
                                  Scanner sc) {
        while (true) {
            System.out.println("\n--- ADMIN Menu ---");
            System.out.println("1)  Register New Dispatcher");
            System.out.println("2)  Register New Delivery Personnel");
            System.out.println("3)  Remove User (Dispatcher or Delivery Personnel)");
            System.out.println("4)  Lock User Account");
            System.out.println("5)  Unlock User Account");
            System.out.println("6)  List All Users");
            System.out.println("7)  View Password Policy");
            System.out.println("8)  Configure Password Policy");
            System.out.println("9)  Logout");
            System.out.print("Choose: ");

            String c = sc.nextLine().trim();
            users = db.loadUsers();

            switch (c) {
                case "1":
                    auth.adminCreateDispatcher(users, sc);
                    break;
                case "2":
                    auth.adminCreateDeliveryPersonnel(users, sc);
                    break;
                case "3":
                    auth.adminRemoveUser(users, sc);
                    break;
                case "4":
                    auth.adminLockUser(users, sc);
                    break;
                case "5":
                    auth.adminUnlockUser(users, sc);
                    break;
                case "6":
                    auth.adminListUsers(users);
                    break;
                case "7":
                    auth.adminViewPasswordPolicy();
                    break;
                case "8":
                    auth.adminConfigurePasswordPolicy(sc);
                    break;
                case "9":
                    MyLogger.writeToLog("LOGOUT: username=" + user.username);
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void dispatcherMenu(User user,
                                       AuthService auth,
                                       ShipmentService shipSvc,
                                       Map<String, User> users,
                                       Map<String, Shipment> shipments,
                                       FileDB db,
                                       Scanner sc) {
        while (true) {
            System.out.println("\n--- DISPATCHER Menu ---");
            System.out.println("1) View All Shipments");
            System.out.println("2) Assign Delivery to Driver");
            System.out.println("3) Update Delivery Status");
            System.out.println("4) Register New Delivery Personnel");
            System.out.println("5) View My Profile");
            System.out.println("6) Update My Profile");
            System.out.println("7) Logout");
            System.out.print("Choose: ");

            String c = sc.nextLine().trim();
            users = db.loadUsers();

            switch (c) {
                case "1":
                    shipSvc.dispatcherViewAllShipments(shipments);
                    break;
                case "2":
                    shipSvc.dispatcherAssignDelivery(shipments, users, sc);
                    break;
                case "3":
                    shipSvc.dispatcherUpdateDeliveryStatus(shipments, sc);
                    break;
                case "4":
                    auth.dispatcherCreateDeliveryPersonnel(users, sc);
                    break;
                case "5":
                    auth.dispatcherViewProfile(user);
                    break;
                case "6":
                    auth.dispatcherUpdateProfile(user, users, sc);
                    break;
                case "7":
                    MyLogger.writeToLog("LOGOUT: username=" + user.username);
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void driverMenu(User user,
                                   AuthService auth,
                                   ShipmentService shipSvc,
                                   Map<String, User> users,
                                   Map<String, Shipment> shipments,
                                   FileDB db,
                                   Scanner sc) {
        while (true) {
            System.out.println("\n--- DELIVERY PERSONNEL Menu ---");
            System.out.println("1) View My Assigned Deliveries");
            System.out.println("2) Update Delivery Status");
            System.out.println("3) View My Profile");
            System.out.println("4) Update My Profile");
            System.out.println("5) Logout");
            System.out.print("Choose: ");

            String c = sc.nextLine().trim();
            users = db.loadUsers();

            switch (c) {
                case "1":
                    shipSvc.driverViewAssignedDeliveries(user, shipments);
                    break;
                case "2":
                    shipSvc.driverUpdateDeliveryStatus(user, shipments, sc);
                    break;
                case "3":
                    auth.driverViewProfile(user);
                    break;
                case "4":
                    auth.driverUpdateProfile(user, users, sc);
                    break;
                case "5":
                    MyLogger.writeToLog("LOGOUT: username=" + user.username);
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void customerMenu(User user,
                                     AuthService auth,
                                     ShipmentService shipSvc,
                                     Map<String, User> users,
                                     Map<String, Shipment> shipments,
                                     FileDB db,
                                     Scanner sc) {
        while (true) {
            System.out.println("\n--- CUSTOMER Menu ---");
            System.out.println("1) Create Shipment Request");
            System.out.println("2) Track My Packages");
            System.out.println("3) View My Profile");
            System.out.println("4) Update My Profile");
            System.out.println("5) Logout");
            System.out.print("Choose: ");

            String c = sc.nextLine().trim();
            users = db.loadUsers();

            switch (c) {
                case "1":
                    shipSvc.customerCreateShipment(user, shipments, sc);
                    break;
                case "2":
                    shipSvc.customerTrackShipments(user, shipments);
                    break;
                case "3":
                    auth.customerViewProfile(user);
                    break;
                case "4":
                    auth.customerUpdateProfile(user, users, sc);
                    break;
                case "5":
                    MyLogger.writeToLog("LOGOUT: username=" + user.username);
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
