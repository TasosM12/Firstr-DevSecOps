package com.uni.AlgoQuest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/** Κεντρικό παράθυρο του AlgoQuest: μενού, πλοήγηση οθονών, προφίλ, παράσημα, βαθμολογίες. */
public class GameFrame extends JFrame {

    final SaveData store = Store.load();
    final AdaptiveEngine engine = new AdaptiveEngine();   // κοινή προσαρμοστική δυσκολία
    Runnable replay = () -> { };                          // ενέργεια για το «Παίξε ξανά»
    private long seed = newSeed();

    private final JPanel root = new JPanel(new BorderLayout());
    private JTextField nameField, codeField;
    private JLabel chip;

    public GameFrame() {
        setTitle("AlgoQuest — Η Ακαδημία των Αλγορίθμων");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setMinimumSize(new Dimension(880, 620));
        setLocationRelativeTo(null);
        setIconImage(makeIcon());
        setContentPane(root);
        openMenu();
    }

    /** Εναλλαγή οθόνης. */
    void open(JPanel p) {
        root.removeAll();
        root.add(p);
        root.revalidate();
        root.repaint();
    }

    String playerName() {
        String s = nameField != null ? nameField.getText().trim() : "";
        return s.isEmpty() ? "Παίκτης" : s;
    }

    PlayerProfile profile(String name) {
        return store.profiles.computeIfAbsent(name, PlayerProfile::new);
    }

    private static long newSeed() { return Math.abs(new Random().nextLong() % 0xFFFFFFFL) + 1; }

    /**
     * RNG παρτίδας. Αν ο παίκτης έδωσε κωδικό πρόκλησης (π.χ. AQ-3F9A2), παράγεται
     * η ΙΔΙΑ ακολουθία ερωτήσεων/πινάκων — έτσι δύο φίλοι παίζουν την ίδια πρόκληση
     * και συγκρίνουν σκορ (διαμοιρασμός περιεχομένου).
     */
    Random sessionRng() {
        String c = codeField != null ? codeField.getText().trim().toUpperCase() : "";
        if (c.startsWith("AQ-")) c = c.substring(3);
        if (!c.isEmpty()) {
            try { seed = Long.parseLong(c, 36); }
            catch (NumberFormatException e) { seed = Math.abs(c.hashCode()) + 1; }
        } else {
            seed = newSeed();
        }
        return new Random(seed);
    }

    String challengeCode() { return "AQ-" + Long.toString(seed, 36).toUpperCase(); }

    // ───────────────────────────── Μενού ─────────────────────────────

    void openMenu() {
        JPanel s = Theme.screen(new GridBagLayout());
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel logo = Theme.title("AlgoQuest", 46);
        logo.setForeground(Theme.ACCENT);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = Theme.label("Η Ακαδημία των Αλγορίθμων — μάθε αλγορίθμους παίζοντας!", 17, Theme.MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        // Κάρτα παίκτη: όνομα + κωδικός πρόκλησης + προφίλ
        JPanel pcard = Theme.card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        nameField = Theme.field(14);
        nameField.setText("Παίκτης");
        codeField = Theme.field(10);
        codeField.setToolTipText("Βάλε εδώ κωδικό πρόκλησης φίλου (π.χ. AQ-3F9A2) για να παίξετε την ίδια παρτίδα");
        g.gridx = 0; g.gridy = 0; pcard.add(Theme.label("Όνομα παίκτη:", 15, Theme.TEXT), g);
        g.gridx = 1;              pcard.add(nameField, g);
        g.gridx = 2;              pcard.add(Theme.label("Κωδικός πρόκλησης (προαιρετικό):", 15, Theme.TEXT), g);
        g.gridx = 3;              pcard.add(codeField, g);
        chip = Theme.label(" ", 14, Theme.GOLD);
        g.gridx = 0; g.gridy = 1; g.gridwidth = 4; g.anchor = GridBagConstraints.CENTER;
        pcard.add(chip, g);
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { refreshChip(); }
            public void removeUpdate(DocumentEvent e)  { refreshChip(); }
            public void changedUpdate(DocumentEvent e) { refreshChip(); }
        });
        refreshChip();

