package repository;

import model.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class DataStore {
	private final String folder;
    private final File lockersFile;
    private final File reservationsFile;

    private final Map<String, Locker> lockers = new TreeMap<>(Comparator.comparingInt(
    	    id -> Integer.parseInt(id.substring(1))  
    ));
    private final Map<String, Reservation> reservations = new HashMap<>();
    private double totalRevenue = 0.0;
    
    public String getFolderPath() {
        return folder;
    }
    
    public double getTotalRevenue() { 
    	return totalRevenue; 
    } 
    
    public void addRevenue(double amount) { 
    	totalRevenue += amount; 
    }

    public DataStore(String folder) {
    	this.folder = folder;
        this.lockersFile = new File(folder, "lockers.txt");
        this.reservationsFile = new File(folder, "reservations.txt");
        
        new File(folder).mkdirs(); 
        try {
            loadLockers();
            //Initialize sample lockers
            if (lockers.isEmpty()) {
                for (int i = 1; i <= 20; i++) {
                    String id = "L" + String.format("%03d", i);
                    lockers.put(id, new Locker(id, true));
                }
                saveLockers();
            }
            loadReservations();
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
        }
    }

    //Lockers
    private void loadLockers() throws IOException {
        if (!lockersFile.exists()) return;

        Scanner input = new Scanner(new FileReader(lockersFile));
        String id = null;
        Boolean available = null;
        Boolean underMaintenance = null;

        while (input.hasNextLine()) {
            String line = input.nextLine().trim();
            if (line.isEmpty()) {
                if (id != null) {
                	Locker locker = new Locker(id, available != null && available);
                    if (underMaintenance != null) {
                        locker.setUnderMaintenance(underMaintenance);
                    }
                    lockers.put(id, locker);
                    id = null;
                    available = null;
                    underMaintenance = null;
                }
                continue;
            }

            if (line.startsWith("Locker:")) {
                id = line.substring("Locker:".length()).trim();
            } else if (line.startsWith("Available:")) {
                available = Boolean.parseBoolean(line.substring("Available:".length()).trim());
            } else if (line.startsWith("UnderMaintenance:")) {
                underMaintenance = Boolean.parseBoolean(line.substring("UnderMaintenance:".length()).trim());
            }
        }

        if (id != null) {
        	Locker locker = new Locker(id, available != null && available);
            if (underMaintenance != null) {
                locker.setUnderMaintenance(underMaintenance);
            }
            lockers.put(id, locker);
        }

        input.close();
    }

    private void saveLockers() throws IOException {
    	try (PrintWriter out = new PrintWriter(new FileWriter(lockersFile))){
    		for (Locker l : lockers.values()) {
                out.println("Locker: " + l.getId());
                out.println("Available: " + l.isAvailable());
                out.println("UnderMaintenance: " + l.isUnderMaintenance());
                out.println();
            }
            out.close();
    	}
    }
    
    //Reservations
    private void loadReservations() throws IOException {
        if (!reservationsFile.exists()) return;

        Scanner input = new Scanner(new FileReader(reservationsFile));
        Map<String, String> fields = new HashMap<>();

        while (input.hasNextLine()) {
            String line = input.nextLine().trim();
            
            if (line.startsWith("TOTAL_REVENUE:")) {
                totalRevenue = Double.parseDouble(line.substring("TOTAL_REVENUE:".length()).trim());
                continue;
            }
            
            if (line.equals("---")) {
                createReservationFromFields(fields);
                fields.clear();
            } else if (!line.isEmpty()) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    fields.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        if (!fields.isEmpty()) {
            createReservationFromFields(fields);
        }

        input.close();
    }

    private void saveReservations() throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(reservationsFile));
        for (Reservation r : reservations.values()) {
            out.println("ID: " + r.getId());
            out.println("Phone: " + r.getPhone());
            out.println("Locker: " + r.getLockerId());
            out.println("Code: " + r.getCode());
            out.println("Service: " + r.getServiceType());
            out.println("Fee: " + r.getServiceFee());
            out.println("CreatedAt: " + (r.getCreatedAt() == null ? "" : r.getCreatedAt()));
            out.println("DropoffAt: " + (r.getDropoffAt() == null ? "" : r.getDropoffAt()));
            out.println("PickupAt: " + (r.getPickupAt() == null ? "" : r.getPickupAt()));
            out.println("Payment: " + r.getPaymentStatus());
            out.println("Amount: " + String.format("%.2f", r.getAmount()));
            out.println("---");
        }
        out.println("TOTAL_REVENUE: " + String.format("%.2f", totalRevenue));
        out.close();
    }

    private void createReservationFromFields(Map<String, String> fields) {
        String id = fields.get("ID");
        String phone = fields.get("Phone");
        String lockerId = fields.get("Locker");
        String code = fields.get("Code");
        String serviceType = fields.get("Service");
        double fee = Double.parseDouble(fields.getOrDefault("Fee", "0"));

        Reservation r = Reservation.newPending(id, phone, lockerId, code, serviceType, fee);

        if (!fields.getOrDefault("CreatedAt", "").isEmpty())
            r.setCreatedAt(LocalDateTime.parse(fields.get("CreatedAt")));
        if (!fields.getOrDefault("DropoffAt", "").isEmpty())
            r.setDropoffAt(LocalDateTime.parse(fields.get("DropoffAt")));
        if (!fields.getOrDefault("PickupAt", "").isEmpty())
            r.setPickupAt(LocalDateTime.parse(fields.get("PickupAt")));

        r.setPaymentStatus(fields.getOrDefault("Payment", "UNPAID"));
        r.setAmount(Double.parseDouble(fields.getOrDefault("Amount", "0.00")));

        reservations.put(r.getId(), r);
    }

    public void saveAll() {
        try {
            saveLockers();
            saveReservations();
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public Map<String, Locker> getLockersMap() {
        return lockers;
    }

    public Collection<Reservation> getReservations() {
        return reservations.values();
    }
    
    public void loadAll() throws IOException {
        loadLockers();
        loadReservations();
    }
    
    public Optional<Locker> findFirstAvailableLocker() {
        return lockers.values().stream()
                .filter(l -> l.isAvailable() && !l.isUnderMaintenance())
                .findFirst();
    }

    public Set<String> getActiveCodes() {
        Set<String> codes = new HashSet<>();
        for (Reservation r : reservations.values()) {
            if (!"PAID".equalsIgnoreCase(r.getPaymentStatus())) {
                codes.add(r.getCode());
            }
        }
        return codes;
    }

    public Optional<Reservation> findActiveByLockerAndCode(String lockerId, String code) {
        return reservations.values().stream()
                .filter(r -> r.getLockerId().equalsIgnoreCase(lockerId)
                        && r.getCode().equals(code)
                        && !"PAID".equalsIgnoreCase(r.getPaymentStatus()))
                .findFirst();
    }

    public Optional<Locker> findLocker(String id) {
        return Optional.ofNullable(lockers.get(id));
    }

    public void saveReservationAndLocker(Reservation r, Locker l) {
        reservations.put(r.getId(), r);
        lockers.put(l.getId(), l);
    }

    public void upsertReservation(Reservation r) {
        reservations.put(r.getId(), r);
    }

    public void completeReservation(Reservation r, Locker l) {
        reservations.put(r.getId(), r);
        lockers.put(l.getId(), l);
        if ("PAID".equalsIgnoreCase(r.getPaymentStatus())) {
            addRevenue(r.getAmount());
        }
    }

    public Optional<Reservation> findLatestForLocker(String lockerId) {
        return reservations.values().stream()
                .filter(r -> r.getLockerId().equalsIgnoreCase(lockerId))
                .max(Comparator.comparing(Reservation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public void saveLocker(Locker l) {
        lockers.put(l.getId(), l);
    }

    public Map<String, Locker> getLockers() {
        return lockers;
    }

    public double totalRevenue() {
        return totalRevenue;
    }
}
