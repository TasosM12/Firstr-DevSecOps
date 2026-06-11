package com.uni.AlgoQuest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * Sort Race — διαδραστικό παιχνίδι ταξινόμησης.
 * Ο παίκτης ανταλλάσσει ΓΕΙΤΟΝΙΚΕΣ στήλες (όπως ο Bubble Sort) μέχρι να ταξινομηθούν
 * αύξουσα. Στόχος: όσο το δυνατόν λιγότερες αντιμεταβολές — το ελάχιστο δυνατό
 * ισούται με το πλήθος των αντιστροφών του αρχικού πίνακα (αυτό διδάσκει το παιχνίδι).
 */
class SortRacePanel extends JPanel {

    private final GameFrame frame;
    private final String name;
    private final int diff;
    private final int[] vals;
    private final int optimal;                 // αντιστροφές αρχικού πίνακα = ελάχιστες κινήσεις

    private int sel = -1, hover = -1, swaps, hintsUsed, seconds;
    private int flashA = -1, flashB = -1;      // ζεύγος που «αναβοσβήνει» ως υπόδειξη
    private boolean done;

    private final JLabel statsLbl, infoLbl;
    private final JButton doneBtn;
    private final JPanel canvas;
    private final javax.swing.Timer clock;

    SortRacePanel(GameFrame frame, String name) {
        this.frame = frame;
        this.name = name;
        this.diff = frame.engine.level();
        Random rng = frame.sessionRng();

        // Τυχαίες, διακριτές τιμές 5..99
        int n = frame.engine.arraySize();
        vals = new int[n];
        boolean[] taken = new boolean[100];
        for (int i = 0; i < n; i++) {
            int v;
            do { v = 5 + rng.nextInt(95); } while (taken[v]);
            taken[v] = true;
            vals[i] = v;
        }
        optimal = inversions();
        clock = new javax.swing.Timer(1000, e -> { seconds++; updateStats(); });

        setLayout(new BorderLayout());
        JPanel s = Theme.screen(new BorderLayout(0, 10));
        s.setBorder(new EmptyBorder(14, 22, 14, 22));
        add(s);

        // ── Πάνω: τίτλος + στατιστικά + έξοδος
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel t = Theme.title("Sort Race — ταξινόμησε με τις λιγότερες αντιμεταβολές!", 19);
        statsLbl = Theme.label(" ", 14, Theme.MUTED);
        JButton quit = Theme.ghost("✕ Μενού");
        quit.addActionListener(e -> { clock.stop(); frame.openMenu(); });
        top.add(t, BorderLayout.WEST);
        top.add(quit, BorderLayout.EAST);
        top.add(statsLbl, BorderLayout.SOUTH);
        s.add(top, BorderLayout.NORTH);

        // ── Κέντρο: καμβάς με τις στήλες
        canvas = new JPanel() {
            @Override protected void paintComponent(Graphics g) { paintBars((Graphics2D) g.create()); }
        };
        canvas.setOpaque(false);
        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { click(barAt(e.getX())); }
            @Override public void mouseMoved(MouseEvent e)   { hover = barAt(e.getX()); canvas.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = -1; canvas.repaint(); }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        JPanel card = Theme.card(new BorderLayout());
        card.add(canvas, BorderLayout.CENTER);
        s.add(card, BorderLayout.CENTER);

        // ── Κάτω: ανατροφοδότηση + κουμπιά
        JPanel bottom = Theme.card(new BorderLayout(0, 6));
        infoLbl = Theme.title("Κάνε κλικ σε δύο γειτονικές στήλες για να τις ανταλλάξεις.", 15);
        infoLbl.setForeground(Theme.TEXT);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        JButton hint = Theme.gold("Υπόδειξη Bubble Sort (−6 π.)");
        hint.addActionListener(e -> hint());
        doneBtn = Theme.primary("Αποτελέσματα ★");
        doneBtn.setEnabled(false);
        doneBtn.addActionListener(e -> finishGame());
        btns.add(hint);
        btns.add(doneBtn);
        bottom.add(infoLbl, BorderLayout.CENTER);
        bottom.add(btns, BorderLayout.EAST);
        s.add(bottom, BorderLayout.SOUTH);

        clock.start();
        updateStats();
    }

    // ───────────────────────── Λογική ─────────────────────────

    private int inversions() {
        int inv = 0;
        for (int i = 0; i < vals.length; i++)
            for (int j = i + 1; j < vals.length; j++)
                if (vals[i] > vals[j]) inv++;
        return inv;
    }

    private boolean sorted() {
        for (int i = 0; i + 1 < vals.length; i++)
            if (vals[i] > vals[i + 1]) return false;
        return true;
    }

    private void click(int i) {
        if (done || i < 0) return;
        if (sel < 0) { sel = i; }
        else if (sel == i) { sel = -1; }
        else if (Math.abs(sel - i) == 1) { doSwap(sel, i); sel = -1; }
        else {
            infoLbl.setText("Μόνο ΓΕΙΤΟΝΙΚΕΣ στήλες — έτσι δουλεύει ο Bubble Sort!");
            infoLbl.setForeground(Theme.ERR);
            sel = i;
        }
        canvas.repaint();
    }

