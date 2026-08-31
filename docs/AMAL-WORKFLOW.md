# 🏗️ AMAL WORKFLOW — "full next level, magar masla bilkul nahi"

> `AMAL-ENGINE-PLAN.md` batata hai **KYA** banega.
> Ye dastavez batati hai **KAISE** banega — is tarah ke 6730 line ki chalti hui app
> mein 6 phase ka bara kaam ho jaye aur **ek cheez bhi na toote**.
>
> Ye khud koi feature nahi. Ye wo **qanoon** hai jis ke tehat har feature banega.

---

# HISSA A — Buniyad: app ka maujooda dhancha (jo pehle se SAHIH hai)

```
public/index.html   6730 lines
  ├─ AWAAZ          1000   awaaz ka WAHID DARWAZA        → AWAAZ.speak()
  ├─ FISH            625   🐟 engine                      → FISH.speak()
  ├─ EDGE_TTS        315   🌊 engine                      → edgeTTS_speak()
  ├─ DIMAAG          896   sochne ka nizam                → askAI()
  ├─ BRAIN           242   10 dimaag ka pool              → BRAIN.ask()
  ├─ SETFORM         774   settings ka WAHID DARWAZA      → SETFORM.save()
  └─ THEMES / PERSONAS / TOOLS …
```

**Yahan pehle se ek bohat achhi aadat maujood hai: "WAHID DARWAZA".**
Poori app awaaz ke liye sirf `AWAAZ.speak()` bulati hai. Settings sirf `SETFORM.save()`.
Isi liye v4.6 → v4.7 → v4.8 mein teen naye engine (Doctor, Edge, Fish) **jur gaye aur
kuch nahi toota**.

> **Qanoon 0 — Jo tareeqa 3 dafa chal chuka hai, wohi 6 phase ke liye bhi chalega.**
> Naya `AMAL` bhi bilkul isi shakl mein banega: **ek namespace, ek darwaza.**

---

# HISSA B — 7 QANOON (in ke bina koi line nahi likhi jayegi)

### ⚖️ Qanoon 1 — HAR NAYI CHEEZ EK SWITCH KE PEECHE
Har naya hissa `FLAGS` mein apna switch lekar aayega, aur **shuru mein OFF**:
```
FLAGS = { saaf:1, poolTools:0, amalText:0, bijli:0, aankhein:0, stream:0, triggers:0 }
```
- Switch OFF = code maujood hai magar **chalta hi nahi** → purana raasta jyun ka tyun.
- Kuch bigra → switch OFF → **Maya foran theek**. Nayi APK ka intezar nahi.
- Ye switches Settings mein **🧪 LAB** naam ke chhupe khane mein honge.

### ⚖️ Qanoon 2 — PURANA RAASTA KABHI NAHI MITEGA (Strangler)
Naya engine purane ke **saath** chalega, uski **jagah** nahi:
```
AMAL.run(hukm)
   ├─ naya raasta try karo
   └─ kisi bhi wajah se na chale → PURANA raasta (jo aaj chal raha hai)
```
Purana code **P6 mukammal aur device par tasdeeq hone tak** hataya nahi jayega.
*(Bilkul waise jaise Fish nakaam ho to Gemini → Edge → phone.)*

### ⚖️ Qanoon 3 — SHADOW MODE: pehle dekho, phir karo
Naya router pehli dafa **koi amal nahi karega**. Wo sirf **chupke se likhega** ke
"main kya karti":
```
🕵️ SHADOW: "britness barhao 100%"
    purana router → (kuch nahi)
    naya router   → brightness_control{percent:100}   ✅ behtar
```
Ye log Settings mein dikhega. **Jab tak 50 asli hukm par naya router purane se
behtar ya barabar na ho, uska switch ON nahi hoga.**
→ Yani feature aap ke phone par **sabit** hone ke baad chalu hota hai, umeed par nahi.

### ⚖️ Qanoon 4 — INVARIANTS: 8 baatein jo KABHI nahi toot saktin
Har phase ke baad ye 8 test chalenge. **Ek bhi laal = release nahi.**

