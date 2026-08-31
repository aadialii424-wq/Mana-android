# 🗄️🎙️ SETFORM + AWAAZ POOL — MAYA v4.5.0

> **Do shikayaten, do asal jarhen, do mustaqil ilaj.**
> User ne kaha: *"kuch settings baar baar reset ho rahi hain… girlfriend mode khud band
> ho jata hai"* aur *"gemini ki voice chal hi nahi rahi, baar baar band ho jati hai."*
> Dono baaton mein wo bilkul theek the. Neeche har cheez ka forensic hisaab hai.

---

# HISSA 1 — 🗄️ SETFORM: "settings khud reset ho jati hain"

## 1.1 Asal jarh (ye koi mystery nahi thi)

SAVE button par **do alag click handler** lage hue the:

```
                 ┌──────────── SAVE dabao ────────────┐
                 │                                    │
   handler A (line ~4008)                  handler B (line ~4690)
   • keys parhe aur save kiye                • $("#sGf").checked parha
   • phir loadSettingsForm() chala diya  ───▶ • magar wo abhi abhi
     jisne form ko PURANI settings se           handler A ne OFF kar diya tha!
     dobara likh diya → GF switch OFF        • gfMode = false save kar diya
```

Yaani **handler A ne form ko wapas purani halat par likh diya, aur handler B ne
usi palti hui halat ko "sach" maan kar save kar diya.**

Iska matlab ye tha ke ye SAB fields SAVE button se **kabhi save ho hi nahi sakte the**:

| Field | Kya hota tha |
|-------|--------------|
| 💖 Girlfriend mode | on karo → save → khud off |
| Gender, Phone, Persona | badlo → save → purana wapas |
| Language, Proactive, Remember, Convo mode | wahi |
| Music app, Fav song, YT channel, Assistant naam | wahi |

Groq key isi liye "saath mein" gayab lagti thi — asal mein key to save ho jati thi,
magar usi click mein GF mode wapas off ho jata tha, is liye lagta tha "sab reset ho gaya".

**Sabaq:** ek hi cheez ko do jagah se likhna hamesha bug hai. Sawal ye nahi tha ke
"kaun sa handler theek karen" — sawal ye tha ke **do handler hone hi nahi chahiye the.**

## 1.2 Ilaj — ek registry, ek darwaza

Ab settings ka **wahid** raasta `SETFORM` hai, aur uske peeche ek data-driven list:

```js
var SET_FIELDS = [
  { id: "sName",  key: "name",   t: "text",  def: "Sir" },
  { id: "sGf",    key: "gfMode", t: "check", def: false },
  { id: "sKey",   key: "apikey", t: "keys" },
  { id: "sRate",  key: "rate",   t: "num", def: 1, out: "rateVal", dp: 2 },
  …44 fields…
];
```

Yehi ek list **load bhi karti hai aur save bhi**. Is liye ab ye mumkin hi nahi ke
koi field load ho magar save na ho (ya ulta) — wo poora bug class khatam ho gaya.

### SAVE ka safar — ab ek hi tarteeb

```
SAVE dabao
   │
   1. collect()   ── SAB 44 khane parho     ◀── pehle sab kuch parha jata hai
   │
   2. fixKeys()   ── galat khane mein pari key sahi jagah bhejo
   │
   3. apply()     ── asraat: theme, AWAAZ reset, BRAIN POOL reset, wake service
   │
   4. persist()   ── localStorage
   │
   5. load()      ── ab form dobara likho  ◀── ab MEHFOOZ: sab parha ja chuka
```

Pehle qadam 5 (form dobara likhna) qadam 1 ke **beech** mein ho raha tha. Bas yehi
poora bug tha, aur is tarteeb se wo dobara mumkin nahi.

### Field types

| `t` | Matlab | Misal |
|-----|--------|-------|
| `text` | saada matn, trim | naam, sheher |
| `keys` | **kai keys** — comma/nayi line se alag | har API key |
| `check` | switch (`def:true` = "false na ho to on") | GF mode, Turbo |
| `sel` | dropdown | persona, language |
| `num` / `int` | number + live label | rate, radius |

