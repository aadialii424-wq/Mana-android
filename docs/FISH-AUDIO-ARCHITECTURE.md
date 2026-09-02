# 🐟 FISH AUDIO S2.1 Pro — BE-HISAAB AWAAZ + *ANDAAZ* WAPAS (v4.8.0)

> **Aap ka sawal tha:** *"Edge TTS mein woh sound hai jo Gemini key se Maya ke liye use ki thi, jo mujhe pasand aayi thi — to woh yahan hoga?"*
>
> **Sach:** nahi. Aur wajah sirf awaaz nahi thi — **ANDAAZ** tha. Ye dastavez batati
> hai ke wo andaaz kaise mara, aur Fish Audio use kaise wapas laata hai.

---

## Hissa 0 — Asal masla: awaaz nahi, **ANDAAZ** mara tha

Gemini ki awaaz "pyari" kyun lagti thi? Sirf awaaz ki wajah se nahi. App har
jumle ke sath Gemini ko **lafzon mein hidayat** bhejti hai:

```js
prompt: "Say this warmly and affectionately, like a close friend who cares.
         Speak natural Urdu. Text: " + text
```

Gemini us hidayat par **amal karta hai**. App ke 9 mood isi tarah kaam karte hain:

| Mood | Gemini ko jane wali hidayat |
|---|---|
| 🤗 Warm | *"Say this warmly and affectionately, like a close friend who cares"* |
| 🤫 Whisper | *"Whisper this softly and gently"* |
| 🔥 Hype | *"Say this with high energy and excitement"* |
| 🎭 Funny | *"Say this playfully with a mischievous, teasing tone"* |

**Edge TTS ye kar hi nahi sakta.** Microsoft ne uska SSML sirf `voice` + `prosody`
(pitch/rate) tak mehdood kar rakha hai. Yani v4.7.0 mein Edge ne robot ki jagah
to le li, magar **aap ke saare 9 mood us par bekaar** ho gaye.

---

## Hissa 1 — Fish us andaaz ko WAPAS laata hai

Fish ki apni dastavez (`models-overview`):

> *"S2.1-Pro treats `[bracket]` tags as standard text rather than dedicated control
> tokens... You can use **any descriptive expression** and the model will interpret
> it, such as `[whispers sweetly]` or `[laughing nervously]`. Cues can be placed
> **anywhere** in your text."*

To hum ne seedha pul bana diya — `FISH.BRACKET`:

| Mood | Gemini ko jata tha | 🐟 Fish ko jata hai |
|---|---|---|
| 🤗 warm | "Say this warmly and affectionately, like a close friend" | `[warmly, affectionately, like a close friend]` |
| 😄 cheerful | "cheerfully, with a bright smile" | `[cheerful, bright, smiling]` |
| 😌 calm | "calmly and softly, relaxed pace" | `[calm, soft tone, relaxed pace]` |
| 💼 pro | "crisp, confident, professional" | `[crisp, confident, professional]` |
| 🔥 hype | "high energy and excitement" | `[excited, high energy]` |
| 🤫 whisper | "Whisper this softly and gently" | `[whispers softly and gently]` |
| 📰 news | "like a professional news anchor" | `[clear news anchor tone]` |
| 🎭 funny | "playfully, mischievous, teasing" | `[playful, teasing, amused]` |

**Ek test is pul ko taala laga kar band rakhta hai:** wo `index.html` se
`VOICE_MOODS` khud parhta hai aur zid karta hai ke **har** mood ka Fish ishara
maujood ho. Naya mood banao aur Fish ka ishara bhoolo → **test fail**.

---

## Hissa 2 — Do purane zakhm, dono bhar gaye

### Zakhm 1 — CORS (isi liye fish.audio pehle **nikala** gaya tha)

Test file mein aaj tak ye line thi:
```js
is(src.indexOf('fish.audio') < 0, 'fish.audio poori tarah nikal gaya (CORS error khatam)');
```
Browser `api.fish.audio` ko seedha call nahi kar sakta. **Bilkul wahi masla jo
Edge ka tha** — aur hal bhi wahi: **Kotlin bridge CORS ke tabey nahi hota.**
Wo test ab sach ke mutabiq badal diya gaya:
```js
is(src.indexOf('fishTTS_speak') < 0, 'purana toota hua fishTTS_speak khatam');
is(src.indexOf('MayaBridge.httpBytes') > 0, 'fish.audio ab WAPAS hai — magar sirf native bridge se');
```

### Zakhm 2 — 🔴 BINARY (ye chhupa hua qatil tha)

Purana bridge:
```kotlin
txt = conn.inputStream.bufferedReader().use { it.readText() }      // <- MATN
b64 = Base64.encodeToString(txt.toByteArray(Charsets.UTF_8), NO_WRAP)
```
Matn ke liye theek. **MP3 par tabahi** — har ghair-UTF8 byte `U+FFFD` ban jata
hai, aur audio kachra ho jati hai. Ye bug aisa hai jo "chal to raha hai" lagta
hai magar awaaz kabhi nahi aati.

