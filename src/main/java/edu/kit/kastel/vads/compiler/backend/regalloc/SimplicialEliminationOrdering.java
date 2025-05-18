package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.*;

public class SimplicialEliminationOrdering {
    public static List<Node> generateSimplicialEliminationOrder(Map<Node, Set<Node>> nodes)
    {
        Map<Node, Integer> weightedNodes = new HashMap<>();
        List<Node> simplicalOrder = new ArrayList<>();

        for (Node node : nodes.keySet()) {
            weightedNodes.put(node, 0);
        }

        while (!weightedNodes.isEmpty()) {
            var minimalNode = weightedNodes.entrySet().stream().min(Map.Entry.comparingByValue()).get();

            weightedNodes.remove(minimalNode.getKey());
            simplicalOrder.add(minimalNode.getKey());

            for (Node nodeInMap : weightedNodes.keySet()) {
                if (nodes.get(nodeInMap).contains(minimalNode.getKey())) {
                    weightedNodes.put(nodeInMap, weightedNodes.get(nodeInMap) + 1);
                }
            }
        }

        return simplicalOrder;
    }
}
