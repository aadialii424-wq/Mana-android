# 🎯 v5.5.0 — NISHANA: hukm ab sahih jagah jata hai

> Aap ke v5.4.0 diagnostic se bane **4 fix**. Pehle **jeet**, phir bug.

---

## ✅ Pehle: v5.4.0 kaam kar gaya

| | |
|---|---|
| 🤝 **SACH** | *"Chat khol kar matn type kar diya hai — ab **SEND ka button dabana hoga**"* — **jhoot khatam** 🎉 |
| ⚡ **BIJLI** | *"1 minute ka timer laga diya, Boss ⚡"* · `DIMAAG: ⚡ BIJLI × 1` |
| 🎙️ **SUNO** | `"ارینہ ایجنٹ اے ائی" → "arinh aijnt ae ayi"` |
| 👑 **MALIK** | poora ta'aruf |
| ⚡ **AMAL** | 35 tools · `tools na lene wale: koi nahi` |

---

## 🐛 BUG 1 — local hukm **DIMAAG ka kaam cheen leta tha**

```
AAP : "Camera khol ke picture lo"
MAYA: "ke picture lo meri list mein nahi hai. Try: WhatsApp, YouTube…"
```

`tryLocal` ka regex `khol` dekh kar **`ke picture lo`** ko **app ka naam** samajh
baitha — aur AI tak baat pohanchi hi nahi. *(Halanke `see_camera` tool maujood hai
aur ek dafa chala bhi tha: `AI TOOL: see_camera (pool)`.)*

**Ilaj:** app ka naam **APPS list mein ho to hi** local handle kare — warna
**`return null`** → **DIMAAG faisla karega** *(uske paas `open_app` aur `see_camera` dono hain)*.

## 🐛 BUG 2 — `"arena agent search karo"` → **`karo` dhoonda gaya**

```js
/(?:search|khojo|dhoondo)\s*(?:karo|kar do)?\s*(.+)/
```
Ye `search` ke **BAAD** wala hissa uthata tha. `"arena agent search karo"` mein
`search` ke baad sirf `"karo"` bacha tha → regex ne wohi query bana diya. 💀

**Ilaj:** ab **dono shaklein** —
```
"search X"           ✅   (pehle bhi chalti thi)
"X search karo"      ✅   (ab chalti hai)
```
Aur `"chrome par"` / `"google par"` khud hat jata hai. **Aur agar bacha hua matn
bekaar ho** (`karo`, `kar do`, `ok`) → **search hota hi nahi**, DIMAAG kare.

## 🐛 BUG 3 — 🚨 **bina kuch kiye "kar rahi hoon"** (vision ka jhoot)

```
AAP : "Kya tum meri screen dekh sakti ho?"
MAYA: "Ji haan Boss! Aankhein mode activate kar rahi hoon — screen par nazar rakh 👀"
   ← koi tool chala hi nahi. Kuch hua hi nahi.
AAP : "Ok to batao screen par kya nazar aa raha hai"
MAYA: "screen khol rahi hoon — photo khich rahi hoon"     ← phir jhoot
```

**P3 ka SACH guard yahan nahi laga** — kyunke wo tab chalta hai jab koi tool
`sure:false` ke sath chale. **Yahan koi tool chala hi nahi tha.**

**Ilaj — naya guard:** *"turn track hua · koi tool nahi chala · phir bhi
'kar rahi hoon' ka daawa"* = **JHOOT**. App khud theek kar deti hai:

> *"Main dekh sakti hoon, Boss — magar **camera KHOLNA parega**. Bolo "camera se
> dekho" to main camera khol dungi, phir photo khinch kar bata dungi.
> **Screen ka screenshot main abhi nahi le sakti.**"*

**Aur hifazat:** tool sach much chala ho → jawab **bilkul nahi chhua jata**.

## 🐛 BUG 4 — 📊 TRACE mein sirf `🧠 —` dikhta tha

Har bubble ke neeche khali chip. Wajah: trace **render ke waqt** `aiState.provider`
parhta tha, jo tab tak `—` ho chuka hota tha.

**Ilaj:** dimaag ka naam ab **jawab aate hi DARJ** hota hai *(`TRACE.brain()`)*.
Aur **kuch asli na ho to chip dikhta hi nahi**.
```
🧠 ⚡ Groq 340ms   🔧 brightness_control · done   🗣️ fish
```

## 🐛 BUG 5 — `"Maya"` likha, **angrezi** jawab aaya

```
AAP : "Maya"
MAYA: "Hey Boss, I'm here! What can I do for you today?"
```
MIRROR ka usool *"jis script mein likho"* — magar `"Maya"` Latin hai, to model
angrezi par chala gaya.

**Ilaj:** ab **ZUBAAN bhi mirror** hoti hai, sirf script nahi. Aur saaf likha hai:
> *"ANGREZI mein jawab SIRF tab jab user ne **POORA jumla** angrezi mein likha ho —
> sirf naam ya ek lafz (jaise "Maya", "ok") angrezi nahi ginta."*

---

## 🧪 Saboot
```
✅ CSS PASS · Settings 72 · AWAAZ 294 · DIMAAG 155 · 🧪 LAB 304
──────────────────────────────────────────────────────────────
                                              825 / 825
```
Section 22 mein **15 naye** — paanchon bug ke taale.

---

## ▶️ Test karo

1. Release → tag **`v5.5.0`** (versionCode 65)
2. Wohi jumle jo nakaam hue the:
   - *"Camera khol ke picture lo"* → 👁️ ab camera khulna chahiye
   - *"arena agent search karo"* → **arena agent** dhoondna chahiye, `karo` nahi
   - *"Kya tum meri screen dekh sakti ho?"* → jhoot ki jagah **sach**
   - *"Maya"* → **Roman Urdu** jawab, angrezi nahi
3. Har bubble ke neeche **📊 trace** — ab `🧠 ⚡ Groq 340ms` dikhna chahiye
