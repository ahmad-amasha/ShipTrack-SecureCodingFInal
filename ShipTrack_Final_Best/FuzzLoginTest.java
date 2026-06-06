package shiptrack;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class FuzzLoginTest {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {

        String username = data.consumeString(30);
        String password = data.consumeString(30);

        Scanner sc = new Scanner(username + "\n" + password + "\n");

        Map<String, User> users = new LinkedHashMap<>();

        try {
            FileDB db = new FileDB("data/users.txt", "data/shipments.txt", "data/config.txt");
            AuthService auth = new AuthService(db);

            User loggedIn = auth.login(users, sc);

            // Since the users map is empty, no login should succeed.
            // If it returns a user, this means a login bypass happened.
            if (loggedIn != null) {
                throw new RuntimeException("Login bypass detected with fuzzed input");
            }

        } catch (IllegalArgumentException e) {
            // Acceptable invalid input case
            return;
        } catch (Exception e) {
            // Any unexpected exception should be reported to Jazzer
            throw new RuntimeException("Login function crashed with fuzzed input", e);
        }
    }
}