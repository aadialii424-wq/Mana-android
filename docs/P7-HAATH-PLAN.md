# ✋ P7 — HAATH: Maya aap ka phone KHUD chalayegi

> **Ye plan hai, code nahi.** Maya ko haath nahi lagaya — `npm test` **825/825**.
> Aap ke "haan" ke baad hi ek line likhi jayegi.

---

# HISSA 1 — 🔬 FORENSIC: Maya ke HAATH pehle se maujood hain

Maine socha tha ye sab se bara aur khatarnak kaam hoga. **Ghalat tha.**

## 1.1 💥 `AutoSendService.kt` ek **asli AccessibilityService** hai

```kotlin
class AutoSendService : AccessibilityService() {
    val root = rootInActiveWindow                          // POORI SCREEN ka tree
    root.findAccessibilityNodeInfosByViewId("…:id/send")   // element dhoondna
    root.findAccessibilityNodeInfosByText("Send")          // matn se dhoondna
    n.performAction(AccessibilityNodeInfo.ACTION_CLICK)    // TAP
    performGlobalAction(GLOBAL_ACTION_BACK)                // BACK
    MainActivity.instance?.evalAsyncPublic("…")            // JS ko batana
}
```

**Ye 70% kaam pehle se ho chuka hai** — bas WhatsApp ke SEND button ke liye hard-code hai.

## 1.2 💥 Wake word **pehle se chal raha hai**

```kotlin
// WakeWordService.kt — foreground service, hamesha sunti hai
val isWake = t.contains("maya") || t.contains("maywa") || t.contains("mya")
```
Aap ki farmaish *"Maya bolun to jaag jaye, warna chup"* — **ye already bana hua hai.**

## 1.3 🔴 To ROKA KYA HAI? **Sirf 3 line XML**

`res/xml/autosend_service_config.xml`:
```xml
android:packageNames="com.whatsapp"          ❌ sirf WhatsApp dikhta hai
android:accessibilityFlags="flagDefault"     ❌ view-id aur windows nahi milte
<!-- canPerformGestures GAYAB -->            ❌ SCROLL / SWIPE nahi kar sakti
```

**Naya:**
```xml
<!-- packageNames HATA do -> har app dikhegi -->
android:accessibilityFlags="flagDefault|flagReportViewIds|flagRetrieveInteractiveWindows|flagIncludeNotImportantViews"
android:canPerformGestures="true"            ✅ scroll, swipe, koi bhi jagah tap
```

> **Teen line. Bas.** Aur Maya poora phone chala sakti hai — Chrome, Instagram, koi bhi app.

## 1.4 ✅ Aur baqi sab bhi maujood hai

| | |
|---|---|
| App kholna (intent) | `open_app` tool ✅ |
| URL kholna | `openURL()` ✅ |
| Phone lock | `lockScreen()` ✅ |
| Tools ka nizam | `AMAL` (P2) ✅ |
| Ijazat / undo / rails | P3 ✅ |
| Bolna | AWAAZ ✅ |

---

# HISSA 2 — 🏗️ DHANCHA: "HAATH"

```
      🎙️  "Maya… chrome kholo aur lm arena par jao"
                        │
                        ▼
              🧭 MISSION  (LLM, 1 call)
              qadam ki fehrist banao
                        │
        ┌───────────────▼───────────────┐
        │       ✋ HAATH KA LOOP        │
        │                               │
        │  👁️ DEKHO   screen dump       │  ← Accessibility tree
        │  🧠 SOCHO   agla amal kya?    │  ← LLM (ya RASTA se muft)
        │  ✋ KARO    tap/type/scroll   │  ← performAction / gesture
        │  ⏳ RUKO    screen badle tak  │  ← onAccessibilityEvent
        │                               │
        └───────────────┬───────────────┘
                        ▼
        🗣️ "3 mein se 2 — Search par tap kiya"
```

## 2.1 👁️ DEKHO — screen ko matn banana

