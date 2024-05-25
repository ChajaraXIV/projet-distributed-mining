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

    public WorkerHandler(Socket socket, ServerLauncher server) {
        this.socket = socket;
        this.server = server;
        this.status = "Connected";
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(getSocket().getInputStream()));
            out = new PrintWriter(getSocket().getOutputStream(), true);

            sendMessage("WHO_ARE_YOU_?");
            String response = in.readLine();
            if ("ITS_ME".equals(response)) {
                sendMessage("GIMME_PASSWORD");
                response = in.readLine();
                if (("PASSWD " + ServerLauncher.getPassword()).equals(response)) {
                    sendMessage("HELLO_YOU");
                    handleCommands();
                } else {
                    sendMessage("YOU_DONT_FOOL_ME");
                    getSocket().close();
                }
            } else {
                getSocket().close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.removeWorker(this);
        }
    }
    
    private void handleCommands() throws IOException {
        while (true) {
            String command = in.readLine();
            if (command == null) break;

            if ("READY".equals(command)) {
                sendMessage("OK");
                status = "Authenticated";
            }
        }
    }

    
    public void mine(int start, int increment, boolean isAuthenticated, int difficulty, String payload) {
        try {
            if (isAuthenticated) {
            	isMining = true;
            	long FalseStrat = 47203976527L;
                while (isMining) {
                    for (long nonce = FalseStrat+start; ; nonce += increment) {
                    	currentNonce = nonce;
                    	String hash = calculateHash(nonce, payload);
                        if (hash.startsWith(getDifficultyString(difficulty))) {
                        	boolean valid = WebApp.validateWork(difficulty,Long.toHexString(nonce), hash);
                            if (valid) {
                            	sendMessage("FOUND " + hash + " " + nonce);
                                server.broadcastAuthenticated("SOLVED");
                                server.broadcastToConnect();
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

    public String getDifficultyString(int difficulty) {
        return "0".repeat(Math.max(0, difficulty));
    }

    public String calculateHash(long nonce, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert payload to bytes
            byte[] payloadBytes = payload.getBytes();

            // Convert nonce to byte array, remove leading zeros
            //byte[] nonceBytes = intToByteArray(nonce);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(nonce);
            byte[] fullArray = buffer.array();
            int leadingZeroes = 0;
            while (leadingZeroes < fullArray.length && fullArray[leadingZeroes] == 0) {
                leadingZeroes++;
            }
            byte[] nonceBytes = Arrays.copyOfRange(fullArray, leadingZeroes, fullArray.length);

            // Concatenate payloadBytes and nonceBytes
            byte[] inputBytes = new byte[payloadBytes.length + nonceBytes.length];
            System.arraycopy(payloadBytes, 0, inputBytes, 0, payloadBytes.length);
            System.arraycopy(nonceBytes, 0, inputBytes, payloadBytes.length, nonceBytes.length);

            // Calculate SHA-256 hash
            byte[] hashBytes = digest.digest(inputBytes);

            // Convert hash bytes to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] intToByteArray(int value) {
        return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)value
        };
    }

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
