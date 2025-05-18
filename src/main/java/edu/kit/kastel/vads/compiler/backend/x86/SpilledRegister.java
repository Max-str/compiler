package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record SpilledRegister(int id) implements Register {
    @Override
    public String toString() {
        return -(id()) * 4  + "(%rbp)";
    }
}