```
SCREEN: com.android.chrome
 [1] editfield  "Search or type URL"
 [2] button     "Search"
 [3] link       "Agent Arena | AI Leaderboard"
 [4] text       "lmarena.ai"
 [5] button     "New tab"
 …scrollable…
```

**Sab se ahem masla — SIZE.** Chrome ke ek page mein 500+ node hote hain.
Ilaj:
- sirf **interactive** (clickable/editable/scrollable) + **nazar aane wale** node
- har label **60 harf** tak
- ek jaise node **jama** kar do (`"reel × 12"`)
- **≤40 element**, `≤1200 token` — warna prompt phat jayega *(CHHED 9 ka sabaq)*

## 2.2 ✋ KARO — 8 amal, bas

| Amal | Kaise |
|---|---|
| `tap(n)` | `performAction(ACTION_CLICK)` |
| `type(n, "matn")` | `ACTION_SET_TEXT` |
| `scroll(up/down)` | `dispatchGesture` |
| `swipe(left/right)` | `dispatchGesture` *(reels!)* |
| `back` / `home` / `recents` | `performGlobalAction` |
| `open(app/url)` | intent *(pehle se hai)* |
| `wait(ms)` | |
| `done("jawab")` | mission khatam |

## 2.3 🧠 SOCHO — magar **har qadam par LLM = tabahi**

Har qadam = 1 LLM call = **2–5 sec** + quota. 10 qadam = **40 sec aur 10 calls**.
Gemini free 250/din → **25 kaam roz**. Ye chalega nahi.

---

# HISSA 3 — 💥 ASAL TABAHI: **RASTA** (aur yehi WTF hai)

## 3.1 Pehli dafa — Maya thoranri der sochti hai
```
"Maya, chrome kholo aur lm arena par jao"

👁️ dekha  🧠 socha  ✋ Chrome khola
👁️ dekha  🧠 socha  ✋ address bar par tap
👁️ dekha  🧠 socha  ✋ "lmarena.ai" type kiya
👁️ dekha  🧠 socha  ✋ Enter
✅ 38 second · 5 LLM call
```

## 3.2 **Aur Maya ye RASTA yaad kar leti hai**
```json
RASTA["lm arena kholo"] = [
  { open: "chrome" },
  { tap: "address bar" },
  { type: "lmarena.ai\n" }
]
```

## 3.3 💥 Doosri dafa
```
"Maya, lm arena kholo"

✅ 2.5 second · 0 LLM call · internet ke bina bhi
```

> **YEHI WO LAMHA HAI.** Pehli dafa 38 second, doosri dafa **2.5 second**.
> Aap sochenge: *"ye kaise ho gaya?"*
>
> **Aur ye khud ba khud hota hai.** Aap ko kuch record nahi karna, kuch likhna nahi.
> Maya jo kaam ek dafa kar leti hai, **usay yaad rakh leti hai.**

**Aur rasta tootne par?** Screen match na ho → **khud ba khud LLM loop par wapas** →
naya rasta seekh kar purana update. *(Chup-chaap. Aap ko pata bhi nahi chalega.)*

---

# HISSA 4 — 🎬 Aap ke apne kaam, qadam ba qadam

### `"Maya, lm arena kholo"` → *(rasta)* 2.5 sec
### `"is prompt ko daal do: ___"`
```
👁️ chat box dhoondo  ✋ tap  ✋ type  ✋ send
```
### `"doosre tab par jao, google flow"`
```
✋ tab switcher  ✋ "+"  ✋ type "labs.google/flow"  ✋ Enter
```
### `"Instagram kholo, reels chalao"`
```
✋ open instagram  ✋ Reels tab  → "scroll karo" → ✋ swipe up
```
### `"ye wali video open karo"`
```
👁️ screen par videos ginti  🧠 "samne wali" = beech wali  ✋ tap
```