### Aur kya theek hua

* **`fixKeys()`** — Gemini ke khane mein `gsk_` pari hai? khud Groq ke khane mein
  chali jayegi. Ab ye `csk-` (Cerebras), `sk-or-` (OpenRouter), `nvapi-` (NVIDIA),
  `ghp_`/`github_pat_` bhi pehchanta hai — aur purani key mitata nahi, **comma laga kar
  saath jorta hai** (multi-key ka faida barqarar).
* **GF mode + gender** — pehle chup-chaap off hota tha. Ab agar Gender male nahi hai to
  MAYA batati hai: *"Girlfriend mode ke liye Gender = Male chahiye"*.
* Naya field add karna ab **ek line** hai. Wo khud load hoga, khud save hoga.

---

# HISSA 2 — 🎙️ AWAAZ POOL: "gemini ki voice baar baar band ho jati hai"

## 2.1 Teen asal jarhen

### 🔴 J1 — Gemini TTS ka muft quota sirf **~15 request ROZ** hai
Ye baqi Gemini models jaisa 1,500/din nahi hai. `gemini-2.5-flash-preview-tts` free tier
par **15 RPD** deta hai. Aur hamara purana code har jawab ko **420 harf** ke tukron mein
torta tha — yaani ek lamba jawab = **4-5 request**.

> 15 ÷ 4 = **din mein sirf 3-4 jawab**, phir awaaz mar jati thi. 😳
> Ye "band ho jati hai" nahi tha — ye **quota khatam** tha.

### 🔴 J2 — 403 ko "mari hui key" samajhna
```js
if (st === 401 || st === 403) { AWAAZ.keyBad = true; … }   // ← purana
```
`keyBad` **poore session ke liye** neural awaaz band kar deta tha. Magar Gemini 403
aksar **quota** ke liye bhejta hai, kharab key ke liye nahi. Ek quota 403 = poore din
ke liye awaaz khatam. (Yehi ghalti hum DIMAAG v2 mein pehle theek kar chuke the —
AWAAZ mein reh gayi thi.)

### 🔴 J3 — model fallback apne aap band ho jata tha
```js
if (retryable && !AWAAZ.model && at + 1 < models.length) { at++; go(); }
```
`!AWAAZ.model` ka matlab: **jab ek dafa koi model chal gaya, uske baad kabhi doosra model
try nahi hoga.** Yaani wo model band hote hi awaaz hamesha ke liye gir jati thi.

## 2.2 Ilaj — teen tehen wali AWAAZ POOL

```
      bolna hai
          │
   ┌──────┴─────────────────────────────────────────────┐
   │ TEH 1 — 💎 Gemini neural (behtareen, tumhari keys)  │
   │   key1 ─ quota? ─▶ key2 ─ quota? ─▶ key3 …          │
   │   model mar gaya? ─▶ agla model (ab hamesha)        │
   └──────┬─────────────────────────────────────────────┘
          │ sab keys thak gayin
   ┌──────┴─────────────────────────────────────────────┐
   │ TEH 2 — 🌸 MUFT NEURAL (bina key, bina signup)      │
   │   asli insani awaaz, robot nahi                     │
   └──────┬─────────────────────────────────────────────┘
          │ wo bhi na chale
   ┌──────┴─────────────────────────────────────────────┐
   │ TEH 3 — 📱 Phone ki apni awaaz (hamesha, 0 data)    │
   └────────────────────────────────────────────────────┘
```

### Chaabi 1 — kam request, wahi awaaz
Purana: har 420 harf = ek request.
Naya **`chunkPlan()`**: **pehla tukra 300 harf** (taake awaaz *foran* shuru ho),
**baqi 1500-1500** (taake request kam lagen).

