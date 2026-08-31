# ⚡ AMAL ENGINE — Maya ko "batane wali" se "karne wali" banane ka naqsha

> **Halat:** Maya ke paas **44 tools** hain — magar **10 dimaagon mein se sirf 1** unhen chala sakta hai.
> Jab wo ek dimaag thak jata hai, Maya **jhoot bolne lagti hai**: kaam karne ke bajaye
> kaam ka *bayan* likh deti hai.
>
> Ye dastavez pehle **tashkhees** karti hai (code se, andaze se nahi), phir **ilaj ka naqsha**.
> Ye plan hai — code nahi. Manzoori ke baad hi haath lagega.

---

# HISSA 1 — TASHKHEES: aap ke screenshot mein 7 bug hain

## 🔴 BUG 1 — Router "brightness" ko kaam samajhta hi nahi *(smoking gun)*

`public/index.html:1208`
```js
var ACTION_WORDS = /(alarm|timer|reminder|call|phone|whatsapp|message|sms|open|khol|
chalao|play|baja|download|file|pdf|photo|screenshot|search|khojo|dhoond|wiki|wikipedia|
fetch|yaad rakh|diary|battery|mausam|weather|share|youtube|google|average|nikalo|
calculate|convert|kitna|kitne)/i;
```

Chala kar dekha:

| Jumla | Nateeja |
|---|---|
| `Britness barhao 100%` | ❌ **BAAT** (kaam nahi) |
| `brightness barhao` | ❌ **BAAT** |
| `torch on karo` | ❌ **BAAT** |
| `alarm laga do` | ✅ TOOL |

`torch_control` · `volume_control` · `brightness_control` **teeno** tool list mein hain,
system prompt mein bhi hain (`(4g) Phone: torch_control/volume_control/brightness_control`)
— **magar router ki lughat mein nahi.** Yani `needsTools() = false`.

**Aur is ka natija?** `BRAIN.plan(needTools)` mein:
```js
if (needTools && p.tools) pri -= 100;     // <- ye kabhi nahi chala
```
Gemini (wahid tool-wala dimaag) ko tarjeeh **mili hi nahi**.

---

## 🔴 BUG 2 — 9 mein se 9 dimaagon ko tools milte HI nahi

`BRAIN.ask()` ka poora payload:
```js
var payload = JSON.stringify({
  model: model, messages: msgs, temperature: 0.7, max_tokens: 320, stream: false
});
```

**`tools` ka naam-o-nishan nahi.** Aur `BRAINS` list mein:
```
$ grep -n "tools:true" public/index.html
4133:  { id:"gemini", ... tools:true, ... }        <- BAS. Sirf ek.
```

Halanke **Groq, Cerebras, Mistral, OpenRouter, NVIDIA, GitHub Models — sab
OpenAI-shakl function calling support karte hain.** Bas jorna bhool gaye.

---

## 🔴 BUG 3 — Har dimaag ko un tools ka hukm milta hai jo uske paas hain hi nahi

`sysPrompt()` **har** dimaag ko bhejta hai:
> *"(4) Kaam aksar tools se karo, sirf baat mat karo... (4g) Phone: torch_control/volume_control/brightness_control"*

To ek tool-less dimaag kya kare? Wo **naqal** karta hai:

```
brightness_control(level=100)
```

**Yehi aap ke screenshot mein likha hai.** Wo hallucination nahi — wo **farmabardari** hai.
Hum ne use hukm diya jise wo poora kar hi nahi sakta tha.

---

## 🔴 BUG 4 — `<think>` kabhi saaf nahi hota *(sab se sharmnaak)*

```
$ grep -n "</think>" public/index.html
(kuch nahi)
```

`cleanSpeech()` markdown, emoji, URL sab hataata hai — **`<think>` nahi**.
`addBubble()` seedha `esc(text)` dikha deta hai.

Aaj ke muft models (`gpt-oss-120b`, `qwen3`, `deepseek-r1`) **default par** apni poori
soch `<think>…</think>` mein likhte hain. Natija:
- 🖥️ Poori soch **bubble mein**
- 🔊 Poori soch **bol kar sunai** gayi (screenshot mein 🔊 laga hua hai)

---

## 🔴 BUG 5 — 320 token: soch mein hi khatam

```js
max_tokens: 320                    // BRAIN.ask
maxOutputTokens: 280               // geminiTry
```

