package app;

import model.*;
import repository.DataStore;
import security.AdminGate;
import util.CodeGenerator;
import util.DateTimeHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class LockerApp {
	private final Scanner sc = new Scanner(System.in);
    private final DataStore db = new DataStore("data");
    private final AdminGate adminGate = new AdminGate("admin123"); // change if needed
    private final Map<String, Double> serviceFees = Map.of(
            ServiceType.WASH_AND_FOLD, 10.0,
            ServiceType.DRY_CLEANING, 18.0
    );
    private static final double LOCKER_FEE_PER_HOUR = 2.0; // RM 2 per started hour

    public void run() {
        splash();
        try {
            db.loadAll();
        } 
        catch (IOException e) {
            System.out.println("First run: storage will be created. (" + e.getMessage() + ")");
        }
        home();
        try { 
        	db.saveAll(); 
        } 
        catch (IOException ignored) {}
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
        	default : System.out.println("Invalid choice.");
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
            default : System.out.println("Invalid choice.");
            }
        }
    }
    
    private void dropOff() {
    	System.out.println("\n----- Drop Off -----");
        String phone;
        do {
            phone = ask("Phone number (0 to cancel): ");
            if (phone.equals("0")) {
                System.out.println("Action cancelled.");
                return; // exit the method
            }
            if (!phone.matches("\\d{8,15}")) {
                System.out.println("Invalid phone number (Enter 8-15 digits, e.g. 012345678)\n");
            }
        } while (!phone.matches("\\d{8,15}"));

        // show services
        String service = chooseServiceType();
        if (service == null) return;
        double serviceFee = serviceFees.get(service);

        // find free locker
        Optional<Locker> free = db.findFirstAvailableLocker();
        if (free.isEmpty()) {
            System.out.println("No lockers available now.");
            return;
        }
        Locker locker = free.get();
        
        // allocate and mark unavailable
        String code = CodeGenerator.unique6Digits(db.getActiveCodes());
        String resId = CodeGenerator.reservationId();
        Reservation r = Reservation.newPending(resId, phone, locker.getId(), code, service, serviceFee);
        locker.setAvailable(false);
        db.saveReservationAndLocker(r, locker);

        System.out.printf("\nLocker unlocked! \nLocker ID: %s | Code: %s\n", locker.getId(), code);
        System.out.printf("[Locker ID and code already sent to phone %s via WhatsApp]\n", phone);
        
        r.setDropoffAt(LocalDateTime.now());
        db.upsertReservation(r);
    }

    private String chooseServiceType() {
        System.out.println("Service Types:");
        System.out.println("1) Wash & Fold\t (RM " + serviceFees.get(ServiceType.WASH_AND_FOLD) + ")");
        System.out.println("2) Dry Cleaning\t (RM " + serviceFees.get(ServiceType.DRY_CLEANING) + ")");
        String s = ask("Choose: ");
        return switch (s) {
            case "1" -> ServiceType.WASH_AND_FOLD;
            case "2" -> ServiceType.DRY_CLEANING;
            default -> { System.out.println("Invalid service type. Drop off cancelled.");
            yield null; 
            }
        };
    }

    private void payAndPickup() {
    	
    	Reservation r = null;
    	Optional<Reservation> or;
    	String lockerID = null;
    	
    	do {
    	    String lockerId = ask("\nLocker ID (e.g., L001, 0 to cancel): ").toUpperCase();
    	    if (lockerId.equals("0")) {
    	        System.out.println("Action cancelled.");
    	        return; // exit the method
    	    }

    	    String code = ask("6-digit code (0 to cancel): ");
    	    if (code.equals("0")) {
    	        System.out.println("Action cancelled.");
    	        return; // exit the method
    	    }

    	    or = db.findActiveByLockerAndCode(lockerId, code);

    	    if (or.isEmpty()) {
    	        System.out.println("Invalid locker/code or not reserved. Please try again.");
    	    }
    	} while (or.isEmpty());

    	r = or.get();

        if (r.getDropoffAt() == null) {
            System.out.println("No drop-off recorded yet. Please drop-off first.");
            return;
        }

        LocalDateTime pickupTime = LocalDateTime.now();
        Duration d = Duration.between(r.getDropoffAt(), pickupTime);
        long hours = DateTimeHandler.ceilHours(d);               // per started hour
        double lockerFee = hours * LOCKER_FEE_PER_HOUR;
        double total = r.getServiceFee() + lockerFee;

        System.out.printf("Service: %s (RM %.2f) + Locker fee: %d hour(s) × RM %.2f = RM %.2f\n",
                r.getServiceType(), r.getServiceFee(), hours, LOCKER_FEE_PER_HOUR, total);

        String pay = ask("Pay now? (y/n): ");
        if (!pay.equalsIgnoreCase("y")) { 
        	System.out.println("\nPayment cancelled."); 
        	return;
        }
        
       // Simulated payment
        r.setPickupAt(pickupTime);
        r.setAmount(total);
        r.setPaymentStatus(PaymentStatus.PAID);

		// Unlock and reset locker
        Optional<Locker> ol = db.findLocker(lockerID);
        if (ol.isEmpty()) { System.out.println("Locker not found!"); return; }
        Locker locker = ol.get();
        System.out.println("\nLocker unlocked! Please collect your bag.");
        locker.setAvailable(true);

        db.completeReservation(r, locker);
        System.out.println("Transaction complete. Thank you!");
    }
    
    //Admin menu
    private void adminLogin() {
    	System.out.println("\n----- Admin Login -----");
        String pass = ask("Admin password: ");
        if (!adminGate.allow(pass)) { System.out.println("Access denied."); return; }
        adminMenu();
    }

    private void adminMenu() {
        while (true) {
        	System.out.println("\n----- Admin Menu -----");
            System.out.println("1) Unlock a Locker");    
            System.out.println("2) View Locker Details");
            System.out.println("3) List Reservations");
            System.out.println("4) Back");
            
            String c = ask("Choose: ");
            switch (c) {
            case "1" : adminUnlock(); break;
            case "2" : adminViewLockerDetails(); break;
            case "3" : listReservations(); break;
            case "4" : return;
            default : System.out.println("Invalid.");
            }
        }
    }

    private void adminUnlock() {
        String id = ask("Locker ID (e.g., L001): ").toUpperCase();
        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) { System.out.println("Locker not found."); return; }
        // Admin unlock does NOT reset to available (SRS)
        System.out.println("Locker " + id + " unlocked!");
    }

    private void adminViewLockerDetails() {
        String id = ask("Locker ID (e.g., L001): ").toUpperCase();
        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) { System.out.println("Locker not found."); return; }

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
        } 
        else {
            System.out.println("No history for this locker yet.");
        }
        System.out.printf("Total Revenue (all lockers): RM %.2f%n", db.totalRevenue());
    }

    private void listReservations() {
    	System.out.println("\n----- Reservations -----");
        db.getReservations().stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .forEach(r -> System.out.printf("%s | %s\t| %s\t| Locker %s | Code %s | %s\t| RM %.2f%n",
                        r.getId(), r.getPhone(), r.getServiceType(), r.getLockerId(), r.getCode(),
                        r.getPaymentStatus(), r.getAmount()));
    }
    
    //Helpers
    private String ask(String msg) { 
    	System.out.print(msg); 
    	return sc.nextLine().trim(); 
    }
}

