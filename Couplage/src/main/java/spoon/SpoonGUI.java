package spoon;

import javax.swing.*;

import spoon.reflect.declaration.CtType;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;

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

    public SpoonGUI() {
        setTitle("Spoon Analysis Tool");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        projectPathField = new JTextField(20);
        browseButton = new JButton("Browse");
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        analyzeButton = new JButton("Analyze Coupling");
        generateGraphButton = new JButton("Generate Graph");
        clusterButton = new JButton("Perform Clustering");
        identifyModulesButton = new JButton("Identify Modules");
        thresholdField = new JTextField("0.05", 5);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseForProject();
            }
        });

        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                analyzeCoupling();
            }
        });

        generateGraphButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateGraph();
            }
        });

        clusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performClustering();
            }
        });

        identifyModulesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                identifyModules();
            }
        });
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Project Path:"));
        topPanel.add(projectPathField);
        topPanel.add(browseButton);
        topPanel.add(analyzeButton);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(generateGraphButton);
        centerPanel.add(clusterButton);
        centerPanel.add(new JLabel("Coupling Threshold:"));
        centerPanel.add(thresholdField);
        centerPanel.add(identifyModulesButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.SOUTH);
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
        resultArea.setText("Generated Graph (DOT format):\n" + dotGraph);
    }

    private void performClustering() {
        if (launcher == null || couplingAnalyzer == null) {
            JOptionPane.showMessageDialog(this, "Please analyze coupling first.");
            return;
        }

        clusteringAnalyzer = new HierarchicalClusteringAnalyzer(launcher, couplingAnalyzer.getCouplingGraph());
        resultArea.setText("Clustering Results:\n");
        int step = 1;
        while (clusteringAnalyzer.performNextClusteringStep()) {
            resultArea.append("Step " + step + ":\n");
            for (List<CtType<?>> cluster : clusteringAnalyzer.getCurrentClusters()) {
                resultArea.append("Cluster: ");
                for (CtType<?> type : cluster) {
                    resultArea.append(type.getSimpleName() + " ");
                }
                resultArea.append("\n");
            }
            resultArea.append("\n");
            step++;
        }
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SpoonGUI().setVisible(true);
            }
        });
    }
}