Reasoning model pehle **soch** likhta hai. 320 token us soch mein khatam.
Screenshot dekhiye — **pehle `<think>` ka `</think>` hai hi nahi.** Wo beech mein kata hua hai.

Aur `finish_reason: "length"` ko koi nahi dekhta — kata hua kachra `out` ban kar
seedha screen par aa jata hai. **"(soch rahi hoon) me stuck thi"** ki yehi wajah hai.

---

## 🔴 BUG 6 — Do zubanon ka hukm aapas mein larta hai

| Kahan | Hukm |
|---|---|
| `RULES (3)` | *"User ki language aur script mein jawab: Roman Urdu → Roman Urdu"* |
| `LANGUAGE RULE` (baad mein lagta hai) | *"Reply in Hindi (Devanagari script)"* |

Model ne baad wala maana → aap ko **Devanagari** mila. Do mutazad hukm = model ka
qusoor nahi, hamara design ka.

---

## 🔴 BUG 7 — Jhoot pakarne wala koi nahi

Maya boli: **"चमक 100 पर सेट कर दी है, बॉस।"**
Haqeeqat: **kuch nahi hua.**

Kyunke koi qanoon nahi hai jo kahe: *"kamyabi ka daawa sirf tab jab tool ne `ok` diya ho."*

### 🧬 Poori zanjeer (screenshot ka forensic)

```
"Britness barhao 100%"
   └─ needsTools() = FALSE            (BUG 1: lughat mein lafz nahi)
      └─ Gemini ko tarjeeh nahi mili; wo quota mein tha → pool ka dimaag aaya
         └─ BRAIN.ask() ne tools bheje hi nahi   (BUG 2)
            └─ magar prompt ne tools ka hukm diya (BUG 3)
               └─ model ne <think> likha… 320 token khatam (BUG 5) → kata hua
                  └─ kisi ne <think> saaf nahi kiya (BUG 4) → screen + AWAAZ
                     └─ model ne tool ka naam MATN mein likha; koi parser nahi
                        └─ Devanagari mein jhoota confirmation (BUG 6 + 7)
```

**Ek bhi cheez "AI ki ghalati" nahi thi. Saat jagah hamara dhancha adhoora tha.**

---

# HISSA 2 — ILAJ KA NAQSHA: 10 tehen

## 🧱 TEH 1 — UNIVERSAL TOOL LAYER (ek sach, teen zubanein)

Aaj: tool list sirf Gemini ki shakl mein maujood hai.
Kal: **ek** canonical registry, teen tarjume:

```
TOOLS[]  (ek jagah — naam, tafseel, args, trigger lafz, khatra ka darja)
   ├─ toGemini()  →  { functionDeclarations: [...] }
   ├─ toOpenAI()  →  { tools: [{type:"function", function:{...}}] }
   └─ toText()    →  prompt mein likha hua protocol (neeche TEH 2)
```

**Faida:** tool chalane wale dimaag **1 → 7** ho jayenge (Groq, Cerebras, Mistral,
OpenRouter, NVIDIA, GitHub — sab function calling jante hain).

---

## 🧱 TEH 2 — AMAL PROTOCOL (jo dimaag function-calling nahi jante)

Un ke liye ek sakht, aasan protocol:

```
‹AMAL›{"tool":"brightness_control","args":{"level":100}}‹/AMAL›
```

- Ek sakht parser yehi shakl qubool karta hai → **sach much chalata hai** → nateeja wapas deta hai.
- Ek narm "rescue" parser purani ghalat shakl bhi pakarta hai:
  `brightness_control(level=100)` → **wohi jo aap ke screenshot mein tha, ab CHALEGA.**

**Yani jo aaj jhoot tha, wo kal amal ban jayega.**

---

## 🧱 TEH 3 — SAAF-KARO (soch kabhi bahar na aaye) — *ye ghair-mashroot hai*

Ek hi darwaza, bubble aur awaaz **dono** se pehle:

| Shakl | Kis model ki |
|---|---|
| `<think>…</think>` · `<thinking>` · `<reasoning>` | qwen, deepseek-r1 |
| `<\|channel\|>analysis … <\|message\|>` | gpt-oss (harmony) |
| **band na hua** `<think>` (kata hua) | 320-token wala hadsa |
| `Here's a thinking process:` · `1. **Analyze User Input:**` | plain-text soch |
| `‹AMAL›…‹/AMAL›` | hamara apna protocol |

