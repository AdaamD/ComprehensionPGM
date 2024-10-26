package part2;

import org.eclipse.jdt.core.dom.*;
import java.util.*;

public class CallGraphVisitor extends ASTVisitor {
    private Map<String, Set<String>> callGraph = new HashMap<>();
    private String currentClass;
    private String currentMethod;

    @Override
    public boolean visit(TypeDeclaration node) {
        currentClass = node.getName().getIdentifier();
        return true;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        currentMethod = currentClass + "." + node.getName().getIdentifier();
        callGraph.putIfAbsent(currentMethod, new HashSet<>());
        return true;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (currentMethod != null) {
            Expression expr = node.getExpression();
            String calledClass = (expr != null) ? expr.toString() : currentClass;
            String calledMethod = calledClass + "." + node.getName().getIdentifier();
            callGraph.get(currentMethod).add(calledMethod);
        }
        return true;
    }

    public Map<String, Set<String>> getCallGraph() {
        return callGraph;
    }
}
