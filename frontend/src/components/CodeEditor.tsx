/**
 * CodeEditor Component
 * Uses Monaco Editor (VS Code's editor engine) for 8085 assembly.
 */

import Editor from '@monaco-editor/react'
import { useRef } from 'react'
import type * as Monaco from 'monaco-editor'
import { useTheme } from '../context/ThemeContext'

interface CodeEditorProps {
  value: string
  onChange: (value: string) => void
  onAssemble: () => void
}

export const DEFAULT_CODE = `; 8085 Assembly - Counting Demo
; -----------------------------------------------
; This program counts from 1 to 5 and stops.
; Click Assemble, then use Step to go one instruction at a time!

        ORG 2000H       ; Load program at address 2000H

START:
        MVI B, 05H      ; B = 5  (loop counter)
        MVI A, 00H      ; A = 0  (accumulator, will count up)

LOOP:
        INR A           ; A = A + 1
        DCR B           ; B = B - 1
        JNZ LOOP        ; if B != 0, jump back to LOOP

        STA 3000H       ; Store final value of A at memory address 3000H

        HLT             ; Stop the CPU
`

function setupMonaco(monaco: typeof Monaco) {
  // Only register once
  const existing = monaco.languages.getLanguages().find(l => l.id === '8085asm')
  if (existing) return

  monaco.languages.register({ id: '8085asm' })

  monaco.languages.setMonarchTokensProvider('8085asm', {
    tokenizer: {
      root: [
        [/;.*$/, 'comment'],
        [/^[A-Za-z_][A-Za-z0-9_]*(?=\s*:)/, 'type.identifier'],
        [/\b(MOV|MVI|LDA|STA|ADD|ADI|SUB|INR|DCR|ANA|ORA|XRA|CMP|JMP|JZ|JNZ|NOP|HLT|ORG)\b/i, 'keyword'],
        [/\b(A|B|C|D|E|H|L|SP|PC|M)\b/, 'variable.name'],
        [/\b[0-9A-Fa-f]+H\b/, 'number.hex'],
        [/\b\d+\b/, 'number'],
        [/[,]/, 'delimiter'],
      ],
    },
  } as Monaco.languages.IMonarchLanguage)

  monaco.editor.defineTheme('8085-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'comment', foreground: '6e7681', fontStyle: 'italic' },
      { token: 'keyword', foreground: 'ff7b72', fontStyle: 'bold' },
      { token: 'type.identifier', foreground: 'f0c674' },
      { token: 'variable.name', foreground: '79c0ff' },
      { token: 'number.hex', foreground: '39d353' },
      { token: 'number', foreground: '39d353' },
      { token: 'delimiter', foreground: '8b949e' },
    ],
    colors: {
      'editor.background': '#0D1117',
      'editor.foreground': '#E6EDF3',
      'editor.lineHighlightBackground': '#161B22',
      'editor.selectionBackground': '#264F78',
      'editorLineNumber.foreground': '#484F58',
      'editorLineNumber.activeForeground': '#8B949E',
      'editorGutter.background': '#0D1117',
      'editorCursor.foreground': '#39D353',
    },
  })

  // Light Theme
  monaco.editor.defineTheme('8085-light', {
    base: 'vs',
    inherit: true,
    rules: [
      { token: 'comment', foreground: '6A737D', fontStyle: 'italic' },
      { token: 'keyword', foreground: 'CF222E', fontStyle: 'bold' },
      { token: 'type.identifier', foreground: '9A6700' },
      { token: 'variable.name', foreground: '0969DA' },
      { token: 'number.hex', foreground: '1A7F37' },
      { token: 'number', foreground: '1A7F37' },
      { token: 'delimiter', foreground: '57606A' },
    ],
    colors: {
      'editor.background': '#FFFFFF',
      'editor.foreground': '#1F2328',
      'editor.lineHighlightBackground': '#F6F8FA',
      'editor.selectionBackground': '#B6D6FF',
      'editorLineNumber.foreground': '#8C959F',
      'editorLineNumber.activeForeground': '#57606A',
      'editorGutter.background': '#FFFFFF',
      'editorCursor.foreground': '#0969DA',
    },
  })
}

export function CodeEditor({ value, onChange, onAssemble }: CodeEditorProps) {
  const onAssembleRef = useRef(onAssemble)
  onAssembleRef.current = onAssemble

  const { theme } = useTheme()

  return (
    <div
      className="h-full"
      style={{
        border: '1px solid var(--border-color)',
        borderRadius: 6,
        overflow: 'hidden',
      }}
    >
      <Editor
        language="8085asm"
        value={value}
        onChange={v => onChange(v ?? '')}
        theme={theme === 'dark' ? '8085-dark' : '8085-light'}
        beforeMount={setupMonaco}
        options={{
          fontSize: 13,
          fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace",
          lineNumbers: 'on',
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          wordWrap: 'off',
          tabSize: 8,
          automaticLayout: true,
          padding: { top: 8, bottom: 8 },
          renderLineHighlight: 'line',
          suggestOnTriggerCharacters: false,
          quickSuggestions: false,
          parameterHints: { enabled: false },
        }}
        onMount={(editor, monaco) => {
          editor.addAction({
            id: 'assemble',
            label: 'Assemble',
            keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter],
            run: () => onAssembleRef.current(),
          })
        }}
      />
    </div>
  )
}