**Qanoon:** saaf karne ke baad kuch na bache → jawab ko **EMPTY** samjho aur
**agle dimaag** par jao. Khaali bubble kabhi nahi.

---

## 🧱 TEH 4 — ROUTER ko aankhein do (lughat tools se KHUD banegi)

Hath se likhi regex hamesha peeche reh jayegi — yehi BUG 1 tha.
Ilaj: **har tool apne trigger lafz khud lekar aayega:**

```
brightness_control:
  triggers: [brightness, britness, brightnes, chamak, roshni, screen light,
             raushni, barhao, kam karo, dim, bright]
```
`ACTION_WORDS` **in hi se ban** jayega.

**Aur ek test taala laga dega:** har tool kam az kam ek jumle se pakra jana chahiye.
Naya tool banao aur triggers bhool jao → **test fail**. BUG 1 dobara mumkin nahi.

*(Saath: "britness" jaisi aam ghaltiyon ke liye halka fuzzy match.)*

---

## 🧱 TEH 5 — TOKEN BUDGET (soch ko jagah do, ya band karo)

| Model ki qism | Aaj | Naya |
|---|---|---|
| Aam chat | 320 | 400 |
| **Reasoning** (`gpt-oss`, `qwen3`, `r1`) | **320 ☠️** | **1400** + `reasoning_effort:"low"` |
| Gemini 2.5/3 | 280 | 400 (`thinkingBudget:0` pehle se hai ✅) |

Aur `finish_reason:"length"` par kata hua matn **kabhi na dikhao** — ya to jari rakho ya agla dimaag.

---

## 🧱 TEH 6 — SACH BOLO (jhoot ka darwaza band)

Har tool ab **saaf jawab** dega: `{ ok: true|false, value, error }`

- Confirmation ka jumla **tool ke asli nateeje se** banega, model ki tasavvur se nahi.
- `ok:false` → Maya kahegi *kya* nahi hua aur *kyun*.
- Prompt ka nirala qanoon: **"Jab tak tool `ok` na de, kamyabi ka daawa MANA hai."**
- Test: tool fail karwao → jawab mein kamyabi ka daawa **nahi** hona chahiye.

---

## 🧱 TEH 7 — ASLI AGENT LOOP (sochna → karna → dekhna → karna)

Aaj: Gemini par `maxSteps = 2`, pool par **loop hai hi nahi**.
Kal: **ek hi loop, dono zubanon ke liye**, 5 qadam tak:

```
soch → tool → nateeja → soch → tool → … → aakhri jawab
```
Har qadam ka waqt aur budget mehdood, taake wo kabhi na atke.

---

## 🧱 TEH 8 — IJAZAT (ye "tabahi level" access ki lagaam hai)

Aap ko **full automatic control** chahiye — magar bina lagaam ke wo khatra hai.
Har tool ka apna darja:

| Darja | Kya hota hai | Misalein |
|---|---|---|
| 🟢 **SABZ** | foran, bina poochhe | brightness, torch, volume, timer, weather, battery, calculate |
| 🟡 **ZARD** | kar do, magar bata kar + "wapas karo" ka button | alarm, reminder, app kholna, diary, memory |
| 🔴 **SURKH** | pehle **saaf ijazat** | call, SMS, WhatsApp, file share, kuch mitana |

Aur ek **⚡ TRUST MODE** switch — jo surkh ko bhi zard bana de, jab aap poora
control dena chahen. **Faisla hamesha aap ka.**

---

## 🧱 TEH 9 — AMAL TRACE (nazar aane wala saboot)

Har jawab ke neeche chhoti chip:

```
🔦 torch_control  ✅ 120ms      💡 brightness_control  ✅ level:100
```

Dabao to args + asli nateeja khul jaye.
**Jhoot pakarna asaan, aur dekhne mein "next level".**

---

## 🧱 TEH 10 — SABOOT (`tools/test-amal-engine.js`)

| Kya | Kitne |
|---|---|
| **Golden commands** — Urdu/Roman/English jumle → sahih tool + args | ~60 |
| **Har tool pakra jaye** (BUG 1 ka taala) | 44 |
| **Sanitizer** — har reasoning shakl, kata hua `<think>` samet | ~20 |
| **Dialect** — teeno tarjume aik hi tool list dein | ~10 |
| **Text protocol** — sakht + rescue parser | ~12 |
| **Agent loop** — multi-step, budget, timeout | ~10 |
| **Jhoot mana hai** — tool fail → daawa na ho | ~8 |
| **Ijazat** — surkh bina ijazat na chale | ~10 |

