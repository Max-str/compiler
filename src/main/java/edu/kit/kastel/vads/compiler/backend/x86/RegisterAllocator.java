package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.ColouredGraph;
import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.SimplicialEliminationOrdering;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.liveliness.Analysis;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.*;

public class RegisterAllocator implements edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator {
    private int spilledCount = 0;

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        var liveness = new Analysis(graph).analyze();
        var orderedNodes = SimplicialEliminationOrdering.generateSimplicialEliminationOrder(liveness);
        var colouredGraph = new ColouredGraph(liveness, orderedNodes).getGraphColours();

        var mapping = mapColorToRegister(new ArrayList<>(colouredGraph.values()));

        var result = new HashMap<Node, Register>();

        for (var node : orderedNodes) {
            result.put(node, mapping.get(colouredGraph.get(node)));
        }

        return result;
    }

    private Map<Integer, Register> mapColorToRegister(List<Integer> colors) {
        List<Register> usableRegisters = new ArrayList<>(List.of(
            X86Register.R8D, X86Register.R9D, X86Register.R10D, X86Register.R11D,
            X86Register.R12D, X86Register.R13D, X86Register.R14D, X86Register.R15D
        ));

        Map<Integer, Register> registerMap = new HashMap<>();

        for (int color : colors.stream().distinct().toList()) {
            if (usableRegisters.isEmpty()) {
                registerMap.put(color, new SpilledRegister(spilledCount++));
            } else {
                registerMap.put(color, usableRegisters.removeFirst());
            }
        }

        return registerMap;
    }
}
