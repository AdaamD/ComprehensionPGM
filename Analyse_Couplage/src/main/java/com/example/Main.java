package com.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {
    	if (args.length < 2) {
            System.out.println("Veuillez spécifier le chemin du projet à analyser et le seuil de couplage.");
            return;
        }

        String projectPath = args[0];
        double couplingThreshold = Double.parseDouble(args[1]);
        
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        analyzer.analyzeProject(projectPath);
        analyzer.printCallGraph();
        analyzer.calculateCoupling();

        // Utilisation de HierarchicalClusteringAnalyzer
        HierarchicalClusteringAnalyzer clusteringAnalyzer = new HierarchicalClusteringAnalyzer(analyzer.getCouplingGraph());

        System.out.println("\nRésultats du clustering hiérarchique :");
        int step = 1;
        while (clusteringAnalyzer.performNextClusteringStep()) {
            System.out.println("\n\nÉtape " + step + " du clustering :");
            
            // Afficher les clusters fusionnés et leur couplage
            HierarchicalClusteringAnalyzer.ClusterPair mergedPair = clusteringAnalyzer.getLastMergedPair();
            if (mergedPair != null) {
                System.out.println("Clusters fusionnés : ");
                System.out.println("  Cluster 1: " + mergedPair.cluster1);
                System.out.println("  Cluster 2: " + mergedPair.cluster2);
                System.out.println("  Couplage entre les clusters fusionnés : " + String.format("%.4f", mergedPair.coupling));
            }

            System.out.println("\nClusters actuels :");
            for (HierarchicalClusteringAnalyzer.Cluster cluster : clusteringAnalyzer.getClusters()) {
                double intraCoupling = clusteringAnalyzer.calculateIntraClusterCoupling(cluster);
                System.out.println("  " + cluster + " (Couplage intra-cluster : " + String.format("%.4f", intraCoupling) + ")");
            }

            // Afficher le couplage moyen entre les clusters restants
            double averageInterClusterCoupling = clusteringAnalyzer.calculateAverageInterClusterCoupling();
            System.out.println("Couplage moyen entre les clusters restants : " + String.format("%.4f", averageInterClusterCoupling));

            System.out.println();
            step++;
        }

        System.out.println("Clustering hiérarchique terminé.");
        
        // Utilisation de ModuleIdentifier
        int totalClasses = analyzer.getCouplingGraph().size();
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(analyzer.getCouplingGraph(), couplingThreshold, totalClasses);
        List<ModuleIdentifier.Module> modules = moduleIdentifier.identifyModules(clusteringAnalyzer.getClusters());

        System.out.println("\nModules identifiés :");
        for (int i = 0; i < modules.size(); i++) {
            ModuleIdentifier.Module module = modules.get(i);
            System.out.println("Module " + (i + 1) + ":");
            System.out.println("  Classes : " + module.getClasses());
            System.out.println("  Couplage moyen : " + String.format("%.4f", moduleIdentifier.calculateAverageCoupling(module.getClasses())));
            System.out.println();
        }

        System.out.println("Nombre total de modules : " + modules.size());
        System.out.println("Nombre maximum de modules autorisés : " + moduleIdentifier.getMaxModules());
    }
    
}
