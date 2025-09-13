package model;

import java.time.LocalDateTime;

public class Reservation {
    private String id;
    private String phone;
    private String lockerId;
    private String code; // 6-digit
    private String serviceType; // String constant (ServiceType.*)
    private double serviceFee;

    private LocalDateTime createdAt;
    private LocalDateTime dropoffAt;
    private LocalDateTime pickupAt;

    private String paymentStatus = PaymentStatus.UNPAID; // String constants
    private double amount; // final amount

    public static Reservation newPending(String id, String phone, String lockerId, String code,
                                         String type, double serviceFee) {
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
    public String getServiceType() { return serviceType; }
    public double getServiceFee() { return serviceFee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDropoffAt() { return dropoffAt; }
    public LocalDateTime getPickupAt() { return pickupAt; }
    public String getPaymentStatus() { return paymentStatus; }
    public double getAmount() { return amount; }

    public void setDropoffAt(LocalDateTime t) { this.dropoffAt = t; }
    public void setPickupAt(LocalDateTime t) { this.pickupAt = t; }
    public void setPaymentStatus(String s)   { this.paymentStatus = s; }
    public void setAmount(double a)          { this.amount = a; }
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt; }
}
