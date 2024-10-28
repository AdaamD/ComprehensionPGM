package spoon;

import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;
import java.lang.*;

import java.util.*;

public class CouplingAnalyzer {
    private Map<String, Map<String, Integer>> callGraph = new HashMap<>();
    private Map<String, Map<String, Double>> couplingGraph = new HashMap<>();
    private CouplingGraphGenerator graphGenerator = new CouplingGraphGenerator();


//Réalise l'analyse de couplage d'un projet Java situé dans le répertoire projectPath
    public void analyze(String projectPath) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(17);
        launcher.addInputResource(projectPath);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.setSourceOutputDirectory("spooned");

        launcher.addProcessor(new MethodCallProcessor());
        launcher.run();

        calculateCoupling();
        printCouplingGraph();
        //generateAndExportGraph();
    }

    //Classe interne pour traiter les appels de méthodes
    private class MethodCallProcessor extends AbstractProcessor<CtClass<?>> {
        //Méthode qui est appelée pour chaque classe du projet
        @Override
        public void process(CtClass<?> ctClass) {
            String className = ctClass.getQualifiedName();
            Map<String, Integer> calledClasses = new HashMap<>();

            //Pour chaque méthode de la classe, on récupère les références aux méthodes appelées
            for (CtMethod<?> method : ctClass.getMethods()) {
                method.getElements(new TypeFilter<>(CtExecutableReference.class)).forEach(execRef -> {
                    CtType<?> declaringClass = execRef.getDeclaringType().getTypeDeclaration();
                    if (declaringClass != null) {
                        String calledClassName = declaringClass.getQualifiedName();
                        if (!calledClassName.equals(className) && !calledClassName.startsWith("java.")) {
                            calledClasses.merge(calledClassName, 1, Integer::sum);
                        }
                    }
                });
            }

            callGraph.put(className, calledClasses);
        }
    }

    //Méthode qui calcule le couplage entre les classes
    private void calculateCoupling() {
        int totalCalls = calculateTotalCalls();

        for (String classA : callGraph.keySet()) {
            for (String classB : callGraph.keySet()) {
                if (!classA.equals(classB)) {
                    int callsAB = countCalls(classA, classB);
                    if (callsAB > 0) {
                        double coupling = (double) callsAB / totalCalls;
                        couplingGraph.computeIfAbsent(classA, k -> new HashMap<>()).put(classB, coupling);
                    }
                }
            }
        }
    }

    //Méthode qui calcule le nombre total d'appels de méthodes
    private int calculateTotalCalls() {
        return callGraph.values().stream()
                .mapToInt(map -> map.values().stream().mapToInt(Integer::intValue).sum())
                .sum();
    }

    //Méthode qui compte le nombre d'appels de méthodes entre deux classes
    private int countCalls(String classA, String classB) {
        return callGraph.getOrDefault(classA, Collections.emptyMap())
                .getOrDefault(classB, 0);
    }

    //Méthode qui affiche le graphe de couplage
    private void printCouplingGraph() {
        System.out.println("Graphe de couplage pondéré :");
        for (Map.Entry<String, Map<String, Double>> entry : couplingGraph.entrySet()) {
            String classA = entry.getKey();
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                String classB = innerEntry.getKey();
                double coupling = innerEntry.getValue();
                System.out.printf("%s -> %s : %.4f%n", classA, classB, coupling);
            }
        }
    }
/*
    private void generateAndExportGraph() {
        graphGenerator.generateWeightedGraph(couplingGraph);
        String dotGraph = graphGenerator.exportGraphToDOT();
        System.out.println("\nGraphe au format DOT :");
        System.out.println(dotGraph);
    }
*/
    public Map<String, Map<String, Double>> getCouplingGraph() {
        return couplingGraph;
    }
}
