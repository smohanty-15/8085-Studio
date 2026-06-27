# 8085 Studio v1.0

A browser-based Intel 8085 microprocessor emulator. Write assembly, assemble it, run it, and watch registers and memory update in real time.

---

## What You Need Installed

| Tool | Version | Download |
|------|---------|----------|
| Java | 21+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| Node.js | 18+ | https://nodejs.org |

---

## Quick Start (2 terminals)

### Terminal 1 — Start the Backend (Spring Boot)

```bash
cd 8085-Studio/backend
mvn spring-boot:run
```

You should see:
```
Started Main in X.XXX seconds (JVM running for ...)
```

Backend runs at: **http://localhost:9090**

---

### Terminal 2 — Start the Frontend (React + Vite)

```bash
cd 8085-Studio/frontend
npm install
npm run dev
```

Frontend runs at: **http://localhost:5173**

---

## How to Use

1. Open **http://localhost:5173** in your browser
2. Write 8085 assembly code in the editor (or click **Examples** to load a sample)
3. Click **Assemble** (or press Ctrl+Enter)
4. Click **Run** to execute the whole program, or **Step** to go one instruction at a time
5. Watch the Registers and Flags update on the right
6. Watch memory in the Memory Viewer at the bottom (the ▶ arrow shows current PC)
7. Click **Reset** to clear everything and start over

---

## Supported Instructions

### Data Transfer
| Instruction | Example | What it does |
|------------|---------|--------------|
| `MOV` | `MOV A, B` | Copy B into A |
| `MVI` | `MVI A, 10H` | Load immediate value 16 into A |
| `LDA` | `LDA 2050H` | Load A from memory address 2050H |
| `STA` | `STA 2050H` | Store A to memory address 2050H |

### Arithmetic
| Instruction | Example | What it does |
|------------|---------|--------------|
| `ADD` | `ADD B` | A = A + B |
| `ADI` | `ADI 05H` | A = A + 5 (immediate) |
| `SUB` | `SUB C` | A = A - C |
| `INR` | `INR B` | B = B + 1 |
| `DCR` | `DCR B` | B = B - 1 |

### Logical
| Instruction | Example | What it does |
|------------|---------|--------------|
| `ANA` | `ANA B` | A = A AND B |
| `ORA` | `ORA B` | A = A OR B |
| `XRA` | `XRA A` | A = A XOR A = 0 (clear A) |
| `CMP` | `CMP B` | Compare A with B (sets flags only) |

### Branching
| Instruction | Example | What it does |
|------------|---------|--------------|
| `JMP` | `JMP LOOP` | Unconditional jump |
| `JZ`  | `JZ DONE`  | Jump if Zero flag = 1 |
| `JNZ` | `JNZ LOOP` | Jump if Zero flag = 0 |

### Control
| Instruction | What it does |
|------------|--------------|
| `NOP` | No operation |
| `HLT` | Halt the CPU |

---

## Number Formats

```asm
MVI A, 10H      ; Hex (most common in 8085)
MVI A, 16       ; Decimal
MVI A, 00010000B ; Binary
```

---

## Labels and ORG

```asm
        ORG 2000H    ; Start loading at address 0x2000

START:               ; This is a label
        MVI A, 00H
LOOP:
        INR A
        JNZ LOOP     ; Jump to LOOP
        HLT
```

---

## Project Structure

```
8085-Studio/
├── backend/                          ← Spring Boot Java
│   └── src/main/java/com/studio8085/
│       ├── Main.java                 ← Spring Boot entry point
│       ├── controller/
│       │   └── EmulatorController.java   ← REST API endpoints
│       ├── service/
│       │   └── EmulatorService.java      ← Business logic
│       └── emulator/
│           ├── CPU.java              ← Fetch-decode-execute engine
│           ├── Registers.java        ← A, B, C, D, E, H, L, PC, SP
│           ├── Flags.java            ← S, Z, AC, P, CY
│           ├── Memory.java           ← 64KB memory array
│           ├── Assembler.java        ← Text → machine code bytes
│           └── EmulatorSession.java  ← Bundles everything together
│
└── frontend/                         ← React + Vite + Tailwind
    └── src/
        ├── App.tsx                   ← Main layout
        ├── components/
        │   ├── Toolbar.tsx           ← Assemble/Run/Step/Reset buttons
        │   ├── CodeEditor.tsx        ← Monaco editor with syntax highlighting
        │   ├── RegisterPanel.tsx     ← Shows A, B, C... PC, SP values
        │   ├── FlagsPanel.tsx        ← Shows S, Z, AC, P, CY flags
        │   ├── MemoryViewer.tsx      ← Shows memory as a table
        │   ├── MessagePanel.tsx      ← Error and success messages
        │   └── SamplePrograms.tsx    ← Example program dropdown
        ├── hooks/
        │   └── useEmulator.ts        ← All state management
        ├── services/
        │   └── api.ts                ← HTTP calls to Spring Boot
        └── types/
            └── emulator.ts           ← TypeScript interfaces
```

---

## API Endpoints

All endpoints are at `http://localhost:9090/api`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/assemble` | Assemble source code |
| `POST` | `/api/step` | Execute one instruction |
| `POST` | `/api/run` | Run until HLT |
| `POST` | `/api/reset` | Reset CPU + memory |
| `GET`  | `/api/state` | Get CPU state |
| `GET`  | `/api/memory?start=0&length=64` | Get memory |

---

## Troubleshooting

**Backend won't start:**
- Make sure Java 21 is installed: `java --version`
- Make sure port 9090 is free

**Frontend can't connect to backend:**
- Make sure Spring Boot is running first
- Check that you see "Started Main" in the backend terminal
- The Vite proxy forwards `/api` → `localhost:9090` automatically

**"Unknown instruction" error:**
- Check spelling (instructions are case-insensitive: `MVI` = `mvi`)
- Make sure the instruction is in the supported list above

**Infinite loop / program won't stop:**
- Make sure your loop has a proper exit condition
- Always end programs with `HLT`
- The emulator stops automatically after 100,000 instructions
