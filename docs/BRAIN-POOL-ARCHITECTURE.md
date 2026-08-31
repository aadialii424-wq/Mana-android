# 🧠🔥 BRAIN POOL — MAYA v4.4.0

> **Ek jumle mein:** MAYA ab ek dimaag par nahi chalti. Uske paas **10 dimaag** hain,
> jinme se **2 aise hain jinhe kisi key ki zaroorat hi nahi**. Is liye "quota khatam"
> ab app ke rukne ki wajah nahi rahi.

---

## 1. Masla kya tha (asal jarh)

v4.3.0 ke DIMAAG ENGINE ne *jhooti* nakami khatam kar di thi — messages sache ho gaye the.
Magar user ne Groq ki key daalne ke baad bhi ye dekha:

> *"Aaj ka free Gemini quota khatam… Backup brain bhi thak gaya."*

Tehqeeq se do alag wajahen nikleen:

### 🔴 J1 — Groq ke models mar chuke the
Groq ne **16 August 2026** ko `llama-3.3-70b-versatile` aur `llama-3.1-8b-instant`
**band** kar diye (Groq deprecations page). Hamara default Groq model wahi tha.
Natija: Groq ki key bilkul theek hone ke bawajood **har call fail**.
User ko lagta tha key kaam nahi kar rahi — asal mein model ka janaza ho chuka tha.

### 🔴 J2 — do provider ki qaid
Gemini (250–1500 req/din) + Groq. Dono ke quota ek hi din mein khatam ho sakte hain,
aur agar **koi key hi na ho** to app ke paas kuch nahi bachta tha.

**Sabaq:** provider list ko **hard-code karna** hi bug hai. Provider marte rehte hain,
quota khatam hote rehte hain. Structure aisa chahiye jo isse *farz* kar ke chale.

---

## 2. Structure — teen tehen

```
                 ┌───────────────────────────────────────────┐
   SAWAL  ──────▶│              BRAIN.plan()                 │
                 │  har provider × har key, behtareen pehle   │
                 └───────────────────┬───────────────────────┘
                                     │
   ┌─────────────────────────────────┴─────────────────────────────────┐
   │ TEH 1 — KEYED (jitni keys, utna quota)                            │
   │   💎 Gemini   ⚡ Groq   🚀 Cerebras   🇪🇺 Mistral                  │
   │   🔀 OpenRouter   🐙 GitHub   🟩 NVIDIA   🇨🇳 Z.ai                 │
   └─────────────────────────────────┬─────────────────────────────────┘
                                     │ sab thak gaye?
   ┌─────────────────────────────────┴─────────────────────────────────┐
   │ TEH 2 — KEYLESS (koi signup nahi, hamesha zinda)                  │
   │   🆓 LLM7.io          🌸 Pollinations                              │
   └─────────────────────────────────┬─────────────────────────────────┘
                                     │ internet hi nahi?
   ┌─────────────────────────────────┴─────────────────────────────────┐
   │ TEH 3 — LOCAL (waqt, tareekh, hisab, yaad-dasht)                  │
   └───────────────────────────────────────────────────────────────────┘
```

Har teh se guzar kar hi MAYA "nahi ho saka" kehti hai — aur tab bhi wo **batati hai
ke kaun sa qadam uthana hai**, sirf maazrat nahi karti.

---

## 3. Teen chaabiyan jo "unlimited" banati hain

### 🔑 Chaabi 1 — kai keys, ek khana
Har key field ab **kai keys** leta hai (comma ya nayi line se alag):

```
gsk_pehli_key, gsk_doosri_key
gsk_teesri_key
```

`BRAIN.keys()` inhe alag karta hai; `BRAIN.plan()` **har key ko alag koshish** banata hai.
Gemini ka quota *per-project* hota hai → 3 alag Google accounts ki 3 keys = **3 guna quota**.
Ek key ka quota khatam hote hi agli key khud chal jati hai; user ko pata bhi nahi chalta.

### 🔑 Chaabi 2 — model auto-discovery (deprecation-proof)
Jab kisi provider ka jawab kehta hai *"model not found / decommissioned / deprecated"*:

