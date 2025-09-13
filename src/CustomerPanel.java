package gui;

import model.*;
import repository.DataStore;
import util.CodeGenerator;
import util.DateTimeHandler;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class CustomerPanel extends JPanel {
	private final DataStore db;

    private final JTextField phoneField = new JTextField(14);
    private final JComboBox<String> serviceBox = new JComboBox<>(new String[] {
    		ServiceType.WASH_AND_FOLD, ServiceType.DRY_CLEANING
    });

    private final JTextField lockerIdField2 = new JTextField(6);
    private final JTextField codeField2 = new JTextField(8);

    private static final Map<String, Double> SERVICE_FEES = Map.of(
            ServiceType.WASH_AND_FOLD, 10.0,
            ServiceType.DRY_CLEANING, 18.0
    );
    private static final double LOCKER_FEE_PER_HOUR = 2.0;

    public CustomerPanel(DataStore db) {
        this.db = db;
        setLayout(new BorderLayout(12, 12));
        add(buildDropOffPanel(), BorderLayout.WEST);
        add(buildPayPickupPanel(), BorderLayout.CENTER);
    }

    //Left panel: Drop-Off
    private JPanel buildDropOffPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Drop-Off"));
        GridBagConstraints c = gbc();

        c.gridx=0; c.gridy=0; p.add(new JLabel("Phone:"), c);
        c.gridx=1; p.add(phoneField, c);

        c.gridx=0; c.gridy=1; p.add(new JLabel("Service Type:"), c);
        c.gridx=1; p.add(serviceBox, c);

        JButton createBtn = new JButton("Drop-Off");
        createBtn.addActionListener(e -> onDropOff());
        c.gridx=1; c.gridy=2;
        p.add(createBtn, c);

        return p;
    }

    //Right panel: Pay & Pick-Up
    private JPanel buildPayPickupPanel() {
    	JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Pay & Pick-Up"));
        GridBagConstraints c = gbc();

        c.gridx=0; c.gridy=0; p.add(new JLabel("Locker ID (e.g., L001):"), c);
        c.gridx=1; p.add(lockerIdField2, c);

        c.gridx=0; c.gridy=1; p.add(new JLabel("6-digit Code:"), c);
        c.gridx=1; p.add(codeField2, c);

        JButton payBtn = new JButton("Pay & Pick-Up");
        payBtn.addActionListener(e -> onPayAndPickup());
        c.gridx=1; c.gridy=2; p.add(payBtn, c);

        return p;
    }

    private void onDropOff() {
    	String phone = phoneField.getText().trim();
        if (!phone.matches("\\d{8,15}")) {
            msg("Invalid phone number (Enter 8-15 digits, e.g., 012345678)");
            return;
        }
        String type = (String) serviceBox.getSelectedItem();
        double serviceFee = SERVICE_FEES.get(type);

        var free = db.findFirstAvailableLocker();
        if (free.isEmpty()) { msg("No lockers available now."); return; }
        Locker locker = free.get();

        String code = CodeGenerator.unique6Digits(db.getActiveCodes());
        String rid = CodeGenerator.reservationId();
        Reservation r = Reservation.newPending(rid, phone, locker.getId(), code, type, serviceFee);
        locker.setAvailable(false);
        
        r.setDropoffAt(LocalDateTime.now());
        
        db.saveReservationAndLocker(r, locker);
        db.upsertReservation(r);

        msg("Drop-off successful.\nLocker unlocked!\nLocker: " + locker.getId() + "\nCode: " + code + "\n(Sent to phone " + phone + ")");
    }

    private void onPayAndPickup() {
        String lockerId = lockerIdField2.getText().trim().toUpperCase();
        String code = codeField2.getText().trim();

        Optional<Reservation> or = db.findActiveByLockerAndCode(lockerId, code);
        if (or.isEmpty()) { msg("Invalid locker/code or not reserved."); return; }
        Reservation r = or.get();
        if (r.getDropoffAt() == null) { msg("No drop-off recorded yet."); return; }

        LocalDateTime pickupTime = LocalDateTime.now();
        Duration dur = Duration.between(r.getDropoffAt(), pickupTime);
        long hours = DateTimeHandler.ceilHours(dur);
        double lockerFee = hours * LOCKER_FEE_PER_HOUR;
        double total = r.getServiceFee() + lockerFee;

        int ok = JOptionPane.showConfirmDialog(this,
                "Service: " + r.getServiceType() + " (RM " + String.format("%.2f", r.getServiceFee()) + ")\n" +
                "Locker fee: " + hours + " hour(s) × RM " + LOCKER_FEE_PER_HOUR + " = RM " + String.format("%.2f", lockerFee) + "\n" +
                "Total: RM " + String.format("%.2f", total) + "\n\nProceed to pay?",
                "Payment", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        r.setPickupAt(pickupTime);
        r.setAmount(total);
        r.setPaymentStatus(PaymentStatus.PAID);

        var ol = db.findLocker(lockerId);
        if (ol.isEmpty()) { msg("Locker not found."); return; }
        Locker locker = ol.get();
        locker.setAvailable(true);

        db.completeReservation(r, locker);
        msg("Payment successful.\nLocker unlocked. Thank you!");
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }
    private void msg(String s){ JOptionPane.showMessageDialog(this, s); }
}
