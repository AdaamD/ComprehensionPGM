package part2;

import java.util.*;

public class CouplingCalculator {
    // Le graphe d'appel obtenu à partir de l'ASTAnalyzer
    private Map<String, Set<String>> callGraph;

    /**
     * Constructeur qui initialise le calculateur avec un ASTAnalyzer.
     * @param analyzer L'ASTAnalyzer contenant le graphe d'appel analysé.
     */
    public CouplingCalculator(ASTAnalyzer analyzer) {
        this.callGraph = analyzer.getCallGraph();
    }

    /**
     * Calcule la matrice de couplage entre toutes les classes.
     * @return Une map représentant la matrice de couplage.
     */
    public Map<String, Map<String, Double>> calculateCoupling() {
        Map<String, Map<String, Double>> couplingMatrix = new HashMap<>();
        Set<String> allClasses = getAllClasses();

        for (String classA : allClasses) {
            for (String classB : allClasses) {
                if (!classA.equals(classB)) {
                    double coupling = calculateCouplingBetweenClasses(classA, classB);
                    if (coupling > 0) {
                        couplingMatrix
                                .computeIfAbsent(classA, k -> new HashMap<>())
                                .put(classB, coupling);
                    }
                }
            }
        }

        return couplingMatrix;
    }

    /**
     * Obtient l'ensemble de toutes les classes uniques du graphe d'appel.
     * @return Un ensemble de noms de classes.
     */
    Set<String> getAllClasses() {
        Set<String> classes = new HashSet<>();
        for (String method : callGraph.keySet()) {
            classes.add(getClassName(method));
        }
        for (Set<String> calledMethods : callGraph.values()) {
            for (String calledMethod : calledMethods) {
                classes.add(getClassName(calledMethod));
            }
        }
        return classes;
    }

    /**
     * Extrait le nom de la classe à partir du nom complet d'une méthode.
     * @param methodName Le nom complet de la méthode.
     * @return Le nom de la classe.
     */
    private String getClassName(String methodName) {
        int lastDotIndex = methodName.lastIndexOf('.');
        return lastDotIndex != -1 ? methodName.substring(0, lastDotIndex) : methodName;
    }

    /**
     * Calcule le couplage entre deux classes spécifiques.
     * @param classA Le nom de la première classe.
     * @param classB Le nom de la deuxième classe.
     * @return La valeur de couplage entre les deux classes.
     */
    private double calculateCouplingBetweenClasses(String classA, String classB) {
        int couplingAB = 0;
        int totalCouplings = 0;

        for (Map.Entry<String, Set<String>> entry : callGraph.entrySet()) {
            String callerClass = getClassName(entry.getKey());

            for (String calledMethod : entry.getValue()) {
                String calledClass = getClassName(calledMethod);

                if (!callerClass.equals(calledClass)) {
                    totalCouplings++;
                    if ((callerClass.equals(classA) && calledClass.equals(classB)) ||
                            (callerClass.equals(classB) && calledClass.equals(classA))) {
                        couplingAB++;
                    }
                }
            }
        }

        return totalCouplings > 0 ? (double) couplingAB / totalCouplings : 0.0;
    }

    /**
     * Affiche la matrice de couplage.
     * @param couplingMatrix La matrice de couplage à afficher.
     */
    public void printCouplingMatrix(Map<String, Map<String, Double>> couplingMatrix) {
        System.out.println("Matrice de couplage (en pourcentage) :");
        for (Map.Entry<String, Map<String, Double>> entry : couplingMatrix.entrySet()) {
            String classA = entry.getKey();
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                String classB = innerEntry.getKey();
                double coupling = innerEntry.getValue();
                System.out.printf("Couplage entre %s et %s : %.2f%%%n", classA, classB, coupling * 100);
            }
        }
    }

    /**
     * Obtient le couplage entre deux classes spécifiques.
     * @param classA Le nom de la première classe.
     * @param classB Le nom de la deuxième classe.
     * @return La valeur de couplage entre les deux classes.
     */
    public double getCouplingBetweenClasses(String classA, String classB) {
        return calculateCouplingBetweenClasses(classA, classB);
    }


    /**
     * Obtient les N paires de classes les plus couplées.
     * @param topN Le nombre de paires à retourner.
     * @return Une liste des N paires de classes les plus couplées.
     */
    public List<CouplingPair> getTopCoupledClasses(int topN) {
        List<CouplingPair> couplingPairs = new ArrayList<>();
        Map<String, Map<String, Double>> couplingMatrix = calculateCoupling();

        for (Map.Entry<String, Map<String, Double>> entry : couplingMatrix.entrySet()) {
            String classA = entry.getKey();
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                String classB = innerEntry.getKey();
                double coupling = innerEntry.getValue();
                couplingPairs.add(new CouplingPair(classA, classB, coupling));
            }
        }

        couplingPairs.sort((a, b) -> Double.compare(b.coupling, a.coupling));
        return couplingPairs.subList(0, Math.min(topN, couplingPairs.size()));
    }



    /**
     * Affiche les N paires de classes les plus couplées.
     * @param topCoupled La liste des paires de classes les plus couplées.
     */
    public void printTopCoupledClasses(List<CouplingPair> topCoupled) {
        System.out.println("\nTop 5 des classes les plus couplées (en pourcentage) :");
        for (int i = 0; i < topCoupled.size(); i++) {
            CouplingPair pair = topCoupled.get(i);
            System.out.printf("%d. %s - %s : %.2f%%%n", i + 1, pair.classA, pair.classB, pair.coupling * 100);
        }
    }

    /**
     * Classe interne représentant une paire de classes couplées.
     */
    public static class CouplingPair {
        public final String classA;
        public final String classB;
        public final double coupling;

        public CouplingPair(String classA, String classB, double coupling) {
            this.classA = classA;
            this.classB = classB;
            this.coupling = coupling;
        }

        @Override
        public String toString() {
            return String.format("%s - %s : %.4f", classA, classB, coupling);
        }
    }

    /**
     * Méthode principale pour exécuter le calculateur de couplage.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Veuillez entrer le chemin du projet à analyser :");
        String projectPath = scanner.nextLine();

        ASTAnalyzer analyzer = new ASTAnalyzer();
        analyzer.analyze(projectPath);

        CouplingCalculator calculator = new CouplingCalculator(analyzer);
        Map<String, Map<String, Double>> couplingMatrix = calculator.calculateCoupling();

        calculator.printCouplingMatrix(couplingMatrix);

        List<CouplingPair> topCoupled = calculator.getTopCoupledClasses(5);
        calculator.printTopCoupledClasses(topCoupled);

        scanner.close();
    }
}