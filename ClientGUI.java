import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * ClientGUI - Graphical user interface for the Client
 * This class only handles the GUI - network communication follows Phase I protocol
 */
public class ClientGUI extends JFrame {
    private static final int DEFAULT_PORT = 8089;
    
    // Network components
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isConnected = false;
    
    // GUI Components - Connection Panel
    private JTextField serverIPField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusLabel;
    
    // GUI Components - File Selection Panel
    private JPanel fileButtonPanel;
    private ArrayList<String> availableFiles;
    
    // GUI Components - Data Display Area
    private JTextArea displayArea;
    private JScrollPane scrollPane;
    
    public ClientGUI() {
        // Set up the frame
        setTitle("Student Data Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));
        
        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 1. Connection Panel (Top)
        JPanel connectionPanel = createConnectionPanel();
        
        // 2. File Selection Panel (Middle)
        JPanel fileSelectionPanel = createFileSelectionPanel();
        
        // 3. Data Display Area (Bottom)
        JPanel dataDisplayPanel = createDataDisplayPanel();
        
        // Combine connection and file selection panels
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        topPanel.add(connectionPanel);
        topPanel.add(fileSelectionPanel);
        
        // Add panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(dataDisplayPanel, BorderLayout.CENTER);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Center the frame on screen
        setLocationRelativeTo(null);
        
        availableFiles = new ArrayList<>();
    }
    
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Connection"));
        
        // Server IP Field
        panel.add(new JLabel("Server IP:"));
        serverIPField = new JTextField("localhost", 12);
        panel.add(serverIPField);
        
        // Port Field
        panel.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        panel.add(portField);
        
        // Connect Button
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);
        
