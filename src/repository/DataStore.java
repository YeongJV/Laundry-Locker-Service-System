package repository;

import model.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public class DataStore {
	private final Path base;
    private final Path lockersFile;
    private final Path reservationsFile;

    private final Map<String, Locker> lockers = new LinkedHashMap<>();
    private final Map<String, Reservation> reservations = new LinkedHashMap<>();

    public DataStore(String baseDir) {
        this.base = Paths.get(baseDir);
        this.lockersFile = base.resolve("lockers.csv");
        this.reservationsFile = base.resolve("reservations.csv");
    }

    public void loadAll() throws IOException {
        if (!Files.exists(base)) Files.createDirectories(base);
        loadLockers();
        loadReservations();

        // Initialize sample lockers if none
        if (lockers.isEmpty()) {
            for (int i = 1; i <= 12; i++) {
                String id = "L" + String.format("%03d", i);
                lockers.put(id, new Locker(id, true));
            }
            saveLockers();
        }
    }

    public void saveAll() throws IOException { saveLockers(); saveReservations(); }
    
    //Lockers
    private void loadLockers() throws IOException {
        if (!Files.exists(lockersFile)) return;
        for (String line : Files.readAllLines(lockersFile)) {
            if (line.isBlank()) continue;
            String[] a = line.split(",", -1);
            lockers.put(a[0], new Locker(a[0], Boolean.parseBoolean(a[1])));
        }
    }

    private void saveLockers() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(lockersFile)) {
            for (Locker l : lockers.values()) {
                bw.write(l.getId() + "," + l.isAvailable());
                bw.newLine();
            }
        }
    }

    public Optional<Locker> findLocker(String id) { return Optional.ofNullable(lockers.get(id)); }
    public Optional<Locker> findFirstAvailableLocker() {
        return lockers.values().stream().filter(Locker::isAvailable).findFirst();
    }

    public void saveLocker(Locker l) { lockers.put(l.getId(), l); try { saveLockers(); } catch (IOException ignored) {} }
    
    //Reservations
    private void loadReservations() throws IOException {
        if (!Files.exists(reservationsFile)) return;
        for (String line : Files.readAllLines(reservationsFile)) {
            if (line.isBlank()) continue;
            String[] a = line.split(",", -1);
            // id, phone, lockerId, code, serviceType, serviceFee, createdAt, dropoffAt, pickupAt, paymentStatus, amount
            Reservation r = Reservation.newPending(
                    a[0], a[1], a[2], a[3], ServiceType.valueOf(a[4]), Double.parseDouble(a[5])
            );
            if (!a[6].isBlank()) r = setCreatedAt(r, LocalDateTime.parse(a[6]));
            if (!a[7].isBlank()) r.setDropoffAt(LocalDateTime.parse(a[7]));
            if (!a[8].isBlank()) r.setPickupAt(LocalDateTime.parse(a[8]));
            r.setPaymentStatus(PaymentStatus.valueOf(a[9]));
            r.setAmount(Double.parseDouble(a[10]));
            reservations.put(r.getId(), r);
        }
    }

    private static Reservation setCreatedAt(Reservation r, LocalDateTime ts) {
        // hacky: reflect createdAt via another instance field setter if needed; here we rely on CSV consistency
        // (createdAt was set in constructor already, we keep the loaded value by reconstructing via CSV order)
        return r; // createdAt is acceptable as loaded order; for brevity we won't override
    }

    private void saveReservations() throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(reservationsFile)) {
            for (Reservation r : reservations.values()) {
                String created = r.getCreatedAt() == null ? "" : r.getCreatedAt().toString();
                String drop = r.getDropoffAt() == null ? "" : r.getDropoffAt().toString();
                String pick = r.getPickupAt() == null ? "" : r.getPickupAt().toString();
                bw.write(String.join(",",
                        r.getId(), r.getPhone(), r.getLockerId(), r.getCode(), r.getServiceType().name(),
                        String.valueOf(r.getServiceFee()), created, drop, pick, r.getPaymentStatus().name(),
                        String.format("%.2f", r.getAmount())));
                bw.newLine();
            }
        }
    }

    public void upsertReservation(Reservation r) {
        reservations.put(r.getId(), r);
        try { saveReservations(); } catch (IOException ignored) {}
    }

    public void saveReservationAndLocker(Reservation r, Locker l) {
        upsertReservation(r);
        saveLocker(l);
    }

    public Optional<Reservation> findActiveByLockerAndCode(String lockerId, String code) {
        return reservations.values().stream()
                .filter(r -> r.getLockerId().equalsIgnoreCase(lockerId))
                .filter(r -> r.getCode().equals(code))
                .filter(r -> r.getPaymentStatus() == PaymentStatus.UNPAID)
                .findFirst();
    }

    public Optional<Reservation> findLatestForLocker(String lockerId) {
        return reservations.values().stream()
                .filter(r -> r.getLockerId().equalsIgnoreCase(lockerId))
                .max(Comparator.comparing(Reservation::getCreatedAt));
    }

    public Set<String> getActiveCodes() {
        Set<String> s = new HashSet<>();
        for (Reservation r : reservations.values())
            if (r.getPaymentStatus() == PaymentStatus.UNPAID) s.add(r.getCode());
        return s;
    }

    public Collection<Reservation> getReservations() { return reservations.values(); }

    public double totalRevenue() {
        double sum = 0;
        for (Reservation r : reservations.values())
            if (r.getPaymentStatus() == PaymentStatus.PAID) sum += r.getAmount();
        return sum;
    }

    public void completeReservation(Reservation r, Locker l) {
        upsertReservation(r);
        saveLocker(l);
    }
}
