# ⚡ v4.10.0 — AMAL: TOOLS AB HAR DIMAAG KO

> Ye release **aap ke apne device ke diagnostic** se bani hai. Har fix ke peeche
> aap ki asli chat ya asli log hai — koi andaza nahi.

---

## 🔬 Aap ke diagnostic ne kya sabit kiya

### ✅ Jo P1 mein theek hua tha, wo SACH MEIN theek hua
```
soch kaati gayi : 7 / 12        ← SAAF ne 7 dafa <think> pakra
nakaam turn     : 0
```
Poori chat mein **ek bhi `<think>` nahi**. Zubaan bhi sahih — aap Devanagari mein
likho to Devanagari, Roman mein likho to Roman. **MIRROR chal raha hai.** ✅
Aur 👑 MALIK bhi chala — Maya ne Adil Chandio aur Monarch bataya. ✅

### 🚨 Magar asal masla nanga ho gaya
```
tool wale turn : 0 / 12         ← EK BHI TOOL NAHI CHALA
DIMAAG: GEMINI × 1, Groq × 8, Mistral × 3
LOG   : BRAIN GEMINI: QUOTA_DAY  (5 dafa)
```

**Zanjeer:** Gemini — MAYA ka **wahid** tool-wala dimaag — ka **din ka quota
khatam** tha. Baqi 11 turn Groq aur Mistral ne diye, aur unhe `tools` bheje **HI
NAHI** jate the. Magar prompt unhen tools ka **hukm** deta tha.

Natija aap ki chat mein:
| Aap ne kaha | Maya ne kiya |
|---|---|
| "ब्राइटनेस को 100% करो" | *"ठीक है, जान!"* — **kuch nahi hua** |
| "अभी तक तो नहीं हुई" | *"100 पर चला दी"* — **jhoot** |
| "Funk Taka song lagao" | *"chalaa diya"* — **jhoot** |
| "Nhi hua" | `play_youtube(query="Funk` — **matn mein, kata hua** |
| "whatsapp par message karo" | *"system mein access disable hai"* — **bahana** |

---

## ⚡ ILAJ — P2a

Groq · Cerebras · Mistral · OpenRouter · NVIDIA · GitHub — **sab OpenAI-shakl
function calling jante hain.** Bas jora nahi gaya tha.

```
1 tool-wala dimaag   →   7 tool-wale dimaag
```

- **Ek tool registry, do tarjume** — Gemini ka `functionDeclarations` aur OpenAI ka `tools[]`
- **Schema ka case theek** — Gemini `"OBJECT"` → OpenAI `"object"` *(warna seedha 400)*
- **`tool_calls` parse + chalao + nateeja wapas** → dimaag phir jumla banata hai (2 qadam tak)
- **Koi provider `tools` par 400 de?** → **bina tools DOBARA** koshish, aur us provider ko yaad rakh lo. **Dimaag marta nahi.**

### 🔑 ARG ALIAS — screenshot wala BUG 8
```
model likhta hai : brightness_control(level=100)
execTool maangta : args.percent
```
Ab `level` · `value` · `pct` · `"100%"` · `song` · `vol` · `state` — **sab sahih
naam par map** ho jate hain. `"80%"` → `80`. `"on"` → `true`.

---

## 🧭 ROUTER — 12 andhe tools ab dekhte hain

Purani `ACTION_WORDS` haath se likhi thi. Chala kar dekha tha: **33 mein se 12
tools** use nazar hi nahi aate the — `brightness` · `torch` · `volume` ·
`reminder` · `namaz` · `reply` · `notify` …

Ab **har tool apne trigger lafz khud deta hai** aur regex **unhi se banti hai**.
`"britness"` jaisi aam ghalti bhi shamil.

> **Aur ek test taala laga deta hai:** har tool ke apne trigger hone LAZMI hain.
> Naya tool banao aur trigger bhool jao → **test fail**. BUG 1 dobara mumkin nahi.

---

## 🩹 Aap ke device se mile 5 aur bug

| # | Bug | Ilaj |
|---|---|---|
| 1 | `play_youtube(query="Funk` — **kata hua** tool call SAAF se chhoot gaya | band bracket ab lazmi nahi |
| 2 | Aap ne **Devanagari** mein "किसने बनाया" poocha — MALIK ka fast-path chala hi nahi | Devanagari + Urdu + "adil chandio" sab shamil |
| 3 | Diagnostic ne **"VERSION 4.1.0"** dikhaya | splash par hard-code purana version tha — khatam |
| 4 | `GEMINI: QUOTA_DAY` **5 dafa** — har turn dobara koshish | din ka quota khatam → **poora dimaag skip**. *(p90 31s ka bara sabab)* |
| 5 | `Cerebras: HTTP_402` **do dafa** — ab paisa maangta hai | 402 → **6 ghante** ka cooldown |

---

## 📱 Aur ek achhi khabar — aap ka phone NAYA hai

```
device  : TECNO KL4 · Android 14
webview : 152.0.7977.64          ← bilkul naya
```

Maine plan mein andaza lagaya tha ke WebView **purana** (~Chrome 55-79) hai —
kyunke `FIX-v4.0.1` doc mein `?.` parse fail hua tha. **Wo purana daur tha.**

**Iska matlab:** 🗣️ **P5 (streaming) ka khatra 🔴 se 🟡** ho gaya, aur
👁️ **P4 (vision) bhi mehfooz** hai. ES5 pabandi phir bhi qaim rakhi hai —
muft mein milne wali hifazat hai.

---

## 🧪 LAB mein naya switch

```
📊 NAAP        [ON ]
🧹 SAAF ZUBAAN [ON ]   ← aap ne chalu kiya, chal raha hai
👑 MALIK       [ON ]   ← chal raha hai
⚡ AMAL        [OFF]   ← YE NAYA — chalu karo
```

---

## 🧪 Saboot

```
✅ CSS PASS · Settings 72 · AWAAZ 293 · DIMAAG 155 · 🧪 LAB 126
──────────────────────────────────────────────────────────────
                                              646 / 646
```
`126` mein **34 naye**: OpenAI dialect · arg alias · tool sach much chalta hai ·
router 26/26 hukm · har tool ka trigger · aur aap ke device wale **paanchon** bug.

---

## ▶️ Aap ke liye

1. Release → tag **`v4.10.0`** (versionCode 58)
2. Settings → 🧪 LAB → **⚡ AMAL ON**
3. Phir wohi teen cheezein try karo jo nakaam hui thin:
   - *"britness 100 karo"*
   - *"funk taka song lagao"*
   - *"whatsapp kholo aur Monarch ko hi bhejo"*
4. **📋 DIAGNOSTIC** bhej do — dekhna hai `tool wale turn` ab kitne hain

**Agla (P2b):** jo dimaag function-calling bilkul nahi jante, unke liye
`‹AMAL›` text protocol — taake `play_youtube(query="Funk` **likha nahi, KIYA** jaye.
