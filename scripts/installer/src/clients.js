// The clients a generated project keeps: web, cli and mobile (Android and iOS). The API is always
// there. A client's own folders are left out whole; its passages in shared files are marked
// blocks of the template, stripped here.

/** @typedef {'web' | 'cli' | 'mobile'} Client */

/** @type {readonly Client[]} */
export const CLIENTS = ['web', 'cli', 'mobile'];

/**
 * Whether a marker name holds for the selection: a client, `native` (the shared module, kept with
 * the CLI or mobile), or either one negated with `!`.
 * @param {string} name
 * @param {ReadonlySet<Client>} clients
 * @returns {boolean}
 */
export function holds(name, clients) {
  if (name.startsWith('!')) {
    return !holds(name.slice(1), clients);
  }
  if (name === 'native') {
    return clients.has('cli') || clients.has('mobile');
  }
  if (CLIENTS.includes(/** @type {Client} */ (name))) {
    return clients.has(/** @type {Client} */ (name));
  }
  throw new Error(`Unknown client marker "${name}"`);
}

/**
 * The selection from a comma-separated list, such as `web,cli`.
 * @param {string} value
 * @returns {Set<Client>}
 */
export function parseClients(value) {
  return new Set(
    /** @type {Client[]} */ (
      value
        .split(',')
        .map((part) => part.trim())
        .filter(Boolean)
    ),
  );
}

/**
 * @param {ReadonlySet<string>} clients
 * @returns {string | undefined} why the selection is invalid, if it is
 */
export function validateClients(clients) {
  const unknown = [...clients].filter((client) => !CLIENTS.includes(/** @type {Client} */ (client)));
  if (unknown.length > 0) {
    return `Unknown client: ${unknown.join(', ')}. Choose among ${CLIENTS.join(', ')}.`;
  }
  if (clients.size === 0) {
    return 'Choose at least one client.';
  }
  return undefined;
}

/** The folders and files of each marker name, left out when it does not hold. */
const CLIENT_PATHS = {
  web: [
    /^apps\/services\/api-app\//,
    /^build-logic\/src\/main\/kotlin\/frontend-convention\.gradle\.kts$/,
    /^build-logic\/src\/functionalTest\/kotlin\/dev\/ylp\/buildlogic\/FrontendConventionTest\.kt$/,
    /^docs\/product\/use-the-web-app\.md$/,
  ],
  mobile: [
    /^apps\/clients\/(android|ios)\//,
    /^apps\/clients\/shared\/src\/(androidMain|androidHostTest|iosMain)\//,
    /^\.github\/workflows\/(android|ios)\.yml$/,
    /^build-logic\/src\/main\/kotlin\/(android-app-convention|xcode-app)\.gradle\.kts$/,
    /^build-logic\/src\/functionalTest\/kotlin\/dev\/ylp\/buildlogic\/(AndroidAppConventionTest|XcodeAppTest)\.kt$/,
    /^docs\/(contributor\/how-to\/run|product\/use)-the-(android|ios)-app\.md$/,
    /^docs\/architecture\/decisions\/00(25|27)-/,
  ],
  cli: [
    /^apps\/clients\/cli\//,
    /^\.github\/workflows\/cli\.yml$/,
    /^build-logic\/src\/main\/kotlin\/kotlin-native-cli-convention\.gradle\.kts$/,
    /^build-logic\/src\/functionalTest\/kotlin\/dev\/ylp\/buildlogic\/KotlinNativeCliConventionTest\.kt$/,
    /^docs\/(contributor\/how-to\/run|product\/use)-the-cli\.md$/,
    /^docs\/architecture\/decisions\/0030-/,
  ],
  native: [
    /^apps\/clients\/shared\//,
    /^build-logic\/src\/main\/kotlin\/(kotlin-multiplatform-convention|openapi-kotlin-client)\.gradle\.kts$/,
    /^build-logic\/src\/main\/kotlin\/dev\/ylp\/buildlogic\/Sqlite\.kt$/,
    /^build-logic\/src\/functionalTest\/kotlin\/dev\/ylp\/buildlogic\/(KotlinMultiplatformConventionTest|OpenApiKotlinClientTest)\.kt$/,
    /^docs\/architecture\/clients\.md$/,
    /^docs\/architecture\/decisions\/00(24|29)-/,
  ],
};

/**
 * Whether a template path belongs to a client the project does not keep.
 * @param {string} path relative path with forward slashes
 * @param {ReadonlySet<Client>} clients
 */
export function isLeftOut(path, clients) {
  return Object.entries(CLIENT_PATHS).some(
    ([name, patterns]) => !holds(name, clients) && patterns.some((pattern) => pattern.test(path)),
  );
}

/**
 * A marker line, in any comment style: `# client:web:start`, `// client:!web:end`,
 * `<!-- client:cli:start -->`, or `%% client:web:start` in a Mermaid diagram.
 */
const MARKER = /^[ \t]*(?:#|\/\/|<!--|%%)[ \t]*client:(!?[a-z]+):(start|end)[ \t]*(?:-->)?[ \t]*$/;

/**
 * A marker at the end of a line, for that line alone: a table row (inside its last cell, before
 * the closing pipe), a list item or one sentence of a paragraph written a sentence per line,
 * where a marker line of its own would break the Markdown.
 */
const LINE_MARKER = /[ \t]*(?:<!--[ \t]*client:(!?[a-z]+)[ \t]*-->([ \t]*\|)?|(?:#|\/\/)[ \t]*client:(!?[a-z]+))[ \t]*$/;

/**
 * The text without the blocks and the marked lines of the markers that do not hold for the
 * selection, and without any marker. Blocks may nest.
 * @param {string} text
 * @param {ReadonlySet<Client>} clients
 * @param {string} [path] for error messages
 * @returns {string}
 */
export function stripClientBlocks(text, clients, path = 'text') {
  if (!text.includes('client:')) {
    return text;
  }
  const lines = text.split('\n');
  /** @type {{ name: string, line: number }[]} */
  const open = [];
  /** @type {string[]} */
  const kept = [];
  let skipping = 0;
  lines.forEach((line, index) => {
    const marker = MARKER.exec(line);
    if (!marker) {
      const lineMarker = LINE_MARKER.exec(line);
      if (skipping > 0) return;
      if (!lineMarker) {
        kept.push(line);
      } else if (holds(lineMarker[1] ?? lineMarker[3], clients)) {
        kept.push(line.slice(0, lineMarker.index) + (lineMarker[2] ? ' |' : ''));
      }
      return;
    }
    const [, name, edge] = marker;
    holds(name, clients);
    if (edge === 'start') {
      open.push({ name, line: index + 1 });
      if (skipping > 0 || !holds(name, clients)) skipping++;
      return;
    }
    const last = open.pop();
    if (!last || last.name !== name) {
      throw new Error(`${path}:${index + 1}: client:${name}:end without its start`);
    }
    if (skipping > 0) skipping--;
  });
  if (open.length > 0) {
    const last = open[open.length - 1];
    throw new Error(`${path}:${last.line}: client:${last.name}:start without its end`);
  }
  return kept.join('\n');
}
