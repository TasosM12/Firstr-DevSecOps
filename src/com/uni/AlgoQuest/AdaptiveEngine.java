package com.uni.AlgoQuest;

import java.util.*;

/**
 * Μηχανισμός ΠΡΟΣΑΡΜΟΣΤΙΚΗΣ ΔΥΣΚΟΛΙΑΣ.
 * Παρακολουθεί την απόδοση του παίκτη και ανεβοκατεβάζει το επίπεδο (1..3):
 *  - 3 συνεχόμενες σωστές  → ανεβαίνει επίπεδο
 *  - 2 συνεχόμενα λάθη     → κατεβαίνει επίπεδο
 * Από το επίπεδο εξαρτώνται: η δεξαμενή ερωτήσεων, ο χρόνος απάντησης,
 * το μέγεθος του πίνακα στο Sort Race και το εύρος στο Κυνήγι του Αριθμού.
 */
class AdaptiveEngine {

    private int level = 1;        // 1 = Εύκολο, 2 = Μέτριο, 3 = Δύσκολο
    private int hot = 0, cold = 0; // τρέχον σερί σωστών / λαθών

    int level() { return level; }

    String levelName() {
        return switch (level) { case 1 -> "Εύκολο"; case 2 -> "Μέτριο"; default -> "Δύσκολο"; };
    }

    /** Καταγράφει απάντηση και επιστρέφει μήνυμα προσαρμογής αν άλλαξε το επίπεδο (αλλιώς null). */
    String record(boolean ok) {
        if (ok) { hot++; cold = 0; } else { cold++; hot = 0; }
        if (ok && hot >= 3 && level < 3) {
            level++; hot = 0;
            return "▲ Η δυσκολία ανέβηκε σε «" + levelName() + "» — 3 σωστές στη σειρά, μπράβο!";
        }
        if (!ok && cold >= 2 && level > 1) {
            level--; cold = 0;
            return "▼ Η δυσκολία προσαρμόστηκε σε «" + levelName() + "» — συνέχισε, το έχεις!";
        }
        return null;
    }

    /** Διαθέσιμος χρόνος ανά ερώτηση κουίζ (δευτερόλεπτα). */
    int secondsForQuestion() {
        return switch (level) { case 1 -> 30; case 2 -> 22; default -> 15; };
    }

    /** Μέγεθος πίνακα για το Sort Race. */
    int arraySize() {
        return switch (level) { case 1 -> 6; case 2 -> 8; default -> 10; };
    }

    /** Εύρος αριθμών για το παιχνίδι δυαδικής αναζήτησης. */
    int searchRange() {
        return switch (level) { case 1 -> 50; case 2 -> 100; default -> 200; };
    }

    /** Επιλέγει επόμενη ερώτηση από τη δεξαμενή του τρέχοντος επιπέδου, χωρίς επαναλήψεις. */
    Question nextQuestion(Random rng, Set<Question> used) {
        List<Question> pool = new ArrayList<>();
        for (Question q : QuestionBank.ALL)
            if (q.difficulty == level && !used.contains(q)) pool.add(q);
        if (pool.isEmpty())                       // αν εξαντλήθηκε το επίπεδο, πάρε από όλες
            for (Question q : QuestionBank.ALL)
                if (!used.contains(q)) pool.add(q);
        if (pool.isEmpty()) { used.clear(); return nextQuestion(rng, used); }
        Question q = pool.get(rng.nextInt(pool.size()));
        used.add(q);
        return q;
    }
}
