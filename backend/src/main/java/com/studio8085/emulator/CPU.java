package com.studio8085.emulator;

/**
 * 8085 CPU Emulator
 *
 * This is the heart of the emulator. It reads one instruction from memory,
 * decodes what it means, and executes it — just like a real 8085 chip does.
 *
 * The FETCH → DECODE → EXECUTE cycle:
 *   1. FETCH:   Read the opcode byte at the address in PC
 *   2. DECODE:  Figure out which instruction it is (by looking at the opcode)
 *   3. EXECUTE: Perform the operation (add, move, jump, etc.)
 *   4. UPDATE:  Advance PC to next instruction, update flags/registers
 *
 * Opcodes are one byte (0x00 to 0xFF), telling the CPU what to do.
 * Some instructions need extra bytes for data (e.g., MVI A, 10H needs 2 bytes total).
 */
public class CPU {

    private final Registers registers;
    private final Flags flags;
    private final Memory memory;

    private boolean halted = false;   // becomes true when HLT executes
    private String lastError = null;  // last error message (if any)

    public CPU(Registers registers, Flags flags, Memory memory) {
        this.registers = registers;
        this.flags = flags;
        this.memory = memory;
    }

    public boolean isHalted() { return halted; }
    public String getLastError() { return lastError; }
    public void clearError() { lastError = null; }

    /** Reset CPU to initial state (clear halt, clear error) */
    public void reset() {
        halted = false;
        lastError = null;
    }

    /**
     * Execute ONE instruction (Step mode).
     *
     * Returns true if execution should continue, false if halted or error.
     *
     * HOW TO READ THIS CODE:
     *   - We fetch the opcode from memory[PC], then increment PC
     *   - Based on the opcode, we know what the instruction is
     *   - We fetch any extra bytes (like the immediate value in MVI A, 5)
     *   - We perform the operation
     */
    public boolean step() {
        if (halted) return false;

        int pc = registers.getPC();
        int opcode = memory.read(pc);
        registers.setPC(pc + 1);  // advance past opcode

        try {
            executeOpcode(opcode);
        } catch (Exception e) {
            lastError = "Error at address " + String.format("%04X", pc)
                    + ": " + e.getMessage();
            halted = true;
            return false;
        }

        return !halted;
    }

    /**
     * Run until HLT or error.
     * Has a safety limit to prevent infinite loops from locking up the server.
     */
    public void run() {
        int maxSteps = 100_000;
        int steps = 0;
        while (!halted && steps < maxSteps) {
            step();
            steps++;
        }
        if (steps >= maxSteps) {
            lastError = "Execution limit reached (possible infinite loop). Use HLT to stop.";
            halted = true;
        }
    }

