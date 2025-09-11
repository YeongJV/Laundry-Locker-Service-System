package security;

public class AdminGate {
	private final String adminPassword;
    public AdminGate(String adminPassword) { 
    	this.adminPassword = adminPassword; 
    }
    public boolean allow(String input) { 
    	return adminPassword.equals(input); 
    }
}
