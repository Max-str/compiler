package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        for (IrGraph graph : program) {
            RegisterAllocator allocator = new RegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            emitSetup(builder);

            generateForGraph(graph, builder, registers);
        }
        return builder.toString();
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case AddNode add -> generateBinaryOperation(builder, registers, add, X86Instruction.ADD);
            case SubNode sub -> generateBinaryOperation(builder, registers, sub, X86Instruction.SUB);
            case MulNode mul -> generateBinaryOperation(builder, registers, mul, X86Instruction.IMUL);
            case DivNode div -> generateDiv(builder, registers, div);
            case ModNode mod -> generateMod(builder, registers, mod);
            case ReturnNode r -> generateReturn(builder, registers, r);
            case ConstIntNode c -> generateConst(builder, registers, c);
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private static void emitSetup(StringBuilder builder) {
        builder
            .append(".global main\n")
            .append(".global _main\n")
            .append(".text\n")
            .append("\n")
            .append("main:\n")
            .append("  movq $0, %rax\n")
            .append("  movq $0, %rdx\n")
            .append("  call _main\n")
            .append("  movq %rax, %rdi\n")
            .append("  movq $0x3C, %rax\n")
            .append("  syscall\n")
            .append("_main:\n")
            .append("  push %rbp\n")
            .append("  mov %rsp, %rbp\n");
    }

    private static void emitGenericOperation(StringBuilder builder, X86Instruction operation)
    {
        builder.repeat(" ", 2)
            .append(operation)
            .append("\n");
    }

    private static void emitGenericOperation(StringBuilder builder, X86Instruction operation, Register operand)
    {
        builder.repeat(" ", 2)
            .append(operation)
            .append(" ")
            .append(operand)
            .append("\n");
    }

    private static void emitGenericOperation(StringBuilder builder, X86Instruction operation, Register firstOperand, Register secondOperand)
    {
        builder.repeat(" ", 2)
            .append(operation)
            .append(" ")
            .append(firstOperand)
            .append(", ")
            .append(secondOperand)
            .append("\n");
    }


    private static void emitMovImmediate(StringBuilder builder, int immediate, Register target) {
        builder.repeat(" ", 2)
            .append("movl")
            .append(" ")
            .append("$").append(immediate)
            .append(", ")
            .append(target)
            .append("\n");
    }

    private static void emitMov(StringBuilder builder, Register source, Register target) {
        emitGenericOperation(builder, X86Instruction.MOV, source, target);
    }

    private static void emitLeave(StringBuilder builder) {
        emitGenericOperation(builder, X86Instruction.LEAVE);
    }

    private static void emitReturn(StringBuilder builder) {
        emitGenericOperation(builder, X86Instruction.RET);
    }

    private static void emitConvertToQuadword(StringBuilder builder) {
        emitGenericOperation(builder, X86Instruction.CDQ);
    }

    private static void generateReturn(StringBuilder builder, Map<Node, Register> registers, ReturnNode node) {
        emitMov(builder, registers.get(predecessorSkipProj(node, ReturnNode.RESULT)), X86Register.EAX);
        emitLeave(builder);
        emitReturn(builder);
    }

    private static void emitIDiv(StringBuilder builder, Register target) {
        emitGenericOperation(builder, X86Instruction.IDIV, target);
    }

    private static void emitGenericOperand(StringBuilder builder, X86Instruction operation, Register source, Register target) {
        emitGenericOperation(builder, operation, source, target);
    }

    private static void generateConst(StringBuilder builder, Map<Node, Register> registers, ConstIntNode node) {
        emitMovImmediate(builder, node.value(), registers.get(node));
    }

    private static void generateDiv(StringBuilder builder, Map<Node, Register> registers, DivNode node) {
        if (registers.get(predecessorSkipProj(node, DivNode.RIGHT)) instanceof SpilledRegister) {
            emitMov(builder, registers.get(predecessorSkipProj(node, DivNode.LEFT)), X86Register.EAX);
            emitConvertToQuadword(builder);
            emitMov(builder, registers.get(predecessorSkipProj(node, DivNode.RIGHT)), new TemporaryRegister());
            emitIDiv(builder, new TemporaryRegister());
            emitMov(builder, X86Register.EAX, registers.get(node));
        } else {
            emitMov(builder, registers.get(predecessorSkipProj(node, DivNode.LEFT)), X86Register.EAX);
            emitConvertToQuadword(builder);
            emitIDiv(builder, registers.get(predecessorSkipProj(node, DivNode.RIGHT)));
            emitMov(builder, X86Register.EAX, registers.get(node));
        }
    }

    private static void generateMod(StringBuilder builder, Map<Node, Register> registers, ModNode node) {
        if (registers.get(predecessorSkipProj(node, DivNode.RIGHT)) instanceof SpilledRegister) {
            emitMov(builder, registers.get(predecessorSkipProj(node, DivNode.LEFT)), X86Register.EAX);
            emitConvertToQuadword(builder);
            emitMov(builder, registers.get(predecessorSkipProj(node, DivNode.RIGHT)), new TemporaryRegister());
            emitIDiv(builder, new TemporaryRegister());
            emitMov(builder, X86Register.EDX, registers.get(node));
        } else {
            emitMov(builder, registers.get(predecessorSkipProj(node, DivNode.LEFT)), X86Register.EAX);
            emitConvertToQuadword(builder);
            emitIDiv(builder, registers.get(predecessorSkipProj(node, DivNode.RIGHT)));
            emitMov(builder, X86Register.EDX, registers.get(node));
        }
    }

    private static void generateBinaryOperation(
        StringBuilder builder,
        Map<Node, Register> registers,
        BinaryOperationNode node,
        X86Instruction opcode
    ) {
        var targetRegister = registers.get(node);

        if (targetRegister instanceof SpilledRegister) {
            var realRegister = new TemporaryRegister();

            emitMov(builder, registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)), realRegister);
            emitGenericOperand(builder, opcode, registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)), realRegister);
            emitMov(builder, realRegister, targetRegister);
        } else {
            emitMov(builder, registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)), registers.get(node));
            emitGenericOperand(builder, opcode, registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)), targetRegister);
        }
    }
}
