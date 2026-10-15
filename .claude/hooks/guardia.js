#!/usr/bin/env node
// Guardia de Karsy (hook PreToolUse).
// Se ejecuta ANTES de cada comando o edición. Si la acción es riesgosa,
// sale con código 2: Claude Code la bloquea y le muestra al agente el motivo.

const fs = require("fs");
const path = require("path");

let entrada = "";
process.stdin.on("data", (c) => (entrada += c));
process.stdin.on("end", () => {
  let datos;
  try {
    datos = JSON.parse(entrada || "{}");
  } catch {
    process.exit(0); // si no se entiende la entrada, no estorbar
  }

  const herramienta = datos.tool_name || "";
  const args = datos.tool_input || {};
  const raiz = process.env.CLAUDE_PROJECT_DIR || datos.cwd || process.cwd();

  const bloquear = (motivo) => {
    process.stderr.write(`Bloqueado por la guardia de Karsy: ${motivo}\n`);
    process.exit(2);
  };

  // ---- Comandos de terminal ----
  if (herramienta === "Bash" || herramienta === "PowerShell") {
    const cmd = String(args.command || "");
    const reglas = [
      [/\bgit\s+push\b/, "no se hace push; el humano revisa y sube los cambios."],
      [/\bgit\s+reset\s+--hard\b/, "git reset --hard borra trabajo sin posibilidad de recuperarlo."],
      [/\bgit\s+clean\s+-[a-z]*f/, "git clean -f borra archivos sin posibilidad de recuperarlos."],
      [/\bgit\s+(checkout|switch)\s+(main|develop)\b/, "no se trabaja directo en main ni develop."],
      [/\bgit\s+branch\s+-D\b/, "no se borran ramas."],
      [/\brm\s+-[a-z]*r[a-z]*f|\brm\s+-[a-z]*f[a-z]*r/, "rm -rf no está permitido."],
      [/Remove-Item[^\n]*-Recurse/i, "borrado recursivo no está permitido."],
      [/\bsupabase\s+db\s+push\b/, "las migraciones a la base remota las aplica el humano."],
      [/\bsupabase\b[^\n]*--linked/, "no se ejecutan comandos contra la base remota (--linked)."],
      [/\bsupabase\s+migration\s+repair\b/, "migration repair altera el historial de la base remota."],
      [/\b(cat|type|Get-Content|less|more|head|tail)\b[^\n]*(local\.properties|\.env\b)/i, "no se leen archivos con llaves."],
      [/\b(npm|yarn|pnpm)\s+(install|add|i)\s+\S/, "no se agregan dependencias nuevas sin pedirlo."],
    ];
    for (const [patron, motivo] of reglas) {
      if (patron.test(cmd)) bloquear(motivo);
    }
    process.exit(0);
  }

  // ---- Ediciones de archivos ----
  if (["Edit", "Write", "MultiEdit", "NotebookEdit"].includes(herramienta)) {
    const archivo = String(args.file_path || args.notebook_path || "");
    if (!archivo) process.exit(0);
    const abs = path.resolve(raiz, archivo);
    const rel = path.relative(raiz, abs).split(path.sep).join("/");

    if (rel.startsWith(".claude/hooks/") || rel === ".claude/settings.json") {
      bloquear("los agentes no pueden cambiar sus propias reglas ni hooks.");
    }
    if (/(^|\/)local\.properties$/.test(rel) || /(^|\/)\.env(\.|$)/.test(rel) && !rel.endsWith(".env.example")) {
      bloquear("no se editan archivos con llaves.");
    }
    if (/^supabase\/migrations\/.+\.sql$/.test(rel) && fs.existsSync(abs)) {
      bloquear("las migraciones existentes no se editan; crea una migración nueva con un timestamp posterior.");
    }
    if (rel.startsWith("..")) {
      bloquear("no se editan archivos fuera del proyecto.");
    }
    process.exit(0);
  }

  process.exit(0);
});
