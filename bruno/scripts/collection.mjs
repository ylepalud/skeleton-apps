// Keeps the Bruno collection in step with the API contract.
//   node scripts/collection.mjs check <contract>   fails when the committed requests differ
//   node scripts/collection.mjs write <contract>   rewrites them
// Only the generated request folders (one per tag, marked by a folder.yml) are compared or
// rewritten: opencollection.yml and the environments are written by hand.
import $RefParser from '@apidevtools/json-schema-ref-parser';
import { execFileSync } from 'node:child_process';
import { cpSync, existsSync, mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const [mode, contract] = process.argv.slice(2);
if (!['check', 'write'].includes(mode) || !contract) {
  console.error('Usage: collection.mjs <check|write> <contract>');
  process.exit(2);
}

const collectionDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const work = mkdtempSync(join(tmpdir(), 'bruno-collection-'));

try {
  // Bruno's importer does not follow references to other files: bundle the contract first.
  const bundled = join(work, 'openapi.json');
  writeFileSync(bundled, JSON.stringify(await $RefParser.bundle(contract), null, 2));
  const generatedDir = join(work, 'collection');
  execFileSync(
    join(collectionDir, 'node_modules', '.bin', 'bru'),
    ['import', 'openapi', '--source', bundled, '--output', generatedDir, '--collection-name', 'api'],
    { stdio: 'pipe' },
  );

  const generated = requestFolders(generatedDir);
  const committed = requestFolders(collectionDir);

  if (mode === 'write') {
    for (const folder of committed) {
      rmSync(join(collectionDir, folder), { recursive: true });
    }
    for (const folder of generated) {
      cpSync(join(generatedDir, folder), join(collectionDir, folder), { recursive: true });
    }
    console.log(`Collection updated: ${generated.join(', ')}`);
  } else {
    const differences = compare(generatedDir, generated, committed);
    if (differences.length > 0) {
      console.error('The Bruno collection does not match the API contract:');
      differences.forEach((difference) => console.error(`  - ${difference}`));
      console.error('Run ./gradlew :bruno:updateCollection and commit the result.');
      process.exit(1);
    }
    console.log('The Bruno collection matches the API contract.');
  }
} finally {
  rmSync(work, { recursive: true, force: true });
}

function requestFolders(dir) {
  return readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && entry.name !== 'node_modules')
    .filter((entry) => existsSync(join(dir, entry.name, 'folder.yml')))
    .map((entry) => entry.name)
    .sort();
}

function filesUnder(dir) {
  return readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => relative(dir, join(entry.parentPath, entry.name)))
    .sort();
}

function compare(generatedDir, generated, committed) {
  const differences = [];
  for (const folder of committed.filter((name) => !generated.includes(name))) {
    differences.push(`${folder}/ is no longer in the contract`);
  }
  for (const folder of generated) {
    if (!committed.includes(folder)) {
      differences.push(`${folder}/ is missing`);
      continue;
    }
    const expected = filesUnder(join(generatedDir, folder));
    const actual = filesUnder(join(collectionDir, folder));
    for (const file of expected) {
      if (!actual.includes(file)) {
        differences.push(`${folder}/${file} is missing`);
      } else if (readFileSync(join(generatedDir, folder, file), 'utf8') !== readFileSync(join(collectionDir, folder, file), 'utf8')) {
        differences.push(`${folder}/${file} differs`);
      }
    }
    for (const file of actual.filter((name) => !expected.includes(name))) {
      differences.push(`${folder}/${file} is no longer in the contract`);
    }
  }
  return differences;
}
