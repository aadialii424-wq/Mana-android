# 🎚️ SUKOON (P9) — Audio Arbitration: ek waqt mein EK cheez

**v5.9.0 (code 69)** · Phase P9 · v5.8.0 ke 3 live maslon ka ilaj

---

## 1. Masla kya tha? (aasli jadbahad — real symptoms)

Aap ke phone par INSPECTOR ne 3 cheezein bataeen:

1. **Aawaaz KAT-ti thi** — "MAYA online Boss" kehne ke beech mein ("MAYA onl—")
   awaaz marr jati thi.
2. **Mic ka bulb chamakta/bhujhta** raha — wake pehre ke bhi, tap-to-speak ke bhi.
3. **"Maya" kehne par jaagti nahi** — wake ON karne ke baad bhi (aur aap ka
   wake switch khud-ba-khud band mil jata tha: nakami 19, mic 37, JAAGI 0).

Teeno ke peeche **EK hi jang-boot** thi:

> Ek waqt mein do kore audio ke maalik banne ki jang — awaaz (TTS) vs mic —
> aur OS (Android) har bar MIC ko focus de kar awaaz maar deta tha.

## 2. Jadbohoot (root cause — 4 hisse)

| # | Jadbahad | Wajah |
|---|----------|-------|
| R1 | Aawaaz katna | Mic (recognizer/VAD) khulte hi Android **AUDIO FOCUS mic ko de deta hai** → chal rahi TTS awaaz kat jati hai. Kotlin ko pata hi nahi tha ke Maya bol rahi hai — `speaking` flag sirf JS ke andar tha. |
| R2 | Mic on/off strobe | **Do SpeechRecognizer ek waqt par** (wake service + app ka tap-to-speak) — mic to ek hai. Ek khula to doosra band → `startListening()` → error → `restart` → doosre ne kholne ki koshish → **loop**. Plus do pending restarts ek saath chal sakte the (koi token nahi tha). |
| R3 | Wake band + switch mita | Jang error **8 (RECOGNIZER_BUSY)** deti thi. v5.8.0 ka code error 8 par **`stopSelf()`** kar ke service maar deta tha, aur JS `__wakeErr(8)` par **aap ka `settings.wakeWord = false`** bhi kar deta tha — isi liye switch "apne aap" band hota tha. |
| R4 | Self-wake loop | Speaker se Maya ki apni awaaz VAD/mic tak pahunchti → VAD "awaaz" samajhta → recognizer on → wo awaaz phir "Maya" lag sakti thi → **khud ko jagana**. |

## 3. Hal ka usool

> **Ek waqt mein EK cheez — ya Maya bol rahi hai, ya Maya sun rahi hai. Dono kabhi ek saath nahi. Aur do recognizer kabhi paida hi nahi hote.**

Iske liye 7 layers — JS se Kotlin tak EK hi silsila:

### 🎚️ L1 — HAAL bridge (JS → Kotlin)
- JS `SUKOON` module (`index.html`): awaaz/mic ki har tabdeeli par HAAL badalta hai —
  `KHALI` (kuch nahi) / `BOL_RAHI` (Maya bol rahi hai) / `APP_SUN` (app ka mic).
- Har tabdeeli `MayaBridge.setHaal(h)` se **lamhe mein** Kotlin tak jati hai
  (`MainActivity.setHaal` → `WakeWordService.setHaal`).
- SUKOON haath **sachchai JS mein, amal Kotlin mein** — dono jagah `try{}catch{}` mein
  (bridge na ho to bhi app chalti rahe).

### 🚪 L2 — Mic ke CHAAR darwaze ab HAAL poochchte hain
Kotlin mein mic kholne ke jo bhi raaste the, sab ke darwaze par `haalBlock()` ka pehra:
1. `restart(delay)` — har schedule se pehle
2. `actuallyStart()` — recognizer banane se pehle
3. `startGate()` + VAD loop — pehra chalne se pehle...
4. `watchdog()` — har-12-minute wala recognizer-reset se pehle

VAJAH MILE (Maya bol rahi hai / app ka mic / echo tail) → **ruk, 700ms baad phir poochho**.
Rokna KAAN v3 mein likha jata hai (`skip` counter) — andha nahi rahenge.

