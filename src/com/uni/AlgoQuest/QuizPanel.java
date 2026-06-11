package com.uni.AlgoQuest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Κουίζ πολλαπλής επιλογής με προσαρμοστική δυσκολία, χρονόμετρο, σερί,
 * βοήθεια 50-50 και άμεση ανατροφοδότηση με εξήγηση.
 * Υποστηρίζει 1 παίκτη ή Versus 2 παικτών (απαντούν εναλλάξ στις ίδιες ερωτήσεις).
 */
class QuizPanel extends JPanel {

    private final GameFrame frame;
    private final String[] names;
    private final AdaptiveEngine[] eng;
    private final int[] score, correctCnt, streak, best, fifty;
    private final boolean versus;
    private final int totalQ;
    private final Random rng;
    private final Set<Question> used = new HashSet<>();

    private int qNo = 0, turn = 0, timeLeft, correctBtn;
    private boolean answered;
    private Question cur;
    private final javax.swing.Timer clock;

    // UI
    private final JLabel turnLbl, scoreLbl, diffLbl, streakLbl, timeLbl, headLbl;
    private final JLabel qLbl;
    private final JTextArea explainArea;
    private final TimeBar timeBar = new TimeBar();
    private final JButton[] opts = new JButton[4];
    private final JButton nextBtn, fiftyBtn;

