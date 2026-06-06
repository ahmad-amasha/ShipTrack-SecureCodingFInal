package shiptrack;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RegisterCustomerJUnitTest {

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
    public void validInputs_customerIsSaved() {
        auth.registerCustomer(db.loadUsers(), new Scanner(
                "cust_valid\n" +
                "Valid Customer\n" +
                "123456\n" +
                "0799000000\n" +
                "Secure@Pass1\n"
        ));

        Map<String, User> users = db.loadUsers();
        assertTrue(users.containsKey("cust_valid"));
        assertEquals(Role.CUSTOMER, users.get("cust_valid").role);
    }

    @Test
    public void invalidId_customerIsNotSaved() {
        auth.registerCustomer(db.loadUsers(), new Scanner(
                "cust_badid\n" +
                "Bad ID\n" +
                "12AB56\n" +
                "0799000000\n" +
                "Secure@Pass1\n"
        ));

        assertFalse(db.loadUsers().containsKey("cust_badid"));
    }

    @Test
    public void invalidContact_customerIsNotSaved() {
        auth.registerCustomer(db.loadUsers(), new Scanner(
                "cust_badcontact\n" +
                "Bad Contact\n" +
                "123456\n" +
                "07ABC00000\n" +
                "Secure@Pass1\n"
        ));

        assertFalse(db.loadUsers().containsKey("cust_badcontact"));
    }

    @Test
    public void duplicateUsername_secondCustomerIsNotSaved() {
        auth.registerCustomer(db.loadUsers(), new Scanner(
                "cust_dup\n" +
                "First User\n" +
                "111111\n" +
                "0791111111\n" +
                "Secure@Pass1\n"
        ));
        int afterFirst = db.loadUsers().size();

        auth.registerCustomer(db.loadUsers(), new Scanner(
                "cust_dup\n" +
                "Second User\n" +
                "222222\n" +
                "0792222222\n" +
                "Secure@Pass2\n"
        ));

        assertEquals(afterFirst, db.loadUsers().size());
    }

    @Test
    public void weakPasswordIsRejectedThenStrongPasswordSucceeds() {
        auth.registerCustomer(db.loadUsers(), new Scanner(
                "cust_policy\n" +
                "Policy User\n" +
                "123456\n" +
                "0799000000\n" +
                "NoSpecial1\n" +
                "Secure@Pass1\n"
        ));

        assertTrue(db.loadUsers().containsKey("cust_policy"));
    }
}
