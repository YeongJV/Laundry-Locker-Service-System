package app;

import model.*;
import repository.DataStore;
import security.AdminGate;
import util.CodeGenerator;
import util.DateTimeHandler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class LockerApp {
	private final Scanner sc = new Scanner(System.in);
    private final DataStore db = new DataStore("data");
    private final AdminGate adminGate = new AdminGate("admin123"); 
    private static final double LOCKER_FEE_PER_HOUR = 2.0; // RM 2 per hour

    public void run() {
        splash();
        
        home();
        db.saveAll(); 
        System.out.println("Goodbye!");
    }

    private void splash() {
        System.out.println("\n===== Laundry Locker Service System =====");
        System.out.println("Reserve locker space for drop-off & pick-up");
    }

    private void home() {
        while (true) {
            System.out.println("\nSelect role:");
            System.out.println("1) Customer");
            System.out.println("2) Admin");
            System.out.println("3) Exit");
            String c = ask("Choose: ");
            switch (c) {
            case "1" : customerMenu(); break;
            case "2" : adminLogin(); break;
            case "3" : 
                    System.out.println("\nLogging out...");
                    System.out.println("Thank you for using the Laundry Locker System!");
                    return;
            default : System.out.println("\nInvalid choice. Please try again!");
            }
        }
    }
    
    //Customer menu
    private void customerMenu() {
        while (true) {
            System.out.println("\n----- Customer Menu -----");
            System.out.println("1) Drop-Off");
            System.out.println("2) Pay & Pick-Up");
            System.out.println("3) Back");    
            String c = ask("Choose: ");
            switch (c) {
            case "1" : dropOff(); break;
            case "2" : payAndPickup(); break;
            case "3" : return;
            default : System.out.println("\nInvalid choice. Please try again!");
            }
        }
    }
    
    private void dropOff() {
        System.out.println("\n----- Drop Off -----");
        String phone;
        do {
        	phone = ask("Phone number (0 to cancel): ");
            if (phone.equals("0")) {
                System.out.println("\nAction cancelled.");
                return; 
            }
            if (!phone.matches("\\d{8,11}")) {
                System.out.println("\nInvalid phone number (Enter 8-11 digits, e.g., 012345678) \nPlease try again!\n");
            }
        } while (!phone.matches("\\d{8,11}"));

        // show services
        Service service = chooseService();
        if (service == null) return;
        
        // find free locker
        Optional<Locker> free = db.findFirstAvailableLocker();
        if (free.isEmpty()) {
            System.out.println("\nNo lockers available now.");
            return;
        }
        Locker locker = free.get();
        
        // allocate and mark unavailable
        String code = CodeGenerator.unique6Digits(db.getActiveCodes());
        String resId = CodeGenerator.reservationId();
        Reservation r = Reservation.newPending(resId, phone, locker.getId(), code, service);
        r.setAmount(service.getFee());
        locker.setAvailable(false);
        db.saveReservationAndLocker(r, locker);

        System.out.printf("\nLocker unlocked! \nLocker ID: %s | Code: %s\n", locker.getId(), code);
        System.out.printf("[Locker ID and code already sent to phone %s via WhatsApp]\n", phone);
        
        r.setDropoffAt(LocalDateTime.now());
        db.upsertReservation(r);
    }

    private Service chooseService() {
    	while (true) {
    		System.out.println("\nService Types:");
            System.out.println("1) Wash & Fold\t (RM 10.0)");
            System.out.println("2) Dry Cleaning\t (RM 18.0)");
            System.out.println("0) Cancel");
            String s = ask("Choose: ");
            switch (s) {
            case "1": return new WashAndFoldService();
            case "2": return new DryCleaningService();
            case "0":
                System.out.println("\nAction cancelled."); 
                return null;
            default:
                System.out.println("\nInvalid choice. Please try again!");
            }
        }
    }

    private void payAndPickup() {
        System.out.println("\n----- Pay & Pick-Up -----");

        String lockerId;
        while (true) {
            lockerId = ask("\nLocker ID (L001-L020, 0 to cancel): ").toUpperCase().trim();
            if (lockerId.equals("0")) {
                System.out.println("\nAction cancelled.");
                return;
            }
            if (!isValidLockerId(lockerId)) {
                System.out.println("\nInvalid locker ID. Please enter L001-L020.");
                continue;
            }
            break; 
        }

        String code;
        while (true) {
            code = ask("6-digit code (0 to cancel): ").trim();
            if (code.equals("0")) {
                System.out.println("\nAction cancelled.");
                return;
            }
            if (!code.matches("\\d{6}")) {
                System.out.println("\nInvalid code. Must be 6 digits.");
                continue;
            }
            break; 
        }

        Optional<Reservation> or = db.findActiveByLockerAndCode(lockerId, code);
        if (or.isEmpty()) {
            System.out.println("\nInvalid locker/code or not reserved. Please try again.");
            return;
        }

        Reservation r = or.get();
        if (r.getDropoffAt() == null) {
            System.out.println("\nNo drop-off recorded yet. Please drop-off first.");
            return;
        }

        LocalDateTime pickupTime = LocalDateTime.now();
        Duration d = Duration.between(r.getDropoffAt(), pickupTime);
        long hours = DateTimeHandler.ceilHours(d);
        double lockerFee = hours * LOCKER_FEE_PER_HOUR;
        double total = r.getServiceFee() + lockerFee;

        System.out.printf("Service: %s (RM %.2f) + Locker fee: %d hour(s) × RM %.2f = RM %.2f%n",
                r.getServiceType(), r.getServiceFee(), hours, LOCKER_FEE_PER_HOUR, total);

        String pay = ask("Pay now? (y/n): ").trim();
        if (!pay.equalsIgnoreCase("y")) {
            System.out.println("\nPayment cancelled.");
            return;
        }

        r.setPickupAt(pickupTime);
        r.setAmount(total);
        r.setPaymentStatus(PaymentStatus.PAID);

        Optional<Locker> ol = db.findLocker(r.getLockerId());
        if (ol.isEmpty()) {
            System.out.println("\nLocker not found!");
            return;
        }

        Locker locker = ol.get();
        locker.setAvailable(true);
        System.out.println("\nLocker unlocked! Please collect your bag.");

        db.completeReservation(r, locker);
        System.out.println("Transaction complete. Thank you!");
    }
    
    //Validate locker ID (only L001-L020)
    private boolean isValidLockerId(String id) {
        if (!id.matches("L\\d{3}")) return false; 
        int num = Integer.parseInt(id.substring(1)); 
        return num >= 1 && num <= 20; 
    }
    
    //Admin menu
    private void adminLogin() {
        System.out.println("\n----- Admin Login -----");
        String pass = ask("Admin password: ");
        if (!adminGate.authenticate(pass)) {
        	System.out.println("\nAccess denied!"); 
        	return;
        }
        adminMenu();
    }

    private void adminMenu() {
        while (true) {
            System.out.println("\n----- Admin Menu -----");
            System.out.println("1) Unlock a Locker");
            System.out.println("2) View Locker Details");
            System.out.println("3) List Reservations");
            System.out.println("4) Remark Locker Status");
            System.out.println("5) View All Locker Status");
            System.out.println("6) Back");
           
            String c = ask("Choose: ");
            switch (c) {
            case "1" : adminUnlock(); break;
            case "2" : adminViewLockerDetails(); break;
            case "3" : listReservations(); break;
            case "4" : adminChangeStatus(); break;
            case "5" : adminViewAllLockerStatus(); break;
            case "6" : return;
            default : System.out.println("\nInvalid input. Please try again!");
            }
        }
    }

    private void adminUnlock() {
    	String id;
        while (true) {
            id = ask("\nLocker ID (L001-L020, 0 to cancel): ").toUpperCase().trim();
            if (id.equals("0")) {
                System.out.println("\nAction cancelled.");
                return;
            }
            if (!isValidLockerId(id)) {
                System.out.println("\nInvalid locker ID. Please enter L001-L020.");
                continue;
            }
            break; 
        }

        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) { 
            System.out.println("\nLocker not found."); 
            return; 
        }
        
        System.out.println("\nLocker " + id + " unlocked!");
    }

    private void adminViewLockerDetails() {
    	String id;
        while (true) {
            id = ask("\nLocker ID (L001-L020, 0 to cancel): ").toUpperCase().trim();
            if (id.equals("0")) {
                System.out.println("\nAction cancelled.");
                return;
            }
            if (!isValidLockerId(id)) {
                System.out.println("\nInvalid locker ID. Please enter L001-L020.");
                continue;
            }
            break;
        }

        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) { 
            System.out.println("\nLocker not found."); 
            return; 
        }

        Locker l = ol.get();
        Optional<Reservation> last = db.findLatestForLocker(id);
        System.out.println("\n----- Locker Details -----");
        System.out.println("Locker: " + l.getId());
        System.out.println("Availability: " + (l.isAvailable() ? "AVAILABLE" : "UNAVAILABLE"));
        if (last.isPresent()) {
            Reservation r = last.get();
            Duration dur = DateTimeHandler.safeDuration(r.getDropoffAt(), r.getPickupAt());
            long hrs = dur == null ? 0 : DateTimeHandler.ceilHours(dur);
            System.out.println("Customer Phone: " + r.getPhone());
            System.out.println("Service Type: " + r.getServiceType());
            System.out.println("Usage Duration: " + (dur == null ? "-" : hrs + " hour(s)"));
            System.out.printf("Payment: %s | Amount: RM %.2f%n", r.getPaymentStatus(), r.getAmount());
        } else {
            System.out.println("\nNo history for this locker yet.");
        }
        System.out.printf("Total Revenue (all lockers): RM %.2f%n", db.getTotalRevenue());
    }

    private void listReservations() {
    	String confirm;
        while (true) {
            confirm = ask("\nList all reservations? (y/n): ").trim().toLowerCase();
            if (confirm.equals("y") || confirm.equals("n")) {
                break; 
            } else {
                System.out.println("Invalid input. Please enter 'y' or 'n'.");
            }
        }
    	
        if (confirm.equals("n")) {
        	System.out.println("\nAction cancelled."); 
        	return;
        }
        
        System.out.println("\n----- Reservations -----");
        db.getReservations().stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .forEach(r -> System.out.printf("%-11s | %-12s | %-14s | Locker %-5s | Code %-7s | %-6s | RM %6.2f%n",
                        r.getId(), r.getPhone(), r.getServiceType(), r.getLockerId(), r.getCode(),
                        r.getPaymentStatus(), r.getAmount()));
    }

    private void adminChangeStatus() {
            System.out.println("\n----- Maintenance -----");
            System.out.println("1) REMARK Maintenance");
            System.out.println("2) REMOVE Maintenance");
            System.out.println("3) Back");    
            String c = ask("Choose: ");
            switch (c) {
            case "1" : adminMarkMaintenance(); break;
            case "2" : adminRemoveMaintenance(); break;
            case "3" : return;
            default : System.out.println("\nInvalid choice. Please try again!");
            }
    }
    	
    private void adminMarkMaintenance() {
    	String id;
        while (true) {
            id = ask("\nEnter locker ID to mark as under maintenance (L001-L020, 0 to cancel): ").toUpperCase().trim();
            if (id.equals("0")) {
                System.out.println("\nAction cancelled.");
                return;
            }
            if (!isValidLockerId(id)) {
                System.out.println("\nInvalid locker ID. Please enter L001-L020.");
                continue;
            }
            break;
        }

        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) {
            System.out.println("\nLocker not found.");
            return;
        }

        Locker l = ol.get();
        l.setUnderMaintenance(true);
        db.saveLocker(l);

        System.out.println("\nLocker " + id + " is now set to UNDER MAINTENANCE.");
    }

    private void adminRemoveMaintenance() {
    	String id;
        while (true) {
            id = ask("\nEnter locker ID to return as available (L001-L020, 0 to cancel): ").toUpperCase().trim();
            if (id.equals("0")) {
                System.out.println("\nAction cancelled.");
                return;
            }
            if (!isValidLockerId(id)) {
                System.out.println("\nInvalid locker ID. Please enter L001-L020.");
                continue;
            }
            break;
        }

        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) {
            System.out.println("\nLocker not found.");
            return;
        }

        Locker l = ol.get();
        if (!l.isUnderMaintenance()) {
            System.out.println("\nThis locker is already available.");
            return;
        }

        l.setUnderMaintenance(false);
        l.setAvailable(true);
        db.saveLocker(l);

        System.out.println("\nLocker " + id + " is now back to AVAILABLE.");
    }
    
    private void adminViewAllLockerStatus() {

        System.out.println("\n----- Locker Status -----");
        db.getLockers().values().stream()
        	.sorted(Comparator.comparingInt(l -> Integer.parseInt(l.getId().substring(1))))
        	.forEach(locker -> {
        		String status = "";
                if (locker.isUnderMaintenance()) {
                    status = "UNDER MAINTENANCE";
                } else {
                    if (locker.isAvailable()) {
                        status = "AVAILABLE";
                    } else {
                        status = "OCCUPIED";
                    }
                }
            
                System.out.println("Locker " + locker.getId() + " : " + status);
        	});           
    }
    
    
    private String ask(String msg) { 
    	System.out.print(msg); 
    	return sc.nextLine().trim(); 
    }
}
