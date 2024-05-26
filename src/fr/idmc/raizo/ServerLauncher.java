package fr.idmc.raizo;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerLauncher {
    // Port utilisé par le serveur pour écouter les connexions entrantes
    private static final int PORT = 1337;
    // Mot de passe utilisé pour l'authentification des workers
    private static final String PASSWORD = "azerty";
    // Liste des workers connectés au serveur
    private List<WorkerHandler> workers = new ArrayList<>();
    // Socket serveur pour accepter les connexions
    private ServerSocket serverSocket;
    // Indique si le serveur est en cours d'exécution
    private boolean running = true;

    public static void main(String[] args) throws Exception {
        ServerLauncher serverLauncher = new ServerLauncher();
        
        // Thread pour lancer le serveur
        Thread serverThread = new Thread(() -> {
            try {
                serverLauncher.runServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        
        // Thread pour gérer les commandes utilisateur
        Thread inputThread = new Thread(() -> {
            try {
                serverLauncher.handleCommands();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        inputThread.start();
        
        // Attendre la fin du thread du serveur
        serverThread.join();
    }

    public void runServer() throws Exception {
        serverSocket = new ServerSocket(PORT);
        while (running) {
            // Accepter une nouvelle connexion
            Socket socket = serverSocket.accept();
            WorkerHandler workerHandler = new WorkerHandler(socket, this);
            workers.add(workerHandler); // Ajouter le worker à la liste
            Thread workerThread = new Thread(workerHandler);
            workerThread.start(); // Démarrer un nouveau thread pour gérer le worker
        }
    }

    public void handleCommands() throws Exception {
        final Console console = System.console();
        if (console == null) {
            System.err.println("No console available");
            return;
        }
        boolean keepGoing = true;
        while (keepGoing) {
            final String command = console.readLine("$ ");
            if (command == null) break;
            keepGoing = processCommand(command.trim()); // Traiter la commande utilisateur
        }
    }

    protected boolean processCommand(String cmd) throws Exception {
        if ("quit".equals(cmd)) {
            // Fermer tous les sockets des workers et arrêter le serveur
            for (WorkerHandler worker : workers) {
                worker.sendMessage("CLOSING SOCKET");
                worker.getSocket().close();
            }
            running = false;
            System.exit(0);
        } else if ("cancel".equals(cmd)) {
            // Annuler les tâches de minage pour les workers authentifiés
            for (WorkerHandler worker : workers) {
                if ("Authenticated".equals(worker.getStatus())) {
                    worker.setMining(false);
                    worker.sendMessage("CANCELLED");
                    worker.setStatus("Connected");
                }
            }
        } else if ("status".equals(cmd)) {
            // Afficher le statut de tous les workers
            System.out.println("Workers status:");
            for (WorkerHandler worker : workers) {
                System.out.println(worker.getStatus());
            }
        } else if ("progress".equals(cmd)) {
            // Demander la progression de chaque worker
            for (WorkerHandler worker : workers) {
                worker.sendMessage("PROGRESS");
                if (worker.isMining()) {
                    String progressMessage = "TESTING " + Long.toHexString(worker.getCurrentNonce()).toUpperCase();
                    worker.sendMessage(progressMessage);
                } else {
                    worker.sendMessage("NOPE");
                }
            }
        } else if ("help".equals(cmd.trim())) {
            // Afficher l'aide sur les commandes disponibles
            System.out.println(" • status - display information about connected workers");
            System.out.println(" • solve <d> - try to mine with given difficulty");
            System.out.println(" • progress - see what each worker is testing or not");
            System.out.println(" • cancel - cancel a task");
            System.out.println(" • help - describe available commands");
            System.out.println(" • quit - terminate pending work and quit");
        } else if (cmd.startsWith("solve")) {
            // Démarrer une tâche de minage avec la difficulté spécifiée
            String[] parts = cmd.split(" ");
            if (parts.length == 2 && parts[1].matches("\\d+")) {
                int difficulty = Integer.parseInt(parts[1]);
                if (difficulty > 32 || difficulty < 1) {
                    System.out.println("difficulty must be between 1 and 32 !");
                } else {
                    // Générer le payload pour la tâche de minage
                    String payload = WebApp.generateWork(difficulty);
                    if (payload.startsWith("Difficulty")) {
                        broadcastAuthenticated(payload);
                    } else {
                        int counter = 0;
                        for (WorkerHandler worker : workers) {
                            int increment = workers.size();
                            final int currentCounter = counter;
                            if ("Authenticated".equals(worker.getStatus())) {
                                // Démarrer un thread pour chaque worker pour la tâche de minage
                                Thread thread = new Thread(() -> {
                                    worker.sendMessage("NONCE " + currentCounter + " " + increment);
                                    worker.sendMessage("PAYLOAD " + payload);
                                    worker.sendMessage("SOLVE " + difficulty);
                                    worker.mine(currentCounter, increment, "Authenticated".equals(worker.getStatus()), difficulty, payload);
                                });
                                thread.start();
                            }
                            counter++;
                        }
                    }
                }
            } else {
                System.out.println("Usage: solve <difficulty>");
            }
        } else {
            // Commande non reconnue
            System.out.println("Cette commande n'est pas disponible! Tapez une parmi les suivantes :");
            System.out.println(" • status - display information about connected workers");
            System.out.println(" • solve <d> - try to mine with given difficulty");
            System.out.println(" • progress - see what each worker is testing or not");
            System.out.println(" • cancel - cancel a task");
            System.out.println(" • help - describe available commands");
            System.out.println(" • quit - terminate pending work and quit");
        }

        return true;
    }

    // Diffuser un message à tous les workers
    public synchronized void broadcast(String message) {
        for (WorkerHandler worker : workers) {
            worker.sendMessage(message);
        }
    }
    
    // Diffuser un message à tous les workers authentifiés
    public synchronized void broadcastAuthenticated(String message) {
        for (WorkerHandler worker : workers) {
            if ("Authenticated".equals(worker.getStatus())) {
                worker.sendMessage(message);
            }
        }
    }

    // Mettre à jour le statut des workers authentifiés vers "Connected"
    public synchronized void broadcastToConnect() {
        for (WorkerHandler worker : workers) {
            if ("Authenticated".equals(worker.getStatus())) {
                worker.setStatus("Connected");
            }
        }
    }

    // Retirer un worker de la liste des workers connectés
    public synchronized void removeWorker(WorkerHandler worker) {
        workers.remove(worker);
    }

    // Retourne le mot de passe pour l'authentification
    public static String getPassword() {
        return PASSWORD;
    }
}