    QuizPanel(GameFrame frame, String[] names) {
        this.frame = frame;
        this.names = names;
        this.versus = names.length == 2;
        this.totalQ = versus ? 12 : 10;
        this.rng = frame.sessionRng();

        // Ο 1ος παίκτης συνεχίζει την «κοινή» δυσκολία του frame, ο 2ος ξεκινά δική του.
        eng = versus ? new AdaptiveEngine[]{ frame.engine, new AdaptiveEngine() }
                     : new AdaptiveEngine[]{ frame.engine };
        int n = names.length;
        score = new int[n]; correctCnt = new int[n]; streak = new int[n]; best = new int[n];
        fifty = new int[n];
        Arrays.fill(fifty, 1);   // όλοι ξεκινούν με μία βοήθεια 50-50

        setLayout(new BorderLayout());
        JPanel s = Theme.screen(new BorderLayout(0, 10));
        s.setBorder(new EmptyBorder(14, 22, 14, 22));
        add(s);

        // ── Πάνω μπάρα: σειρά/πρόοδος, δυσκολία, σερί, σκορ, χρόνος, εγκατάλειψη
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        turnLbl = Theme.title(" ", 18);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        diffLbl   = Theme.label(" ", 14, Theme.ACCENT);
        streakLbl = Theme.label(" ", 14, Theme.GOLD);
        scoreLbl  = Theme.label(" ", 15, Theme.TEXT);
        timeLbl   = Theme.label(" ", 15, Theme.TEXT);
        JButton quit = Theme.ghost("✕ Μενού");
        quit.addActionListener(e -> { stop(); frame.openMenu(); });
        right.add(diffLbl); right.add(streakLbl); right.add(scoreLbl); right.add(timeLbl); right.add(quit);
        top.add(turnLbl, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JPanel topWrap = new JPanel(new BorderLayout(0, 8));
        topWrap.setOpaque(false);
        topWrap.add(top, BorderLayout.NORTH);
        timeBar.setPreferredSize(new Dimension(100, 10));
        topWrap.add(timeBar, BorderLayout.SOUTH);
        s.add(topWrap, BorderLayout.NORTH);

        // ── Κέντρο: ερώτηση + 4 επιλογές
        JPanel center = Theme.card(new BorderLayout(0, 14));
        qLbl = Theme.title(" ", 19);
        qLbl.setBorder(new EmptyBorder(4, 6, 4, 6));
        center.add(qLbl, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            opts[i] = Theme.button(" ", Theme.FIELD, Theme.TEXT, 15);
            opts[i].setHorizontalAlignment(SwingConstants.LEFT);
            opts[i].addActionListener(e -> answer(idx));
            grid.add(opts[i]);
        }
        center.add(grid, BorderLayout.CENTER);
        s.add(center, BorderLayout.CENTER);

        // ── Κάτω: ανατροφοδότηση + εξήγηση + κουμπιά
        JPanel bottom = Theme.card(new BorderLayout(0, 6));
        headLbl = Theme.title(" ", 16);
        explainArea = Theme.area(14, Theme.MUTED);
        explainArea.setRows(3);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        fiftyBtn = Theme.gold("Βοήθεια 50-50");
        fiftyBtn.addActionListener(e -> useFifty());
        nextBtn = Theme.primary("Επόμενη ▶");
        nextBtn.addActionListener(e -> { SoundFx.click(); next(); });
        btns.add(fiftyBtn); btns.add(nextBtn);
        bottom.add(headLbl, BorderLayout.NORTH);
        bottom.add(explainArea, BorderLayout.CENTER);
        bottom.add(btns, BorderLayout.SOUTH);
        s.add(bottom, BorderLayout.SOUTH);

        clock = new javax.swing.Timer(1000, e -> tick());
        nextQuestion();
    }

    private void stop() { clock.stop(); }

    // ───────────────────────── Ροή παιχνιδιού ─────────────────────────

    private void nextQuestion() {
        answered = false;
        cur = eng[turn].nextQuestion(rng, used);
        qNo++;

        // Ανακάτεμα των επιλογών ώστε η σωστή να μην είναι πάντα στην ίδια θέση
        List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(order, rng);
        correctBtn = order.indexOf(cur.correct);
        for (int i = 0; i < 4; i++) {
            opts[i].setText("<html><div style='width:330px'>" + (char) ('Α' + i) + ".  "
                    + cur.options[order.get(i)] + "</div></html>");
            opts[i].putClientProperty("bg", Theme.FIELD);
            opts[i].putClientProperty("dead", null);
            opts[i].setForeground(Theme.TEXT);
            opts[i].repaint();
        }

        qLbl.setText("<html><div style='width:760px'>" + qNo + ". " + cur.text + "</div></html>");
        headLbl.setText("Θέμα: " + cur.topic + "  •  Επίπεδο: " + eng[turn].levelName());
        headLbl.setForeground(Theme.MUTED);
        explainArea.setText("Σκέψου καλά… Περισσότεροι πόντοι για γρήγορη απάντηση και σερί σωστών!");

        timeLeft = eng[turn].secondsForQuestion();
        updateHud();
        nextBtn.setEnabled(false);
        clock.restart();
    }

    private void updateHud() {
        String prog = "Ερώτηση " + qNo + "/" + totalQ;
        turnLbl.setText(versus ? "Σειρά: " + names[turn] + "  —  " + prog : names[0] + "  —  " + prog);
        diffLbl.setText("Δυσκολία: " + eng[turn].levelName());
        streakLbl.setText(streak[turn] > 1 ? "Σερί ×" + streak[turn] + "!" : " ");
        scoreLbl.setText(versus
                ? names[0] + " " + score[0] + "  |  " + names[1] + " " + score[1]
                : "Σκορ: " + score[0]);
        timeLbl.setText(timeLeft + "s");
        timeLbl.setForeground(timeLeft <= 5 ? Theme.ERR : Theme.TEXT);
        timeBar.set(timeLeft / (double) eng[turn].secondsForQuestion(),
                timeLeft <= 5 ? Theme.ERR : Theme.ACCENT);
        fiftyBtn.setText("Βοήθεια 50-50 (×" + fifty[turn] + ")");
        fiftyBtn.setEnabled(!answered && fifty[turn] > 0);
    }

    private void tick() {
        timeLeft--;
        if (timeLeft <= 0) { answer(-1); return; }
        updateHud();
    }

    /** Απάντηση του παίκτη (chosen = -1 σημαίνει «τελείωσε ο χρόνος»). */
    private void answer(int chosen) {
        if (answered) return;
        if (chosen >= 0 && opts[chosen].getClientProperty("dead") != null) return; // κομμένη από 50-50
        answered = true;
        clock.stop();

        // Χρωματισμός: πράσινη η σωστή, κόκκινη η λάθος επιλογή του παίκτη
        opts[correctBtn].putClientProperty("bg", Theme.OK);
        opts[correctBtn].setForeground(Color.BLACK);
        if (chosen >= 0 && chosen != correctBtn) {
            opts[chosen].putClientProperty("bg", Theme.ERR);
            opts[chosen].setForeground(Color.BLACK);
        }
        for (JButton b : opts) b.repaint();

        boolean ok = chosen == correctBtn;
        StringBuilder fb = new StringBuilder();
        if (ok) {
            streak[turn]++;
            correctCnt[turn]++;
            best[turn] = Math.max(best[turn], streak[turn]);
            int base = 10 * cur.difficulty;
            int timeBonus = Math.min(timeLeft, 15);
            int streakBonus = Math.min(streak[turn] * 2, 10);
            int pts = base + timeBonus + streakBonus;
            score[turn] += pts;
            headLbl.setText("✓ Σωστά!  +" + pts + " πόντοι (βάση " + base
                    + " + ταχύτητα " + timeBonus + " + σερί " + streakBonus + ")");
            headLbl.setForeground(Theme.OK);
            SoundFx.correct();
            if (correctCnt[turn] % 3 == 0 && fifty[turn] < 3) {
                fifty[turn]++;
                fb.append("Αμοιβή: κέρδισες μία βοήθεια 50-50!\n");
            }
        } else {
            streak[turn] = 0;
            headLbl.setText(chosen < 0 ? "⏱ Τέλος χρόνου! Η σωστή απάντηση φαίνεται με πράσινο."
                                       : "✗ Λάθος… Η σωστή απάντηση φαίνεται με πράσινο.");
            headLbl.setForeground(Theme.ERR);
            SoundFx.wrong();
        }

        String adapt = eng[turn].record(ok);            // προσαρμογή δυσκολίας
        fb.append("Εξήγηση: ").append(cur.explain);
        if (adapt != null) fb.append("\n").append(adapt);
        explainArea.setText(fb.toString());

        nextBtn.setText(qNo == totalQ ? "Αποτελέσματα ★" : "Επόμενη ▶");
        nextBtn.setEnabled(true);
        updateHud();
        fiftyBtn.setEnabled(false);
    }

    private void next() {
        if (qNo >= totalQ) { finishGame(); return; }
        turn = qNo % names.length;     // εναλλαγή παικτών στο Versus
        nextQuestion();
    }

    /** Βοήθεια 50-50: «σβήνει» δύο λάθος επιλογές. */
    private void useFifty() {
        if (answered || fifty[turn] <= 0) return;
        fifty[turn]--;
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < 4; i++) if (i != correctBtn) wrong.add(i);
        Collections.shuffle(wrong, rng);
        for (int k = 0; k < 2; k++) {
            JButton b = opts[wrong.get(k)];
            b.putClientProperty("bg", Theme.BG2);
            b.putClientProperty("dead", Boolean.TRUE);
            b.setForeground(Theme.MUTED.darker());
            b.repaint();
        }
        SoundFx.click();
        updateHud();
    }

