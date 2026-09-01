# 👂 v5.7.0 — KAAN: wake word ab **sach mein** jaagti hai

> **Aap:** *"Background mein mic on/off hota hai, magar 'Maya' bolne par kuch nahi hota."*
>
> **7 bug mile.** Aur sab se bara ye ke **hum bilkul ANDHE the.**

---

## 🚨 BUG 1 — hum andhe the *(sab se bara)*

`WakeWordService` **kabhi nahi** batati thi ke usne **kya suna** ya **kya error** aaya.
Is liye aap ko sirf mic ka on/off dikhta tha — aur kuch nahi. **Debug karna namumkin tha.**

**Ab:** har waqia darj hota hai, aur Settings mein ek button:
```
👂 WAKE WORD ka haal

  switch      : ON
  suna        : 14 dafa
  JAAGI       : 3 dafa
  nakami      : 22   (aakhri: 7 — samajh nahi aaya)
  mic chala   : 39 dafa

  ── aakhri 22 waqiat ──
  22:41:07 ▶️ ur-PK|39
  22:41:05 ⚠️ 7 — samajh nahi aaya  (lagatar 4)
  22:40:58 👂 mera naam | maya suno
  22:40:58 ✅ maya suno  #2
```

## 🚨 BUG 2 — `isWake` bana kar **chhor diya** jata tha

```kotlin
val isWake = t.contains("maya") || …     // ← dead variable, kabhi use hi nahi hua
```
`grep isWake` → **sirf ek line, koi istemal nahi.**

## 🚨 BUG 3 — Urdu **کبھی** match ho hi nahi sakta tha

```kotlin
t.contains("\\u0645\\u0627\\u06CC\\u0627")
//          ^^ DOUBLE backslash — ye Urdu harf NAHI, literal matn hai
```

## 🚨 BUG 4 — `EXTRA_MAX_RESULTS = 1`

Sirf **pehla** andaza. **Magar SUNO (v4.11.0) ne hamein sikhaya tha ke sahih jawab
aksar doosre ya teesre andaze mein hota hai** — aur wake word par ye aur ahem hai.

**Ab 6 andaze**, aur **har ek** mein wake word dhoonda jata hai.
*(Test: wake word teesre andaze mein mila → pakra gaya ✅)*

## 🚨 BUG 5 — zubaan `"en-IN"` hard-code

Aap ka `stt: ur-PK` hai. **Aap Urdu bolte ho, wo Angrezi sun rahi thi.**
Ab zubaan **settings se** jati hai.

## 🚨 BUG 6 — **yehi "mic on/off" ki wajah thi**

```kotlin
6, 7 -> restart(250)        // NO_MATCH par sirf 250ms baad dobara
```
Android 11+ background mic ko **throttle** karta hai. Itni tez restart par Google ka
recognizer **chup ho jata hai** — mic on hota hai, band hota hai, aur kuch nahi hota.

**Ab backoff:** `0.7s → 3.5s` jaise jaise lagatar nakami barhti hai.

## 🚨 BUG 7 — matching bohat sakht

Sirf `maya / mya / boss`. Ab: `maiya · mahiya · mayaa · my a · maja · مایا · माया` —
aur *"maya brightness barhao"* **ek hi saans mein** kaam karta hai.

---

## 🏗️ Aur ek bara faisla — **Kotlin ab BEWAQOOF hai**

```
PEHLE:  Kotlin faisla karta tha (aur ghalat karta tha)
AB:     Kotlin sirf SAARE andaze JS ko de deta hai
        Faisla, matching, aur log — sab JS mein
```

> **Faida:** aage wake word ki tuning ke liye **nayi APK nahi banani paregi.**
> Sirf `index.html` badlega.

---

## 🧪 Saboot
```
✅ CSS PASS · Settings 72 · AWAAZ 294 · DIMAAG 155 · 🧪 LAB 368
──────────────────────────────────────────────────────────────
                                              889 / 889
```
Section 24 mein **29 naye** — Urdu/Devanagari matching, fuzzy, **teesre andaze wala
test**, log, backoff, aur **saaton Kotlin bug ke taale**.
*(Aur `SAFE MODE` — app band ho to kuch na karna — bilkul salamat.)*

---

## ▶️ Test karo

1. Release → tag **`v5.7.0`** (versionCode 67)
2. **Wake word OFF karo, phir ON** *(zubaan ki nayi setting jane ke liye)*
3. Maya ko background mein chhoro *(band mat karo — swipe away na karo)*
4. Bolo: **"Maya"** … thoro rukro … phir **"Maya brightness barhao"**
5. Settings → LAB → **👂 WAKE WORD KA HAAL**

### 🎯 Ab jawab milega — chahe kaam kare ya na kare

| Report kya kehti hai | Matlab |
|---|---|
| `suna: 0 · mic chala: 0` | service chal hi nahi rahi → mic permission ya battery optimization |
| `suna: 8 · JAAGI: 0` | **sun rahi hai magar "Maya" pehchan nahi rahi** → mujhe wo lines bhejo, main matching theek kar dunga *(bina APK ke!)* |
| `nakami: 40 · lagatar 8` | Android throttle kar raha hai → backoff aur barhana parega |
| `JAAGI: 3` | ✅ **chal raha hai** |

**Diagnostic ke sath ye report bhi bhejna — ab hum andhe nahi hain.**
