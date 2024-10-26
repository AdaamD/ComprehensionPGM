package spoon;

import spoon.reflect.declaration.CtType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String projectPath = "C:\\Users\\adam_\\Desktop\\Evolution_Restruc_Log\\TP2\\Test_Projet2";
        
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath);
        launcher.buildModel();
        
        // Étape 1 : Analyse du couplage
        System.out.println("Analyse du couplage :");
        CouplingAnalyzer couplingAnalyzer = new CouplingAnalyzer();
        couplingAnalyzer.analyze(projectPath);  // Modifié pour utiliser le launcher
        Map<String, Map<String, Double>> couplingGraph = couplingAnalyzer.getCouplingGraph();

        // Étape 2 : Génération du graphe de couplage
        System.out.println("\nGénération du graphe de couplage :");
        CouplingGraphGenerator graphGenerator = new CouplingGraphGenerator();
        graphGenerator.generateWeightedGraph(couplingGraph);
        String dotGraph = graphGenerator.exportGraphToDOT();
        System.out.println(dotGraph);

        // Étape 3 : Clustering hiérarchique
        System.out.println("\nClustering hiérarchique :");
        HierarchicalClusteringAnalyzer clusteringAnalyzer = new HierarchicalClusteringAnalyzer(launcher, couplingGraph);

        System.out.println("Début du clustering hiérarchique :");
        List<List<List<CtType<?>>>> allClusteringSteps = new ArrayList<>();
        while (clusteringAnalyzer.performNextClusteringStep()) {
            allClusteringSteps.add(clusteringAnalyzer.getCurrentClusters());
        }
        System.out.println("Clustering hiérarchique terminé.");

        // Récupération du résultat final du clustering
        List<List<CtType<?>>> finalClusters = allClusteringSteps.isEmpty() ? 
            new ArrayList<>() : allClusteringSteps.get(allClusteringSteps.size() - 1);

        // Etape 4: Identification des modules
        System.out.println("\nIdentification des modules :");

        double couplingThreshold = 0.05; // Exemple de seuil de couplage

        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(launcher, couplingGraph, couplingThreshold);
        List<ModuleIdentifier.Module> modules = moduleIdentifier.identifyModules(finalClusters);

        System.out.println("Modules identifiés :");
        for (ModuleIdentifier.Module module : modules) {
            System.out.println(module);
        }
    }
}
