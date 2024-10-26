package com.example;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

public class CouplingAnalyzerGUI extends JFrame {
    private JTextField projectPathField;
    private JTextArea resultArea;
    private JButton browseButton;
    private JButton analyzeButton;
    private JButton generateGraphButton;
    private JButton clusterButton;
    private JButton identifyModulesButton;
    private JTextField thresholdField;
    private JLabel graphLabel;

    private ProjectAnalyzer analyzer;
    private HierarchicalClusteringAnalyzer clusteringAnalyzer;
    private ModuleIdentifier moduleIdentifier;

    private JTabbedPane tabbedPane;
    private JPanel graphPanel;
    private double zoomFactor = 1.0;

    private Color backgroundColor = new Color(240, 240, 245);
    private Color buttonColor = new Color(70, 130, 180);
    private Color textColor = new Color(50, 50, 50);
    private Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);

    public CouplingAnalyzerGUI() {
        setTitle("Coupling Analyzer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(backgroundColor);

        initComponents();
        layoutComponents();

        analyzer = new ProjectAnalyzer();
    }

    private void initComponents() {
        projectPathField = new JTextField(20);
        projectPathField.setFont(mainFont);
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        resultArea.setBackground(new Color(250, 250, 250));
        resultArea.setForeground(textColor);
        browseButton = createStyledButton("Browse");
        analyzeButton = createStyledButton("Analyze");
        generateGraphButton = createStyledButton("Generate Graph");
        clusterButton = createStyledButton("Perform Clustering");
        identifyModulesButton = createStyledButton("Identify Modules");
        thresholdField = new JTextField("0.01", 5);
        thresholdField.setFont(mainFont);
        graphLabel = new JLabel();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(mainFont);
        graphPanel = new JPanel(new BorderLayout());
        graphPanel.setBackground(backgroundColor);

        browseButton.addActionListener(e -> browseForProject());
        analyzeButton.addActionListener(e -> analyzeCoupling());
        generateGraphButton.addActionListener(e -> generateGraph());
        clusterButton.addActionListener(e -> performClustering());
        identifyModulesButton.addActionListener(e -> identifyModules());

        // Désactiver les boutons initialement
        generateGraphButton.setEnabled(false);
        clusterButton.setEnabled(false);
        identifyModulesButton.setEnabled(false);
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

        // Réinitialiser toutes les données précédentes
        resetAnalysisData();

        analyzer.analyzeProject(projectPath);
        analyzer.calculateCoupling();
        Map<String, Map<String, Double>> couplingGraph = analyzer.getCouplingGraph();

        resultArea.setText("Coupling Analysis Results:\n");
        for (Map.Entry<String, Map<String, Double>> entry : couplingGraph.entrySet()) {
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                resultArea.append(String.format("%s -> %s : %.4f\n", 
                    entry.getKey(), innerEntry.getKey(), innerEntry.getValue()));
            }
        }
        tabbedPane.setSelectedIndex(0);

        // Activer les boutons après l'analyse
        generateGraphButton.setEnabled(true);
        clusterButton.setEnabled(true);
        identifyModulesButton.setEnabled(true);
    }

    private void resetAnalysisData() {
        // Réinitialiser toutes les données d'analyse
        analyzer = new ProjectAnalyzer();
        clusteringAnalyzer = null;
        moduleIdentifier = null;

        // Effacer les résultats précédents
        resultArea.setText("");

        // Effacer l'image du graphe précédent
        graphLabel.setIcon(null);
        graphPanel.removeAll();
        graphPanel.revalidate();
        graphPanel.repaint();

        // Réinitialiser le facteur de zoom
        zoomFactor = 1.0;

        // Désactiver les boutons jusqu'à ce qu'une nouvelle analyse soit effectuée
        generateGraphButton.setEnabled(false);
        clusterButton.setEnabled(false);
        identifyModulesButton.setEnabled(false);
    }

    private void generateGraph() {
        Map<String, Map<String, Double>> couplingGraph = analyzer.getCouplingGraph();
        if (couplingGraph == null || couplingGraph.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please analyze the project first.");
            return;
        }

        StringBuilder dotGraph = new StringBuilder("digraph G {\n");
        for (Map.Entry<String, Map<String, Double>> entry : couplingGraph.entrySet()) {
            String fromClass = entry.getKey();
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                String toClass = innerEntry.getKey();
                double weight = innerEntry.getValue();
                dotGraph.append(String.format("  \"%s\" -> \"%s\" [label=\"%.2f\"];\n", fromClass, toClass, weight));
            }
        }
        dotGraph.append("}");

        try {
            MutableGraph g = new Parser().read(dotGraph.toString());
            File outputFile = new File("coupling_graph_" + System.currentTimeMillis() + ".png");
            Graphviz.fromGraph(g).width(700).render(Format.PNG).toFile(outputFile);

            displayImageInGraphPanel(outputFile.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error generating graph: " + e.getMessage());
        }
    }

    private void displayImageInGraphPanel(String filename) {
        try {
            ImageIcon icon = new ImageIcon(filename);
            graphLabel.setIcon(icon);
            
            JScrollPane scrollPane = new JScrollPane(graphLabel);
            
            JButton zoomInButton = createStyledButton("Zoom In");
            JButton zoomOutButton = createStyledButton("Zoom Out");
            JButton resetZoomButton = createStyledButton("Reset Zoom");

            zoomInButton.addActionListener(e -> updateZoom(1.1, filename));
            zoomOutButton.addActionListener(e -> updateZoom(0.9, filename));
            resetZoomButton.addActionListener(e -> resetZoom(filename));

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(backgroundColor);
            buttonPanel.add(zoomInButton);
            buttonPanel.add(zoomOutButton);
            buttonPanel.add(resetZoomButton);

            graphPanel.removeAll();
            graphPanel.add(scrollPane, BorderLayout.CENTER);
            graphPanel.add(buttonPanel, BorderLayout.SOUTH);

            tabbedPane.setSelectedIndex(1);

            graphPanel.revalidate();
            graphPanel.repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error displaying image: " + e.getMessage());
        }
    }

    private void updateZoom(double factor, String filename) {
        zoomFactor *= factor;
        updateImage(filename);
    }

    private void resetZoom(String filename) {
        zoomFactor = 1.0;
        updateImage(filename);
    }

    private void updateImage(String filename) {
        ImageIcon originalIcon = new ImageIcon(filename);
        Image originalImage = originalIcon.getImage();
        int newWidth = (int) (originalImage.getWidth(null) * zoomFactor);
        int newHeight = (int) (originalImage.getHeight(null) * zoomFactor);
        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        graphLabel.setIcon(new ImageIcon(scaledImage));
        graphLabel.revalidate();
    }

    private void performClustering() {
        if (analyzer.getCouplingGraph() == null) {
            JOptionPane.showMessageDialog(this, "Please analyze the project first.");
            return;
        }

        clusteringAnalyzer = new HierarchicalClusteringAnalyzer(analyzer.getCouplingGraph());
        resultArea.setText("Clustering Results:\n\n");

        int step = 1;
        while (clusteringAnalyzer.performNextClusteringStep()) {
            resultArea.append("Step " + step + " of clustering:\n");

            HierarchicalClusteringAnalyzer.ClusterPair mergedPair = clusteringAnalyzer.getLastMergedPair();
            if (mergedPair != null) {
                resultArea.append("Merged clusters:\n");
                resultArea.append("  Cluster 1: " + mergedPair.cluster1 + "\n");
                resultArea.append("  Cluster 2: " + mergedPair.cluster2 + "\n");
                resultArea.append("  Coupling between merged clusters: " + String.format("%.4f", mergedPair.coupling) + "\n");
            }

            resultArea.append("Current clusters:\n");
            for (HierarchicalClusteringAnalyzer.Cluster cluster : clusteringAnalyzer.getClusters()) {
                double intraCoupling = clusteringAnalyzer.calculateIntraClusterCoupling(cluster);
                resultArea.append("  " + cluster + " (Intra-cluster coupling: " + String.format("%.4f", intraCoupling) + ")\n");
            }

            double averageInterClusterCoupling = clusteringAnalyzer.calculateAverageInterClusterCoupling();
            resultArea.append("Average inter-cluster coupling: " + String.format("%.4f", averageInterClusterCoupling) + "\n\n");

            step++;
        }

        resultArea.append("Hierarchical clustering completed.\n");
        tabbedPane.setSelectedIndex(0);
    }

    private void identifyModules() {
        if (clusteringAnalyzer == null) {
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

        int totalClasses = analyzer.getCouplingGraph().size();
        moduleIdentifier = new ModuleIdentifier(analyzer.getCouplingGraph(), threshold, totalClasses);
        List<ModuleIdentifier.Module> modules = moduleIdentifier.identifyModules(clusteringAnalyzer.getClusters());

        resultArea.setText("Identified Modules:\n");
        for (int i = 0; i < modules.size(); i++) {
            ModuleIdentifier.Module module = modules.get(i);
            resultArea.append("Module " + (i + 1) + ":\n");
            resultArea.append("  Classes: " + module.getClasses() + "\n");
            resultArea.append("  Average coupling: " + String.format("%.4f", moduleIdentifier.calculateAverageCoupling(module.getClasses())) + "\n\n");
        }

        resultArea.append("Total number of modules: " + modules.size() + "\n");
        resultArea.append("Maximum allowed modules: " + moduleIdentifier.getMaxModules() + "\n");

        tabbedPane.setSelectedIndex(0);
    }

   


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new CouplingAnalyzerGUI().setVisible(true);
        });
    }
}
