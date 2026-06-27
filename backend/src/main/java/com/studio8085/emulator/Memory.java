package com.studio8085.emulator;

/**
 * 8085 Memory - 64 Kilobytes
 *
 * The 8085 can address 2^16 = 65536 bytes (64 KB) of memory.
 * Addresses range from 0x0000 to 0xFFFF.
 *
 * Memory is used for:
 *   - Storing the program (instructions + data)
 *   - The stack (PUSH/POP operations use SP)
 *   - General data storage (LDA/STA read/write a specific address)
 */
public class Memory {

    public static final int SIZE = 65536;  // 64 KB

    private final int[] data = new int[SIZE];  // Each cell holds one byte (0-255)

    /**
     * Read a single byte from memory at the given address.
     */
    public int read(int address) {
        return data[address & 0xFFFF];
    }

    /**
     * Write a single byte to memory.
     */
    public void write(int address, int value) {
        data[address & 0xFFFF] = value & 0xFF;
    }

    /**
     * Read a 16-bit value (two bytes) from memory.
     * The 8085 stores 16-bit values in little-endian order:
     *   low byte at address, high byte at address+1
     *
     * Example: value 0x1234 stored at 0x2000:
     *   memory[0x2000] = 0x34  (low byte)
     *   memory[0x2001] = 0x12  (high byte)
     */
    public int read16(int address) {
        int low  = data[address & 0xFFFF];
        int high = data[(address + 1) & 0xFFFF];
        return (high << 8) | low;
    }

    /**
     * Write a 16-bit value to two consecutive memory locations (little-endian).
     */
    public void write16(int address, int value) {
        data[address & 0xFFFF]        = value & 0xFF;         // low byte
        data[(address + 1) & 0xFFFF]  = (value >> 8) & 0xFF; // high byte
    }

    /**
     * Load an array of bytes into memory starting at the given address.
     * Used by the assembler to load the assembled program.
     */
    public void load(int startAddress, int[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            data[(startAddress + i) & 0xFFFF] = bytes[i] & 0xFF;
        }
    }

    /**
     * Get a snapshot of memory from startAddress for 'length' bytes.
     * Used by the memory viewer in the UI.
     */
    public int[] dump(int startAddress, int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = data[(startAddress + i) & 0xFFFF];
        }
        return result;
    }

    /** Clear all memory to zero */
    public void reset() {
        for (int i = 0; i < SIZE; i++) {
            data[i] = 0;
        }
    }
}
