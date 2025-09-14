package model;

public class Locker {
	private final String id;
    private boolean available;
    private boolean underMaintenance;

    public Locker(String id, boolean available) {
        this.id = id; this.available = available; this.underMaintenance = false;
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

    public boolean isUnderMaintenance(){
        return underMaintenance;
    }

    public void setUnderMaintenance(boolean underMaintenance){
        this.underMaintenance = underMaintenance;
    }
}
