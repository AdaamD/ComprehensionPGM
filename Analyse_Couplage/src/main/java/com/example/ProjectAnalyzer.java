package com.example;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ProjectAnalyzer {
    private final Map<String, Map<String, Map<String, Integer>>> callGraph = new HashMap<>();
    private Map<String, Map<String, Double>> couplingGraph = new HashMap<>();

    // Les packages standard Java à ignorer
    private static final List<String> IGNORED_PACKAGES = Arrays.asList(
        "java.", "javax.", "sun.", "com.sun.", "org.w3c.", "org.xml."
    );

    // Méthode pour analyser un projet Java
    public void analyzeProject(String projectPath) {
        try {
            // Configurez le classpath pour inclure les fichiers du projet analysé
            String[] classpath = { projectPath };
            String[] sources = { projectPath };
            
            ASTParser parser = ASTParser.newParser(AST.JLS14);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
            
            Files.walk(Paths.get(projectPath))
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> analyzeFile(path, parser));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println("Call graph size: " + callGraph.size());
        for (Map.Entry<String, Map<String, Map<String, Integer>>> entry : callGraph.entrySet()) {
            System.out.println("Class: " + entry.getKey() + ", Methods: " + entry.getValue().size());
        }

    }

    // Méthode pour analyser un fichier Java
    private void analyzeFile(Path filePath, ASTParser parser) {
        try {
            String content = new String(Files.readAllBytes(filePath));
            parser.setSource(content.toCharArray());
            parser.setUnitName(filePath.toString());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setEnvironment(null, null, null, true);
            
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            
            CallGraphVisitor visitor = new CallGraphVisitor();
            cu.accept(visitor);
            mergeCallGraphs(visitor.getCallGraph());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour fusionner les graphes d'appels locaux
    private void mergeCallGraphs(Map<String, Map<String, Map<String, Integer>>> localGraph) {
        for (Map.Entry<String, Map<String, Map<String, Integer>>> classEntry : localGraph.entrySet()) {
            String className = classEntry.getKey();
            for (Map.Entry<String, Map<String, Integer>> methodEntry : classEntry.getValue().entrySet()) {
                String methodName = methodEntry.getKey();
                for (Map.Entry<String, Integer> callEntry : methodEntry.getValue().entrySet()) {
                    callGraph.computeIfAbsent(className, k -> new HashMap<>())
                             .computeIfAbsent(methodName, k -> new HashMap<>())
                             .merge(callEntry.getKey(), callEntry.getValue(), Integer::sum);
                }
            }
        }
    }

    // Méthode pour afficher le graphe d'appels
    public void printCallGraph() {
        for (Map.Entry<String, Map<String, Map<String, Integer>>> classEntry : callGraph.entrySet()) {
            System.out.println("Classe " + classEntry.getKey() + ":");
            for (Map.Entry<String, Map<String, Integer>> methodEntry : classEntry.getValue().entrySet()) {
                System.out.println("  Méthode " + methodEntry.getKey() + " appelle:");
                for (Map.Entry<String, Integer> callEntry : methodEntry.getValue().entrySet()) {
                    System.out.println("    - " + callEntry.getKey() + " (" + callEntry.getValue() + " fois)");
                }
            }
        }
    }

    // Méthode pour calculer le couplage entre les classes
    public void calculateCoupling() {
        couplingGraph.clear();
        int totalRelations = 0;

        for (Map.Entry<String, Map<String, Map<String, Integer>>> classEntry : callGraph.entrySet()) {
            String callerClass = classEntry.getKey();
            for (Map.Entry<String, Map<String, Integer>> methodEntry : classEntry.getValue().entrySet()) {
                for (Map.Entry<String, Integer> callEntry : methodEntry.getValue().entrySet()) {
                    String[] calledParts = callEntry.getKey().split("\\.");
                    if (calledParts.length < 2) continue; // Ignorer les appels mal formés
                    String calledClass = calledParts[0];
                    int calls = callEntry.getValue();

                    if (!callerClass.equals(calledClass) && !isIgnoredClass(calledClass)) {
                        couplingGraph.computeIfAbsent(callerClass, k -> new HashMap<>())
                                     .merge(calledClass, (double) calls, Double::sum);
                        totalRelations += calls;
                    }
                }
            }
        }

        // Normaliser les valeurs de couplage
        if (totalRelations > 0) {
            for (Map<String, Double> innerMap : couplingGraph.values()) {
                for (Map.Entry<String, Double> entry : innerMap.entrySet()) {
                    entry.setValue(entry.getValue() / totalRelations);
                }
            }
        }
    }


    public Map<String, Map<String, Double>> getCouplingGraph() {
        return couplingGraph;
    }


// Méthode pour vérifier si une classe doit être ignorée
    private boolean isIgnoredClass(String className) {
        // Utilisez la même logique que dans CallGraphVisitor
        Set<String> ignoredClasses = new HashSet<>(Arrays.asList(
            "String", "Integer", "Long", "Double", "Float", "Boolean",
            "List", "ArrayList", "LinkedList", "Map", "HashMap", "Set", "HashSet",
            "PrintStream", "System"
        ));
        
        if (ignoredClasses.contains(className)) {
            return true;
        }
        
        if (!className.contains(".")) {
            return false;
        }
        return IGNORED_PACKAGES.stream().anyMatch(className::startsWith);
    }
}
