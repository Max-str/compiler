package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.*;

public class ColouredGraph {
    private final Map<Node, Set<Node>> livenessAnalysis;
    private final List<Node> simplicialOrder;

    public ColouredGraph(Map<Node, Set<Node>> livenessAnalysis, List<Node> simplicialOrder) {
        this.livenessAnalysis = new HashMap<>(livenessAnalysis);
        this.simplicialOrder = simplicialOrder;

        for (Node node : livenessAnalysis.keySet()) {
            Set<Node> nodes = livenessAnalysis.get(node);

            for (Node neighbour : nodes) {
                if (!livenessAnalysis.containsKey(neighbour)) {
                    livenessAnalysis.put(neighbour, new HashSet<>());
                }
                livenessAnalysis.get(neighbour).add(node);
            }
        }
    }

    public Map<Node, Integer> getGraphColours()
    {
        var colouring = new HashMap<Node, Integer>();

        for (Node node : simplicialOrder) {
            Set<Integer> alreadyUsedColors = new HashSet<>();
            for (Node neighbour : livenessAnalysis.get(node)) {
                if (colouring.containsKey(neighbour))
                    alreadyUsedColors.add(colouring.get(neighbour));
            }

            var color = 0;
            while (alreadyUsedColors.contains(color)) {
                color++;
            }

            colouring.put(node, color);
        }

        return colouring;
    }
}
