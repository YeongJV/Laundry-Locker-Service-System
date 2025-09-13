package gui;

import repository.DataStore;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
	public MainFrame(DataStore db) {
        super("Laundry Locker Service System – GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Customer", new CustomerPanel(db));
        tabs.add("Admin", new AdminPanel(db));
        setContentPane(tabs);
    }
}
