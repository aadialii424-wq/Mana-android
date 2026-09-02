# 🎙️ v4.11.0 — SUNO: Maya ab aap ki AWAAZ theek samajhti hai

---

## 🎉 Pehle: pichhle release ka natija (aap ke apne diagnostic se)

| | v4.9.0 | **v4.10.0** |
|---|---|---|
| **tool wale turn** | **0 / 12** ☠️ | **27 / 44** 🔥 |
| tool chalne ka waqt | — | **798ms** p50 |
| kul waqt p50 | 18.0s | **12.3s** |
| kul waqt p90 | 31.2s | **18.4s** |
| soch leak | 7 / 12 | **3 / 44** |
| `tools na lene wale` | — | **koi nahi** |

**AMAL ne kaam kiya.** Gemini bina bhi 27 kaam ho gaye — Groq, Mistral aur LLM7 se.
Aur Gemini ka day-quota skip hone se **p90 31s → 18.4s** aa gaya.

---

## 🚨 Ab ASAL masla: Maya aap ki awaaz **samajh hi nahi pa rahi thi**

Aap ki apni chat se:

| Aap ne bola | Maya ne suna |
|---|---|
| "Funk Taka" | **"اس لاوا فنک"** · **"سوری فنک"** |
| "Monarch" | **"موناک"** · **"منار"** · **"منا"** |
| "reels kholo" | "ریلز کھولو" |

### Do alag alag kharabiyan thin

**1. LIKHAWAT** — STT ki zubaan `ur-PK` hai, is liye jawab **Urdu script** mein aata
hai. Aap Roman Urdu mein baat karte hain — chat mein Roman Urdu hi likha jana chahiye.

**2. PEHCHAN** — English naam Urdu ki awaaz mein pis jate hain. Aur (ye sab se bara tha):

```kotlin
// PURANA — Kotlin
val text = results?.getStringArrayList(RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                                                            ^^^^^^^^^^^^
```
**Android 3–5 andaze deta hai — hum sirf PEHLA lete the aur baqi phenk dete the.**
Aksar sahih jawab doosre ya teesre andaze mein hota hai.

---

## 🎙️ ILAJ — SUNO (teen tehen)

### A. Saare andaze lo, behtareen chuno
Kotlin ab **poori list** bhejta hai. SUNO har andaze ko **score** deta hai — jismein
sab se zyada **jaane-pehchane naam** hon (Monarch, WhatsApp, YouTube…) wohi jeetta hai.

### B. Urdu script → Roman Urdu
- **Lafz ki lughat pehle** (~90 aam lafz + app/brand ke naam)
- **Phir harf ba harf** (digraph pehle: بھ→bh, تھ→th, چھ→chh)

```
"چلو ٹھیک ہے"                    →  "chalo theek hai"
"یوٹیوب پر سونگ لگاؤ"            →  "YouTube par song lagao"
"موناک کو واٹس ایپ پر میسج بھیجو" →  "Monarch ko WhatsApp par message bhejo"
```

### C. Pise hue naam theek karo
```
monak    → Monarch          instgram → Instagram
manar    → Monarch          watsapp  → WhatsApp
                            yotube   → YouTube
```
**Aur do hifazatein:**
- ✅ **Aam lafz kabhi nahi badle** — `karo` kabhi `Chrome` nahi banta
- ✅ **4 harf ke lafz nishana nahi bante** — warna `"manar"` → `"Maya"` ban jata tha
  (chhota lafz bohat aasani se jeet jata hai). Ab `Monarch` jeetta hai.

### 🧠 Aur SUNO **seekhta** hai
Jo naam aap **khud likh** kar bhejte ho (Monarch, Funk Taka, kisi ka naam) — wo
**hamesha ke liye yaad** ho jate hain. Agli dafa awaaz se bologe to pehchan liye jayenge.
*(120 naam tak, phone par hi.)*

---

## 🩹 Do aur bug — aap ke UI CHECK se

**1. SAVE button ka background transparent tha**
```css
/* PURANA */  #saveSettings{ background:linear-gradient(...) }
```
`background` **shorthand** `background-color` ko **transparent** kar deta hai.
Naye WebView par gradient dikh jata tha, magar purane par button **ghayab**.
Ab `background-image` + solid `background-color` fallback. *(`UI CHECK` ne khud pakra tha)*

**2. SAAF se ek soch chhoot gayi**
```
"(Note: emojis not allowed per rules, remove)"    ← aap ki chat mein aaya tha
```
Ab ye shakl bhi pakri jati hai.

---

## 🧪 LAB mein naya switch

```
📊 NAAP        [ON ]
🧹 SAAF ZUBAAN [ON ]
👑 MALIK       [ON ]
⚡ AMAL        [ON ]     ← 0/12 se 27/44 isi ne kiya
🎙️ SUNO        [OFF]    ← YE NAYA
```

---

## 🧪 Saboot

```
✅ CSS PASS · Settings 72 · AWAAZ 293 · DIMAAG 155 · 🧪 LAB 157
──────────────────────────────────────────────────────────────
                                              677 / 677
```
`157` mein **31 naye** — aur SUNO ke test **aap ki asli chat ke jumlon** par chalte hain
(`موناک`, `منار`, `واٹس ایپ`, `یوٹیوب پر سونگ لگاؤ`).

---

## ⚠️ Jo abhi BAAQI hai (imaandari se)

Aap ki chat mein **do aur** masle the jo is release mein **theek nahi** hue:

**1. 🚨 "✓ Bhej diya, Sir!" ka jhoot** — WhatsApp ka tool sirf **draft khol kar
type** karta hai, **send nahi** karta (jab tak accessibility auto-send ON na ho).
Magar wo `done:true` lauta deta hai, is liye Maya "bhej diya" keh deti hai.
**Ye INVARIANT 3 ki khilaf-warzi hai** aur **agla kaam** yehi hai.

**2. 🧭 Kai-qadam wale hukm** — *"play store kholo AUR search mein messenger type karo"*
ya *"chrome par jao AUR lm arena search karo"* — abhi ek hi tool chalta hai aur baqi
jumla app ka naam samajh liya jata hai. Ye **P2b/P2d** (multi-tool + agent loop) hai.

---

## ▶️ Aap ke liye

1. Release → tag **`v4.11.0`** (versionCode 59)
2. Settings → 🧪 LAB → **🎙️ SUNO ON**
3. Ab **bol kar** try karo: *"Monarch ko WhatsApp par hi bhejo"* · *"Funk Taka song lagao"* · *"Instagram reels kholo"*
4. Dekho chat mein ab **Roman Urdu** likha aata hai ya nahi — aur naam sahih pakre gaye ya nahi
5. 📋 **DIAGNOSTIC** bhej do — log mein `SUNO: "…" -> "…"` ki lines hongi
