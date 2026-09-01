# 🕸️ P6 — KHUD-MUKHTAR · ka STRUCTURE

> **Ye plan hai, code nahi.** Maya ko haath nahi lagaya — `npm test` abhi bhi **825/825**.
> Aap ki manzoori ke baad hi ek line likhi jayegi.

---

# HISSA 1 — 🔬 FORENSIC: pehle dekha ke kya SACH MEIN maujood hai

## 1.1 ✅ Buniyad **pehle se pakki** hai (aur main hairaan hoon)

| Cheez | Kahan | Halat |
|---|---|---|
| **AlarmManager** se waqt par kaam | `ScheduledReceiver.kt` — asli `PendingIntent` + `setExact` | ✅ **maujood** |
| **Reboot ke baad bhi zinda** | `BootReceiver` + `RECEIVE_BOOT_COMPLETED` | ✅ **maujood** |
| **Foreground service** | `WakeWordService` | ✅ **maujood** |
| **Message sunne wali service** | `MayaNotifService` (`notifHistory()`) | ✅ **maujood** |
| **Accessibility auto-send** | `AutoSendService` | ✅ **maujood** |
| **Har amal ka roznamcha** | `LEDGER` (P3) — waqt, tool, args, kaamyab? | ✅ **maujood** |
| **Har turn ka waqt + tool** | `NAAP` (P0) — 120 turn | ✅ **maujood** |
| **Ijazat / undo / rails** | P3 | ✅ **maujood** |

> **Yani P6 ke liye Kotlin mein taqreeban kuch nahi likhna** — sirf jorna hai.
> *(Aur yehi wajah hai ke P6 ka khatra 🟡 hai, 🔴 nahi.)*

## 1.2 🔴 Magar `proactive` **bilkul bekaar** hai

```js
var PRO_LINES = [
  "{n}, soch rahi thi... aaj ka din kaisa ja raha hai?",
  "Bore ho rahi hoon {n} — koi quiz karein?",
  "{n}, ek kaam karo — paani pi lo. Main dekh rahi hoon. 💧"
];
setInterval(… har 6 minute … random line bol do …, 360000);
```

**Har 6 minute ek RANDOM jumla.** Na waqt dekhti hai, na battery, na aap ka kaam.
Ye "khud-mukhtari" nahi — **bay-maqsad shor** hai. *(Aap ka `proactive: true` hai —
yani ye abhi aap ke phone par chal raha hai.)*

## 1.3 🔴 Aur ek chhed: LEDGER **sirf 60 amal** rakhta hai

```js
LEDGER.MAX = 60
```
Aadat pehchanne ke liye **kai din** ka data chahiye. 60 amal 1-2 din mein bhar jate hain.
**Aadat mining ke liye ye barhana parega.**

---

# HISSA 2 — 💥 WTF: wo 3 lamhe jo hilaa denge

Maine soch kar ye teen chune — kyunke teeno **aap ke apne data** se bante hain,
aur teeno mein aap sochenge *"ye kaise ho gaya?"*

## 💥 WTF #1 — **Maya aap ki AADAT khud pakar leti hai**

Ek din chat khol kar ye milta hai:

```
🧠 MAYA NE KUCH NOTICE KIYA

  Pichle 6 din se aap raat ~11:20 par brightness kam karte ho.
  6 mein se 6 din. Har dafa 15 se 25 ke darmiyan.

  Main khud kar diya karun?

  [ ✅ HAAN, ROZ KARO ]   [ ⏰ POOCH KAR KARO ]   [ ❌ NAHI ]
```

**Aap ne kabhi ye nahi bataya. Aap ne khud bhi ghaur nahi kiya tha.**
Maya ne apne **LEDGER** (jo P3 mein bana tha) ko khud parh kar nikala.

**Ye jadu nahi — hisab hai:**
```
LEDGER se: (tool, arg ka bucket, ghanta) ke jore banao
Agar ek jora  >= 4 dafa  aur  >= 3 alag DINON mein  mila
   -> aadat mil gayi -> ijazat maango
```
*Ye is liye WTF hai ke aap ko apni aadat ka pata NAHI tha.*

---

## 💥 WTF #2 — **Maya ka ADHOORA kaam yaad rehta hai**

```
Maya: "Boss, kal raat 8:56 par Monarch ko WhatsApp par matn TYPE kar diya tha,
       magar SEND nahi hua tha. Ab bhej doon?"
```

**Ye aap ki asli chat se aa raha hai.** Us raat aap ne likhwaya, matn type ho gaya,
aur **SEND nahi hua**. Aap bhool gaye. **Maya ko yaad hai.**

**Kaise mumkin hai?** Kyunke P3 mein har amal ka `state` darj hota hai:
```js
LEDGER: { n: "message_contact", st: "typed", ok: true }
                                    ^^^^^^^ "likha gaya, BHEJA NAHI"
```
**Wo data pehle se jama ho raha hai — hum ne use kabhi parha hi nahi.**
Bas `st === "typed"` wali entries dhoondni hain jinke baad koi `AUTO-SEND ✓` na aaya ho.

