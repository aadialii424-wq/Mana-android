# 🤖 MAYA — Personal AI Assistant

**Version 4.7.0 "EDGE TTS" — 0-Budget Build • Android APK + Web PWA**

MAYA = aap ka apna JARVIS — voice controlled AI assistant (Gemini brain),
ab **asli Android app** (APK) ki soorat mein, native superpowers ke saath:

## 🌊 v4.7.0 — EDGE TTS ("koi aur free unlimited TTS dhoondo")

**Mil gaya — aur wo pehle se app mein maujood tha, magar toota hua.**

Microsoft Edge ka "Read aloud" engine = **Azure ki 300+ neural awaazein, koi API key
nahi, koi signup nahi, koi rozana hadd nahi** — aur us mein **ASLI Pakistani Urdu**
awaazein hain: `ur-PK-UzmaNeural` aur `ur-PK-AsadNeural`.

**Ye pehle kyun nahi chalta tha?** Microsoft ka WebSocket `Origin` / `User-Agent` /
`Pragma` headers ke bagair handshake qubool nahi karta — aur browser ka
`new WebSocket()` API custom headers bhej **hi nahi sakta**. Ye JavaScript ki hadd hai.
Chrome, Firefox, **Android WebView** — sab block. Is liye purana Edge code hamesha
khamoshi se mar jata tha (switch default OFF pada tha).

**Ilaj:** WebSocket ab **Kotlin** mein hai (`object EdgeTts` — apna RFC-6455 client,
koi nayi library nahi). Native side par header ki koi pabandi nahi. JS sirf SSML
banata hai, Kotlin MP3 wapas deta hai.

- 🌊 **Nayi seerhi:** Gemini → **EDGE (be-hisaab)** → muft neural → phone
- 🌊 **Naya mode "Sirf Edge TTS"** — Gemini ka roz ka quota bilkul nahi jalta
- 🌊 **Edge awaaz ka picker** — Auto (zubaan + persona), ya 300+ live list mein se koi bhi
- 🌊 **"EDGE AWAAZ SUNO" button** — bolta bhi hai, waqt bhi batata hai, nakami par asal wajah bhi
- 🧪 **56 naye test** (kul 222) — protocol ka saboot naqli Edge server ke against: masking,
  16-bit length, **fragmentation**, PING/PONG, 5350/5350 bytes sahih

📖 Poori tafseel: [`docs/EDGE-TTS-ARCHITECTURE.md`](docs/EDGE-TTS-ARCHITECTURE.md)

## 🩺 v4.6.0 — AWAAZ DOCTOR ("3 nayi keys lagayin, ek bhi nahi chali")

**Wajah hamare code mein nahi thi — Google ke usoolon mein thi. Magar qusoor hamara tha
ke app ne wajah batai hi nahi.**

1. **Quota KEY par nahi, PROJECT par lagta hai.** AI Studio mein "Create API key" baar
   baar dabane se sab keys **ek hi project** mein banti hain aur **ek hi quota pool** se
   peeti hain. 3 nayi keys = wahi purana khatam quota. Faida sirf **alag Google account**
   (ya "new project") se banai gayi key ka hai.
2. **19 June 2026 se "unrestricted" keys Gemini par block hain** → 403, chahe key abhi bani ho.
3. Kuch projects ko free tier milta hi nahi — Google literally `limit: 0` bhejta hai.

*(CORS wajah nahi thi — Google ka preflight `x-goog-api-key` allow karta hai. Check kar ke
rad kiya, taake ghalat cheez theek na karte rahen.)*

**Ilaj — andaza band, muaina shuru.** Settings → AWAAZ mein naya
**🩺 GEMINI VOICE KEYS CHECK KARO** button. Har key par do imtihan:
`ListModels` (key khud zinda hai? restriction to nahi?) aur ek chhoti TTS request
(quota bacha?). Phir sab natije mila kar **saaf faisla** — inme sab se ahem:

> 🎯 **ASAL MASLA: AAP KI SAB KEYS EK HI PROJECT KI HAIN.**
> ILAJ: har key ALAG GOOGLE ACCOUNT se banao — har account = apna quota.
> Tab tak 🌸 MUFT NEURAL awaaz chalti rahegi.

