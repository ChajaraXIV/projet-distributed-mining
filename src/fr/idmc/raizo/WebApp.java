package fr.idmc.raizo;

import java.io.*;
import java.net.*;

public class WebApp {
    private static final String BASE_URL = "https://projet-raizo-idmc.netlify.app/.netlify/functions";

    public static String generateWork(int difficulty) throws IOException {
        String url = BASE_URL + "/generate_work?d=" + difficulty;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer recDtReZk1g8c8eA3");
        
        try {
        	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        	String response = in.readLine();
        	int startIndex = response.indexOf("{\"data\":\"") + "{\"data\":\"".length(); 
        	int endIndex = response.indexOf("\"}", startIndex); 
        	String data = response.substring(startIndex, endIndex);
        	in.close();        	
        	return data;
        } catch(Exception e){
        	BufferedReader error = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        	String response = error.readLine();
        	int startIndex = response.indexOf("{\"details\":\"") + "{\"details\":\"".length(); 
        	int endIndex = response.indexOf("\"}", startIndex); 
        	String data = response.substring(startIndex, endIndex);
        	error.close();        	
        	return data;
        }
    }

    public static boolean validateWork(int difficulty, String nonce, String hash) throws IOException {
        String url = BASE_URL + "/validate_work";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer recDtReZk1g8c8eA3");
        connection.setDoOutput(true);

        String payload = "{\"d\": " + difficulty + ", \"n\": \"" + nonce + "\", \"h\": \"" + hash + "\"}";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes());
            os.flush();
        }

        return connection.getResponseCode() == 200;
    }
}
