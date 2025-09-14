package model;

public abstract class User {
	protected String role;

    public User(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public abstract boolean authenticate(String input);
}
