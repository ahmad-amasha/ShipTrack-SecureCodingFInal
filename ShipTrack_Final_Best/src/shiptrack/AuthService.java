package shiptrack;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;

public class AuthService {

    private final FileDB db;
    private PasswordPolicy policy;

    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;

    public AuthService(FileDB db) {
        this.db = db;
        this.policy = db.loadPolicy();
    }

    public void reloadPolicy() {
        this.policy = db.loadPolicy();
    }

    public PasswordPolicy getPolicy() {
        return policy;
    }

    public void ensureAdminExists(Map<String, User> users, Scanner sc) {
        boolean adminExists = users.values().stream().anyMatch(u -> u.role == Role.ADMIN);
        if (adminExists) {
            return;
        }

        System.out.println("\n=== First Run Setup ===");
        System.out.println("No ADMIN account found. Please create the admin password now.");

        String password = promptForStrongPassword(sc);
        createUser(users, "admin", Role.ADMIN, password, "System Administrator", "", "");
        db.saveUsers(users);

        MyLogger.writeToLog("SYSTEM: first-run admin account created username=admin");
        System.out.println("Admin account created. Username: admin");
    }

    public User login(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Login ===");

        System.out.print("Username: ");
        String username = sc.nextLine().trim();

        System.out.print("Password: ");
        String password = sc.nextLine();

        User user = users.get(username);

        byte[] dummySalt = new byte[SALT_BYTES];
        byte[] saltBytes = user != null ? fromBase64(user.saltBase64) : dummySalt;
        String computedHash = hashPasswordToBase64(password.toCharArray(), saltBytes);

        if (user == null) {
            MyLogger.writeToWarning("LOGIN_FAIL: invalid_credentials username=" + username);
            System.out.println("Invalid credentials.");
            return null;
        }

        if (user.locked) {
            MyLogger.writeToWarning("LOGIN_BLOCKED: locked_account username=" + username);
            System.out.println("Account is locked. Please contact the system administrator.");
            return null;
        }

        if (constantTimeEqualsBase64(computedHash, user.hashBase64)) {
            user.failedAttempts = 0;
            db.saveUsers(users);
            MyLogger.writeToLog("LOGIN_OK: username=" + username + " role=" + user.role);
            System.out.println("Login successful. Welcome, " + displayName(user) + " (" + user.role + ")!");
            return user;
        }

        user.failedAttempts++;
        MyLogger.writeToWarning("LOGIN_FAIL: invalid_credentials username=" + username
                + " attempts=" + user.failedAttempts);

        if (user.failedAttempts >= policy.maxLoginAttempts) {
            user.locked = true;
            MyLogger.writeToWarning("ACCOUNT_LOCKED: username=" + username);
            System.out.println("Too many failed attempts. Account has been locked.");
        } else {
            int remaining = policy.maxLoginAttempts - user.failedAttempts;
            System.out.println("Invalid credentials. Attempts remaining: " + remaining);
        }

        db.saveUsers(users);
        return null;
    }

    public void registerCustomer(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Customer Registration ===");

        String username = readUsername(sc, "Choose a username: ", users);
        if (username == null) {
            return;
        }

        String fullName = readRequiredText(sc, "Full name: ", "Full name", 50);
        if (fullName == null) {
            return;
        }

        String idNumber = readNumericText(sc, "ID number: ", "ID number", 3, 20);
        if (idNumber == null) {
            return;
        }

        String contactNo = readNumericText(sc, "Contact number: ", "Contact number", 7, 20);
        if (contactNo == null) {
            return;
        }

        String password = promptForStrongPassword(sc);

        createUser(users, username, Role.CUSTOMER, password, fullName, idNumber, contactNo);
        db.saveUsers(users);

        MyLogger.writeToLog("REGISTER: new_customer username=" + username);
        System.out.println("Registration successful. You can now log in.");
    }

    public void adminCreateDispatcher(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Admin: Register New Dispatcher ===");
        createStaffAccount(users, sc, Role.DISPATCHER, "Dispatcher", "ADMIN_ACTION");
    }

    public void adminCreateDeliveryPersonnel(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Admin: Register New Delivery Personnel ===");
        createStaffAccount(users, sc, Role.DELIVERY_PERSONNEL, "Delivery Personnel", "ADMIN_ACTION");
    }

    public void dispatcherCreateDeliveryPersonnel(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Dispatcher: Register New Delivery Personnel ===");
        createStaffAccount(users, sc, Role.DELIVERY_PERSONNEL, "Delivery Personnel", "DISPATCHER_ACTION");
    }

    public void adminRemoveUser(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Admin: Remove User ===");

        adminListUsers(users);

        System.out.print("Enter username to remove: ");
        String username = sc.nextLine().trim();

        User user = users.get(username);
        if (user == null) {
            System.out.println("User not found.");
            return;
        }

        if (user.role != Role.DISPATCHER && user.role != Role.DELIVERY_PERSONNEL) {
            System.out.println("Only dispatcher or delivery personnel accounts can be removed by this option.");
            return;
        }

        users.remove(username);
        db.saveUsers(users);

        MyLogger.writeToLog("ADMIN_ACTION: removed_user username=" + username + " role=" + user.role);
        System.out.println("User '" + username + "' removed successfully.");
    }

