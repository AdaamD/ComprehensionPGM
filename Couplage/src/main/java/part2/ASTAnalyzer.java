package part2;

import org.eclipse.jdt.core.dom.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ASTAnalyzer {
    private List<OOMetricsVisitor.ClassInfo> allClassInfos;
    private int totalProjectLines;
    private int maxParameters;
    private int methodThreshold;
    private Map<String, Set<String>> callGraph;

    // Analyse le projet Java situé dans le répertoire spécifié et stocke les résultats
    public void analyze(String projectPath) {
        List<Path> javaFiles = findJavaFiles(projectPath);
        
        if (javaFiles.isEmpty()) {
            System.out.println("Aucun fichier Java trouvé dans le répertoire spécifié.");
            return;
        }

        // Initialisation des variables
        allClassInfos = new ArrayList<>();
        totalProjectLines = 0;
        maxParameters = 0;

        // Parcours des fichiers Java
        CallGraphVisitor callGraphVisitor = new CallGraphVisitor();

        for (Path filePath : javaFiles) {
            String source = readFile(filePath.toString());
            if (source == null) {
                System.out.println("Le fichier " + filePath + " n'a pas pu être lu ou est vide.");
                continue;
            }

            // Création de l'AST
            ASTParser parser = ASTParser.newParser(AST.JLS4);
            parser.setSource(source.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);

            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            // Visite de l'AST
            OOMetricsVisitor visitor = new OOMetricsVisitor();
            cu.accept(visitor);

            // Stockage des résultats
            allClassInfos.addAll(visitor.getClassInfoList());
            totalProjectLines += visitor.getTotalLineCount();
            maxParameters = Math.max(maxParameters, visitor.getMaxParameters());

            // Visite de l'AST pour construire le graphe d'appel
            cu.accept(callGraphVisitor);
        }

        this.callGraph = callGraphVisitor.getCallGraph();
    }

    // Méthode pour obtenir le nombre total de classes du projet
    public int getTotalClasses() {
        return allClassInfos.size();
    }

    // Méthode pour obtenir le nombre total de lignes de code du projet
    public int getTotalLines() {
        return totalProjectLines;
    }

    // Méthode pour obtenir le nombre total de méthodes du projet
    public int getTotalMethods() {
        return allClassInfos.stream().mapToInt(c -> c.methodCount).sum();
    }

    // Méthode pour obtenir le nombre total de packages du projet
    public int getTotalPackages() {
        return (int) allClassInfos.stream().map(c -> c.packageName).distinct().count();
    }

    // Méthodes pour obtenir les moyennes des métriques
    public double getAvgMethodsPerClass() {
        return getTotalClasses() > 0 ? (double) getTotalMethods() / getTotalClasses() : 0;
    }

    // Méthode pour obtenir le nombre moyen de lignes de code par méthode
    public double getAvgLinesPerMethod() {
        return getTotalMethods() > 0 ? (double) getTotalLines() / getTotalMethods() : 0;
    }

    // Méthode pour obtenir le nombre moyen d'attributs par classe
    public double getAvgAttributesPerClass() {
        int totalAttributes = allClassInfos.stream().mapToInt(c -> c.attributeCount).sum();
        return getTotalClasses() > 0 ? (double) totalAttributes / getTotalClasses() : 0;
    }

    // Méthodes pour obtenir les classes avec le plus grand nombre de méthodes et d'attributs
    public List<OOMetricsVisitor.ClassInfo> getTopMethodClasses() {
        int topClassesCount = Math.max(1, getTotalClasses() / 10);
        return allClassInfos.stream()
                .sorted((c1, c2) -> Integer.compare(c2.methodCount, c1.methodCount))
                .limit(topClassesCount)
                .collect(Collectors.toList());
    }

    // Méthode pour obtenir les classes avec le plus grand nombre d'attributs
    public List<OOMetricsVisitor.ClassInfo> getTopAttributeClasses() {
        int topClassesCount = Math.max(1, getTotalClasses() / 10);
        return allClassInfos.stream()
                .sorted((c1, c2) -> Integer.compare(c2.attributeCount, c1.attributeCount))
                .limit(topClassesCount)
                .collect(Collectors.toList());
    }

    // Méthode pour obtenir les classes avec le plus grand nombre de méthodes et d'attributs
    public List<OOMetricsVisitor.ClassInfo> getTopMethodAndAttributeClasses() {
        List<OOMetricsVisitor.ClassInfo> topMethodClasses = getTopMethodClasses();
        List<OOMetricsVisitor.ClassInfo> topAttributeClasses = getTopAttributeClasses();
        return allClassInfos.stream()
                .filter(c -> topMethodClasses.contains(c) && topAttributeClasses.contains(c))
                .collect(Collectors.toList());
    }

    // Méthode pour obtenir les classes avec un nombre de méthodes supérieur à la valeur spécifiée
    public List<OOMetricsVisitor.ClassInfo> getClassesWithManyMethods() {
        return allClassInfos.stream()
                .filter(c -> c.methodCount > methodThreshold)
                .collect(Collectors.toList());
    }

    // Méthode pour obtenir le nombre maximal de paramètres par rapport à toutes les méthodes de l'application
    public int getMaxParameters() {
        return maxParameters;
    }

    // Méthodes pour obtenir les informations des classes et le graphe d'appel
    public List<OOMetricsVisitor.ClassInfo> getAllClassInfos() {
        return allClassInfos;
    }

    // Méthode pour obtenir le graphe d'appel
    public Map<String, Set<String>> getCallGraph() {
        return callGraph;
    }

    // Méthode pour afficher les résultats de l'analyse
    public void printResults() {
       
        printCallGraph();
    }

    // Méthode pour afficher le graphe d'appel
    public void printCallGraph() {
        System.out.println("\n####################");

        System.out.println("\nGraphe d'appel :\n");
        for (Map.Entry<String, Set<String>> entry : callGraph.entrySet()) {    	
        	if (!entry.getValue().isEmpty()) {
                System.out.println(" Méthode :" +entry.getKey() + " appelle : " + entry.getValue());
            }
        }
    }

    // Méthode main pour lancer l'analyse
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Veuillez fournir le chemin du répertoire du projet Java à analyser et la valeur de X pour le nombre de méthodes.");
            return;
        }

        String projectPath = args[0];
        int methodThreshold = Integer.parseInt(args[1]);

        ASTAnalyzer analyzer = new ASTAnalyzer();
        analyzer.analyze(projectPath);
        analyzer.printResults();
    }

    // Méthodes utilitaires pour lire les fichiers Java et trouver les fichiers Java dans un répertoire
    private static List<Path> findJavaFiles(String projectPath) {
        try {
            return Files.walk(Paths.get(projectPath))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Erreur lors de la recherche des fichiers Java : " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // Méthode pour lire un fichier
    private static String readFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
            return null;
        }
    }
}