    /**
     * Decode and execute one opcode.
     *
     * The opcode is a single byte that uniquely identifies the instruction.
     * The 8085 has 246 valid opcodes.
     *
     * OPCODE PATTERNS (for understanding, not exhaustive):
     *   0x00 = NOP
     *   0x76 = HLT
     *   0x40-0x7F = MOV instructions (with exceptions)
     *   0x80-0x87 = ADD B through ADD A
     *   0x90-0x97 = SUB B through SUB A
     *   etc.
     */
    private void executeOpcode(int opcode) {
        switch (opcode) {

            // ==================
            // NOP - No Operation
            // Does nothing, just wastes a cycle
            // ==================
            case 0x00 -> { /* NOP */ }

            // ==================
            // HLT - Halt
            // Stops the CPU
            // ==================
            case 0x76 -> halted = true;

            // ==================
            // MVI - Move Immediate
            // Load a constant value directly into a register
            // Opcode tells us the destination register, next byte is the value
            // ==================
            case 0x06 -> registers.setB(fetchByte()); // MVI B, data
            case 0x0E -> registers.setC(fetchByte()); // MVI C, data
            case 0x16 -> registers.setD(fetchByte()); // MVI D, data
            case 0x1E -> registers.setE(fetchByte()); // MVI E, data
            case 0x26 -> registers.setH(fetchByte()); // MVI H, data
            case 0x2E -> registers.setL(fetchByte()); // MVI L, data
            case 0x3E -> registers.setA(fetchByte()); // MVI A, data

            // ==================
            // MOV - Move Register to Register
            // Pattern: 0x40 to 0x7F (except 0x76 which is HLT)
            // Opcode = 0x40 + (dst * 8) + src
            // dst/src: B=0, C=1, D=2, E=3, H=4, L=5, A=7
            // ==================
            case 0x78 -> registers.setA(registers.getB()); // MOV A, B
            case 0x79 -> registers.setA(registers.getC()); // MOV A, C
            case 0x7A -> registers.setA(registers.getD()); // MOV A, D
            case 0x7B -> registers.setA(registers.getE()); // MOV A, E
            case 0x7C -> registers.setA(registers.getH()); // MOV A, H
            case 0x7D -> registers.setA(registers.getL()); // MOV A, L
            case 0x7F -> { /* MOV A, A - no-op */ }

            case 0x47 -> registers.setB(registers.getA()); // MOV B, A
            case 0x40 -> { /* MOV B, B */ }
            case 0x41 -> registers.setB(registers.getC());
            case 0x42 -> registers.setB(registers.getD());
            case 0x43 -> registers.setB(registers.getE());
            case 0x44 -> registers.setB(registers.getH());
            case 0x45 -> registers.setB(registers.getL());

            case 0x4F -> registers.setC(registers.getA()); // MOV C, A
            case 0x48 -> registers.setC(registers.getB());
            case 0x49 -> { /* MOV C, C */ }
            case 0x4A -> registers.setC(registers.getD());
            case 0x4B -> registers.setC(registers.getE());
            case 0x4C -> registers.setC(registers.getH());
            case 0x4D -> registers.setC(registers.getL());

            case 0x57 -> registers.setD(registers.getA()); // MOV D, A
            case 0x50 -> registers.setD(registers.getB());
            case 0x51 -> registers.setD(registers.getC());
            case 0x52 -> { /* MOV D, D */ }
            case 0x53 -> registers.setD(registers.getE());
            case 0x54 -> registers.setD(registers.getH());
            case 0x55 -> registers.setD(registers.getL());

            case 0x5F -> registers.setE(registers.getA()); // MOV E, A
            case 0x58 -> registers.setE(registers.getB());
            case 0x59 -> registers.setE(registers.getC());
            case 0x5A -> registers.setE(registers.getD());
            case 0x5B -> { /* MOV E, E */ }
            case 0x5C -> registers.setE(registers.getH());
            case 0x5D -> registers.setE(registers.getL());

            case 0x67 -> registers.setH(registers.getA()); // MOV H, A
            case 0x60 -> registers.setH(registers.getB());
            case 0x61 -> registers.setH(registers.getC());
            case 0x62 -> registers.setH(registers.getD());
            case 0x63 -> registers.setH(registers.getE());
            case 0x64 -> { /* MOV H, H */ }
            case 0x65 -> registers.setH(registers.getL());

            case 0x6F -> registers.setL(registers.getA()); // MOV L, A
            case 0x68 -> registers.setL(registers.getB());
            case 0x69 -> registers.setL(registers.getC());
            case 0x6A -> registers.setL(registers.getD());
            case 0x6B -> registers.setL(registers.getE());
            case 0x6C -> registers.setL(registers.getH());
            case 0x6D -> { /* MOV L, L */ }

            // ==================
            // LDA - Load Accumulator Direct
            // A = memory[address]
            // Needs 2 more bytes for the 16-bit address (little-endian)
            // ==================
            case 0x3A -> {
                int addr = fetch16();  // fetch address from next 2 bytes
                registers.setA(memory.read(addr));
            }

            // ==================
            // STA - Store Accumulator Direct
            // memory[address] = A
            // ==================
            case 0x32 -> {
                int addr = fetch16();
                memory.write(addr, registers.getA());
            }

            // ==================
            // ADD - Add register to Accumulator
            // A = A + reg; updates all flags
            // ==================
            case 0x80 -> executeADD(registers.getB());
            case 0x81 -> executeADD(registers.getC());
            case 0x82 -> executeADD(registers.getD());
            case 0x83 -> executeADD(registers.getE());
            case 0x84 -> executeADD(registers.getH());
            case 0x85 -> executeADD(registers.getL());
            case 0x87 -> executeADD(registers.getA());

            // ==================
            // ADI - Add Immediate to Accumulator
            // A = A + data
            // ==================
            case 0xC6 -> executeADD(fetchByte());

            // ==================
            // SUB - Subtract register from Accumulator
            // A = A - reg; updates all flags
            // ==================
            case 0x90 -> executeSUB(registers.getB());
            case 0x91 -> executeSUB(registers.getC());
            case 0x92 -> executeSUB(registers.getD());
            case 0x93 -> executeSUB(registers.getE());
            case 0x94 -> executeSUB(registers.getH());
            case 0x95 -> executeSUB(registers.getL());
            case 0x97 -> executeSUB(registers.getA());

            // ==================
            // INR - Increment Register
            // reg = reg + 1; updates all flags EXCEPT CY
            // ==================
            case 0x04 -> registers.setB(executeINR(registers.getB()));
            case 0x0C -> registers.setC(executeINR(registers.getC()));
            case 0x14 -> registers.setD(executeINR(registers.getD()));
            case 0x1C -> registers.setE(executeINR(registers.getE()));
            case 0x24 -> registers.setH(executeINR(registers.getH()));
            case 0x2C -> registers.setL(executeINR(registers.getL()));
            case 0x3C -> registers.setA(executeINR(registers.getA()));

            // ==================
            // DCR - Decrement Register
            // reg = reg - 1; updates all flags EXCEPT CY
            // ==================
            case 0x05 -> registers.setB(executeDCR(registers.getB()));
            case 0x0D -> registers.setC(executeDCR(registers.getC()));
            case 0x15 -> registers.setD(executeDCR(registers.getD()));
            case 0x1D -> registers.setE(executeDCR(registers.getE()));
            case 0x25 -> registers.setH(executeDCR(registers.getH()));
            case 0x2D -> registers.setL(executeDCR(registers.getL()));
            case 0x3D -> registers.setA(executeDCR(registers.getA()));

            // ==================
            // ANA - AND Accumulator with register
            // A = A AND reg; updates flags (CY=0, AC=0)
            // ==================
            case 0xA0 -> executeANA(registers.getB());
            case 0xA1 -> executeANA(registers.getC());
            case 0xA2 -> executeANA(registers.getD());
            case 0xA3 -> executeANA(registers.getE());
            case 0xA4 -> executeANA(registers.getH());
            case 0xA5 -> executeANA(registers.getL());
            case 0xA7 -> executeANA(registers.getA());

            // ==================
            // ORA - OR Accumulator with register
            // A = A OR reg
            // ==================
            case 0xB0 -> executeORA(registers.getB());
            case 0xB1 -> executeORA(registers.getC());
            case 0xB2 -> executeORA(registers.getD());
            case 0xB3 -> executeORA(registers.getE());
            case 0xB4 -> executeORA(registers.getH());
            case 0xB5 -> executeORA(registers.getL());
            case 0xB7 -> executeORA(registers.getA());

            // ==================
            // XRA - XOR Accumulator with register
            // A = A XOR reg; useful for clearing A (XRA A makes A=0)
            // ==================
            case 0xA8 -> executeXRA(registers.getB());
            case 0xA9 -> executeXRA(registers.getC());
            case 0xAA -> executeXRA(registers.getD());
            case 0xAB -> executeXRA(registers.getE());
            case 0xAC -> executeXRA(registers.getH());
            case 0xAD -> executeXRA(registers.getL());
            case 0xAF -> executeXRA(registers.getA());

            // ==================
            // CMP - Compare Accumulator with register
            // Performs A - reg but DOES NOT change A.
            // Only updates flags (so you can check Z, CY etc.)
            // Used before JZ, JNZ, etc.
            // ==================
            case 0xB8 -> executeCMP(registers.getB());
            case 0xB9 -> executeCMP(registers.getC());
            case 0xBA -> executeCMP(registers.getD());
            case 0xBB -> executeCMP(registers.getE());
            case 0xBC -> executeCMP(registers.getH());
            case 0xBD -> executeCMP(registers.getL());
            case 0xBF -> executeCMP(registers.getA());

            // ==================
            // JMP - Unconditional Jump
            // PC = address (always jumps)
            // ==================
            case 0xC3 -> registers.setPC(fetch16());

            // ==================
            // JZ - Jump if Zero
            // If Z flag is set (last result was zero), jump to address
            // Otherwise, continue to next instruction
            // ==================
            case 0xCA -> {
                int addr = fetch16();
                if (flags.isZ()) registers.setPC(addr);
            }

            // ==================
            // JNZ - Jump if Not Zero
            // If Z flag is NOT set, jump to address
            // ==================
            case 0xC2 -> {
                int addr = fetch16();
                if (!flags.isZ()) registers.setPC(addr);
            }

            default -> throw new RuntimeException(
                    "Unknown opcode: 0x" + String.format("%02X", opcode));
        }
    }