1. `BRAIN.dropModel()` us model ko list se nikal deta hai,
2. `BRAIN.discover()` provider ke `/models` endpoint se **zinda models** ki nayi list leta hai,
3. nayi list `localStorage` mein mehfooz ho jati hai.

Yaani **16 August wala hadsa dobara nahi hoga** — app khud ko theek kar legi, bina update ke.

### 🔑 Chaabi 3 — keyless farsh
LLM7.io (`api_key: "unused"`) aur Pollinations (`text.pollinations.ai/openai`)
**bina kisi account ke** chalte hain. Is liye ek bilkul naya phone, bilkul khaali Settings,
pehle hi sawal par jawab deta hai. `BRAIN.liveCount()` kabhi 0 nahi hota jab tak internet hai.

---

## 4. Andar kya hai

### `BRAINS` registry
```js
{ id, name, icon, kind:"gemini"|"openai", url, modelsUrl, models[],
  keyField, keyless, tools, pri, free, signup }
```
Naya provider add karna = **is array mein ek line**. Baqi sab khud chalta hai:
plan, cooldown, Settings ka status panel, Doctor ki report.

| # | Dimaag | Muft kya milta hai | Key kahan se |
|---|--------|--------------------|--------------|
| 1 | 💎 Gemini | 15/min · 250–1500/din · **tools** | aistudio.google.com |
| 2 | ⚡ Groq | 30/min · 1000/din har model | console.groq.com |
| 3 | 🚀 Cerebras | ~10 lakh token roz | cloud.cerebras.ai |
| 4 | 🇪🇺 Mistral | ~1 arab token mahina | console.mistral.ai |
| 5 | 🔀 OpenRouter | 20/min · 50/din (`:free`) | openrouter.ai |
| 6 | 🐙 GitHub Models | 15/min · 150/din | github.com/settings/tokens |
| 7 | 🟩 NVIDIA NIM | ~40/min · 120+ models | build.nvidia.com |
| 8 | 🇨🇳 Z.ai GLM | GLM Flash muft | z.ai |
| 9 | 🆓 **LLM7** | **koi key nahi** — 10/min | (marzi se) token.llm7.io |
| 10 | 🌸 **Pollinations** | **koi key nahi, koi signup nahi** | — |

### Cooldown jo yaad rehta hai
`localStorage["maya_brainpool"]` mein har *(provider, key)* ka cooldown mehfooz hai:

| Wajah | Kitni der | Kyun |
|-------|-----------|------|
| `QUOTA` (minute wala) | 90 sec | thori der mein khud khul jata hai |
| `QUOTA` (din wala) | 60 min | aaj ka kaam khatam, bar bar chherna bekaar |
| `KEY_BAD` | 30 min | key theek hone tak bekaar koshish na ho |
| `SERVER` | 2 min | provider ka apna masla |

App band kar ke kholne par bhi ye yaad rehta hai — is liye subah wala khatam quota
raat tak bar bar nahi chhera jata.

### Transport — APK ka superpower
```
MayaBridge.httpPostAsync(url, auth, body, reqId, timeoutMs)
        └─ alag Thread ─▶ window.__httpDone(reqId, status, base64Body)
```
APK ke andar har request **Kotlin se** jati hai, WebView se nahi:

* **CORS bilkul lagoo nahi hota** → wo providers bhi chal jate hain jo browser se block hain,
* UI **kabhi jam nahi hoti** (purana sync `httpPost` bridge fallback ke taur par maujood hai),
* jawab Base64 mein aata hai → Urdu/Hindi text kabhi kharab nahi hota
  (`decodeURIComponent(escape(atob(b64)))`).

Browser/PWA mein wahi kaam `XMLHttpRequest` karta hai.

---

## 5. Ek sawal ka safar

