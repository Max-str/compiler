package edu.kit.kastel.vads.compiler.ir.liveliness;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class Analysis {
    private final IrGraph graph;

    public Analysis(IrGraph graph) {
        this.graph = graph;
    }

    public Map<Node, Set<Node>> analyze()
    {
        var orderedNodes = graph.getOrderedNodes();

        Map<Node, Set<Node>> livenessAnalysis = new HashMap<>();
        Set<Node> liveIn = new HashSet<>();

        for (int i = orderedNodes.size() - 1; i >= 0; i--) {
            var node = orderedNodes.get(i);

            switch (node) {
                case BinaryOperationNode b -> {
                    liveIn.add(predecessorSkipProj(b, BinaryOperationNode.LEFT));
                    liveIn.add(predecessorSkipProj(b, BinaryOperationNode.RIGHT));
                }
                case ReturnNode r -> liveIn.add(predecessorSkipProj(r, ReturnNode.RESULT));
                default -> {}
            }

            liveIn.remove(node);
            livenessAnalysis.put(node, new HashSet<>(Set.copyOf(liveIn)));
        }

        return livenessAnalysis;
    }
}
