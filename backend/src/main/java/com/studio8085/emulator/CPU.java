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
 * The 8085 has 246 valid opcodes spanning 74 instruction types.
 *
 * FULL OPCODE MAP SUMMARY:
 *   0x00        = NOP
 *   0x01,11,21,31 = LXI B/D/H/SP
 *   0x02,12     = STAX B/D
 *   0x03,13,23,33 = INX B/D/H/SP
 *   0x04..3C    = INR (all registers)
 *   0x05..3D    = DCR (all registers)
 *   0x06..3E    = MVI (all registers)
 *   0x07        = RLC
 *   0x09,19,29,39 = DAD B/D/H/SP
 *   0x0A,1A     = LDAX B/D
 *   0x0B,1B,2B,3B = DCX B/D/H/SP
 *   0x0F        = RRC
 *   0x17        = RAL
 *   0x1F        = RAR
 *   0x20        = RIM
 *   0x22        = SHLD
 *   0x27        = DAA
 *   0x2A        = LHLD
 *   0x2F        = CMA
 *   0x30        = SIM
 *   0x32        = STA
 *   0x37        = STC
 *   0x3A        = LDA
 *   0x3F        = CMC
 *   0x40..7F    = MOV (64 opcodes, 0x76=HLT)
 *   0x76        = HLT
 *   0x80..87    = ADD B..A
 *   0x88..8F    = ADC B..A
 *   0x90..97    = SUB B..A
 *   0x98..9F    = SBB B..A
 *   0xA0..A7    = ANA B..A
 *   0xA8..AF    = XRA B..A
 *   0xB0..B7    = ORA B..A
 *   0xB8..BF    = CMP B..A
 *   0xC0        = RNZ
 *   0xC1,D1,E1,F1 = POP B/D/H/PSW
 *   0xC2        = JNZ
 *   0xC3        = JMP
 *   0xC4        = CNZ
 *   0xC5,D5,E5,F5 = PUSH B/D/H/PSW
 *   0xC6        = ADI
 *   0xC7..FF    = RST 0..7 (scattered)
 *   0xC8        = RZ
 *   0xC9        = RET
 *   0xCA        = JZ
 *   0xCC        = CZ
 *   0xCD        = CALL
 *   0xCE        = ACI
 *   0xCF        = RST 1
 *   0xD0        = RNC
 *   0xD2        = JNC
 *   0xD3        = OUT
 *   0xD4        = CNC
 *   0xD6        = SUI
 *   0xD7        = RST 2
 *   0xD8        = RC
 *   0xDA        = JC
 *   0xDB        = IN
 *   0xDC        = CC
 *   0xDE        = SBI
 *   0xDF        = RST 3
 *   0xE0        = RPO
 *   0xE2        = JPO
 *   0xE3        = XTHL
 *   0xE4        = CPO
 *   0xE6        = ANI
 *   0xE7        = RST 4
 *   0xE8        = RPE
 *   0xE9        = PCHL
 *   0xEA        = JPE
 *   0xEB        = XCHG
 *   0xEC        = CPE
 *   0xEE        = XRI
 *   0xEF        = RST 5
 *   0xF0        = RP
 *   0xF1        = POP PSW
 *   0xF2        = JP
 *   0xF3        = DI
 *   0xF4        = CP
 *   0xF5        = PUSH PSW
 *   0xF6        = ORI
 *   0xF7        = RST 6
 *   0xF8        = RM
 *   0xF9        = SPHL
 *   0xFA        = JM
 *   0xFB        = EI
 *   0xFC        = CM
 *   0xFE        = CPI
 *   0xFF        = RST 7
 */
public class CPU {

    private final Registers registers;
    private final Flags flags;
    private final Memory memory;

    private boolean halted = false;   // becomes true when HLT executes
    private String lastError = null;  // last error message (if any)

    // Interrupt enable flip-flop (controlled by DI/EI)
    private boolean interruptsEnabled = false;

    public CPU(Registers registers, Flags flags, Memory memory) {
        this.registers = registers;
        this.flags = flags;
        this.memory = memory;
    }

    public boolean isHalted() { return halted; }
    public String getLastError() { return lastError; }
    public void clearError() { lastError = null; }
    public boolean areInterruptsEnabled() { return interruptsEnabled; }

