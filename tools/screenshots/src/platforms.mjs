import { readdirSync } from 'node:fs';

const DIR = new URL('./config/', import.meta.url);

/**
 * Every `src/config/<id>.mjs` is a platform. Adding one needs no edits
 * anywhere else — the CLI, the preview and the verifier all discover them.
 */
export async function loadPlatforms() {
  const files = readdirSync(DIR.pathname)
    .filter((f) => f.endsWith('.mjs'))
    .sort();
  const out = {};
  for (const file of files) {
    const mod = await import(new URL(file, DIR).href);
    if (!mod.platform?.id) throw new Error(`${file} must export a \`platform\` with an \`id\``);
    out[mod.platform.id] = mod.platform;
  }
  return out;
}