    // ==================
    // Helper: Fetch Methods
    // These read the next byte(s) from memory and advance PC
    // ==================

    /** Fetch one byte from memory at PC and increment PC */
    private int fetchByte() {
        int val = memory.read(registers.getPC());
        registers.setPC(registers.getPC() + 1);
        return val;
    }

    /** Fetch a 16-bit address (2 bytes, little-endian) */
    private int fetch16() {
        int low  = fetchByte();
        int high = fetchByte();
        return (high << 8) | low;
    }

    // ==================
    // Helper: Execute Operations
    // These implement the actual arithmetic/logic with flag updates
    // ==================

    /**
     * ADD: A = A + value
     * Carry flag: set if result > 255 (overflow beyond 8 bits)
     * Aux carry: set if carry from bit 3 to bit 4
     */
    private void executeADD(int value) {
        int a = registers.getA();
        int result = a + value;
        boolean carry = result > 0xFF;
        boolean auxCarry = ((a & 0x0F) + (value & 0x0F)) > 0x0F;
        registers.setA(result & 0xFF);
        flags.updateFlags(result, carry, auxCarry);
    }

    /**
     * SUB: A = A - value
     * In subtraction, CY is set if borrow occurs (i.e. A < value)
     */
    private void executeSUB(int value) {
        int a = registers.getA();
        int result = a - value;
        boolean borrow = result < 0;
        boolean auxBorrow = (a & 0x0F) < (value & 0x0F);
        registers.setA(result & 0xFF);
        flags.updateFlags(result & 0xFF, borrow, auxBorrow);
    }

