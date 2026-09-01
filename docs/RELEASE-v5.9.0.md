# MAYA v5.9.0 — 🎚️ SUKOON: awaaz kabhi nahi kategi · mic-larai khatam

**versionCode 69 · versionName 5.9.0 · build 2 May 2026 shaam**

> Ek waqt mein EK cheez — Maya ya **bol** rahi hai, ya **sun** rahi hai. Kabhi dono ek saath nahi.

## Aap ke 3 live masle → teeno ka ilaj

| Aap ne bataya | Jadbahad (root) | Ilaj |
|---|---|---|
| 🔊 Aawaaz beech mein KAT jati ("MAYA onl—") | Mic khulte hi Android audio-focus mic ko de deta tha — TTS mar jati thi | **L1+L3**: JS ka `SUKOON` har lamha Kotlin ko batata hai "BOL_RAHI" — mic bolte waqt **paida hi nahi hota** |
| 🎤 Mic ka bulb on/off chamakta tha (wake + tap-to-speak dono mein) | Do SpeechRecognizer ek mic par lar rahe the; do pending restarts bhi ek saath chal sakte the | **L4 MIC SULAH** (tap-to-speak = service neeche) + **L6 RACE TOKEN** (har pending restart ka duct-ticket) |
| 👂 "Maya" kehne par nahi jaagti; wake switch apne aap band | Error 8 (RECOGNIZER_BUSY) par service **khud ko maar deti thi** AUR `__wakeErr` aap ka switch **mita deta tha** | **L5 ERR-8 MERCY**: na stopSelf, na switch chhedna — sirf 2s sukoon. `err8` counter mein saboot |

## 7 layers (poori detail: [`SUKOON-AUDIO-ARBITRATION.md`](SUKOON-AUDIO-ARBITRATION.md))

1. **L1 HAAL bridge** — `SUKOON` (JS) → `MayaBridge.setHaal` → `WakeWordService.setHaal` (Kotlin)
2. **L2 mic ke 4 darwaze** — restart · actuallyStart · VAD gate · watchdog — sab `haalBlock()` se poochchte hain
3. **L3 BOLNA PEHLE + 550ms echo tail** — `speak()` call se hi BOL_RAHI (fetch latency bhi cover); tail timer `APP_SUN` ko stomp nahi karta
4. **L4 MIC SULAH** — `pauseForApp()/resumeFromApp()` + 60s stale-pause recovery
5. **L5 ERR-8 MERCY** — wake switch ab KABHI khud nahi mitega
6. **L6 RACE TOKEN** — `pendingGen`: purana pending restart murda
7. **L7 SELF-WAKE SHIELD** — Maya ke bolte waqt VAD pehra bhi khamosh (apni awaaz par jaagne ka loop imkan-harab)

## Nazaarat (ab andha nahi rahenge)

- **KAAN v3**: `HAAL: KHALI/BOL_RAHI/APP_SUN` · `roka gaya: N dafa` · `err8 maaf: N` — LAB 🩺 panal + diagnostic 📋 dono mein
- LAB mein naya switch: **🎚️ SUKOON** (default ON — bugfix; band karna ho to aap ke haath mein, turant SharedPrefs tak)

## Bonus safai

- `appVersion()` → "5.9.0-native" (4.3.0 ka purana jhoot khatam)
- Boot toast ab sahi version bataata hai
- `sw.js` cache `maya-v5.9.0` — purani cached file kabhi nahi chalegi

## Saboot

- `npm test` = **970/970 PASS** (CSS ✓ · settings 72 · voice 294 · brain 155 · lab 449)
- Lab engine Section **26/26b**: 38 naye SUKOON tests — Kotlin source locks + **asli timed behavior** (550ms tail, tail-stomp race, greeting scene: sirf BOL>KHALI)
- CI har push par `assembleDebug` — Kotlin compile proof push ke baad ✅/❌ nazar ayega

## Device par kya dekhna hai (aap ka hissa)

1. **Greeting pora**: app kholo → "MAYA online Boss" — poora sunna chahiye, beech mein nahi katna
2. **Mic ka bulb**: Maya bol rahi ho to mic ka bulb BILKUL nahi chamakna chahiye
3. **"Maya" ka zinda hona**: KHALI waqt mein "Maya" bolo → jaagne chahiye ("Ji Boss?")
4. **Wake switch ki wafadari**: wake ON rakho — ghanton baad bhi ON hi hona chahiye jab tak aap band na karo
5. LAB → KAAN DOCTOR: `HAAL` aur `roka gaya` ki ginti nazar aani chahiye
