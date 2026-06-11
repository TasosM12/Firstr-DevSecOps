package com.uni.AlgoQuest;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Απλά ηχητικά εφέ ανατροφοδότησης (συνθετικοί τόνοι) — χωρίς εξωτερικά αρχεία ήχου. */
final class SoundFx {

    static volatile boolean enabled = true;

    private SoundFx() { }

    /** Παίζει έναν ημιτονοειδή τόνο σε ξεχωριστό νήμα ώστε να μην «παγώνει» το UI. */
    private static void tone(double freq, int ms, double vol) {
        if (!enabled) return;
        new Thread(() -> {
            try {
                AudioFormat af = new AudioFormat(44100f, 8, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(af);
                line.open(af);
                line.start();
                byte[] buf = new byte[(int) (44100 * ms / 1000.0)];
                for (int i = 0; i < buf.length; i++) {
                    double t = i / 44100.0;
                    double envelope = Math.min(1, (buf.length - i) / 900.0); // απαλό σβήσιμο στο τέλος
                    buf[i] = (byte) (Math.sin(2 * Math.PI * freq * t) * 55 * vol * envelope);
                }
                line.write(buf, 0, buf.length);
                line.drain();
                line.close();
            } catch (Exception ignored) { /* αν δεν υπάρχει κάρτα ήχου, απλώς σιωπή */ }
        }, "algoquest-sfx").start();
    }

    static void click()   { tone(700, 45, 0.5); }
    static void swap()    { tone(520, 45, 0.4); }
    static void correct() { tone(880, 90, 0.7); tone(1318, 150, 0.6); }
    static void wrong()   { tone(196, 230, 0.8); }
    static void win()     { tone(659, 130, 0.7); tone(880, 170, 0.7); tone(1109, 260, 0.7); }
}
