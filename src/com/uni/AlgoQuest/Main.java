package com.uni.AlgoQuest;

import javax.swing.*;

/**
 * AlgoQuest — Η Ακαδημία των Αλγορίθμων
 * Εκπαιδευτικό παιχνίδι για το μάθημα των Αλγορίθμων (quiz, ταξινόμηση, δυαδική αναζήτηση)
 * με προσαρμοστική δυσκολία, πόντους/παράσημα, λειτουργία 2 παικτών και διαμοιρασμό σκορ.
 *
 * Σημείο εκκίνησης της εφαρμογής.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new GameFrame().setVisible(true);
        });
    }
}
