package aqua.blatt1.broker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int DEFAULT_CLIENTS = 3;
    private static final long CLIENT_START_DELAY_MS = 500;

    /**
     * Startet erst den Broker und danach eine variable Anzahl an Clients.
     * Anzahl der Clients als erstes Argument, z. B. "5" (Standard: 3).
     * Jeder Client läuft als eigener Prozess, damit das Schließen eines
     * Client-Fensters (System.exit) nicht den Broker beendet.
     */
    public static void main(String[] args) {
        int clientCount = parseClientCount(args);

        // 1. Broker in eigenem Thread starten
        Broker broker = new Broker();
        Thread brokerThread = new Thread(broker::broker, "Broker");
        brokerThread.start();

        // 2. Clients als eigene Prozesse starten
        List<Process> clients = new ArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> clients.forEach(Process::destroy)));

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        for (int i = 1; i <= clientCount; i++) {
            try {
                // kurz warten, damit der Broker bereit ist und die Clients nacheinander registriert werden
                Thread.sleep(CLIENT_START_DELAY_MS);
                Process client = new ProcessBuilder(javaBin, "-cp", classpath, "aqua.blatt1.client.Aqualife")
                        .inheritIO()
                        .start();
                clients.add(client);
                System.out.println("Client " + i + " von " + clientCount + " gestartet.");
            } catch (IOException e) {
                System.err.println("Client " + i + " konnte nicht gestartet werden: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 3. Warten, bis der Broker gestoppt wird ("OK"-Dialog), dann alle Clients beenden
        try {
            brokerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clients.forEach(Process::destroy);
        System.exit(0);
    }

    private static int parseClientCount(String[] args) {
        if (args.length > 0) {
            try {
                int n = Integer.parseInt(args[0]);
                if (n >= 0) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
                // fällt auf den Standard zurück
            }
            System.err.println("Ungültige Client-Anzahl \"" + args[0] + "\", verwende " + DEFAULT_CLIENTS + ".");
        }
        return DEFAULT_CLIENTS;
    }
}
