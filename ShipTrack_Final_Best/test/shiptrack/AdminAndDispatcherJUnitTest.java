package shiptrack;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AdminAndDispatcherJUnitTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private FileDB db;
    private AuthService auth;

    @Before
    public void setup() throws Exception {
        File data = folder.newFolder("data");
        db = new FileDB(
                new File(data, "users.txt").getPath(),
                new File(data, "shipments.txt").getPath(),
                new File(data, "config.txt").getPath()
        );
        auth = new AuthService(db);
    }

    @Test
    public void adminCanRemoveDispatcherButNotCustomer() {
        auth.registerCustomer(db.loadUsers(), new Scanner(
                "cust123\nCustomer One\n123456\n0791234567\nSecure@Pass1\n"
        ));

        auth.adminCreateDispatcher(db.loadUsers(), new Scanner(
                "disp1\nDispatcher One\n222222\n0792222222\nSecure@Pass2\n"
        ));

        auth.adminRemoveUser(db.loadUsers(), new Scanner("cust123\n"));
        assertTrue(db.loadUsers().containsKey("cust123"));

        auth.adminRemoveUser(db.loadUsers(), new Scanner("disp1\n"));
        assertFalse(db.loadUsers().containsKey("disp1"));
    }

    @Test
    public void dispatcherCanRegisterDeliveryPersonnel() {
        auth.dispatcherCreateDeliveryPersonnel(db.loadUsers(), new Scanner(
                "driver1\nDriver One\n333333\n0793333333\nSecure@Pass3\n"
        ));

        Map<String, User> users = db.loadUsers();
        assertTrue(users.containsKey("driver1"));
        assertEquals(Role.DELIVERY_PERSONNEL, users.get("driver1").role);
    }

    @Test
    public void adminCanConfigurePasswordPolicy() {
        auth.adminConfigurePasswordPolicy(new Scanner("12\n2\n1\n2\n1\n3\n"));

        PasswordPolicy policy = db.loadPolicy();
        assertEquals(12, policy.minLength);
        assertEquals(2, policy.minUppercase);
        assertEquals(1, policy.minLowercase);
        assertEquals(2, policy.minDigits);
        assertEquals(1, policy.minSpecial);
        assertEquals(3, policy.maxLoginAttempts);
    }
}
