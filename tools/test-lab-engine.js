#!/usr/bin/env node
/* ════════════════════════════════════════════════════════════════════════
   tools/test-lab-engine.js — 🧪 MAYA LAB ka SABOOT   (P0 + P1 + 👑 MALIK)
   ------------------------------------------------------------------------
   Asli code public/index.html se nikal kar jsdom mein chalta hai.

   Sab se ahem: SAAF ke test asli screenshot ke matn par chalte hain — wohi
   jo user ne bheja tha. Wo bug dobara nikal hi nahi sakta.

     node tools/test-lab-engine.js
   ════════════════════════════════════════════════════════════════════════ */

const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const ROOT = path.resolve(__dirname, '..');
const HTML = fs.readFileSync(path.join(ROOT, 'public/index.html'), 'utf8');

let pass = 0, fail = 0;
const is = (cond, name, info) => {
  if (cond) { pass++; console.log('  \x1b[32m✅\x1b[0m ' + name + (info ? '  \x1b[2m— ' + info + '\x1b[0m' : '')); }
  else { fail++; console.log('  \x1b[31m❌\x1b[0m ' + name + (info ? '  \x1b[33m— ' + info + '\x1b[0m' : '')); }
};
const head = (t) => console.log('\n\x1b[1m' + t + '\x1b[0m');

/* ── LAB source ── */
const LA = HTML.indexOf('var SCHEMA = {');
const LB = HTML.indexOf('var AWAAZ = {');
if (LA < 0 || LB < 0 || LB < LA) { console.error('MAYA LAB source nahi mila — index.html badal gaya?'); process.exit(1); }
const LAB = HTML.slice(LA, LB);

/* ⚡ AMAL (P2a) — TOOL_DECLS + execTool ke baad rehta hai */
const AA = HTML.indexOf('var AMAL = {');
const AB = HTML.indexOf('/* --- OpenAI-shakl providers ab BRAIN POOL sambhalta hai --- */');
if (AA < 0 || AB < 0 || AB < AA) { console.error('AMAL source nahi mila'); process.exit(1); }
const AMALSRC = HTML.slice(AA, AB);
const TD_A = HTML.indexOf('var TOOL_DECLS = [');
const TD_B = HTML.indexOf('async function execTool');
const TOOLSRC = HTML.slice(TD_A, TD_B);

function world(flags) {
  const dom = new JSDOM('<!doctype html><body></body>', { runScripts: 'dangerously', url: 'https://appassets.androidplatform.net/' });
  const w = dom.window;
  w.pushLog = () => {};
  w.$ = () => null;
  w.NATIVE = false;
  w.settings = { name: 'Boss', lang: 'roman-ur' };
  w.sysLogs = [];
  w.eval(LAB);
  w.eval(TOOLSRC);
  w.execTool = async function (n, a) { w.__ran = w.__ran || []; w.__ran.push({ n, a }); return { done: true, echo: a }; };
  w.eval(AMALSRC);
  if (flags) { for (const k in flags) w.FLAGS.set(k, flags[k]); }
  return w;
}

/* Wohi matn jo user ne screenshot mein bheja tha */
const REAL_SCREENSHOT = `<think>
The user wants to set brightness to 100%.
I need to use the brightness_control tool.
The user spoke in Roman Hindi/Hinglish ("Britness barhao 100%").
Language rule says: "Reply in Hindi (Devanagari script)."
I will call the tool, then reply in Hindi (Devanagari) confirming the action.

Tool: brightness_control
Parameters: likely level or value = 100.

Response draft (Hindi Devanagari):
चमक 100 पर सेट कर दी है, बॉस।

Check constraints:
- Spoken aloud style: yes.
- 1-3 sentences, <60 words: yes.
- Hindi Devanagari: yes.
</think>

brightness_control(level=100)`;

const REAL_TRUNCATED = `<think>
Here's a thinking process:
1. **Analyze User Input:**
   - User says: "Chalo choro isko or Suno"
   - Language: Roman Hindi/Hinglish
2. **Draft Construction (Mental):**
   बॉस, बताइए अब मैं क्या करूँ?
3. **Refine against Constraints:**
   - Hindi (Devanagari)? Yes.`;

