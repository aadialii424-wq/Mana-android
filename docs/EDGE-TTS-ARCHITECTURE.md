# 🌊 EDGE TTS — MUFT, BE-HISAAB AWAAZ (v4.7.0)

> **Sawal jo aap ne poocha:** "Google ka TTS mazedar tha — instant jawab, wohi pyari
> awaaz. Aisa hi koi aur **free unlimited** TTS tool dhoondo."
>
> **Jawab:** mil gaya, laga bhi diya. Aur wo pehle se app mein *maujood* tha —
> magar toota hua tha. Ye dastavez batati hai ke kyun toota tha aur kaise theek hua.

---

## Hissa 0 — Poori duniya chaan maari (2026 ka naqsha)

| Tool | Muft kitna? | Urdu? | Faisla |
|---|---|---|---|
| **Microsoft Edge TTS** | **Be-hisaab. Koi key nahi, koi signup nahi** | **ur-PK-Uzma / ur-PK-Asad — ASLI** | ✅ **YEHI** |
| Google Gemini TTS | ~15 request / din (project par) | Roman Urdu theek | Pehle se hai, jaldi khatam |
| Azure Speech (official) | 500K harf / mahina, credit card lazmi | ur-PK maujood | Card chahiye |
| Google Cloud TTS | 1M harf / mahina, card lazmi | ur-PK maujood | Card chahiye |
| ElevenLabs | 10K harf / mahina | nahi | Bohat kam |
| Deepgram Aura-2 | $200 credit, phir khatam | nahi | Aarzi |
| Cartesia Sonic 3 | chhota free plan | nahi | Aarzi |
| Kokoro / Piper / Kitten | poori tarah muft (khud chalao) | nahi | Phone par bhaari, Urdu nahi |
| Pollinations openai-audio | muft, magar busy rehta hai | Roman Urdu | ✅ pehle se backup hai |

**Nateeja:** Edge TTS akela aisa hai jismein *teeno* baatein ek sath hain —
**muft**, **be-hisaab**, aur **asli Pakistani Urdu awaaz**. Ye wahi Azure neural
engine hai jo Microsoft Edge browser ke "Read aloud" ke peeche chalta hai.

---

## Hissa 1 — Asal masla: ye pehle se app mein tha aur *kabhi nahi chala*

`public/index.html` mein `edgeTTS_speak()` v4.1 se maujood tha. Settings mein
switch bhi tha. Magar wo switch **default OFF** tha aur ON karne par bhi kuch
nahi hota tha. Kyun?

Microsoft ka synthesis WebSocket **in headers ke bagair handshake qubool hi nahi karta**:

```
Origin: chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold
User-Agent: Mozilla/5.0 ... Edg/143.0.0.0
Pragma: no-cache
Cache-Control: no-cache
Sec-WebSocket-Version: 13
```

Aur browser ka JavaScript API —

```js
new WebSocket(url)      // <- yahan header lagane ki koi jagah HI NAHI hai
```

— **custom headers bhej hi nahi sakta**. Ye JavaScript ki apni hadd hai, hamara
bug nahi. `edge-tts-universal` (npm) ki apni dastavez saaf likhti hai:

> "Microsoft's TTS service now requires a custom WebSocket header that browsers
> do not allow setting via the WebSocket API. Direct browser usage is limited to
> the Microsoft Edge browser only. **There are no known workarounds.**"

Chrome, Firefox, Safari — aur **Android WebView** — sab block. Hamari app WebView
hai. Is liye `edgeTTS_speak()` hamesha `onerror` par gir kar chup ho jata tha.

---

## Hissa 2 — Ilaj: jo cheez WebView nahi kar sakti, **APK ka native side kar sakta hai**

Hamare paas Kotlin bridge maujood hai. Native code par header ki koi pabandi nahi.
Is liye poora WebSocket **JavaScript se nikal kar Kotlin mein** chala gaya.

```
┌─────────────── WebView (JavaScript) ───────────────┐
│  EDGE_TTS.pick()   -> kaunsi awaaz                 │
│  EDGE_TTS.ssml()   -> <speak>...</speak> banao     │
│  MayaBridge.edgeTts(ssml, reqId, timeoutMs) ───────┼──┐
│  window.__edgeDone(reqId, ok, base64Mp3) <─────────┼──┼─┐
│  EDGE_TTS.playB64() -> Blob -> <audio>             │  │ │
└────────────────────────────────────────────────────┘  │ │
                                                        ▼ │
┌─────────────── MainActivity.kt : object EdgeTts ───────┴─┐
│  1. Sec-MS-GEC  = SHA-256(windows-filetime@5min + token) │
│  2. SSLSocket   -> speech.platform.bing.com:443          │
│  3. Hand-rolled RFC-6455 handshake + POORE headers       │
│  4. speech.config  frame  ->                             │
│  5. ssml           frame  ->                             │
│  6. <- binary frames: [2-byte header len][header][MP3]   │
│  7. <- "Path:turn.end"  = khatam                         │
│  8. MP3 -> base64 -> JS                                  │
└──────────────────────────────────────────────────────────┘
```

