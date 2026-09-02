# 🚪 v5.8.0 — "MAYA" ke bagair KUCH NAHI · 🎤 qareebi awaaz

**P8a + P8b + P8c — sab kuch, ek build.**

---

## 🔒 P8a — SAKHT DARWAZA *(aap ki asal farmaish)*

```
"yeh karo"                →  ⛔ BILKUL KUCH NAHI
"brightness barhao"       →  ⛔ kuch nahi
"chalo Maya yeh karo"     →  ⛔ kuch nahi   (Maya SHURU mein honi chahiye)
"brightness barhao Maya"  →  ⛔ kuch nahi   (aakhir mein nahi chalega)

"Maya"                    →  ✅ "Ji Boss? Boliye... 🚪 15s"
"Maya brightness barhao"  →  ✅ seedha kaam
"hey Maya yeh karo"       →  ✅ (ek filler chalega)
```

### 🐛 Aur wo bug jo aap ki shikayat ki jarh tha
```js
// PURANA
if ((autoListen || wakeWord) && wasVoice) startListening();
//                                        ← "Maya" ki koi shart NAHI
```
**Wake mode ON hote hi Maya har jawab ke baad khud sunna shuru kar deti thi.**
Ab: **sirf tab jab DARWAZA khula ho.**

### 🚪 Darwaza
```
"Maya brightness barhao"   →  ✅        🚪 15 sec khula
"aur volume bhi"           →  ✅        🚪 dobara 15 sec  (Maya kehne ki zaroorat nahi)
"torch on karo"            →  ✅        🚪 dobara 15 sec
     … 15 sec khamoshi …              🔒 BAND
"yeh karo"                 →  ⛔ ab phir "Maya" chahiye
```
**"bas" · "theek hai" · "shukriya"** → darwaza **foran band**.
Settings mein slider: **`0` = har baar "Maya"** … **`60` = lambi baat-cheet**.

---

## 🩺 P8b — KAAN DOCTOR + recognizer ki SEERHI

### Ab hum jaan sakte hain ke masla kya hai
```
🩺 KAAN DOCTOR
  ✅ Mic ki ijazat        : mili hui
  ❌ Phone ka voice input : Android System Intelligence ⚠️
     ⚠️ YEHI SAB SE BARA MASLA HAI. Android 12+ par ye service
        SpeechRecognizer ke sath theek kaam NAHI karti.
  ⚠️ On-device recognizer : nahi — language pack download karni paregi
  🎯 Abhi chal raha hai   : google
  ⚠️ Battery optimization : MAYA par lagi hai — service mar sakti hai

  👉 KYA KARNA HAI:
     • [VOICE INPUT] dabao → "Speech Recognition & Synthesis" chuno
     • [BATTERY] dabao → MAYA ko "Unrestricted" karo
```
**Aur buttons seedha wahi screen kholte hain** — menu mein bhatakna khatam.
*(Aap ne bilkul theek kaha tha ke Default apps mein wo option hai hi nahi.)*

### 🎯 Teen darje ka recognizer
```
1. Android 12+ ka ON-DEVICE recognizer   (offline, isi kaam ke liye bana)
2. warna Google ka recognizer ZABARDASTI (AiAi bug ka ilaj)
3. warna aam wala
```

### 💀 Aur wo chhupa hua bug — **SUNO ki taqat mari hui thi**
```
WakeWordService : EXTRA_MAX_RESULTS = 6   ✅
MainActivity    : EXTRA_MAX_RESULTS = 1   ❌  ← MAIN MIC
```
**SUNO ka poora nizam "kai andazon mein se behtareen chuno" par khara hai — aur
main mic use sirf EK andaza deta tha.** *(Isi liye `"Funk Taka"` → `"اس لاوا فنک"`.)*

**Ab 6 andaze + har ek ka `CONFIDENCE`** *(jo pehle kabhi parha hi nahi gaya)*.
SUNO chunta hai: **jaane-pehchane naam × Android ka yaqeen**.

**Aur kam yaqeen par ghalat kaam nahi karti:**
> *"👂 Theek se sunai nahi diya — "…" kaha tha? Dobara boliye."*

---

## 🎤 P8c — QAREEB *(aap ki nayi farmaish)*

Android ka apna API — jo hum **ek bhi** istemal nahi kar rahe the:
```kotlin
setPreferredMicrophoneFieldDimension(+0.8f)   // 🔍 -1 poora kamra … +1 sirf qareeb
setPreferredMicrophoneDirection(TOWARDS_USER) // mic ka rukh AAP ki taraf
AudioSource.VOICE_RECOGNITION                 // ASR ke liye bana + AGC
NoiseSuppressor · AutomaticGainControl · AcousticEchoCanceler
```

### 🎧 KHAMOSHI KA PEHRA (VAD) — *"mic on/off" ka asal ilaj*
```
sasta AudioRecord chalta hai (zoom + shor-kush ke sath)
   │
   ├─ SANNATA        →  recognizer BILKUL band  💤
   └─ QAREEBI AWAAZ  →  mic chhoro → recognizer chalao → wapas pehre par
```
**Door ki dheemi awaaz (TV, doosra kamra) rad** — sirf **farsh se +14 dB** upar,
**300ms** tak wali awaaz par jaagti hai.

### 🧪 MIC TEST
```
🎤 MIC TEST
  Kamre ka shor   : 38 dB   (khamosh ✅)
  Aap ki awaaz    : 67 dB
  Farq (SNR)      : 29 dB   ✅ behtareen

  ── is device par kya chala ──
  ✅ Mic ZOOM (background rad)      ✅ Noise suppressor
  ✅ Mic ka rukh aap ki taraf       ❌ Echo canceler (is device par nahi)
```
**⚠️ Imaandari:** ye **darkhwast** hain, hukm nahi. Har device support nahi karta —
is liye **har ek ka asli natija** dikhaya jata hai. **Andaza nahi.**

---

## 🔒 Qanoon 2 — purani APK bhi chalti rahegi
Naya format `{t,c}` hai, magar purana `"matn"` bhi qubool hota hai *(test se locked)*.

## 🧪 Saboot
```
✅ CSS PASS · Settings 72 · AWAAZ 294 · DIMAAG 155 · 🧪 LAB 411
──────────────────────────────────────────────────────────────
                                              932 / 932
```
Section 25 mein **43 naye** — darwaze ke har raaste, `"yeh karo"` ke 4 imtihan,
`"bas"`, 0-sec mode, mic zoom, VAD, DOCTOR, aur confidence.

---

# ▶️ Test karo

1. Release → tag **`v5.8.0`** (versionCode 68)
2. ⚠️ **Wake word OFF → ON** *(nayi settings service tak jane ke liye)*
3. Settings → LAB → **🩺 KAAN DOCTOR** ← **sab se pehle ye**
   → jo bhi ❌ ho, uske button dabao
4. **🧪 MIC TEST** — 1 sec chup, phir *"Maya kaise ho"*
5. Ab test:
   - *"yeh karo"* → **kuch nahi hona chahiye** ✅
   - *"Maya"* → **"Ji Boss? Boliye… 🚪 15s"**
   - *"brightness barhao"* *(15 sec ke andar)* → chalna chahiye
   - *"bas"* → darwaza band
   - phir *"yeh karo"* → **kuch nahi**

**DOCTOR + MIC TEST + 👂 WAKE ka haal — teeno bhej dena.**