    /** Reset CPU to initial state (clear halt, clear error) */
    public void reset() {
        halted = false;
        lastError = null;
        interruptsEnabled = false;
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
     * The 8085 has 246 valid opcodes (10 are undefined/unofficial).
     *
     * OPCODE PATTERNS:
     *   0x00         = NOP
     *   0x76         = HLT
     *   0x40-0x7F    = MOV instructions (except 0x76)
     *   0x80-0x87    = ADD B..A
     *   0x88-0x8F    = ADC B..A
     *   0x90-0x97    = SUB B..A
     *   0x98-0x9F    = SBB B..A
     *   0xA0-0xA7    = ANA B..A
     *   0xA8-0xAF    = XRA B..A
     *   0xB0-0xB7    = ORA B..A
     *   0xB8-0xBF    = CMP B..A
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
            // LXI - Load Register Pair Immediate
            // Loads a 16-bit immediate into a register pair (little-endian from memory)
            // LXI B,d16 = 0x01; LXI D,d16 = 0x11; LXI H,d16 = 0x21; LXI SP,d16 = 0x31
            // ==================
            case 0x01 -> { int v = fetch16(); registers.setC(v & 0xFF); registers.setB((v >> 8) & 0xFF); } // LXI B
            case 0x11 -> { int v = fetch16(); registers.setE(v & 0xFF); registers.setD((v >> 8) & 0xFF); } // LXI D
            case 0x21 -> { int v = fetch16(); registers.setL(v & 0xFF); registers.setH((v >> 8) & 0xFF); } // LXI H
            case 0x31 -> registers.setSP(fetch16()); // LXI SP

            // ==================
            // STAX - Store Accumulator Indirect
            // memory[rp] = A
            // STAX B = 0x02 (uses BC pair), STAX D = 0x12 (uses DE pair)
            // ==================
            case 0x02 -> memory.write(getBC(), registers.getA()); // STAX B
            case 0x12 -> memory.write(getDE(), registers.getA()); // STAX D

            // ==================
            // INX - Increment Register Pair (16-bit)
            // rp = rp + 1; no flags affected
            // ==================
            case 0x03 -> setBC(getBC() + 1); // INX B
            case 0x13 -> setDE(getDE() + 1); // INX D
            case 0x23 -> setHL(getHL() + 1); // INX H
            case 0x33 -> registers.setSP((registers.getSP() + 1) & 0xFFFF); // INX SP

            // ==================
            // INR - Increment Register
            // reg = reg + 1; updates S, Z, AC, P; does NOT affect CY
            // ==================
            case 0x04 -> registers.setB(executeINR(registers.getB()));  // INR B
            case 0x0C -> registers.setC(executeINR(registers.getC()));  // INR C
            case 0x14 -> registers.setD(executeINR(registers.getD()));  // INR D
            case 0x1C -> registers.setE(executeINR(registers.getE()));  // INR E
            case 0x24 -> registers.setH(executeINR(registers.getH()));  // INR H
            case 0x2C -> registers.setL(executeINR(registers.getL()));  // INR L
            case 0x34 -> memory.write(getHL(), executeINR(memory.read(getHL()))); // INR M
            case 0x3C -> registers.setA(executeINR(registers.getA()));  // INR A

            // ==================
            // DCR - Decrement Register
            // reg = reg - 1; updates S, Z, AC, P; does NOT affect CY
            // ==================
            case 0x05 -> registers.setB(executeDCR(registers.getB()));  // DCR B
            case 0x0D -> registers.setC(executeDCR(registers.getC()));  // DCR C
            case 0x15 -> registers.setD(executeDCR(registers.getD()));  // DCR D
            case 0x1D -> registers.setE(executeDCR(registers.getE()));  // DCR E
            case 0x25 -> registers.setH(executeDCR(registers.getH()));  // DCR H
            case 0x2D -> registers.setL(executeDCR(registers.getL()));  // DCR L
            case 0x35 -> memory.write(getHL(), executeDCR(memory.read(getHL()))); // DCR M
            case 0x3D -> registers.setA(executeDCR(registers.getA()));  // DCR A

            // ==================
            // MVI - Move Immediate
            // Load a constant value directly into a register (or memory M)
            // Opcode tells us the destination register, next byte is the value
            // ==================
            case 0x06 -> registers.setB(fetchByte()); // MVI B, data
            case 0x0E -> registers.setC(fetchByte()); // MVI C, data
            case 0x16 -> registers.setD(fetchByte()); // MVI D, data
            case 0x1E -> registers.setE(fetchByte()); // MVI E, data
            case 0x26 -> registers.setH(fetchByte()); // MVI H, data
            case 0x2E -> registers.setL(fetchByte()); // MVI L, data
            case 0x36 -> memory.write(getHL(), fetchByte()); // MVI M, data
            case 0x3E -> registers.setA(fetchByte()); // MVI A, data

            // ==================
            // Rotate Instructions
            // RLC  = 0x07: Rotate A Left Circular  (bit7 → CY, bit7 → bit0)
            // RRC  = 0x0F: Rotate A Right Circular (bit0 → CY, bit0 → bit7)
            // RAL  = 0x17: Rotate A Left Through Carry  (bit7 → CY, old CY → bit0)
            // RAR  = 0x1F: Rotate A Right Through Carry (bit0 → CY, old CY → bit7)
            // Only CY flag is affected; S, Z, P, AC unchanged
            // ==================
            case 0x07 -> { // RLC
                int a = registers.getA();
                int msb = (a >> 7) & 1;
                registers.setA(((a << 1) | msb) & 0xFF);
                flags.setCY(msb == 1);
            }
            case 0x0F -> { // RRC
                int a = registers.getA();
                int lsb = a & 1;
                registers.setA(((a >> 1) | (lsb << 7)) & 0xFF);
                flags.setCY(lsb == 1);
            }
            case 0x17 -> { // RAL
                int a = registers.getA();
                int oldCY = flags.isCY() ? 1 : 0;
                int newCY = (a >> 7) & 1;
                registers.setA(((a << 1) | oldCY) & 0xFF);
                flags.setCY(newCY == 1);
            }
            case 0x1F -> { // RAR
                int a = registers.getA();
                int oldCY = flags.isCY() ? 1 : 0;
                int newCY = a & 1;
                registers.setA(((a >> 1) | (oldCY << 7)) & 0xFF);
                flags.setCY(newCY == 1);
            }

            // ==================
            // RIM - Read Interrupt Mask
            // Reads serial data input and interrupt masks into A
            // bit 7 = SID (serial input data), bit 6 = I7.5 pending,
            // bit 5 = I6.5 pending, bit 4 = I5.5 pending,
            // bit 3 = IE (interrupt enable), bits 2-0 = mask bits
            // (Simplified: returns 0 in this emulator)
            // ==================
            case 0x20 -> registers.setA(interruptsEnabled ? 0x08 : 0x00); // RIM

            // ==================
            // SIM - Set Interrupt Mask
            // Sets interrupt mask bits from A (bits 0-2), serial output (bit 7)
            // (Simplified: just captures interrupt enable state)
            // ==================
            case 0x30 -> { /* SIM - no full interrupt hardware modeled */ }

            // ==================
            // SHLD - Store HL Direct
            // memory[addr] = L, memory[addr+1] = H
            // ==================
            case 0x22 -> {
                int addr = fetch16();
                memory.write(addr, registers.getL());
                memory.write(addr + 1, registers.getH());
            }

            // ==================
            // LHLD - Load HL Direct
            // L = memory[addr], H = memory[addr+1]
            // ==================
            case 0x2A -> {
                int addr = fetch16();
                registers.setL(memory.read(addr));
                registers.setH(memory.read(addr + 1));
            }

            // ==================
            // DAA - Decimal Adjust Accumulator
            // Adjusts A after BCD addition/subtraction to keep it in valid BCD range.
            // If lower nibble > 9 or AC set: add 6 to lower nibble
            // If upper nibble > 9 or CY set: add 6 to upper nibble (effectively +0x60)
            // ==================
            case 0x27 -> executeDAA();

            // ==================
            // CMA - Complement Accumulator (bitwise NOT)
            // A = ~A; no flags changed
            // ==================
            case 0x2F -> registers.setA((~registers.getA()) & 0xFF);

            // ==================
            // STA - Store Accumulator Direct
            // memory[address] = A
            // ==================
            case 0x32 -> {
                int addr = fetch16();
                memory.write(addr, registers.getA());
            }

            // ==================
            // STC - Set Carry Flag
            // CY = 1; no other flags changed
            // ==================
            case 0x37 -> flags.setCY(true);

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
            // CMC - Complement Carry Flag
            // CY = !CY; no other flags changed
            // ==================
            case 0x3F -> flags.setCY(!flags.isCY());

            // ==================
            // DAD - Double Add (add register pair to HL)
            // HL = HL + rp; only CY flag affected
            // DAD B=0x09, DAD D=0x19, DAD H=0x29, DAD SP=0x39
            // ==================
            case 0x09 -> executeDAD(getBC());  // DAD B
            case 0x19 -> executeDAD(getDE());  // DAD D
            case 0x29 -> executeDAD(getHL());  // DAD H (HL + HL)
            case 0x39 -> executeDAD(registers.getSP()); // DAD SP

            // ==================
            // LDAX - Load Accumulator Indirect
            // A = memory[rp]  (rp = BC or DE)
            // LDAX B = 0x0A, LDAX D = 0x1A
            // ==================
            case 0x0A -> registers.setA(memory.read(getBC())); // LDAX B
            case 0x1A -> registers.setA(memory.read(getDE())); // LDAX D

            // ==================
            // DCX - Decrement Register Pair (16-bit)
            // rp = rp - 1; no flags affected
            // ==================
            case 0x0B -> setBC(getBC() - 1); // DCX B
            case 0x1B -> setDE(getDE() - 1); // DCX D
            case 0x2B -> setHL(getHL() - 1); // DCX H
            case 0x3B -> registers.setSP((registers.getSP() - 1) & 0xFFFF); // DCX SP

            // ==================
            // MOV - Move Register to Register
            // Pattern: 0x40 to 0x7F (except 0x76 which is HLT)
            // Opcode = 0x40 + (dst * 8) + src
            // dst/src codes: B=0, C=1, D=2, E=3, H=4, L=5, M=6, A=7
            // M means memory[HL]
            // ==================
            case 0x40 -> { /* MOV B,B - no-op */ }
            case 0x41 -> registers.setB(registers.getC());           // MOV B,C
            case 0x42 -> registers.setB(registers.getD());           // MOV B,D
            case 0x43 -> registers.setB(registers.getE());           // MOV B,E
            case 0x44 -> registers.setB(registers.getH());           // MOV B,H
            case 0x45 -> registers.setB(registers.getL());           // MOV B,L
            case 0x46 -> registers.setB(memory.read(getHL()));       // MOV B,M
            case 0x47 -> registers.setB(registers.getA());           // MOV B,A

            case 0x48 -> registers.setC(registers.getB());           // MOV C,B
            case 0x49 -> { /* MOV C,C - no-op */ }
            case 0x4A -> registers.setC(registers.getD());           // MOV C,D
            case 0x4B -> registers.setC(registers.getE());           // MOV C,E
            case 0x4C -> registers.setC(registers.getH());           // MOV C,H
            case 0x4D -> registers.setC(registers.getL());           // MOV C,L
            case 0x4E -> registers.setC(memory.read(getHL()));       // MOV C,M
            case 0x4F -> registers.setC(registers.getA());           // MOV C,A

            case 0x50 -> registers.setD(registers.getB());           // MOV D,B
            case 0x51 -> registers.setD(registers.getC());           // MOV D,C
            case 0x52 -> { /* MOV D,D - no-op */ }
            case 0x53 -> registers.setD(registers.getE());           // MOV D,E
            case 0x54 -> registers.setD(registers.getH());           // MOV D,H
            case 0x55 -> registers.setD(registers.getL());           // MOV D,L
            case 0x56 -> registers.setD(memory.read(getHL()));       // MOV D,M
            case 0x57 -> registers.setD(registers.getA());           // MOV D,A

            case 0x58 -> registers.setE(registers.getB());           // MOV E,B
            case 0x59 -> registers.setE(registers.getC());           // MOV E,C
            case 0x5A -> registers.setE(registers.getD());           // MOV E,D
            case 0x5B -> { /* MOV E,E - no-op */ }
            case 0x5C -> registers.setE(registers.getH());           // MOV E,H
            case 0x5D -> registers.setE(registers.getL());           // MOV E,L
            case 0x5E -> registers.setE(memory.read(getHL()));       // MOV E,M
            case 0x5F -> registers.setE(registers.getA());           // MOV E,A

            case 0x60 -> registers.setH(registers.getB());           // MOV H,B
            case 0x61 -> registers.setH(registers.getC());           // MOV H,C
            case 0x62 -> registers.setH(registers.getD());           // MOV H,D
            case 0x63 -> registers.setH(registers.getE());           // MOV H,E
            case 0x64 -> { /* MOV H,H - no-op */ }
            case 0x65 -> registers.setH(registers.getL());           // MOV H,L
            case 0x66 -> registers.setH(memory.read(getHL()));       // MOV H,M
            case 0x67 -> registers.setH(registers.getA());           // MOV H,A

            case 0x68 -> registers.setL(registers.getB());           // MOV L,B
            case 0x69 -> registers.setL(registers.getC());           // MOV L,C
            case 0x6A -> registers.setL(registers.getD());           // MOV L,D
            case 0x6B -> registers.setL(registers.getE());           // MOV L,E
            case 0x6C -> registers.setL(registers.getH());           // MOV L,H
            case 0x6D -> { /* MOV L,L - no-op */ }
            case 0x6E -> registers.setL(memory.read(getHL()));       // MOV L,M
            case 0x6F -> registers.setL(registers.getA());           // MOV L,A

            case 0x70 -> memory.write(getHL(), registers.getB());    // MOV M,B
            case 0x71 -> memory.write(getHL(), registers.getC());    // MOV M,C
            case 0x72 -> memory.write(getHL(), registers.getD());    // MOV M,D
            case 0x73 -> memory.write(getHL(), registers.getE());    // MOV M,E
            case 0x74 -> memory.write(getHL(), registers.getH());    // MOV M,H
            case 0x75 -> memory.write(getHL(), registers.getL());    // MOV M,L
            // 0x76 = HLT (handled above)
            case 0x77 -> memory.write(getHL(), registers.getA());    // MOV M,A

            case 0x78 -> registers.setA(registers.getB());           // MOV A,B
            case 0x79 -> registers.setA(registers.getC());           // MOV A,C
            case 0x7A -> registers.setA(registers.getD());           // MOV A,D
            case 0x7B -> registers.setA(registers.getE());           // MOV A,E
            case 0x7C -> registers.setA(registers.getH());           // MOV A,H
            case 0x7D -> registers.setA(registers.getL());           // MOV A,L
            case 0x7E -> registers.setA(memory.read(getHL()));       // MOV A,M
            case 0x7F -> { /* MOV A,A - no-op */ }

            // ==================
            // ADD - Add register (or M) to Accumulator
            // A = A + reg; updates all flags
            // opcode = 0x80 + reg_code
            // ==================
            case 0x80 -> executeADD(registers.getB());
            case 0x81 -> executeADD(registers.getC());
            case 0x82 -> executeADD(registers.getD());
            case 0x83 -> executeADD(registers.getE());
            case 0x84 -> executeADD(registers.getH());
            case 0x85 -> executeADD(registers.getL());
            case 0x86 -> executeADD(memory.read(getHL())); // ADD M
            case 0x87 -> executeADD(registers.getA());

            // ==================
            // ADC - Add register (or M) with Carry to Accumulator
            // A = A + reg + CY; updates all flags
            // opcode = 0x88 + reg_code
            // ==================
            case 0x88 -> executeADC(registers.getB());
            case 0x89 -> executeADC(registers.getC());
            case 0x8A -> executeADC(registers.getD());
            case 0x8B -> executeADC(registers.getE());
            case 0x8C -> executeADC(registers.getH());
            case 0x8D -> executeADC(registers.getL());
            case 0x8E -> executeADC(memory.read(getHL())); // ADC M
            case 0x8F -> executeADC(registers.getA());

            // ==================
            // SUB - Subtract register (or M) from Accumulator
            // A = A - reg; updates all flags
            // opcode = 0x90 + reg_code
            // ==================
            case 0x90 -> executeSUB(registers.getB());
            case 0x91 -> executeSUB(registers.getC());
            case 0x92 -> executeSUB(registers.getD());
            case 0x93 -> executeSUB(registers.getE());
            case 0x94 -> executeSUB(registers.getH());
            case 0x95 -> executeSUB(registers.getL());
            case 0x96 -> executeSUB(memory.read(getHL())); // SUB M
            case 0x97 -> executeSUB(registers.getA());

            // ==================
            // SBB - Subtract register (or M) with Borrow from Accumulator
            // A = A - reg - CY; updates all flags
            // opcode = 0x98 + reg_code
            // ==================
            case 0x98 -> executeSBB(registers.getB());
            case 0x99 -> executeSBB(registers.getC());
            case 0x9A -> executeSBB(registers.getD());
            case 0x9B -> executeSBB(registers.getE());
            case 0x9C -> executeSBB(registers.getH());
            case 0x9D -> executeSBB(registers.getL());
            case 0x9E -> executeSBB(memory.read(getHL())); // SBB M
            case 0x9F -> executeSBB(registers.getA());

            // ==================
            // ANA - AND Accumulator with register (or M)
            // A = A AND reg; CY=0, AC=1 (set on real 8085), updates S/Z/P
            // opcode = 0xA0 + reg_code
            // ==================
            case 0xA0 -> executeANA(registers.getB());
            case 0xA1 -> executeANA(registers.getC());
            case 0xA2 -> executeANA(registers.getD());
            case 0xA3 -> executeANA(registers.getE());
            case 0xA4 -> executeANA(registers.getH());
            case 0xA5 -> executeANA(registers.getL());
            case 0xA6 -> executeANA(memory.read(getHL())); // ANA M
            case 0xA7 -> executeANA(registers.getA());

            // ==================
            // XRA - XOR Accumulator with register (or M)
            // A = A XOR reg; CY=0, AC=0, updates S/Z/P
            // Useful: XRA A clears A and all flags (except sets Z=1, P=1)
            // opcode = 0xA8 + reg_code
            // ==================
            case 0xA8 -> executeXRA(registers.getB());
            case 0xA9 -> executeXRA(registers.getC());
            case 0xAA -> executeXRA(registers.getD());
            case 0xAB -> executeXRA(registers.getE());
            case 0xAC -> executeXRA(registers.getH());
            case 0xAD -> executeXRA(registers.getL());
            case 0xAE -> executeXRA(memory.read(getHL())); // XRA M
            case 0xAF -> executeXRA(registers.getA());

            // ==================
            // ORA - OR Accumulator with register (or M)
            // A = A OR reg; CY=0, AC=0, updates S/Z/P
            // opcode = 0xB0 + reg_code
            // ==================
            case 0xB0 -> executeORA(registers.getB());
            case 0xB1 -> executeORA(registers.getC());
            case 0xB2 -> executeORA(registers.getD());
            case 0xB3 -> executeORA(registers.getE());
            case 0xB4 -> executeORA(registers.getH());
            case 0xB5 -> executeORA(registers.getL());
            case 0xB6 -> executeORA(memory.read(getHL())); // ORA M
            case 0xB7 -> executeORA(registers.getA());

            // ==================
            // CMP - Compare Accumulator with register (or M)
            // Performs A - reg but DOES NOT change A.
            // Only updates flags (so you can check Z, CY etc.)
            // Used before JZ, JNZ, JC, JNC etc.
            // opcode = 0xB8 + reg_code
            // ==================
            case 0xB8 -> executeCMP(registers.getB());
            case 0xB9 -> executeCMP(registers.getC());
            case 0xBA -> executeCMP(registers.getD());
            case 0xBB -> executeCMP(registers.getE());
            case 0xBC -> executeCMP(registers.getH());
            case 0xBD -> executeCMP(registers.getL());
            case 0xBE -> executeCMP(memory.read(getHL())); // CMP M
            case 0xBF -> executeCMP(registers.getA());

            // ==================
            // ADI - Add Immediate to Accumulator
            // A = A + data
            // ==================
            case 0xC6 -> executeADD(fetchByte());

            // ==================
            // ACI - Add Immediate with Carry
            // A = A + data + CY
            // ==================
            case 0xCE -> executeADC(fetchByte());

            // ==================
            // SUI - Subtract Immediate from Accumulator
            // A = A - data
            // ==================
            case 0xD6 -> executeSUB(fetchByte());

            // ==================
            // SBI - Subtract Immediate with Borrow
            // A = A - data - CY
            // ==================
            case 0xDE -> executeSBB(fetchByte());

            // ==================
            // ANI - AND Immediate with Accumulator
            // A = A AND data
            // ==================
            case 0xE6 -> executeANA(fetchByte());

            // ==================
            // XRI - XOR Immediate with Accumulator
            // A = A XOR data
            // ==================
            case 0xEE -> executeXRA(fetchByte());

            // ==================
            // ORI - OR Immediate with Accumulator
            // A = A OR data
            // ==================
            case 0xF6 -> executeORA(fetchByte());

            // ==================
            // CPI - Compare Immediate with Accumulator
            // Flags set as if A - data; A unchanged
            // ==================
            case 0xFE -> executeCMP(fetchByte());

            // ==================
            // JMP - Unconditional Jump
            // PC = address (always jumps)
            // ==================
            case 0xC3 -> registers.setPC(fetch16());

            // ==================
            // Conditional Jumps — all fetch a 16-bit address from next 2 bytes.
            // Jump is taken only if the respective flag condition is met.
            // JC   = 0xDA: Jump if Carry       (CY=1)
            // JNC  = 0xD2: Jump if No Carry    (CY=0)
            // JZ   = 0xCA: Jump if Zero        (Z=1)
            // JNZ  = 0xC2: Jump if Not Zero    (Z=0)
            // JM   = 0xFA: Jump if Minus       (S=1)
            // JP   = 0xF2: Jump if Plus/Positive(S=0)
            // JPE  = 0xEA: Jump if Parity Even (P=1)
            // JPO  = 0xE2: Jump if Parity Odd  (P=0)
            // ==================
            case 0xC2 -> { int addr = fetch16(); if (!flags.isZ())  registers.setPC(addr); } // JNZ
            case 0xCA -> { int addr = fetch16(); if (flags.isZ())   registers.setPC(addr); } // JZ
            case 0xD2 -> { int addr = fetch16(); if (!flags.isCY()) registers.setPC(addr); } // JNC
            case 0xDA -> { int addr = fetch16(); if (flags.isCY())  registers.setPC(addr); } // JC
            case 0xE2 -> { int addr = fetch16(); if (!flags.isP())  registers.setPC(addr); } // JPO
            case 0xEA -> { int addr = fetch16(); if (flags.isP())   registers.setPC(addr); } // JPE
            case 0xF2 -> { int addr = fetch16(); if (!flags.isS())  registers.setPC(addr); } // JP
            case 0xFA -> { int addr = fetch16(); if (flags.isS())   registers.setPC(addr); } // JM

            // ==================
            // CALL - Unconditional Subroutine Call
            // Pushes return address (PC after CALL) onto stack, then jumps to address
            // ==================
            case 0xCD -> {
                int addr = fetch16();
                pushWord(registers.getPC()); // save return address
                registers.setPC(addr);
            }

            // ==================
            // Conditional CALLs — push return address and jump only if condition met
            // CC  = 0xDC: Call if Carry       (CY=1)
            // CNC = 0xD4: Call if No Carry    (CY=0)
            // CZ  = 0xCC: Call if Zero        (Z=1)
            // CNZ = 0xC4: Call if Not Zero    (Z=0)
            // CM  = 0xFC: Call if Minus       (S=1)
            // CP  = 0xF4: Call if Plus        (S=0)
            // CPE = 0xEC: Call if Parity Even (P=1)
            // CPO = 0xE4: Call if Parity Odd  (P=0)
            // ==================
            case 0xC4 -> { int addr = fetch16(); if (!flags.isZ())  { pushWord(registers.getPC()); registers.setPC(addr); } } // CNZ
            case 0xCC -> { int addr = fetch16(); if (flags.isZ())   { pushWord(registers.getPC()); registers.setPC(addr); } } // CZ
            case 0xD4 -> { int addr = fetch16(); if (!flags.isCY()) { pushWord(registers.getPC()); registers.setPC(addr); } } // CNC
            case 0xDC -> { int addr = fetch16(); if (flags.isCY())  { pushWord(registers.getPC()); registers.setPC(addr); } } // CC
            case 0xE4 -> { int addr = fetch16(); if (!flags.isP())  { pushWord(registers.getPC()); registers.setPC(addr); } } // CPO
            case 0xEC -> { int addr = fetch16(); if (flags.isP())   { pushWord(registers.getPC()); registers.setPC(addr); } } // CPE
            case 0xF4 -> { int addr = fetch16(); if (!flags.isS())  { pushWord(registers.getPC()); registers.setPC(addr); } } // CP
            case 0xFC -> { int addr = fetch16(); if (flags.isS())   { pushWord(registers.getPC()); registers.setPC(addr); } } // CM

            // ==================
            // RET - Unconditional Return from Subroutine
            // PC = memory[SP]; SP += 2
            // ==================
            case 0xC9 -> registers.setPC(popWord());

            // ==================
            // Conditional RETURNs — pop PC from stack only if condition met
            // RNZ = 0xC0: Return if Not Zero   (Z=0)
            // RZ  = 0xC8: Return if Zero       (Z=1)
            // RNC = 0xD0: Return if No Carry   (CY=0)
            // RC  = 0xD8: Return if Carry      (CY=1)
            // RPO = 0xE0: Return if Parity Odd (P=0)
            // RPE = 0xE8: Return if Parity Even(P=1)
            // RP  = 0xF0: Return if Plus       (S=0)
            // RM  = 0xF8: Return if Minus      (S=1)
            // ==================
            case 0xC0 -> { if (!flags.isZ())  registers.setPC(popWord()); } // RNZ
            case 0xC8 -> { if (flags.isZ())   registers.setPC(popWord()); } // RZ
            case 0xD0 -> { if (!flags.isCY()) registers.setPC(popWord()); } // RNC
            case 0xD8 -> { if (flags.isCY())  registers.setPC(popWord()); } // RC
            case 0xE0 -> { if (!flags.isP())  registers.setPC(popWord()); } // RPO
            case 0xE8 -> { if (flags.isP())   registers.setPC(popWord()); } // RPE
            case 0xF0 -> { if (!flags.isS())  registers.setPC(popWord()); } // RP
            case 0xF8 -> { if (flags.isS())   registers.setPC(popWord()); } // RM

            // ==================
            // RST - Restart (software interrupt / vectored call)
            // Pushes PC onto stack, then jumps to n*8 (fixed restart address)
            // RST 0 = 0xC7 → 0x0000
            // RST 1 = 0xCF → 0x0008
            // RST 2 = 0xD7 → 0x0010
            // RST 3 = 0xDF → 0x0018
            // RST 4 = 0xE7 → 0x0020
            // RST 5 = 0xEF → 0x0028
            // RST 6 = 0xF7 → 0x0030
            // RST 7 = 0xFF → 0x0038
            // ==================
            case 0xC7 -> { pushWord(registers.getPC()); registers.setPC(0x0000); } // RST 0
            case 0xCF -> { pushWord(registers.getPC()); registers.setPC(0x0008); } // RST 1
            case 0xD7 -> { pushWord(registers.getPC()); registers.setPC(0x0010); } // RST 2
            case 0xDF -> { pushWord(registers.getPC()); registers.setPC(0x0018); } // RST 3
            case 0xE7 -> { pushWord(registers.getPC()); registers.setPC(0x0020); } // RST 4
            case 0xEF -> { pushWord(registers.getPC()); registers.setPC(0x0028); } // RST 5
            case 0xF7 -> { pushWord(registers.getPC()); registers.setPC(0x0030); } // RST 6
            case 0xFF -> { pushWord(registers.getPC()); registers.setPC(0x0038); } // RST 7

            // ==================
            // PUSH - Push Register Pair onto Stack
            // SP--, memory[SP] = high; SP--, memory[SP] = low
            // PUSH B=0xC5, PUSH D=0xD5, PUSH H=0xE5, PUSH PSW=0xF5
            // PSW = Accumulator (A) paired with Flags register
            // ==================
            case 0xC5 -> pushWord(getBC());  // PUSH B
            case 0xD5 -> pushWord(getDE());  // PUSH D
            case 0xE5 -> pushWord(getHL());  // PUSH H
            case 0xF5 -> pushWord(getPSW()); // PUSH PSW (A + Flags)

            // ==================
            // POP - Pop Register Pair from Stack
            // low = memory[SP], SP++; high = memory[SP], SP++
            // POP B=0xC1, POP D=0xD1, POP H=0xE1, POP PSW=0xF1
            // ==================
            case 0xC1 -> setBC(popWord());  // POP B
            case 0xD1 -> setDE(popWord());  // POP D
            case 0xE1 -> setHL(popWord());  // POP H
            case 0xF1 -> setPSW(popWord()); // POP PSW (restores A + Flags)

            // ==================
            // XTHL - Exchange HL with Top of Stack
            // Temp = memory[SP]; memory[SP] = L; L = Temp
            // Temp = memory[SP+1]; memory[SP+1] = H; H = Temp
            // ==================
            case 0xE3 -> {
                int sp = registers.getSP();
                int l = registers.getL();
                int h = registers.getH();
                registers.setL(memory.read(sp));
                registers.setH(memory.read(sp + 1));
                memory.write(sp, l);
                memory.write(sp + 1, h);
            }

            // ==================
            // SPHL - Move HL to Stack Pointer
            // SP = HL; no flags affected
            // ==================
            case 0xF9 -> registers.setSP(getHL());

            // ==================
            // PCHL - Move HL to Program Counter (indirect jump)
            // PC = HL; no flags affected
            // ==================
            case 0xE9 -> registers.setPC(getHL());

            // ==================
            // XCHG - Exchange HL with DE
            // H↔D, L↔E; no flags affected
            // ==================
            case 0xEB -> {
                int tempHL = getHL();
                setHL(getDE());
                setDE(tempHL);
            }

            // ==================
            // IN - Input from I/O Port
            // A = port[imm8]; (emulated as NOP — no real I/O hardware)
            // ==================
            case 0xDB -> {
                int port = fetchByte();
                // In a real 8085, this reads from an external I/O device.
                // Here we set A to 0 as a no-op simulation.
                registers.setA(0x00);
            }

            // ==================
            // OUT - Output to I/O Port
            // port[imm8] = A; (emulated as NOP — no real I/O hardware)
            // ==================
            case 0xD3 -> fetchByte(); // consume port byte, discard (no I/O hardware)

            // ==================
            // DI - Disable Interrupts
            // Clears the interrupt enable flip-flop
            // ==================
            case 0xF3 -> interruptsEnabled = false;

            // ==================
            // EI - Enable Interrupts
            // Sets the interrupt enable flip-flop (takes effect after next instruction)
            // ==================
            case 0xFB -> interruptsEnabled = true;

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
    // Helper: Register Pair Access
    // The 8085 pairs B/C, D/E, H/L for 16-bit operations
    // ==================

    private int getBC() { return (registers.getB() << 8) | registers.getC(); }
    private int getDE() { return (registers.getD() << 8) | registers.getE(); }
    private int getHL() { return (registers.getH() << 8) | registers.getL(); }

    private void setBC(int v) { v &= 0xFFFF; registers.setB((v >> 8) & 0xFF); registers.setC(v & 0xFF); }
    private void setDE(int v) { v &= 0xFFFF; registers.setD((v >> 8) & 0xFF); registers.setE(v & 0xFF); }
    private void setHL(int v) { v &= 0xFFFF; registers.setH((v >> 8) & 0xFF); registers.setL(v & 0xFF); }

    /**
     * PSW - Program Status Word: A paired with Flags as a 16-bit value.
     * Upper byte = A, lower byte = encoded flags register.
     *
     * Flags bit layout in PSW (8080/8085 standard):
     *   bit 7 = S, bit 6 = Z, bit 5 = 0, bit 4 = AC,
     *   bit 3 = 0, bit 2 = P, bit 1 = 1, bit 0 = CY
     */
    private int getPSW() {
        int f = 0x02; // bit 1 is always 1 per 8080/8085 spec
        if (flags.isS())  f |= 0x80;
        if (flags.isZ())  f |= 0x40;
        if (flags.isAC()) f |= 0x10;
        if (flags.isP())  f |= 0x04;
        if (flags.isCY()) f |= 0x01;
        return (registers.getA() << 8) | f;
    }

    private void setPSW(int psw) {
        registers.setA((psw >> 8) & 0xFF);
        int f = psw & 0xFF;
        flags.setS( (f & 0x80) != 0);
        flags.setZ( (f & 0x40) != 0);
        flags.setAC((f & 0x10) != 0);
        flags.setP( (f & 0x04) != 0);
        flags.setCY((f & 0x01) != 0);
    }

    // ==================
    // Helper: Stack Operations
    // SP grows downward (high → low addresses as items are pushed)
    // ==================

    /**
     * Push a 16-bit word onto the stack.
     * SP--, memory[SP] = high byte; SP--, memory[SP] = low byte
     */
    private void pushWord(int value) {
        int sp = (registers.getSP() - 1) & 0xFFFF;
        memory.write(sp, (value >> 8) & 0xFF);   // high byte
        sp = (sp - 1) & 0xFFFF;
        memory.write(sp, value & 0xFF);           // low byte
        registers.setSP(sp);
    }

    /**
     * Pop a 16-bit word from the stack.
     * low = memory[SP], SP++; high = memory[SP], SP++
     */
    private int popWord() {
        int sp = registers.getSP();
        int low  = memory.read(sp);
        int high = memory.read((sp + 1) & 0xFFFF);
        registers.setSP((sp + 2) & 0xFFFF);
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
     * ADC: A = A + value + CY
     * Like ADD but also adds the current carry flag.
     * Used for multi-byte addition where you chain 8-bit adds.
     */
    private void executeADC(int value) {
        int a = registers.getA();
        int cy = flags.isCY() ? 1 : 0;
        int result = a + value + cy;
        boolean carry = result > 0xFF;
        boolean auxCarry = ((a & 0x0F) + (value & 0x0F) + cy) > 0x0F;
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
     * SBB: A = A - value - CY
     * Like SUB but also subtracts the carry flag (borrow from previous op).
     * Used for multi-byte subtraction.
     */
    private void executeSBB(int value) {
        int a = registers.getA();
        int cy = flags.isCY() ? 1 : 0;
        int result = a - value - cy;
        boolean borrow = result < 0;
        boolean auxBorrow = (a & 0x0F) < ((value & 0x0F) + cy);
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

    /**
     * DAD: HL = HL + rp
     * Only CY is affected (set if 16-bit result > 0xFFFF)
     * All other flags (S, Z, AC, P) are NOT changed
     */
    private void executeDAD(int rp) {
        int hl = getHL();
        int result = hl + rp;
        flags.setCY(result > 0xFFFF);
        setHL(result & 0xFFFF);
    }

    /**
     * DAA - Decimal Adjust Accumulator
     * Adjusts A after BCD addition to produce correct 2-digit BCD result.
     * After ADD/ADC: if lower nibble > 9 or AC set → add 6 to lower nibble
     *               if upper nibble > 9 or CY set → add 0x60 (sets CY)
     */
    private void executeDAA() {
        int a = registers.getA();
        int correction = 0;
        boolean newCY = flags.isCY();

        // Lower nibble adjustment
        if (flags.isAC() || (a & 0x0F) > 9) {
            correction |= 0x06;
        }
        // Upper nibble adjustment
        if (flags.isCY() || a > 0x99) {
            correction |= 0x60;
            newCY = true;
        }

        int result = (a + correction) & 0xFF;
        boolean auxCarry = ((a & 0x0F) + (correction & 0x0F)) > 0x0F;
        registers.setA(result);
        flags.updateFlags(result, newCY, auxCarry);
        flags.setCY(newCY);
    }

    /** ANA: A = A AND value; CY=0, AC=0 (logical AND) */
    private void executeANA(int value) {
        int result = registers.getA() & value;
        registers.setA(result);
        flags.updateFlagsLogical(result);
    }

    /** ORA: A = A OR value; CY=0, AC=0 */
    private void executeORA(int value) {
        int result = registers.getA() | value;
        registers.setA(result);
        flags.updateFlagsLogical(result);
    }

    /** XRA: A = A XOR value; CY=0, AC=0 */
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