    /**
     * INR: reg = reg + 1
     * Does NOT affect carry flag (CY remains unchanged)
     */
    private int executeINR(int value) {
        int result = (value + 1) & 0xFF;
        boolean auxCarry = (value & 0x0F) == 0x0F;
        boolean oldCY = flags.isCY();  // save carry
        flags.updateFlags(result, false, auxCarry);
        flags.setCY(oldCY);  // restore carry
        return result;
    }

    /**
     * DCR: reg = reg - 1
     * Does NOT affect carry flag
     */
    private int executeDCR(int value) {
        int result = (value - 1) & 0xFF;
        boolean auxBorrow = (value & 0x0F) == 0;
        boolean oldCY = flags.isCY();
        flags.updateFlags(result, false, auxBorrow);
        flags.setCY(oldCY);
        return result;
    }

    /** ANA: A = A AND value */
    private void executeANA(int value) {
        int result = registers.getA() & value;
        registers.setA(result);
        flags.updateFlagsLogical(result);
    }

    /** ORA: A = A OR value */
    private void executeORA(int value) {
        int result = registers.getA() | value;
        registers.setA(result);
        flags.updateFlagsLogical(result);
    }

    /** XRA: A = A XOR value */
    private void executeXRA(int value) {
        int result = registers.getA() ^ value;
        registers.setA(result);
        flags.updateFlagsLogical(result);
    }

    /**
     * CMP: compare A with value by computing A - value.
     * Result is discarded (A unchanged), only flags are set.
     * Useful: after CMP B, you can check JZ (equal), JNZ (not equal),
     *         or check CY (A < B means borrow, so CY=1)
     */
    private void executeCMP(int value) {
        int a = registers.getA();
        int result = a - value;
        boolean borrow = result < 0;
        boolean auxBorrow = (a & 0x0F) < (value & 0x0F);
        // Don't change A, only update flags
        flags.updateFlags(result & 0xFF, borrow, auxBorrow);
    }
}
