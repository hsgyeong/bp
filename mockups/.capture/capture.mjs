import puppeteer from "puppeteer";
import { fileURLToPath, pathToFileURL } from "url";
import { dirname, resolve } from "path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const indexPath = resolve(__dirname, "..", "index.html");
const outDir = resolve(__dirname, "..", "png");
const baseUrl = pathToFileURL(indexPath).href;

const screens = [
  "00-login",
  "01-home",
  "02-ledger-list",
  "03-stats-month-amount",
  "04-signup",
  "05-ledger-calendar",
  "06-stats-month-category",
  "07-stats-week-amount",
  "08-more-settings",
  "09-add-transaction",
  "10-category-settings",
  "11-budget-settings",
  "12-profile",
  "13-withdraw-sheet",
  "14-change-password",
];

const browser = await puppeteer.launch({ headless: "new" });
const page = await browser.newPage();
await page.setViewport({ width: 440, height: 880, deviceScaleFactor: 3 });

for (let i = 0; i < screens.length; i++) {
  await page.goto(`${baseUrl}?only=${i}`, { waitUntil: "networkidle0" });
  await page.evaluate(() => document.fonts.ready);
  const handle = await page.evaluateHandle(() =>
    [...document.querySelectorAll(".screen-wrap")]
      .find((w) => w.style.display !== "none")
      .querySelector(".phone")
  );
  const el = handle.asElement();
  const file = resolve(outDir, `${screens[i]}.png`);
  await el.screenshot({ path: file });
  console.log(`✓ ${screens[i]}.png`);
}

await browser.close();
console.log(`\n완료: ${screens.length}장 → mockups/png/`);
