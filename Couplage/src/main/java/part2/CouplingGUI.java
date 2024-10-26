package part2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Base64;


public class CouplingGUI extends JFrame {
    private CouplingCalculator calculator;
    private ASTAnalyzer analyzer;
    private SimpleCouplingGraphGenerator graphGenerator;
    private JTextField pathField;
    private JTextArea couplingMatrixArea;
    private JTextArea topCoupledArea;
    private JTextArea dotContentArea;

    public CouplingGUI() {
        this.analyzer = new ASTAnalyzer();
        initUI();
    }

    private void initUI() {
        setTitle("Analyse de Couplage");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 240, 250));

        // Panneau pour l'entrée du chemin du projet
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.setBackground(new Color(220, 220, 240));
        JLabel pathLabel = new JLabel("Chemin du projet:");
        pathField = new JTextField(30);
        JButton browseButton = new JButton("Parcourir");
        browseButton.addActionListener(this::browseForFolder);
        inputPanel.add(pathLabel);
        inputPanel.add(pathField);
        inputPanel.add(browseButton);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Panneau pour les résultats textuels
        JPanel textResultsPanel = new JPanel(new GridLayout(3, 1));
        
        couplingMatrixArea = new JTextArea();
        couplingMatrixArea.setEditable(false);
        couplingMatrixArea.setBackground(new Color(250, 250, 255));
        JScrollPane matrixScrollPane = new JScrollPane(couplingMatrixArea);
        matrixScrollPane.setBorder(BorderFactory.createTitledBorder("Matrice de Couplage"));
        
        topCoupledArea = new JTextArea();
        topCoupledArea.setEditable(false);
        topCoupledArea.setBackground(new Color(250, 250, 255));
        JScrollPane topCoupledScrollPane = new JScrollPane(topCoupledArea);
        topCoupledScrollPane.setBorder(BorderFactory.createTitledBorder("Top 5 des Classes les Plus Couplées"));

        dotContentArea = new JTextArea();
        dotContentArea.setEditable(false);
        dotContentArea.setBackground(new Color(250, 255, 250));
        JScrollPane dotScrollPane = new JScrollPane(dotContentArea);
        dotScrollPane.setBorder(BorderFactory.createTitledBorder("Contenu DOT"));

        textResultsPanel.add(matrixScrollPane);
        textResultsPanel.add(topCoupledScrollPane);
        textResultsPanel.add(dotScrollPane);

        mainPanel.add(textResultsPanel, BorderLayout.CENTER);

        // Boutons pour lancer l'analyse et ouvrir GraphvizOnline
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(220, 220, 240));
        JButton analyzeButton = new JButton("Analyser le Couplage");
        analyzeButton.addActionListener(e -> analyzeProject());
        JButton openGraphvizButton = new JButton("Ouvrir dans GraphvizOnline");
        openGraphvizButton.addActionListener(this::openInGraphvizOnline);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(openGraphvizButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void browseForFolder(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            pathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void analyzeProject() {
        String projectPath = pathField.getText();
        if (projectPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer un chemin de projet valide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            analyzer.analyze(projectPath);
            calculator = new CouplingCalculator(analyzer);
            graphGenerator = new SimpleCouplingGraphGenerator(calculator);
            updateResults();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'analyse du projet: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateResults() {
        Map<String, Map<String, Double>> couplingMatrix = calculator.calculateCoupling();
        List<CouplingCalculator.CouplingPair> topCoupled = calculator.getTopCoupledClasses(5);

        updateCouplingMatrix(couplingMatrix);
        updateTopCoupled(topCoupled);
        updateDotContent();
    }

    private void updateCouplingMatrix(Map<String, Map<String, Double>> couplingMatrix) {
        StringBuilder matrixText = new StringBuilder("Matrice de Couplage:\n");
        for (Map.Entry<String, Map<String, Double>> entry : couplingMatrix.entrySet()) {
            String classA = entry.getKey();
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                String classB = innerEntry.getKey();
                double coupling = innerEntry.getValue();
                matrixText.append(String.format("%s -> %s : %.2f%%\n", classA, classB, coupling * 100));
            }
        }
        couplingMatrixArea.setText(matrixText.toString());
    }

    private void updateTopCoupled(List<CouplingCalculator.CouplingPair> topCoupled) {
        StringBuilder topCoupledText = new StringBuilder("Top 5 des Classes les Plus Couplées:\n");
        for (int i = 0; i < topCoupled.size(); i++) {
            CouplingCalculator.CouplingPair pair = topCoupled.get(i);
            topCoupledText.append(String.format("%d. %s - %s : %.2f%%\n", 
                i + 1, pair.classA, pair.classB, pair.coupling * 100));
        }
        topCoupledArea.setText(topCoupledText.toString());
    }

    private void updateDotContent() {
        String graphRepresentation = graphGenerator.generateCouplingGraph();
        graphGenerator.saveGraphToFile(graphRepresentation, "coupling_graph.dot");
        
        try {
            String dotContent = readDotFile("coupling_graph.dot");
            dotContentArea.setText(dotContent);
        } catch (IOException e) {
            dotContentArea.setText("Erreur lors de la lecture du fichier DOT: " + e.getMessage());
        }
    }

    private String readDotFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }


    private void openInGraphvizOnline(ActionEvent e) {
        String dotContent = dotContentArea.getText();
        
        try {
            // Encoder le contenu DOT pour l'URL
            String encodedDot = URLEncoder.encode(dotContent, StandardCharsets.UTF_8.toString());
            
            // Construire l'URL complète
            String url = "https://dreampuf.github.io/GraphvizOnline/#" + encodedDot;

            // Ouvrir l'URL dans le navigateur par défaut
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CouplingGUI gui = new CouplingGUI();
            gui.setVisible(true);
        });
    }
}
