#!/usr/bin/env node
/* ============================================================================
   MAYA — SETTINGS UI FUNCTIONAL TEST  (v4.1.0 IRONCLAD)
   ----------------------------------------------------------------------------
   jsdom mein poori index.html load kar ke asal mein check karta hai:
     • dono copies (public/ aur app assets) byte-for-byte same hain
     • koi ES6+ syntax nahi jo purane WebView ka script tor de
     • har slider enhance hua (track + fill + thumb + −/+ bane)
     • − / + buttons se value badalti hai aur input/change event firta hai
     • switch toggle par native checkbox.checked badalta hai + is-on class lagti hai
     • MayaUI.layout() ne main/tab ki height px mein set ki
     • saare zaroori setting IDs maujood hain (purana save/load JS na toote)
     • Settings markup mein koi banned inline style nahi

   Chalane ka tareeqa:
     npm i --no-save jsdom
     node tools/test-settings-ui.js
   ========================================================================= */
'use strict';

var fs = require('fs');
var path = require('path');
var ROOT = path.join(__dirname, '..');
var PUB = path.join(ROOT, 'public', 'index.html');
var APP = path.join(ROOT, 'app', 'src', 'main', 'assets', 'web', 'index.html');

var pass = 0, fail = 0;
function ok(name, extra) { pass++; console.log('  \u2705 ' + name + (extra ? '  \u2014 ' + extra : '')); }
function no(name, extra) { fail++; console.log('  \u274C ' + name + (extra ? '  \u2014 ' + extra : '')); }
function is(cond, name, extra) { cond ? ok(name, extra) : no(name, extra); }
function section(t) { console.log('\n' + t); }

/* ---------------------------------------------------------- 1. FILE CHECKS */
section('1. FILES');
var html = fs.readFileSync(PUB, 'utf8');
is(fs.existsSync(APP) && fs.readFileSync(APP, 'utf8') === html,
   'public/index.html aur app assets copy identical hain');

