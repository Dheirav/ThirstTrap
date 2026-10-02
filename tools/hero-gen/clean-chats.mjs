// Delete old image-generation chats from ChatGPT, through the same signed-in
// browser gen.mjs drives.
//
//   node clean-chats.mjs                      list every chat, do nothing
//   node clean-chats.mjs --match "axolotl"    list only the ones that match
//   node clean-chats.mjs --match "..." --delete   actually delete those
//   node clean-chats.mjs --match "..." --delete --yes   skip the countdown
//
// Listing is the default ON PURPOSE. Deleting a chat on chatgpt.com cannot be
// undone from here, so nothing is removed until you have seen the exact list
// that will go and asked for it by name. There is deliberately no "--all".
//
// Needs ./browser.sh running, and NOT a gen.mjs run in flight: both drive the
// same browser and will fight over the page.
import { chromium } from "playwright";

const PORT = process.env.AXL_CDP_PORT || 9222;
const arg = (n, d = null) => {
  const i = process.argv.indexOf(n);
  return i >= 0 && process.argv[i + 1] && !process.argv[i + 1].startsWith("--") ? process.argv[i + 1] : d;
};
const has = (n) => process.argv.includes(n);

const match = arg("--match");
const doDelete = has("--delete");
if (doDelete && !match) {
  console.error("Refusing to delete without --match. Name what should go.");
  process.exit(2);
}
const re = match ? new RegExp(match, "i") : null;

const browser = await chromium.connectOverCDP(`http://127.0.0.1:${PORT}`);
const ctx = browser.contexts()[0];
const page = ctx.pages().find((p) => p.url().includes("chatgpt.com")) || (await ctx.newPage());
if (!page.url().includes("chatgpt.com")) await page.goto("https://chatgpt.com/");
await page.waitForTimeout(1500);

// The sidebar lazy-loads, so scroll it to the bottom until the count stops growing.
async function allChats() {
  let last = -1;
  for (let i = 0; i < 40; i++) {
    const n = await page.locator("nav a[href^='/c/']").count();
    if (n === last) break;
    last = n;
    await page.locator("nav a[href^='/c/']").last().scrollIntoViewIfNeeded().catch(() => {});
    await page.waitForTimeout(450);
  }
  return page.locator("nav a[href^='/c/']").evaluateAll((as) =>
    as.map((a) => ({ href: a.getAttribute("href"), title: (a.innerText || "").trim().split("\n")[0] })));
}

const chats = await allChats();
const hits = re ? chats.filter((c) => re.test(c.title)) : chats;

console.log(`${chats.length} chats in the sidebar, ${hits.length} ${re ? "matching /" + match + "/i" : "total"}\n`);
hits.forEach((c, i) => console.log(`  ${String(i + 1).padStart(3)}. ${c.title}`));

if (!doDelete) {
  console.log(`\nNothing deleted. Re-run with --delete to remove the ${hits.length} above.`);
  await browser.close();
  process.exit(0);
}
if (!hits.length) { await browser.close(); process.exit(0); }

if (!has("--yes")) {
  console.log(`\nDeleting these ${hits.length} chats in 8 seconds. Ctrl-C to stop.`);
  await new Promise((r) => setTimeout(r, 8000));
}

let gone = 0, failed = 0;
for (const c of hits) {
  try {
    const row = page.locator(`nav a[href='${c.href}']`).first();
    await row.scrollIntoViewIfNeeded();
    await row.hover();
    // the row's own kebab, then Delete in the menu that opens
    await row.locator("xpath=..").locator("button[aria-label*='ptions'], button[data-testid*='options']").first().click({ timeout: 4000 });
    await page.waitForTimeout(300);
    await page.getByRole("menuitem", { name: /delete/i }).first().click({ timeout: 4000 });
    await page.waitForTimeout(300);
    await page.getByRole("button", { name: /^delete$/i }).first().click({ timeout: 4000 });
    await page.waitForTimeout(700);
    gone++;
    console.log(`  deleted  ${c.title}`);
  } catch (e) {
    failed++;
    console.log(`  SKIPPED  ${c.title}  (${String(e).split("\n")[0].slice(0, 70)})`);
  }
}
console.log(`\n${gone} deleted, ${failed} skipped.`);
if (failed) console.log("Skipped ones usually mean ChatGPT moved the menu; delete those by hand.");
await browser.close();
