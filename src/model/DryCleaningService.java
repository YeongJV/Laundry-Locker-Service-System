package model;

public class DryCleaningService implements Service {
	private double fee;

    public DryCleaningService(double fee) {
        this.fee = fee;
    }

    @Override
    public double getFee() { 
    	return fee; 
    }

    @Override
    public String getType() { 
    	return "DRY_CLEANING"; 
    }
}