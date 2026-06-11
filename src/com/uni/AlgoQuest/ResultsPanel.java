package com.uni.AlgoQuest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Οθόνη αποτελεσμάτων: σκορ, αστέρια, νέα παράσημα, λεπτομέρειες παρτίδας
 * και ΔΙΑΜΟΙΡΑΣΜΟΣ: κάρτα σκορ που αντιγράφεται στο πρόχειρο και αποθηκεύεται
 * σε αρχείο .txt, μαζί με τον κωδικό πρόκλησης για να παίξουν φίλοι την ίδια παρτίδα.
 */
class ResultsPanel extends JPanel {

    private final GameFrame frame;
    private final SessionResult r;
    private final JLabel status;

    ResultsPanel(GameFrame frame, SessionResult r, List<String> freshBadges) {
        this.frame = frame;
        this.r = r;

        setLayout(new BorderLayout());
        JPanel s = Theme.screen(new GridBagLayout());
        add(s);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel t = Theme.title("Αποτελέσματα — " + r.game, 28);
        t.setForeground(Theme.ACCENT);
        t.setAlignmentX(CENTER_ALIGNMENT);
        col.add(t);
        col.add(Box.createVerticalStrut(16));

        // ── Κάρτες παικτών (1 ή 2)
        int top = 0;
        for (PlayerScore ps : r.players) top = Math.max(top, ps.score);
        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        cardsRow.setOpaque(false);
        for (PlayerScore ps : r.players)
            cardsRow.add(playerCard(ps, r.players.size() == 2 && ps.score == top));
        cardsRow.setAlignmentX(CENTER_ALIGNMENT);
        col.add(cardsRow);
        col.add(Box.createVerticalStrut(14));

        // ── Λεπτομέρειες + νέα παράσημα + κωδικός πρόκλησης
        JPanel info = Theme.card(new GridLayout(0, 1, 0, 4));
        for (String line : r.lines) info.add(Theme.label(line, 14, Theme.MUTED));
        for (String b : freshBadges) {
            JLabel l = Theme.label("ΝΕΟ ΠΑΡΑΣΗΜΟ:  " + b, 15, Theme.GOLD);
            l.setFont(Theme.font(Font.BOLD, 15));
            info.add(l);
        }
        if (r.challengeCode != null) {
            JLabel code = Theme.label("Κωδικός πρόκλησης: " + r.challengeCode
                    + "   —   δώσ' τον σε φίλο για να παίξει την ίδια παρτίδα!", 14, Theme.ACCENT);
            info.add(code);
        }
        info.setAlignmentX(CENTER_ALIGNMENT);
        info.setMaximumSize(new Dimension(700, 220));
        col.add(info);
        col.add(Box.createVerticalStrut(16));

        // ── Κουμπιά
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btns.setOpaque(false);
        JButton share = Theme.gold("Διαμοιρασμός κάρτας σκορ");
        share.addActionListener(e -> share());
        JButton again = Theme.primary("Παίξε ξανά");
        again.addActionListener(e -> { SoundFx.click(); frame.replay.run(); });
        JButton board = Theme.ghost("Βαθμολογίες");
        board.addActionListener(e -> { SoundFx.click(); frame.openLeaderboard(); });
        JButton menu = Theme.ghost("Αρχικό μενού");
        menu.addActionListener(e -> { SoundFx.click(); frame.openMenu(); });
        btns.add(share); btns.add(again); btns.add(board); btns.add(menu);
        btns.setAlignmentX(CENTER_ALIGNMENT);
        col.add(btns);

        status = Theme.label(" ", 13, Theme.OK);
        status.setAlignmentX(CENTER_ALIGNMENT);
        col.add(Box.createVerticalStrut(8));
        col.add(status);

        s.add(col, new GridBagConstraints());
    }

    /** Κάρτα ενός παίκτη με σκορ, αστέρια και πρόοδο προφίλ. */
    private JPanel playerCard(PlayerScore ps, boolean winner) {
        JPanel c = Theme.card(new GridLayout(0, 1, 0, 2));
        c.setPreferredSize(new Dimension(300, winner ? 210 : 195));

        if (winner) {
            JLabel w = Theme.label("ΝΙΚΗΤΗΣ", 13, Theme.GOLD);
            w.setFont(Theme.font(Font.BOLD, 13));
            w.setHorizontalAlignment(SwingConstants.CENTER);
            c.add(w);
        }
        JLabel name = Theme.title(ps.name, 20);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        c.add(name);

        JLabel score = Theme.label(ps.score + " πόντοι", 30, Theme.GOLD);
        score.setFont(Theme.font(Font.BOLD, 30));
        score.setHorizontalAlignment(SwingConstants.CENTER);
        c.add(score);

        JLabel stars = Theme.label("★".repeat(ps.stars) + "☆".repeat(3 - ps.stars), 22, Theme.GOLD);
        stars.setHorizontalAlignment(SwingConstants.CENTER);
        c.add(stars);

        if (ps.total > 0) {
            JLabel cl = Theme.label("Σωστές: " + ps.correct + "/" + ps.total
                    + (ps.bestStreak > 1 ? "  •  Σερί: " + ps.bestStreak : ""), 14, Theme.MUTED);
            cl.setHorizontalAlignment(SwingConstants.CENTER);
            c.add(cl);
        }
        PlayerProfile pr = frame.profile(ps.name);
        JLabel lvl = Theme.label("Επίπεδο " + pr.level() + "  •  " + pr.xp + " XP  •  "
                + pr.badges.size() + " παράσημα", 13, Theme.MUTED);
        lvl.setHorizontalAlignment(SwingConstants.CENTER);
        c.add(lvl);
        return c;
    }

    /** Δημιουργεί την κάρτα σκορ, την αντιγράφει στο πρόχειρο και τη σώζει σε .txt. */
    private void share() {
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════ ALGOQUEST ══════════════\n");
        sb.append(" Παιχνίδι: ").append(r.game).append("\n");
        sb.append(" Ημερομηνία: ")
          .append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
        for (PlayerScore ps : r.players) {
            sb.append(" ").append(ps.name).append(": ").append(ps.score).append(" πόντοι  ")
              .append("★".repeat(ps.stars)).append("☆".repeat(3 - ps.stars));
            if (ps.total > 0) sb.append("  (").append(ps.correct).append("/").append(ps.total).append(" σωστές)");
            sb.append("\n");
        }
        for (String line : r.lines) sb.append(" ").append(line).append("\n");
        if (r.challengeCode != null) {
            sb.append(" Κωδικός πρόκλησης: ").append(r.challengeCode).append("\n");
            sb.append(" Βάλε τον κωδικό στο AlgoQuest και δες\n αν μπορείς να με ξεπεράσεις!\n");
        }
        sb.append("════════════════════════════════════════\n");

        String text = sb.toString();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                   .setContents(new StringSelection(text), null);
        } catch (Exception ignored) { }

        String stamp = new SimpleDateFormat("HHmmss").format(new Date());
        Path file = Path.of("AlgoQuest_card_" + stamp + ".txt");
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
            status.setText("Η κάρτα αντιγράφηκε στο πρόχειρο και αποθηκεύτηκε στο αρχείο "
                    + file.toAbsolutePath().getFileName() + " — στείλ' τη στους φίλους σου!");
        } catch (IOException ex) {
            status.setText("Η κάρτα αντιγράφηκε στο πρόχειρο (αποτυχία εγγραφής αρχείου).");
        }
        SoundFx.correct();
    }
}