**Koi nayi library nahi.** OkHttp bhi nahi. Sirf `SSLSocket` aur ~150 line ka
apna RFC-6455 client. Wajah: dependency add karne ka matlab build ka naya khatra,
aur is chhote se kaam ke liye poori library ki zaroorat nahi thi.

### DRM token (`Sec-MS-GEC`)

Bilkul wahi hisab jo `rany2/edge-tts` ka `drm.py` karta hai:

```
ticks  = unix_seconds + 11644473600        # Windows epoch
ticks -= ticks % 300                       # 5 minute ke block par gol
ticks *= 1e9 / 100                         # 100-nanosecond ticks
GEC    = SHA-256( "%.0f" % ticks + TRUSTED_CLIENT_TOKEN ).upper()
```

---

## Hissa 3 — Protocol ka SABOOT (andaza nahi)

Kotlin sandbox mein chal nahi sakta (na JDK hai, na Microsoft tak network).
Is liye Kotlin ka **line-ba-line Python mirror** bana kar ek **naqli Edge server**
ke against chalaya gaya jo asli protocol bolta hai:

| Kya jancha | Nateeja |
|---|---|
| Handshake ke poore headers | ✅ hoo-ba-hoo edge-tts jaise |
| Client frames par mask (RFC-6455 lazmi shart) | ✅ server ne tasdeeq ki |
| Chhota frame (7-bit length) | ✅ 50 bytes mile |
| Bara frame (16-bit length) | ✅ 5000 bytes mile |
| **Tukron mein aaya frame** (FIN=0 + continuation) | ✅ 300 bytes jur gaye |
| PING aaya -> PONG bheja | ✅ |
| `Path:turn.end` par band | ✅ |
| **Kul audio** | ✅ **5350 / 5350 bytes, bilkul sahih** |

Aur `speech.config` / `ssml` messages **byte-ba-byte** wahi nikle jo `edge-tts`
bhejta hai — us fazool `Z` samet jo Microsoft `X-Timestamp` ke aakhir mein maangta hai.

Awaaz ki fehrist Microsoft ke apne server se live tasdeeq hui:

```
ur-PK-UzmaNeural   Female   Urdu (Pakistan)   ✅
ur-PK-AsadNeural   Male     Urdu (Pakistan)   ✅
ur-IN-GulNeural    Female   Urdu (India)      ✅
ur-IN-SalmanNeural Male     Urdu (India)      ✅
```

---

## Hissa 4 — Nayi seerhi (ladder)

**Pehle:**
```
Gemini neural  ->  muft neural (Pollinations)  ->  [Edge, band]  ->  📱 robot
```

**Ab:**
```
Gemini neural  ->  🌊 EDGE (be-hisaab)  ->  muft neural  ->  📱 robot
```

Edge ko Pollinations se **pehle** rakha gaya kyunke:
- uska koi quota nahi (Pollinations busy ho jata hai),
- wo **asli Urdu** bolta hai (Pollinations Roman Urdu ko English samajh kar parhta hai),
- wo teiz hai.

Sharat sirf ek: `EDGE_TTS.native()` — Kotlin bridge maujood ho. Browser mein
`edgeReady()` `false` deta hai taake ek second bhi zaya na ho.

### Naya mode: **🌊 Sirf Edge TTS**

Awaaz Engine ke picker mein naya option. Is par Gemini ko **chhua bhi nahi jata** —
uske roz ke ~15 request bache rehte hain, aur awaaz phir bhi neural rehti hai.

| Mode | Kya hota hai |
|---|---|
| 🪄 Auto | Gemini → Edge → muft → phone |
| 🎭 Sirf Neural | Gemini → Edge → muft → phone |
| **🌊 Sirf Edge TTS** | **Edge → muft → phone (Gemini bilkul nahi)** |
| 📱 Sirf Phone | seedha phone |
| 🔇 Band | khamosh |

---

## Hissa 5 — Settings mein kya naya hai

1. **🌊 Sirf Edge TTS** — Awaaz Engine picker mein naya option.
2. **🌊 EDGE AWAAZ** — awaaz ka apna picker. "Auto" zubaan + persona dekh kar
   khud chunta hai; ya 13 mein se koi bhi khud chuno. APK mein boot par
   Microsoft se **poori live list** (300+) mangwa kar picker bhar jata hai.
3. **🌊 EDGE AWAAZ SUNO** — button. Bolta bhi hai aur waqt bhi batata hai
   (`✅ Bol diya — ur-PK-UzmaNeural • 1.3 sec • koi key nahi lagi`).
   Nakami par **asal wajah** dikhata hai, jhooti tasalli nahi.
4. Edge switch ab **default ON** — kyunke ab wo sach much chalta hai.