```
sawal
  │
  ├─ CACHE?  6 ghante purana wahi sawal → 0 request, turant jawab
  │
  ├─ BRAIN.plan(needTools)
  │     tools chahiye → Gemini sab se upar
  │     warna → pri: Gemini 5 · Groq 10 · Cerebras 12 · Mistral 30 …
  │     cooldown wale bilkul list mein nahi aate
  │
  ├─ 4 keyed koshishein  ──── kamyab? → jawab + us dimaag ko azaad karo
  │        │ har nakami: classify() → cooldown → agla
  │        ▼
  ├─ SAB keyless koshishein (kabhi chhori nahi jatin)
  │        │
  │        ▼
  ├─ LOCAL jawab mumkin hai? (waqt/hisab/yaad) → foran do, intezaar mat karao
  │        │
  │        ▼
  ├─ waqti masla (QUOTA/SERVER/NETWORK)? → 1 dafa khud-ba-khud dobara, 5–45s baad
  │        │
  │        ▼
  └─ sacha paigham: kya hua + user kya kare (7 alag paigham, ek jumla nahi)
```

---

## 6. Paighamat — ab har paigham kaam ka hai

| Halat | MAYA kya kehti hai |
|-------|--------------------|
| Koi dimaag zinda nahi | "Mere sab 10 dimaag abhi band hain… Groq (console.groq.com) sab se tez hai, Cerebras roz 10 lakh token deta hai. **Ek key = poora dimaag wapas.**" |
| Din ka quota khatam | "…Do minute ka pakka ilaj: Cerebras ya Mistral ki free key daal do. **Ya kisi bhi key ke saamne comma laga kar doosri key daal do — quota dugna ho jata hai.**" |
| Sab thak gaye, dobara koshish | "Sab 6 dimaag thak gaye — 30s baad KHUD dobara try karungi" |

Header ka pill bhi ab sach bolta hai: **`AI READY • 6 DIMAAG`**.

---

## 7. Settings mein kya naya hai

* **7 naye key khane**: Groq, Cerebras, Mistral, OpenRouter, NVIDIA, Z.ai, LLM7 —
  har ek par likha hai *muft kya milta hai* aur *key kahan se milegi*.
* Upar live ginti: **"🧠 BRAIN POOL — 6 dimaag zinda"**, aur yaad dilata hint ke
  har khane mein kai keys daal sakte ho.
* **🧠 BRAIN POOL DEKHO** button → poori list: har dimaag, har key, LIVE ya kitne
  second ka cooldown, aur uska free tier.
* Purana "GROQ MODEL" dropdown **nikal diya gaya** — wahi to mara hua model chun raha tha.
  Ab model app khud chunti hai aur khud badalti hai.
* Doctor report mein bhi poora pool table chhapta hai.

---

## 8. Saboot — `tools/test-brain-engine.js` (155 test)

Naye 8 baab, jo BRAIN POOL ke har dawe ko pakadte hain:

| Baab | Kya sabit karta hai |
|------|---------------------|
| 15. REGISTRY | 10+ dimaag, 2 keyless, **Groq ke mare hue llama models kahin nahi** |
| 16. KAI KEYS | `"a, b\nc"` → 3 alag koshishein; quota wali key plan se nikal jati hai |
| 17. COOLDOWN | localStorage mein mehfooz; **restart ke baad bhi yaad** |
| 18. KEYLESS FLOOR | **bilkul khaali Settings** par bhi jawab; Gemini ko chhera tak nahi |
| 19. POOL WALK | Groq gira → Cerebras ne uthaya; gire hue ko cooldown, kamyab azaad |
| 20. MARA HUA MODEL | `dropModel()` + auto-discovery se khud-marammat |
| 21. AUTH + PAYLOAD | `Bearer` theek, system message pehle, gemini ka `model` role kabhi nahi |
| 22. STATUS | LIVE / NO_KEY / QUOTA — Settings aur Doctor ko sach dikhta hai |
| 23. SOURCE | dead model IDs, purana `openaiCompatChat`, dono ki wapsi par test fail |

Poora `npm test`: **CSS PASS · 45/45 Settings · 118/118 AWAAZ · 155/155 DIMAAG+POOL**

---

## 9. Aage naya dimaag kaise joden

1. `BRAINS` array mein ek object add karo (`id`, `url`, `models`, `keyField`, `pri`).
2. Settings mein ek `ui-field` + load/save ki ek line.
3. `tools/test-brain-engine.js` ke baab 15 mein uska id add kar do.

Bas. Plan, cooldown, rotation, status panel, Doctor — sab khud sambhal lete hain.
