package spoon;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.Launcher;

import java.util.*;
import java.util.stream.Collectors;

public class HierarchicalClusteringAnalyzer {
    private Launcher launcher;
    private List<Cluster> clusters = new ArrayList<>();
    private Map<String, Map<String, Double>> couplingGraph;
    private int step = 0;

    public HierarchicalClusteringAnalyzer(Launcher launcher, Map<String, Map<String, Double>> couplingGraph) {
        this.launcher = launcher;
        this.couplingGraph = couplingGraph;
        initializeClusters();
    }

    private void initializeClusters() {
        for (CtType<?> type : launcher.getModel().getAllTypes()) {
            if (type instanceof CtClass) {
                clusters.add(new Cluster((CtClass<?>) type));
            }
        }
    }
    
    public List<List<CtType<?>>> getCurrentClusters() {
        return clusters.stream()
                .map(cluster -> new ArrayList<CtType<?>>(cluster.getClasses()))
                .collect(Collectors.toList());
    }

    public boolean performNextClusteringStep() {
        if (clusters.size() <= 1) {
            return false; // Clustering is complete
        }

        step++;
        System.out.println("Étape " + step + " du clustering :");

        Pair<Cluster, Cluster> mostCoupledPair = findMostCoupledClusters();
        if (mostCoupledPair == null) {
            System.out.println("Aucune paire de clusters couplés trouvée.");
            return false;
        }

        double couplingValue = calculateClusterCoupling(mostCoupledPair.first, mostCoupledPair.second);
        System.out.println("Regroupement des clusters : " + mostCoupledPair.first + " et " + mostCoupledPair.second);
        System.out.println("Valeur de couplage : " + String.format("%.4f", couplingValue));

        mergeClusters(mostCoupledPair.first, mostCoupledPair.second);
        printCurrentClustersWithCoupling();

        return true;
    }

    private void printCurrentClustersWithCoupling() {
        System.out.println("Clusters actuels :");
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            double intraCoupling = calculateIntraClusterCoupling(cluster);
            System.out.println("Cluster " + (i + 1) + ": " + cluster + 
                               " (Couplage intra-cluster : " + String.format("%.4f", intraCoupling) + ")");
        }
        System.out.println();
    }

    public double calculateIntraClusterCoupling(Cluster cluster) {
        double totalCoupling = 0;
        int pairs = 0;
        List<CtType<?>> classes = new ArrayList<>(cluster.getClasses());
        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                totalCoupling += getCouplingBetweenClasses(
                    classes.get(i).getQualifiedName(), 
                    classes.get(j).getQualifiedName()
                );
                pairs++;
            }
        }
        return pairs > 0 ? totalCoupling / pairs : 0;
    }
   
    
    
    

    Pair<Cluster, Cluster> findMostCoupledClusters() {
        Pair<Cluster, Cluster> mostCoupledPair = null;
        double maxCoupling = 0;

        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                Cluster clusterA = clusters.get(i);
                Cluster clusterB = clusters.get(j);
                double coupling = calculateClusterCoupling(clusterA, clusterB);
                if (coupling > maxCoupling) {
                    maxCoupling = coupling;
                    mostCoupledPair = new Pair<>(clusterA, clusterB);
                }
            }
        }

        return mostCoupledPair;
    }

    private double calculateClusterCoupling(Cluster clusterA, Cluster clusterB) {
        double totalCoupling = 0;
        for (CtClass<?> classA : clusterA.getClasses()) {
            for (CtClass<?> classB : clusterB.getClasses()) {
                totalCoupling += getCouplingBetweenClasses(classA.getQualifiedName(), classB.getQualifiedName());
            }
        }
        return totalCoupling / (clusterA.size() * clusterB.size());
    }

    private double getCouplingBetweenClasses(String classA, String classB) {
        return couplingGraph.getOrDefault(classA, Collections.emptyMap()).getOrDefault(classB, 0.0) +
               couplingGraph.getOrDefault(classB, Collections.emptyMap()).getOrDefault(classA, 0.0);
    }

    private void mergeClusters(Cluster clusterA, Cluster clusterB) {
        Cluster newCluster = new Cluster(clusterA, clusterB);
        clusters.remove(clusterA);
        clusters.remove(clusterB);
        clusters.add(newCluster);
    }

    private void printCurrentClusters() {
        System.out.println("Clusters actuels :");
        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster " + (i + 1) + ": " + clusters.get(i));
        }
        System.out.println();
    }

    static class Cluster {
        private Set<CtClass<?>> classes = new HashSet<>();

        public Cluster(CtClass<?> ctClass) {
            classes.add(ctClass);
        }

        public Cluster(Cluster a, Cluster b) {
            classes.addAll(a.classes);
            classes.addAll(b.classes);
        }
        
        public Cluster(List<CtType<?>> types) {
            for (CtType<?> type : types) {
                if (type instanceof CtClass) {
                    classes.add((CtClass<?>) type);
                }
            }
        }
            

        public Set<CtClass<?>> getClasses() {
            return classes;
        }

        public int size() {
            return classes.size();
        }

        @Override
        public String toString() {
            return classes.stream()
                .map(CtClass::getSimpleName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        }
    }

    private static class Pair<T, U> {
        public final T first;
        public final U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
    }
}