Regression budget: **0**. Maujooda 520 test green rahenge.

---

# HISSA 3 — QADAM BA QADAM (chhote, mehfooz release)

| | Version | Kya | Kyun pehle | Khatra |
|---|---|---|---|---|
| **P1** | **v4.9.0 — SAAF ZUBAAN** | TEH 3 (sanitizer) + TEH 5 (token) + BUG 6 (zubaan ka tazad) | **Aaj jo aap ne dekha, wo aaj hi khatam.** Chhota, foran nazar aane wala | 🟢 kam |
| **P2** | **v5.0.0 — AMAL ENGINE** | TEH 1 + 2 + 4 + 6 + 7 | Asal tabahi: **1 → 7 dimaag** kaam kar sakenge, aur jhoot band | 🟡 darmiyana |
| **P3** | **v5.1.0 — IJAZAT + TRACE** | TEH 8 + 9 | Poora control mehfooz tareeqe se + nazar aane wala saboot | 🟢 kam |
| **P4** | **v5.2.0 — KHUD-MUKHTAR** | khud se kaam, chains, skills, waqt par amal | Sirf tab jab neeche ki buniyad pakki ho | 🔴 baad mein |

---

## 🎯 Mera mashwara

**P1 se shuru karein.** Chhota kaam hai, magar aap ki aaj ki shikayat ka **poora**
jawab hai: `<think>` ghayab, Devanagari theek, "soch rahi hoon" par atakna khatam.

Us ke baad **P2** — jahan asal tabahi hai: Maya ke saare dimaag tools chalane
lagenge, aur `brightness_control(level=100)` **likha nahi — kiya** jayega.

---

# HISSA 4 — BLUEPRINT (naqshe se banane laiq tafseel tak)

## 4.1 — 🔴 BUG 1 socha se **bara** nikla: 33 mein se 12 tools router ko nazar hi nahi aate

Har tool ka ek aam Roman-Urdu jumla bana kar `ACTION_WORDS` par chalaya:

```
✅ ROUTER PAKARTA HAI          21/33
❌ ROUTER BILKUL ANDHA HAI     12/33
```

| Tool | Jumla jo router **miss** kar deta hai |
|---|---|
| `brightness_control` | "britness barhao 100%" |
| `torch_control` | "torch on karo" |
| `volume_control` | "awaaz 50 kar do" |
| `set_reminder` | "mujhe 20 minute baad yaad dilana" |
| `prayer_times` | "maghrib ka waqt kya hai" |
| `reply_message` | "Ali ko reply karo theek hai" |
| `recall_memory` | "mujhe kya kya yaad hai" |
| `search_memory` | "yaad hai maine kya kaha tha" |
| `web_search` | "internet par talash karo" |
| `web_fetch` | "ye site parho" |
| `create_skill` | "ye tareeqa seekh lo" |
| `notify` | "notification bhejo" |

**36% tools par Maya ko pata hi nahi chalta ke kaam maanga gaya hai.**
Aap ne bas un mein se ek (`brightness`) try kiya tha.

## 4.2 — 🔴 BUG 8 (naya): parameter ka naam bhi match nahi karta

```js
// execTool
parseInt(args.percent, 10)          // <- asal naam: percent
```
```
// model ne likha:
brightness_control(level=100)       // <- level
```

Function-calling mein schema jata hai to Gemini sahih naam likhta hai — magar
**text protocol** wale dimaag andaza lagayenge. Is liye **ARG ALIAS** teh lazmi hai:

```
brightness_control: percent  ← level, value, brightness, pct, amount, "100%"
volume_control:     percent  ← level, value, volume, vol
torch_control:      on       ← state, enable, value ("on"/"off"/true/1)
```

## 4.3 — TOOL REGISTRY ka aakhri naqsha (33 tools · darja · triggers)

Har tool ab **khud** apne triggers aur khatre ka darja lekar aayega.

