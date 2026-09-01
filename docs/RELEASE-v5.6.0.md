# 👁️ v5.6.0 — P7a: NAZAR (Maya SCREEN parh sakti hai)

> **P7 ka pehla qadam.** Maya screen **DEKHTI** hai — **CHHUTI NAHI**.

---

## 🔬 Forensic ne jo nikala tha

`AutoSendService.kt` **pehle se** ek asli `AccessibilityService` thi — `rootInActiveWindow`,
`performAction(CLICK)`, `performGlobalAction(BACK)` sab maujood. Bas **teen line XML** ne
use WhatsApp tak mehdood kar rakha tha.

## ✅ Ab wo teen line badal gayin

```xml
android:packageNames="com.whatsapp"        →  HATA di   (ab har app)
android:accessibilityFlags="flagDefault"   →  flagDefault|flagReportViewIds
<!-- canPerformGestures gayab -->          →  canPerformGestures="true"
```
*(Gestures ON kar diye taake **P7b par dobara permission na maangni pare** — magar
is release mein **istemal nahi** hote.)*

---

## 👁️ NAZAR kaise kaam karti hai

```
Kotlin  dumpScreen()      →  tree par chalo, motay tor par chhaano
                             (sirf nazar aane wale · dabne/likhne/scroll hone wale)
JS      NAZAR.compact()   →  DOOSRI chhanti (dohre hataao, jama karo, kaato)
                             ≤40 element · label ≤52 harf
```

**Kyun dohri chhanti?** Chrome ke ek page mein **500+ node** hote hain. Poora tree
dimaag ko bhejenge to prompt phat jayega — *(CHHED 9 ka sabaq: 33 tools ka JSON hi
1989 token kha jata tha)*. Test mein sabit: **120 element → 40, ~204 token.**

### Aap ko kya dikhega
```
👁️ SCREEN PAR ABHI — Chrome

  ✏️ [0] Search or type URL      #url_bar
  🔘 [1] Search                   ×2
  🔘 [2] New tab                  #tab_switcher
  ·  [3] Agent Arena | AI Agent Performance Leaderboard
  ↕️ [4] (bay-naam)

  5 cheezein dikhayi  (kul 7)

  🔒 Maya ne kuch CHHUA nahi — sirf DEKHA hai.
```
Har element ka **apna number** — P7b mein dimaag isi number se tap karega.

---

## 🔒 Hifazat — is release mein Maya **kuch chhu hi nahi sakti**

| | |
|---|---|
| **Test ka taala** | `dumpScreen()` ke andar **`performAction` hai hi nahi** |
| 🟢 **SABZ darja** | `read_screen` sirf parhta hai |
| 🤝 **SACH** | `state: "info"` — koi amal ka daawa nahi |
| 🔒 **Qanoon 2** | **purana WhatsApp AutoSend bilkul salamat** *(test se locked)* |
| 🔇 **Switch** | LAB → 👁️ NAZAR — OFF ho to tool saaf mana kar deta hai |
| 🚧 **Hadd** | tree par 22 darje tak, element ki ginti par cap — bara page app jam nahi karega |

---

## 🆕 Naya tool: `read_screen` *(35 → 36)*

Dimaag khud bhi bula sakta hai:
> *"screen par kya hai"* · *"kaunse buttons hain"* · *"kya dikh raha hai"*

---

## ⚠️ **ZAROORI — ek dafa ye karna parega**

Service ka config badla hai. Android **naye config ko tab tak nahi maanta** jab tak
service dobara na jori jaye:

```
Settings → Accessibility → MAYA AutoSend → OFF  →  phir ON
```
*(Agar pehle se ON thi tab bhi.)* App khud bhi ye batati hai agar screen na parh paye.

---

## 🧪 Saboot
```
✅ CSS PASS · Settings 72 · AWAAZ 294 · DIMAAG 155 · 🧪 LAB 339
──────────────────────────────────────────────────────────────
                                              860 / 860
```
Section 23 mein **35 naye** — chhanti, jama karna, label kaatna, 40 ki hadd,
token budget, nakami par sach, **aur `dumpScreen` mein `performAction` ka na hona**.

---

## ▶️ Test karo — **yehi faisla-kun qadam hai**

1. Release → tag **`v5.6.0`** (versionCode 66)
2. **Settings → Accessibility → MAYA AutoSend → OFF → ON** ⚠️
3. LAB → **👁️ NAZAR ON**
4. **Chrome kholo** *(ya Instagram)* → Maya par wapas aao → LAB →
   **👁️ SCREEN ABHI PARHO**
5. Dekho: kya wo **sach much** screen ke buttons aur khane theek dikha rahi hai?

### 🎯 Faisla is par hai
| Natija | Agla qadam |
|---|---|
| ✅ Screen theek parh rahi hai | **P7b — usay haath lagane ki ijazat** *(tap/type/scroll)* |
| ⚠️ Adhoora ya ghalat parh rahi hai | pehle **chhanti theek** karenge — tap karna abhi khatarnak hai |

**Diagnostic bhejo, aur ho sake to `👁️ SCREEN ABHI PARHO` ka natija bhi.**