| # | Qanoon | Test kya karega |
|---|---|---|
| 1 | **Maya kabhi khamosh nahi** | har nakami ka raasta banao → jawab phir bhi aaye |
| 2 | **Andar ki soch kabhi bahar nahi** | 10 reasoning shaklein → bubble aur awaaz dono saaf |
| 3 | **Bina `ok` ke kamyabi ka daawa nahi** | tool fail karwao → jawab mein "ho gaya" na ho |
| 4 | **🔴 tool bina ijazat kabhi nahi** | call/SMS ka hukm → ijazat maange bagair na chale |
| 5 | **Purana raasta hamesha zinda** | sab naye switch OFF → app v4.8 jaisi chale |
| 6 | **Settings kabhi khud reset na hon** | SETFORM ke 72 test (pehle se maujood) |
| 7 | **Awaaz ki seerhi kabhi na toote** | Fish→Gemini→Edge→muft→phone (293 test) |
| 8 | **Ek hukm = ek amal** | dohra amal na ho (double-fire guard) |

### ⚖️ Qanoon 5 — HAR PHASE APNA TAALA KHUD LAGAYEGA
Jo bug theek ho, uska test **usi commit mein** likha jayega — taake wo **dobara wapas
na aa sake**. *(Misaal: BUG 1 ke liye "har tool pakra jaye" wala 33-test.)*
**Regression budget hamesha 0.**

### ⚖️ Qanoon 6 — EK PHASE = EK RELEASE = EK ROLLBACK
Har phase apna alag commit + tag. Bigar jaye to:
```
git revert <us phase ka commit>     → sirf wo phase gaya, baqi sab salamat
```
Kabhi bhi do phase ek commit mein nahi.

### ⚖️ Qanoon 7 — DEVICE PAR AAP KI TASDEEQ = DARWAZA
Main `npm test` green kar sakta hoon, magar **asli faisla aap ke kaan aur aankh ka hai.**
Aap ke "haan chal raha hai" ke bagair **agla phase shuru nahi hoga.**

---

# HISSA C — NAYA DHANCHA (code kahan rahega)

Sab kuch usi `index.html` mein rahega *(single-file app hai — service worker aur
APK mirror isi par khare hain)*, magar **saaf hudood** ke sath:

```
var AMAL = {                        ← naya WAHID DARWAZA (~700 lines)

  FLAGS   : {…}                     ← Qanoon 1  · har feature ka switch
  TOOLS   : [ …33+ ]                ← ek registry: naam · args · triggers · darja · alias
  DIALECT : { gemini, openai, text }← ek registry, teen tarjume
  SAAF    : fn(text)                ← soch ka sanitizer (10 shaklein)
  ROUTER  : { match, shadow, learn }← lughat TOOLS se khud banti hai
  RUN     : fn(name, args)          ← ijazat → amal → {ok,value,error} → ledger
  LEDGER  : { push, undo, today }   ← har amal + ⟲ wapas
  LOOP    : fn(msgs)                ← asli agent loop (dono zubanon ke liye ek)
  TRACE   : { start, step, end }    ← live naqsha
  GUARD   : { rate, otp, unknown }  ← safety rails
}
```

### Nirbharta ka rukh (kabhi ulta nahi)
```
SETFORM  →  AMAL  →  { BRAIN, AWAAZ, MayaBridge }
```
- `AMAL` neeche walon ko **bula sakta hai**.
- `AWAAZ` / `BRAIN` `AMAL` ko **kabhi nahi** bulayenge.
- **Faida:** awaaz ka nizam (293 test) aur pool (155 test) ko haath hi nahi lagta.

### Ek chhota magar ahem test: DHANCHE KA TAALA
Test harness code ko **naam se dhoond kar** nikalta hai:
```js
const FA = HTML.indexOf('var FISH = {');
```
Agar koi in nishanion ko hila de to **520 test khamoshi se ghalat cheez jaanchne
lagenge**. Is liye ek naya test:
> *"AWAAZ · EDGE_TTS · FISH · AMAL · BRAIN · DIMAAG · SETFORM — saaton maujood hon,
> isi tarteeb mein hon, aur ek doosre mein na ghusen."*

---

# HISSA D — HAR PHASE KA WORKFLOW (7 qadam, har dafa wahi)