**Naya `httpBytes()`** raw bytes uthata hai, chhuta nahi:
```kotlin
val bos = ByteArrayOutputStream()
stream.use { s -> while (true) { val n = s.read(buf); if (n < 0) break; bos.write(buf, 0, n) } }
b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)      // <- RAW
```
Saath hi **custom headers** bhi — Fish ko `model: s2.1-pro-free` chahiye, jo
purana bridge bhej hi nahi sakta tha (wo sirf `Authorization` janta tha).

**Test isay pakar kar rakhta hai:** `httpBytes` ke andar lafz `bufferedReader`
maujood **nahi** hona chahiye — aur `httpPostAsync` (matn wala) jyun ka tyun rahe.

---

## Hissa 3 — Dhancha

```
┌──────────── WebView (JS) ────────────┐
│ FISH.styled(text, mood)              │  "[warmly...] Assalam o alaikum"
│ FISH.body()  -> JSON                 │  { text, reference_id, format:mp3, prosody }
│ FISH.headers() -> model: s2.1-pro-free
│ MayaBridge.httpBytes(POST,...) ──────┼──┐
│ window.__binDone(id,status,b64,...) <┼──┼─┐
│ FISH.play(b64) -> Blob -> <audio>    │  │ │
└──────────────────────────────────────┘  │ │
                                          ▼ │
┌─── MainActivity.kt : httpBytes() ────────┴─┐
│ custom headers (JSON se)                   │
│ raw bytes -> base64 (koi text-decode nahi) │
│ ghalati bhi bytes mein (JSON) — ek hi raah │
└────────────────────────────────────────────┘
```

**Request:**
```
POST https://api.fish.audio/v1/tts
Authorization: Bearer <key>
Content-Type: application/json
model: s2.1-pro-free

{ "text":"[warmly, affectionately...] Assalam o alaikum",
  "reference_id":"<awaaz>", "format":"mp3", "mp3_bitrate":128,
  "latency":"balanced", "prosody":{ "speed":1.0, "volume":0 } }
```
Zubaan **khud pehchani** jati hai — koi `lang` parameter nahi. Urdu, Hindi,
Roman Urdu, English — sab ek hi call.

---

## Hissa 4 — Nayi seerhi

```
🐟 FISH      be-hisaab · ~70ms · MOOD zinda      (sirf jab key ho)
   ↓ nakaam?
🎭 GEMINI    aap ki pasandeeda awaaz             (~15/din)
   ↓
🌊 EDGE      muft, be-hisaab, asli Urdu          (koi key nahi)
   ↓
🌸 muft neural   →   📱 phone
```

**Fish Gemini se bhi upar kyun?** Kyunke Gemini ka roz ka quota ~15 hai aur Fish
ka koi nahi — aur dono andaaz samajhte hain. Key na ho to ye teh **khud ko skip**
kar deti hai aur purana raasta jyun ka tyun rehta hai *(iska apna test hai)*.

| Mode | Kya hota hai |
|---|---|
| 🪄 Auto | Fish → Gemini → Edge → muft → phone |
| **🐟 Sirf Fish Audio** | **Fish → Edge → muft → phone (Gemini bilkul nahi)** |
| 🌊 Sirf Edge TTS | Edge → muft → phone |
| 🎭 Sirf Neural / 📱 Phone / 🔇 Band | pehle jaisa |

---

## Hissa 5 — 🩺 FISH DOCTOR (aur wo 31 August wala dar)

Fish ke blog par likha hai *"free access through **August 31, 2026**"* — jo
**guzar chuka**. Magar official docs (aaj tak) `s2.1-pro-free` ko **$0.00** hi
dikhati hai. Unhone pehle **do dafa** barhaya hai. **Yaqeen sirf ek asli key deti hai.**

Is liye DOCTOR andaza nahi lagata — Fish se **khud poochta** hai:

| HTTP | Verdict | Doctor kya kehta hai |
|---|---|---|
| **200 + audio** | `OK` | ✅ **ZINDA HAI** — muft window khuli hai, koi rozana hadd nahi |
| **402** | `PAYMENT` | 🚨 **Muft daur BAND** ya balance khatam — *magar Edge zinda hai, ghabrao mat* |
| **401** | `KEY_BAD` | Key qubool nahi — nayi banao |
| **403** | `FORBIDDEN` | Is key ko ijazat nahi |
| **429** | `RATE` | Fair Use ki hadd — thori der baad khud chalegi |
| **503** | `BUSY` | Fish ka server busy — aap ka qusoor nahi |
| **0** | `NETWORK` | Pohanch hi nahi hui |
| — | `BROWSER` | Sirf APK mein chalta hai (CORS) |
| — | `KEY_MISSING` | Key dali hi nahi — kahan se banani hai, wo bhi batata hai |

