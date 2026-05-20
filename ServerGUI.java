import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * ServerGUI - Graphical user interface for the Server
 * This class only handles the GUI - all business logic is in ServerCore
 */
public class ServerGUI extends JFrame implements ServerCore.ServerLogger {
    private static final int DEFAULT_PORT = 8089;
    
    // Server core (business logic)
    private ServerCore serverCore;
    
    // GUI Components
    private JTextField portField;
    private JLabel ipLabel;
    private JLabel statusLabel;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;
    private JButton clearLogButton;
    
    public ServerGUI() {
        // Set up the frame
        setTitle("Student Data Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLayout(new BorderLayout(10, 10));
        
        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 1. Server Configuration Panel (Top)
        JPanel configPanel = createConfigPanel();
        
        // 2. Server Controls Panel (Middle-Top)
        JPanel controlPanel = createControlPanel();
        
        // 3. Server Activity Log (Bottom)
        JPanel logPanel = createLogPanel();
        
        // Combine config and control panels
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        topPanel.add(configPanel);
        topPanel.add(controlPanel);
        
        // Add panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(logPanel, BorderLayout.CENTER);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Center the frame on screen
        setLocationRelativeTo(null);
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Server Configuration"));
        
        // IP Address Display
        panel.add(new JLabel("Server IP:"));
        String serverIP = getLocalIPAddress();
        ipLabel = new JLabel(serverIP);
        ipLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        ipLabel.setForeground(new Color(0, 100, 0));
        panel.add(ipLabel);
        
        panel.add(Box.createHorizontalStrut(20));
        
        // Port Number Input
        panel.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        panel.add(portField);
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Server Controls"));
        
        // Start Button
        startButton = new JButton("Start Server");
        startButton.addActionListener(e -> startServer());
        panel.add(startButton);
        
        // Stop Button
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());
        panel.add(stopButton);
        
        panel.add(Box.createHorizontalStrut(20));
        
        // Status Label
        panel.add(new JLabel("Status:"));
        statusLabel = new JLabel("Stopped");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.RED);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(255, 200, 200));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(statusLabel);
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Server Activity Log"));
        
        // Text Area with scroll
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Clear Log Button
        clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearLogButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            
            // Create server core with this GUI as the logger
            serverCore = new ServerCore(port, this);
            serverCore.start();
            
            // Update UI
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Running");
                statusLabel.setForeground(new Color(0, 128, 0));
                statusLabel.setBackground(new Color(200, 255, 200));
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                portField.setEnabled(false);
            });
            
            // Accept clients in a separate thread
            new Thread(() -> {
                while (serverCore.isRunning()) {
                    try {
                        serverCore.acceptClient();
                    } catch (IOException ex) {
                        if (serverCore.isRunning()) {
                            log("Error accepting client: " + ex.getMessage());
                        }
                    }
                }
            }).start();
            
        } catch (NumberFormatException ex) {
            showError("Invalid port number");
        } catch (IOException ex) {
            showError("Failed to start server: " + ex.getMessage());
        }
    }
    
    private void stopServer() {
        try {
            if (serverCore != null) {
                serverCore.stop();
            }
            
            // Update UI
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Stopped");
                statusLabel.setForeground(Color.RED);
                statusLabel.setBackground(new Color(255, 200, 200));
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                portField.setEnabled(true);
            });
            
        } catch (IOException ex) {
            showError("Error stopping server: " + ex.getMessage());
        }
    }
    
    // Implementation of ServerLogger interface
    @Override
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + getCurrentTime() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", 
                JOptionPane.ERROR_MESSAGE);
            log("ERROR: " + message);
        });
    }
    
    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }
    
    private String getLocalIPAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException ex) {
            return "Unknown";
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI server = new ServerGUI();
            server.setVisible(true);
        });
    }
}
