package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record TemporaryRegister() implements Register {
    @Override
    public String toString() {
        return "%edi";
    }
}
