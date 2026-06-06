ShipTrack Final Best Version

Folder structure:
- src/shiptrack/     Main application source files
- test/shiptrack/    JUnit 4 test files for unit testing and fuzz/security testing evidence

How to compile the application:
javac -d out src/shiptrack/*.java

How to run the application:
java -cp out shiptrack.Main

First run:
- The app asks you to create the admin password.
- Use a strong password that matches the configured policy, for example: Admin@1234

Important changes in this version:
1. Updated PBKDF2-HMAC-SHA256 iterations from 210,000 to 600,000.
2. Restricted admin remove-user action to Dispatcher and Delivery Personnel only.
3. Added Dispatcher ability to register Delivery Personnel to match the brief wording.
4. Added stronger validation for usernames, names, IDs, contact numbers, and shipment inputs.
5. Kept role-based menus for Admin, Dispatcher, Delivery Personnel, and Customer.
6. Added better JUnit tests using temporary files instead of depending on real data/users.txt.
7. Added fuzz/security-style tests for invalid usernames, weak passwords, and unsafe shipment input.
