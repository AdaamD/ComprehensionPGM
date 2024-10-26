package com.example;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class CallGraphVisitor extends ASTVisitor {
    private final Map<String, Map<String, Map<String, Integer>>> callGraph = new HashMap<>();
    private String currentClass = "";
    private String currentMethod = "";
    private Map<String, String> localVariableTypes = new HashMap<>();

    private static final List<String> IGNORED_PACKAGES = Arrays.asList(
        "java.", "javax.", "sun.", "com.sun.", "org.w3c.", "org.xml."
    );

    @Override
    public boolean visit(TypeDeclaration node) {
        currentClass = node.getName().getIdentifier();
        return true;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        currentMethod = node.getName().getIdentifier();
        localVariableTypes.clear();
        for (Object param : node.parameters()) {
            if (param instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration svd = (SingleVariableDeclaration) param;
                addVariableType(svd.getName().getIdentifier(), svd.getType());
            }
        }
        return true;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        IVariableBinding binding = node.resolveBinding();
        if (binding != null && binding.getType() != null) {
            localVariableTypes.put(node.getName().getIdentifier(), binding.getType().getName());
        }
        return true;
    }

    @Override
    public boolean visit(Assignment node) {
        if (node.getLeftHandSide() instanceof SimpleName) {
            SimpleName name = (SimpleName) node.getLeftHandSide();
            ITypeBinding typeBinding = node.getRightHandSide().resolveTypeBinding();
            if (typeBinding != null) {
                localVariableTypes.put(name.getIdentifier(), typeBinding.getName());
            }
        }
        return true;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (!currentClass.isEmpty() && !currentMethod.isEmpty()) {
            String calledClass = resolveCalledClass(node);
            if (calledClass != null && !isIgnoredClass(calledClass)) {
                String calledMethod = node.getName().getIdentifier();
                callGraph.computeIfAbsent(currentClass, k -> new HashMap<>())
                         .computeIfAbsent(currentMethod, k -> new HashMap<>())
                         .merge(calledClass + "." + calledMethod, 1, Integer::sum);
            }
        }
        return true;
    }


    private String resolveCalledClass(MethodInvocation node) {
        String resolvedClass = resolveClassInternal(node);
        if (resolvedClass != null) {
            // Vérifiez si la classe résolue est une classe interne (contient un $)
            if (resolvedClass.contains("$")) {
                resolvedClass = resolvedClass.substring(0, resolvedClass.indexOf("$"));
            }
            // Vérifiez si la classe résolue est un type générique (contient un <)
            if (resolvedClass.contains("<")) {
                resolvedClass = resolvedClass.substring(0, resolvedClass.indexOf("<"));
            }
            // Supprimez le package si c'est une classe Java standard
            if (resolvedClass.startsWith("java.") || resolvedClass.startsWith("javax.")) {
                resolvedClass = resolvedClass.substring(resolvedClass.lastIndexOf('.') + 1);
            }
            if (!isIgnoredClass(resolvedClass)) {
                return resolvedClass;
            }
        }
        return null;
    }

    private String resolveClassInternal(MethodInvocation node) {
        Expression expr = node.getExpression();
        if (expr != null) {
            if (expr instanceof SimpleName) {
                String varName = ((SimpleName) expr).getIdentifier();
                String type = localVariableTypes.get(varName);
                if (type != null) {
                    return type;
                }
            }
            ITypeBinding typeBinding = expr.resolveTypeBinding();
            if (typeBinding != null) {
                return typeBinding.getName();
            }
        }
        
        IMethodBinding methodBinding = node.resolveMethodBinding();
        if (methodBinding != null) {
            ITypeBinding declaringClass = methodBinding.getDeclaringClass();
            if (declaringClass != null) {
                return declaringClass.getName();
            }
        }
        
        // Si nous ne pouvons toujours pas résoudre, essayons de deviner
        if (expr instanceof SimpleName) {
            String varName = ((SimpleName) expr).getIdentifier();
            // Si le nom de la variable commence par une minuscule, supposons que c'est le nom de la classe avec une majuscule
            if (Character.isLowerCase(varName.charAt(0))) {
                return Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
            }
            return varName;
        }
        
        return null;
    }

    private boolean isIgnoredClass(String className) {
        // Ajoutez ici les noms des classes standard que vous voulez ignorer
        Set<String> ignoredClasses = new HashSet<>(Arrays.asList(
            "String", "Integer", "Long", "Double", "Float", "Boolean",
            "List", "ArrayList", "LinkedList", "Map", "HashMap", "Set", "HashSet",
            "PrintStream", "System"
        ));
        
        if (ignoredClasses.contains(className)) {
            return true;
        }
        
        // Si le nom de classe ne contient pas de point, c'est probablement une classe du projet
        if (!className.contains(".")) {
            return false;
        }
        return IGNORED_PACKAGES.stream().anyMatch(className::startsWith);
    }



    private void addVariableType(String variableName, Type type) {
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType) type;
            localVariableTypes.put(variableName, simpleType.getName().getFullyQualifiedName());
        } else if (type.isQualifiedType()) {
            QualifiedType qualifiedType = (QualifiedType) type;
            localVariableTypes.put(variableName, qualifiedType.getName().getFullyQualifiedName());
        } else if (type.isParameterizedType()) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type baseType = parameterizedType.getType();
            if (baseType.isSimpleType()) {
                SimpleType simpleType = (SimpleType) baseType;
                localVariableTypes.put(variableName, simpleType.getName().getFullyQualifiedName());
            }
        }
    }

    public Map<String, Map<String, Map<String, Integer>>> getCallGraph() {
        return callGraph;
    }
}
