package com.example;

import java.util.*;

public class ModuleIdentifier {
    private final Map<String, Map<String, Double>> couplingGraph;
    private final double couplingThreshold;
    private final int maxModules;

    public ModuleIdentifier(Map<String, Map<String, Double>> couplingGraph, double couplingThreshold, int totalClasses) {
        this.couplingGraph = couplingGraph;
        this.couplingThreshold = couplingThreshold;
        this.maxModules = totalClasses / 2;
    }

    public double getCouplingThreshold() {
		return couplingThreshold;
	}

	public int getMaxModules() {
		return maxModules;
	}

    // Méthode pour identifier les modules à partir des clusters
	public List<Module> identifyModules(List<HierarchicalClusteringAnalyzer.Cluster> clusters) {
        List<Module> modules = new ArrayList<>();

        // Identifier les modules valides
        for (HierarchicalClusteringAnalyzer.Cluster cluster : clusters) {
            if (isValidModule(cluster)) {
                modules.add(new Module(cluster.getClasses()));
            }
        }

        // Fusionner les petits modules si nécessaire
        while (modules.size() > maxModules) {
            mergeLeastCoupledModules(modules);
        }

        return modules;
    }

    // Méthode pour vérifier si un cluster est un module valide
    private boolean isValidModule(HierarchicalClusteringAnalyzer.Cluster cluster) {
        double averageCoupling = calculateAverageCoupling(cluster.getClasses());
        return averageCoupling > couplingThreshold;
    }

    // Méthode pour calculer le couplage moyen entre les classes d'un module
    public double calculateAverageCoupling(Set<String> classes) {
        double totalCoupling = 0;
        int count = 0;
        
        for (String class1 : classes) {
            for (String class2 : classes) {
                if (!class1.equals(class2)) {
                    totalCoupling += getCouplingBetweenClasses(class1, class2);
                    count++;
                }
            }
        }
        
        return count > 0 ? totalCoupling / count : 0;
    }

    // Méthode pour obtenir le couplage entre deux classes
    private double getCouplingBetweenClasses(String class1, String class2) {
        if (couplingGraph.containsKey(class1) && couplingGraph.get(class1).containsKey(class2)) {
            return couplingGraph.get(class1).get(class2);
        }
        return 0;
    }

    // Méthode pour fusionner les modules les moins couplés
    private void mergeLeastCoupledModules(List<Module> modules) {
        double minInterModuleCoupling = Double.MAX_VALUE;
        Module module1ToMerge = null;
        Module module2ToMerge = null;

        for (int i = 0; i < modules.size(); i++) {
            for (int j = i + 1; j < modules.size(); j++) {
                double interModuleCoupling = calculateInterModuleCoupling(modules.get(i), modules.get(j));
                if (interModuleCoupling < minInterModuleCoupling) {
                    minInterModuleCoupling = interModuleCoupling;
                    module1ToMerge = modules.get(i);
                    module2ToMerge = modules.get(j);
                }
            }
        }

        if (module1ToMerge != null && module2ToMerge != null) {
            modules.remove(module2ToMerge);
            module1ToMerge.merge(module2ToMerge);
        }
    }

    // Méthode pour calculer le couplage entre deux modules
    private double calculateInterModuleCoupling(Module module1, Module module2) {
        double totalCoupling = 0;
        int count = 0;

        for (String class1 : module1.getClasses()) {
            for (String class2 : module2.getClasses()) {
                totalCoupling += getCouplingBetweenClasses(class1, class2);
                count++;
            }
        }

        return count > 0 ? totalCoupling / count : 0;
    }

    public static class Module {
        private Set<String> classes;

        public Module(Set<String> classes) {
            this.classes = new HashSet<>(classes);
        }

        public void merge(Module other) {
            this.classes.addAll(other.classes);
        }

        public Set<String> getClasses() {
            return classes;
        }

        @Override
        public String toString() {
            return "Module: " + classes;
        }
    }
}
