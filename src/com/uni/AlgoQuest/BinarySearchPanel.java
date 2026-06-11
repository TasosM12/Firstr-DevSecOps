package com.uni.AlgoQuest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Το Κυνήγι του Αριθμού — παιχνίδι που διδάσκει τη ΔΥΑΔΙΚΗ ΑΝΑΖΗΤΗΣΗ.
 * Ο υπολογιστής διαλέγει κρυφό αριθμό στο [1..N]. Μετά από κάθε προσπάθεια ο παίκτης
 * μαθαίνει αν ο κρυφός είναι μεγαλύτερος ή μικρότερος και βλέπει το διάστημα των
 * υποψηφίων να μικραίνει. Βέλτιστη στρατηγική: πάντα η μέση ⇒ ⌈log₂N⌉ βήματα.
 */
class BinarySearchPanel extends JPanel {

    private final GameFrame frame;
    private final String name;
    private final int diff, N, secret, optimal;

    private int lo = 1, hi, steps, hintsUsed;
    private boolean done;
    private final List<int[]> guesses = new ArrayList<>();   // {τιμή, -1 πολύ μεγάλη / +1 πολύ μικρή / 0 σωστή}

    private final JLabel infoLbl, statsLbl;
    private final JTextArea log;
    private final JTextField input;
    private final JButton guessBtn, doneBtn;
    private final JPanel canvas;

    BinarySearchPanel(GameFrame frame, String name) {
        this.frame = frame;
        this.name = name;
        this.diff = frame.engine.level();
        Random rng = frame.sessionRng();
        this.N = frame.engine.searchRange();
        this.hi = N;
        this.secret = 1 + rng.nextInt(N);
        this.optimal = (int) Math.ceil(Math.log(N) / Math.log(2));

        setLayout(new BorderLayout());
        JPanel s = Theme.screen(new BorderLayout(0, 10));
        s.setBorder(new EmptyBorder(14, 22, 14, 22));
        add(s);

        // ── Πάνω: τίτλος + στατιστικά + έξοδος
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel t = Theme.title("Το Κυνήγι του Αριθμού — σκέψου σαν τη δυαδική αναζήτηση!", 19);
        statsLbl = Theme.label(" ", 14, Theme.MUTED);
        JButton quit = Theme.ghost("✕ Μενού");
        quit.addActionListener(e -> frame.openMenu());
        top.add(t, BorderLayout.WEST);
        top.add(quit, BorderLayout.EAST);
        top.add(statsLbl, BorderLayout.SOUTH);
        s.add(top, BorderLayout.NORTH);

        // ── Κέντρο: οπτικοποίηση διαστήματος + ιστορικό
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        canvas = new JPanel() {
            @Override protected void paintComponent(Graphics g) { paintRange((Graphics2D) g.create()); }
        };
        canvas.setOpaque(false);
        canvas.setPreferredSize(new Dimension(100, 170));
        JPanel canvasCard = Theme.card(new BorderLayout());
        canvasCard.add(canvas, BorderLayout.CENTER);
        center.add(canvasCard, BorderLayout.NORTH);

        log = Theme.area(14, Theme.MUTED);
        log.setText("Ο υπολογιστής διάλεξε έναν αριθμό από το 1 έως το " + N + ".\n"
                + "Με τη στρατηγική της διχοτόμησης αρκούν το πολύ " + optimal + " προσπάθειες. Καλή τύχη!\n");
        JScrollPane sp = new JScrollPane(log);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        JPanel logCard = Theme.card(new BorderLayout());
        logCard.add(sp, BorderLayout.CENTER);
        center.add(logCard, BorderLayout.CENTER);
        s.add(center, BorderLayout.CENTER);

        // ── Κάτω: είσοδος + κουμπιά + ανατροφοδότηση
        JPanel bottom = Theme.card(new BorderLayout(0, 6));
        infoLbl = Theme.title("Δώσε την πρώτη σου προσπάθεια. Συμβουλή: η μέση του διαστήματος κόβει τους μισούς υποψήφιους!", 15);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setOpaque(false);
        input = Theme.field(7);
        input.addActionListener(e -> guess());
        guessBtn = Theme.primary("Μάντεψε!");
        guessBtn.addActionListener(e -> guess());
        JButton hintBtn = Theme.gold("Δείξε τη μέση (−8 π.)");
        hintBtn.addActionListener(e -> hint());
        doneBtn = Theme.primary("Αποτελέσματα ★");
        doneBtn.setEnabled(false);
        doneBtn.addActionListener(e -> finishGame());
        row.add(Theme.label("Η πρόβλεψή σου:", 15, Theme.TEXT));
        row.add(input);
        row.add(guessBtn);
        row.add(hintBtn);
        row.add(doneBtn);
        bottom.add(infoLbl, BorderLayout.CENTER);
        bottom.add(row, BorderLayout.SOUTH);
        s.add(bottom, BorderLayout.SOUTH);

        updateStats();
    }

