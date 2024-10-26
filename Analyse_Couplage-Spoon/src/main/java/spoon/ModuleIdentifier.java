package spoon;

import spoon.Launcher;
import spoon.reflect.declaration.CtType;
import java.util.*;

public class ModuleIdentifier {
    private Launcher launcher;
    private Map<String, Map<String, Double>> couplingGraph;
    private int maxModules;
    private double couplingThreshold;

    public ModuleIdentifier(Launcher launcher, Map<String, Map<String, Double>> couplingGraph, double couplingThreshold) {
        this.launcher = launcher;
        this.couplingGraph = couplingGraph;
        this.maxModules = launcher.getModel().getAllTypes().size() / 2;
        this.couplingThreshold = couplingThreshold;
    }

    public List<Module> identifyModules(List<List<CtType<?>>> hierarchicalClusters) {
        List<Module> modules = new ArrayList<>();
        List<CtType<?>> allClasses = hierarchicalClusters.get(hierarchicalClusters.size() - 1);

        // Commencer avec chaque classe comme un module séparé
        for (CtType<?> ctType : allClasses) {
            modules.add(new Module(Collections.singletonList(ctType)));
        }

        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < modules.size(); i++) {
                for (int j = i + 1; j < modules.size(); j++) {
                    Module moduleA = modules.get(i);
                    Module moduleB = modules.get(j);
                    double coupling = calculateModuleCoupling(moduleA, moduleB);
                    
                    if (coupling >= couplingThreshold) {
                        // Fusionner les modules si le couplage est supérieur au seuil
                        moduleA.addClasses(moduleB.getClasses());
                        modules.remove(j);
                        changed = true;
                        break;
                    }
                }
                if (changed) break;
            }
        } while (changed && modules.size() > 1 && modules.size() > maxModules);

        return modules;
    }

    private double calculateModuleCoupling(Module moduleA, Module moduleB) {
        double totalCoupling = 0;
        for (CtType<?> classA : moduleA.getClasses()) {
            for (CtType<?> classB : moduleB.getClasses()) {
                totalCoupling += getCouplingBetweenClasses(classA.getQualifiedName(), classB.getQualifiedName());
            }
        }
        return totalCoupling / (moduleA.size() * moduleB.size());
    }

    private double getCouplingBetweenClasses(String classA, String classB) {
        return couplingGraph.getOrDefault(classA, Collections.emptyMap()).getOrDefault(classB, 0.0) +
               couplingGraph.getOrDefault(classB, Collections.emptyMap()).getOrDefault(classA, 0.0);
    }

    public static class Module {
        private List<CtType<?>> classes;

        public Module(List<CtType<?>> classes) {
            this.classes = new ArrayList<>(classes);
        }

        public List<CtType<?>> getClasses() {
            return classes;
        }

        public void addClasses(List<CtType<?>> newClasses) {
            this.classes.addAll(newClasses);
        }

        public int size() {
            return classes.size();
        }

        @Override
        public String toString() {
            return "Module: " + classes.stream().map(CtType::getSimpleName).toList();
        }
    }
}
