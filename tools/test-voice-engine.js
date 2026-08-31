#!/usr/bin/env node
/* ════════════════════════════════════════════════════════════════════════
   tools/test-voice-engine.js — AWAAZ ENGINE v6 ka saboot
   ------------------------------------------------------------------------
   Asli engine code public/index.html se nikal kar jsdom mein chalta hai,
   XHR/Audio/Blob naqli hain. Jo bug v4.1.0 tak chhupa raha (raw PCM bina WAV
   header ke) wo ab test ke bina paas nahi ho sakta.

     node tools/test-voice-engine.js
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

/* ─────────────── engine source nikaalo ─────────────── */
const A = HTML.indexOf('var GEMINI_VOICES = [');
const B = HTML.indexOf('function geminiTTS_stop()');
if (A < 0 || B < 0 || B < A) { console.error('AWAAZ engine source nahi mila — index.html badal gaya?'); process.exit(1); }
const ENGINE = HTML.slice(A, B);

/* ─────────────── naqli duniya ─────────────── */
function makeWorld(opts = {}) {
  const dom = new JSDOM('<!doctype html><body></body>', { runScripts: 'dangerously', url: 'https://appassets.androidplatform.net/' });
  const w = dom.window;

  const state = {
    calls: [],          /* har XHR */
    blobs: [],          /* har createObjectURL(Blob) */
    played: [],         /* har audio src */
    logs: [],
    deviceSaid: [],
    respond: opts.respond || (() => ({ status: 200, body: okBody() }))
  };

  w.settings = Object.assign({
    voiceOn: true, voiceEngine: 'auto', apikey: 'KEY-123', ttsKey: '',
    gVoice: 'Kore', voiceMood: 'auto', persona: 'maya',
    neuralWifiOnly: false, neuralMaxChars: 1400, edgeTTS: false,
    tts: 'ur-PK', rate: 1, pitch: 1.05, voiceName: '', name: 'Boss'
  }, opts.settings || {});

  w.NATIVE = false;
  w.pushLog = (m) => state.logs.push(m);
  w.paintAwaaz = () => {};
  w.pickVoice = () => null;
  w.voices = [];
  w.synth = {
    cancel() {},
    speak(u) { state.deviceSaid.push(u.text); setTimeout(() => u.onend && u.onend(), 0); }
  };
  w.SpeechSynthesisUtterance = function (t) { this.text = t; };

  /* URL.createObjectURL — Blob ke bytes pakar lo */
  let n = 0;
  w.URL.createObjectURL = function (blob) {
    const url = 'blob:fake/' + (++n);
    state.blobs.push({ url, type: blob.type, blob });
    return url;
  };
  w.URL.revokeObjectURL = function () {};

  /* Audio — src set hote hi bajta hai aur khatam ho jata hai */
  w.Audio = function () {
    const self = this;
    this.onended = null; this.onerror = null;
    this._src = '';
    Object.defineProperty(this, 'src', {
      get() { return self._src; },
      set(v) { self._src = v; state.played.push(v); }
    });
    this.pause = function () { self.paused = true; };
    this.play = function () {
      return { catch() {} , then() {} , _p: setTimeout(() => { if (!self.paused && self.onended) self.onended(); }, 1) };
    };
  };

  /* XMLHttpRequest */
  w.XMLHttpRequest = function () {
    const self = this;
    this.headers = {}; this.status = 0; this.responseText = '';
    this.open = (m, u) => { self.method = m; self.url = u; };
    this.setRequestHeader = (k, v) => { self.headers[k.toLowerCase()] = v; };
    this.abort = () => { self.aborted = true; clearTimeout(self._t); };
    this.send = function (body) {
      self.body = body;
      try { self.json = JSON.parse(body); } catch (e) { self.json = null; }
      state.calls.push(self);
      self._t = setTimeout(function () {
        if (self.aborted) return;
        const r = state.respond(self, state.calls.length);
        if (r.timeout) { self.ontimeout && self.ontimeout(); return; }
        if (r.neterr) { self.onerror && self.onerror(); return; }
        self.status = r.status;
        self.responseText = typeof r.body === 'string' ? r.body : JSON.stringify(r.body);
        self.onload && self.onload();
      }, 1);
    };
  };

  w.eval(ENGINE);
  return { w, state, AWAAZ: w.AWAAZ };
}