    public void adminLockUser(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Admin: Lock User Account ===");

        System.out.print("Enter username to lock: ");
        String username = sc.nextLine().trim();

        User user = users.get(username);
        if (user == null) {
            System.out.println("User not found.");
            return;
        }
        if (user.role == Role.ADMIN) {
            System.out.println("The admin account cannot be locked.");
            return;
        }
        if (user.locked) {
            System.out.println("Account is already locked.");
            return;
        }

        user.locked = true;
        user.failedAttempts = 0;
        db.saveUsers(users);

        MyLogger.writeToLog("ADMIN_ACTION: locked_account username=" + username);
        System.out.println("Account '" + username + "' locked.");
    }

    public void adminUnlockUser(Map<String, User> users, Scanner sc) {
        System.out.println("\n=== Admin: Unlock User Account ===");

        System.out.print("Enter username to unlock: ");
        String username = sc.nextLine().trim();

        User user = users.get(username);
        if (user == null) {
            System.out.println("User not found.");
            return;
        }
        if (!user.locked) {
            System.out.println("Account is not locked.");
            return;
        }

        user.locked = false;
        user.failedAttempts = 0;
        db.saveUsers(users);

        MyLogger.writeToLog("ADMIN_ACTION: unlocked_account username=" + username);
        System.out.println("Account '" + username + "' unlocked.");
    }

    public void adminListUsers(Map<String, User> users) {
        System.out.println("\n=== All Users ===");
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        for (User user : users.values()) {
            System.out.printf("  %-20s | %-20s | locked=%-5s | name=%s%n",
                    user.username, user.role, user.locked, displayName(user));
        }
    }

    public void adminConfigurePasswordPolicy(Scanner sc) {
        System.out.println("\n=== Admin: Configure Password Policy ===");
        System.out.println("Current policy: " + policy);
        System.out.println("Enter new values. Press Enter to keep the current value.");

        policy.minLength = readNonNegativeInt(sc,
                "Minimum password length       (" + policy.minLength + "): ", policy.minLength, 1);
        policy.minUppercase = readNonNegativeInt(sc,
                "Minimum uppercase letters     (" + policy.minUppercase + "): ", policy.minUppercase, 0);
        policy.minLowercase = readNonNegativeInt(sc,
                "Minimum lowercase letters     (" + policy.minLowercase + "): ", policy.minLowercase, 0);
        policy.minDigits = readNonNegativeInt(sc,
                "Minimum digits                (" + policy.minDigits + "): ", policy.minDigits, 0);
        policy.minSpecial = readNonNegativeInt(sc,
                "Minimum special characters    (" + policy.minSpecial + "): ", policy.minSpecial, 0);
        policy.maxLoginAttempts = readNonNegativeInt(sc,
                "Maximum failed login attempts (" + policy.maxLoginAttempts + "): ", policy.maxLoginAttempts, 1);

        policy.normalize();
        db.savePolicy(policy);

        MyLogger.writeToLog("ADMIN_ACTION: updated_password_policy " + policy);
        System.out.println("Password policy updated successfully.");
        System.out.println("New policy: " + policy);
    }

    public void adminViewPasswordPolicy() {
        System.out.println("\n=== Current Password Policy ===");
        System.out.println(policy);
    }

    public void dispatcherViewProfile(User user) {
        printProfile(user);
    }

    public void dispatcherUpdateProfile(User user, Map<String, User> users, Scanner sc) {
        updateCommonProfile(user, users, sc, "DISPATCHER_ACTION");
    }

    public void customerViewProfile(User user) {
        printProfile(user);
    }

    public void customerUpdateProfile(User user, Map<String, User> users, Scanner sc) {
        updateCommonProfile(user, users, sc, "CUSTOMER_ACTION");
    }

    public void driverViewProfile(User user) {
        printProfile(user);
    }

    public void driverUpdateProfile(User user, Map<String, User> users, Scanner sc) {
        updateCommonProfile(user, users, sc, "DRIVER_ACTION");
    }

    private void createStaffAccount(Map<String, User> users, Scanner sc,
                                    Role role, String roleLabel, String logPrefix) {
        String username = readUsername(sc, "Choose username: ", users);
        if (username == null) {
            return;
        }

        String fullName = readRequiredText(sc, "Full name: ", "Full name", 50);
        if (fullName == null) {
            return;
        }

        String idNumber = readNumericText(sc, "ID number: ", "ID number", 3, 20);
        if (idNumber == null) {
            return;
        }

        String contactNo = readNumericText(sc, "Contact number: ", "Contact number", 7, 20);
        if (contactNo == null) {
            return;
        }

        String password = promptForStrongPassword(sc);

        createUser(users, username, role, password, fullName, idNumber, contactNo);
        db.saveUsers(users);

        MyLogger.writeToLog(logPrefix + ": registered_" + role + " username=" + username);
        System.out.println(roleLabel + " account created successfully.");
    }

