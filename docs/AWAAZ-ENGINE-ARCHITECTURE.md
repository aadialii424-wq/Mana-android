# 🎙️ MAYA — AWAAZ ENGINE ARCHITECTURE (v4.2.0 "VOICE")

> Settings UI ki tarah: **pehle contract, phir code, phir machine se saboot.**

---

## 0. Pehle se kya toota hua tha (code se saboot)

| # | Masla | Saboot |
|---|---|---|
| 1 | **Gemini TTS kabhi chala hi nahi** | `geminiTTS_speak()` raw PCM ko seedha `new Blob(...)` mein daal kar `<audio>` ko de raha tha. Gemini `audio/L16;codec=pcm;rate=24000` deta hai — **WAV header ke baghair koi player ise nahi bajata**. Har dafa `onerror` → chup-chaap fallback |
| 2 | **fish.audio = red error** | `fetch("https://api.fish.audio/...")` seedha WebView se. Origin `appassets.androidplatform.net` → **CORS block**. Comment mein likha tha "bypasses CORS via MayaBridge" magar bridge use hi nahi hota tha |
| 3 | **`puterTTS_speak` defined hi nahi** | 2 jagah call hota hai (line 1576, 1585) → **ReferenceError** |
| 4 | **`testMayaVoice` do dafa** | `}function testMayaVoice() {` — doosra pehle ko kha jata hai |
| 5 | **4 engine, koi malik nahi** | fish → edge → gemini → native, har ek ka apna fallback, 3 jagah `var edgeVoice` dobara |
| 6 | **Nakami khamosh** | Fail hone par sirf `pushLog` — user ko kabhi pata nahi chalta ke awaaz kyun nahi aayi |

---

## 1. ASOOL (ye faisle pathar par likhe hain)

1. **Ek hi darwaza** — poori app mein awaaz ka sirf ek rasta: `AWAAZ.speak()`. Koi doosra TTS call nahi.
2. **Har nakami ka naam hota hai** — `KEY_MISSING`, `KEY_BAD`, `QUOTA`, `OFFLINE`, `TIMEOUT`, `MODEL`, `AUDIO_BLOCKED`, `TOO_LONG`. Chup-chaap fail **mana** hai.
3. **Fallback hamesha** — neural na chale to phone ki awaaz. **Khamoshi kabhi nahi** (jab tak user ne khud OFF na kiya ho).
4. **User ko sach dikhe** — badge hamesha batata hai abhi kaun bol raha hai: 🎭 NEURAL / 📱 PHONE / 🔇 OFF.
5. **Paisa/data user ka hai** — cache, chunking aur WiFi-only option; har clip dobara nahi mangwayi jati.
6. **ES5 + XHR** — `fetch`/`async` nahi, `XMLHttpRequest` (timeout + abort built-in, har WebView par chalta hai).

---

## 2. TIER SYSTEM

```
speak(text)
   │
   ├─ 0. GATE      voiceOn? text saaf? engine mode?
   │
   ├─ 1. 🎭 GEMINI TTS   ← asli maza (30 awaazein + mood)
   │      shart: key + online + cooldown nahi + (WiFi-only ? wifi : true)
   │      lamba text → sentence chunks, agla chunk pichle ke bajte waqt prefetch
   │      nakami → naam ke sath log + niche wale tier par
   │
   ├─ 2. 🌊 EDGE TTS     ← muft neural, koi key nahi (optional, default OFF)
   │
   ├─ 3. 📱 DEVICE TTS   ← MayaBridge.speak() ya speechSynthesis — 0 data, 0 latency
   │
   └─ 4. 🔇 SILENT       ← sirf tab jab user ne OFF kiya ho; UI mein wajah likhi hoti hai
```

---

## 3. GEMINI TTS — technical contract

```
POST https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent
     ?key=<wahi Gemini key>
body {
  contents: [{ parts: [{ text: "<MOOD PROMPT>: <matn>" }] }],
  generationConfig: {
    responseModalities: ["AUDIO"],
    speechConfig: { voiceConfig: { prebuiltVoiceConfig: { voiceName: "Kore" } } }
  }
}
```
- **MODEL**: `gemini-2.5-flash-preview-tts` (default) → fail par `gemini-3.1-flash-tts-preview` → jo chale wo yaad rakh lo (`settings.ttsModel`).
- **Jawab**: `candidates[0].content.parts[n].inlineData` = base64 **raw PCM**,
  mime `audio/L16;codec=pcm;rate=24000` → **s16le, 24000 Hz, mono**.
- **Lazmi qadam**: base64 → bytes → **44-byte WAV header lagao** → `Blob("audio/wav")` → `<audio>`.
  Sample rate mime se parho (hard-code nahi), warna awaaz teez/dheemi bajegi.

### Mood prompts (yahi "mazedar awaaz" ka raaz hai)
| Mood | Prompt |
|---|---|
| `auto` | persona se: Maya → warm dost, Friday → crisp professional, Venom → shararti |
| `warm` | "Say warmly and affectionately, like a close friend" |
| `cheerful` | "Say cheerfully with a bright smile in your voice" |
| `calm` | "Say calmly and softly, relaxed pace" |
| `pro` | "Say in a crisp, confident, professional tone" |
| `hype` | "Say with high energy and excitement" |
| `whisper` | "Whisper softly and gently" |
| `news` | "Read like a clear news anchor" |

