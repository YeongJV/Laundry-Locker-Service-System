package model;

public class Locker {
	private final String id;
    private boolean available;

    public Locker(String id, boolean available) {
        this.id = id; this.available = available;
    }

    public String getId() {
    	return id;
    }
    public boolean isAvailable() {
    	return available;
    }
    public void setAvailable(boolean a) { 
    	this.available = a; 
    }
}
