// PLAYWRIGHT_MODULE=/path/to/playwright node tests/web-parity.cjs
const assert=require('node:assert/strict'),fs=require('node:fs'),http=require('node:http'),path=require('node:path');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
(async()=>{
 const root=path.resolve(__dirname,'../docs');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://localhost').pathname);
  if(!file.startsWith(root+path.sep)||!fs.existsSync(file)){res.writeHead(404).end();return}
  res.setHeader('Content-Type',file.endsWith('.html')?'text/html':file.endsWith('.json')?'application/json':'application/octet-stream');res.end(fs.readFileSync(file));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH});const page=await browser.newPage({viewport:{width:1280,height:1100}});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{window.setDocument=text=>{
   const element=document.querySelector('#paper');element.textContent=text;element.focus();
   const range=document.createRange();range.selectNodeContents(element);range.collapse(false);
   window.getSelection().removeAllRanges();window.getSelection().addRange(range);lastCaret=range.cloneRange();
  }});
  await page.goto(`http://127.0.0.1:${server.address().port}/simulator.html`);
  await page.waitForFunction(()=>document.querySelectorAll('#keys .key').length>30);
  const value=expression=>page.evaluate(expression);
  assert.equal(await value('currentLayoutVariant()'),'pc');
  for(const [key,symbol] of Object.entries({'1':'!','0':')',',':'<','.':'>','/':'?','\\':'|','`':'~'})){
   await value("setDocument('x');shift=true;capsLock=false;symbolPage=false;render();renderLiveKeyboard()");
   assert.equal(await value(`displayedKey(${JSON.stringify(key)})`),symbol);await value(`press(${JSON.stringify(key)})`);
   assert.equal(await page.locator('#paper').innerText(),'x'+symbol);
  }
  await value("setDocument('x');shift=false;capsLock=true;press('1');press('ą')");
  assert.equal(await page.locator('#paper').innerText(),'x1Ą');
  await value("setDocument('x');shift=true;capsLock=false;symbolPage=true;press('1')");
  assert.equal(await page.locator('#paper').innerText(),'x1');
  await value("symbolPage=false;shift=false;capsLock=false;render()");
  await page.locator('#keys .key').getByText('⇧',{exact:true}).hover();await page.mouse.down();await page.waitForTimeout(600);await page.mouse.up();
  assert.equal(await value('capsLock'),true);assert.equal(await value('shift'),false);
  await value("setDocument('x');press('1')");assert.equal(await page.locator('#paper').innerText(),'x1');
  await value('capsLock=false;shift=false;render()');await page.locator('#settingsOpen').click();
  for(const lang of ['lt-LT','en-US'])for(const variant of ['pc','qwerty']){
   await page.locator('#voiceLang').selectOption(lang);await page.locator('#layoutVariant').selectOption(variant);
   assert.equal(await value('currentLayoutVariant()'),variant);
   if(lang==='lt-LT'&&variant==='qwerty'){
    const letters=await value("effectiveLayoutRows().slice(3,6).flat().join('')");
    for(const letter of 'abcdefghijklmnopqrstuvwxyząčęėįšųūž')assert(letters.includes(letter),letter);
   }
  }
  await page.locator('#voiceLang').selectOption('lt-LT');await page.locator('#layoutVariant').selectOption('qwerty');
  await page.locator('#settingsClose').click();await page.locator('#configure').click();
  const original=await value('layout()[3].slice(0,2)');
  await page.locator('#keys .keyrow').nth(3).locator('button').nth(0).click();
  await page.locator('#keys .keyrow').nth(3).locator('button').nth(1).click();
  assert.deepEqual(await value('layout()[3].slice(0,2)'),original.slice().reverse());
  await page.locator('#configure').click();await page.reload();
  assert.deepEqual(await value('layout()[3].slice(0,2)'),original.slice().reverse());
  await page.locator('#configure').click();
  await page.locator('#keys .keyrow').nth(3).locator('button').first().hover();await page.mouse.down();await page.waitForTimeout(600);await page.mouse.up();
  const before=await value('layout()[3].length');await page.locator('#layoutDelete').click();
  assert.equal(await value('layout()[3].length'),before-1);
  page.once('dialog',d=>d.dismiss());await page.locator('#resetLayout').click();assert.equal(await value('layout()[3].length'),before-1);
  page.once('dialog',d=>d.accept());await page.locator('#resetLayout').click();assert.deepEqual(await value('layout()[3].slice(0,2)'),original);
  await page.locator('#configure').click();await page.locator('#fnOpen').click();assert.equal(await page.locator('#fnBar button').count(),8);
  await page.locator('#comboOpen').click();await page.locator('#comboName').fill('Copy');
  await page.locator('#comboCaptureKeys button').getByText('Ctrl',{exact:true}).click();await page.locator('#comboCaptureKeys button').getByText('C',{exact:true}).click();
  await page.locator('#comboSave').click();assert.equal(await page.locator('#comboBar button').innerText(),'Copy');
  assert.equal(await page.locator('#liveKeyboard .preview-combo').innerText(),'Copy');
  await page.locator('#comboOpen').click();await page.locator('#comboList input[type=checkbox]').uncheck();assert.equal(await page.locator('#comboBar button').count(),0);
  await page.locator('#comboClose').click();await page.locator('#snippetOpen').click();await page.locator('#snippetAdd').click();
  await page.locator('#snippetText').fill('echo first\necho second');await page.locator('#snippetSave').click();
  await value("setDocument('')");await page.locator('#clipItems .cliptext').click();
  assert.equal(await page.locator('#paper').innerText(),'echo first\necho second');
  for(const fraction of [0.1,0.5,0.9]){
   await value("setDocument('x');shift=false;capsLock=false;render()");const space=page.locator('#keys .key.space'),box=await space.boundingBox();
   await space.click({position:{x:box.width*fraction,y:box.height/2}});assert.equal(await page.locator('#paper').textContent(),'x ');
  }
  await value("setDocument('abcdefghijklmnopqrst');shift=false;render()");
  await page.locator('#keys .key').getByText('←',{exact:true}).hover();await page.mouse.down();await page.waitForTimeout(650);await page.mouse.up();
  const position=await value('window.getSelection().anchorOffset');assert(position<19&&position>0,`repeat position ${position}`);
  await page.waitForTimeout(180);assert.equal(await value('window.getSelection().anchorOffset'),position);
  await page.locator('#settingsOpen').click();await page.locator('#pressAnimation').uncheck();
  assert(await page.locator('#keyboard').evaluate(el=>el.classList.contains('press-animation-off')));
  assert(!/granted/i.test(await page.locator('#micPermissionStatus').innerText()));
  await page.screenshot({path:process.env.WEB_SCREENSHOT||'/tmp/devboard026-web.png',fullPage:true});assert.deepEqual(errors,[]);
  console.log('Web parity tests passed: mapping, layouts, persistence, editing, Fn/combos, snippets, Space, repeat, animation, permissions.');
 }finally{if(browser)await browser.close();server.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
