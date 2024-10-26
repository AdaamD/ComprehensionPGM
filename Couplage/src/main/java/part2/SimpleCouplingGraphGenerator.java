package part2;

import java.util.*;
import java.io.*;

public class SimpleCouplingGraphGenerator {
    private final CouplingCalculator couplingCalculator;

    public SimpleCouplingGraphGenerator(CouplingCalculator couplingCalculator) {
        this.couplingCalculator = couplingCalculator;
    }

    public String generateCouplingGraph() {
        Map<String, Map<String, Double>> couplingMatrix = couplingCalculator.calculateCoupling();
        StringBuilder graphRepresentation = new StringBuilder("digraph CouplingGraph {\n");

        for (Map.Entry<String, Map<String, Double>> entry : couplingMatrix.entrySet()) {
            String classA = entry.getKey();
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                String classB = innerEntry.getKey();
                double coupling = innerEntry.getValue();

                if (coupling > 0) {
                    String couplingPercentage = String.format("%.2f%%", coupling * 100);
                    graphRepresentation.append("  \"")
                                       .append(classA)
                                       .append("\" -> \"")
                                       .append(classB)
                                       .append("\" [label=\"")
                                       .append(couplingPercentage)
                                       .append("\"];\n");
                }
            }
        }

        graphRepresentation.append("}");
        return graphRepresentation.toString();
    }

    public void saveGraphToFile(String graphContent, String filePath) {
        try (PrintWriter out = new PrintWriter(filePath)) {
            out.println(graphContent);
        } catch (FileNotFoundException e) {
            System.err.println("Erreur lors de l'écriture du fichier : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Veuillez entrer le chemin du projet à analyser :");
        String projectPath = scanner.nextLine();

        ASTAnalyzer analyzer = new ASTAnalyzer();
        analyzer.analyze(projectPath);

        CouplingCalculator calculator = new CouplingCalculator(analyzer);
        SimpleCouplingGraphGenerator graphGenerator = new SimpleCouplingGraphGenerator(calculator);

        String graphRepresentation = graphGenerator.generateCouplingGraph();
        System.out.println("Représentation du graphe de couplage :");
        System.out.println(graphRepresentation);

        graphGenerator.saveGraphToFile(graphRepresentation, "coupling_graph.dot");
        System.out.println("Le graphe a été sauvegardé dans 'coupling_graph.dot'");

        // Afficher la matrice de couplage
        Map<String, Map<String, Double>> couplingMatrix = calculator.calculateCoupling();
        calculator.printCouplingMatrix(couplingMatrix);

        // Afficher les classes les plus couplées
        List<CouplingCalculator.CouplingPair> topCoupled = calculator.getTopCoupledClasses(5);
        calculator.printTopCoupledClasses(topCoupled);

        scanner.close();
    }
}