        // Πλέγμα με τα παιχνίδια
        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.add(gameBtn("Κουίζ Αλγορίθμων",
                "10 ερωτήσεις με προσαρμοστική δυσκολία, χρονόμετρο, σερί και βοήθεια 50-50",
                () -> startQuiz(false)));
        grid.add(gameBtn("Versus για 2 παίκτες",
                "Παίξτε εναλλάξ στις ίδιες ερωτήσεις — ποιος ξέρει καλύτερα τους αλγορίθμους;",
                () -> startQuiz(true)));
        grid.add(gameBtn("Sort Race",
                "Ταξινόμησε τις στήλες με γειτονικές αντιμεταβολές, όπως ο Bubble Sort — με τις λιγότερες κινήσεις!",
                this::startSort));
        grid.add(gameBtn("Το Κυνήγι του Αριθμού",
                "Ζήσε τη δυαδική αναζήτηση: βρες τον κρυφό αριθμό με τις λιγότερες ερωτήσεις",
                this::startSearch));

        // Κάτω σειρά: βαθμολογίες, οδηγίες, ήχος
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);
        JButton lb = Theme.ghost("Πίνακας Βαθμολογιών");
        lb.addActionListener(e -> { SoundFx.click(); openLeaderboard(); });
        JButton how = Theme.ghost("Πώς παίζεται;");
        how.addActionListener(e -> showHelp());
        JCheckBox snd = new JCheckBox("Ήχοι", true);
        snd.setOpaque(false);
        snd.setForeground(Theme.MUTED);
        snd.setFont(Theme.font(Font.PLAIN, 14));
        snd.setFocusPainted(false);
        snd.addActionListener(e -> SoundFx.enabled = snd.isSelected());
        row.add(lb); row.add(how); row.add(snd);

        col.add(logo);
        col.add(Box.createVerticalStrut(4));
        col.add(sub);
        col.add(Box.createVerticalStrut(22));
        pcard.setAlignmentX(CENTER_ALIGNMENT);
        col.add(pcard);
        col.add(Box.createVerticalStrut(18));
        grid.setAlignmentX(CENTER_ALIGNMENT);
        grid.setMaximumSize(new Dimension(820, 230));
        col.add(grid);
        col.add(Box.createVerticalStrut(18));
        row.setAlignmentX(CENTER_ALIGNMENT);
        col.add(row);

        s.add(col, new GridBagConstraints());
        open(s);
    }

    /** Ενημερώνει το «τσιπάκι» προφίλ κάτω από το όνομα (XP, επίπεδο, παράσημα). */
    private void refreshChip() {
        PlayerProfile p = store.profiles.get(playerName());
        if (p == null) {
            chip.setText("Νέος εξερευνητής — ξεκίνα την πρώτη σου αποστολή!");
            chip.setForeground(Theme.MUTED);
        } else {
            chip.setText("Επίπεδο " + p.level() + "  •  " + p.xp + " XP (" + p.levelProgress()
                    + "% προς το επόμενο)  •  " + p.badges.size() + " παράσημα  •  Δυσκολία: " + engine.levelName());
            chip.setForeground(Theme.GOLD);
        }
    }

    private JButton gameBtn(String title, String desc, Runnable go) {
        JButton b = Theme.button("<html><div style='width:310px'>"
                + "<span style='font-size:15px;color:#38BDF8'><b>◆ " + title + "</b></span><br>"
                + "<span style='font-size:11px;color:#93A4BD'>" + desc + "</span></div></html>",
                Theme.CARD, Theme.TEXT, 15);
        b.addActionListener(e -> { SoundFx.click(); go.run(); });
        return b;
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this, """
                ΚΟΥΙΖ: Απάντησε σωστά πριν τελειώσει ο χρόνος. Κερδίζεις πόντους για ταχύτητα και σερί.
                Κάθε 3 σωστές κερδίζεις βοήθεια 50-50. Η δυσκολία προσαρμόζεται στην απόδοσή σου!

                SORT RACE: Κάνε κλικ σε δύο ΓΕΙΤΟΝΙΚΕΣ στήλες για να τις ανταλλάξεις (σαν τον Bubble Sort).
                Στόχος: αύξουσα σειρά με τις λιγότερες δυνατές αντιμεταβολές.

                ΤΟ ΚΥΝΗΓΙ ΤΟΥ ΑΡΙΘΜΟΥ: Βρες τον κρυφό αριθμό. Μετά από κάθε προσπάθεια μαθαίνεις
                αν είναι μεγαλύτερος ή μικρότερος. Σκέψου σαν τη δυαδική αναζήτηση: ρώτα τη μέση!

                VERSUS: Δύο παίκτες απαντούν εναλλάξ στις ίδιες ερωτήσεις. Νικά το μεγαλύτερο σκορ.

                ΠΡΟΚΛΗΣΕΙΣ: Κάθε παρτίδα έχει κωδικό (π.χ. AQ-3F9A2). Δώσ' τον σε φίλο για να παίξει
                την ίδια παρτίδα και συγκρίνετε σκορ!""",
                "Πώς παίζεται το AlgoQuest", JOptionPane.INFORMATION_MESSAGE);
    }

    // ─────────────────────── Εκκίνηση παιχνιδιών ───────────────────────

    void startQuiz(boolean versus) {
        String[] names;
        if (versus) {
            String p2 = (String) JOptionPane.showInputDialog(this, "Όνομα 2ου παίκτη:",
                    "Versus", JOptionPane.QUESTION_MESSAGE, null, null, "Παίκτης 2");
            if (p2 == null) return;
            p2 = p2.trim().isEmpty() ? "Παίκτης 2" : p2.trim();
            names = new String[]{ playerName(), p2 };
        } else {
            names = new String[]{ playerName() };
        }
        replay = () -> startQuiz(versus);
        open(new QuizPanel(this, names));
    }

    void startSort()   { replay = this::startSort;   open(new SortRacePanel(this, playerName())); }
    void startSearch() { replay = this::startSearch; open(new BinarySearchPanel(this, playerName())); }

    // ─────────────────────── Τέλος παρτίδας ───────────────────────

    /** Κλείσιμο παρτίδας: XP, νέα παράσημα, εγγραφή στις βαθμολογίες, αποθήκευση, οθόνη αποτελεσμάτων. */
    void finish(SessionResult r) {
        List<String> freshBadges = new ArrayList<>();
        for (PlayerScore ps : r.players) {
            PlayerProfile pr = profile(ps.name);
            pr.xp += ps.score;
            pr.gamesPlayed++;
            pr.bestStreak = Math.max(pr.bestStreak, ps.bestStreak);
            if (pr.gamesPlayed == 1) ps.badges.add("Πρώτα Βήματα");
            if (ps.bestStreak >= 5)  ps.badges.add("Σερί ×5");
            for (String b : ps.badges)
                if (pr.badges.add(b)) freshBadges.add("★ " + b + "  (" + ps.name + ")");
            store.scores.add(new ScoreEntry(ps.name, r.game, ps.score));
        }
        store.scores.sort((a, b) -> b.score - a.score);
        if (store.scores.size() > 50) store.scores.subList(50, store.scores.size()).clear();
        Store.save(store);
        open(new ResultsPanel(this, r, freshBadges));
    }

    // ─────────────────────── Πίνακας βαθμολογιών ───────────────────────

    void openLeaderboard() {
        JPanel s = Theme.screen(new GridBagLayout());
        JPanel card = Theme.card(new BorderLayout(0, 14));

        JLabel t = Theme.title("Πίνακας Βαθμολογιών", 26);
        t.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(t, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(0, 1, 0, 6));
        list.setOpaque(false);
        if (store.scores.isEmpty()) {
            list.add(Theme.label("Δεν υπάρχουν ακόμη σκορ — παίξε την πρώτη σου παρτίδα!", 15, Theme.MUTED));
        }
        Color[] medal = { Theme.GOLD, new Color(0xC0C7D0), new Color(0xCD8A4B) };
        for (int i = 0; i < Math.min(12, store.scores.size()); i++) {
            ScoreEntry e = store.scores.get(i);
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            JLabel rank = Theme.label((i + 1) + ".", 16, i < 3 ? medal[i] : Theme.MUTED);
            rank.setFont(Theme.font(Font.BOLD, 16));
            rank.setPreferredSize(new Dimension(34, 22));
            JLabel who = Theme.label(e.name + "   —   " + e.game, 15, Theme.TEXT);
            JLabel pts = Theme.label(e.score + " π.   " + e.date, 14, i < 3 ? medal[i] : Theme.MUTED);
            row.add(rank, BorderLayout.WEST);
            row.add(who, BorderLayout.CENTER);
            row.add(pts, BorderLayout.EAST);
            list.add(row);
        }
        card.add(list, BorderLayout.CENTER);

        JButton back = Theme.primary("Πίσω στο μενού");
        back.addActionListener(e -> { SoundFx.click(); openMenu(); });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(back);
        card.add(south, BorderLayout.SOUTH);

        card.setPreferredSize(new Dimension(660, 540));
        s.add(card, new GridBagConstraints());
        open(s);
    }

    /** Προγραμματικά σχεδιασμένο εικονίδιο παραθύρου («A» σε γαλάζιο πλακίδιο). */
    private static Image makeIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.ACCENT);
        g.fillRoundRect(2, 2, 60, 60, 18, 18);
        g.setColor(new Color(0x0B1626));
        g.setFont(new Font("Segoe UI", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("A", (64 - fm.stringWidth("A")) / 2, 46);
        g.dispose();
        return img;
    }
}