    // ───────────────────────── Λογική ─────────────────────────

    private void guess() {
        if (done) return;
        int g;
        try { g = Integer.parseInt(input.getText().trim()); }
        catch (NumberFormatException e) {
            infoLbl.setText("Δώσε έναν ακέραιο από το 1 έως το " + N + ".");
            infoLbl.setForeground(Theme.ERR);
            return;
        }
        if (g < 1 || g > N) {
            infoLbl.setText("Εκτός ορίων! Οι αριθμοί είναι από το 1 έως το " + N + ".");
            infoLbl.setForeground(Theme.ERR);
            return;
        }
        steps++;
        input.setText("");
        boolean wasted = g < lo || g > hi;   // πρόβλεψη σε ήδη αποκλεισμένη περιοχή

        if (g == secret) {
            guesses.add(new int[]{ g, 0 });
            win();
        } else if (g < secret) {
            guesses.add(new int[]{ g, +1 });
            lo = Math.max(lo, g + 1);
            feedback(g, "ΜΕΓΑΛΥΤΕΡΟΣ ▲", wasted);
            SoundFx.click();
        } else {
            guesses.add(new int[]{ g, -1 });
            hi = Math.min(hi, g - 1);
            feedback(g, "ΜΙΚΡΟΤΕΡΟΣ ▼", wasted);
            SoundFx.click();
        }
        updateStats();
        canvas.repaint();
    }

