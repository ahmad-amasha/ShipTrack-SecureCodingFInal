package shiptrack;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LoginJUnitTest {

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

        Map<String, User> users = db.loadUsers();
        auth.ensureAdminExists(users, new Scanner("Admin@1234\n"));
    }

    @Test
    public void correctUsernameAndPassword_loginSucceeds() {
        User logged = auth.login(db.loadUsers(), new Scanner("admin\nAdmin@1234\n"));
        assertNotNull(logged);
        assertEquals("admin", logged.username);
        assertEquals(Role.ADMIN, logged.role);
    }

    @Test
    public void correctUsernameAndWrongPassword_loginFails() {
        User logged = auth.login(db.loadUsers(), new Scanner("admin\nWrong@1234\n"));
        assertNull(logged);
    }

    @Test
    public void wrongUsernameAndCorrectPassword_loginFails() {
        User logged = auth.login(db.loadUsers(), new Scanner("ghost\nAdmin@1234\n"));
        assertNull(logged);
    }

    @Test
    public void accountLocksAfterMaximumFailedAttempts() {
        PasswordPolicy policy = db.loadPolicy();
        Map<String, User> users = db.loadUsers();

        for (int i = 0; i < policy.maxLoginAttempts; i++) {
            auth.login(users, new Scanner("admin\nWrong@1234\n"));
            users = db.loadUsers();
        }

        assertTrue(db.loadUsers().get("admin").locked);
    }

    @Test
    public void lockedAccountCannotLoginWithCorrectPassword() {
        Map<String, User> users = db.loadUsers();
        User admin = users.get("admin");
        admin.locked = true;
        db.saveUsers(users);

        User logged = auth.login(db.loadUsers(), new Scanner("admin\nAdmin@1234\n"));
        assertNull(logged);
    }
}