Saath mein: TTS model **auto-discovery** (`ListModels` se), APK mein Gemini TTS ab
**Kotlin bridge** se (CORS ka sawal khatam, poora error text parha jata hai), aur sacche
paighamat jo 🩺 button ki taraf bhejte hain.

📖 Tafseel: [`docs/SETFORM-AWAAZ-POOL.md`](docs/SETFORM-AWAAZ-POOL.md) (Hissa 3)
🧪 `npm test` — CSS PASS · 64 Settings · **166** AWAAZ · 155 DIMAAG

---

## 🗄️🎙️ v4.5.0 — SETFORM + AWAAZ POOL (do asli bug, jarh se)

### 🗄️ "Settings baar baar reset ho jati hain / girlfriend mode khud band ho jata hai"

**Asal jarh:** SAVE button par **do** click handler lage hue the. Handler A keys save
kar ke `loadSettingsForm()` chala deta tha — jisne form ko *purani* settings se dobara
likh diya (GF switch OFF) — aur phir handler B usi palti hui halat ko save kar deta tha.
Natija: GF mode, gender, phone, persona, language, proactive, remember, convo mode,
music app, assistant naam — **ye sab SAVE button se kabhi save ho hi nahi sakte the.**

**Ilaj:** ab ek hi `SETFORM` darwaza hai aur ek data-driven `SET_FIELDS` registry
(44 fields) jo **load aur save dono** chalati hai:

```
collect() → fixKeys() → apply() → persist() → load()
   ▲ SAB parho pehle              form dobara likhna AB safe hai ▲
```

Saath mein: `fixKeys()` galat khane mein pari key khud sahi jagah bhejta hai
(`gsk_`, `csk-`, `sk-or-`, `nvapi-`, `github_pat_`) — aur purani key mitata nahi,
comma laga kar jorta hai. GF mode ab chup-chaap band nahi hota, wajah batata hai.

### 🎙️ "Gemini ki voice chal hi nahi rahi, baar baar band ho jati hai"

**Teen asal jarhen mileen:**

1. **Gemini TTS ka muft quota sirf ~15 request ROZ hai** (baqi models jaise 1,500 nahi).
   Purana code har 420 harf par ek request bhejta tha → ek lamba jawab = 4-5 request →
   **din mein sirf 3-4 jawab** aur awaaz khatam.
2. **403 ko "mari hui key" samajh liya jata tha** — halanke Gemini 403 aksar *quota* ke
   liye bhejta hai. Ek quota 403 poore session ki awaaz band kar deta tha.
3. **Model fallback** ek dafa koi model chalne ke baad **band** ho jata tha
   (`!AWAAZ.model` shart), is liye wo model marte hi awaaz hamesha ke liye girti thi.

**Ilaj — AWAAZ POOL, teen tehen:**

| Teh | Kya |
|-----|-----|
| 💎 **Gemini neural** | **kai keys** (comma se) — key1 ka quota khatam? key2 khud chal padti hai. Model mara? agla model. |
| 🌸 **MUFT NEURAL** | **bina key, bina signup** — asli insani awaaz jab Gemini ka din khatam ho |
| 📱 **Phone ki awaaz** | hamesha, 0 data |

Plus **smart chunking**: pehla tukra 300 harf (awaaz *foran* shuru), baqi 1500-1500
(kam request). 2,400 harf ka jawab pehle **6** request leta tha — ab **2**.
Har key ka apna cooldown `localStorage` mein mehfooz (restart ke baad bhi yaad),
aur Settings mein live pool nazar aata hai.

📖 Poori forensic tafseel: [`docs/SETFORM-AWAAZ-POOL.md`](docs/SETFORM-AWAAZ-POOL.md)
🧪 Saboot: `npm test` — CSS PASS · **64** Settings · **142** AWAAZ · **155** DIMAAG

---

## 🧠🔥 v4.4.0 — BRAIN POOL ("quota khatam" ka mustaqil ilaj)

**Masla:** Groq ki key daalne ke baad bhi *"Aaj ka free Gemini quota khatam… Backup brain bhi thak gaya."*
**Asal jarh:** Groq ne **16 Aug 2026** ko apne purane Llama models band kar diye — hamara default wahi tha,
is liye theek key ke bawajood har Groq call fail ho rahi thi. Doosri jarh: sirf 2 provider = 2 quota ki qaid.

