package gui;

import repository.DataStore;

import javax.swing.*;
import java.io.IOException;

public class MainGUI {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
            try {
                DataStore db = new DataStore("data");
                try { db.loadAll(); } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "First run: storage will be created.\n" + e.getMessage());
                }
                MainFrame frame = new MainFrame(db);
                frame.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Fatal: " + ex.getMessage());
            }
        });
	}

}
