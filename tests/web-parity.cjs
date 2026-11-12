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
  await page.locator('#chooseSystemKeyboard').click();
  assert(await page.locator('#systemKeyboardPicker').isVisible());
  assert.match(await page.locator('#systemKeyboardPicker').innerText(),/cannot inspect.*open Android system settings/s);
  assert.equal(await page.locator('#systemKeyboardPicker .demo-ime').count(),0);
  await page.locator('#profileClone').click();assert.match(await page.locator('#profilePanelStatus').innerText(),/Enter a profile name/);
  const profileRows=await value('JSON.stringify(layout())');
  await page.locator('#profileNewName').fill('Work');await page.locator('#profileClone').click();
  assert.equal(await value('profile'),'Work');assert.equal(await value('JSON.stringify(layout())'),profileRows);
  await page.locator('#profileNewName').fill('Default');await page.locator('#profileClone').click();
  assert.match(await page.locator('#profilePanelStatus').innerText(),/already exists/);
  await page.locator('#profilePackage').fill('com.termux');await page.locator('#profileAssign').click();
  assert.match(await page.locator('#profileBindings').innerText(),/com.termux → Work/);
  await page.locator('#profileAuto').check();
  const colors=await page.locator('#profileNewName').evaluate(el=>({text:getComputedStyle(el).color,hint:getComputedStyle(el,'::placeholder').color}));
  assert.equal(colors.text,'rgb(255, 255, 255)');assert.equal(colors.hint,'rgb(211, 216, 223)');
  const actions=await page.locator('.profile-actions button').evaluateAll(a=>a.map(x=>x.getBoundingClientRect().top));assert.equal(actions[0],actions[1]);
  await page.locator('#settingsProfile').selectOption('Default');
  // A real export can be imported again without losing any data on cancellation.
  const downloadPromise=page.waitForEvent('download');await page.locator('#profileExport').click();
  const download=await downloadPromise,exported=fs.readFileSync(await download.path());
  const exportedData=JSON.parse(exported);assert.equal(exportedData.activeProfile,'Default');
  assert.equal(exportedData.appBindings['com.termux'],'Work');assert.equal(exportedData.autoProfile,true);
  await value("layout()[3][0]='changed';profilePanel.bindings['com.termux']='Default';profilePanel.auto=false;persist();saveProfilePanel()");
  const changed=await value('JSON.stringify({profiles,profile,combosByProfile,profilePanel})');
  page.once('dialog',d=>d.dismiss());await page.locator('#file').setInputFiles({name:'export.json',mimeType:'application/json',buffer:exported});
  await page.waitForFunction(()=>document.querySelector('#profilePanelStatus').textContent.includes('cancelled'));
  assert.equal(await value('JSON.stringify({profiles,profile,combosByProfile,profilePanel})'),changed);
  page.once('dialog',d=>d.accept());await page.locator('#file').setInputFiles({name:'export.json',mimeType:'application/json',buffer:exported});
  await page.waitForFunction(()=>document.querySelector('#profilePanelStatus').textContent==='Profiles imported.');
  assert.equal(await value('JSON.stringify(layout())'),profileRows);assert.equal(await value("profilePanel.bindings['com.termux']"),'Work');assert.equal(await value('profilePanel.auto'),true);
  const androidRows=await value('clone(defaults.Terminal)');androidRows[3][0]='android';
  const android={version:2,profiles:{Android:{Terminal:androidRows,variants:{'en-US:pc':{Terminal:androidRows}}}},activeProfile:'Android',appBindings:{'com.example':'Android'},autoProfile:false};
  await page.locator('#file').setInputFiles({name:'android.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(android))});
  await page.waitForFunction(()=>profile==='Android');
  assert.equal(await value("profiles.Android.Terminal[3][0]"),'android');
  assert.equal(await value("profiles.Android.variants['en-US:pc'].Terminal[3][0]"),'android');
  assert(await value('Array.isArray(profiles.Android.Normal)&&Array.isArray(profiles.Android.Code)'));
  assert.equal(await value("profilePanel.bindings['com.example']"),'Android');assert.equal(await value('profilePanel.auto'),false);
  // Reserved object keys must be rejected atomically, before any profile or binding changes.
  for(const key of ['__proto__','constructor','prototype']){
   const before=await value('JSON.stringify({profiles,profile,combosByProfile,profilePanel})');
   const payload={profiles:{PollutionProbe:{Terminal:androidRows}},appBindings:JSON.parse(`{\"${key}\":\"PollutionProbe\"}`)};
   await page.locator('#file').setInputFiles({name:'unsafe.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(payload))});
   await page.waitForFunction(()=>document.querySelector('#profilePanelStatus').textContent.includes('Invalid app assignment'));
   assert.equal(await value('JSON.stringify({profiles,profile,combosByProfile,profilePanel})'),before);
   assert(await value('Object.getPrototypeOf(profilePanel.bindings)===Object.prototype'));
   assert.equal(await value("Object.hasOwn(profiles,'PollutionProbe')"),false);
   await page.locator('#profilePackage').fill(key);await page.locator('#profileAssign').click();
   assert.match(await page.locator('#profilePanelStatus').innerText(),/valid Android package/);
  }
  const bindingOnly={profiles:{NewProfile:{Terminal:androidRows}},appBindings:{'com.example':'NewProfile'}};
  const bindingFile={name:'binding.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(bindingOnly))};
  page.once('dialog',d=>d.dismiss());await page.locator('#file').setInputFiles(bindingFile);
  await page.waitForFunction(()=>document.querySelector('#profilePanelStatus').textContent.includes('cancelled'));
  assert.equal(await value("Object.hasOwn(profiles,'NewProfile')"),false);assert.equal(await value("profilePanel.bindings['com.example']"),'Android');
  page.once('dialog',d=>d.accept());await page.locator('#file').setInputFiles(bindingFile);
  await page.waitForFunction(()=>Object.hasOwn(profiles,'NewProfile'));
  assert.equal(await value("profilePanel.bindings['com.example']"),'NewProfile');assert.equal(await value('profilePanel.auto'),false);
  await page.locator('#settingsProfile').selectOption('Default');
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
  await page.locator('#comboAction').focus();await page.keyboard.press('Control');assert.equal(await page.locator('#comboAction').inputValue(),'Ctrl');
  await page.keyboard.press('Control+C');assert.equal(await page.locator('#comboAction').inputValue(),'Ctrl + C');
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
  await value("setDocument('abcdefghijklmnopqrstuvwxyz');shift=false;render()");
  const arrow=page.locator('#keys .key').getByText('←',{exact:true});
  await arrow.hover();await page.mouse.down();await page.waitForTimeout(550);
  const beforeMove=await value('window.getSelection().anchorOffset');
  await page.mouse.move(5,5);await page.waitForTimeout(300);
  const afterMove=await value('window.getSelection().anchorOffset');
  assert.equal(afterMove,beforeMove,'arrow repeat should stop when pointer leaves the key');
  await page.mouse.up();await page.waitForTimeout(150);
  assert.equal(await value('window.getSelection().anchorOffset'),afterMove);

  await page.locator('#settingsOpen').click();await page.locator('#pressAnimation').uncheck();
  assert(await page.locator('#keyboard').evaluate(el=>el.classList.contains('press-animation-off')));
  assert(!/granted/i.test(await page.locator('#micPermissionStatus').innerText()));
  // Check actual geometry on a narrow phone, including the live preview.
  await page.locator('#settingsClose').click();
  await page.setViewportSize({width:320,height:800});
  await page.locator('#fnBar').scrollIntoViewIfNeeded();
  const fkeys=await page.locator('#fnBar button').evaluateAll(nodes=>nodes.map(n=>{const r=n.getBoundingClientRect();return {text:n.textContent,left:r.left,right:r.right,top:r.top,bottom:r.bottom}}));
  const keyboardBox=await page.locator('#keyboard').boundingBox();
  assert.equal(fkeys.length,8);assert.equal(fkeys[7].text,'F8');
  assert(keyboardBox.x>=0 && keyboardBox.x+keyboardBox.width<=320, 'keyboard must fit the viewport');
  assert(fkeys[7].left>=0 && fkeys[7].right<=320, 'F8 must fit the viewport');
  assert(fkeys.every(f=>Math.abs(f.top-fkeys[0].top)<1 && f.left>=keyboardBox.x && f.right<=keyboardBox.x+keyboardBox.width && f.bottom>f.top));
  const space=page.locator('#keys .key.space');await space.scrollIntoViewIfNeeded();
  const markers=await space.locator('.space-inner span').evaluateAll(nodes=>nodes.map(n=>{const r=n.getBoundingClientRect(),style=getComputedStyle(n);return {text:n.textContent,left:r.left,right:r.right,top:r.top,width:r.width,visibility:style.visibility}}));
  const spaceBox=await space.boundingBox();
  assert.deepEqual(markers.map(m=>m.text),['<','Space','>']);
  assert(markers.every(m=>m.width>0 && m.visibility==='visible' && Math.abs(m.top-markers[0].top)<1 && m.left>=spaceBox.x && m.right<=spaceBox.x+spaceBox.width));
  // Scrolling a tall keyboard must leave toolbar and every Fn key visible.
  await value("pref.keyHeight=49;pref.keyGap=7;pref.numberOffset=14;applyTheme(pref.theme);render()");
  const assertStickyFn=async()=>{
   const visible=await page.locator('#keyboard').evaluate(root=>{
    const kb=root.getBoundingClientRect(),toolbar=document.querySelector('#fnOpen').getBoundingClientRect();
    const keys=[...document.querySelectorAll('#fnBar button')].map(el=>el.getBoundingClientRect());
    return {scroll:root.scrollTop,max:root.scrollHeight-root.clientHeight,kb:{top:kb.top,bottom:kb.bottom},toolbar:{top:toolbar.top,bottom:toolbar.bottom},keys:keys.map(r=>({top:r.top,bottom:r.bottom,left:r.left,right:r.right}))};
   });
   assert(visible.max>0&&visible.scroll>0,'keyboard must actually scroll');
   assert(visible.toolbar.top>=visible.kb.top&&visible.toolbar.bottom<=visible.kb.bottom,'toolbar should remain visible');
   assert(visible.keys.length===8&&visible.keys.every(r=>r.top>=visible.kb.top&&r.bottom<=visible.kb.bottom&&r.top>=0&&r.bottom<=800&&r.left>=0&&r.right<=320),'F1–F8 should remain visible while scrolling');
   const f8=page.locator('#fnBar button').last();await f8.click();assert.match(await page.locator('#status').innerText(),/F8/);
  };
  await page.locator('#keyboard').evaluate(el=>el.scrollTop=el.scrollHeight);await assertStickyFn();
  await page.locator('#keys .key').getByText('😀',{exact:true}).click();
  assert.equal(await value('emojiOpen'),true);assert.equal(await page.locator('#fnBar button').count(),8);
  await page.locator('#keyboard').evaluate(el=>el.scrollTop=el.scrollHeight);await assertStickyFn();
  await page.locator('#settingsOpen').click();
  const narrowActions=await page.locator('.profile-actions button').evaluateAll(a=>a.map(x=>{const r=x.getBoundingClientRect();return {top:r.top,left:r.left,right:r.right}}));
  assert.equal(narrowActions[0].top,narrowActions[1].top);assert(narrowActions.every(x=>x.left>=0&&x.right<=320));
  for(const state of ['granted','denied','prompt','unsupported']){
   const permissionPage=await browser.newPage();
   await permissionPage.addInitScript(state=>Object.defineProperty(navigator,'permissions',{value:state==='unsupported'?undefined:{query:async()=>({state,onchange:null})},configurable:true}),state);
   await permissionPage.goto(`http://127.0.0.1:${server.address().port}/simulator.html`);
   await permissionPage.locator('#settingsOpen').click();
   await permissionPage.waitForFunction(()=>document.querySelector('#micPermissionStatus').textContent!=='Checking…');
   assert.equal(await permissionPage.locator('#micPermissionStatus').evaluate(el=>el.classList.contains('granted')),state==='granted');
   if(state==='unsupported')assert.match(await permissionPage.locator('#micPermissionStatus').innerText(),/unavailable/i);
   await permissionPage.close();
  }
  await page.screenshot({path:process.env.WEB_SCREENSHOT||'/tmp/devboard026-web.png',fullPage:true});assert.deepEqual(errors,[]);
  console.log('Web parity tests passed: mapping, layouts, persistence, editing, Fn/combos, snippets, Space, repeat, animation, permissions.');
 }finally{if(browser)await browser.close();server.close()}
})().catch(error=>{console.error(error);process.exitCode=1});