**Ilaj:** ab MAYA ke paas **10 dimaag** hain, teen tehon mein —

| Teh | Dimaag |
|-----|--------|
| **KEYED** | 💎 Gemini · ⚡ Groq · 🚀 Cerebras · 🇪🇺 Mistral · 🔀 OpenRouter · 🐙 GitHub · 🟩 NVIDIA · 🇨🇳 Z.ai |
| **KEYLESS** (koi signup nahi) | 🆓 LLM7 · 🌸 Pollinations |
| **LOCAL** | waqt · tareekh · hisab · yaad-dasht |

Teen chaabiyan:

1. **Kai keys, ek khana** — har key field comma/nayi line se **kai keys** leta hai.
   3 Gemini keys = **3 guna quota**. Ek khatam, agli khud chal padti hai.
2. **Model auto-discovery** — provider "model decommissioned" kahe to app us model ko
   nikal kar `/models` se **zinda list** le aati hai. **Groq wala hadsa dobara nahi hoga.**
3. **Keyless farsh** — bilkul khaali Settings par bhi MAYA jawab deti hai.

Saath mein: cooldown `localStorage` mein mehfooz (restart ke baad bhi yaad), APK mein har
request Kotlin ke **async bridge** se (CORS lagoo nahi, UI kabhi jam nahi), Settings mein
live **"BRAIN POOL DEKHO"** panel, aur header pill par sach — **`AI READY • 6 DIMAAG`**.

📖 Poori tafseel: [`docs/BRAIN-POOL-ARCHITECTURE.md`](docs/BRAIN-POOL-ARCHITECTURE.md)
🧪 Saboot: `node tools/test-brain-engine.js` — **155/155**

---

## 🧠 v4.3.0 — DIMAAG ENGINE v2 ("Sab free brains busy" ka ilaj)

Wo message bar bar isliye aata tha ke **3 bug ek doosre ko khila rahe the**:

1. **HTTP 400 ko "key ka masla" samjha jata tha** → Gemini par **10 minute** ka blackout.
   Halanki 400 aksar kharab request hoti hai, key ka masla `401`/`403` hota hai.
2. **`chatHist.slice(-8)` ka pehla turn `model` ho jata tha** → Gemini multi-turn history
   `user` se shuru maangta hai → **400** → upar wala blackout. Chaar baat-cheet ke baad
   ye **har dafa** hota tha. Yahi "bar bar" ki asal jarh.
3. **Ek sawal par 5 model try** hote the → free quota 5 guna tez khatam.

Ab:
- **Har nakami ka naam** — KEY_MISSING / KEY_BAD / QUOTA / QUOTA_DAY / BAD_REQUEST / SERVER / NETWORK / MODEL_404 / EMPTY
- **Khud-marammat** — 400 aaye to history saaf kar ke, bina tools, foran dobara koshish
- **Quota izzat se** — Google jitna waqt maange (`retryDelay`) utna cooldown; per-day quota alag pehchana jata hai
- **Bekaar request kabhi nahi** — offline / key nahi / blackout par network call hi nahi jati
- **Khali haath nahi** — sab fail? pehle local jawab (waqt, tareekh, hisab, yaad-dasht), warna **asli wajah + agla qadam**
- **7 alag paigham** us ek jumle ki jagah — aur wo jumla source se hi nikal gaya
- **Header pill ab sach bolta hai** — `AI ONLINE` sirf jab aakhri 15 min mein asli jawab aaya ho
- **Doctor** mein har provider ki koshish ka poora record

Saboot: `tools/test-brain-engine.js` — **109 assertions**.
Tafseel: [`docs/DIMAAG-ENGINE-ARCHITECTURE.md`](docs/DIMAAG-ENGINE-ARCHITECTURE.md)

## 🎙️ v4.2.0 — AWAAZ ENGINE v6

**Neural awaaz ab sach much bolti hai.** Pehle Gemini TTS ka raw PCM bina WAV header ke
`<audio>` ko diya jata tha — is liye har dafa chup-chaap fail hota tha. Ab:

