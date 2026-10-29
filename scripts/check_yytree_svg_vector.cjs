// Requires Playwright; optional argument selects a Chromium/Edge executable.
const { chromium } = require('playwright');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
(async () => {
  const browser = await chromium.launch({headless:true, executablePath:process.argv[2]});
  try {
    const page = await browser.newPage({viewport:{width:1600,height:1200},deviceScaleFactor:1});
    for(let i=1;i<=10;i++) {
      const file=path.resolve('temp/yytree-pdf-vector',`${i}.svg`);
      await page.goto(pathToFileURL(file).href);
      await page.evaluate(()=>document.fonts.ready);
      const valid=await page.evaluate(()=>!!document.querySelector('svg') &&
        !document.querySelector('parsererror,image,foreignObject') &&
        document.querySelectorAll('path,rect,polygon').length>10 &&
        document.documentElement.textContent.includes('test 中文'));
      if(!valid) throw new Error(`Invalid vector SVG: ${i}`);
      await page.screenshot({path:file.replace('.svg','-svg.png')});
    }
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
