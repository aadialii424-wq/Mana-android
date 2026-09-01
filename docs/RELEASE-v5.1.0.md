# 🗣️ v5.1.0 — BOLI: hamare LEHJE mein · 🎀 pyari awaazein

> **Aap ne kaha:** *"Maya hamare accent mein nahi bol rahi — Gemini se jo artist
> uthaya tha (Kore), wo wala accent chahiye."*
>
> **Aur asal wajah AWAAZ ki nahi nikli — MATN ki thi.** 💀

---

## 🔬 Jarh — Fish ki apni dastavez ne bata di

```
Fish Audio docs:
  "The model DETECTS THE LANGUAGE of the input text and
   generates audio in that language."
```

Aur hum Fish ko **ye** bhejte the:
```js
text: "Ho gaya boss, brightness set kar di"
       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       LATIN harf  →  Fish samajhta hai ANGREZI  →  ANGREZI LEHJA
```

**Awaaz chahe kitni bhi Hindustani ho — matn Latin hoga to lehja angrezi hi rahega.**
Aap 19 turn se yehi sun rahe the.

---

## 🗣️ ILAJ — BOLI

**Screen par Roman Urdu hi rahega** *(aap ne yehi maanga tha)* — magar **bolne** ke
liye matn Devanagari (hi-IN) ya Urdu script (ur-PK) mein badal kar bheja jayega.

```
SUNO :  Urdu script  →  Roman            (aap ki awaaz ko LIKHNE ke liye)
BOLI :  Roman        →  Devanagari/Urdu  (Maya ke BOLNE ke liye)
        ↑ ek doosre ka bilkul ulta
```

**Misaalein (test se):**
| Screen par | Fish ko jata hai |
|---|---|
| `Ho gaya boss, brightness set kar di` | `हो गया बॉस, ब्राइटनेस सेट कर दी` |
| `WhatsApp par Monarch ko message bhejo` | `WhatsApp पर Monarch को मैसेज भेजो` |
| `theek hai boss` *(ur-PK)* | `ٹھیک ہے بوس` |

**🔑 Brand ke naam Latin hi rehte hain** — WhatsApp, YouTube, Monarch, Instagram.
Fish inhen theek parhta hai, aur baqi matn Devanagari hone ki wajah se **poora jumla
Hindustani lehje mein** aata hai.

**Kaam kaise karta hai:** ~200 aam Roman Urdu lafz ki lughat (sahih) + harf-ba-harf
fallback (taqreeban). English par bilkul nahi lagta. Pehle se Devanagari ho to dobara nahi badalta.

---

## 🎀 PYARI — Kore jaisi awaaz dhoondna

Aap ki Fish awaaz shayad angrezi bolne wali thi. Ab:

**🎀 PYARI ZANANA AWAAZEIN DHOONDO** button — **paanch talash ek sath**
(`hindi` · `urdu` · `female` · `indian` · `girl`), phir chhaan kar tarteeb:

```
zanana / meethi / soft / pyari  →  +60
mardana / deep / male           →  −80
Hindi ya Urdu zubaan            →  +40
zyada pasand ki gayi            →  +30 tak
```
Sab se pyari awaaz **sab se upar**.

**Aur Fish ki API mein `language` filter maujood thi** — hum use kar hi nahi rahe the.
Ab `?language=hi&language=ur` chalta hai.

---

## ⚠️ Aur ek sach — **aap ka pitch 1.3 zaya ja raha tha**

```
Settings: pitch = 1.3    ← aap ne awaaz patli/pyari karne ke liye barhaya
Fish API: prosody { speed, volume }    ← PITCH ka option hai hi nahi
```
**Fish par pitch ka koi asar nahi hota.** (Edge aur phone ki awaaz par hota hai.)
Ab Settings mein saaf likha hai: *"awaaz patli chahiye to **awaaz badlo**, pitch nahi."*

---

## 🎭 Aur agar aap ko **Kore hi** chahiye…

Diagnostic kehta hai: `gemini : 2/2 key zinda` — **Kore abhi bhi maujood hai!**
Magar `voiceEngine: fish` ki wajah se Gemini ki baari hi nahi aati.

| Aap kya chahte hain | Setting |
|---|---|
| **Kore, jab tak quota chale** *(~30/din, 2 keys)* | Awaaz Engine → **🪄 Auto** |
| Be-hisaab awaaz, Hindustani lehje mein | **🐟 Sirf Fish** + 🗣️ BOLI ON + 🎀 se awaaz chuno |
| Asli Urdu, koi hadd nahi, koi key nahi | **🌊 Sirf Edge** *(ur-PK-UzmaNeural)* |

---

## 🧪 LAB

```
📊 NAAP  [ON]   🧹 SAAF  [ON]   👑 MALIK [ON]
⚡ AMAL  [ON]   🎙️ SUNO  [ON]   🤝 SACH  [ON]
🗣️ BOLI  [OFF]  ← YE NAYA
```

## 🧪 Saboot
```
✅ CSS PASS · Settings 72 · AWAAZ 293 · DIMAAG 155 · 🧪 LAB 205
──────────────────────────────────────────────────────────────
                                              725 / 725
```

---

## ▶️ Aap ke liye — 3 qadam

1. Release → tag **`v5.1.0`** (versionCode 61)
2. LAB → **🗣️ BOLI ON** → Maya se baat karo. **Lehja foran badal jayega.**
3. Agar awaaz phir bhi pasand na ho → Settings → **🎀 PYARI ZANANA AWAAZEIN DHOONDO**
   → upar wali chuno → **🐟 SUNO** → pasand aaye to **SAVE**

*(Aur ek dafa **🪄 Auto** bhi try karo — Kore wapas aa jayegi jab tak quota hai.)*
