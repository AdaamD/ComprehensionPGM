package com.example;

import java.util.*;

public class HierarchicalClusteringAnalyzer {
    private List<Cluster> clusters;
    private Map<String, Map<String, Double>> couplingGraph;
    private ClusterPair lastMergedPair;

    public HierarchicalClusteringAnalyzer(Map<String, Map<String, Double>> couplingGraph) {
        this.couplingGraph = couplingGraph;
        this.clusters = new ArrayList<>();
        
        // Initialiser chaque classe comme un cluster séparé
        for (String className : couplingGraph.keySet()) {
            clusters.add(new Cluster(className));
        }
    }

    public boolean performNextClusteringStep() {
        if (clusters.size() <= 1) {
            return false; // Le clustering est terminé
        }

        // Trouver les deux clusters les plus couplés
        double maxCoupling = -1;
        Cluster cluster1 = null;
        Cluster cluster2 = null;

        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                double coupling = calculateCouplingBetweenClusters(clusters.get(i), clusters.get(j));
                if (coupling > maxCoupling) {
                    maxCoupling = coupling;
                    cluster1 = clusters.get(i);
                    cluster2 = clusters.get(j);
                }
            }
        }

        if (cluster1 != null && cluster2 != null) {
            // Fusionner les deux clusters
            Cluster mergedCluster = new Cluster(cluster1, cluster2);
            clusters.remove(cluster1);
            clusters.remove(cluster2);
            clusters.add(mergedCluster);
            lastMergedPair = new ClusterPair(cluster1, cluster2, maxCoupling);
            return true;
        }

        return false;
    }

    private double calculateCouplingBetweenClusters(Cluster c1, Cluster c2) {
        double totalCoupling = 0;
        for (String class1 : c1.getClasses()) {
            for (String class2 : c2.getClasses()) {
                totalCoupling += getCouplingBetweenClasses(class1, class2);
            }
        }
        return totalCoupling / (c1.getClasses().size() * c2.getClasses().size());
    }

    private double getCouplingBetweenClasses(String class1, String class2) {
        if (couplingGraph.containsKey(class1) && couplingGraph.get(class1).containsKey(class2)) {
            return couplingGraph.get(class1).get(class2);
        }
        return 0;
    }

    public List<Cluster> getClusters() {
        return new ArrayList<>(clusters);
    }

    public ClusterPair getLastMergedPair() {
        return lastMergedPair;
    }

    public double calculateIntraClusterCoupling(Cluster cluster) {
        double totalCoupling = 0;
        int count = 0;
        List<String> classes = new ArrayList<>(cluster.getClasses());
        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                totalCoupling += getCouplingBetweenClasses(classes.get(i), classes.get(j));
                count++;
            }
        }
        return count > 0 ? totalCoupling / count : 0;
    }

    public double calculateAverageInterClusterCoupling() {
        double totalCoupling = 0;
        int count = 0;
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                totalCoupling += calculateCouplingBetweenClusters(clusters.get(i), clusters.get(j));
                count++;
            }
        }
        return count > 0 ? totalCoupling / count : 0;
    }

    public static class Cluster {
        private Set<String> classes;

        public Cluster(String className) {
            this.classes = new HashSet<>();
            this.classes.add(className);
        }

        public Cluster(Cluster c1, Cluster c2) {
            this.classes = new HashSet<>(c1.classes);
            this.classes.addAll(c2.classes);
        }

        public Set<String> getClasses() {
            return classes;
        }

        @Override
        public String toString() {
            return classes.toString();
        }
    }

    public static class ClusterPair {
        public final Cluster cluster1;
        public final Cluster cluster2;
        public final double coupling;

        public ClusterPair(Cluster cluster1, Cluster cluster2, double coupling) {
            this.cluster1 = cluster1;
            this.cluster2 = cluster2;
            this.coupling = coupling;
        }
    }
}