        // Disconnect Button
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnectFromServer());
        panel.add(disconnectButton);
        
        panel.add(Box.createHorizontalStrut(20));
        
        // Status Label
        panel.add(new JLabel("Status:"));
        statusLabel = new JLabel("Disconnected");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(Color.RED);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(255, 200, 200));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        panel.add(statusLabel);
        
        return panel;
    }
    
    private JPanel createFileSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("File Selection"));
        
        // Panel for dynamic file buttons
        fileButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel instructionLabel = new JLabel("Connect to server to view available files");
        instructionLabel.setForeground(Color.GRAY);
        fileButtonPanel.add(instructionLabel);
        
        panel.add(fileButtonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDataDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Data Display"));
        
        // Create text area for displaying data
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        displayArea.setMargin(new Insets(10, 10, 10, 10));
        
        // Add scroll pane
        scrollPane = new JScrollPane(displayArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void connectToServer() {
        String serverIP = serverIPField.getText().trim();
        if (serverIP.isEmpty()) {
            serverIP = "localhost";
        }
        
        try {
            int port = Integer.parseInt(portField.getText().trim());
            
            // Connect to server (Phase I protocol)
            socket = new Socket(serverIP, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;
            
            // Update UI
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Connected");
                statusLabel.setForeground(new Color(0, 128, 0));
                statusLabel.setBackground(new Color(200, 255, 200));
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                serverIPField.setEnabled(false);
                portField.setEnabled(false);
            });
            
            // Receive file list (Phase I protocol)
            receiveFileList();
            
        } catch (NumberFormatException ex) {
            showError("Invalid port number");
        } catch (IOException ex) {
            showError("Connection failed: " + ex.getMessage());
        }
    }
    
    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            isConnected = false;
            
            // Update UI
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Disconnected");
                statusLabel.setForeground(Color.RED);
                statusLabel.setBackground(new Color(255, 200, 200));
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                serverIPField.setEnabled(true);
                portField.setEnabled(true);
                
                // Clear file buttons
                fileButtonPanel.removeAll();
                JLabel label = new JLabel("Connect to server to view available files");
                label.setForeground(Color.GRAY);
                fileButtonPanel.add(label);
                fileButtonPanel.revalidate();
                fileButtonPanel.repaint();
                
                // Clear display
                displayArea.setText("");
            });
            
        } catch (IOException ex) {
            showError("Error disconnecting: " + ex.getMessage());
        }
    }
    
    private void receiveFileList() throws IOException {
        // Phase I protocol: FILELIST, files..., END
        String line = in.readLine();
        if (!"FILELIST".equals(line)) {
            showError("Unexpected server response");
            return;
        }
        
        availableFiles.clear();
        
        while (!(line = in.readLine()).equals("END")) {
            availableFiles.add(line);
        }
        
        // Create buttons for files
        SwingUtilities.invokeLater(() -> {
            fileButtonPanel.removeAll();
            
            for (String file : availableFiles) {
                JButton fileButton;
                if (file.equals("OVERVIEW")) {
                    fileButton = new JButton("View Overall Statistics");
                    fileButton.setBackground(new Color(100, 150, 255));
                    fileButton.setForeground(Color.WHITE);
                } else {
                    String courseName = file.replace(".txt", "");
                    fileButton = new JButton(courseName);
                }
                
                fileButton.setFont(new Font("Arial", Font.PLAIN, 12));
                fileButton.addActionListener(e -> requestFile(file));
                fileButtonPanel.add(fileButton);
            }
            
            fileButtonPanel.revalidate();
            fileButtonPanel.repaint();
        });
    }
    
    private void requestFile(String filename) {
        if (!isConnected) {
            showError("Not connected to server");
            return;
        }
        
        try {
            // Reconnect for new request (Phase I closes after each request)
            String serverIP = serverIPField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            
            socket = new Socket(serverIP, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Skip file list
            String line = in.readLine();
            if (!"FILELIST".equals(line)) {
                showError("Unexpected server response");
                return;
            }
            
            while (!in.readLine().equals("END")) {
                // Skip file list
            }
            
            // Send file request (Phase I protocol)
            out.println(filename);
            
            // Receive content
            line = in.readLine();
            if (line.startsWith("ERROR")) {
                showError(line);
                return;
            }
            
            if (!"CONTENT".equals(line)) {
                showError("Unexpected server response");
                return;
            }
            
            // Display content
            if ("OVERVIEW".equals(filename)) {
                displayOverview();
            } else {
                displayStudentData(filename);
            }
            
            socket.close();
            
        } catch (IOException ex) {
            showError("Error requesting file: " + ex.getMessage());
        }
    }
    
    private void displayStudentData(String filename) throws IOException {
        StringBuilder display = new StringBuilder();
        
        String courseName = filename.replace(".txt", "");
        
        // Header (like Phase I console output)
        display.append("\n");
        display.append("========================================\n");
        display.append("     ").append(courseName).append(" - Student Records\n");
        display.append("========================================\n\n");
        
        // Table header
        display.append("+-------------------------+------------+-------+\n");
        display.append("| Name                    | Student ID | Grade |\n");
        display.append("+-------------------------+------------+-------+\n");
        
        String line;
        int count = 0;
        
        while (!(line = in.readLine()).equals("END")) {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                String name = parts[0].trim();
                String id = parts[1].trim();
                String grade = parts[2].trim();
                
                // Convert numeric grade to letter grade
                String letterGrade = getLetterGrade(Integer.parseInt(grade));
                
                // Format row - fixed width for alignment
                display.append(String.format("| %-23s | %-10s | %-5s |\n",
                        truncate(name, 23), id, grade + " (" + letterGrade + ")"));
                count++;
            }
        }
        
        display.append("+-------------------------+------------+-------+\n");
        display.append("\nTotal students: ").append(count).append("\n");
        
        // Update display area
        final String displayText = display.toString();
        SwingUtilities.invokeLater(() -> {
            displayArea.setText(displayText);
            displayArea.setCaretPosition(0);
        });
        
        // Show success message
        final int studentCount = count;
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Loaded " + studentCount + " students from " + courseName,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    private void displayOverview() throws IOException {
        StringBuilder display = new StringBuilder();
        
        display.append("\n");
        display.append("═══════════════════════════════════════════\n");
        display.append("      STUDENT GRADE OVERVIEW STATISTICS    \n");
        display.append("═══════════════════════════════════════════\n\n");
        
        String line;
        while (!(line = in.readLine()).equals("END")) {
            if (!line.startsWith("===")) {
                display.append("  ").append(line).append("\n");
            }
        }
        
        display.append("\n═══════════════════════════════════════════\n");
        
        // Update display area
        final String displayText = display.toString();
        SwingUtilities.invokeLater(() -> {
            displayArea.setText(displayText);
            displayArea.setCaretPosition(0);
        });
    }
    
    private String getLetterGrade(int grade) {
        if (grade >= 90) return "A";
        else if (grade >= 80) return "B";
        else if (grade >= 70) return "C";
        else if (grade >= 60) return "D";
        else return "F";
    }
    
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error",
                    JOptionPane.ERROR_MESSAGE);
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI client = new ClientGUI();
            client.setVisible(true);
        });
    }
}