- **44-byte WAV header** (sample rate mime se parhi jati hai) → Gemini ki studio awaaz chalti hai
- **30 neural awaazein** + **9 mood** (warm / cheerful / calm / pro / hype / whisper / news / funny) + persona ka auto-mood
- **3-tier fallback** — 🎭 Gemini → 🌊 Edge (optional) → 📱 phone. **Khamoshi kabhi nahi.**
- **Har nakami ka naam** — key, quota, offline, WiFi-only, timeout… Settings aur Doctor dono mein saaf likha
- **Data bachao** — 12 clips ka cache, in-flight de-dupe, 429 par 90s cooldown, WiFi-only switch, awaaz ke liye alag key
- **Lambe jawab** ab tukron mein bolte hain (agla tukra pehle wale ke bajte waqt aa jata hai — beech mein khamoshi nahi)
- **Har jawab par 🔊** — dobara suno / rok do
- Hataya gaya: fish.audio (CORS red error) aur puter.js ka murda call
- Theek kiya: slider `touchmove` ka console warning, aur raw error banner ab khamosh nishan hai

Saboot: `npm test` → CSS check + 45 UI test + 118 voice-engine test.
Tafseel: [`docs/AWAAZ-ENGINE-ARCHITECTURE.md`](docs/AWAAZ-ENGINE-ARCHITECTURE.md)

## 🆕 v4.1.0 "IRONCLAD" — Settings UI ground-up rebuild

> Patch-work band. Pehle architecture (`docs/SETTINGS-UI-ARCHITECTURE.md`), phir code,
> phir machine se test. `npm test` green hue baghair kuch push nahi hota.

- 🧱 **UI KIT v5** — Settings ka har control naye sirey se: `ui-group / ui-field /
  ui-row / ui-slider / ui-sw / ui-btn`. Sirf block/table layout, px sizes aur
  `var(--token,#hex)` colors — **koi** `color-mix` / `accent-color` / `gap` /
  `inset` / `grid` / `env()` nahi
- 🎚️ **Custom sliders** — native `<input type=range>` ab sirf **state** rakhta hai
  (chhupa hua), dikhne wala slider div se bana hai: track + fill + 28px thumb +
  **big − / + buttons** (drag na chale to bhi value badal sakti hai)
- 🔘 **Custom switches** — 54×30, `:checked ~` **aur** JS `is-on` class — do rastay
- 📏 **Measured layout** — `MayaUI.layout()` screen naap kar `main`/`.tab` ki height
  px mein set karta hai (resize / rotate / splash ke baad dobara). Scroll ab
  flexbox ki mehrbani par nahi
- 🧪 **UI CHECK button** — Settings → Data & Maintenance: har control ko naap kar
  batata hai "127 visible / 0 invisible" + kaunsa control kahan gayab hai
- 🐛 **ES6 landmine hataya** — 32 `\u{...}` code-point escapes (Chrome <44 par poora
  script SyntaxError) surrogate pairs mein badle
- ✅ **2 automated test** — `npm test`: CSS linter (banned feature = fail) +
  jsdom functional test (slider/switch/layout/IDs) — **33/33 pass**

## 🆕 v4.0.2 "OBSIDIAN" — Settings UI Fix (purane WebView par bhi)

- 🎚️ **Sliders wapas dikhte hain** — pitch / raftar / corner / text-size ke range inputs ka
  track + thumb ab explicit hex/`var()` colors se bane hain (pehle `accent-color` drop hone par
  patli invisible lakeer reh jati thi)
- 🔘 **Switches asli toggle jaise** — 48×26px, fixed offsets (`top/left`), `transform` ki jagah
  `left` animation, `color-mix()` ke baghair
- 🏷️ **Labels aur inputs solid** — har `.f` field par background + border + `min-height:44px`
  (invisible field khatam)
- 📜 **Settings ab poori scroll hoti hai** — `#app > main > .tab` chain par `min-height:0`
  (pehle `main` content jitna lamba ho jata tha aur body ka `overflow:hidden` neeche ke
  sections — AWAAZ, THEME — kaat deta tha)
- 🧪 **Feature detect** — `CSS.supports()` se `color-mix` / `accent-color` / flex-`gap` check,
  `<html>` par `nocm` / `noacc` / `nogap` class + Doctor report mein "CSS COMPAT" line
- 📐 **`gap` aur `env()` fallbacks** — margin-based spacing + nav ka safe-area padding fallback

## 🆕 v4.0.1 "OBSIDIAN" — WebView Compat Fix