Urdu/Hindi matn detect ho to prompt mein zubaan ka ishara bhi jata hai.

---

## 4. Data / quota policy (0-budget app ka ehtiram)

| Cheez | Qadam |
|---|---|
| **Cache** | aakhri 12 clips (key = model+voice+mood+text) — dobara wahi jumla = 0 bytes, 0 quota |
| **Chunking** | 450 harf ke tukre, sentence boundary par; agla tukra pichle ke bajte waqt prefetch (smooth, koi khamoshi nahi) |
| **Guard** | `neuralMaxChars` (default 1200) se zyada → seedha device TTS |
| **WiFi-only** | optional switch — mobile data par khud device TTS |
| **429** | 90 second cooldown, us dauran Gemini try hi nahi hota (error spam khatam) |
| **401/403** | key kharab — user ko ek dafa saaf batao, phir tab tak band jab tak key na badle |

---

## 5. UI contract (IRONCLAD kit ke andar)

Settings → **AWAAZ — VOICE STUDIO**:
1. `sVoiceEngine` — 🎭 Auto / 🎭 Sirf Neural / 📱 Sirf Phone / 🔇 Band
2. `sGVoice` — 30 Gemini awaazein (mard/aurat alag, Roman Urdu tafseel ke sath)
3. `sVoiceMood` — 8 mood
4. `sNeuralWifi` — sirf WiFi par neural (switch)
5. `testVoice` — bolta bhi hai **aur batata bhi hai** kaunsa engine chala
6. `awaazStatus` — live: engine • voice • mood • aakhri nakami ki wajah

Chat mein: har MAYA bubble par chhota **🔊** — wahi jawab neural awaaz mein dobara suno.
Chat header ka badge: **🎭 NEURAL / 📱 PHONE / 🔇 OFF**.

---

## 6. Kya NAHI karenge
- ❌ fish.audio (CORS, alag key, ek hi awaaz) — **poora nikal diya**
- ❌ puter.js ke murda references
- ❌ `fetch` / `async` naye code mein
- ❌ Gemini TTS ko **default** banana jab key hi na ho
- ❌ Koi bhi nakami bina naam ke

## 7. Saboot (tests)
`tools/test-voice-engine.js` — jsdom + naqli XHR:
WAV header bilkul theek (RIFF/fmt/data + 24000 Hz) • request body sahi (voiceName, responseModalities) •
mood prompt persona ke hisaab se • 429 → cooldown → agli dafa Gemini skip • cache hit par doosri request nahi •
lamba matn chunks mein • har error code ka sahi naam • fallback chain • `AWAAZ.stop()` sach mein rokta hai

---

## 8. STATUS — v4.2.0 mein kya bana (✅ ship ho gaya)

| Kaam | Kahan | Halat |
|---|---|---|
| PCM → WAV header (44 byte, rate mime se) | `AWAAZ.wav()` / `AWAAZ.rateOf()` | ✅ |
| Ek darwaza `AWAAZ.speak()` + 3 tier fallback | `public/index.html` | ✅ |
| 30 neural awaazein + 9 mood + persona auto-mood | `GEMINI_VOICES` / `VOICE_MOODS` / `PERSONA_MOOD` | ✅ |
| Model auto-switch + yaad rakhna | `AWAAZ.modelOrder()` | ✅ |
| Error taxonomy + insani wajah | `AWAAZ.WHY` / `AWAAZ.note()` | ✅ |
| 429 → 90s cooldown, 401/403 → keyBad | `AWAAZ.fetchClip()` | ✅ |
| Cache (12 clips, LRU) + in-flight de-dupe | `AWAAZ.cacheGet/cachePut/flight` | ✅ |
| Chunking + prefetch (lambe jawab bina khamoshi) | `AWAAZ.chunks()` / `AWAAZ.neural()` | ✅ |
| WiFi-only + alag TTS key + engine mode | Settings → AWAAZ | ✅ |
| Har jawab par 🔊 replay | `addBubble()` + `.say-btn` | ✅ |
| Live badge + Doctor report | `awaazBadge()` / `paintAwaaz()` / `AWAAZ.status()` | ✅ |
| fish.audio + puter.js ke murda raste | **nikal diye** | ✅ |
| `touchmove` cancelable guard (I2) | UI KIT slider | ✅ |
| Raw console error banner → khamosh nishan + Doctor | `window.__consoleErr` | ✅ |

### Saboot
```
npm test
  ✅ CSS CHECK PASS            (old-WebView simulation)
  ✅ SAB TEST PASS — 45/45     (tools/test-settings-ui.js — 10 mein AWAAZ studio)
  ✅ SAB TEST PASS — 118/118   (tools/test-voice-engine.js)
```

### User ke liye 3 qadam
1. Settings → **API KEYS** → Gemini key daalo (chaho to AWAAZ ke liye alag key — alag quota).
2. Settings → **AWAAZ** → engine `Auto`, apni awaaz aur mood chuno → **SAVE**.
3. **🎧 TEST AWAAZ SUNO** — niche likh kar aayega ke kis engine ne bola.

Agar phone ki awaaz aaye to status line **wajah** batati hai (key, quota, internet, WiFi-only) — ab andaza lagane ki zaroorat nahi.
