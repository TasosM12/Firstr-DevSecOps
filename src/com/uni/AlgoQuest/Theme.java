package com.uni.AlgoQuest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Κεντρικό οπτικό στυλ της εφαρμογής: χρώματα, γραμματοσειρές και έτοιμα styled components. */
final class Theme {

    static final Color BG     = new Color(0x0F1B2D);   // σκούρο φόντο (πάνω)
    static final Color BG2    = new Color(0x16263F);   // σκούρο φόντο (κάτω, gradient)
    static final Color CARD   = new Color(0x1C2E4A);   // φόντο καρτών
    static final Color FIELD  = new Color(0x223A5E);   // φόντο κουμπιών-επιλογών
    static final Color ACCENT = new Color(0x38BDF8);   // γαλάζιο accent
    static final Color GOLD   = new Color(0xFBBF24);   // χρυσό (αμοιβές/αστέρια)
    static final Color OK     = new Color(0x34D399);   // πράσινο (σωστό)
    static final Color ERR    = new Color(0xF87171);   // κόκκινο (λάθος)
    static final Color TEXT   = new Color(0xE8EFF9);
    static final Color MUTED  = new Color(0x93A4BD);

    private Theme() { }

    static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    static void install() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) { }
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 15));
        UIManager.put("OptionPane.buttonFont", font(Font.BOLD, 14));
        UIManager.put("TextField.font", font(Font.PLAIN, 15));
    }

    static JLabel label(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    static JLabel title(String text, int size) {
        JLabel l = label(text, size, TEXT);
        l.setFont(font(Font.BOLD, size));
        return l;
    }

    /** Κουμπί με στρογγυλεμένες γωνίες και εφέ hover/πατήματος. */
    static JButton button(String text, Color bg, Color fg, int size) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = (Color) getClientProperty("bg");
                if (base == null) base = bg;
                if (!isEnabled()) base = base.darker();
                else if (getModel().isPressed()) base = base.darker();
                else if (getModel().isRollover()) base = base.brighter();
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.putClientProperty("bg", bg);
        b.setForeground(fg);
        b.setFont(font(Font.BOLD, size));
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton primary(String t) { return button(t, ACCENT, new Color(0x0B1626), 15); }
    static JButton gold(String t)    { return button(t, GOLD,   new Color(0x33260A), 15); }
    static JButton ghost(String t)   { return button(t, CARD, TEXT, 14); }

    /** Πάνελ-κάρτα με στρογγυλεμένο φόντο. */
    static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        return p;
    }

    /** Πάνελ ολόκληρης οθόνης με κάθετο gradient φόντο. */
    static JPanel screen(LayoutManager lm) {
        JPanel p = new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, BG, 0, getHeight(), BG2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(true);
        return p;
    }

    static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(font(Font.PLAIN, 15));
        f.setBackground(FIELD);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2C4368), 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        return f;
    }

    /** Κείμενο πολλών γραμμών με αναδίπλωση, διαφανές (για εξηγήσεις/ανατροφοδότηση). */
    static JTextArea area(int size, Color color) {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setOpaque(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(font(Font.PLAIN, size));
        a.setForeground(color);
        a.setBorder(new EmptyBorder(4, 2, 4, 2));
        return a;
    }
}