    private void updateCommonProfile(User user, Map<String, User> users, Scanner sc, String logPrefix) {
        System.out.println("\n=== Update My Profile ===");
        System.out.println("Leave field empty to keep current value.");

        System.out.print("Full name (current: " + displayName(user) + "): ");
        String fullName = sc.nextLine().trim();
        if (!fullName.isEmpty()) {
            if (!isSafeText(fullName, 50)) {
                System.out.println("Full name contains invalid characters or is too long. Update cancelled.");
                return;
            }
            user.fullName = fullName;
        }

        System.out.print("Contact number (current: " + user.contactNo + "): ");
        String contactNo = sc.nextLine().trim();
        if (!contactNo.isEmpty()) {
            if (!contactNo.matches("\\d{7,20}")) {
                System.out.println("Contact number must contain digits only and be 7 to 20 digits long. Update cancelled.");
                return;
            }
            user.contactNo = contactNo;
        }

        users.put(user.username, user);
        db.saveUsers(users);

        MyLogger.writeToLog(logPrefix + ": updated_profile username=" + user.username);
        System.out.println("Profile updated successfully.");
    }

    private void printProfile(User user) {
        System.out.println("\n=== My Profile ===");
        System.out.println("Username : " + user.username);
        System.out.println("Role     : " + user.role);
        System.out.println("Name     : " + displayName(user));
        System.out.println("ID No.   : " + user.idNumber);
        System.out.println("Contact  : " + user.contactNo);
    }

    public String promptForStrongPassword(Scanner sc) {
        while (true) {
            System.out.print("Choose password (" + policy + "): ");
            String password = sc.nextLine();
            String error = policy.validate(password);
            if (error == null) {
                return password;
            }
            System.out.println("Weak password: " + error);
        }
    }

    private String readUsername(Scanner sc, String prompt, Map<String, User> users) {
        System.out.print(prompt);
        String username = sc.nextLine().trim();

        if (!username.matches("[A-Za-z0-9_]{3,20}")) {
            System.out.println("Username must be 3 to 20 characters and contain only letters, numbers, and underscore.");
            return null;
        }
        if (users.containsKey(username)) {
            System.out.println("Username already exists. Please choose another.");
            return null;
        }
        return username;
    }

    private String readRequiredText(Scanner sc, String prompt, String fieldName, int maxLength) {
        System.out.print(prompt);
        String value = sc.nextLine().trim();
        if (!isSafeText(value, maxLength)) {
            System.out.println(fieldName + " is required, must not contain |, and must be at most " + maxLength + " characters.");
            return null;
        }
        return value;
    }

    private String readNumericText(Scanner sc, String prompt, String fieldName, int minLength, int maxLength) {
        System.out.print(prompt);
        String value = sc.nextLine().trim();
        if (!value.matches("\\d{" + minLength + "," + maxLength + "}")) {
            System.out.println(fieldName + " must contain digits only and be " + minLength + " to " + maxLength + " digits long.");
            return null;
        }
        return value;
    }

    private boolean isSafeText(String value, int maxLength) {
        return value != null
                && !value.isEmpty()
                && value.length() <= maxLength
                && !value.contains("|")
                && !value.contains("\n")
                && !value.contains("\r");
    }

    private int readNonNegativeInt(Scanner sc, String prompt, int defaultValue, int minimumValue) {
        System.out.print(prompt);
        String input = sc.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }

        try {
            int value = Integer.parseInt(input);
            if (value < minimumValue) {
                System.out.println("Value must be at least " + minimumValue + ". Keeping current value.");
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Keeping current value.");
            return defaultValue;
        }
    }

    private void createUser(Map<String, User> users,
                            String username, Role role, String password,
                            String fullName, String idNumber, String contactNo) {
        byte[] salt = generateSalt(SALT_BYTES);
        String saltB64 = toBase64(salt);
        String hashB64 = hashPasswordToBase64(password.toCharArray(), salt);
        users.put(username, new User(username, role, saltB64, hashB64,
                false, 0, fullName, idNumber, contactNo));
    }

    private byte[] generateSalt(int bytes) {
        byte[] salt = new byte[bytes];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    String hashPasswordToBase64(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = secretKeyFactory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            Arrays.fill(password, '\0');
            return toBase64(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed: " + e.getMessage(), e);
        }
    }

    private String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private byte[] fromBase64(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    private boolean constantTimeEqualsBase64(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        try {
            return MessageDigest.isEqual(fromBase64(a), fromBase64(b));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String displayName(User user) {
        return user.fullName == null || user.fullName.isEmpty() ? user.username : user.fullName;
    }
}
