package part2;

import java.util.*;

public class ModuleIdentifier {
    private CouplingCalculator couplingCalculator;
    private int maxModules;
    private double minAverageCoupling;

    public ModuleIdentifier(CouplingCalculator couplingCalculator, double cp) {
        this.couplingCalculator = couplingCalculator;
        int totalClasses = couplingCalculator.getAllClasses().size();
        this.maxModules = totalClasses / 2; // Nombre maximum de modules autorisés
        this.minAverageCoupling = cp;
    }

    public List<List<String>> identifyModules(List<HierarchicalClustering.Cluster> allClusters) {
        List<List<String>> modules = new ArrayList<>();

        // Parcourir les clusters du plus grand au plus petit
        for (int i = allClusters.size() - 1; i >= 0; i--) {
            HierarchicalClustering.Cluster cluster = allClusters.get(i);
            if (modules.size() >= maxModules) {
                break;  // Arrêter si le nombre maximum de modules est atteint
            }
            if (cluster.coupling >= minAverageCoupling && !isSubsetOfExistingModule(cluster.classes, modules)) {
                modules.add(cluster.classes);
            }
        }

        return modules;
    }

    private boolean isSubsetOfExistingModule(List<String> classes, List<List<String>> modules) {
        for (List<String> module : modules) {
            if (module.containsAll(classes)) {
                return true;
            }
        }
        return false;
    }

    public int getMaxModules() {
        return maxModules;
    }

    public static void main(String[] args) {
        ASTAnalyzer analyzer = new ASTAnalyzer();
        analyzer.analyze("C:\\Users\\adam_\\Desktop\\Evolution_Restruc_Log\\TP2\\Test_Projet");
        CouplingCalculator couplingCalculator = new CouplingCalculator(analyzer);

        HierarchicalClustering clustering = new HierarchicalClustering(couplingCalculator);
        List<HierarchicalClustering.Cluster> allClusters = clustering.performClustering();

        double cp = 0.05; // Définissez votre seuil de couplage ici
        ModuleIdentifier identifier = new ModuleIdentifier(couplingCalculator, cp);
        List<List<String>> modules = identifier.identifyModules(allClusters);

        System.out.println("Modules identifiés :");
        for (int i = 0; i < modules.size(); i++) {
            System.out.println("Module " + (i + 1) + ": " + modules.get(i));
            System.out.println("Nombre de classes : " + modules.get(i).size());
            System.out.println();
        }
        System.out.println("Nombre total de modules : " + modules.size());
        System.out.println("Nombre maximum de modules autorisés : " + identifier.getMaxModules());
        System.out.println("Nombre total de classes : " + couplingCalculator.getAllClasses().size());

    }
}