### 🗣️ L3 — BOLNA PEHLE + echo tail
- `speak()` ko **call** karte hi `bolStart()` — Fish/Edge fetch ki 1-2 second bhi cover
  (pehle mic usi dauran khul sakta tha).
- `speaking = true/false` ke **25 hook** jagah jagah (AWAAZ, answer, error...).
- Aawaaz khatam → **550ms echo tail** (`SUKOON.tailMs` = `ECHO_TAIL_MS`) — speaker ki
  goonj mic tak pahunchne se pehle mic nahi khulta.
- Tail ka timer **faisla us waqt** karta hai: agar isi dauran app ka mic khul gaya
  (`APP_SUN`) to tail usse chhoota NAHI.

### 🤝 L4 — MIC SULAH (tap-to-speak hamesha jeetta hai)
- `listen()` start se pehle `WakeWordService.pauseForApp()` → service apna mic
  chhor kar chup.
- Mic band → `SUNEnd` → KHALI → service `restart(300)` se wapas zinda.
- **60s stale-pause recovery**: WebView mar bhi jaye to watchdog pause ko
  khud azad karta hai — wake hamesha ke liye nahi sota.

### 🕊️ L5 — ERR-8 MERCY (sab se zaalim bug ka qatl)
- Error 8 ab na `stopSelf()`, na aap ka switch — sirf **`report("err8")` + 2s baad dobara**.
- JS `__wakeErr(code)` **kabhi `settings.wakeWord = false` nahi karta** — sirf
  batata hai (toast + pushLog → KAAN DOCTOR). Faisla aap ka.

### 🎫 L6 — RACE TOKEN (pending restart ka duct-ticket)
- Har scheduled restart ko `++pendingGen` se ek token milta hai; chalne ke waqt
  token match na ho to **murda**.
- Naya HAAL aaya (BOL_RAHI/APP_SUN) ya `hardPause()` hua → `pendingGen++` →
  saare pending restart ek saath murda. Dobara do pending kabhi ek saath nahi chalte →
  **mic strobe khatam**.

### 🛡️ L7 — SELF-WAKE SHIELD
- VAD gate ke rec-loop ke andar har chunk se pehle `haalBlock()` — Maya bol rahi hai
  to **pehra bhi khamosh** (apni awaaz par jaagne ka loop imkan-harab).

## 4. Nazaarat (andha nahi rahenge)

- **KAAN v3**: `skips` (kitni dafa mic HAAL ki wajah se roka gaya), `err8` count,
  `HAAL: KHALI/BOL_RAHI/APP_SUN` — LAB ke KAAN panal aur diagnostic WAKE block dono mein.
- `report("skip", why)` har roke gaye mic ki wajah likhta hai.
- **LAB switch**: `🎚️ SUKOON` — default **ON** (ye bug-fix hai). Band karna ho to
  switch se — pref `sukoon` turant SharedPrefs tak mirror hota hai (`labSukoonMirror`
  + `setWakeService`), Kotlin ka gate yeh pref **live** parhta hai.

## 5. Regression hifazat (rules)

- `speaking`/`listening` ka **sab kaam bilkul pehle jaisa** — sirf saath mein SUKOON
  ko khabar hoti hai (try/catch).
- Wake word, darwaza, permission card — sab pehle jaisa.
- SUKOON OFF = v5.8.0 ka purana rawaiya wapas (haalBlock turant null → kuch nahi rukta).
- `sw.js` cache `maya-v5.9.0` — purana browser-cache kabhi purani file na chalaye.

## 6. Bonus bugs jo raaste mein mare

- `appVersion()` ab "5.9.0-native" (pehle 4.3.0-native ka purana jhoot).
- Boot toast ab waqai version bataata hai (pehle "v4.1.0 IRONCLAD").

## 7. Saboot

- `node tools/test-lab-engine.js` → **Section 26/26b** ke 38 tests (Kotlin source locks
  + asli timed behavior: bolStart/bolEnd/tail/races).
- CI: har push par `assembleDebug` — Kotlin build sach mein compile hoti hai.
- Device check: greeting "MAYA online Boss" pora sunai de; bolte waqt mic ka bulb
  chamakna band; "Maya" par jaage; wake switch hamesha ON rahe jab tak AAP band na karin.