**Aur poore waqt:** aap dekh rahe ho, Maya bol rahi hai *("Chrome khol diya… ab
address bar par ja rahi hoon…")*, aur aap beech mein **"ruko"** keh kar rok sakte ho.

---

# HISSA 5 — 🛡️ LAGAAM: ye sab se KHATARNAK taqat hai

> Jo cheez kisi bhi button ko daba sakti hai, wo **paise bhej sakti hai, cheezein
> mita sakti hai, kuch bhi kar sakti hai.** Yahan lagaam P3 se bhi **sakht** chahiye.

| # | Qanoon | Wajah |
|---|---|---|
| 1 | 🚫 **KAALI SOOCHI** — koi bhi button jismein `pay · buy · purchase · delete · uninstall · confirm order · transfer · send money · جیز کیش · easypaisa` ho → **KABHI KHUD NAHI**, hamesha ijazat | ek ghalat tap = paisa gaya |
| 2 | 🏦 **Bank/paisa apps mein HAATH BAND** — bank, wallet, UPI, Play billing screens par agent **ruk jata hai** | |
| 3 | 🔢 **Qadam ki hadd** — ek mission max **15 qadam**, **3 minute** | loop mein na phanse |
| 4 | 🔁 **Chakkar ka pehra** — wohi screen 3 dafa → ruk kar poochho | atak jaye to zid na kare |
| 5 | ⛔ **"RUKO" hamesha jeetta hai** — aap ka ek lafz sab rok deta hai | |
| 6 | 👀 **Har qadam nazar aata hai** — bolti hai + notification + LEDGER | chori-chhupe kuch nahi |
| 7 | 🔴 **Nayi app pehli dafa = ijazat** — "Instagram mein pehli dafa haath laga rahi hoon, theek hai?" | |
| 8 | ⚗️ **DRY-RUN** — "main ye 6 qadam uthati", chalti kuch nahi | naya rasta test karne ko |
| 9 | 🔒 **Screen band = agent band** | jeb mein na chalti rahe |
| 10 | 📜 **Har tap LEDGER mein** + ⟲ `back` se undo | P3 se muft |

**Aur sab se ahem — 🎛️ MASTER SWITCH:**
Accessibility ijazat **aap khud** Android Settings mein dete ho, aur **ek switch se poora
HAATH band** kar sakte ho. Maya kabhi khud ye ijazat nahi maang sakti.

---

# HISSA 6 — ⚠️ IMAANDARI SE: kya MUSHKIL hoga

Ye main chhupaunga nahi:

| Masla | Sach | Ilaj |
|---|---|---|
| **Raftaar** | Pehli dafa har kaam **20–40 sec**. Ye LLM ki raftaar hai, meri nahi | RASTA (doosri dafa 2-3 sec) |
| **Quota** | Har qadam ek LLM call. Gemini free 250/din | RASTA + Groq 1000/din + sirf 🟢 kaam par LLM |
| **Chrome ke andar** | Web ka content accessibility mein aata to hai, magar kabhi kabhi **adhoora** | jab node na mile to **coordinate par tap** (gesture) |
| **App update** | Instagram apna layout badal de to rasta toot jayega | khud ba khud LLM par wapas + naya rasta seekhna |
| **Har app nahi** | Kuch apps (banking) accessibility **block** karti hain | saaf keh degi: "ye app mujhe dekhne nahi deti" |
| **Battery** | Accessibility service hamesha chalti hai | Android ka apna nizam — asar kam hai. Aur switch aap ke haath mein |
| **Kotlin badlega** | ✅ **HAAN — ye P5 ke ilawa wahid jagah hai** | neeche protokol ↓ |

## 🔴 Kotlin ka khaas protokol *(kyunke main compile nahi kar sakta)*

1. **`AutoSendService` ka purana WhatsApp wala kaam BILKUL NAHI chhuunga** — sirf naye
   function jurenge. Purana chalta rahega.
2. **XML config alag commit** mein — build fail ho to sirf wo `git revert`.
3. **JS pehle poochhega** `if (MayaBridge.uiDump)` — na mile to purana raasta.
   **Purani APK bhi nayi index.html ke sath chalti rahegi.**
4. Har naye Kotlin function par brace/import/API scan *(jaisa ab tak har dafa kiya)*.
5. **Aap pehle build karo, phir switch ON.** Build fail = 1 minute mein revert.

---

# HISSA 7 — 📅 QADAM BA QADAM

| | Kya | Kotlin? | Khatra | Aap ko kya milega |
|---|---|---|---|---|
| **P7a** | 👁️ **DEKHO** — screen dump + Settings mein "SCREEN PARHO" button | ✅ chhota | 🟡 | Maya batati hai screen par kya hai. **Kuch chhuti nahi.** |
| **P7b** | ✋ **KARO** — tap/type/scroll/swipe/back + kaali soochi + ijazat | ✅ | 🟡 | *"search par tap karo"* · *"neeche scroll karo"* |
| **P7c** | 🧭 **MISSION** — poora agent loop + live status + "RUKO" | ❌ sirf JS | 🟡 | *"chrome kholo aur lm arena par jao"* |
| **P7d** | 💥 **RASTA** — seekhna aur yaad rakhna | ❌ sirf JS | 🟢 | **doosri dafa 2.5 sec** ← WTF |

**P7a sab se pehle kyun:** wo **kuch chhuti nahi** — sirf **dekhti** hai. Aap apni
aankhon se dekh loge ke Maya screen theek parh rahi hai ya nahi. **Us ke baad hi
usay chhoone ki ijazat dena aqalmandi hai.**

---

# HISSA 8 — 🧪 SABOOT (~70 naye test, kul ~895)

| Hissa | Kya sabit karega | Kitne |
|---|---|---|
| Screen dump | node chhanna · ≤40 element · ≤1200 token · khali screen par crash nahi | 12 |
| Amal | 8 amal ki shakl · ghalat index rad · type se pehle field check | 14 |
| **KAALI SOOCHI** | `pay/buy/delete/confirm order/easypaisa` — **kabhi khud nahi** | **12** |
| Mission loop | 15 qadam ki hadd · 3 min · chakkar ka pehra · "RUKO" foran | 12 |
| **RASTA** | seekhna · dohrana · **toot jaye to LLM par wapas** · ghalat rasta na chale | 12 |
| Kotlin source | naye function maujood · **purana WhatsApp wala kaam salamat** · gestures ON | 8 |

**Aur ek khaas test:** *"agent khud kabhi paise/delete wala button na dabaye"* —
40 khatarnak labels par chalega.

---

# HISSA 9 — 🗺️ P7 ke baad Maya kya hogi

```
✅ samajhti hai      ✅ karti hai        ✅ sach bolti hai
✅ ijazat leti hai   ✅ wapas kar sakti  ✅ dekh sakti hai (camera)
✅ offline chalti    ✅ khud pehchanti (P6)
🆕 AAP KA PHONE KHUD CHALATI HAI — aur raasta YAAD rakhti hai
```

---

# ❓ AB FAISLA AAP KA

**Do sawal hain, aur dono ahem hain:**

### 1️⃣ Tarteeb
| | |
|---|---|
| **A** | **P7 pehle** *(phone control)* — jo aap ne abhi maanga |
| **B** | **P6 pehle** *(khud-mukhtar)* — plan pehle se tayyar, khatra kam |
| **C** | **P6a + P7a ek sath** — dono ki sab se mehfooz teh, ek build |

### 2️⃣ Pehla qadam — **P7a (sirf DEKHNA)** se shuru karun?
> Maya screen **parhegi** magar **chhuegi nahi**. Aap apni aankhon se tasdeeq kar loge
> ke wo Chrome/Instagram theek parh rahi hai — **phir** usay haath lagane ki ijazat denge.
>
> Mera mashwara: **haan.** Kyunke agar wo screen theek nahi parh pati, to tap karna
> khatarnak hai — aur ye hum **pehle** hi jaan lenge.

**Aap ka "haan" aane tak main ek line bhi nahi likhunga.** 👑