| Jawab | Purani requests | Nayi requests |
|-------|-----------------|---------------|
| chhota (250 harf) | 1 | 1 |
| darmiyana (900 harf) | 3 | **2** |
| lamba (2,400 harf) | 6 | **2** |

Yaani wahi 15/din ka quota ab **teen-chaar guna** zyada jawab bolta hai.

### Chaabi 2 — kai TTS keys
`ttsKey` khana ab comma se **kai keys** leta hai, aur main Gemini keys bhi apne aap
shamil ho jati hain. Har key ka **apna** cooldown hota hai:

```
💎 Gemini key1 (abhi yahi) — ✅ LIVE
💎 Gemini key2             — ⏳ QUOTA_DAY 2840s
🌸 Muft neural (bina key)  — ✅ LIVE
📱 Phone ki awaaz          — ✅ hamesha
```
**3 keys = 45 request roz.** Ek key ka quota khatam hote hi agli khud chal padti hai —
usi jumle ke beech mein, user ko pata bhi nahi chalta.

### Chaabi 3 — 403 ka sahi tarjuma

| Jawab | Purana faisla | Naya faisla |
|-------|---------------|-------------|
| 401 | key mar gayi (hamesha ke liye) | us **key** par 30 min |
| 403 + *"API key not valid"* | key mar gayi | us **key** par 30 min |
| 403 + *"quota exceeded"* | ❌ key mar gayi | ✅ **QUOTA** — 90 sec |
| 429 + *"per day"* | 90 sec | ✅ **QUOTA_DAY** — 1 ghanta |
| 400/404 + *"model…"* | model masla | ✅ wo model list se **nikal** diya |

Aur ab har halat **`localStorage`** mein mehfooz hai — app band kar ke kholne par bhi
MAYA ko yaad hai ke kaun si key aaj khatam ho chuki hai, is liye wo bekaar dobara nahi
chheri jati.

---

## 3. Ab user ko kya nazar aayega

| Pehle | Ab |
|-------|-----|
| GF mode on → save → khud off | ✅ on hi rehta hai |
| "gemini voice band ho gayi" | 🌸 **muft neural awaaz** le leti hai — robot nahi |
| chup-chaap phone ki awaaz | Settings mein saaf likha: kaun si key zinda, kaun si thaki, kitne second |
| 3-4 jawab ke baad awaaz khatam | teen-chaar guna zyada, aur keys jorne par aur bhi |

Settings → **AWAAZ** mein ab live pool nazar aata hai, aur Doctor report mein bhi.

---

## 4. Saboot — automated test

| Suite | Pehle | Ab |
|-------|-------|-----|
| `check-oldwebview-css.js` | PASS | **PASS** |
| `test-settings-ui.js` | 45 | **64** (19 naye — SETFORM) |
| `test-voice-engine.js` | 118 | **142** (24 naye — AWAAZ POOL) |
| `test-brain-engine.js` | 155 | **155** |

Naye test jo *asal shikayat* ko pakadte hain:

```
11. SETFORM — SAVE par kuch bhi chup-chaap reset na ho
  ✅ 🔥 GF MODE save hua (purana bug: chup-chaap false ho jata tha)
  ✅   → saath mein Groq key bhi save hui
  ✅   → save ke baad switch ON hi raha (form wapas nahi palta)
  ✅ doosri dafa SAVE par bhi GF mode zinda
  ✅ SAVE button par sirf EK handler (do handler hi asal bug the)
  ✅ registry ka har field DOM mein maujood
  ✅ Gemini khane mein pari GROQ key khud sahi jagah chali gayi

10. KHARAB KEY — per key faisla, aur khamoshi phir bhi nahi
  ✅ comma se do TTS keys nikleen
  ✅ doosri key se awaaz aa gayi          ← key rotation
  ✅ engine neural hi raha (device par nahi gira)
  ✅ din wale quota ka alag thappa (QUOTA_DAY)
  ✅ restart ke baad bhi yaad raha        ← localStorage
12. FALLBACK — khamoshi kabhi nahi
  ✅ key na ho to Google ko call hi nahi jati
  ✅   → magar MUFT NEURAL (bina key) ko zaroor try kiya
 7. CHUNKING
  ✅ naya plan purane se KAM request bhejta hai (quota bachta hai)
  ✅ pehla tukra chhota hai (foran bolne ke liye)
```

