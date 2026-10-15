#!/usr/bin/env node
// Verificación al terminar (hook Stop).
// Cuando el agente principal quiere dar la tarea por terminada, este script
// corre las pruebas de lo que cambió. Si fallan, sale con código 2 y el agente
// NO puede terminar: recibe el error y tiene que arreglarlo.
// Para no gastar sin fin, deja de insistir después de 3 intentos por sesión.

const { execSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");

const MAX_INTENTOS = 3;

let entrada = "";
process.stdin.on("data", (c) => (entrada += c));
process.stdin.on("end", () => {
  let datos = {};
  try {
    datos = JSON.parse(entrada || "{}");
  } catch {}

  const raiz = process.env.CLAUDE_PROJECT_DIR || datos.cwd || process.cwd();
  const esWindows = process.platform === "win32";
  const correr = (cmd, cwd = raiz) =>
    execSync(cmd, { cwd, stdio: "pipe", encoding: "utf8", timeout: 9 * 60 * 1000 });

  // 1. ¿Qué cambió respecto al último commit (incluye archivos nuevos)?
  let cambios = [];
  try {
    cambios = correr("git status --porcelain")
      .split("\n")
      .map((l) => l.slice(3).trim().replace(/^"|"$/g, ""))
      .filter(Boolean);
  } catch {
    process.exit(0); // sin git no se puede decidir; no bloquear
  }

  const tocaAndroid = cambios.some((f) => /\.(kt|kts)$/.test(f) || f.startsWith("app/") || f.startsWith("gradle/"));
  const tocaWeb = cambios.some((f) => f.startsWith("web/") && !f.startsWith("web/dist/"));
  if (!tocaAndroid && !tocaWeb) process.exit(0);

  // 2. Límite de intentos para no entrar en un ciclo infinito.
  const contador = path.join(os.tmpdir(), `karsy-stop-${datos.session_id || "sin-sesion"}.txt`);
  let intentos = 0;
  try { intentos = parseInt(fs.readFileSync(contador, "utf8"), 10) || 0; } catch {}
  if (intentos >= MAX_INTENTOS) {
    process.stderr.write(`Verificación: se alcanzó el máximo de ${MAX_INTENTOS} intentos; se deja terminar. Revisa a mano.\n`);
    process.exit(0);
  }

  const fallas = [];
  const ultimas = (txt, n = 40) => String(txt || "").trim().split("\n").slice(-n).join("\n");

  // 3. Android: pruebas JVM.
  if (tocaAndroid) {
    const gradle = esWindows ? "gradlew.bat" : "./gradlew";
    try {
      correr(`${gradle} testDebugUnitTest --console=plain`);
    } catch (e) {
      fallas.push(`== Pruebas de Android (testDebugUnitTest) FALLARON ==\n${ultimas((e.stdout || "") + (e.stderr || ""))}`);
    }
  }

  // 4. Web: chequeo de tipos.
  if (tocaWeb && fs.existsSync(path.join(raiz, "web", "node_modules"))) {
    try {
      correr("npm run typecheck", path.join(raiz, "web"));
    } catch (e) {
      fallas.push(`== Typecheck de web FALLÓ ==\n${ultimas((e.stdout || "") + (e.stderr || ""))}`);
    }
  }

  if (fallas.length === 0) {
    try { fs.unlinkSync(contador); } catch {}
    process.exit(0);
  }

  try { fs.writeFileSync(contador, String(intentos + 1)); } catch {}
  process.stderr.write(
    `No puedes terminar todavía: las verificaciones fallaron (intento ${intentos + 1} de ${MAX_INTENTOS}).\n` +
      `Arregla la causa sin desactivar ni borrar pruebas.\n\n${fallas.join("\n\n")}\n`
  );
  process.exit(2);
});