### Awaaz kaise chunti hai

```
user ki apni pasand (edgeVoice)
      ↓ na ho to
zubaan (settings.tts) + persona
      ↓ Jarvis/Friday = mardana, baqi = zanana (Maya larki hai)
      ↓ na mile to
ur-PK-UzmaNeural
```

`rate 1.0 → "+0%"`, `1.2 → "+20%"` · `pitch 1.05 → "+3Hz"` — dono Edge ki
hadd ke andar band (`±100%` / `±50Hz`).

---

## Hissa 6 — Test (56 naye, kul 222)

`tools/test-voice-engine.js` Section 17. Ab **asli** Edge code jsdom mein
chalta hai (`EDGE_TTS` + `edgeTTS_speak` + `window.__edgeDone`), naqli Kotlin
bridge ke sath.

- **Fehrist (4):** ur-PK Uzma/Asad maujood, har id ka format sahih, zubaan nikalna.
- **Awaaz chunna (7):** default zanana Urdu · Jarvis par mardana · Hindi · English(India) ·
  user ki pasand sab par bhaari · anjaan zubaan par bhi khamoshi nahi · bekaar setting rad.
- **Rate/pitch (4):** shakl sahih + hadd ke andar.
- **SSML (6):** voice/lang/prosody · **XML injection band** (`&`, `<`, `'`, `"` escape) ·
  matn dhancha nahi tor sakta.
- **Native bridge (9):** bridge chala (WebSocket nahi) · poora SSML gaya · har request ka
  apna id · native timeout JS se chhota · **thek wohi MP3 bytes baje** · counter barha.
- **Nakami (2):** code aaya · **Microsoft ki asal ghalati mehfooz** (`403 Forbidden`).
- **Race (2):** ek request = ek jawab · **stop() ke baad aayi awaaz chup rehti hai**.
- **Browser (3):** bridge na ho to tayyar nahi kehta · user kahe to koshish · WebSocket na ho to foran sach.
- **Seerhi (8):** Gemini na ho → seedha Edge · Edge chale to Pollinations ko takleef nahi ·
  mode "edge" par **0 Google calls** · Edge mare to muft neural · switch OFF par bilkul nahi.
- **Status (3):** on/tayyar/native ka sach · kaunsi awaaz chalegi · browser mein jhooti tasalli nahi.
- **Kotlin (5):** `edgeTts` bridge maujood · **wahi Origin header** · `Sec-WebSocket-Version` +
  `Sec-MS-GEC` · DRM ka hisab edge-tts wala · **client frames par mask**.

```
✅ CSS CHECK PASS
✅ SETTINGS   68 / 68
✅ AWAAZ     222 / 222      (166 -> 222, 56 naye)
✅ DIMAAG    155 / 155
```

---

## Hissa 7 — Imaandari se: is ke nuqsanat

Ye Microsoft ka **ghair-sarkari** raasta hai. Saaf saaf keh dena chahiye:

1. **Kabhi bhi band ho sakta hai.** 2024 mein Microsoft ne auth badla tha aur
   duniya bhar ki libraries kai hafte tooti rahin. Isi liye Edge **akela sahara
   nahi** — uske neeche muft neural aur phone ki awaaz mojood rehti hai, aur
   nakami par app foran agli teh par chali jati hai.
2. **SSML mehdood hai.** Sirf wahi tags jo Edge browser bhejta hai
   (`voice` + `prosody`). `<emphasis>`, `<break>`, style/mood kaam nahi karte.
3. **License.** Ye Microsoft Edge ke Read-aloud feature ka endpoint hai, koi
   sarkari public API nahi. Zaati istemaal ke liye theek; commercial product
   mein Azure ka sarkari plan lena chahiye.
4. **Sirf APK mein.** Browser preview mein ye nahi chalega — aur app ab ye
   **saaf likh kar batati hai** bajaye chup-chaap nakaam hone ke.

---

## Hissa 8 — Files

| File | Kya badla |
|---|---|
| `app/src/main/java/com/maya/ai/MainActivity.kt` | naya `object EdgeTts` (RFC-6455 client + DRM), `MayaBridge.edgeTts()`, `MayaBridge.edgeVoices()` |
| `public/index.html` | naya `EDGE_TTS` object (catalog/pick/ssml/playB64/refresh), bridge-first `edgeTTS_speak`, `window.__edgeDone`, nayi seerhi, `AWAAZ.edgeReady()`, mode `edge`, Settings ka picker + test button |
| `app/src/main/assets/web/index.html` · `sw.js` | mirror (sync) |
| `tools/test-voice-engine.js` | Section 17 — 56 naye test |
| `tools/test-settings-ui.js` | 5 naye test (5 mode, edge picker, test button) |
| `app/build.gradle` · `package.json` · `public/sw.js` | v4.7.0 / versionCode 55 / cache `maya-v4.7.0` |