    private void doSwap(int a, int b) {
        int before = inversions();
        int tmp = vals[a]; vals[a] = vals[b]; vals[b] = tmp;
        swaps++;
        SoundFx.swap();
        int after = inversions();
        if (after < before) {
            infoLbl.setText("Καλή κίνηση! Απομένουν " + after + " αντιστροφές.");
            infoLbl.setForeground(Theme.OK);
        } else {
            infoLbl.setText("Προσοχή: αυτή η κίνηση αύξησε την αταξία (" + after + " αντιστροφές).");
            infoLbl.setForeground(Theme.ERR);
        }
        updateStats();
        if (sorted()) boardDone();
    }

    private void hint() {
        if (done) return;
        for (int i = 0; i + 1 < vals.length; i++) {
            if (vals[i] > vals[i + 1]) {
                flashA = i; flashB = i + 1;
                hintsUsed++;
                infoLbl.setText("Ο Bubble Sort θα αντάλλασσε τις κίτρινες στήλες (θέσεις "
                        + (i + 1) + " και " + (i + 2) + ").");
                infoLbl.setForeground(Theme.GOLD);
                canvas.repaint();
                javax.swing.Timer t = new javax.swing.Timer(900, e -> {
                    flashA = flashB = -1;
                    canvas.repaint();
                });
                t.setRepeats(false);
                t.start();
                updateStats();
                return;
            }
        }
    }

    private void boardDone() {
        done = true;
        clock.stop();
        SoundFx.win();
        int stars = stars();
        infoLbl.setText("Ταξινομήθηκε! " + "★".repeat(stars) + "☆".repeat(3 - stars)
                + "  —  " + swaps + " αντιμεταβολές (βέλτιστες: " + optimal + ")");
        infoLbl.setForeground(Theme.GOLD);
        doneBtn.setEnabled(true);
        frame.engine.record(stars >= 2);   // τροφοδοτεί την προσαρμοστική δυσκολία
        canvas.repaint();
    }

    private int stars() {
        return swaps <= optimal ? 3 : swaps <= optimal + 2 ? 2 : 1;
    }

    private void finishGame() {
        int stars = stars();
        int pts = Math.max(10, (25 * diff + 4 * optimal) * stars - 6 * hintsUsed);
        SessionResult r = new SessionResult("Sort Race");
        r.challengeCode = frame.challengeCode();
        PlayerScore ps = new PlayerScore(name);
        ps.score = pts;
        ps.stars = stars;
        ps.bestStreak = 0;
        if (stars == 3 && hintsUsed == 0) ps.badges.add("Master Ταξινόμησης");
        r.players.add(ps);
        r.lines.add("Αντιμεταβολές: " + swaps + " (βέλτιστες: " + optimal + ")");
        r.lines.add("Υποδείξεις: " + hintsUsed + "  •  Χρόνος: " + seconds + "s");
        r.lines.add("Μέγεθος πίνακα: " + vals.length + "  •  Δυσκολία: " + diffName());
        frame.finish(r);
    }

    private String diffName() {
        return switch (diff) { case 1 -> "Εύκολο"; case 2 -> "Μέτριο"; default -> "Δύσκολο"; };
    }

    private void updateStats() {
        statsLbl.setText("Αντιμεταβολές: " + swaps + "   •   Βέλτιστες δυνατές: " + optimal
                + "   •   Υποδείξεις: " + hintsUsed + "   •   Χρόνος: " + seconds + "s"
                + "   •   Δυσκολία: " + diffName());
    }

    // ───────────────────────── Σχεδίαση ─────────────────────────

    private int pad() { return 40; }
    private int gap() { return 14; }

    private int barWidth() {
        return Math.max(20, (canvas.getWidth() - 2 * pad() - gap() * (vals.length - 1)) / vals.length);
    }

    private int barAt(int x) {
        int bw = barWidth();
        for (int i = 0; i < vals.length; i++) {
            int bx = pad() + i * (bw + gap());
            if (x >= bx && x <= bx + bw) return i;
        }
        return -1;
    }

    private void paintBars(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int W = canvas.getWidth(), H = canvas.getHeight();
        int bw = barWidth();
        int max = 0;
        for (int v : vals) max = Math.max(max, v);

        g2.setFont(Theme.font(Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < vals.length; i++) {
            int x = pad() + i * (bw + gap());
            int h = (int) ((H - 90) * (vals[i] / (double) max));
            int y = H - 40 - h;

            Color c = Theme.ACCENT;
            if (done) c = Theme.OK;
            else if (i == flashA || i == flashB) c = Theme.GOLD;
            else if (i == sel) c = Theme.GOLD;
            else if (i == hover) c = Theme.ACCENT.brighter();
            g2.setColor(c);
            g2.fillRoundRect(x, y, bw, h, 12, 12);

            if (i == sel && !done) {                    // περίγραμμα στην επιλεγμένη
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, bw, h, 12, 12);
            }

            String v = String.valueOf(vals[i]);         // τιμή πάνω από τη στήλη
            g2.setColor(Theme.TEXT);
            g2.drawString(v, x + (bw - fm.stringWidth(v)) / 2, y - 8);

            String idx = String.valueOf(i + 1);         // θέση κάτω από τη στήλη
            g2.setColor(Theme.MUTED);
            g2.drawString(idx, x + (bw - fm.stringWidth(idx)) / 2, H - 18);
        }
        g2.dispose();
    }
}
