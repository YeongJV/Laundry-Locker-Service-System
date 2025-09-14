package model;

public class WashAndFoldService implements Service {
	private double fee;

    public WashAndFoldService(double fee) {
        this.fee = fee;
    }
    
    @Override
    public double getFee() {
        return fee;
    }

    @Override
    public String getType() {
        return "WASH_AND_FOLD";
    }
}
