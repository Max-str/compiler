package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public enum X86Register implements Register {
    EAX("eax"), EBX("ebx"), ECX("ecx"), EDX("edx"),
    ESI("esi"), EDI("edi"), ESP("esp"), EBP("ebp"),
    R8D("r8d"), R9D("r9d"), R10D("r10d"), R11D("r11d"),
    R12D("r12d"), R13D("r13d"), R14D("r14d"), R15D("r15d");

    private final String name;

    X86Register(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "%" + name;
    }
}
