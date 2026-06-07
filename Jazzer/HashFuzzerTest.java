package shiptrack;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class HashFuzzerTest {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {

        String passwordString = data.consumeString(80);
        char[] password = passwordString.toCharArray();

        int saltLength = data.consumeInt(0, 64);
        byte[] salt = data.consumeBytes(saltLength);

        // Empty salt is not useful for this fuzz test, so skip it.
        if (salt.length == 0) {
            return;
        }

        try {
            FileDB db = new FileDB("data/users.txt", "data/shipments.txt", "data/config.txt");
            AuthService auth = new AuthService(db);

            String result = auth.hashPasswordToBase64(password, salt);

            // If hashing succeeds, the result should be a non-empty Base64 string.
            if (result == null || result.isEmpty()) {
                throw new RuntimeException("Hash result was null or empty");
            }

        } catch (IllegalArgumentException e) {
            // Acceptable invalid input case
            return;
        } catch (Exception e) {
            // Any unexpected exception should be reported to Jazzer
            throw new RuntimeException("Hash function crashed with fuzzed input", e);
        }
    }
}