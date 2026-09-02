import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import yaml from 'js-yaml';

const COPY_ROOT = new URL('../../../assets/screenshotcopy/', import.meta.url).pathname;

export function copyRoot() {
  return COPY_ROOT;
}

export function availableLocales() {
  return readdirSync(COPY_ROOT, { withFileTypes: true })
    .filter((d) => d.isDirectory())
    .map((d) => d.name)
    .sort();
}

export function loadCopy(locale) {
  const file = join(COPY_ROOT, locale, 'screenshot_copy.yml');
  if (!existsSync(file)) throw new Error(`No copy for locale "${locale}" (${file})`);
  const raw = yaml.load(readFileSync(file, 'utf8')) ?? {};
  const base =
    locale === 'base'
      ? raw
      : yaml.load(readFileSync(join(COPY_ROOT, 'base', 'screenshot_copy.yml'), 'utf8'));

  const blank = (v) => v === undefined || String(v).trim() === '';
  const missing = Object.keys(base).filter((k) => raw[k] === undefined);
  const untranslated = Object.keys(base).filter((k) => raw[k] !== undefined && blank(raw[k]));
  return {
    get(key, field) {
      const k = `screenshot_copy_${key}_${field}`;
      const v = raw[k];
      if (v === undefined) throw new Error(`Missing copy key "${k}" for locale "${locale}"`);
      return String(v);
    },
    has(key, field) {
      return raw[`screenshot_copy_${key}_${field}`] !== undefined;
    },
    missing,
    untranslated,
    complete: missing.length === 0 && untranslated.length === 0,
    translated: Object.keys(base).length - untranslated.length,
  };
}