```
1️⃣  ISSUE CARD   Kya toota hai? Saboot ke sath (code ki line / chala kar dikhaya hua)
        ↓
2️⃣  CONTRACT     Kya banega, kya NAHI banega, kaunsa switch, kaunse test
        ↓          → aap ki manzoori ← (yahan main rukta hoon)
3️⃣  CODE         Switch OFF ke sath. Purana raasta chhua tak nahi.
        ↓
4️⃣  SABOOT       Naye test + purane 520 → SAB green. Regression 0.
        ↓
5️⃣  SHADOW       APK mein switch OFF, magar log chalta hai.
        ↓          Aap 2-3 din normal istemal karo.  🕵️ log dekho.
6️⃣  FLAG ON      Log sahih → LAB se switch ON → aap device par tasdeeq karo
        ↓
7️⃣  TAALA        Test likh kar bug ko qaid karo → tag → agla phase
```

> **Qadam 2 aur 6 par main hamesha rukunga.** Aap ki manzoori ke bagair aage nahi.

---

# HISSA E — HAR PHASE KA KHATRA, AUR USKA THEEK ILAJ

| P | Kya | Kya bigar sakta hai | Ilaj (mechanism) | Bacha |
|---|---|---|---|---|
| **P1** SAAF ZUBAAN | sanitizer · token · zubaan | sanitizer zyada kaat de → jawab adhoora | saaf ke baad khali/chhota → **purana matn** wapas + agla dimaag · `FLAGS.saaf` | 🟢 |
| **P2a** pool ko tools | `BRAIN.ask` mein `tools` | koi provider `tools` par 400 de → **wo dimaag mar jaye** | 400 aaye → **usi waqt bina tools dobara** + us provider par tools band yaad rakho · `FLAGS.poolTools` | 🟢 |
| **P2b** text protocol | `‹AMAL›` parser | parser jhoote match kare → ghalat amal | sirf 🟢 tools bina ijazat · parser sakht · rescue sirf tool ke naam par · shadow pehle | 🟢 |
| **P2c** router | lughat TOOLS se | naya router purane se bura nikle | **Qanoon 3 — SHADOW.** 50 hukm par sabit hone tak ON nahi | 🟢 |
| **P2d** agent loop | multi-step | loop mein atak jaye / bar bar chale | qadam ≤5 · kul waqt ≤30s · ek tool ek turn mein ek dafa · `gen` guard *(AWAAZ mein pehle se maujood tareeqa)* | 🟢 |
| **P3** ijazat + ledger | lagaam | ijazat ka dialog atak jaye | timeout par **na** karo (default = mana) · undo hamesha | 🟢 |
| **P4a** BIJLI | 50ms local | ghalat local match | yaqeen kam → purana raasta · sirf 🟢 · ledger + ⟲ | 🟢 |
| **P4b** AANKHEIN | camera | camera na khule / bara photo | `takePhoto()` pehle se maujood ✅ · photo chhota karo · nakaam = aam jawab | 🟢 |
| **P5** STREAM | **naya Kotlin SSE** | **build fail / crash** | **neeche alag protokol** ↓ | 🟡 |
| **P6** TRIGGERS | agar→to | rule loop → 50 notification | har rule 1/ghanta · kul 20/din · `GUARD.rate` · DRY-RUN pehle | 🟢 |

---

# HISSA F — 🔴 P5 ka khaas protokol (wahid jagah jahan Kotlin badlega)

Main Kotlin **compile nahi kar sakta**. Is liye P5 par ye 6 pabandiyan:

1. **Sirf JORNA, badalna nahi.** Naya `httpStream()` likha jayega. `httpBytes()`,
   `httpPostAsync()`, `edgeTts()` — kisi ko **chhua bhi nahi** jayega.
2. **JS pehle poochhega:** `if (MayaBridge.httpStream)` — na mile to purana raasta.
   Yani **purani APK bhi nayi index.html ke sath chalti rahegi**.
3. **Python mirror se saboot** — bilkul Edge TTS wala tareeqa: Kotlin ka hoo-ba-hoo
   tarjuma bana kar naqli SSE server par chalaunga *(us dafa 5350/5350 bytes sahih nikle the)*.
4. **Brace/import/API scan** — jaise ab tak har Kotlin commit par kiya.
5. **P5 bilkul akela commit** hoga — kisi aur cheez ke sath nahi.
6. **Aap pehle APK build karo.** Build fail = `git revert` = ek minute mein wapas.
   Build pass = phir switch ON.