section('2. SYNTAX (purane WebView ka script na toote)');
var scripts = [];
html.replace(/<script(?![^>]*src=)[^>]*>([\s\S]*?)<\/script>/g, function (m, code) { scripts.push(code); return m; });
is(scripts.length >= 2, 'inline scripts mile', scripts.length + ' script blocks');
var syntaxBad = 0;
scripts.forEach(function (code, i) {
  try { new Function(code); } catch (e) { syntaxBad++; console.log('     script #' + (i + 1) + ': ' + e.message); }
});
is(syntaxBad === 0, 'har inline script parse hota hai');
/* comments + string literals hata do — warna apne hi comment ka lafz "token" ban jata hai */
function stripCode(src) {
  return src
    .replace(/\/\*[\s\S]*?\*\//g, ' ')          // block comments
    .replace(/(^|[^:'"\\])\/\/[^\n]*/g, '$1 ')  // line comments
    .replace(/"(?:[^"\\]|\\.)*"/g, '""')        // "strings"
    .replace(/'(?:[^'\\]|\\.)*'/g, "''")        // 'strings'
    /* regex literals — inke andar ` ya => jaise tokens jhoota alarm dete hain */
    .replace(/([(,=:[!&|?{};+]\s*)\/(?![*\/])(?:\\.|\[(?:\\.|[^\]\\])*\]|[^\/\\\n])+\/[gimsuy]*/g, '$1/RE/');
}
var joined = stripCode(scripts.join('\n'));
var rawJoined = scripts.join('\n');
is(!/\\u\{/.test(rawJoined.replace(/\/\*[\s\S]*?\*\//g, ' ')),
   'koi ES6 \\u{...} code-point escape nahi (Chrome <44 ise tor deta hai)');
is(!/=>/.test(joined), 'koi arrow function nahi');
is(!/(^|[^.\w])(const|let)\s+[A-Za-z_$]/.test(joined), 'koi const/let nahi');
is(!/\?\./.test(joined), 'koi optional chaining nahi');
is(!/`/.test(joined), 'koi template literal nahi');

/* ------------------------------------------------------------- 3. MARKUP */
section('3. SETTINGS MARKUP');
var setSection = html.slice(html.indexOf('id="tab-set"'), html.indexOf('</section>', html.indexOf('id="tab-set"')));
var NEEDED_IDS = ['sName', 'sGender', 'sPhone', 'sCity', 'sMusicApp', 'sFavSong', 'sYtChannel',
  'sAssistName', 'sPersona', 'sGf', 'sRemember', 'sConvo', 'sLang', 'sProactive', 'sAuto', 'sWake',
  'sVoiceOn', 'sNotifOn', 'sBatAlert', 'sStt', 'sTts', 'sVoice', 'sPitch', 'sRate', 'sRadius',
  'sFont', 'sEdgeGlow', 'sKey', 'sModel', 'sGqKey',  'sGhKey', 'sCerebras', 'sMistral', 'sOpenRouter', 'sNvidia', 'sZai', 'sLlm7', 'sTtsKey', 'sTurbo',
  'sVoiceEngine', 'sGVoice', 'sVoiceMood', 'sNeuralWifi', 'sEdgeTts',
  'saveSettings', 'pitchVal', 'rateVal', 'radiusVal', 'fontVal', 'themeGrid', 'accentRow',
  'artistGrid', 'diagOut', 'keyTestOut', 'notifStatus', 'batStatus', 'asStatus', 'uiCheckBtn'];
var missingIds = NEEDED_IDS.filter(function (id) { return setSection.indexOf('id="' + id + '"') === -1; });
is(missingIds.length === 0, 'saare ' + NEEDED_IDS.length + ' setting IDs maujood hain',
   missingIds.length ? 'missing: ' + missingIds.join(', ') : '');
is(!/style="[^"]*color-mix|style="[^"]*accent-color|style="[^"]*display:\s*grid/.test(setSection),
   'settings markup mein koi banned inline style nahi');

/* --------------------------------------------------------- 4. LIVE DOM */
section('4. LIVE DOM (jsdom)');
var JSDOM;
try { JSDOM = require('jsdom').JSDOM; }
catch (e) { console.log('  \u26A0 jsdom nahi mila — `npm i --no-save jsdom` chalao'); process.exit(2); }

var dom = new JSDOM(html, {
  runScripts: 'dangerously',
  pretendToBeVisual: true,
  url: 'https://localhost/',
  virtualConsole: new (require('jsdom').VirtualConsole)()   // app ke apne warnings chhupa do
});
var win = dom.window, doc = win.document;

/* jsdom layout nahi karta — getBoundingClientRect ko CSS ke hisaab se naqli
   size do taake geometry logic test ho sake (visibility ka asli test device
   par 🧪 UI CHECK karta hai). */
var rectFor = function (el) {
  var st = win.getComputedStyle(el);
  if (st.display === 'none') return { width: 0, height: 0, left: 0, top: 0, right: 0, bottom: 0 };
  var w = parseFloat(st.width) || 300, h = parseFloat(st.height) || parseFloat(st.minHeight) || 24;
  return { width: w, height: h, left: 0, top: 0, right: w, bottom: h };
};
win.Element.prototype.getBoundingClientRect = function () { return rectFor(this); };

setTimeout(function () {
  var MayaUI = win.MayaUI;
  is(!!MayaUI, 'MayaUI module load hua');
  if (!MayaUI) { done(); return; }

  /* --- sliders --- */
  section('5. SLIDERS');
  var ranges = doc.querySelectorAll('#tab-set input[type=range]');
  is(ranges.length === 4, '4 sliders mile', ranges.length + ' mile');
  var sliderBad = 0;
  Array.prototype.forEach.call(ranges, function (r) {
    var ui = r.__muiUI;
    if (!ui) { sliderBad++; console.log('     ' + r.id + ': enhance nahi hua'); return; }
    var haveAll = ui.track && ui.fill && ui.thumb && ui.minus && ui.plus;
    if (!haveAll) { sliderBad++; console.log('     ' + r.id + ': adhoora DOM'); }
  });
  is(sliderBad === 0, 'har slider ka custom DOM bana (track + fill + thumb + \u2212/+)');

  var pitch = doc.getElementById('sPitch');
  var before = parseFloat(pitch.value);
  var evInput = 0, evChange = 0;
  pitch.addEventListener('input', function () { evInput++; });
  pitch.addEventListener('change', function () { evChange++; });
  pitch.__muiUI.plus.click();
  var afterPlus = parseFloat(pitch.value);
  is(Math.abs(afterPlus - (before + 0.05)) < 1e-6, '+ button se value barhti hai',
     before + ' \u2192 ' + afterPlus);
  is(evInput > 0 && evChange > 0, 'input + change events firte hain (purana JS chalta rahega)',
     'input=' + evInput + ' change=' + evChange);
  pitch.__muiUI.minus.click();
  is(Math.abs(parseFloat(pitch.value) - before) < 1e-6, '\u2212 button se value ghatti hai',
     afterPlus + ' \u2192 ' + pitch.value);
  is(doc.getElementById('pitchVal').textContent.indexOf(String(before.toFixed(2))) === 0,
     'label ka value readout update hua', '#pitchVal = ' + doc.getElementById('pitchVal').textContent);

  /* clamp check */
  var radius = doc.getElementById('sRadius');
  for (var i = 0; i < 50; i++) radius.__muiUI.plus.click();
  is(parseFloat(radius.value) === 24, 'slider max par ruk jata hai (clamp)', 'sRadius = ' + radius.value);
  for (var j = 0; j < 60; j++) radius.__muiUI.minus.click();
  is(parseFloat(radius.value) === 4, 'slider min par ruk jata hai (clamp)', 'sRadius = ' + radius.value);
  is(radius.__muiUI.fill.style.width === '0%', 'min par fill 0% hai', 'fill = ' + radius.__muiUI.fill.style.width);

  /* thumb position */
  radius.value = 14; radius.__muiPaint();
  is(radius.__muiUI.thumb.style.left === '50%', 'thumb sahi jagah par hai (14/4..24 = 50%)',
     'left = ' + radius.__muiUI.thumb.style.left);

  /* --- switches --- */
  section('6. SWITCHES');
  var sws = doc.querySelectorAll('#tab-set .ui-sw input');
  is(sws.length >= 11, 'sab switches mile', sws.length + ' switches');
  var gf = doc.getElementById('sGf');
  var wasOn = gf.checked;
  var chg = 0;
  gf.addEventListener('change', function () { chg++; });
  gf.checked = !wasOn;
  gf.dispatchEvent(new win.Event('change'));
  is(gf.checked === !wasOn, 'checkbox state badalta hai');
  is((gf.parentNode.className.indexOf('is-on') > -1) === gf.checked,
     'is-on class checkbox ke sath sync hai', 'class = "' + gf.parentNode.className + '"');
  var trackEl = gf.parentNode.querySelector('.ui-sw-t');
  var knobEl = gf.parentNode.querySelector('.ui-sw-k');
  is(!!trackEl && !!knobEl, 'switch ka track + knob DOM maujood hai');

  /* --- layout --- */
  section('7. MEASURED LAYOUT');
  win.innerHeight = 800;
  var mh = MayaUI.layout();
  var main = doc.querySelector('main');
  var tabSet = doc.getElementById('tab-set');
  is(/px$/.test(main.style.height) && parseFloat(main.style.height) > 100,
     'main ki height px mein set hui', 'main = ' + main.style.height);
  is(/px$/.test(tabSet.style.height) && parseFloat(tabSet.style.height) > 100,
     'tab ki height px mein set hui', '#tab-set = ' + tabSet.style.height);
  is(tabSet.style.overflowY === 'auto', 'tab par overflow-y:auto laga hai');
  win.innerHeight = 500;
  MayaUI.layout();
  is(parseFloat(main.style.height) < mh, 'resize par height dobara naapi jati hai',
     mh + 'px \u2192 ' + main.style.height);

  /* --- groups --- */
  section('8. GROUPS');
  var grp = doc.getElementById('grp-voice');
  var head = grp.querySelector('.ui-head');
  var openBefore = grp.className.indexOf('is-open') > -1;
  head.click();
  is((grp.className.indexOf('is-open') > -1) !== openBefore, 'group header click par khulta/band hota hai');
  is(win.getComputedStyle(grp.querySelector('.ui-body')).display === 'block',
     'khulne par body display:block hoti hai');

  /* --- self test --- */
  section('9. SELF TEST API');
  var res = MayaUI.selfTest();
  is(res && typeof res.text === 'string' && res.text.indexOf('UI CHECK') > -1, 'selfTest() report deta hai');
  is(res.visible > 40, 'selfTest ne 40+ controls naape', res.visible + ' visible / ' + res.invisible + ' invisible');

  /* --- AWAAZ studio (v4.2.0) --- */
  section('10. AWAAZ STUDIO');
  try { win.loadSettingsForm(); } catch (e) { console.log('     loadSettingsForm: ' + e.message); }
  var gv = doc.getElementById('sGVoice'), vm = doc.getElementById('sVoiceMood');
  is(gv && gv.options.length === 30, 'voice picker mein 30 awaazein bhar gayin', gv ? gv.options.length + ' options' : 'select hi nahi');
  is(vm && vm.options.length >= 9, 'mood picker bhar gaya', vm ? vm.options.length + ' options' : 'select hi nahi');
  is(gv && gv.value === 'Kore', 'settings ki chuni hui awaaz select par lagi', gv && gv.value);
  var eng = doc.getElementById('sVoiceEngine');
  is(eng && eng.options.length === 5, 'engine ke 5 mode (auto/neural/edge/device/off)', eng ? eng.options.length + '' : '-');
  var engVals = eng ? Array.prototype.map.call(eng.options, function (o) { return o.value; }).join(',') : '';
  is(engVals === 'auto,neural,edge,device,off', 'edge mode picker mein maujood hai', engVals);
  var ev = doc.getElementById('sEdgeVoice');
  is(!!ev && ev.options.length > 5, 'Edge awaaz picker bhar gaya', ev ? ev.options.length + ' options' : 'nadarad');
  is(!!ev && ev.options[0].value === '', 'Edge picker ka pehla option Auto hai', ev ? ev.options[0].textContent.slice(0, 24) : '-');
  is(!!doc.getElementById('edgeTest'), 'EDGE AWAAZ SUNO button maujood', 'button');
  is(eng && eng.value === (win.settings.voiceEngine || 'auto'), 'engine mode form par load hua', eng && eng.value);
  try { win.loadSettingsForm(); } catch (e) {}
  is(gv && gv.options.length === 30, 'dobara load par options duplicate nahi hote', gv ? gv.options.length + '' : '-');
  var hint = doc.getElementById('awaazStatus');
  is(hint && hint.textContent && hint.textContent !== '\u2014', 'AWAAZ status line likhi gayi', hint && hint.textContent.slice(0, 46));
  is(!!win.AWAAZ && win.AWAAZ.VER >= 7, 'AWAAZ engine v7+ load hua');
  is(typeof win.paintAwaaz === 'function' && typeof win.awaazBadge === 'function', 'badge + status painter maujood');
  /* save -> settings mein sach much pahunchta hai */
  gv.value = 'Puck'; vm.value = 'whisper'; eng.value = 'device';
  doc.getElementById('sNeuralWifi').checked = true;
  try { doc.getElementById('saveSettings').click(); } catch (e) { console.log('     save: ' + e.message); }
  is(win.settings.gVoice === 'Puck' && win.settings.voiceMood === 'whisper', 'awaaz + mood save hue', win.settings.gVoice + '/' + win.settings.voiceMood);
  is(win.settings.voiceEngine === 'device' && win.settings.neuralWifiOnly === true, 'engine mode + WiFi-only save hue');
  is(win.AWAAZ.moodId() === 'whisper' && win.AWAAZ.voiceId() === 'Puck', 'engine ne nayi settings foran uthayin');

  /* --- SETFORM (v4.5.0): jo bug user ne pakda tha --- */
  section('11. SETFORM — SAVE par kuch bhi chup-chaap reset na ho');
  is(Array.isArray(win.SET_FIELDS) && win.SET_FIELDS.length >= 40, 'SET_FIELDS registry maujood', (win.SET_FIELDS || []).length + ' fields');
  is(!!win.SETFORM && typeof win.SETFORM.save === 'function', 'SETFORM.save() wahid darwaza hai');

  /* har registry field ka DOM element sach much maujood ho */
  var ghayb = (win.SET_FIELDS || []).filter(function (f) { return !doc.getElementById(f.id); });
  is(ghayb.length === 0, 'registry ka har field DOM mein maujood', ghayb.map(function (f) { return f.id; }).join(', ') || 'sab mile');

  /* ── ASAL BUG: GF mode + Groq key ek saath, phir save ── */
  doc.getElementById('sGender').value = 'male';
  doc.getElementById('sGf').checked = true;
  doc.getElementById('sGqKey').value = 'gsk_test_key_123456';
  doc.getElementById('sProactive').checked = true;
  doc.getElementById('sAssistName').value = 'Mana';
  doc.getElementById('sPhone').value = '03001234567';
  doc.getElementById('sLang').value = 'urdu';
  try { doc.getElementById('saveSettings').click(); } catch (e) { console.log('     save: ' + e.message); }

  is(win.settings.gfMode === true, '🔥 GF MODE save hua (purana bug: chup-chaap false ho jata tha)', String(win.settings.gfMode));
  is(win.settings.groqKey === 'gsk_test_key_123456', '  → saath mein Groq key bhi save hui', win.settings.groqKey);
  is(win.settings.proactive === true, '  → proactive bhi bacha');
  is(win.settings.assistName === 'Mana', '  → naam bhi bacha', win.settings.assistName);
  is(win.settings.phone === '03001234567', '  → phone bhi bacha');
  is(win.settings.lang === 'urdu', '  → language bhi bachi');
  is(doc.getElementById('sGf').checked === true, '  → save ke baad switch ON hi raha (form wapas nahi palta)');

  /* dobara save karne par bhi wahi rahe (pehle yahan reset hota tha) */
  try { doc.getElementById('saveSettings').click(); } catch (e) {}
  is(win.settings.gfMode === true, 'doosri dafa SAVE par bhi GF mode zinda');
  is(win.settings.groqKey === 'gsk_test_key_123456', 'doosri dafa SAVE par key bhi zinda');

  /* ek hi save handler — do handler hi asal bug the */
  var srcAll = fs.readFileSync(path.join(ROOT, 'public/index.html'), 'utf8');
  is((srcAll.match(/\$\("#saveSettings"\)\.addEventListener/g) || []).length === 1,
     'SAVE button par sirf EK handler (do handler hi asal bug the)');
  is(!/function loadV4Form\(\)\{\n  try \{/.test(srcAll), 'purana loadV4Form nikal gaya');

  /* kai keys */
  doc.getElementById('sKey').value = 'AIzaKey_One, AIzaKey_Two';
  try { doc.getElementById('saveSettings').click(); } catch (e) {}
  is(win.settings.apikey === 'AIzaKey_One,AIzaKey_Two', 'Gemini khane mein kai keys comma se save hueen', win.settings.apikey);
  is(win.BRAIN.keys(win.BRAINS.filter(function (b) { return b.id === 'gemini'; })[0]).length === 2, '  → BRAIN POOL ne dono keys pakar leen');

  /* galat khane mein key -> khud sahi jagah */
  doc.getElementById('sKey').value = 'gsk_ye_groq_ki_key_hai';
  doc.getElementById('sGqKey').value = '';
  try { doc.getElementById('saveSettings').click(); } catch (e) {}
  is(win.settings.groqKey === 'gsk_ye_groq_ki_key_hai', 'Gemini khane mein pari GROQ key khud sahi jagah chali gayi', win.settings.groqKey);
  is(win.settings.apikey === '', '  → Gemini khana khaali ho gaya');

  /* gender male na ho to GF mode ka koi matlab nahi */
  doc.getElementById('sGender').value = 'female';
  doc.getElementById('sGf').checked = true;
  try { doc.getElementById('saveSettings').click(); } catch (e) {}
  is(win.settings.gfMode === false, 'Gender female par GF mode khud band (aur wajah batai gayi)');

  done();
}, 400);

function done() {
  console.log('\n' + '='.repeat(58));
  console.log(fail === 0
    ? '\u2705 SAB TEST PASS \u2014 ' + pass + '/' + (pass + fail)
    : '\u274C ' + fail + ' TEST FAIL \u2014 ' + pass + '/' + (pass + fail) + ' pass');
  console.log('='.repeat(58));
  process.exit(fail === 0 ? 0 : 1);
}