---

## 5. Aage kaam karne walon ke liye

* **Naya setting field** → `SET_FIELDS` mein ek line. Bas.
* **Naya TTS provider** → `AWAAZ` mein `pollen()` jaisa ek function + `speak()` ki
  seerhi mein ek line.
* **Kabhi bhi** `loadSettingsForm()` ko save ke beech mein mat bulao — yehi wo ghalti
  thi jisne poora field-set bemaani kar diya tha. Test #11 ab is par pehra deta hai.

---

# HISSA 3 — 🩺 AWAAZ DOCTOR (v4.6.0)
## "3 nayi keys generate kar ke lagayin — ek bhi nahi chali"

User ne bilkul theek kiya jo kaha. Aur iski wajah **hamare code mein nahi, Google ke
usoolon mein** thi — magar **qusoor hamara** tha ke app ne wajah **batai hi nahi**.

## 3.1 Google ke do usool jo har banda pehli dafa mein pakda jata hai

### 🔴 Usool 1 — quota KEY par nahi, **PROJECT** par lagta hai

> *"Rate limits are applied at the **project level**, not per API key. If you have three
> API keys in the same Google Cloud project, they all share the same quota pool.
> **Creating additional API keys won't increase your limits.**"*

AI Studio mein **"Create API key"** baar baar dabane se sab keys **ek hi project** mein
banti hain. Is liye:

```
   3 nayi keys  ─┐
                 ├──▶  EK project  ──▶  EK quota (~15 TTS request/din)
   3 nayi keys  ─┘                        ← jo pehle hi khatam tha
```

**Teen nayi keys = wahi purana khatam quota.** Ek bhi nahi chali — bilkul wahi jo hua.
Faida sirf tab hai jab har key **alag Google ACCOUNT** (ya "new project") se bane.

Pichhli baar maine kaha tha "doosri/teesri account ki key" — magar app ne ye **kahin
likha nahi tha**, aur na hi ye **pakad kar bataya**. Wo kami ab poori ho gayi.

### 🔴 Usool 2 — 19 June 2026 se "unrestricted" keys **block**

> *"Starting June 19, 2026, Google will block Gemini API calls made with keys that do not
> have API-level restrictions configured. Keys still set to 'any API' will stop working."*

Cloud Console se bani nayi key jispar restriction nahi lagi → **403 PERMISSION_DENIED**,
chahe abhi banai ho.

### 🔴 Usool 3 — kuch projects ko free tier milta hi nahi (`limit: 0`)
Google ka jawab literally `limit: 0` bhejta hai. Nayi keys banane se kuch nahi hota.

*(Aur CORS? Wo wajah **nahi** thi — Google ka preflight `x-goog-api-key` ko allow karta
hai. Ye check kar ke rad kiya gaya, taake hum ghalat cheez theek na karte rahen.)*

## 3.2 Ilaj — andaza band, **muaina** shuru

Settings → AWAAZ mein naya button: **🩺 GEMINI VOICE KEYS CHECK KARO**

Har key par **do** imtihan hote hain:

| Qadam | Kya poochta hai | Kya farq karta hai |
|-------|-----------------|--------------------|
| 1. `ListModels` | key khud zinda hai? | ghalat key / **restriction** / dead key pakadta hai |
| 2. chhoti TTS request | TTS ka quota bacha? | key theek magar **quota** khatam — ye alag masla hai |

Ye do-qadam split hi asal jadoo hai: pehle *"key kharab hai"* aur *"quota khatam hai"*
ek jaise dikhte the. Ab ye do bilkul alag natije hain.