- 🧩 **Old-WebView COMPAT** — `inset`/`color-mix` fallback (Chrome <87 layout toot nahi sakta)
- 🛟 **file:// fallback** — WebViewAssetLoader fail ho to bhi app khulti hai (blank screen khatam)
- 📶 **old-WebView detect + toast** — purana Android System WebView update karne ka nudge
- 🔧 **false-alarm fix** — "WebView load NAHI hua" ka jhoota toast khatam (markAlive + onPageFinished)
- ⚡ **sw.js full ES5** — purane WebView par service worker bhi parse hota hai

## 🆕 v4.0.0 "OBSIDIAN" — Kya Naya Hai

- 🏠 **HOME screen + animated ORB** — tap to speak, live states (listening/thinking/speaking)
- 💜 **3 PERSONAS** — Maya (best friend) / Friday (professional) / Venom (alien funny) — voice se badlo: *"maya venom ko bhejo"*
- 💖 **Girlfriend Mode** — settings ya voice command se
- 🎨 **THEME ENGINE** — 7 themes (Midnight Obsidian, Cyber Cyan, Aurora, Rose, Forest, Daylight, Paper) + accent colors + corner-radius/text-size sliders + **Edge Glow**
- 🧠 **Memories screen** — poora CRUD (add/edit/delete) + JSON backup download
- 🌐 **13 Languages** — Roman Urdu, Urdu, Hindi, Hinglish, English, Punjabi, Bangla, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam
- ✨ **Proactive Mode** — Maya khud ba khud baat karti hai (memory-based)
- 🗣️ **Assistant name** + Conversation Mode + favorite song memory
- 📋 Naya **drawer navigation** (☰) — Home / Chat / Memories / Skills / HUD / Commands / Settings
- 🐛 **Bug fixes** — `cleanSpeech` crash fix (voice replies ab safe), duplicate ID fix

**Video-series roadmap (getmaya.online wali Maya jaisa):** Phase 2 = Voice Guardian + Touch Guard + SOS + screen lock — structure ready hai (`docs/maya-v4-structure.md`).


- 🎤 **Native voice input** — Urdu (ur-PK) / Hindi / English (Google recognition)
- 🗣️ **Native voice output** — system TTS engine (behtar Urdu awaaz)
- ⏰ **REAL system alarm** — *"subah 6 baje alarm lagao"* → clock app mein set!
- ⏱️ **REAL system timer** — screen band ho to bhi notification + bajega
- 🔋 Battery status, vibration, notifications
- 🔁 Auto-listen mode (screen-on lock ke saath)
- 🧠 Gemini AI brain (FREE API key — app ke andar guide)
- 🧷 Memory bank — *"yaad rakhna ki ..."*

---

## 🛠️ APK KAISE BANAYEIN — Step by Step (FREE, PC ki zaroorat NAHI)

### STEP 1 — GitHub account (free)
1. **github.com** kholo → **Sign up** → account banao

### STEP 2 — Repository banao
1. GitHub par login kar ke **+** (top right) → **New repository**
2. Naam do: `maya-android` → **Create repository** (Public ya Private dono chalega)

### STEP 3 — Ye files upload karo
1. Repository page par **"uploading an existing file"** link par click karo
2. Ye poora folder drag & drop karo (sab kuch: `.github`, `app`, gradle files, etc.)
   - **Ahem:** chhupi hui folders (`.github`) browser upload mein nahi dikhte —
     is liye GitHub web par manually banao:
     - **Add file → Create new file** → naam likho:
       `.github/workflows/build-apk.yml` → is file ka content paste karo → Commit
     - Baqi files (`settings.gradle`, `build.gradle`, `gradle.properties`,
       `app/build.gradle`, `app/src/...` sab) bhi **Create new file** se sahi
       path ke saath banao aur content paste karo (ye zaroori hai kyunki
       `.github` folder zip se upload nahi hota)
   - Binary files (icons) upload page se normal drag & drop hongi
3. **Commit changes** dabao

### STEP 4 — APK build (automatic)
1. Repository mein **Actions** tab kholo
2. **"Build MAYA APK"** workflow dikhegi — pehli push par khud chal jayegi
   (nahi chale to **Run workflow** button dabao)
3. **3-6 minute** wait karo (pehli baar Gradle dependencies download hoti hain)