> **Is liye P5 sab se aakhir mein hai.** P1-P4 aur P6 mein Kotlin ko haath tak nahi lagta —
> wahan "build toot jayega" wala khatra **wujood hi nahi rakhta**.

---

# HISSA G — TEST KA PAIMANA (kaam khatam kab mana jayega)

Har phase ke liye **DEFINITION OF DONE** — saaton sahih hon, warna phase adhoora:

```
□ 1  Naye test likhe gaye (pehle fail hote hon, phir pass)
□ 2  Purane 520 test green — regression 0
□ 3  8 INVARIANT test green
□ 4  Dhanche ka taala green (namespace tarteeb salamat)
□ 5  Mirror sync (public/ == app/src/main/assets/web/)
□ 6  Doc likha (kya · kyun · nuqsanat imaandari se)
□ 7  Aap ne device par apne haath se tasdeeq ki
```

Test ka kul hisab jab sab mukammal ho:
```
aaj                          520
+ P1 sanitizer                24
+ P2 amal engine             178
+ P3 ijazat/ledger/undo       40
+ P4 bijli + aankhein         35
+ P5 stream + barge-in        25
+ P6 triggers + routines      35
─────────────────────────────────
                             857
```

---

# HISSA H — TARTEEB, AUR IS TARTEEB KI WAJAH

```
P1 ── P2 ── P3 ── P4 ── P6 ── P5
🟢    🟡    🟢    🟢    🟡    🔴
```

| Kyun ye tarteeb | |
|---|---|
| **P1 pehle** | Sab se sasta, sab se mehfooz, aur **aap ki aaj ki poori shikayat** ka jawab. Kotlin nahi chhoota. |
| **P2 doosra** | Baqi sab isi par khara hai. Ye asal buniyad hai. |
| **P3 teesra — P4/P6 se PEHLE** | **Ye lagaam hai.** Ijazat + ledger + undo + rails maujood hone se pehle Maya ko zyada taqat dena be-waqoofi hai. |
| **P4 chautha** | Sasta aur bara dhamaka (vision + 50ms), aur ab lagaam maujood hai. |
| **P6 paanchwan** | Asli khud-mukhtari — **sirf tab mehfooz jab P3 maujood ho**. |
| **P5 aakhir mein** | Wahid jagah jahan Kotlin badalta hai. Sab kuch chal raha hoga, to is ka khatra bhi mol lena aasan hoga. |

**Ghaur:** maine P5 (streaming) ko **P6 ke baad** rakh diya hai — halanke wo zyada
"mazedar" hai. Wajah sirf ek: **wo wahid cheez hai jo APK ka build tor sakti hai.**
Us se pehle app poori tarah next-level ho chuki hogi.

---

# HISSA I — Khulasa: "masla kyun nahi hoga"

| Aap ka dar | Is ka jawab |
|---|---|
| *"kuch toot jayega"* | Har cheez **switch ke peeche** (Qanoon 1) + **purana raasta zinda** (Qanoon 2) |
| *"pata hi nahi chalega kya bigra"* | **8 invariant** + 857 test + har phase ka apna taala |
| *"naya feature bura nikla to?"* | **SHADOW mode** — chalane se pehle aap ke apne phone par sabit hoga |
| *"wapas kaise karun?"* | Ek phase = ek commit = `git revert` (Qanoon 6) |
| *"APK build fail hua to?"* | Sirf P5 mein mumkin — aur wo **sab se aakhir** mein, akela, Python mirror se sabit shuda |
| *"Maya ghalat kaam kar degi"* | 🟢/🟡/🔴 darje + ijazat + rate limit + **⟲ undo** + OTP/paisa guard |
| *"beech mein chhor doge?"* | Har phase **apne aap mein mukammal** hai. P2 par ruk jayen to bhi app P2 tak next-level rahegi. |

---

## 🎯 Aakhri baat

Ye workflow koi nayi cheez nahi hai — **yehi tareeqa is app par 4 dafa chal chuka hai**
(v4.5 SETFORM · v4.6 Doctor · v4.7 Edge · v4.8 Fish). Har dafa naya bara engine jura
aur **ek bhi purana test laal nahi hua**.

Farq sirf itna hai ke ab hum ne use **likh kar qanoon bana diya hai** — taake 6 phase ke
bare kaam mein bhi ek bhi cheez ittefaq par na chhoray.
