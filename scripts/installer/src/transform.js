// Turns the template's file tree into a new project: exclusions, then renames.
// Pure functions over paths and contents, so every rule is unit-tested.

/** The template's own identity, replaced in every generated project. */
export const TEMPLATE = {
  owner: 'ylepalud',
  name: 'skeleton-apps',
  packageName: 'dev.ylp',
  clientId: 'api-app',
};

/**
 * @typedef {object} Answers
 * @property {string} name        project name, kebab-case
 * @property {string} packageName base Java package, e.g. dev.example.demo
 * @property {string} clientId    OIDC client id of the web app
 * @property {string} owner       GitHub owner of the new repository
 */

/** Paths that belong to the template, never to a generated project. */
const EXCLUDED = [
  /^\.git\//,
  /^scripts\/installer\//,
  /^package\.json$/,
  /^package-lock\.json$/,
  /^node_modules\//,
  /^\.github\/workflows\/installer\.yml$/,
  /^CHANGELOG\.md$/,
  /^docs\/template\//,
];

/** Where a generated project records the template it came from, in place of the template's own guide. */
export const PROVENANCE_PATH = 'docs/template/README.md';

/**
 * The page that replaces the template section in a generated project.
 * @param {string} ref the template version the project was generated from
 */
export function provenancePage(ref) {
  const repository = `https://github.com/${TEMPLATE.owner}/${TEMPLATE.name}`;
  return `# Template

This project was generated from [${TEMPLATE.name}](${repository}) at \`${ref}\`.

The template's [releases](${repository}/releases) list what changed since; port what you want by hand.
`;
}

/**
 * Whether a template path stays out of the generated project.
 * @param {string} path relative path with forward slashes
 */
export function isExcluded(path) {
  return EXCLUDED.some((pattern) => pattern.test(path));
}

/**
 * Whether content is binary, in which case it is copied untouched.
 * @param {Uint8Array} content
 */
export function isBinary(content) {
  return content.subarray(0, 8000).includes(0);
}

/**
 * Text that only makes sense in the template, kept out of generated projects:
 * a marked block (Markdown comments `template-only:start` / `template-only:end`),
 * and references to the template's own issues, like " (issue #12)".
 */
const TEMPLATE_ONLY = [/<!-- template-only:start -->[\s\S]*?<!-- template-only:end -->\n?/g, / \(issue #\d+\)|, issue #\d+(?=\))/g];

/** @param {string} value */
function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * The path of a file in the generated project: the package folders follow the new package.
 * @param {string} path
 * @param {Answers} answers
 */
export function transformPath(path, answers) {
  const from = TEMPLATE.packageName.replaceAll('.', '/');
  const to = answers.packageName.replaceAll('.', '/');
  return path.replace(new RegExp(`(^|/)${escapeRegExp(from)}(?=/)`), `$1${to}`);
}

/**
 * The content of a text file in the generated project.
 * @param {string} path the template path
 * @param {string} text
 * @param {Answers} answers
 */
export function transformText(path, text, answers) {
  if (path === '.release-please-manifest.json') {
    return '{\n  ".": "0.0.0"\n}\n';
  }
  let result = text;
  if (path.endsWith('.md')) {
    result = TEMPLATE_ONLY.reduce((current, pattern) => current.replace(pattern, ''), result);
  }
  result = result
    .replaceAll(`${TEMPLATE.owner}/${TEMPLATE.name}`, `${answers.owner}/${answers.name}`)
    .replaceAll(TEMPLATE.name, answers.name)
    .replace(new RegExp(`\\b${escapeRegExp(TEMPLATE.packageName)}\\b`, 'g'), answers.packageName)
    .replace(
      new RegExp(`\\b${escapeRegExp(TEMPLATE.packageName.replaceAll('.', '/'))}\\b`, 'g'),
      answers.packageName.replaceAll('.', '/'),
    );
  // `api-app` also names the web module: only the client id settings change.
  if (path === 'services/api-app/public/config.json') {
    result = result.replace(`"clientId": "${TEMPLATE.clientId}"`, `"clientId": "${answers.clientId}"`);
  }
  if (path === 'compose.yaml') {
    result = result.replace(`OIDC_CLIENT_ID: ${TEMPLATE.clientId}`, `OIDC_CLIENT_ID: ${answers.clientId}`);
  }
  return result;
}

/**
 * The generated project's files from the template's. File modes are kept as they are.
 * @template {{ content: Uint8Array }} F
 * @param {Map<string, F>} files template paths to files
 * @param {Answers} answers
 * @returns {Map<string, F>}
 */
export function transformTree(files, answers) {
  const decoder = new TextDecoder('utf-8', { fatal: true });
  const encoder = new TextEncoder();
  /** @type {Map<string, F>} */
  const result = new Map();
  for (const [path, file] of files) {
    if (isExcluded(path)) {
      continue;
    }
    const target = transformPath(path, answers);
    if (isBinary(file.content)) {
      result.set(target, file);
      continue;
    }
    const text = transformText(path, decoder.decode(file.content), answers);
    result.set(target, { ...file, content: encoder.encode(text) });
  }
  return result;
}