### STEP 5 — APK download + install
1. Actions tab mein complete hone wali run par click karo (✅ green)
2. Neeche **Artifacts** section mein **MAYA-APK** dikhega → click kar ke
   **MAYA-APK.zip** download karo
3. Zip extract karo → andar `app-debug.apk` milegi
4. Phone mein APK kholo → **"Install unknown apps / Install anyway"** allow karo
   (Play Protect warning aaye to **Install anyway** — apna hi code hai, 100% safe)
5. MAYA app khulo → **INITIALIZE** → mic + notification permission allow karo

### STEP 6 — Gemini brain (2 minute)
1. `aistudio.google.com` → **Get API key** → copy
2. MAYA app → **⚙️ Settings** → API key paste → **SAVE**
3. Ho gaya — kuch bhi poocho! 🚀

---

## 📱 App mein kya hai

| Screen | Kaam |
|---|---|
| 🏠 HOME | Animated orb — tap to speak, persona chips, quick actions, live reply |
| 💬 CHAT | Voice (🎤) ya type se baat karo (text replies) |
| 🧠 MEMORIES | Maya ki yaadein — add / edit / delete / backup |
| 🧩 SKILLS | 8 built-in skills + custom seekhe hue skills |
| 🎯 HUD | Live clock, battery, AI status, system log, quick actions |
| ⚡ COMMANDS | Saari commands + RUN buttons + setup guides |
| ⚙️ SETTINGS | Personal / Personas / Awaaz / Customize / API Keys / Advanced |

## 🗣️ Voice Commands (misal)

- "maya, subah 6 baje alarm lagao" → **real alarm**
- "maya, paanch minute ka timer lagao" → **system timer**
- "maya, open whatsapp" / "open youtube"
- "maya, youtube pe [gaana] chalao"
- "maya, search karo [kuch bhi]"
- "maya, battery kitni hai" / "waqt kya hua"
- "maya, yaad rakhna ki kal doctor appointment hai"
- "maya, call karo 0300xxxxxxx"
- Aur kuch bhi — Gemini khud jawab degi (usi zubaan mein!)

---

## ❓ MASLA HO TO (Troubleshooting)

| Masla | Hal |
|---|---|
| Mic nahi chalta | Settings → Apps → MAYA → Permissions → Microphone allow |
| Awaz nahi aati | Settings → Accessibility → Text-to-speech → Google TTS install |
| Urdu sunai nahi deti | Google app update karo (voice recognition isi se aati hai) |
| AI jawab nahi deta | API key check karo + internet on karo |
| Alarm nahi lagta | Phone ka Clock app check karo (kuch phones SKIP_UI block karte hain) |
| Build fail (Actions) | Run ka log kholo → error dekho → file theek kar ke dubara push |

## 🔒 Privacy

- API key + chat + memory **sirf aap ke phone** mein (app data)
- AI sawal **direct Google Gemini** ko jata hai — koi doosra server nahi
- Code 100% open — khud dekh lo! 😄

## 🌐 Web Version (FREE Deploy)

MAYA ka web version bhi hai — phone ke browser mein chalta hai!

### GitHub Pages se deploy (FREE, 2 minute):
1. Repository push karo (ye files automatically deploy ho jayengi: `public/` folder)
2. GitHub par **Settings → Pages** kholo
3. **Source** mein "GitHub Actions" select karo
4. **Save** dabao — 1-2 minute mein live ho jayega!
5. Link milega: `https://tumhara-username.github.io/maya-android/`

### Phone mein install karo (PWA):
1. Upar ka link phone ke Chrome mein kholo
2. Menu (⋮) → **Add to Home screen** → **Install**
3. MAYA ka icon phone pe aa jayega — app jaisa chalega!

### Netlify se deploy (FREE, alternative):
1. **app.netlify.com/drop** kholo (free account)
2. `public/` folder drag & drop karo
3. Live link mil jayega!

> **Note:** Web version mein alarm, wake word, aur auto-send jaise native features nahi chalte.
> Voice chat, AI brain, aur memory bank full kaam karte hain!

---

## 🗺️ Roadmap

- ✅ Phase 1 — PWA Pro (web app)
- ✅ Phase 2 — Native APK (ye!)
- ⏳ Phase 3 — Smart Brain (AI function calling, routines)
- ⏳ Phase 4 — Boss Level (wake word, offline AI)

