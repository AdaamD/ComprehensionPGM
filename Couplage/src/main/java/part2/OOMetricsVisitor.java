package part2;

import org.eclipse.jdt.core.dom.*;
import java.util.*;

public class OOMetricsVisitor extends ASTVisitor {
    private int totalLineCount = 0; // Compte total des lignes pour tous les fichiers
    private List<ClassInfo> classInfoList = new ArrayList<>(); // Informations sur les classes
    private ClassInfo currentClass; // Informations sur la classe en cours de traitement
    private int maxParameters = 0; // Nombre maximum de paramètres pour une méthode

    static class ClassInfo {

        // Informations sur une classe
        String name;
        String packageName;
        int methodCount;
        int attributeCount;
        int lineCount;
        List<MethodInfo> methodInfos = new ArrayList<>();

        ClassInfo(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
            this.methodCount = 0;
            this.attributeCount = 0;
            this.lineCount = 0;
        }
    }

    // Informations sur une méthode
    static class MethodInfo {
        String name;
        int lineCount;
        int parameterCount;

        MethodInfo(String name, int lineCount, int parameterCount) {
            this.name = name;
            this.lineCount = lineCount;
            this.parameterCount = parameterCount;
        }
    }

    // Visiteur pour les classes
    @Override
    public boolean visit(TypeDeclaration node) {
        String packageName = "";
        if (node.getRoot() instanceof CompilationUnit) {
            PackageDeclaration packageDecl = ((CompilationUnit) node.getRoot()).getPackage();
            if (packageDecl != null) {
                packageName = packageDecl.getName().getFullyQualifiedName();
            }
        }
        currentClass = new ClassInfo(node.getName().getIdentifier(), packageName);
        classInfoList.add(currentClass);
        return true;
    }


    @Override
    public void endVisit(TypeDeclaration node) {
        if (currentClass != null) {
            CompilationUnit cu = (CompilationUnit) node.getRoot();
            currentClass.lineCount = cu.getLineNumber(node.getStartPosition() + node.getLength()) - 
                                     cu.getLineNumber(node.getStartPosition()) + 1;
        }
        currentClass = null;
    }

    // Visiteur pour les méthodes
    @Override
    public boolean visit(MethodDeclaration node) {
        if (currentClass != null) {
            String methodName = node.getName().getIdentifier();
            CompilationUnit cu = (CompilationUnit) node.getRoot();
            int startLine = cu.getLineNumber(node.getStartPosition());
            int endLine = cu.getLineNumber(node.getStartPosition() + node.getLength());
            int lineCount = endLine - startLine + 1;
            int parameterCount = node.parameters().size();

            currentClass.methodInfos.add(new MethodInfo(methodName, lineCount, parameterCount));
            currentClass.methodCount++;
            maxParameters = Math.max(maxParameters, parameterCount);
        }
        return true;
    }

    // Visiteur pour les attributs
    @Override
    public boolean visit(FieldDeclaration node) {
        if (currentClass != null) {
            currentClass.attributeCount += node.fragments().size();
        }
        return true;
    }


    @Override
    public void endVisit(CompilationUnit node) {
        totalLineCount += node.getLineNumber(node.getLength() - 1);
    }

    // Méthodes pour obtenir les informations sur les classes
    public int getClassCount() {
        return classInfoList.size();
    }
    public int getTotalLineCount() {
        return totalLineCount;
    }
    public int getMethodCount() {
        return classInfoList.stream().mapToInt(c -> c.methodCount).sum();
    }
    public int getAttributeCount() {
        return classInfoList.stream().mapToInt(c -> c.attributeCount).sum();
    }
    public List<ClassInfo> getClassInfoList() {
        return classInfoList;
    }
    public int getMaxParameters() {
        return maxParameters;
    }
}
