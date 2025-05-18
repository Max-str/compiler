package edu.kit.kastel.vads.compiler.backend.x86;

public enum X86Instruction {
    MOV("mov"), ADD("add"), SUB("sub"), IDIV("idiv"), IMUL("imul"),
    RET("ret"), LEAVE("leave"), PUSH("push"), POP("pop"), CALL("call"), CDQ("cdq");

    private final String instruction;

    X86Instruction(String instruction) {
        this.instruction = instruction;
    }

    public String toString() {
        return instruction;
    }
}
