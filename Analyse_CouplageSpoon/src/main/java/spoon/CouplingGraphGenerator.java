package spoon;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class CouplingGraphGenerator {
    private Graph<String, DefaultWeightedEdge> weightedGraph;

    //Génère un graphe pondéré à partir d'un graphe de couplage
    public void generateWeightedGraph(Map<String, Map<String, Double>> couplingGraph) {
        weightedGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        //Ajout des sommets et des arêtes
        for (String classA : couplingGraph.keySet()) {
            weightedGraph.addVertex(classA);
            //Ajout des arêtes
            for (Map.Entry<String, Double> entry : couplingGraph.get(classA).entrySet()) {
                String classB = entry.getKey();
                Double weight = entry.getValue();
                weightedGraph.addVertex(classB);
                DefaultWeightedEdge edge = weightedGraph.addEdge(classA, classB);
                if (edge != null) {
                    weightedGraph.setEdgeWeight(edge, weight);
                }
            }
        }
    }

    //Exporte le graphe pondéré au format DOT
    public String exportGraphToDOT() {
        DOTExporter<String, DefaultWeightedEdge> exporter = new DOTExporter<>();
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v));
            return map;
        });
        exporter.setEdgeAttributeProvider((e) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            double weight = weightedGraph.getEdgeWeight(e);
            map.put("label", DefaultAttribute.createAttribute(String.format("%.4f", weight)));
            return map;
        });

        Writer writer = new StringWriter();
        exporter.exportGraph(weightedGraph, writer);
        return writer.toString();
    }
}
