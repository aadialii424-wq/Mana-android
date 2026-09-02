# ⚡ v5.4.0 — P4: BIJLI (50ms) · 👁️ AANKHEIN

> P3 mein **lagaam** lag gayi thi. Ab **taqat** dena mehfooz hai.

---

## ⚡ BIJLI — phone control **50ms** mein, dimaag se **PEHLE**

**Aaj tak:**
```
aap bolte ho  →  dimaag sochta hai (2.6s p50, 13.2s p90)  →  tool chalta hai
```

**Ab (🟢 SABZ kaam ke liye):**
```
"torch on karo"
   ~40ms   🔦 TORCH ON        ← ho gaya. LLM ne abhi kuch nahi kiya.
  ~900ms   "Torch on kar di, Boss ⚡"   ← dimaag sirf JUMLA banata hai
```

### 🌐 Aur sab se bari baat — **internet ke bina bhi chalta hai**
Airplane mode · signal nahi · sab dimaag thak gaye — **koi farq nahi**.
Torch, brightness, volume, timer, battery — **sab chalenge**, aur Maya khud jumla
bana kar bol degi.

### Kya kya samajhta hai
```
torch on karo · torch band karo
britness 100 karo · brightness full · chamak aadhi kar do · roshni kam karo
awaaz 50 kar do
10 minute ka timer · 30 second ka timer
battery kitni hai
```
*(`britness` jaisi ghalat spelling bhi.)*

### 🔒 Aur kab BIJLI **kuch nahi** karti
| | |
|---|---|
| `Ammi ko call karo` | 🔴 surkh — **kabhi nahi** |
| `Monarch ko WhatsApp par bhejo` | 🔴 surkh — **kabhi nahi** |
| `alarm laga do 7 baje` | 🟡 zard — **nahi** |
| sirf `torch` *(on/off nahi bataya)* | yaqeen nahi → **dimaag hi kare** |
| sirf `brightness` *(value nahi)* | yaqeen nahi → **dimaag hi kare** |
| lamba jumla (90+ harf) | baat-cheet hai, hukm nahi |

**Dohri hifazat:** tool `IJAZAT` mein 🟢 SABZ hona chahiye **aur** `BIJLI.OK` list
mein bhi. Surkh tools us list mein **hain hi nahi**.

**Aur:** BIJLI nakaam ho → **chup-chaap dimaag ko de deti hai**. Wohi tool 8 second
mein dobara nahi chalta. Aur 📜 LEDGER + ⟲ UNDO pehle se maujood hain — **wapas bhi ho sakta hai**.

---

## 👁️ AANKHEIN — Maya ab **DEKH** sakti hai

**Ye taqat pehle se maujood thi** — `takePhoto()`, `pickImage()`, `visionAsk()` sab
likhe hue the. Magar:

- ❌ sirf **ek tang regex** par chalti thi: `ye dekho` / `photo khincho`
- ❌ **TOOL nahi thi** — is liye dimaag khud kabhi camera nahi khol sakta tha
- ❌ `"is bill mein total kitna hai"` → kuch nahi hota tha

### Ab do naye tools *(33 → 35)*

```
👁️ see_camera   "ye dekho" · "ye kya hai" · "is bill ka total" ·
                "ye likha kya hai" · "ye dawa kis liye hai"
🖼️ see_image    "gallery wali photo dekho" · "ye screenshot parho"
```

**Dimaag KHUD faisla karta hai** ke jawab ke liye dekhna parega.

**Aur sawal yaad rehta hai** — pehle tasveer aane par asal sawal kho jata tha.
Ab prompt saaf kehta hai: *"tasveer mein likha hua ho to parh kar batao. Hisab
maanga gaya ho to hisab karo."*

**🟡 ZARD darja** — camera **khulta** hai, aap dekh kar dabate ho. **Chori-chhupe photo nahi.**
**🤝 SACH** — `state: "started"`. Maya *"camera khol diya"* kehti hai, *"dekh liya"* ka **jhoota daawa nahi**.

---

## 🧪 LAB
```
📊 NAAP [ON]   🧹 SAAF [ON]   👑 MALIK [ON]   ⚡ AMAL [ON]
🎙️ SUNO [ON]   🤝 SACH [ON]   🗣️ BOLI [ON]   🛡️ IJAZAT [ON]
⚡ BIJLI [OFF]  ← YE NAYA
```

## 🧪 Saboot
```
✅ CSS PASS · Settings 72 · AWAAZ 294 · DIMAAG 155 · 🧪 LAB 288
──────────────────────────────────────────────────────────────
                                              809 / 809
```
Section 21 mein **35 naye** — har hukm ki pehchan, **8 hifazat** (surkh kabhi nahi,
yaqeen na ho to nahi, lamba jumla nahi), offline jumla, aur AANKHEIN ke 10 taale.

---

## 🗺️ Naqsha

```
✅ P0 NAAP   ✅ P1 SAAF   ✅ P2 AMAL   ✅ 🎙️ SUNO   ✅ 🗣️ BOLI
✅ 🔒 MEHFOOZ  ✅ P3 IJAZAT+TRACE   ✅ P4 BIJLI + AANKHEIN   ← 🎉

👉 P6  🕸️ KHUD-MUKHTAR   agar→to triggers · routines · khud-sikh router
   P5  🗣️ ZINDA          streaming + barge-in (Kotlin — aakhir mein)
```

---

## ▶️ Test karo

1. Release → tag **`v5.4.0`** (versionCode 64)
2. LAB → **⚡ BIJLI ON**
3. Ye try karo:
   - *"torch on karo"* → 📊 trace mein **`🧠 ⚡ BIJLI`** dikhega, aur foran ho jayega
   - **✈️ Airplane mode ON karo** → phir *"brightness 100 karo"* → **phir bhi chalega** 🔥
   - *"ye dekho"* ya *"is bill ka total kitna hai"* → 👁️ camera khulega
   - *"Ammi ko call karo"* → 🔴 ijazat ka card *(BIJLI ne haath nahi lagaya)*
4. 📋 DIAGNOSTIC bhejo — `NAAP` mein `tool` ka waqt **798ms se girna** chahiye
