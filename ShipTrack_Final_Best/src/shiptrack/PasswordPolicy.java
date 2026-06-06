package shiptrack;

public class PasswordPolicy {

    public int minLength;
    public int minUppercase;
    public int minLowercase;
    public int minDigits;
    public int minSpecial;
    public int maxLoginAttempts;

    public PasswordPolicy() {
        this.minLength = 10;
        this.minUppercase = 1;
        this.minLowercase = 1;
        this.minDigits = 1;
        this.minSpecial = 1;
        this.maxLoginAttempts = 5;
    }

    public PasswordPolicy(int minLength, int minUppercase, int minLowercase,
                          int minDigits, int minSpecial, int maxLoginAttempts) {
        this.minLength = Math.max(1, minLength);
        this.minUppercase = Math.max(0, minUppercase);
        this.minLowercase = Math.max(0, minLowercase);
        this.minDigits = Math.max(0, minDigits);
        this.minSpecial = Math.max(0, minSpecial);
        this.maxLoginAttempts = Math.max(1, maxLoginAttempts);
        normalize();
    }

    public void normalize() {
        int requiredCategories = minUppercase + minLowercase + minDigits + minSpecial;
        if (minLength < requiredCategories) {
            minLength = requiredCategories;
        }
        if (maxLoginAttempts < 1) {
            maxLoginAttempts = 1;
        }
    }

    public String validate(String password) {
        if (password == null || password.length() < minLength) {
            return "Password must be at least " + minLength + " characters long.";
        }

        long upperCount = password.chars().filter(Character::isUpperCase).count();
        long lowerCount = password.chars().filter(Character::isLowerCase).count();
        long digitCount = password.chars().filter(Character::isDigit).count();
        long specialCount = password.chars().filter(c -> !Character.isLetterOrDigit(c)).count();

        if (upperCount < minUppercase) {
            return "Password must contain at least " + minUppercase + " uppercase letter(s).";
        }
        if (lowerCount < minLowercase) {
            return "Password must contain at least " + minLowercase + " lowercase letter(s).";
        }
        if (digitCount < minDigits) {
            return "Password must contain at least " + minDigits + " digit(s).";
        }
        if (specialCount < minSpecial) {
            return "Password must contain at least " + minSpecial + " special character(s).";
        }

        return null;
    }

    @Override
    public String toString() {
        return "Min length=" + minLength
                + ", Min uppercase=" + minUppercase
                + ", Min lowercase=" + minLowercase
                + ", Min digits=" + minDigits
                + ", Min special=" + minSpecial
                + ", Max login attempts=" + maxLoginAttempts;
    }
}
