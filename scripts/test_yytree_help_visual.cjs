// Run after test_yytree_help.R; requires Playwright. An optional first argument
// selects a local Chromium/Edge executable instead of Playwright's runtime.
// Screenshots stay in temp/ and the inspection report stays in logs/.
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.argv[2] });
  const report = [];
  try {
    const page = await browser.newPage({ viewport: { width: 960, height: 900 } });
    // R includes optional math CDN resources; these pages use no mathematics.
    await page.route(/^https?:/, route => route.abort());
    for (const topic of ['layout', 'runtime', 'free_shapes']) {
      const file = path.resolve('temp', 'yytree-help', `${topic}.html`);
      await page.goto(pathToFileURL(file).href);
      await page.screenshot({ path: file.replace(/\.html$/, '.png'), fullPage: true });
      const metrics = await page.evaluate(() => ({
        title: document.title,
        headings: Array.from(document.querySelectorAll('h2,h3,h4')).map(node => node.textContent.trim()),
        viewportWidth: innerWidth,
        contentWidth: document.documentElement.scrollWidth,
        tableWidths: Array.from(document.querySelectorAll('table')).map(node => Math.ceil(node.getBoundingClientRect().width)),
        bodyFont: getComputedStyle(document.body).fontFamily
      }));
      report.push({ topic, ...metrics });
      if (metrics.contentWidth > metrics.viewportWidth)
        throw new Error(`${topic}: help page overflows horizontally`);
      if (!metrics.headings.includes('Arguments') || !metrics.headings.includes('Examples'))
        throw new Error(`${topic}: expected help sections are missing`);
    }
  } finally {
    fs.mkdirSync('logs', { recursive: true });
    fs.writeFileSync(path.join('logs', 'yytree-help-visual.json'), JSON.stringify(report, null, 2));
    await browser.close();
  }
  console.log('Visual layout checks passed for layout, runtime and free_shapes at 960px.');
})().catch(error => { console.error(error); process.exitCode = 1; });
