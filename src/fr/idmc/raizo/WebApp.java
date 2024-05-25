package fr.idmc.raizo;

import java.io.*;
import java.net.*;

public class WebApp {
    // URL de base pour les fonctions Netlify spécifiques à l'application
    private static final String BASE_URL = "https://projet-raizo-idmc.netlify.app/.netlify/functions";

    // Génère un travail en fonction de la difficulté spécifiée
    public static String generateWork(int difficulty) throws IOException {
        // Construire l'URL pour la génération du travail avec le paramètre de difficulté
        String url = BASE_URL + "/generate_work?d=" + difficulty;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        
        // Configurer la requête HTTP en tant que GET
        connection.setRequestMethod("GET");
        // Définir le type de contenu pour la requête
        connection.setRequestProperty("Content-Type", "application/json");
        // Ajouter le jeton d'autorisation pour accéder à l'API sécurisée
        connection.setRequestProperty("Authorization", "Bearer recDtReZk1g8c8eA3");
        
        try {
            // Lire la réponse de la requête
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = in.readLine();
            // Extraire les données JSON pertinentes de la réponse
            int startIndex = response.indexOf("{\"data\":\"") + "{\"data\":\"".length();
            int endIndex = response.indexOf("\"}", startIndex);
            String data = response.substring(startIndex, endIndex);
            in.close();            
            // Retourner les données extraites
            return data;
        } catch(Exception e) {
            // Gérer les erreurs de la requête en lisant le flux d'erreur
            BufferedReader error = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String response = error.readLine();
            // Extraire les détails des erreurs de la réponse JSON
            int startIndex = response.indexOf("{\"details\":\"") + "{\"details\":\"".length();
            int endIndex = response.indexOf("\"}", startIndex);
            String data = response.substring(startIndex, endIndex);
            error.close();
            // Retourner les détails de l'erreur
            return data;
        }
    }

    // Valide le travail en fonction de la difficulté, du nonce et du hash fournis
    public static boolean validateWork(int difficulty, String nonce, String hash) throws IOException {
        // Construire l'URL pour la validation du travail
        String url = BASE_URL + "/validate_work";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        
        // Configurer la requête HTTP en tant que POST
        connection.setRequestMethod("POST");
        // Ajouter le jeton d'autorisation pour accéder à l'API sécurisée
        connection.setRequestProperty("Authorization", "Bearer recDtReZk1g8c8eA3");
        // Permettre l'envoi de données dans le corps de la requête
        connection.setDoOutput(true);

        // Créer le payload JSON avec les paramètres de validation
        String payload = "{\"d\": " + difficulty + ", \"n\": \"" + nonce + "\", \"h\": \"" + hash + "\"}";
        try (OutputStream os = connection.getOutputStream()) {
            // Écrire et envoyer le payload dans le corps de la requête
            os.write(payload.getBytes());
            os.flush();
        }

        // Vérifier si la réponse HTTP indique un succès (code 200)
        return connection.getResponseCode() == 200;
    }
}
