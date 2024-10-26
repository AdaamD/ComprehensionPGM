package part2;

import java.util.*;

public class HierarchicalClustering {

    public static class Cluster {
        List<String> classes;
        double coupling;

        public Cluster(List<String> classes, double coupling) {
            this.classes = classes;
            this.coupling = coupling;
        }

        public Cluster(String className) {
            this.classes = new ArrayList<>(Collections.singletonList(className));
            this.coupling = 0;
        }
    }

    private CouplingCalculator couplingCalculator;

    public HierarchicalClustering(CouplingCalculator couplingCalculator) {
        this.couplingCalculator = couplingCalculator;
    }

    public List<Cluster> performClustering() {
        Set<String> classNames = couplingCalculator.getAllClasses();
        List<Cluster> clusters = new ArrayList<>();
        for (String className : classNames) {
            clusters.add(new Cluster(className));
        }

        List<Cluster> allClusters = new ArrayList<>();

        int step = 1;
        while (clusters.size() > 1) {
            Pair<Cluster, Cluster> closestPair = findClosestClusters(clusters);
            Cluster c1 = closestPair.getFirst();
            Cluster c2 = closestPair.getSecond();

            System.out.println("Étape " + step + ": Regroupement de " + c1.classes + " et " + c2.classes);

            List<String> mergedClasses = new ArrayList<>(c1.classes);
            mergedClasses.addAll(c2.classes);
            double newCoupling = calculateClusterCoupling(mergedClasses);
            Cluster c3 = new Cluster(mergedClasses, newCoupling);

            clusters.remove(c1);
            clusters.remove(c2);
            clusters.add(c3);
            
            // N'ajouter que les clusters avec plus d'un élément
            if (c3.classes.size() > 1) {
                allClusters.add(c3);
                System.out.println("Nouveau cluster formé: " + c3.classes);
                System.out.printf("Couplage du nouveau cluster: %.2f%%\n", c3.coupling * 100);
                System.out.println();
            }

            step++;
        }

        return allClusters;
    }




    private Pair<Cluster, Cluster> findClosestClusters(List<Cluster> clusters) {
        Cluster closestC1 = null, closestC2 = null;
        double maxCoupling = -1;

        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                Cluster c1 = clusters.get(i);
                Cluster c2 = clusters.get(j);
                double coupling = calculateInterClusterCoupling(c1, c2);
                if (coupling > maxCoupling) {
                    maxCoupling = coupling;
                    closestC1 = c1;
                    closestC2 = c2;
                }
            }
        }

        return new Pair<>(closestC1, closestC2);
    }

    private double calculateInterClusterCoupling(Cluster c1, Cluster c2) {
        double totalCoupling = 0;
        for (String class1 : c1.classes) {
            for (String class2 : c2.classes) {
                totalCoupling += couplingCalculator.getCouplingBetweenClasses(class1, class2);
            }
        }
        return totalCoupling / (c1.classes.size() * c2.classes.size());
    }

    private double calculateClusterCoupling(List<String> classes) {
        double totalCoupling = 0;
        int count = 0;
        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                totalCoupling += couplingCalculator.getCouplingBetweenClasses(classes.get(i), classes.get(j));
                count++;
            }
        }
        return count > 0 ? totalCoupling / count : 0;
    }

    // Classe utilitaire pour représenter une paire de clusters
    private static class Pair<T, U> {
        private T first;
        private U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        public T getFirst() { return first; }
        public U getSecond() { return second; }
    }

    // Méthode principale pour tester
    public static void main(String[] args) {
    	// Supposons que vous ayez déjà un ASTAnalyzer et un CouplingCalculator
    	ASTAnalyzer analyzer = new ASTAnalyzer();
    	analyzer.analyze("C:\\Users\\adam_\\Desktop\\Evolution_Restruc_Log\\TP2\\Test_Projet");
    	CouplingCalculator couplingCalculator = new CouplingCalculator(analyzer);

    	HierarchicalClustering clustering = new HierarchicalClustering(couplingCalculator);
    	List<Cluster> allClusters = clustering.performClustering();

    	// Affichage du résultat
    	System.out.println("Clustering hiérarchique terminé. Clusters formés :");
    	for (int i = 0; i < allClusters.size(); i++) {
    	    Cluster cluster = allClusters.get(i);
    	    System.out.println("Cluster " + (i + 1) + ":");
    	    for (String className : cluster.classes) {
    	        System.out.println("  " + className);
    	    }
    	    System.out.printf("Couplage du cluster : %.2f%%\n", cluster.coupling * 100);
    	    System.out.println();
    	}

    	// Affichage du cluster final (le dernier de la liste)
    	Cluster finalCluster = allClusters.get(allClusters.size() - 1);
    	System.out.println("Cluster final :");
    	for (String className : finalCluster.classes) {
    	    System.out.println(className);
    	}
    	System.out.printf("Couplage final : %.2f%%\n", finalCluster.coupling * 100);

    }
}
