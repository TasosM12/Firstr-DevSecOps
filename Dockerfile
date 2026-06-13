# AlgoQuest — εκπαιδευτικό παιχνίδι σε Java (Swing)
# Σταθερό JDK 17 base image. Αν ο Docker Scout δεν βρει vulnerabilities,
# κλείδωσε ένα παλιότερο tag (π.χ. eclipse-temurin:17.0.7_7-jdk-jammy).
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Αντιγραφή του πηγαίου κώδικα
COPY src ./src

# Μεταγλώττιση — το -encoding UTF-8 χρειάζεται για τα ελληνικά κείμενα του κώδικα
RUN javac -encoding UTF-8 -d out src/com/uni/AlgoQuest/*.java

# Εκκίνηση της εφαρμογής (GUI app — απαιτεί οθόνη για να εμφανιστεί παράθυρο).
# Δεν επηρεάζει το build ή το scan του Docker Scout.
CMD ["java", "-cp", "out", "com.uni.AlgoQuest.Main"]
