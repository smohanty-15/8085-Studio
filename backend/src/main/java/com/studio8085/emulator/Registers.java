package com.studio8085.emulator;

/**
 * 8085 CPU Registers
 *
 * The 8085 has these registers:
 *   A  - Accumulator (8-bit). Most arithmetic/logic operations use this.
 *   B,C,D,E,H,L - General purpose 8-bit registers.
 *                 They can also work in pairs: BC, DE, HL (16-bit).
 *   PC - Program Counter (16-bit). Points to the next instruction to execute.
 *   SP - Stack Pointer (16-bit). Points to the top of the stack in memory.
 */
public class Registers {

    // 8-bit registers (values 0-255)
    private int A = 0;  // Accumulator
    private int B = 0;
    private int C = 0;
    private int D = 0;
    private int E = 0;
    private int H = 0;
    private int L = 0;

    // 16-bit registers (values 0-65535)
    private int PC = 0;  // Program Counter
    private int SP = 0xFFFF;  // Stack Pointer starts at top of memory

    // ---- Getters ----

    public int getA() { return A; }
    public int getB() { return B; }
    public int getC() { return C; }
    public int getD() { return D; }
    public int getE() { return E; }
    public int getH() { return H; }
    public int getL() { return L; }
    public int getPC() { return PC; }
    public int getSP() { return SP; }

    // ---- Setters (mask to correct bit width) ----

    public void setA(int v) { A = v & 0xFF; }
    public void setB(int v) { B = v & 0xFF; }
    public void setC(int v) { C = v & 0xFF; }
    public void setD(int v) { D = v & 0xFF; }
    public void setE(int v) { E = v & 0xFF; }
    public void setH(int v) { H = v & 0xFF; }
    public void setL(int v) { L = v & 0xFF; }
    public void setPC(int v) { PC = v & 0xFFFF; }
    public void setSP(int v) { SP = v & 0xFFFF; }

    /**
     * Get a register value by its single-letter name.
     * Used by the emulator to look up operands like "MOV A, B"
     */
    public int get(String reg) {
        return switch (reg.toUpperCase()) {
            case "A" -> A;
            case "B" -> B;
            case "C" -> C;
            case "D" -> D;
            case "E" -> E;
            case "H" -> H;
            case "L" -> L;
            default -> throw new RuntimeException("Unknown register: " + reg);
        };
    }

    /**
     * Set a register value by name.
     */
    public void set(String reg, int value) {
        switch (reg.toUpperCase()) {
            case "A" -> setA(value);
            case "B" -> setB(value);
            case "C" -> setC(value);
            case "D" -> setD(value);
            case "E" -> setE(value);
            case "H" -> setH(value);
            case "L" -> setL(value);
            default -> throw new RuntimeException("Unknown register: " + reg);
        }
    }

    /** HL register pair - used for memory addressing */
    public int getHL() {
        return (H << 8) | L;
    }

    /** Reset all registers to initial state */
    public void reset() {
        A = B = C = D = E = H = L = 0;
        PC = 0;
        SP = 0xFFFF;
    }
}
