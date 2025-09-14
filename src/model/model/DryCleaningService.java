package model;

public class DryCleaningService implements Service {
	@Override
    public double getFee() {
        return 18.0;
    }

    @Override
    public String getType() {
        return "DRY_CLEANING";
    }
}