### Report aisi dikhti hai

```
━━━ KEY 1  (…x7Qp2m) ━━━
  Key khud   : ✅ zinda — Google ne 3 TTS model dikhaye
  TTS        : ❌ QUOTA (429) — gemini-2.5-flash-preview-tts
               Quota exceeded for quota metric 'Generate Content free tier requests'
  ➜ Quota khatam. Yaad rahe: quota KEY par nahi, PROJECT par lagta hai.

━━━ KEY 2  (…m4K1za) ━━━   … wahi …
━━━ KEY 3  (…b9Lp0c) ━━━   … wahi …

═══════════ FAISLA ═══════════
🎯 ASAL MASLA: AAP KI SAB KEYS EK HI PROJECT KI HAIN.

   Google ka quota KEY par nahi, PROJECT par lagta hai.
   AI Studio mein 'Create API key' baar baar dabane se sab keys
   EK HI project mein banti hain — aur ek hi quota pool se peeti hain.
   Is liye 3 nayi keys = wahi purana khatam quota.

   ILAJ (yehi kaam karta hai):
   • Har key ALAG GOOGLE ACCOUNT se banao (Gmail badal kar),
     aistudio.google.com/apikey — har account = apna quota.
   • Ya AI Studio mein 'Create API key in new project' chuno.

   Tab tak 🌸 MUFT NEURAL awaaz chalti rahegi — bina kisi key ke.
```

### Saat mumkin faisle

| Faisla | Kab | App kya kehti hai |
|--------|-----|-------------------|
| `OK` | koi key chal gayi | neural on kar deti hai, cooldown saaf |
| `RESTRICT` | 403 / permission | "19 June 2026 se…" + restriction lagane ka tareeqa |
| `BADKEY` | "API key not valid" | "poori key copy karo — AIza se shuru, ~39 harf" |
| `LIMIT0` | `limit: 0` | "is project ko free tier mila hi nahi — alag account" |
| **`SAMEPROJECT`** | **sab keys zinda, sab ka quota khatam** | **"sab keys ek hi project ki hain"** |
| `QUOTA` | ek key, quota khatam | "~15/din, Pacific midnight par reset" |
| `NET` | request pahunchi hi nahi | "internet/firewall dekho" |

## 3.3 Saath mein

* **TTS model auto-discovery** — `ListModels` se `tts` wale models khud chun leta hai,
  is liye Google naam badle to bhi app nahi marti.
* **APK mein Gemini TTS ab Kotlin bridge se** (`postKeyed`) — CORS/preflight ka sawal hi
  khatam, aur Google ka poora error text parha ja sakta hai.
* **Sacche paighamat** — ab "Gemini key ghalat ya band hai" ki jagah:
  *"Gemini key nahi chal rahi — 🩺 KEYS CHECK KARO dabao, asal wajah pata chal jayegi"*.
* Settings ka hint bhi sach bolta hai: *"EK HI account ki 3 keys ka koi faida nahi"*.

## 3.4 Saboot — 20 naye test

```
11b. AWAAZ DOCTOR — "3 nayi keys lagayin, ek bhi nahi chali"
  ✅ limit: 0 ko alag pehchanta hai (project ko free tier mila hi nahi)
  ✅ restriction wali key pehchani
  ✅ ghalat key pehchani · din ka quota pehchana
  ✅ har masle ka ILAJ bhi likha hai
  ✅ 🔥 teenon keys zinda magar teenon ka quota khatam -> EK HI PROJECT ka faisla
  ✅   → user ko saaf batata hai ke sab keys ek hi project ki hain
  ✅   → asal ilaj bhi batata hai (alag Google account)
  ✅ restriction sab se pehle pakda jata hai · June 2026 wali policy ka hawala
  ✅ ListModels se sirf TTS model chune gaye
  ✅ har qadam live likha gaya
```

`npm test` → CSS PASS · **64** Settings · **166** AWAAZ · **155** DIMAAG
