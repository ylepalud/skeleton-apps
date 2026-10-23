// The questions the installer asks, their validation and their defaults.

const JAVA_KEYWORDS = new Set(
  ('abstract assert boolean break byte case catch char class const continue default do double else ' +
    'enum extends final finally float for goto if implements import instanceof int interface long ' +
    'native new package private protected public return short static strictfp super switch ' +
    'synchronized this throw throws transient try void volatile while true false null var record yield')
    .split(' '),
);

/**
 * @param {string} value
 * @returns {string | undefined} why the project name is invalid, if it is
 */
export function validateName(value) {
  if (!/^[a-z][a-z0-9]*(-[a-z0-9]+)*$/.test(value)) {
    return 'Use kebab-case: lower-case letters and digits, separated by single dashes, starting with a letter.';
  }
  if (value.length > 50) {
    return 'Keep it under 50 characters.';
  }
  return undefined;
}

/**
 * @param {string} value
 * @returns {string | undefined} why the package is invalid, if it is
 */
export function validatePackage(value) {
  const parts = value.split('.');
  if (parts.length < 2 || !parts.every((part) => /^[a-z][a-z0-9_]*$/.test(part))) {
    return 'Use at least two lower-case segments separated by dots, e.g. dev.example.demo.';
  }
  const keyword = parts.find((part) => JAVA_KEYWORDS.has(part));
  if (keyword) {
    return `"${keyword}" is a Java keyword.`;
  }
  return undefined;
}

/**
 * @param {string} value
 * @returns {string | undefined} why the client id is invalid, if it is
 */
export function validateClientId(value) {
  return /^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(value)
    ? undefined
    : 'Use letters, digits, dots, dashes and underscores.';
}

/**
 * @param {string} value
 * @returns {string | undefined} why the GitHub owner is invalid, if it is
 */
export function validateOwner(value) {
  // Lower case only: it becomes part of the container image names, which must be lower case.
  return /^[a-z0-9](?:[a-z0-9]|-(?=[a-z0-9])){0,38}$/.test(value)
    ? undefined
    : 'Use the lower-case GitHub user or organisation name.';
}

/**
 * The default base package: dev.<owner>.<name without dashes>.
 * @param {string} owner
 * @param {string} name
 */
export function defaultPackage(owner, name) {
  /** @param {string} value */
  const segment = (value) => {
    const cleaned = value.toLowerCase().replace(/[^a-z0-9]/g, '');
    const safe = /^[a-z]/.test(cleaned) ? cleaned : `x${cleaned}`;
    return JAVA_KEYWORDS.has(safe) ? `${safe}_` : safe;
  };
  return `dev.${segment(owner)}.${segment(name)}`;
}

/**
 * The default OIDC client id of the web app.
 * @param {string} name
 */
export function defaultClientId(name) {
  return `${name}-web`;
}

/**
 * The default GitHub owner, from the git configuration.
 * @param {string | undefined} githubUser `git config github.user`
 * @param {string | undefined} userName `git config user.name`
 */
export function defaultOwner(githubUser, userName) {
  const candidate = (githubUser || userName || '').trim().toLowerCase().replace(/[^a-z0-9-]/g, '');
  return validateOwner(candidate) === undefined ? candidate : '';
}