Har soorat mein Fish ka **apna message** bhi dikhta hai. `402`/`429` par Fish
**5 minute so jati hai** (baar baar deewar se sar nahi maarti) — aur ye neend
`localStorage` mein mehfooz rehti hai.

---

## Hissa 6 — Settings mein kya naya hai

1. **🐟 Fish Audio switch** — default ON (magar key ke bagair kuch nahi karta)
2. **🐟 FISH API KEY** — `fish.audio/app/api-keys` (muft, card nahi lagta)
3. **🐟 FISH AWAAZ** — awaaz ka picker
4. **📚 AWAAZ LIBRARY LAO** — Fish ki public library seedha app mein (naam, zubaan, ❤ likes)
5. **🐟 FISH AWAAZ SUNO** — bolti hai aur waqt batati hai; nakami par **asal wajah**
6. **🩺 FISH CHECK KARO** — upar wala Doctor
7. Engine picker mein naya **🐟 Sirf Fish Audio** mode

---

## Hissa 7 — Test (71 naye, kul 293)

`tools/test-voice-engine.js` Section 18 — asli `FISH` code jsdom mein, naqli
Kotlin bridge ke sath.

- **Mood (6):** har mood ka ishara maujood *(VOICE_MOODS se khud parh kar)*, ishara matn se pehle, mood na ho to matn saaf
- **Request (10):** endpoint, **`model: s2.1-pro-free` header**, Bearer, mp3, reference_id (aur khaali ho to bheja hi na jaye), speed clamp
- **Binary (5):** blob `audio/mpeg`, **exact bytes jyun ke tyun** (`0xFF 0xFB … 0x42`), counter, raftaar
- **Nakami (14):** 401/402/403/429/503/0/200-khaali ka poora naqsha, cooldown, Fish ka apna message, non-JSON, khaali jawab par crash nahi
- **Pehredaar (5):** key nahi → **ek byte nahi jata**, browser → CORS ka sach, switch OFF, cooldown, cooldown persist
- **Seerhi (11):** Fish **sab se pehle** · **0 Google calls** · Fish mare → Gemini → Edge → muft · **key na ho to purana raasta bilkul waisa hi** · mode "fish" · stop() ke baad khamoshi
- **Doctor (8):** KEY_MISSING · BROWSER · **OK (ZINDA)** · **PAYMENT (402 + 31 Aug)** · KEY_BAD + Fish ka message · RATE
- **Library (5):** GET /model, parsing, kharab entry chhanti, nakami par sach
- **Purana raasta (7):** `fishTTS_speak` khatam · **har request native check karta hai** · koi seedha XHR nahi · Kotlin `httpBytes` maujood · **`bufferedReader` us mein hai hi nahi** · custom headers · `httpPostAsync` mein koi regression nahi

```
✅ CSS CHECK PASS
✅ SETTINGS    72 / 72     (68 -> 72)
✅ AWAAZ      293 / 293    (222 -> 293, 71 naye)
✅ DIMAAG     155 / 155
```

---

## Hissa 8 — Imaandari se: nuqsanat

1. **Muft daur ki koi zamanat nahi.** Blog "31 Aug 2026" kehta hai, docs "$0.00".
   Isi liye Fish **akela sahara nahi** — 402 aate hi seerhi khud Edge par gir
   jati hai aur Doctor saaf likh deta hai ke kya hua.
2. **Aap ke jumle Fish ke paas ruk sakte hain** — *"requests may be retained for
   model improvement."* Maya ki baat-cheet unke server par jayegi. Settings mein
   ye baat **saaf likhi hui** hai. Hassas baat ke liye 🌊 Edge ya 📱 phone chuno.
3. **SLA nahi.** ~70ms unka daawa hai, zamanat nahi.
4. **Commercial:** *"Products generating more than $1M ARR should contact us."*
   Maya zaati app hai → koi masla nahi.
5. **Sirf APK mein.** Browser mein CORS rokta hai — aur app ye **saaf batati hai**
   bajaye chup-chaap nakaam hone ke.
6. **Key chahiye** (Edge ke bar-aks). Muft hai, card nahi lagta.

---

## Hissa 9 — Files

| File | Kya badla |
|---|---|
| `MainActivity.kt` | naya `MayaBridge.httpBytes()` — raw bytes + custom headers (async) |
| `public/index.html` | naya `FISH` object (BRACKET/body/headers/play/doctor/library), `window.__binDone`, `AWAAZ.fish` + `fishReady`, nayi seerhi, mode `fish`, badge, Settings ka poora panel |
| `app/src/main/assets/web/*` | mirror (sync) |
| `tools/test-voice-engine.js` | Section 18 — 71 naye test; purana "fish nikal gaya" wala test sach ke mutabiq badla |
| `tools/test-settings-ui.js` | 4 naye test (6 mode, Fish key/switch, picker, teeno button) |
| `app/build.gradle` · `package.json` · `public/sw.js` | v4.8.0 / versionCode 56 / cache `maya-v4.8.0` |