### 🟢 SABZ — foran, bina poochhe (15)
| Tool | Triggers (Roman + Urdu + English) |
|---|---|
| `brightness_control` | brightness, britness, brightnes, chamak, roshni, raushni, screen light, dim, bright |
| `volume_control` | volume, awaaz, awaz, sound, aawaz, speaker, ki awaz, mute |
| `torch_control` | torch, flash, light, batti, roshni karo, flashlight |
| `set_timer` | timer, ٹائمر, minute ka, second ka, count down |
| `get_weather` | mausam, weather, garmi, sardi, barish, temperature |
| `battery_status` | battery, charge, charging, batri |
| `prayer_times` | namaz, azan, fajr, zuhr, asr, maghrib, isha, prayer, waqt kya |
| `run_javascript` | calculate, kitna, kitne, percent, hisab, jama, average, convert, nikalo |
| `wiki_search` | wikipedia, wiki, kaun hai, kya hai, kaun tha |
| `web_search` | search, talash, dhoond, khojo, internet par, google par, pata karo |
| `search_memory` | yaad hai, yaad tha, maine kaha tha, pehle bataya |
| `recall_memory` | kya yaad, sab yaad, yaaddasht, memory dikhao |
| `diary_search` | diary, roznamcha, kal kya hua, pichle hafte |
| `list_files` | files, download, photo, picture, video, music dikhao |
| `notify` | notification, notify, yaad dila do screen par |

### 🟡 ZARD — kar do, magar bata kar + ⟲ wapas karo (12)
| Tool | Triggers |
|---|---|
| `set_alarm` | alarm, jaga dena, subah uthana, ٓالارم |
| `set_reminder` | reminder, yaad dilana, yaad dila dena, baad mein bata |
| `open_app` | kholo, khol do, open, chalao, app |
| `play_youtube` | gana, gaana, song, video, youtube, baja, chalao |
| `search_web` | google kholo, browser, web par kholo |
| `web_fetch` | site parho, page parho, link parho, is url se, rate kya hai |
| `diary_write` | diary mein likho, roznamcha, aaj ye hua, note kar lo |
| `save_memory` | yaad rakho, yaad rakhna, note kar lo hamesha |
| `create_skill` | seekh lo, ye tareeqa, aaindah aise, skill |
| `open_file` | file kholo, pdf kholo, photo kholo |
| `read_messages` | message parho, kya message, naye message, whatsapp dekho |
| `schedule_message` | baad mein bhejna, X minute baad message |

### 🔴 SURKH — pehle SAAF IJAZAT (6)
| Tool | Triggers | Kyun surkh |
|---|---|---|
| `call_contact` | call karo, phone karo, mila do | asli call lag jayegi |
| `call_number` | number par call, dial | asli call |
| `message_contact` | message karo, whatsapp karo, bhejo | doosre insaan tak jayega |
| `send_sms` | sms, text bhejo | paisa + doosra insaan |
| `reply_message` | reply karo, jawab do | doosre insaan tak |
| `share_file` | share karo, bhejo file | zaati data bahar |

> ⚡ **TRUST MODE** ON → surkh bhi zard ban jate hain (bata kar kar do).
> Faisla hamesha aap ka, Settings se.

## 4.4 — SAAF-KARO ki mukammal fehrist

| # | Shakl | Kis model se |
|---|---|---|
| 1 | `<think>…</think>` | qwen3, deepseek-r1 |
| 2 | `<thinking>` / `<reasoning>` / `<scratchpad>` | mukhtalif |
| 3 | `<\|channel\|>analysis … <\|message\|>` | gpt-oss (harmony) |
| 4 | **band na hua** `<think>` (token khatam) | **aap ka screenshot** |
| 5 | `Here's a thinking process:` … pehle khali khat tak | plain-text soch |
| 6 | `1. **Analyze User Input:**` type numbered analysis | plain-text soch |
| 7 | `Check constraints:` / `Refine against Constraints:` | **aap ka screenshot** |
| 8 | `Response draft (…):` / `Draft Construction` | **aap ka screenshot** |
| 9 | `‹AMAL›…‹/AMAL›` | hamara apna protocol |
| 10 | Akela `tool_name(arg=value)` line | tool-less dimaag |

**Qanoon:** saaf hone ke baad khali bacha → jawab **EMPTY** → **agla dimaag**.
Khali bubble ya adhoora jumla kabhi nahi.

## 4.5 — Kahan kahan haath lagega (file map)

