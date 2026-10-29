import { readFileSync, writeFileSync, mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

const html = readFileSync("index.html", "utf8");
const scripts = [...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/gi)]
  .map(match => match[1].trim())
  .filter(Boolean);
if (!scripts.length) {
  console.error("No inline JavaScript blocks found in index.html");
  process.exit(1);
}
const directory = mkdtempSync(join(tmpdir(), "atc-live-check-"));
try {
  for (let index = 0; index < scripts.length; index++) {
    const file = join(directory, `inline-${index}.js`);
    writeFileSync(file, scripts[index]);
    const result = spawnSync(process.execPath, ["--check", file], { encoding: "utf8" });
    if (result.status !== 0) {
      console.error(`JavaScript syntax error in inline script ${index + 1}:\n${result.stderr || result.stdout}`);
      process.exitCode = 1;
      break;
    }
  }
  if (!process.exitCode) console.log(`Checked ${scripts.length} inline JavaScript block(s) successfully.`);
} finally {
  rmSync(directory, { recursive: true, force: true });
}