    private void finishGame() {
        stop();
        String gameName = versus ? "Κουίζ Versus" : "Κουίζ Αλγορίθμων";
        SessionResult r = new SessionResult(gameName);
        r.challengeCode = frame.challengeCode();
        int perPlayer = totalQ / names.length;

        for (int i = 0; i < names.length; i++) {
            PlayerScore ps = new PlayerScore(names[i]);
            ps.score = score[i];
            ps.correct = correctCnt[i];
            ps.total = perPlayer;
            ps.bestStreak = best[i];
            int pct = correctCnt[i] * 100 / perPlayer;
            ps.stars = pct >= 80 ? 3 : pct >= 50 ? 2 : 1;
            if (correctCnt[i] == perPlayer) ps.badges.add("Αλάνθαστος");
            r.players.add(ps);
            r.lines.add(names[i] + ": " + correctCnt[i] + "/" + perPlayer
                    + " σωστές • καλύτερο σερί " + best[i]);
        }
        if (versus) {
            if (score[0] != score[1]) {
                int w = score[0] > score[1] ? 0 : 1;
                r.players.get(w).badges.add("Νικητής Versus");
                r.lines.add("Νικητής: " + names[w] + "!");
            } else {
                r.lines.add("Ισοπαλία — ρεβάνς;");
            }
        }
        SoundFx.win();
        frame.finish(r);
    }

    /** Λεπτή, custom-σχεδιασμένη μπάρα χρόνου (θεματική, αντί για JProgressBar). */
    private static class TimeBar extends JPanel {
        private double frac = 1;
        private Color color = Theme.ACCENT;

        TimeBar() { setOpaque(false); }

        void set(double f, Color c) { frac = Math.max(0, Math.min(1, f)); color = c; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, (int) (getWidth() * frac), getHeight(), 10, 10);
            g2.dispose();
        }
    }
}