| File | Kya badlega | P |
|---|---|---|
| `public/index.html` · `SAAF()` *(naya)* | 10 shaklon ka sanitizer — `addBubble` **aur** `cleanSpeech` dono se pehle | P1 |
| `public/index.html` · `BRAIN.ask` | `max_tokens` per-model; reasoning models ko 1400 + `reasoning_effort:"low"` | P1 |
| `public/index.html` · `sysPrompt()` | RULE(3) vs LANGUAGE RULE ka tazad khatam — ek hi hukm | P1 |
| `public/index.html` · `TOOLS[]` *(naya registry)* | 33 tools + triggers + darja + arg-alias; `TOOL_DECLS` isi se banega | P2 |
| `public/index.html` · `ACTION_WORDS` | hath se likhi regex → **registry se khud bane** | P2 |
| `public/index.html` · `BRAIN.ask` | `tools:[...]` bhejo + `tool_calls` parse karo | P2 |
| `public/index.html` · `AMAL.parse()` *(naya)* | sakht + rescue text-protocol parser | P2 |
| `public/index.html` · `execTool` | jawab `{ok, value, error}` par normalize + arg-alias | P2 |
| `public/index.html` · agent loop *(naya)* | dono zubanon ke liye ek loop, 5 qadam | P2 |
| `tools/test-amal-engine.js` *(nayi file)* | ~174 test (neeche) | P1+P2 |
| `app/.../MainActivity.kt` | **koi tabdeeli nahi** — `torch()`, `brightness()` waghera pehle se maujood ✅ | — |

## 4.6 — Test ka naqsha (`tools/test-amal-engine.js`)

| Hissa | Kya sabit karta hai | Kitne |
|---|---|---|
| SAAF-KARO | 10 shaklen + kata hua `<think>` + khali → EMPTY | 24 |
| **Har tool pakra jaye** | 33 tools × kam az kam 1 jumla — **BUG 1 ka taala** | 33 |
| Golden commands | Urdu/Roman/English jumle → sahih tool + sahih args | 60 |
| Arg alias | `level`/`value`/`pct`/"100%" → `percent` — **BUG 8 ka taala** | 12 |
| Dialect | Gemini / OpenAI / text — teeno mein wahi 33 tools | 9 |
| Text protocol | sakht `‹AMAL›` + rescue `name(arg=1)` | 12 |
| Agent loop | multi-step, budget, timeout, gen-guard | 10 |
| **Jhoot mana hai** | tool `ok:false` → jawab mein kamyabi ka daawa **na ho** | 8 |
| Ijazat | surkh bina ijazat na chale; TRUST MODE se chale | 10 |
| **Kul** | | **~178** |

Regression budget **0** — maujooda 520 test (CSS · 72 · 293 · 155) green rahenge.

---

# HISSA 5 — P1 ka mukammal contract (agla release)

**v4.9.0 — SAAF ZUBAAN** · *sirf 3 cheezein, sab kam-khatra:*

1. **`SAAF()` sanitizer** — 10 shaklen. `<think>` ab na dikhega, na bolega.
2. **Token budget** — reasoning models ko 1400 (aaj 320 → soch mein hi khatam).
   `finish_reason:"length"` par kata hua matn **kabhi** na dikhe.
3. **Zubaan ka tazad khatam** — RULE(3) aur LANGUAGE RULE ka jhagra hal;
   `lang` setting ka matlab saaf, aur "user ki script mirror karo" ko tarjeeh.

**Kya theek ho jayega (aap ke screenshot se):**
| Aap ne dekha | P1 ke baad |
|---|---|
| Poora `<think>` bubble mein | ❌ ghayab |
| Poora `<think>` **bol kar** sunaya gaya | ❌ ghayab |
| Kata hua, band na hua `<think>` | ❌ ghayab (aur token bhi kaafi) |
| "(soch rahi hoon) mein stuck" | ✅ khatam |
| Devanagari jab aap Roman Urdu bolte hain | ✅ theek |
| `brightness_control(level=100)` **likha** gaya | ⏳ **P2** mein CHALEGA |
| Jhoota "सेट कर दी है" | ⏳ **P2** |

**P1 mein kya NAHI hoga:** tools abhi bhi sirf Gemini par chalenge. Wo P2 hai.

**Saboot:** ~24 naye sanitizer test + `npm test` green + aap ke device par tasdeeq.