    private void feedback(int g, String dir, boolean wasted) {
        int remaining = hi - lo + 1;
        int needed = remaining <= 1 ? 1 : (int) Math.ceil(Math.log(remaining) / Math.log(2));
        log.append("#" + steps + ":  " + g + "  →  ο κρυφός είναι " + dir
                + "   |   Υποψήφιοι: [" + lo + ".." + hi + "] (" + remaining + " αριθμοί)\n");
        if (wasted) {
            infoLbl.setText("Σπατάλη προσπάθειας: το " + g + " ήταν ήδη αποκλεισμένο! Μείνε στο [" + lo + ".." + hi + "].");
            infoLbl.setForeground(Theme.ERR);
        } else {
            infoLbl.setText("Ο κρυφός είναι " + dir.toLowerCase() + ".  Με διχοτόμηση αρκούν ακόμη ~" + needed + " βήματα.");
            infoLbl.setForeground(Theme.TEXT);
        }
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void hint() {
        if (done) return;
        hintsUsed++;
        int mid = (lo + hi) / 2;
        infoLbl.setText("Η δυαδική αναζήτηση θα ρωτούσε τη ΜΕΣΗ του [" + lo + ".." + hi + "], δηλαδή το " + mid + ".");
        infoLbl.setForeground(Theme.GOLD);
        input.setText(String.valueOf(mid));
        updateStats();
    }

    private void win() {
        done = true;
        SoundFx.win();
        int stars = stars();
        log.append("#" + steps + ":  " + secret + "  →  ΣΩΣΤΑ! ★\n");
        infoLbl.setText("Το βρήκες σε " + steps + " βήματα (βέλτιστο: " + optimal + ")  "
                + "★".repeat(stars) + "☆".repeat(3 - stars));
        infoLbl.setForeground(Theme.GOLD);
        guessBtn.setEnabled(false);
        doneBtn.setEnabled(true);
        frame.engine.record(stars >= 2);   // τροφοδοτεί την προσαρμοστική δυσκολία
        updateStats();
        canvas.repaint();
    }

    private int stars() {
        return steps <= optimal ? 3 : steps <= optimal + 2 ? 2 : 1;
    }

    private void finishGame() {
        int stars = stars();
        int pts = Math.max(10, 30 * diff * stars + Math.max(0, optimal + 4 - steps) * 6 - 8 * hintsUsed);
        SessionResult r = new SessionResult("Το Κυνήγι του Αριθμού");
        r.challengeCode = frame.challengeCode();
        PlayerScore ps = new PlayerScore(name);
        ps.score = pts;
        ps.stars = stars;
        if (steps <= optimal && hintsUsed == 0) ps.badges.add("Λογαριθμικό Μυαλό");
        r.players.add(ps);
        r.lines.add("Εύρος: 1.." + N + "  •  Βήματα: " + steps + " (βέλτιστο: " + optimal + ")");
        r.lines.add("Υποδείξεις: " + hintsUsed + "  •  Δυσκολία: "
                + switch (diff) { case 1 -> "Εύκολο"; case 2 -> "Μέτριο"; default -> "Δύσκολο"; });
        frame.finish(r);
    }

    private void updateStats() {
        statsLbl.setText("Εύρος: 1.." + N + "   •   Βήματα: " + steps + " / βέλτιστο " + optimal
                + "   •   Υποδείξεις: " + hintsUsed
                + "   •   Υποψήφιοι: " + Math.max(0, hi - lo + 1));
    }

    // ───────────────────────── Σχεδίαση ─────────────────────────

    private void paintRange(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int W = canvas.getWidth(), H = canvas.getHeight();
        int x0 = 30, x1 = W - 30, barY = H / 2 - 10, barH = 22;
        double scale = (x1 - x0) / (double) (N - 1);

        // Όλο το εύρος (σκούρο) και το «ζωντανό» διάστημα [lo..hi] (φωτεινό)
        g2.setColor(Theme.BG2);
        g2.fillRoundRect(x0, barY, x1 - x0, barH, 12, 12);
        int lx = x0 + (int) ((lo - 1) * scale);
        int hx = x0 + (int) ((hi - 1) * scale);
        g2.setColor(done ? Theme.OK : Theme.ACCENT);
        g2.fillRoundRect(lx, barY, Math.max(8, hx - lx), barH, 12, 12);

        g2.setFont(Theme.font(Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Theme.MUTED);
        g2.drawString("1", x0 - fm.stringWidth("1") / 2, barY + barH + 18);
        g2.drawString(String.valueOf(N), x1 - fm.stringWidth(String.valueOf(N)) / 2, barY + barH + 18);
        g2.setColor(Theme.TEXT);
        String range = "[" + lo + " .. " + hi + "]";
        g2.drawString(range, (lx + hx - fm.stringWidth(range)) / 2, barY - 28);

        // Σημάδια προηγούμενων προσπαθειών
        for (int[] gs : guesses) {
            int gx = x0 + (int) ((gs[0] - 1) * scale);
            Color c = gs[1] == 0 ? Theme.OK : gs[1] > 0 ? Theme.GOLD : Theme.ERR;
            g2.setColor(c);
            g2.fillOval(gx - 5, barY + barH / 2 - 5, 10, 10);
            String s = String.valueOf(gs[0]);
            g2.drawString(s, gx - fm.stringWidth(s) / 2, barY + barH + 36);
            String arrow = gs[1] == 0 ? "★" : gs[1] > 0 ? "▲" : "▼";
            g2.drawString(arrow, gx - fm.stringWidth(arrow) / 2, barY - 8);
        }
        g2.dispose();
    }
}
