/**
 * SamplePrograms Component
 *
 * A dropdown menu with pre-written example programs.
 * Great for beginners to see real 8085 assembly patterns.
 */

interface SampleProgram {
  name: string
  description: string
  code: string
}

const SAMPLES: SampleProgram[] = [
  {
    name: 'Counter (1 to 5)',
    description: 'Count from 0 to 5 using INR and DCR',
    code: `; Count from 0 to 5
        ORG 2000H

START:
        MVI B, 05H      ; B = 5 (loop counter)
        MVI A, 00H      ; A = 0

LOOP:
        INR A           ; A++
        DCR B           ; B--
        JNZ LOOP        ; repeat while B != 0

        STA 3000H       ; save result to memory
        HLT
`,
  },
  {
    name: 'Add Two Numbers',
    description: 'A simple addition: 25 + 30',
    code: `; Add two numbers: 25 + 30
; Result will be in register A

        ORG 2000H

        MVI A, 19H      ; A = 25 (0x19 = 25 decimal)
        MVI B, 1EH      ; B = 30 (0x1E = 30 decimal)
        ADD B           ; A = A + B = 55

        STA 3000H       ; Store result at 3000H
        HLT
`,
  },
  {
    name: 'Subtract Two Numbers',
    description: 'Subtraction: 50 - 20',
    code: `; Subtract: 50 - 20 = 30
        ORG 2000H

        MVI A, 32H      ; A = 50 (0x32)
        MVI B, 14H      ; B = 20 (0x14)
        SUB B           ; A = A - B = 30

        STA 3000H
        HLT
`,
  },
  {
    name: 'Find Largest of Two',
    description: 'Compare two numbers, store the larger one',
    code: `; Find the largest of two numbers
; Numbers: A=40, B=25 → result: 40

        ORG 2000H

        MVI A, 28H      ; A = 40
        MVI B, 19H      ; B = 25

        CMP B           ; Compare A with B (set flags)
                        ; If A >= B, CY=0, so JNC would jump
                        ; If A < B,  CY=1
        JNZ DONE        ; If A != B (i.e., A > B), skip
        MOV A, B        ; Otherwise A < B, so A = B

DONE:
        STA 3000H       ; Store the larger value
        HLT
`,
  },
  {
    name: 'Copy Data',
    description: 'Move a value between registers',
    code: `; Move data between registers
        ORG 2000H

        MVI A, 42H      ; A = 0x42
        MOV B, A        ; B = A
        MOV C, B        ; C = B
        MOV D, C        ; D = C

        ; Now A, B, C, D all = 0x42
        HLT
`,
  },
  {
    name: 'Bitwise AND Mask',
    description: 'Use ANA to extract the lower nibble',
    code: `; AND operation: extract lower 4 bits (nibble)
; Input:  A = 0xAB (10101011)
; Mask:   B = 0x0F (00001111)
; Result: A = 0x0B (00001011)

        ORG 2000H

        MVI A, 0ABH     ; A = 10101011
        MVI B, 0FH      ; B = 00001111 (mask)
        ANA B           ; A = A AND B = 00001011

        STA 3000H
        HLT
`,
  },
]

interface SampleProgramsProps {
  onLoad: (code: string) => void
}

export function SamplePrograms({ onLoad }: SampleProgramsProps) {
  return (
    <div className="relative group">
      <button className="px-3 py-1 text-xs text-[#8B949E] border border-transparent rounded hover:text-[#58A6FF] hover:border-[#58A6FF]/30 hover:bg-[#58A6FF]/10 transition-all">
        📂 Examples ▾
      </button>

      <div className="absolute top-full left-0 mt-1 w-72 bg-[#161B22] border border-[#30363D] rounded-lg shadow-xl z-50 hidden group-hover:block">
        <div className="p-1">
          {SAMPLES.map((sample) => (
            <button
              key={sample.name}
              onClick={() => onLoad(sample.code)}
              className="w-full text-left px-3 py-2 rounded text-xs hover:bg-[#21262D] transition-colors"
            >
              <div className="text-[#E6EDF3] font-medium">{sample.name}</div>
              <div className="text-[#8B949E] text-[10px] mt-0.5">{sample.description}</div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