---

## 💥 WTF #3 — **Maya apni GHALTIYON se khud seekhti hai**

```
🎓 MAYA KYA NAHI SAMJHI  (pichle hafte)

  "chamak barhao"        × 5 dafa   → ye kya tha?  [💡 brightness] [🔦 torch] [❌]
  "batti jala do"        × 3 dafa   → ye kya tha?  [🔦 torch] [💡 brightness] [❌]
  "arinh aijnt"          × 2 dafa   → ye kaunsa naam tha?  [ likho: ______ ]
```

**Ek tap → hamesha ke liye seekh gayi.** Agli dafa se pakar legi.

Aur ye khud ba khud jama hota hai:
- router samajh na paye → darj
- tool nakaam ho → darj
- **aap ek hi baat 2 dafa dohrayen** *(sab se bara ishara ke Maya samjhi nahi)* → darj
- SUNO ka natija aap ne khud sudhara → darj

> **App waqt ke sath BEHTAR hoti jayegi — bina meri madad ke.**
> Aur BUG 1 *(12 tools router ko nazar nahi aate the)* jaisa masla **dobara paida hi nahi ho sakta.**

---

# HISSA 3 — 🏗️ DHANCHA: 5 sutoon

```
var KHUD = {
  HAAL   : {}   👁️  Maya ko HAALAT ka pata ho
  RULE   : {}   🕸️  agar → to
  AADAT  : {}   🧠  apne roznamche se aadat pakarna      ← WTF #1
  ADHOORA: {}   📌  adhoore kaam yaad rakhna              ← WTF #2
  SEEKH  : {}   🎓  apni ghaltiyon se seekhna             ← WTF #3
  BUDGET : {}   🛡️  kitna bol sakti hai, kitna kar sakti hai
}
```

## 3.1 👁️ HAAL — Maya ko haalat ka pata ho *(sasta, foran asar)*

Har sawal ke sath ek chhota sa haal (~40 token):
```
HAAL: raat 11:47 · battery 12% (charge nahi) · WiFi · headphone laga
      · aakhri amal: brightness 100 (8 min pehle) · Maghrib mein 40 min
```

Phir *"brightness barhao"* par Maya keh sakti hai:
> *"Boss, 12% battery bachi hai — 60% kar doon? Poori karungi to 20 minute mein band."*

**Khatra 🟢 · Asar ☠️☠️☠️☠️** — ye akela Maya ko "koi apna" bana deta hai.

## 3.2 🕸️ RULE — agar → to

```
AGAR battery < 15%            TO batao + saver ka mashwara
AGAR Ammi ka message aaye     TO foran parh kar suna do
AGAR maghrib se 10 min pehle  TO yaad dilao
AGAR raat 11:00 baje          TO brightness 20 + "so jao boss"
```

**Aur rule aap ZUBAANI bana sakte ho:**
> *"jab battery 15% ho to mujhe batana"* → rule mehfooz ✅

**Tukre sab maujood:** `ScheduledReceiver` (waqt) · `MayaNotifService` (message) ·
`battery()` · `prayer_times`. Bas ek chhota rules engine chahiye.

## 3.3 🧠 AADAT — *(WTF #1)*

| | |
|---|---|
| **Data** | `LEDGER` — magar `MAX: 60` se **300** karna parega (~2 hafte) |
| **Hisab** | `(tool, arg bucket, ghanta ±30min)` → ginti + alag din |
| **Shart** | **≥4 dafa** aur **≥3 alag din** → tabhi aadat maani jayegi |
| **Amal** | ijazat maango. `HAAN` → rule ban jaye · `POOCH KAR` → har dafa poochhe |
| **Hifazat** | sirf 🟢 SABZ tools · ek waqt mein sirf **1** tajweez · mana kiya to **dobara kabhi nahi** |

## 3.4 📌 ADHOORA — *(WTF #2)*

```
LEDGER mein dhoondo:  st === "typed"  ya  st === "started"
                      aur uske baad koi tasdeeq na aayi ho
                      aur 10 min se purana ho
   -> "wo kaam adhoora reh gaya tha. Ab karun?"
```
**Ek dafa poochho, phir chup.** Rozana ek se zyada nahi.

## 3.5 🎓 SEEKH — *(WTF #3)*

```
MISS ka register:
   router samajh na paya          -> darj
   tool nakaam hua                 -> darj
   user ne ek hi baat 2 dafa kahi  -> darj  ← sab se bara ishara
   SUNO ka natija user ne sudhara  -> darj

Settings mein: "🎓 MAYA KYA NAHI SAMJHI" → ek tap → AMAL.TRIGGERS mein hamesha ke liye
```

---

# HISSA 4 — 🛡️ LAGAAM (be-lagam khud-mukhtari = tabahi, magar GHALAT wali)

P3 maujood hai, magar **khud se bolne** ke liye naye qawaid chahiye:

| Qanoon | Wajah |
|---|---|
| 🔇 **KHAMOSH GHANTE** — raat 12 se subah 7 tak khud se kuch nahi | neend |
| 💬 **Bol-budget** — rozana **max 6** khud se baatein, do ke darmiyan **≥45 min** | 6-minute ka shor khatam |
| 🚫 **Proactive kabhi 🔴 SURKH nahi** — khud se call/SMS **kabhi nahi** | wapas nahi hota |
| ⏱️ **Har rule 1/ghanta, kul 20/din** | loop se 50 notification na aayen |
| 🔋 **Battery < 10% → sirf zaroori** | Maya khud battery na khaye |
| ⚗️ **DRY-RUN** — "karti to ye karti", chalti kuch nahi | naye rules test karne ko |
| 📜 **Har khud-mukhtar amal LEDGER mein** + ⟲ undo | P3 se muft |
| 🤫 **Aap bol rahe ho / sun rahi hai → chup** | beech mein na tokay |

**Aur sab se ahem:** 🕸️ triggers **shadow mode** mein shuru honge —
*"main ye karti"* likhti rahegi, **karegi kuch nahi**, jab tak aap dekh kar ON na karo.
*(Qanoon 3 — AMAL-WORKFLOW.md)*

---

# HISSA 5 — 🧪 SABOOT (~55 naye test, kul ~880)

| Hissa | Kya sabit karega | Kitne |
|---|---|---|
| HAAL | poore haqaiq, ~40 token se chhota, khali par crash nahi | 8 |
| RULE | agar→to, zubaani rule banana, rate limit, khamosh ghante | 12 |
| **AADAT** | 4×/3-din wali shart · **kam data par kuch na kahe** · mana kiya to dobara nahi · sirf 🟢 | 12 |
| **ADHOORA** | `typed` pakra jaye · bheja hua **na** pakra jaye · rozana ek dafa | 8 |
| **SEEKH** | miss darj ho · dohraya jumla pakre · tap se hamesha ke liye seekhe | 9 |
| BUDGET | 6/din · 45 min gap · khamosh ghante · surkh kabhi nahi · battery guard | 10 |

**Aur ek khaas test:** *"Maya khud se 🔴 SURKH kaam kabhi na kare"* — 33 tools par chalega.

---

# HISSA 6 — 📅 Qadam ba qadam (ek build, teen switch)

| | Kya | Khatra | Kyun |
|---|---|---|---|
| **P6a** | 👁️ **HAAL** + 🛡️ BUDGET *(aur purani bay-maqsad chatter **KHATAM**)* | 🟢 | sasta, foran asar, aur shor band |
| **P6b** | 🧠 **AADAT** + 📌 **ADHOORA** ← **WTF #1 aur #2** | 🟡 | asli dhamaka |
| **P6c** | 🕸️ **RULE** + 🎓 **SEEKH** ← **WTF #3** | 🟡 | poori khud-mukhtari |

Teeno ka code **ek APK** mein *(switch OFF)* → aap **ek dafa** build karo → LAB se
ek ek chalu karo. *(CHHED 11 ka faida.)*

---

# HISSA 7 — ⚠️ Imaandari se: kya GHALAT ho sakta hai

| Dar | Ilaj |
|---|---|
| *"Maya bakwas karne lagegi"* | Bol-budget 6/din + 45 min gap + khamosh ghante. **Aur abhi jo har 6 min wali chatter hai wo KHATAM ho jayegi** — yani shor **kam** hoga |
| *"ghalat aadat pakar legi"* | 4 dafa + 3 alag din ki shart · sirf 🟢 SABZ · **hamesha ijazat** · mana kiya to dobara kabhi nahi |
| *"khud se kuch ghalat kar degi"* | Proactive **kabhi 🔴 SURKH nahi**. Har amal LEDGER + ⟲ undo |
| *"battery kha jayegi"* | Koi polling nahi — `ScheduledReceiver` (AlarmManager). Battery <10% par sirf zaroori |
| *"privacy"* | Sab kuch **phone par**. LEDGER/aadat kabhi bahar nahi jate. Diagnostic mein pehle se redaction |
| *"APK build toot jayega"* | **Kotlin mein kuch nahi badlega** — sab tukre pehle se maujood hain |

---

# HISSA 8 — 🗺️ P6 ke baad kya bachta hai

```
✅ P0 NAAP   ✅ P1 SAAF   ✅ P2 AMAL   ✅ P3 IJAZAT   ✅ P4 BIJLI+AANKHEIN
✅ SUNO · BOLI · MEHFOOZ · NISHANA
👉 P6 KHUD-MUKHTAR        ← ye
   P5 ZINDA (streaming)   ← aakhri, wahid Kotlin wala
```

**P6 ke baad Maya:** samajhti hai · **karti** hai · **sach** bolti hai · **ijazat** leti hai ·
**wapas** kar sakti hai · **dekh** sakti hai · **bina internet** ke bhi chalti hai ·
aur **aap ko khud se pehchanne** lagti hai.

**Sirf ek cheez bachegi:** 🗣️ **P5 ZINDA** — pehla jumla 0.8 sec mein aur beech mein rok
sakna. *(Wahid hissa jisme Kotlin badlega.)*
