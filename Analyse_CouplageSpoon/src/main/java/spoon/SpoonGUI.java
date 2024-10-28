package spoon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import spoon.reflect.declaration.CtType;

public class SpoonGUI extends JFrame {
    private JTextField projectPathField;
    private JTextArea resultArea;
    private JButton browseButton;
    private JButton analyzeButton;
    private JButton generateGraphButton;
    private JButton clusterButton;
    private JButton identifyModulesButton;
    private JTextField thresholdField;

    private Launcher launcher;
    private CouplingAnalyzer couplingAnalyzer;
    private CouplingGraphGenerator graphGenerator;
    private HierarchicalClusteringAnalyzer clusteringAnalyzer;
    private ModuleIdentifier moduleIdentifier;

    private JTabbedPane tabbedPane;
    private JPanel graphPanel;
    private JLabel imageLabel;
    private double zoomFactor = 0.1;

    private Color backgroundColor = new Color(240, 240, 245);
    private Color buttonColor = new Color(70, 130, 180);
    private Color textColor = new Color(50, 50, 50);
    private Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);
    private Font headerFont = new Font("Segoe UI", Font.BOLD, 16);

    public SpoonGUI() {
        setTitle("Spoon Analysis Tool");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(backgroundColor);
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        projectPathField = new JTextField(20);
        projectPathField.setFont(mainFont);
        browseButton = createStyledButton("Browse");
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        resultArea.setBackground(new Color(250, 250, 250));
        resultArea.setForeground(textColor);
        analyzeButton = createStyledButton("Analyze Coupling");
        generateGraphButton = createStyledButton("Generate Graph");
        clusterButton = createStyledButton("Perform Clustering");
        identifyModulesButton = createStyledButton("Identify Modules");
        thresholdField = new JTextField("0.05", 5);
        thresholdField.setFont(mainFont);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(mainFont);
        graphPanel = new JPanel(new BorderLayout());
        graphPanel.setBackground(backgroundColor);

        browseButton.addActionListener(e -> browseForProject());
        analyzeButton.addActionListener(e -> analyzeCoupling());
        generateGraphButton.addActionListener(e -> generateGraph());
        clusterButton.addActionListener(e -> performClustering());
        identifyModulesButton.addActionListener(e -> identifyModules());
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(mainFont);
        button.setBackground(buttonColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        return button;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(backgroundColor);
        topPanel.add(new JLabel("Project Path:"));
        topPanel.add(projectPathField);
        topPanel.add(browseButton);
        topPanel.add(analyzeButton);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttonPanel.add(generateGraphButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(clusterButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        JPanel thresholdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        thresholdPanel.setBackground(backgroundColor);
        thresholdPanel.add(new JLabel("Coupling Threshold:"));
        thresholdPanel.add(thresholdField);
        buttonPanel.add(thresholdPanel);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(identifyModulesButton);

        tabbedPane.addTab("Results", new JScrollPane(resultArea));
        tabbedPane.addTab("Graph", graphPanel);

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    private void browseForProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            projectPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void analyzeCoupling() {
        String projectPath = projectPathField.getText();
        if (projectPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a project directory.");
            return;
        }

        launcher = new Launcher();
        launcher.addInputResource(projectPath);
        launcher.buildModel();

        couplingAnalyzer = new CouplingAnalyzer();
        couplingAnalyzer.analyze(projectPath);

        Map<String, Map<String, Double>> couplingGraph = couplingAnalyzer.getCouplingGraph();

        resultArea.setText("Coupling Analysis Results:\n");
        for (Map.Entry<String, Map<String, Double>> entry : couplingGraph.entrySet()) {
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                resultArea.append(String.format("%s -> %s : %.4f\n", 
                    entry.getKey(), innerEntry.getKey(), innerEntry.getValue()));
            }
        }
    }

    private void generateGraph() {
        if (couplingAnalyzer == null) {
            JOptionPane.showMessageDialog(this, "Please analyze coupling first.");
            return;
        }

        graphGenerator = new CouplingGraphGenerator();
        graphGenerator.generateWeightedGraph(couplingAnalyzer.getCouplingGraph());
        String dotGraph = graphGenerator.exportGraphToDOT();

        try {
            MutableGraph g = new Parser().read(dotGraph);
            // Utiliser un nom de fichier unique pour chaque génération
            String filename = "coupling_graph_" + System.currentTimeMillis() + ".png";
            Graphviz.fromGraph(g).width(500).render(Format.PNG).toFile(new File(filename));
            
            resultArea.append("Graph generated and displayed in the Graph tab.\n");
            displayImageInGraphPanel(filename);
            
        } catch (IOException e) {
            resultArea.append("Error generating graph: " + e.getMessage() + "\n");
        }
    }


    private void displayImageInGraphPanel(String filename) {
        try {
            ImageIcon icon = new ImageIcon(filename);
            imageLabel = new JLabel(icon);
            
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            
            JButton zoomInButton = new JButton("Zoom In");
            JButton zoomOutButton = new JButton("Zoom Out");
            JButton resetZoomButton = new JButton("Reset Zoom");

            zoomInButton.addActionListener(e -> updateZoom(1.1, filename));
            zoomOutButton.addActionListener(e -> updateZoom(1/1.1, filename));
            resetZoomButton.addActionListener(e -> resetZoom(filename));

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(zoomInButton);
            buttonPanel.add(zoomOutButton);
            buttonPanel.add(resetZoomButton);

            graphPanel.removeAll();
            graphPanel.add(scrollPane, BorderLayout.CENTER);
            graphPanel.add(buttonPanel, BorderLayout.SOUTH);

            tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("Graph"));

            revalidate();
            repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error displaying image: " + e.getMessage());
        }
    }


    private void updateZoom(double factor, String filename) {
        zoomFactor *= factor;
        updateImage(filename);
    }

    private void resetZoom(String filename) {
        zoomFactor = 1;
        updateImage(filename);
    }

    private void updateImage(String filename) {
        ImageIcon originalIcon = new ImageIcon(filename);
        Image originalImage = originalIcon.getImage();
        Image scaledImage = getScaledImage(originalImage, zoomFactor);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.revalidate();
    }


    private Image getScaledImage(Image originalImage, double factor) {
        int newWidth = (int) (originalImage.getWidth(null) * factor);
        int newHeight = (int) (originalImage.getHeight(null) * factor);
        return originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }

    private void performClustering() {
        if (launcher == null || couplingAnalyzer == null) {
            JOptionPane.showMessageDialog(this, "Please analyze coupling first.");
            return;
        }

        clusteringAnalyzer = new HierarchicalClusteringAnalyzer(launcher, couplingAnalyzer.getCouplingGraph());
        resultArea.append("Clustering Results:\n\n");

        int step = 1;
        while (clusteringAnalyzer.performNextClusteringStep()) {
            resultArea.append("Étape " + step + " du clustering :\n");

            List<List<CtType<?>>> currentClusters = clusteringAnalyzer.getCurrentClusters();
            for (int i = 0; i < currentClusters.size(); i++) {
                List<CtType<?>> cluster = currentClusters.get(i);
                double intraCoupling = clusteringAnalyzer.calculateIntraClusterCoupling(new HierarchicalClusteringAnalyzer.Cluster(cluster));
                
                resultArea.append("Cluster " + (i + 1) + ": ");
                resultArea.append(cluster.stream()
                   .map(CtType::getSimpleName)
                   .collect(Collectors.joining(", ")));
                resultArea.append(" (Couplage intra-cluster : " + String.format("%.4f", intraCoupling) + ")\n");
            }
            resultArea.append("\n");
            step++;
        }

        resultArea.append("Clustering hiérarchique terminé.\n");
        tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("Results"));
    }
    
    private void identifyModules() {
        if (launcher == null || couplingAnalyzer == null || clusteringAnalyzer == null) {
            JOptionPane.showMessageDialog(this, "Please perform clustering first.");
            return;
        }

        double threshold;
        try {
            threshold = Double.parseDouble(thresholdField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid threshold value. Please enter a valid number.");
            return;
        }

        moduleIdentifier = new ModuleIdentifier(launcher, couplingAnalyzer.getCouplingGraph(), threshold);
        List<List<spoon.reflect.declaration.CtType<?>>> finalClusters = clusteringAnalyzer.getCurrentClusters();
        List<ModuleIdentifier.Module> modules = moduleIdentifier.identifyModules(finalClusters);

        resultArea.setText("Identified Modules:\n");
        for (ModuleIdentifier.Module module : modules) {
            resultArea.append(module.toString() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SpoonGUI().setVisible(true);
        });
    }
}
