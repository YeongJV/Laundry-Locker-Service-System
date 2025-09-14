package model;

public class WashAndFoldService implements Service {
	@Override
    public double getFee() {
        return 10.0;
    }

    @Override
    public String getType() {
        return "WASH_AND_FOLD";
    }
}
