package com.uni.AlgoQuest;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Μοντέλα δεδομένων του παιχνιδιού + μόνιμη αποθήκευση με serialization (.ser). */

/** Μία ερώτηση πολλαπλής επιλογής με εξήγηση και επίπεδο δυσκολίας. */
class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    final int difficulty;            // 1 = Εύκολο, 2 = Μέτριο, 3 = Δύσκολο
    final String topic, text, explain;
    final String[] options;
    final int correct;               // δείκτης σωστής απάντησης στο options

    Question(int difficulty, String topic, String text, String explain, int correct, String... options) {
        this.difficulty = difficulty;
        this.topic = topic;
        this.text = text;
        this.explain = explain;
        this.correct = correct;
        this.options = options;
    }
}

/** Προφίλ παίκτη: XP, επίπεδο και παράσημα — διατηρείται μεταξύ εκτελέσεων. */
class PlayerProfile implements Serializable {
    private static final long serialVersionUID = 1L;
    final String name;
    int xp, gamesPlayed, bestStreak;
    final LinkedHashSet<String> badges = new LinkedHashSet<>();

    PlayerProfile(String name) { this.name = name; }

    int level()         { return 1 + xp / 150; }
    int levelProgress() { return (xp % 150) * 100 / 150; }   // % προόδου προς το επόμενο επίπεδο
}

/** Μία εγγραφή στον πίνακα βαθμολογιών. */
class ScoreEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    final String name, game, date;
    final int score;

    ScoreEntry(String name, String game, int score) {
        this.name = name;
        this.game = game;
        this.score = score;
        this.date = new SimpleDateFormat("dd/MM/yy HH:mm").format(new Date());
    }
}

/** Όλα τα αποθηκεύσιμα δεδομένα της εφαρμογής. */
class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;
    final Map<String, PlayerProfile> profiles = new HashMap<>();
    final List<ScoreEntry> scores = new ArrayList<>();
}

/** Σκορ ενός παίκτη για μία παρτίδα (για την οθόνη αποτελεσμάτων). */
class PlayerScore {
    final String name;
    int score, correct, total, bestStreak, stars;
    final List<String> badges = new ArrayList<>();   // υποψήφια παράσημα από την παρτίδα

    PlayerScore(String name) { this.name = name; }
}

/** Συνολικό αποτέλεσμα μίας παρτίδας (1 ή 2 παίκτες). */
class SessionResult {
    final String game;
    String challengeCode;                              // κωδικός πρόκλησης για διαμοιρασμό
    final List<PlayerScore> players = new ArrayList<>();
    final List<String> lines = new ArrayList<>();      // επιπλέον λεπτομέρειες παρτίδας

    SessionResult(String game) { this.game = game; }
}

/** Φόρτωση/αποθήκευση δεδομένων σε αρχείο .ser στον φάκελο εκτέλεσης. */
final class Store {
    private static final File FILE = new File("algoquest_data.ser");

    private Store() { }

    static SaveData load() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
            return (SaveData) in.readObject();
        } catch (Exception e) {
            return new SaveData();   // πρώτη εκτέλεση ή κατεστραμμένο αρχείο — καθαρή αρχή
        }
    }

    static void save(SaveData data) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(data);
        } catch (IOException ignored) { }
    }
}
