package fr.idmc.raizo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class WorkerHandler implements Runnable {
    private Socket socket;
    private ServerLauncher server;
    private PrintWriter out;
    private BufferedReader in;
    private String status;
    private boolean isMining = false;
    private long currentNonce = 0;

    // Initialisation de WorkerHandler avec le socket de communication et la référence au serveur
    public WorkerHandler(Socket socket, ServerLauncher server) {
        this.socket = socket;
        this.server = server;
        this.status = "Connected"; // État initial du worker
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(getSocket().getInputStream()));
            out = new PrintWriter(getSocket().getOutputStream(), true);

            // Authentification initiale du worker
            sendMessage("WHO_ARE_YOU_?");
            String response = in.readLine();
            if ("ITS_ME".equals(response)) {
                // Demande de mot de passe si la première réponse est correcte
                sendMessage("GIMME_PASSWORD");
                response = in.readLine();
                if (("PASSWD " + ServerLauncher.getPassword()).equals(response)) {
                    sendMessage("HELLO_YOU");
                    handleCommands(); // Gestion des commandes après authentification
                } else {
                    sendMessage("YOU_DONT_FOOL_ME"); // Mauvais mot de passe
                    getSocket().close();
                }
            } else {
                getSocket().close(); // Fermeture du socket si la première réponse est incorrecte
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.removeWorker(this); // Nettoyage et suppression du worker du serveur
        }
    }
    
    // Gérer les commandes envoyées par le worker
    private void handleCommands() throws IOException {
        while (true) {
            String command = in.readLine();
            if (command == null) break;

            // Réponse à la commande "READY"
            if ("READY".equals(command)) {
                sendMessage("OK");
                status = "Authenticated"; // Mise à jour du statut après authentification
            }
        }
    }

    // Fonction de minage exécutée par le worker
    public void mine(int start, int increment, boolean isAuthenticated, int difficulty, String payload) {
        try {
            if (isAuthenticated) {
                isMining = true;
                while (isMining) {
                    for (long nonce = start; ; nonce += increment) {
                        currentNonce = nonce;
                        String hash = calculateHash(nonce, payload);
                        // Vérifie si le hash commence par le nombre requis de zéros
                        if (hash.startsWith(getDifficultyString(difficulty))) {
                            // Validation du travail via l'API
                            boolean valid = WebApp.validateWork(difficulty, Long.toHexString(nonce), hash);
                            if (valid) {
                                sendMessage("FOUND " + hash + " " + nonce);
                                server.broadcastAuthenticated("SOLVED"); // Notification de la solution trouvée
                                server.broadcastToConnect(); // Diffusion aux autres workers
                            }
                            isMining = false;
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Génère une chaîne de zéros correspondant au niveau de difficulté
    public String getDifficultyString(int difficulty) {
        return "0".repeat(Math.max(0, difficulty));
    }

    // Calcule le hash en utilisant le nonce et le payload
    public String calculateHash(long nonce, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convertir le payload en tableau de bytes
            byte[] payloadBytes = payload.getBytes();

            // Convertir le nonce en tableau de bytes en enlevant les zéros initiaux
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(nonce);
            byte[] fullArray = buffer.array();
            int leadingZeroes = 0;
            while (leadingZeroes < fullArray.length && fullArray[leadingZeroes] == 0) {
                leadingZeroes++;
            }
            byte[] nonceBytes = Arrays.copyOfRange(fullArray, leadingZeroes, fullArray.length);

            // Concaténer les bytes du payload et du nonce
            byte[] inputBytes = new byte[payloadBytes.length + nonceBytes.length];
            System.arraycopy(payloadBytes, 0, inputBytes, 0, payloadBytes.length);
            System.arraycopy(nonceBytes, 0, inputBytes, payloadBytes.length, nonceBytes.length);

            // Calculer le hash SHA-256
            byte[] hashBytes = digest.digest(inputBytes);

            // Convertir le hash en chaîne hexadécimale
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Gestion de l'absence de l'algorithme SHA-256
        }
    }

    // Envoie un message au worker
    public void sendMessage(String message) {
        out.println(message);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isMining() {
        return isMining;
    }

    public void setMining(boolean isMining) {
        this.isMining = isMining;
    }

    public long getCurrentNonce() {
        return currentNonce;
    }
}
