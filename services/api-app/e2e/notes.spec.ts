import { expect, Page, test } from '@playwright/test';

/** Signs in through the identity provider's login form; the username becomes the subject. */
async function signIn(page: Page, username: string): Promise<void> {
  await page.goto('/');
  await page.locator('input[name="username"]').fill(username);
  await page.getByRole('button', { name: 'Sign-in' }).click();
  await expect(page.getByRole('heading', { name: /your notes/i })).toBeVisible();
  await expect(page.getByText(username, { exact: true })).toBeVisible();
}

test('a user signs in, then writes, edits and deletes a note', async ({ page }) => {
  const username = `e2e-${Date.now()}`;
  await signIn(page, username);
  await expect(page.getByText('No notes yet.')).toBeVisible();

  await page.getByLabel('Title').fill('Groceries');
  await page.getByLabel('Content').fill('Milk, eggs');
  await page.getByRole('button', { name: 'Add note' }).click();
  await expect(page.getByRole('heading', { name: 'Groceries' })).toBeVisible();

  await page.getByRole('button', { name: 'Edit' }).click();
  await page.getByLabel('Title').fill('Shopping');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByRole('heading', { name: 'Shopping' })).toBeVisible();

  await page.getByRole('button', { name: 'Delete' }).click();
  await page.getByRole('button', { name: 'Delete' }).click();
  await expect(page.getByText('No notes yet.')).toBeVisible();
});

test('a user never sees the notes of another user', async ({ browser }) => {
  const alice = await browser.newPage();
  await signIn(alice, `alice-${Date.now()}`);
  await alice.getByLabel('Title').fill('Private');
  await alice.getByRole('button', { name: 'Add note' }).click();
  await expect(alice.getByRole('heading', { name: 'Private' })).toBeVisible();

  const bob = await (await browser.newContext()).newPage();
  await signIn(bob, `bob-${Date.now()}`);
  await expect(bob.getByText('No notes yet.')).toBeVisible();
});
