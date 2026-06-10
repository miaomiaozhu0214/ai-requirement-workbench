import { readdirSync } from 'node:fs';
import { join, relative } from 'node:path';

const srcDir = new URL('../src', import.meta.url).pathname;
const offenders = [];

function walk(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const fullPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath);
      continue;
    }
    if (entry.isFile() && (entry.name.endsWith('.js') || entry.name.endsWith('.js.map'))) {
      offenders.push(relative(new URL('..', import.meta.url).pathname, fullPath));
    }
  }
}

walk(srcDir);

if (offenders.length > 0) {
  console.error('frontend/src contains generated JavaScript artifacts. Remove these files before building:');
  for (const file of offenders) {
    console.error(`- ${file}`);
  }
  console.error('These files can make Vite load stale .js modules instead of TypeScript/Vue source files.');
  process.exit(1);
}

console.log('frontend/src clean: no .js or .js.map artifacts found.');
