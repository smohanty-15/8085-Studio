package com.studio8085.emulator;

/**
 * 8085 CPU Flags (also called the Status Register or PSW - Program Status Word)
 *
 * After arithmetic or logical operations, the CPU automatically sets these flags
 * based on the result. Branch instructions like JZ (Jump if Zero) check these flags.
 *
 * S  - Sign Flag    : Set if the result is negative (bit 7 of result is 1)
 * Z  - Zero Flag    : Set if the result is zero
 * AC - Aux Carry    : Set if there's a carry from bit 3 to bit 4 (used for BCD math)
 * P  - Parity Flag  : Set if the result has even number of 1-bits
 * CY - Carry Flag   : Set if there's a carry out of bit 7 (overflow for 8-bit)
 */
public class Flags {

    private boolean S  = false;  // Sign
    private boolean Z  = false;  // Zero
    private boolean AC = false;  // Auxiliary Carry
    private boolean P  = false;  // Parity
    private boolean CY = false;  // Carry

    // ---- Getters ----
    public boolean isS()  { return S; }
    public boolean isZ()  { return Z; }
    public boolean isAC() { return AC; }
    public boolean isP()  { return P; }
    public boolean isCY() { return CY; }

    // ---- Setters ----
    public void setS(boolean v)  { S = v; }
    public void setZ(boolean v)  { Z = v; }
    public void setAC(boolean v) { AC = v; }
    public void setP(boolean v)  { P = v; }
    public void setCY(boolean v) { CY = v; }

    /**
     * Update all flags based on the result of an operation.
     *
     * @param result  The raw (possibly > 8-bit) result
     * @param carry   True if there was a carry/borrow
     * @param auxCarry True if there was auxiliary carry (bit 3 -> bit 4)
     */
    public void updateFlags(int result, boolean carry, boolean auxCarry) {
        int byteResult = result & 0xFF;

        // Zero: result is 0
        Z = (byteResult == 0);

        // Sign: bit 7 is set (result >= 128 when unsigned, negative in 2's complement)
        S = (byteResult & 0x80) != 0;

        // Parity: even number of 1-bits
        P = computeParity(byteResult);

        // Carry from arithmetic
        CY = carry;

        // Auxiliary carry
        AC = auxCarry;
    }

    /**
     * Update flags for logical operations (AND, OR, XOR, CMP).
     * Logical ops always clear CY and AC.
     */
    public void updateFlagsLogical(int result) {
        int byteResult = result & 0xFF;
        Z  = (byteResult == 0);
        S  = (byteResult & 0x80) != 0;
        P  = computeParity(byteResult);
        CY = false;
        AC = false;
    }

    /** Count the number of 1-bits; parity is true (even) if count is even */
    private boolean computeParity(int value) {
        int count = Integer.bitCount(value & 0xFF);
        return (count % 2 == 0);
    }

    /** Reset all flags to false */
    public void reset() {
        S = Z = AC = P = CY = false;
    }
}
