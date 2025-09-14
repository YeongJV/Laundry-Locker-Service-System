package security;

import model.User;

public class AdminGate extends User {
	private String adminPassword;

    public AdminGate(String adminPassword) {
        super("ADMIN");
        this.adminPassword = adminPassword;
    }

    @Override
    public boolean authenticate(String input) {
        return adminPassword.equals(input);
    }
}
