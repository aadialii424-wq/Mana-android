# 🧠 MAYA — DIMAAG ENGINE ARCHITECTURE (v4.3.0)

> Shikayat: **"Sab free brains is waqt busy hain, Sir"** bar bar aata tha.
> Ye us jumle ki maut ka certificate hai.

---

## 0. Asal wajah — 3 bugs, ek doosre ko khila rahe the

### B1 — 400 ko "key ka masla" samjha jata tha
```js
if (res.status === 400 || res.status === 403 || /api key/i.test(errStr)) {
  geminiChat.keyErr = true;
  geminiBadUntil = Date.now() + 600000;    // 10 MINUTE ka blackout
  return null;
}
```
HTTP **400 = kharab request** hoti hai (history, tool schema, payload) — key ka masla `401`
ya `403 PERMISSION_DENIED` ya message `"API key not valid"` hota hai. Purana code ek
kharab request par poore Gemini ko **10 minute** ke liye band kar deta tha.

### B2 — history ka pehla turn "model" ho jata tha (400 ki jarh)
```js
var contents = chatHist.slice(-8).map(...)
```
`chatHist` alternate hoti hai `[u, m, u, m, u, m, u, m, u]`. 4 baat-cheet ke baad
`slice(-8)` **`model` se shuru** hoti hai — Gemini multi-turn history `user` se shuru
maangta hai → **400** → (B1) → 10 minute blackout → Groq key na ho to seedha
*"Sab free brains busy"*.

**Isi liye ye "bar bar" aata tha: shuru mein chalta tha, 4 messages ke baad hamesha.**

### B3 — ek sawal = 5 requests
`resolveModels()` 5 model deta tha aur har ek par poori koshish hoti thi. Free tier ka
quota **per-project** hai — 5 model try karna quota **5 guna tez** khatam karta hai.

### B4 — ek jumla, 5 wajahein
Key nahi / key kharab / quota / offline / server down — sab ka jawab wahi ek line.
User ko kabhi pata nahi chalta ke karna kya hai.

---

## 1. ASOOL

1. **Har nakami ka naam** — `KEY_MISSING`, `KEY_BAD`, `QUOTA`, `QUOTA_DAY`, `BAD_REQUEST`, `MODEL_404`, `SERVER`, `NETWORK`, `TIMEOUT`, `EMPTY`, `OFFLINE`.
2. **Saza jurm ke barabar** — sirf **asli** key masla blackout karta hai. 400 par blackout **nahi**.
3. **Khud marammat** — 400 aaye to history saaf kar ke, bina tools, ek dafa dobara. Chal gaya? Bug khatam.
4. **Quota izzat se** — Google jitna waqt maange utna cooldown (`retryDelay`), per-day quota alag se pehchano aur din bhar Gemini ko na chhero.
5. **Bekaar request kabhi nahi** — offline, key nahi, blackout, cooldown → network call hi mat karo.
6. **Khali haath nahi** — sab fail? pehle local jawab (waqt, tareekh, hisab, yaad-dasht), warna **asli wajah + agla qadam**.
7. **Badge sach bole** — `AI ONLINE` sirf tab jab aakhri 15 minute mein **asli jawab** aaya ho.

---

## 2. LADDER

```
askAI()
  │
  ├─ 0. CACHE          6 ghante purana wahi sawal → 0 request
  ├─ 1. TURBO GROQ     simple baat + groqKey → instant
  ├─ 2. 🧠 GEMINI      max 2 model, tools ke sath
  │        └─ 400 → khud marammat (history saaf + bina tools) → dobara
  ├─ 3. 🦙 GROQ        backup (APK mein MayaBridge.httpPost se, CORS nahi)
  ├─ 4. 🐙 GITHUB      backup 2
  ├─ 5. AUTO-RETRY     sirf waqti masla (QUOTA/SERVER/NETWORK/EMPTY) par, Google ke bataye waqt par
  ├─ 6. LOCAL BRAIN    waqt · tareekh · hisab · yaad-dasht
  └─ 7. SACH           asli wajah + agla qadam (7 alag paigham)
```

`verdict()` kai providers ki nakamiyon mein se **ahem tareen** masla chunta hai, taake
"Groq key nahi hai" jaisi mamooli baat asli `SERVER`/`QUOTA` masle ko na dhake.

---

## 3. User ko kya dikhta hai (ek jumle ki jagah 7)

| Code | Paigham (khulasa) |
|---|---|
| `KEY_MISSING` | "Settings → API KEYS mein FREE Gemini key daal do — aistudio.google.com" |
| `KEY_BAD` | "Key kaam nahi kar rahi — nayi banao. Ya Groq ki free key daal do" |
| `QUOTA_DAY` | "**Aaj ka** free quota khatam — Groq ki free key alag quota deti hai" |
| `QUOTA` | "Ek minute ke liye bhar gaya — main khud dobara koshish kar rahi hoon" |
| `SERVER` | "Google ka server kharab hai — aapki koi ghalti nahi" |
| `BAD_REQUEST` | "History ka masla — Settings → CHAT HISTORY CLEAR" |
| `NETWORK`/offline | "Internet nahi — local commands chal rahe hain" |

Header pill: `NO KEY` · `AI READY` · `AI ONLINE • GEMINI` · `AI BUSY` · `QUOTA KHATAM` · `KEY MASLA` · `OFFLINE`.

Doctor (`Settings → DOCTOR`) mein poora record: har provider ki koshish, error code, Google ka message, cooldown, khud-marammat ki ginti, kul requests.

---

## 4. Saboot — `tools/test-brain-engine.js` (109 assertions)

| Section | Kya sabit karta hai |
|---|---|
| 1 | B2 — leading `model` turn hat jata hai, roles alternate, khali turns nikal jate hain |
| 2 | B1 — 400 par **koi blackout nahi**, khud marammat chalti hai; 9 status codes ki sahi tashkhees |
| 3 | B3 — ek sawal par **max 2** request |
| 4 | `retryDelay` Google ke jawab se; per-day quota alag |
| 5 | Sirf asli key masla blackout karta hai |
| 6 | Offline/no-key par **0 network call** |
| 7 | Tools zinda; tool crash par bhi jawab |
| 8 | Gemini → Groq → GitHub ladder |
| 9 | **7 alag paigham**, aur `"Sab free brains is waqt busy hain"` source se hi ghayab |
| 10 | Local brain: waqt, tareekh, hisab, yaad-dasht |
| 11 | Auto-retry sirf waqti masle par; `verdict()` asal masla chunta hai |
| 12 | Cache |
| 13 | Header pill jhoot nahi bolta |
| 14 | Doctor report |
| 15 | Purane bug ka code source mein wapas na aa jaye |

```
npm test
  ✅ CSS CHECK PASS
  ✅ 45/45    tools/test-settings-ui.js
  ✅ 118/118  tools/test-voice-engine.js
  ✅ 109/109  tools/test-brain-engine.js
```

---

## 5. Agar phir bhi masla aaye — 3 second ka tashkhees
1. Header pill parho → `QUOTA KHATAM` / `KEY MASLA` / `OFFLINE` seedha bata dega.
2. Settings → **DOCTOR** → `DIMAAG ENGINE v2` block mein har koshish ka natija.
3. Jawab khud bata dega ke **agla qadam kya hai** — andaza lagane ki zaroorat nahi.

**Sab se tez ilaj:** Groq ki free key (console.groq.com) Settings mein daal do — alag
quota, alag company, aur `llama-3.3-70b` foran jawab deta hai.
