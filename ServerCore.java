import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ServerCore - Contains all the business logic for the student data server
 * This class can be used by both console (Server.java) and GUI (ServerGUI.java) versions
 */
public class ServerCore {
    private static final String[] COURSE_FILES = {
            "CS101.txt", "MATH201.txt", "CMPE351.txt", "CMPE411.txt", "CMPE431.txt"
    };
    private static final String MERGED_FILE = "AllStudents.txt";
    private static final String OVERVIEW_FILE = "Overview.txt";
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean isRunning = false;
    private ServerLogger logger;
    private int port;
    
    /**
     * Interface for logging - allows console or GUI to implement their own logging
     */
    public interface ServerLogger {
        void log(String message);
    }
    
    public ServerCore(int port, ServerLogger logger) {
        this.port = port;
        this.logger = logger;
    }
    
    /**
     * Start the server
     */
    public void start() throws IOException {
        if (isRunning) {
            logger.log("Server is already running");
            return;
        }
        
        // Create thread pool
        threadPool = Executors.newFixedThreadPool(5);
        
        // Process files in background
        logger.log("Initializing server...");
        processFilesInBackground();
        
        // Start server socket
        serverSocket = new ServerSocket(port);
        isRunning = true;
        
        logger.log("Server started on port " + port);
        logger.log("Listening on " + getLocalIPAddress() + ":" + port);
        logger.log("Waiting for client connections...");
    }
    
    /**
     * Accept clients - this should be called in a loop from a separate thread
     */
    public void acceptClient() throws IOException {
        if (!isRunning) {
            return;
        }
        
        Socket clientSocket = serverSocket.accept();
        String clientIP = clientSocket.getInetAddress().getHostAddress();
        logger.log("Client connected from " + clientIP);
        
        // Handle client in thread pool
        threadPool.execute(new ClientHandler(clientSocket, logger));
    }
    
    /**
     * Stop the server
     */
    public void stop() throws IOException {
        isRunning = false;
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        if (threadPool != null) {
            threadPool.shutdown();
        }
        
        logger.log("Server stopped");
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Get local IP address
     */
    public String getLocalIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "Unknown";
        }
    }
    
    /**
     * Process files in background using thread pool
     */
    private void processFilesInBackground() {
        threadPool.execute(new MergeFilesTask(logger));
        threadPool.execute(new AnalysisTask(logger));
    }
    
    // Task 1: Merge all student data and remove duplicates
    static class MergeFilesTask implements Runnable {
        private ServerLogger logger;
        
        MergeFilesTask(ServerLogger logger) {
            this.logger = logger;
        }
        
        @Override
        public void run() {
            logger.log("Starting file merge task...");
            
            try {
                Map<Integer, StudentRecord> studentMap = new HashMap<>();
                
                // Read all course files
                for (String courseFile : COURSE_FILES) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(courseFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            StudentRecord record = parseStudent(line);
                            if (record != null) {
                                int id = record.studentId;
                                
                                if (!studentMap.containsKey(id) ||
                                        studentMap.get(id).grade < record.grade) {
                                    studentMap.put(id, record);
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.log("Error reading " + courseFile);
                    }
                }
                
                // Write merged data
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(MERGED_FILE))) {
                    for (StudentRecord record : studentMap.values()) {
                        writer.write(record.name + ", " + record.studentId + ", " + record.grade);
                        writer.newLine();
                    }
                }
                
                logger.log("Merged " + studentMap.size() + " unique students into " + MERGED_FILE);
                
            } catch (IOException e) {
                logger.log("Error in merge task: " + e.getMessage());
            }
        }
    }
    
    // Task 2: Calculate overview statistics
    static class AnalysisTask implements Runnable {
        private ServerLogger logger;
        
        AnalysisTask(ServerLogger logger) {
            this.logger = logger;
        }
        
        @Override
        public void run() {
            try {
                Thread.sleep(2000); // Wait for merge to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            logger.log("Starting analysis task...");
            
            try (BufferedReader reader = new BufferedReader(new FileReader(MERGED_FILE))) {
                ArrayList<Integer> grades = new ArrayList<>();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    StudentRecord record = parseStudent(line);
                    if (record != null) {
                        grades.add(record.grade);
                    }
                }
                
                if (grades.isEmpty()) {
                    logger.log("No grades found for analysis");
                    return;
                }
                
                // Calculate statistics
                int sum = 0;
                int best = grades.get(0);
                int lowest = grades.get(0);
                
                for (int grade : grades) {
                    sum += grade;
                    if (grade > best) best = grade;
                    if (grade < lowest) lowest = grade;
                }
                
                double average = (double) sum / grades.size();
                
                // Write overview
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(OVERVIEW_FILE))) {
                    writer.write("=== Student Grade Overview ===");
                    writer.newLine();
                    writer.write("Total Students: " + grades.size());
                    writer.newLine();
                    writer.write("Average Grade: " + String.format("%.2f", average));
                    writer.newLine();
                    writer.write("Best Grade: " + best);
                    writer.newLine();
                    writer.write("Lowest Grade: " + lowest);
                    writer.newLine();
                }
                
                logger.log("Analysis complete - Total: " + grades.size() + 
                         ", Avg: " + String.format("%.2f", average) +
                         ", Best: " + best + ", Lowest: " + lowest);
                
            } catch (IOException e) {
                logger.log("Error in analysis task: " + e.getMessage());
            }
        }
    }
    
    // Parse student record from line
    private static StudentRecord parseStudent(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                String name = parts[0].trim();
                int id = Integer.parseInt(parts[1].trim());
                int grade = Integer.parseInt(parts[2].trim());
                return new StudentRecord(name, id, grade);
            }
        } catch (Exception e) {
            // Skip invalid lines
        }
        return null;
    }
    
    // Student record class
    static class StudentRecord {
        String name;
        int studentId;
        int grade;
        
        StudentRecord(String name, int studentId, int grade) {
            this.name = name;
            this.studentId = studentId;
            this.grade = grade;
        }
    }
    
    // Handle individual client connections
    static class ClientHandler implements Runnable {
        private Socket socket;
        private ServerLogger logger;
        
        ClientHandler(Socket socket, ServerLogger logger) {
            this.socket = socket;
            this.logger = logger;
        }
        
        @Override
        public void run() {
            String clientIP = socket.getInetAddress().getHostAddress();
            
            try (
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                // Send file list
                logger.log("Sending file list to client " + clientIP);
                out.println("FILELIST");
                for (String file : COURSE_FILES) {
                    out.println(file);
                }
                out.println("OVERVIEW");
                out.println("END");
                
                // Receive client's choice
                String choice = in.readLine();
                logger.log("Client " + clientIP + " requested file: " + choice);
                
                if ("OVERVIEW".equals(choice)) {
                    sendFile(out, OVERVIEW_FILE);
                } else {
                    sendFile(out, choice);
                }
                
                logger.log("Request completed for client " + clientIP);
                logger.log("Client disconnected: " + clientIP);
                
            } catch (IOException e) {
                logger.log("Client handler error for " + clientIP + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.log("Error closing socket");
                }
            }
        }
        
        private void sendFile(PrintWriter out, String filename) {
            File file = new File(filename);
            
            if (!file.exists()) {
                out.println("ERROR: File not found");
                out.println("END");
                return;
            }
            
            try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
                out.println("CONTENT");
                String line;
                while ((line = fileReader.readLine()) != null) {
                    out.println(line);
                }
                out.println("END");
                
            } catch (IOException e) {
                out.println("ERROR: Could not read file");
                out.println("END");
            }
        }
    }
}
