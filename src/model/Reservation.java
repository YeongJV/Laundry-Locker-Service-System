package model;

import java.time.LocalDateTime;

public class Reservation {
	private String id;
    private String phone;
    private String lockerId;
    private String code; // 6-digit
    private ServiceType serviceType;
    private double serviceFee;

    private LocalDateTime createdAt;
    private LocalDateTime dropoffAt;
    private LocalDateTime pickupAt;

    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private double amount; // final amount

    public static Reservation newPending(String id, String phone, String lockerId, String code,
                                         ServiceType type, double serviceFee) {
        Reservation r = new Reservation();
        r.id = id; r.phone = phone; r.lockerId = lockerId; r.code = code;
        r.serviceType = type; r.serviceFee = serviceFee;
        r.createdAt = LocalDateTime.now();
        return r;
    }

    public String getId() { return id; }
    public String getPhone() { return phone; }
    public String getLockerId() { return lockerId; }
    public String getCode() { return code; }
    public ServiceType getServiceType() { return serviceType; }
    public double getServiceFee() { return serviceFee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDropoffAt() { return dropoffAt; }
    public LocalDateTime getPickupAt() { return pickupAt; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public double getAmount() { return amount; }

    public void setDropoffAt(LocalDateTime t) { this.dropoffAt = t; }
    public void setPickupAt(LocalDateTime t) { this.pickupAt = t; }
    public void setPaymentStatus(PaymentStatus s) { this.paymentStatus = s; }
    public void setAmount(double a) { this.amount = a; }
}
