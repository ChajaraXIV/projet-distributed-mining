package fr.idmc.raizo;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerLauncher {
    private static final int PORT = 1337;
    private static final String PASSWORD = "azerty";
    private List<WorkerHandler> workers = new ArrayList<>();
    private ServerSocket serverSocket;
    private boolean running = true;

    public static void main(String[] args) throws Exception {
        ServerLauncher serverLauncher = new ServerLauncher();
        Thread serverThread = new Thread(() -> {
            try {
                serverLauncher.runServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        serverThread.start();
        
        // Handle commands in the main thread
        Thread inputThread = new Thread(() -> {
        	try {
				serverLauncher.handleCommands();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });
        
        inputThread.start();
        // Wait for the server thread to finish
        serverThread.join();
    }

    public void runServer() throws Exception {
        serverSocket = new ServerSocket(PORT);
        while (running) {
            Socket socket = serverSocket.accept();
            WorkerHandler workerHandler = new WorkerHandler(socket, this);
            workers.add(workerHandler);
            Thread workerThread = new Thread(workerHandler);
            workerThread.start();
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
            keepGoing = processCommand(command.trim());
        }
    }

    protected boolean processCommand(String cmd) throws Exception {
        if ("quit".equals(cmd)) {
            for (WorkerHandler worker : workers) {
                worker.sendMessage("CLOSING SOCKET");
                worker.getSocket().close();
            }
            running = false;
            System.exit(0);
        } else if ("cancel".equals(cmd)) {
            for (WorkerHandler worker : workers) {
            	if("Authenticated".equals(worker.getStatus())) {            		
            		worker.setMining(false);
            		worker.sendMessage("CANCELLED");
            		worker.setStatus("Connected");
            		System.out.println(worker.isMining());
            	}
            }
        } else if ("status".equals(cmd)) {
            System.out.println("Workers status:");
            for (WorkerHandler worker : workers) {
                System.out.println(worker.getStatus());
            }
        }else if ("progress".equals(cmd)) {
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
            System.out.println(" • status - display information about connected workers");
            System.out.println(" • solve <d> - try to mine with given difficulty");
            System.out.println(" • progress - see what each worker is testing or not");
            System.out.println(" • cancel - cancel a task");
            System.out.println(" • help - describe available commands");
            System.out.println(" • quit - terminate pending work and quit");
        } else if (cmd.startsWith("solve")) {
            String[] parts = cmd.split(" ");
            if (parts.length == 2 && parts[1].matches("\\d+")) {
                int difficulty = Integer.parseInt(parts[1]);
                if(difficulty > 32 || difficulty < 1) {
                	System.out.println("difficulty must be between 1 and 32 !");
                }else {                	
                	String payload = WebApp.generateWork(difficulty);
                	if(payload.startsWith("Difficulty")) {
                		broadcastAuthenticated(payload);
                	}else {                		
                		int counter = 0;
                		for (WorkerHandler worker : workers) {
                			int increment = workers.size();
                			final int currentCounter = counter;
                			if ("Authenticated".equals(worker.getStatus())) {
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

    public synchronized void broadcast(String message) {
        for (WorkerHandler worker : workers) {
            worker.sendMessage(message);
        }
    }
    
    public synchronized void broadcastAuthenticated(String message) {
        for (WorkerHandler worker : workers) {
        	if("Authenticated".equals(worker.getStatus())) {        		
        		worker.sendMessage(message);
        	}
        }
    }
    
    public synchronized void broadcastToConnect() {
        for (WorkerHandler worker : workers) {
        	if("Authenticated".equals(worker.getStatus())) {        		
        		worker.setStatus("Connected");;
        	}
        }
    }

    public synchronized void removeWorker(WorkerHandler worker) {
        workers.remove(worker);
    }

    public static String getPassword() {
        return PASSWORD;
    }
}