(function run() {
  console.log('\n\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m');
  console.log('\x1b[1m  🧪  MAYA LAB — P0 (NAAP) + P1 (SAAF) + 👑 MALIK\x1b[0m');
  console.log('\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m');

  /* ═══ 1. SAAF — soch kabhi bahar na aaye ═══ */
  head('1. 🧹 SAAF — andar ki soch (asal bug jo screenshot mein tha)');
  {
    const w = world({ saaf: true });
    const S = w.SAAF;

    /* ── asli screenshot ── */
    const a = S(REAL_SCREENSHOT);
    is(a.indexOf('<think') < 0 && a.indexOf('</think') < 0, '🔑 ASLI SCREENSHOT: <think> poora ghayab', JSON.stringify(a.slice(0, 40)));
    is(a.indexOf('Language rule says') < 0 && a.indexOf('Check constraints') < 0, '   → "Check constraints" wali soch bhi gayi');
    is(a.indexOf('Response draft') < 0, '   → "Response draft" bhi gaya');
    is(a.indexOf('brightness_control(level=100)') < 0, '   → tool ka NATAK bhi hata (P2 mein ye CHALEGA)');
    is(w.SAAF.usable(REAL_SCREENSHOT) === '', '🔑 kuch na bacha → EMPTY → AGLA DIMAAG (khali bubble nahi)', 'usable=""');

    /* ── kata hua think (token khatam) ── */
    const b = S(REAL_TRUNCATED);
    is(b.indexOf('<think') < 0 && b.indexOf('Here\'s a thinking') < 0,
      '🔑 BAND NA HUA <think> bhi kat gaya (320-token wala hadsa)', JSON.stringify(b.slice(0, 30)));
    is(b.indexOf('Analyze User Input') < 0 && b.indexOf('Refine against') < 0, '   → numbered analysis bhi gaya');

    /* ── har shakl ── */
    is(S('<thinking>abc</thinking>Salam') === 'Salam', '<thinking> tag');
    is(S('<reasoning>x</reasoning>Salam') === 'Salam', '<reasoning> tag');
    is(S('<|channel|>analysis blah<|message|>Salam').trim() === 'Salam', 'gpt-oss harmony channel', 'harmony');
    is(S('<|start|>Salam<|end|>').trim() === 'Salam', 'akele harmony token');
    is(S('</think> Salam').trim() === 'Salam', 'akela band hone wala tag');
    is(S('\u2039AMAL\u203A{"tool":"x"}\u2039/AMAL\u203A Salam').trim() === 'Salam', 'hamara AMAL protocol chhupa rehta hai');
    is(S('<tool_call>{"a":1}</tool_call>Salam').trim() === 'Salam', 'XML tool_call');
    is(S('torch_control(on=true)\nHo gaya boss').trim() === 'Ho gaya boss', 'akeli tool-call line hat gayi');
    is(S('Let me think\nSalam boss').trim() === 'Salam boss', '"Let me think" header');

    /* ── ASAL JAWAB kabhi na kate ── */
    is(S('Ho gaya boss, brightness 100 kar di.') === 'Ho gaya boss, brightness 100 kar di.',
      '✅ SAHIH jawab bilkul nahi chhua jata');
    is(S('\u0622\u062C \u0645\u0648\u0633\u0645 \u0627\u0686\u06BE\u0627 \u06C1\u06D2') === '\u0622\u062C \u0645\u0648\u0633\u0645 \u0627\u0686\u06BE\u0627 \u06C1\u06D2', '✅ Urdu script salamat');
    is(S('150 ka 18 percent 27 hota hai (yani solah aana theek).').indexOf('27 hota hai') > 0,
      '✅ jumle ke andar bracket wala hisab salamat');
    is(S('Pehli baat.\nDoosri baat.\nTeesri baat.').split('\n').length === 3, '✅ aam kai-line jawab salamat');

    /* ── nazuk soortein ── */
    is(S(null) === '' && S(undefined) === '' && S('') === '', 'khali/null par crash nahi');
    is(S(S('<think>x</think>Salam')) === 'Salam', 'dobara chalane par bhi wahi (idempotent)');
    const combo = S('<think>a</think><|channel|>b<|message|>Check constraints:\nSalam');
    is(combo.trim() === 'Salam', 'kai shaklein ek sath', JSON.stringify(combo.trim()));
    S('<think>abcdefghij</think>Salam');
    is(S.cut > 10 && /think/.test(S.hit), 'kitna kata aur kya kata — dono darj', S.cut + ' harf · ' + S.hit);

    /* ── switch OFF: kuch na ho ── */
    const off = world({ saaf: false });
    is(off.SAAF.usable(REAL_SCREENSHOT) === REAL_SCREENSHOT,
      '🔒 switch OFF → matn bilkul nahi chhua (Qanoon 1)');
    is(off.SAAF('<think>x</think>Salam') === 'Salam', '   → magar SAAF() khud phir bhi kaam karta hai');
  }

  /* ═══ 2. FLAGS ═══ */
  head('2. 🧪 FLAGS — har cheez switch ke peeche (Qanoon 1)');
  {
    const w = world();
    is(w.FLAGS.on('naap') === true, 'naap default ON (bay-zarar hai)');
    is(w.FLAGS.on('saaf') === false && w.FLAGS.on('malik') === false,
      'saaf aur malik default OFF — pehle sabit hon, phir chalen');
    w.FLAGS.set('saaf', true);
    is(w.FLAGS.on('saaf') === true, 'switch ON hota hai');
    is(String(w.localStorage.getItem('maya_flags')).indexOf('"saaf":true') > 0, 'switch app band hone par bhi yaad');
    w.FLAGS.set('saaf', 'haan');
    is(w.FLAGS.on('saaf') === false, 'sirf sacha boolean qubool (kachra nahi)');
    w.FLAGS.reset();
    is(w.FLAGS.on('naap') === true && w.FLAGS.on('saaf') === false, 'reset default par le aata hai');
    is(w.FLAGS.on('bakwas') === false, 'anjaan switch hamesha OFF');
  }

  /* ═══ 3. NAAP ═══ */
  head('3. 📊 NAAP — naapne ka aala (P0)');
  {
    const w = world({ naap: true });
    const N = w.NAAP;
    N.clear();
    N.start('voice');
    is(!!N.cur && N.cur.src === 'voice', 'turn shuru hua');
    N.mark('brain'); N.mark('voice');
    N.mark('brain');
    is(typeof N.cur.m.brain === 'number', 'mark darj hua');
    const first = N.cur.m.brain;
    N.mark('brain');
    is(N.cur.m.brain === first, 'ek mark sirf EK dafa (pehla waqt hi asli hai)');
    N.end({ brain: 'Groq', engine: 'edge', tool: 'torch_control' });
    is(N.cur === null && N.hist.length === 1, 'turn khatam, tareekh mein darj');
    is(N.hist[0].brain === 'Groq' && N.hist[0].engine === 'edge' && typeof N.hist[0].m.done === 'number',
      'dimaag, awaaz aur kul waqt — sab mehfooz');
    is(N.pct([10, 20, 30, 40, 100], 50) === 30, 'p50 sahih nikalta hai', '30');
    is(N.pct([10, 20, 30, 40, 100], 90) === 100, 'p90 sahih (औsat jhoot bolta, ye nahi)', '100');
    is(N.pct([], 50) === 0, 'khali par crash nahi');
    const st = N.stats();
    is(st.turns === 1 && st.brains.Groq === 1 && st.toolTurns === 1, 'stats ginti sahih');
    is(N.report().indexOf('BASELINE') >= 0 && N.report().indexOf('Groq') > 0, 'report likhi jati hai');
    is(String(w.localStorage.getItem('maya_naap')).indexOf('Groq') > 0, 'naap app band hone par bhi mehfooz');
    N.clear();
    is(N.hist.length === 0 && N.report().indexOf('Abhi koi naap nahi') > 0, 'clear kaam karta hai');
    const off = world({ naap: false });
    off.NAAP.start('text');
    is(off.NAAP.cur === null, '🔒 switch OFF → naap bilkul nahi hoti');
  }

  /* ═══ 4. MALIK ═══ */
  head('4. 👑 MALIK — Maya ko apne banane wale ka pata');
  {
    const w = world({ malik: true });
    const M = w.MALIK;
    is(M.name === 'Adil Chandio' && M.brand === 'Monarch', 'naam aur brand', M.name + ' / ' + M.brand);
    is(M.ASK.test('tumhe kis ne banaya') && M.ASK.test('who made you') &&
       M.ASK.test('tumhara malik kaun hai') && M.ASK.test('Who created you?'),
      'sawal ke sab andaaz pehchane jate hain');
    is(!M.ASK.test('brightness barhao') && !M.ASK.test('mausam kaisa hai'),
      'aam hukm ko galti se ta\'aruf na samjhe');
    is(M.pick('tumhe kis ne banaya', false).indexOf('Adil Chandio') === 0, 'aam sawal → chhota ta\'aruf');
    is(M.pick('who made you', false).indexOf('Adil Chandio') === 0 && /Founder of Monarch/.test(M.pick('who made you', false)),
      'English sawal → English jawab');
    is(M.pick('poora batao kis ne banaya', false).length > M.pick('kis ne banaya', false).length,
      '"poora batao" → lamba ta\'aruf');
    is(/fakhr hai, Monarch/.test(M.pick('kis ne banaya', true)), '👑 Adil KHUD puche → flex mode', 'flex');
    const blk = M.block();
    is(blk.indexOf('Adil Chandio') > 0 && blk.indexOf('Monarch') > 0, 'prompt block mein malik ki pehchan');
    is(/SIRF SACH/.test(blk), '🔒 prompt saaf kehta hai: sirf sach, kuch apni taraf se mat joro');
    is(blk.length < 700, 'block chhota hai — prompt phool kar phate nahi (CHHED 9)', blk.length + ' harf');
    const off = world({ malik: false });
    is(off.MALIK.block() === '', '🔒 switch OFF → prompt mein kuch nahi jata');
    let withNum = 0;
    for (let i = 0; i < M.proof.length; i++) if (/\d/.test(M.proof[i])) withNum++;
    is(M.proof.length >= 5 && withNum >= 4,
      'saboot naapa hua hai, jhooti tareef nahi', withNum + '/' + M.proof.length + ' mein number');
    is(!/best|greatest|genius|legend|world.?class|sab se behtareen/i.test(M.proof.join(' ') + M.intro.flex),
      '🔒 koi khokhli tareef nahi — sirf jo sach mein hua');
  }

  /* ═══ 5. DIAG ═══ */
  head('5. 📋 DIAG — sab kuch copy, magar raaz chhupa kar (CHHED 3 + 14)');
  {
    const w = world();
    const D = w.DIAG;
    is(D.redact('key AIzaSyAbCdEfGhIjKlMnOpQrStUvWxYz123').indexOf('AIzaSyAbCd') < 0, '🔒 Gemini key chhup gayi');
    is(D.redact('gsk_abcdefghij1234567890').indexOf('abcdefghij') < 0, '🔒 Groq key chhupi');
    is(D.redact('Authorization: Bearer sk-abcdef123456').indexOf('sk-abcdef') < 0, '🔒 Bearer token chhupa');
    is(D.redact('ghp_ABCDEFGHIJ1234567890').indexOf('ABCDEFGHIJ') < 0, '🔒 GitHub token chhupa');
    is(D.redact('call 03001234567 abhi') === 'call <number> abhi', '🔒 phone number chhupa', D.redact('call 03001234567 abhi'));
    is(D.redact('mail adil@example.com karo').indexOf('adil@example') < 0, '🔒 email chhupa');
    is(D.redact('brightness 100 kar do') === 'brightness 100 kar do', '✅ aam matn bilkul nahi badla');
    const built = D.build();
    is(built.indexOf('MAYA DIAGNOSTIC') > 0 && built.indexOf('LAB') > 0 && built.indexOf('SETTINGS') > 0,
      'diagnostic ke saare hisse maujood', built.length + ' harf');
    is(built.indexOf('AIzaSy') < 0, '🔒 poore diagnostic mein koi key nahi bachi');
  }

  /* ═══ 6. HARVEST ═══ */
  head('6. 🗃️ HARVEST — aap ki apni zubaan se test set (CHHED 2)');
  {
    const w = world();
    w.localStorage.setItem('maya_chat', JSON.stringify([
      { role: 'user', text: 'britness barhao 100%' },
      { role: 'model', text: 'ho gaya' },
      { role: 'user', text: 'Ali ko call karo 03001234567 par' },
      { role: 'user', text: 'alarm laga do 7 baje' },
      { role: 'user', text: 'x' }
    ]));
    w.ACTION_WORDS = /(alarm|call)/i;
    const got = w.HARVEST.scan();
    is(got.length === 3, 'sirf user ke jumle liye gaye (bohat chhote chhor kar)', got.length + '');
    is(got.join(' ').indexOf('03001234567') < 0 && got.join(' ').indexOf('<number>') > 0,
      '🔒 number chhupa kar hi nikala gaya');
    const rep = w.HARVEST.report();
    is(rep.indexOf('britness barhao 100%') > 0, '🔑 "britness" jaisa lafz pakra gaya — jo main soch bhi nahi sakta tha');
    is(/router ne pakre\s*:\s*2/.test(rep), 'router ka hisab sahih (alarm + call)', 'hit=2');
    is(rep.indexOf('phone par hi hai') > 0, 'saaf likha hai ke data phone se bahar nahi jata');
  }

  /* ═══ 7. TOKEN BUDGET ═══ */
  head('7. 🪙 TOKEN BUDGET — reasoning models ko soch ki jagah (P1)');
  {
    const src = HTML;
    const m = /REASONER:\s*(\/.*?\/[a-z]*),/.exec(src);
    is(!!m, 'REASONER pehchan maujood');
    const re = eval(m[1]);
    is(re.test('openai/gpt-oss-120b:free') && re.test('qwen3.6-plus') && re.test('deepseek-r1'),
      'reasoning models pehchane jate hain');
    is(!re.test('gemini-2.5-flash') && !re.test('llama-4-scout') && !re.test('mistral-small-latest'),
      'aam models galti se reasoning na samjhe jayen');
    is(/budget: function \(model\)[\s\S]{0,140}1400[\s\S]{0,40}400/.test(src),
      '🔑 reasoning ko 1400, baqi ko 400 (pehle sab ko 320 tha — soch usi mein khatam)');
    is(src.indexOf('max_tokens: BRAIN.budget(model)') > 0, 'budget sach much request mein lag raha hai');
    is(src.indexOf('max_tokens: 320') < 0, 'purana 320 wala hadsa code se nikal gaya');
  }

  /* ═══ 8. ZUBAAN ═══ */
  head('8. 🗣️ ZUBAAN — do mutazad hukm ab ek (P1 · BUG 6)');
  {
    const src = HTML;
    is(/LANGUAGE RULE: SAB SE PEHLE user ki script ki NAQAL karo/.test(src),
      '🔑 ab MIRROR pehle hai — jo user bole wohi script');
    is(/Agar user ki script saaf pata na chale to: " \+\s*\(LR\[settings\.lang/.test(src),
      'setting sirf FALLBACK hai, hukm nahi');
    is(!/p \+= "\\nLANGUAGE RULE: " \+ \(LR\[settings\.lang/.test(src),
      'purana seedha "Reply in Hindi (Devanagari)" wala hukm khatam');
    is(src.indexOf('User ki language aur script mein jawab') > 0,
      'base RULE(3) ab LANGUAGE RULE se takrata nahi — dono ek baat kehte hain');
  }

  /* ═══ 9. SCHEMA ═══ */
  head('9. 🔖 SCHEMA — localStorage ki version (CHHED 4)');
  {
    const w = world();
    is(w.SCHEMA.at === w.SCHEMA.NOW, 'version set ho gaya', String(w.SCHEMA.at));
    is(w.localStorage.getItem('maya_schema') === String(w.SCHEMA.NOW), 'version mehfooz hai');
    is(typeof w.SCHEMA.init === 'function' && w.SCHEMA.init() === w.SCHEMA.NOW, 'dobara chalane par bhi wahi');
  }

  /* ═══ 10. TAALE — purana raasta zinda rahe (Qanoon 2) ═══ */
  head('10. 🔒 TAALE — kuch bhi ho, Maya chalti rahe');
  {
    const src = HTML;
    is(/if \(out && typeof SAAF !== "undefined"\)/.test(src),
      '🔑 LAB na ho to bhi dimaag ka jawab aata hai (Qanoon 2)');
    is(/if \(typeof SAAF !== "undefined"\) t = SAAF\.usable/.test(src), 'awaaz ka raasta bhi mehfooz');
    is(/if \(typeof MALIK !== "undefined"\) p \+= MALIK\.block\(\)/.test(src), 'prompt ka raasta bhi mehfooz');
    is((src.match(/try \{ NAAP\./g) || []).length >= 4, 'har NAAP hook try ke andar hai', 'guarded');
    is(src.indexOf('var FLAGS = {') > 0 && src.indexOf('var FLAGS = {') < src.indexOf('var AWAAZ = {'),
      'LAB block AWAAZ se PEHLE hai (sab ke liye maujood)');
    /* dhanche ka taala — harness naam se code kaatta hai */
    const order = ['var SCHEMA = {', 'var FLAGS = {', 'var NAAP = {', 'function SAAF(', 'var MALIK = {',
                   'var DIAG = {', 'var AWAAZ = {', 'var EDGE_TTS = {', 'var FISH = {', 'var BRAIN = {',
                   'var DIMAAG = {', 'var SETFORM = {'];
    let ok = true, at = -1, bad = '';
    for (const k of order) { const i = src.indexOf(k); if (i < 0 || i < at) { ok = false; bad = k; break; } at = i; }
    is(ok, '🔑 DHANCHE KA TAALA — saare namespace maujood aur sahih tarteeb mein', bad || 'sab theek');
  }


  /* ═══ 11. ⚡ AMAL — tools ab har dimaag ko (P2a) ═══ */
  head('11. ⚡ AMAL — asal ilaj: "tool wale turn 0/12"');
  {
    const w = world({ poolTools: true });
    const A = w.AMAL;

    /* ── OpenAI dialect ── */
    const T = A.openaiTools();
    is(T.length === w.TOOL_DECLS.length && T.length >= 30,
      '🔑 saare tools OpenAI shakl mein bhi tayyar', T.length + ' tools');
    is(T[0].type === 'function' && !!T[0].function.name && !!T[0].function.parameters,
      'shakl OpenAI wali hai (type/function/parameters)');
    is(T[0].function.parameters.type === 'object',
      '🔑 Gemini ka "OBJECT" -> OpenAI ka "object" (warna 400 aata)', T[0].function.parameters.type);
    const bri = T.filter(x => x.function.name === 'brightness_control')[0];
    is(!!bri && bri.function.parameters.properties.percent.type === 'integer',
      'andar ke types bhi chhote harf mein', bri ? bri.function.parameters.properties.percent.type : '-');
    is(A.openaiTools() === T, 'ek dafa ban kar yaad rehti hai (har request par nahi banti)');

    /* ── kaun tools le sakta hai ── */
    is(A.poolOn({ id: 'groq', kind: 'openai' }) === true, 'OpenAI-shakl dimaag ko tools milte hain');
    is(A.poolOn({ id: 'gemini', kind: 'gemini' }) === false, 'Gemini ka apna raasta — dohra nahi');
    A.markBad('groq', '400 tools unsupported');
    is(A.poolOn({ id: 'groq', kind: 'openai' }) === false,
      '🔑 jis ne 400 diya usay dobara nahi mara jata (dimaag marna nahi chahiye)');
    const off = world({ poolTools: false });
    is(off.AMAL.poolOn({ id: 'groq', kind: 'openai' }) === false, '🔒 switch OFF → koi tool nahi jata');

    /* ── ARG ALIAS (BUG 8 — screenshot mein level=100 aaya tha) ── */
    is(A.alias('brightness_control', { level: 100 }).percent === 100,
      '🔑 level -> percent (screenshot ka asal bug)', 'percent=100');
    is(A.alias('brightness_control', { value: '80%' }).percent === 80, '"80%" -> 80 (number ban gaya)');
    is(A.alias('volume_control', { vol: 50 }).percent === 50, 'vol -> percent');
    is(A.alias('torch_control', { state: 'on' }).on === true, '"on" -> true');
    is(A.alias('torch_control', { enable: 'band' }).on === false, '"band" -> false');
    is(A.alias('play_youtube', { song: 'Funk Taka' }).query === 'Funk Taka',
      '🔑 song -> query (aap ka "Funk Taka" wala hukm)');
    is(A.alias('brightness_control', { percent: 30, level: 99 }).percent === 30,
      'asli naam maujood ho to alias use nahi hota');
    is(JSON.stringify(A.alias('bakwas_tool', { x: 1 })) === '{"x":1}', 'anjaan tool par kuch nahi bigarta');

    /* ── tool sach much chalta hai ── */
    (async function () {
      const res = await A.runCalls([
        { id: 'c1', function: { name: 'brightness_control', arguments: '{"level":100}' } }
      ]);
      is(w.__ran.length === 1 && w.__ran[0].n === 'brightness_control' && w.__ran[0].a.percent === 100,
        '🔑 tool ASAL MEIN chala (alias ke sath)', 'percent=' + w.__ran[0].a.percent);
      is(res[0].role === 'tool' && res[0].tool_call_id === 'c1' && res[0].name === 'brightness_control',
        'nateeja OpenAI shakl mein wapas gaya');
      const bad = await A.runCalls([{ id: 'c2', function: { name: 'x', arguments: 'kachra{{' } }]);
      is(bad.length === 1, 'kharab arguments par bhi crash nahi');
    })();
  }

  /* ═══ 12. 🧭 ROUTER — 33/33 tools ab nazar aate hain ═══ */
  head('12. 🧭 ROUTER — pehle 12 tools nazar hi nahi aate the');
  {
    const w = world({ poolTools: true });
    const A = w.AMAL;
    const say = {
      brightness_control: 'britness barhao 100%', torch_control: 'torch on karo',
      volume_control: 'awaaz 50 kar do', set_reminder: 'mujhe 20 minute baad yaad dilana',
      prayer_times: 'maghrib ka waqt kya hai', reply_message: 'Ali ko reply karo theek hai',
      recall_memory: 'mujhe kya kya yaad hai', search_memory: 'yaad hai maine kya kaha tha',
      web_search: 'internet par talash karo', web_fetch: 'is website ko parho',
      create_skill: 'ye tareeqa seekh lo', notify: 'notification bhejo',
      set_alarm: 'subah 7 baje ka alarm laga do', set_timer: '10 minute ka timer laga do',
      open_app: 'whatsapp kholo', play_youtube: 'funk taka song lagao',
      call_contact: 'ammi ko call karo', message_contact: 'Ali ko message karo',
      get_weather: 'mausam kaisa hai', battery_status: 'battery kitni hai',
      run_javascript: '150 ka 18 percent kitna hua', wiki_search: 'wikipedia se batao',
      list_files: 'meri files dikhao', diary_write: 'diary mein likho aaj acha din tha',
      save_memory: 'yaad rakho meri birthday 5 june', read_messages: 'naye message parho'
    };
    const re = A.actionRe();
    let missed = [];
    for (const k in say) if (!re.test(say[k])) missed.push(k);
    is(missed.length === 0, '🔑 HAR hukm ab "kaam" pehchana jata hai', missed.length ? missed.join(',') : Object.keys(say).length + '/' + Object.keys(say).length);

    is(A.guess('britness barhao 100%') === 'brightness_control', 'britness → brightness_control (ghalat spelling bhi)');
    is(A.guess('torch on karo') === 'torch_control', 'torch → torch_control');
    is(A.guess('funk taka song lagao') === 'play_youtube', 'song → play_youtube');
    is(A.guess('maghrib ka waqt') === 'prayer_times', 'maghrib → prayer_times');
    is(!re.test('hello maya kaise ho'), 'aam gap-shap ko kaam na samjhe');

    /* har tool ke apne trigger hon */
    const noTrig = [];
    for (let i = 0; i < w.TOOL_DECLS.length; i++) {
      const n = w.TOOL_DECLS[i].name;
      if (!A.TRIGGERS[n] || !A.TRIGGERS[n].length) noTrig.push(n);
    }
    is(noTrig.length === 0, '🔑 HAR tool ke apne trigger lafz maujood (BUG 1 ka taala)', noTrig.join(',') || 'sab');
  }

  /* ═══ 13. 🩹 ASLI DEVICE SE MILE 5 BUG ═══ */
  head('13. 🩹 aap ke device ke diagnostic se mile bug');
  {
    const w = world({ saaf: true, malik: true });
    is(w.SAAF('play_youtube(query="Funk').trim() === '',
      '🔑 KATA HUA tool call bhi chhup gaya (band bracket nahi tha)', 'saaf');
    is(w.SAAF('brightness_control(level=100)').trim() === '', '   → poora tool call bhi');
    is(w.MALIK.ASK.test('\u092e\u0948\u0902 \u0924\u0941\u092e\u094d\u0939\u0947\u0902 \u0915\u093f\u0938\u0928\u0947 \u092c\u0928\u093e\u092f\u093e'),
      '🔑 Devanagari "किसने बनाया" ab pakra jata hai (aap ne isi mein poocha tha)');
    is(w.MALIK.ASK.test('\u062a\u0645\u06c1\u06cc\u06ba \u06a9\u0633 \u0646\u06d2 \u0628\u0646\u0627\u06cc\u0627'), '   → Urdu script bhi');
    is(w.MALIK.ASK.test('\u0906\u0926\u093f\u0932 \u091a\u093e\u0902\u0921\u093f\u092f\u094b \u0915\u094c\u0928 \u0939\u0948'),
      '   → "आदिल चांडियो कौन है" bhi');
    is(w.MALIK.ASK.test('adil chandio kaun hai'), '   → Roman mein bhi');
    const src = HTML;
    is(src.indexOf('VERSION 4.1.0 — IRONCLAD SETTINGS EDITION') < 0,
      '🔑 splash par hard-code "4.1.0" khatam (diagnostic ghalat version dikhata tha)');
    is(/p\.kind === "gemini"[\s\S]{0,120}dayQuotaUntil[\s\S]{0,20}continue/.test(src),
      '🔑 Gemini ka DIN ka quota khatam → poora dimaag skip (p90 31s ka sabab)');
    is(/if \(status === 402\)[\s\S]{0,120}21600000/.test(src),
      '🔑 402 (paisa maanga) → 6 ghante cooldown (Cerebras ne diya tha)');
    is(/if \(useTools && \(status === 400 \|\| status === 422\)/.test(src),
      'tools par 400 → bina tools dobara, dimaag marta nahi');
  }

  console.log('\n\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m');
  if (fail === 0) console.log('\x1b[1m\x1b[32m✅ SAB TEST PASS — ' + pass + '/' + pass + '\x1b[0m');
  else console.log('\x1b[1m\x1b[31m❌ ' + fail + ' TEST FAIL — ' + pass + '/' + (pass + fail) + ' pass\x1b[0m');
  console.log('\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m\n');
  process.exit(fail === 0 ? 0 : 1);
})();
