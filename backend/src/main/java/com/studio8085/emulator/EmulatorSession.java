package com.studio8085.emulator;

/**
 * EmulatorSession bundles CPU, Memory, Registers and Flags together.
 * In Version 1, we use a single global session (no user accounts).
 * All state lives here in RAM — refresh the browser and it resets.
 */
public class EmulatorSession {

    public final Registers registers;
    public final Flags flags;
    public final Memory memory;
    public final CPU cpu;

    public EmulatorSession() {
        this.registers = new Registers();
        this.flags = new Flags();
        this.memory = new Memory();
        this.cpu = new CPU(registers, flags, memory);
    }

    /** Full reset: clear registers, flags, memory, and CPU state */
    public void reset() {
        registers.reset();
        flags.reset();
        memory.reset();
        cpu.reset();
    }
}
