package gui;

import model.Locker;
import model.Reservation;
import repository.DataStore;
import security.AdminGate;
import util.DateTimeHandler;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;

public class AdminPanel extends JPanel {
	private final DataStore db;
    private final AdminGate gate = new AdminGate("admin123"); // same password as console

    private final JPasswordField passField = new JPasswordField(12);
    private boolean loggedIn = false;

    private final JTextField lockerIdUnlock = new JTextField(6);
    private final JTextField lockerIdDetails = new JTextField(6);
    private final JTextArea detailsArea = new JTextArea(8, 40);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Phone", "Service", "Locker", "Code", "Status", "Amount"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JButton refresh = new JButton("Refresh");

    public AdminPanel(DataStore db) {
        this.db = db;
        setLayout(new BorderLayout(12,12));
        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);
        setEnabledState(false);
    }

    private JPanel buildTop() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(new TitledBorder("Login"));
        p.add(new JLabel("Admin Password:"));
        p.add(passField);
        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> onLogin());
        p.add(loginBtn);
        return p;
    }

    private JPanel buildCenter() {
        JPanel panel = new JPanel(new GridLayout(1,2,12,12));

        // Admin Unlock
        JPanel left = new JPanel(new GridBagLayout());
        left.setBorder(new TitledBorder("Admin Unlock (status unchanged)"));
        GridBagConstraints c = gbc();
        c.gridx=0; c.gridy=0; left.add(new JLabel("Locker ID:"), c);
        c.gridx=1; left.add(lockerIdUnlock, c);
        JButton unlockBtn = new JButton("Unlock");
        unlockBtn.addActionListener(e -> onAdminUnlock());
        c.gridx=1; c.gridy=1; left.add(unlockBtn, c);

        // Locker Details
        JPanel right = new JPanel(new GridBagLayout());
        right.setBorder(new TitledBorder("View Locker Details"));
        c = gbc();
        c.gridx=0; c.gridy=0; right.add(new JLabel("Locker ID:"), c);
        c.gridx=1; right.add(lockerIdDetails, c);
        JButton viewBtn = new JButton("View");
        viewBtn.addActionListener(e -> onViewDetails());
        c.gridx=2; right.add(viewBtn, c);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        c.gridx=0; c.gridy=1; c.gridwidth=3;
        right.add(new JScrollPane(detailsArea), c);

        panel.add(left);
        panel.add(right);
        return panel;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder("Reservations"));
        refresh.addActionListener(e -> loadReservations());
        p.add(refresh, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void onLogin() {
        String pass = new String(passField.getPassword());
        if (gate.allow(pass)) {
            loggedIn = true;
            setEnabledState(true);
            JOptionPane.showMessageDialog(this, "Logged in.");
        } else {
            JOptionPane.showMessageDialog(this, "Access denied.");
        }
    }

    private void onAdminUnlock() {
        if (!requireLogin()) return;
        String id = lockerIdUnlock.getText().trim().toUpperCase();
        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) { msg("Locker not found."); return; }
        msg("Locker " + id + " unlocked!");
    }

    private void onViewDetails() {
        if (!requireLogin()) return;
        String id = lockerIdDetails.getText().trim().toUpperCase();
        Optional<Locker> ol = db.findLocker(id);
        if (ol.isEmpty()) { msg("Locker not found."); return; }

        Locker l = ol.get();
        Optional<Reservation> last = db.findLatestForLocker(id);

        StringBuilder sb = new StringBuilder();
        sb.append("Locker: ").append(l.getId()).append("\n");
        sb.append("Availability: ").append(l.isAvailable() ? "AVAILABLE" : "UNAVAILABLE").append("\n");
        if (last.isPresent()) {
            Reservation r = last.get();
            var dur = util.DateTimeHandler.safeDuration(r.getDropoffAt(), r.getPickupAt());
            long hrs = dur == null ? 0 : DateTimeHandler.ceilHours(dur);
            sb.append("Phone Number: ").append(r.getPhone()).append("\n");
            sb.append("Service Type: ").append(r.getServiceType()).append("\n");
            sb.append("Usage Duration: ").append(dur == null ? "-" : hrs + " hour(s)").append("\n");
            sb.append("Payment Status: ").append(r.getPaymentStatus()).append(" | RM ").append(String.format("%.2f", r.getAmount())).append("\n");
        } else {
            sb.append("No history for this locker yet.\n");
        }
        sb.append("Total Revenue (all): RM ").append(String.format("%.2f", db.totalRevenue()));
        detailsArea.setText(sb.toString());
    }

    private void loadReservations() {
    	if (!requireLogin()) return;  // block if not logged in
        tableModel.setRowCount(0);
        db.getReservations().stream()
            .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
            .forEach(r -> tableModel.addRow(new Object[]{
                    r.getId(), r.getPhone(), r.getServiceType(),
                    r.getLockerId(), r.getCode(), r.getPaymentStatus(),
                    String.format("%.2f", r.getAmount())
            }));
    }

    private boolean requireLogin() {
        if (!loggedIn) { msg("Please login first."); return false; }
        return true;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }
    private void setEnabledState(boolean enabled){
        lockerIdUnlock.setEnabled(enabled);
        lockerIdDetails.setEnabled(enabled);
        detailsArea.setEnabled(enabled);
        table.setEnabled(enabled);
        refresh.setEnabled(enabled);
    }
    private void msg(String s){ JOptionPane.showMessageDialog(this, s); }
}