/* 24 kHz mono s16le ke 100 samples */
function pcmB64(samples = 100) {
  const buf = Buffer.alloc(samples * 2);
  for (let i = 0; i < samples; i++) buf.writeInt16LE((i * 300) % 30000 - 15000, i * 2);
  return buf.toString('base64');
}
function okBody(rate = 24000, b64 = pcmB64()) {
  return { candidates: [{ content: { parts: [{ inlineData: { mimeType: 'audio/L16;codec=pcm;rate=' + rate, data: b64 } }] } }] };
}
const wait = (ms = 12) => new Promise(r => setTimeout(r, ms));
const bytesOf = async (blob) => new Uint8Array(await blob.arrayBuffer());
const str = (b, o, n) => String.fromCharCode.apply(null, Array.from(b.slice(o, o + n)));
const u32 = (b, o) => b[o] | (b[o + 1] << 8) | (b[o + 2] << 16) | (b[o + 3] << 24);
const u16 = (b, o) => b[o] | (b[o + 1] << 8);

(async function run() {
  console.log('\n\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m');
  console.log('\x1b[1m  🎙️  AWAAZ ENGINE v6 — SABOOT\x1b[0m');
  console.log('\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m');

  /* ─── 1. WAV HEADER (asal bug) ─── */
  head('1. WAV HEADER — wahi bug jisne neural awaaz mardi thi');
  {
    const { AWAAZ } = makeWorld();
    const pcm = new Uint8Array(200);
    const wav = AWAAZ.wav(pcm, 24000, 1, 16);
    is(wav.length === 244, 'header 44 bytes + PCM', wav.length + ' bytes');
    is(str(wav, 0, 4) === 'RIFF', 'RIFF magic');
    is(str(wav, 8, 4) === 'WAVE', 'WAVE type');
    is(str(wav, 12, 4) === 'fmt ', 'fmt  chunk');
    is(str(wav, 36, 4) === 'data', 'data chunk');
    is(u32(wav, 4) === 236, 'RIFF size = 36 + data', String(u32(wav, 4)));
    is(u16(wav, 20) === 1, 'format = 1 (PCM)');
    is(u16(wav, 22) === 1, 'channels = 1 (mono)');
    is(u32(wav, 24) === 24000, 'sample rate 24000', String(u32(wav, 24)));
    is(u32(wav, 28) === 48000, 'byte rate = rate × 2', String(u32(wav, 28)));
    is(u16(wav, 32) === 2, 'block align = 2');
    is(u16(wav, 34) === 16, 'bits = 16');
    is(u32(wav, 40) === 200, 'data size = PCM length', String(u32(wav, 40)));
    is(AWAAZ.rateOf('audio/L16;codec=pcm;rate=16000') === 16000, 'rate mime se parhi jati hai (hard-code nahi)');
    is(AWAAZ.rateOf('audio/L16') === 24000, 'rate na ho to 24000 default');
    const w2 = AWAAZ.wav(new Uint8Array(10), 16000, 1, 16);
    is(u32(w2, 24) === 16000, '16 kHz par header bhi 16 kHz kehta hai');
  }

  /* ─── 2. GEMINI REQUEST ─── */
  head('2. GEMINI REQUEST — jo Google ko bheja jata hai');
  {
    const { AWAAZ, state } = makeWorld({ settings: { gVoice: 'Puck', voiceMood: 'cheerful' } });
    AWAAZ.speak('Salam Boss', {});
    await wait(20);
    const c = state.calls[0];
    is(!!c, 'ek request gayi', String(state.calls.length));
    is(c.method === 'POST', 'POST hai');
    is(/generativelanguage\.googleapis\.com\/v1beta\/models\/.+:generateContent$/.test(c.url), 'sahi endpoint', c.url.replace(/^.*models\//, 'models/'));
    is(c.headers['x-goog-api-key'] === 'KEY-123', 'key header mein hai (URL mein nahi)');
    is(c.url.indexOf('KEY-123') < 0, 'key URL mein leak nahi hoti');
    const g = c.json.generationConfig;
    is(JSON.stringify(g.responseModalities) === '["AUDIO"]', 'responseModalities = ["AUDIO"]');
    is(g.speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName === 'Puck', 'chuni hui awaaz jati hai', 'Puck');
    const txt = c.json.contents[0].parts[0].text;
    is(/cheerful/i.test(txt), 'mood prompt saath jata hai');
    is(txt.indexOf('Salam Boss') >= 0, 'asal matn bhi jata hai');
  }

  /* ─── 3. PCM → WAV → PLAY (end to end) ─── */
  head('3. END-TO-END — PCM aata hai, WAV bajta hai');
  {
    const { AWAAZ, state } = makeWorld();
    let done = false;
    AWAAZ.speak('Test jumla', { onDone: () => { done = true; } });
    await wait(30);
    is(state.blobs.length === 1, 'ek blob bana', String(state.blobs.length));
    is(state.blobs[0].type === 'audio/wav', 'blob ka type audio/wav hai (raw PCM nahi!)', state.blobs[0].type);
    const b = await bytesOf(state.blobs[0].blob);
    is(str(b, 0, 4) === 'RIFF' && str(b, 8, 4) === 'WAVE', 'blob ke andar sach much WAV hai');
    is(b.length === 44 + 200, 'blob = header + poora PCM', b.length + ' bytes');
    is(state.played.length === 1 && state.played[0] === state.blobs[0].url, 'wahi blob play hua');
    is(done, 'onDone chala');
    is(AWAAZ.engine === 'neural', 'engine = neural', AWAAZ.engine);
    is(AWAAZ.err === '', 'koi error nahi');
  }

  /* ─── 4. MOOD / PERSONA ─── */
  head('4. MOOD — persona ke mutabiq andaaz');
  {
    for (const [persona, want] of [['maya', 'warm'], ['friday', 'pro'], ['venom', 'funny']]) {
      const { AWAAZ } = makeWorld({ settings: { persona, voiceMood: 'auto' } });
      is(AWAAZ.moodId() === want, 'persona ' + persona + ' → mood ' + want, AWAAZ.moodId());
    }
    const { AWAAZ } = makeWorld({ settings: { persona: 'maya', voiceMood: 'whisper' } });
    is(AWAAZ.moodId() === 'whisper', 'user ka chuna mood persona ko harata hai');
    is(/whisper/i.test(AWAAZ.prompt('hello', 'whisper')), 'whisper ka prompt bharta hai');
    is(/Urdu/.test(AWAAZ.prompt('السلام علیکم', 'warm')), 'Urdu matn par zubaan ka ishara jata hai');
    is(/Hindi/.test(AWAAZ.prompt('नमस्ते', 'warm')), 'Hindi matn par zubaan ka ishara jata hai');
  }

  /* ─── 5. VOICE LIST ─── */
  head('5. AWAAZEIN — 30 prebuilt');
  {
    const { w, AWAAZ } = makeWorld();
    const V = w.GEMINI_VOICES;
    is(V.length === 30, '30 awaazein maujood', String(V.length));
    is(new Set(V.map(v => v.id)).size === 30, 'har id alag hai');
    is(V.every(v => v.label && (v.g === 'f' || v.g === 'm')), 'har awaaz ka label + gender hai');
    is(V.some(v => v.id === 'Kore') && V.some(v => v.id === 'Puck') && V.some(v => v.id === 'Charon'), 'jaani-pehchani awaazein shamil hain');
    const bad = makeWorld({ settings: { gVoice: 'KoiFakeAwaaz' } });
    is(bad.AWAAZ.voiceId() === 'Kore', 'ghalat awaaz par Kore par gir jata hai');
    is(w.VOICE_MOODS.length >= 8, '8+ mood', String(w.VOICE_MOODS.length));
  }

  /* ─── 6. CACHE ─── */
  head('6. CACHE — wahi jumla dobara = 0 request');
  {
    const { AWAAZ, state } = makeWorld();
    AWAAZ.speak('Bilkul wahi jumla', {});
    await wait(25);
    const first = state.calls.length;
    AWAAZ.speak('Bilkul wahi jumla', {});
    await wait(25);
    is(first === 1 && state.calls.length === 1, 'doosri dafa koi request nahi gayi', state.calls.length + ' calls');
    is(AWAAZ.cacheHits === 1, 'cache hit gina gaya', String(AWAAZ.cacheHits));
    is(state.played.length === 2, 'phir bhi dono dafa awaaz baji', String(state.played.length));
    AWAAZ.speak('Naya alag jumla', {});
    await wait(25);
    is(state.calls.length === 2, 'naye jumle par nayi request');
  }

  /* ─── 7. CHUNKING ─── */
  head('7. CHUNKING — lamba jawab tukron mein, bina khamoshi');
  {
    const { AWAAZ } = makeWorld();
    const long = ('Ye ek lamba jumla hai jo bar bar dohraya ja raha hai. ').repeat(30);
    const parts = AWAAZ.chunks(long, 420);
    is(parts.length > 1, 'lamba matn toota', parts.length + ' tukre');
    is(parts.every(p => p.length <= 420), 'har tukra hadd ke andar', 'max ' + Math.max(...parts.map(p => p.length)));
    is(parts.join(' ').replace(/\s+/g, ' ').trim() === long.replace(/\s+/g, ' ').trim(), 'ek lafz bhi zaya nahi hua');
    is(AWAAZ.chunks('Chhota jumla.', 420).length === 1, 'chhota matn nahi toota');
    is(AWAAZ.chunks('', 420).length === 0, 'khaali matn = 0 tukre');
    const one = AWAAZ.chunks('x'.repeat(1000), 420);
    is(one.every(p => p.length <= 420), 'bina space wala bara matn bhi tootta hai', one.length + ' tukre');
  }
  {
    const { AWAAZ, state } = makeWorld();
    /* har jumla alag rakho warna cache khud hi kaam kar jayega */
    let long = '';
    for (let i = 0; i < 15; i++) long += 'Ye jumla number ' + i + ' hai aur is mein kuch alag alfaz hain jaise ' + 'x'.repeat(20 + i) + '. ';
    AWAAZ.speak(long, {});
    await wait(120);
    const expect = AWAAZ.chunks(long, 420).length;
    is(expect >= 3, 'matn kai tukron mein toota', expect + ' tukre');
    is(state.played.length === expect, 'har tukra baja', state.played.length + '/' + expect);
    is(state.calls.length === expect, 'har tukre ki ek hi request (prefetch ne duplicate nahi banaya)', state.calls.length + ' calls');
    const urls = new Set(state.played);
    is(urls.size === expect, 'har tukre ka apna audio (dobara wahi clip nahi baji)', urls.size + ' unique');
  }

  /* ─── 8. ERROR TAXONOMY ─── */
  head('8. ERRORS — har nakami ka naam');
  {
    const cases = [
      [{ status: 429, body: {} }, 'QUOTA'],
      [{ status: 401, body: {} }, 'KEY_BAD'],
      [{ status: 403, body: {} }, 'KEY_BAD'],
      [{ status: 500, body: {} }, 'HTTP_500'],
      [{ timeout: true }, 'TIMEOUT'],
      [{ neterr: true }, 'NETWORK'],
      [{ status: 200, body: { candidates: [] } }, 'NO_AUDIO']
    ];
    for (const [resp, code] of cases) {
      const { AWAAZ, state } = makeWorld({ respond: () => resp });
      AWAAZ.speak('kuch bhi', {});
      await wait(40);
      is(AWAAZ.err === code, 'error code = ' + code, AWAAZ.err);
      is(state.deviceSaid.length === 1, '  → phone ki awaaz ne sambhal liya (khamoshi nahi)', AWAAZ.engine);
    }
  }

  /* ─── 9. COOLDOWN ─── */
  head('9. COOLDOWN — 429 ke baad spam band');
  {
    const { AWAAZ, state } = makeWorld({ respond: () => ({ status: 429, body: {} }) });
    AWAAZ.speak('pehli koshish', {});
    await wait(30);
    is(AWAAZ.cooldownUntil > Date.now(), 'cooldown lag gaya', Math.round((AWAAZ.cooldownUntil - Date.now()) / 1000) + 's');
    is(AWAAZ.blockReason('x') === 'QUOTA', 'cooldown ke dauran neural block hai');
    const before = state.calls.length;
    AWAAZ.speak('doosri koshish', {});
    await wait(30);
    is(state.calls.length === before, 'doosri dafa Google ko haath hi nahi lagaya', state.calls.length + ' calls');
    is(state.deviceSaid.length === 2, 'phir bhi dono dafa bola', String(state.deviceSaid.length));
    AWAAZ.cooldownUntil = 0;
    is(AWAAZ.blockReason('x') === '', 'cooldown khatam hote hi neural wapas');
  }

  /* ─── 10. KEY BAD ─── */
  head('10. KHARAB KEY — ek dafa seekha, baar baar nahi');
  {
    const { AWAAZ, state } = makeWorld({ respond: () => ({ status: 403, body: {} }) });
    AWAAZ.speak('a', {}); await wait(30);
    is(AWAAZ.keyBad === true, 'keyBad set hua');
    const before = state.calls.length;
    AWAAZ.speak('b', {}); await wait(30);
    is(state.calls.length === before, 'kharab key ke sath dobara try nahi kiya');
    is(AWAAZ.why('KEY_BAD').length > 5, 'user ke liye insani wajah maujood', AWAAZ.why('KEY_BAD'));
  }

  /* ─── 11. POLICY GATES ─── */
  head('11. POLICY — kab neural chalega, kab nahi');
  {
    const t = (settings, want, name, extra) => {
      const { AWAAZ } = makeWorld({ settings });
      if (extra) extra(AWAAZ);
      is(AWAAZ.blockReason('salam') === want, name, AWAAZ.blockReason('salam') || '(chalega)');
    };
    t({}, '', 'sab theek → neural');
    t({ apikey: '', ttsKey: '' }, 'KEY_MISSING', 'key nahi → KEY_MISSING');
    t({ voiceEngine: 'device' }, 'DEVICE_MODE', 'sirf-phone mode → DEVICE_MODE');
    t({ voiceEngine: 'off' }, 'OFF', 'band → OFF');
    t({ voiceOn: false }, 'OFF', 'voiceOn=false → OFF');
    t({ neuralMaxChars: 3 }, 'TOO_LONG', 'hadd se lamba → TOO_LONG');
    {
      const { w, AWAAZ } = makeWorld();
      Object.defineProperty(w.navigator, 'onLine', { value: false, configurable: true });
      is(AWAAZ.blockReason('x') === 'OFFLINE', 'internet nahi → OFFLINE');
    }
    {
      const { w, AWAAZ } = makeWorld({ settings: { neuralWifiOnly: true } });
      w.navigator.connection = { type: 'cellular' };
      is(AWAAZ.blockReason('x') === 'MOBILE_DATA', 'WiFi-only + mobile data → MOBILE_DATA');
      w.navigator.connection = { type: 'wifi' };
      is(AWAAZ.blockReason('x') === '', 'WiFi par chalta hai');
    }
    {
      const { AWAAZ } = makeWorld({ settings: { apikey: 'MAIN', ttsKey: 'ALAG-TTS' } });
      is(AWAAZ.cfg().key === 'ALAG-TTS', 'alag TTS key ko tarjeeh milti hai');
    }
  }

  /* ─── 12. FALLBACK CHAIN ─── */
  head('12. FALLBACK — khamoshi kabhi nahi');
  {
    const { AWAAZ, state } = makeWorld({ settings: { apikey: '', ttsKey: '' } });
    AWAAZ.speak('bina key ke', {});
    await wait(20);
    is(state.calls.length === 0, 'key na ho to Google ko call hi nahi jati');
    is(state.deviceSaid[0] === 'bina key ke', 'phone ki awaaz ne bola', state.deviceSaid[0]);
    is(AWAAZ.engine === 'device', 'engine = device');
  }
  {
    const { AWAAZ, state } = makeWorld({ settings: { voiceEngine: 'off' } });
    AWAAZ.speak('khamosh raho', {});
    await wait(20);
    is(state.calls.length === 0 && state.deviceSaid.length === 0, 'OFF par sach much khamoshi');
    is(AWAAZ.engine === 'off', 'engine = off');
  }
  {
    /* sirf-neural mode mein bhi nakami par bolna zaroori hai */
    const { AWAAZ, state } = makeWorld({ settings: { voiceEngine: 'neural' }, respond: () => ({ status: 500, body: {} }) });
    AWAAZ.speak('neural mode', {});
    await wait(40);
    is(state.deviceSaid.length === 1, 'sirf-neural mode bhi nakami par phone par girta hai');
  }

  /* ─── 13. MODEL AUTO-SWITCH ─── */
  head('13. MODEL — jo chale wahi yaad');
  {
    const { AWAAZ, state } = makeWorld({
      respond: (xhr, n) => (n === 1 ? { status: 404, body: {} } : { status: 200, body: okBody() })
    });
    AWAAZ.speak('model test', {});
    await wait(40);
    is(state.calls.length === 2, 'pehla model fail → doosra try hua', state.calls.length + ' calls');
    is(AWAAZ.model && state.calls[1].url.indexOf(AWAAZ.model) > 0, 'jo chala wo yaad rakha', AWAAZ.model);
    const before = state.calls.length;
    AWAAZ.speak('ek aur jumla', {});
    await wait(40);
    is(state.calls.length === before + 1, 'agli dafa seedha kaam wala model (koi zaya request nahi)');
  }

  /* ─── 14. STOP ─── */
  head('14. STOP — foran chup');
  {
    const { AWAAZ, state } = makeWorld();
    AWAAZ.speak('bohat lamba jawab jo beech mein roka jayega', {});
    AWAAZ.stop();                                   /* foran, jawab aane se pehle */
    await wait(40);
    is(state.calls[0].aborted === true, 'chalti request abort hui');
    is(state.played.length === 0, 'stop ke baad kuch nahi baja', String(state.played.length));
    is(AWAAZ.xhrs.length === 0, 'xhr list saaf hai');
  }
  {
    const { AWAAZ, state } = makeWorld();
    let done = 0;
    AWAAZ.speak('pehla', { onDone: () => done++ });
    AWAAZ.speak('doosra', { onDone: () => done++ });  /* purana foran mar jaye */
    await wait(40);
    is(done === 1, 'purana speak apne aap mar gaya (double callback nahi)', String(done));
    is(state.played.length === 1, 'sirf naya jumla baja');
  }

  /* ─── 15. STATUS ─── */
  head('15. STATUS — Doctor aur badge ke liye sach');
  {
    const { AWAAZ } = makeWorld();
    const st = AWAAZ.status();
    is(st.ready === true, 'ready = true jab sab theek');
    is(st.voice === 'Kore' && st.mood === 'warm', 'voice + mood report hote hain', st.voice + '/' + st.mood);
    is(typeof st.cache === 'number' && typeof st.cooldown === 'number', 'cache + cooldown ginte hain');
    const off = makeWorld({ settings: { apikey: '', ttsKey: '' } }).AWAAZ.status();
    is(off.ready === false && off.block === 'KEY_MISSING', 'key na ho to status sach bolta hai', off.why);
  }

  /* ─── 16. PURANE NAAM ─── */
  head('16. PURANI API — baqi app na toote');
  {
    const src = HTML;
    is(/function geminiTTS_stop\(\)\s*\{\s*AWAAZ\.stop\(\);\s*\}/.test(src), 'geminiTTS_stop() ab AWAAZ.stop() hai');
    is(src.indexOf('fish.audio') < 0 && src.indexOf('fishTTS_speak') < 0, 'fish.audio poori tarah nikal gaya (CORS error khatam)');
    is(src.indexOf('puterTTS_speak') < 0, 'puter.js ka murda reference khatam');
    is((src.match(/function testMayaVoice/g) || []).length === 1, 'testMayaVoice sirf ek dafa');
    is((src.match(/function speak\(text, wasVoice\)/g) || []).length === 1, 'speak() sirf ek dafa');
    is(/window\.__nativeTtsDone = function \(\) \{ try \{ if \(AWAAZ\.deviceDone\)/.test(src), 'Kotlin ka TTS-done callback engine se juda hai');
    is(/if \(e\.cancelable && e\.preventDefault\)/.test(src), 'touchmove par cancelable guard laga (console warning fix)');
    is(/data-say/.test(src) && /say-btn/.test(src), 'har jawab par 🔊 replay button maujood');
  }

  console.log('\n\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m');
  if (fail === 0) console.log('\x1b[1m\x1b[32m✅ SAB TEST PASS — ' + pass + '/' + pass + '\x1b[0m');
  else console.log('\x1b[1m\x1b[31m❌ ' + fail + ' TEST FAIL — ' + pass + '/' + (pass + fail) + ' pass\x1b[0m');
  console.log('\x1b[1m\x1b[35m══════════════════════════════════════════════════════════\x1b[0m\n');
  process.exit(fail === 0 ? 0 : 1);
})();
