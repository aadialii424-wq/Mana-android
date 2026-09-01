# 👂 P8 — KAAN v2: **"Maya" ke bagair kuch nahi**

> **Ye plan hai, code nahi.** Maya ko haath nahi lagaya — `npm test` **889/889**.
> Aap ke "haan" ke baad hi ek line likhi jayegi.

---

# HISSA 1 — 🔬 FORENSIC: teen asli wajhein

Maine sirf apna code nahi dekha — **Android ki asal hadd** bhi tehqeeq ki. Teen alag
wajhein nikleen, aur **teeno alag ilaj** maangti hain.

---

## 1.1 🚨 `SpeechRecognizer` wake word ke liye **bana hi nahi**

Ye meri raaye nahi — Android developers ki barson ki gawahi hai:

> *"Google will eventually find a way to prevent continuous listening, **as it's not
> what the API was designed for**."*
> *"This is a **known bug**… the only way around it is to **recreate the SpeechRecognizer
> object every time**."*
> *"…the service running it **dies silently**."*

**`startListening()` ek dafa ka kaam hai:** mic kholo → bolo → band. Khamoshi par
`ERROR_NO_MATCH (7)` → hum dobara shuru karte hain → **yehi "mic on/off" hai.**

> **v5.7.0 ka backoff us cycle ko DHEEMA karta hai — KHATAM nahi karta.**
> Kyunke hum aik aisi cheez ko hotword engine bana rahe hain jo hai hi nahi.

---

## 1.2 🚨 **Android 12+ ka AiAi bug — aur aap Android 14 par hain**

Ye sab se badi dariyaft hai:

> *"AiAiSpeechRecognition is part of the **Android System Intelligence** package, new
> for Android 12… however it seems to be **incompatible** with the SpeechRecognizer API."*
> *"**This can be fixed by the user**: Settings → Apps → Default apps → **Digital
> assistant app → Voice input** → change to **'Speech services by Google'**."*

**Aap ka phone: TECNO KL4 · Android 14.** TECNO jaise phones par default voice-input
aksar Google ke bajaye **Android System Intelligence** hota hai — aur us par
`SpeechRecognizer` **khamoshi se nakaam** hota hai.

**Ye bilkul aap ki alaamat se milta hai:** mic chalta hai, band hota hai, aur **kuch nahi hota**.

**Do ilaj:**
1. Aap Settings mein badal dein *(app khud batayegi kaise)*
2. Ya hum **program se hi Google ka recognizer maangein** —
   `ComponentName("com.google.android.googlequicksearchbox", "…GoogleRecognitionService")`

---

## 1.3 🚨 Hum recognizer ko **khamoshi mein bhi** chalate rehte hain

Abhi koi **khamoshi ka pehra (VAD)** nahi. Kamre mein sannata ho tab bhi Google ka
recognizer har 1-3 second mein chalta rehta hai. Nateeja: Android throttle, battery,
aur wo cycle.

```
$ grep -c "AudioRecord|createOnDeviceSpeechRecognizer|ComponentName"  → 0
```
**Ek bhi nahi. Teeno modern hal ghayab hain.**

---

## 1.4 🔴 Aur aap ki ASAL farmaish wala bug

```js
function afterSpeak(wasVoice){
  if ((settings.autoListen || settings.wakeWord) && wasVoice)
      setTimeout(startListening, 350);          // ← "Maya" ki koi shart NAHI
}
```

**Wake mode ON hote hi Maya har jawab ke baad khud sunna shuru kar deti hai — bina
"Maya" kahe.** Bilkul wahi jo aap **nahi** chahte.

---

# HISSA 2 — 🏗️ ILAJ: chaar tehen

## 🎧 TEH 1 — KHAMOSHI KA PEHRA (VAD) · *cycle ka asal ilaj*

**Koi nayi library nahi.** `AudioRecord` Android mein pehle se hai:

```
AudioRecord  (sasta, khamosh, Google ki service se azad)
     │  har 100ms awaaz ki shiddat (RMS) naapo
     │
     ├─ SANNATA  →  KUCH NA KARO. Recognizer band. Koi cycle nahi. 💤
     │
     └─ AWAAZ AAYI (threshold se upar, 300ms tak)
              ↓
         SpeechRecognizer 4 second ke liye chalao
              ↓
         "Maya" mila?  →  jaago      warna  →  wapas so jao
```

| | Pehle | Ab |
|---|---|---|
| Recognizer chalta hai | **hamesha** (har 1-3 sec) | **sirf jab koi bole** |
| Mic ka on/off | lagatar | **taqreeban khatam** |
| Battery | bhaari | halka |
| Android throttle | roz | shaz o nadir |

> **Khamosh kamre mein Maya ka recognizer BILKUL nahi chalega.**

## 🎯 TEH 2 — SAHIH RECOGNIZER (Android 14 ke liye)

```
Android 12+  →  SpeechRecognizer.createOnDeviceSpeechRecognizer()
                (offline, isi kaam ke liye bana, network nahi)
     ↓ na chale to
             →  Google ka recognizer ZABARDASTI maango (ComponentName)
     ↓ na chale to
             →  aam recognizer (jo abhi hai)
```
**Aur teeno mein se jo chala, wo 👂 report mein likha jayega** — taake hum jaan sakein.

## 🔒 TEH 3 — SAKHT DARWAZA *(aap ki asal farmaish)*

```
"yeh karo"                  →  ❌ KUCH NAHI. Bilkul chup.
"Maya"                      →  ✅ "Ji Boss?"  + darwaza khula
"Maya yeh karo"             →  ✅ "Ji Boss" + seedha kaam
"chalo Maya yeh karo"       →  ❌ (Maya SHURU mein honi chahiye)
```

**Teen sakht qawaid:**
1. **Wake word LAZMI** — bina "Maya"/"Boss" ke, wake mode mein **kuch nahi** hota
2. **SHURU mein** — pehle 2-3 lafzon ke andar. Beech ya aakhir mein ho to nahi
3. **`afterSpeak` ka khud-sunna wake mode mein BAND** — *(yehi bug tha)*

### 🚪 "Khula darwaza" — aur is par mujhe aap se poochna hai
Aap ne kaha: *"Maya bolun to activate ho, reply kare, **phir kaam aage ka jo bhi**"*.

| | Tareeqa |
|---|---|
| **A** | **Har baar "Maya" kaho** — sab se sakht. Bilkul jo aap ne kaha |
| **B** | "Maya" ke baad **15 second ka darwaza** — us mein bina Maya kahe baat chalti rahe, phir khud band |
| **C** | Darwaza khula rahe **jab tak aap "bas"/"theek hai" na kaho** |

*(Mera mashwara: **B** — kyunke ek hi kaam ke liye baar baar "Maya" kehna thaka deta hai.
Magar faisla aap ka, aur switch dono taraf hoga.)*

## 🩺 TEH 4 — KAAN DOCTOR *(jab phir bhi na chale)*

Ek button jo **asal wajah** dhoond kar de — andaza nahi:

```
🩺 KAAN DOCTOR

  ✅ Mic ki ijazat            : mili hui hai
  ❌ Voice input service      : Android System Intelligence
     ⚠️ YEHI MASLA HAI. Android 12+ par ye SpeechRecognizer ke sath
        theek kaam nahi karti.
     👉 Settings → Apps → Default apps → Digital assistant →
        Voice input → "Speech services by Google"
     [ SETTINGS KHOLO ]
  ✅ On-device recognizer     : maujood (Android 14)
  ⚠️ Battery optimization     : MAYA par lagi hui hai — service mar sakti hai
     [ SETTINGS KHOLO ]
  ✅ Foreground service       : chal rahi hai
  📊 aakhri 5 min mein        : 12 dafa awaaz, 2 dafa recognizer, 0 wake
```

---

# HISSA 3 — 🅱️ Agar phir bhi na chale: **Porcupine**

Imaandari se: agar TEH 1-4 ke baad bhi Android apna recognizer theek na chalaye, to
**asli hotword engine** hi hal hai.

| Engine | Sach |
|---|---|
| **Porcupine** (Picovoice) | **97%+ durust · <1MB · poora offline · asli Android SDK.** Magar **AccessKey** chahiye, aur muft plan **sirf zaati istemal** (3 device tak). Aap ke liye ye **theek** hai — magar ye ek **nayi library** hai (APK barhega, build ka khatra) |
| **openWakeWord** | behtareen, magar **sirf Python** — Android SDK hai hi nahi ❌ |
| **Vosk** | muft, offline, Android AAR — magar **~40MB model** APK mein |
| **Snowboy / PocketSphinx** | **marr chuke** (2020) ❌ |

**Mera mashwara: pehle TEH 1-4 karo.** Ye **koi nayi library nahi** maangte aur bohat
mumkin hai masla wahin hal ho jaye — khaas kar **1.2 (AiAi bug)** to sirf ek setting ka
kaam ho sakta hai.

---

# HISSA 4 — 🛡️ Battery aur privacy

| Qanoon | |
|---|---|
| 🔋 VAD `AudioRecord` bohat halka hai — recognizer se **kai guna** kam | |
| 🔇 **Khamoshi mein kuch bhi record nahi hota** — RMS ke ilawa koi data nahi | |
| 🚫 **Koi audio kabhi mehfooz nahi hoti** — na phone par, na kahin bheji jati hai | |
| 🔴 Battery <10% → wake service khud so jaye | |
| 🌙 Khamosh ghante *(P6 se)* — raat ko band | |
| 🎛️ **Notification hamesha nazar aayegi** — chori-chhupe sunna nahi | |

---

# HISSA 5 — 🧪 SABOOT (~45 naye, kul ~934)

| Hissa | Kya sabit karega | Kitne |
|---|---|---|
| **SAKHT DARWAZA** | `"yeh karo"` → **kuch nahi** · `"Maya yeh karo"` → chale · beech mein Maya → nahi · `afterSpeak` wake mode mein khud na sune | **14** |
| Khula darwaza | 15 sec mein bina Maya chale · phir band · "bas" par foran band | 8 |
| VAD | sannate mein recognizer band · awaaz par chale · shor mein galat na jaage | 8 |
| Recognizer chunna | on-device → Google → aam, aur jo chala wo report mein | 6 |
| DOCTOR | har wajah ka sahih naam + ilaj ka button | 9 |

**Aur ek khaas test:** *"wake mode mein, bina 'Maya' ke, KOI bhi jumla kuch na kare"* —
30 aam jumlon par chalega.

---

# HISSA 6 — 📅 QADAM

| | Kya | Kotlin? | Khatra | Faida |
|---|---|---|---|---|
| **P8a** | 🔒 **SAKHT DARWAZA** + 🚪 khula darwaza | ❌ **sirf JS** | 🟢 | **Aap ki asal farmaish — foran** |
| **P8b** | 🩺 **KAAN DOCTOR** | ✅ chhota | 🟢 | **Asal wajah pata chal jayegi** |
| **P8c** | 🎧 **VAD** + sahih recognizer | ✅ | 🟡 | mic ka on/off khatam |
| **P8d** | 🅱️ Porcupine *(sirf agar zaroorat pari)* | ✅ nayi library | 🔴 | aakhri chara |

**P8a + P8b ek sath karna behtar hai** — kyunke:
- **P8a mein Kotlin ko haath hi nahi lagta** *(sirf `index.html`)* — yani aap ki farmaish
  **bina kisi khatre ke** poori ho jati hai
- **P8b batayega ke asal masla kya hai** — shayad wo AiAi wali setting ho, aur phir
  **P8c ki zaroorat hi na pare**

---

# ❓ AB FAISLA AAP KA — teen sawal

### 1️⃣ 🚪 Khula darwaza — kaunsa?
| | |
|---|---|
| **A** | **Har baar "Maya"** — sab se sakht |
| **B** | **15 second ka darwaza** *(mera mashwara)* |
| **C** | Khula rahe jab tak **"bas"** na kaho |

### 2️⃣ Kaunse qadam abhi?
| | |
|---|---|
| **P8a + P8b** *(mera mashwara)* | Aap ki farmaish foran poori + asal wajah maloom |
| P8a se P8c | sab kuch ek sath (Kotlin badlega) |
| Sirf P8a | sab se mehfooz, magar mic ka on/off rahega |

### 3️⃣ Aur ek chhoti cheez — **abhi jaanch lein?**
Ye **ek minute** ka kaam hai aur shayad sab kuch hal kar de:

> **Settings → Apps → Default apps → Digital assistant app → Voice input**
> Agar wahan **"Android System Intelligence"** likha hai to use badal kar
> **"Speech services by Google"** kar dein — phir wake word try karein.
>
> **Ye kar ke bata dein — is se pata chal jayega ke P8c ki zaroorat hai bhi ya nahi.**

**Aap ka "haan" aane tak ek line bhi nahi likhunga.** 👑

---

# HISSA 7 — 🎤 QAREEB: "sab se nazdeek wali awaaz suno, background ignore karo"

> **Aap ki nayi farmaish.** Aur tehqeeq ne do cheezein nikalin: Android mein **bilkul
> wahi API maujood hai** jo aap maang rahe ho — aur hum **ek bhi** istemal nahi kar rahe.

## 7.1 🔬 Forensic

```
$ grep -c "NoiseSuppressor|AutomaticGainControl|AcousticEchoCanceler|
           VOICE_RECOGNITION|CONFIDENCE_SCORES|MicrophoneDirection|FieldDimension"

  MainActivity.kt      0  ❌
  WakeWordService.kt   0  ❌
  (baqi sab)           0  ❌
```
**Saaton mein se ek bhi nahi.**

### 💀 Aur ek chhupa hua bug — SUNO ki taqat **mari hui** hai

```
WakeWordService.kt : EXTRA_MAX_RESULTS = 6    ✅ (maine v5.7.0 mein theek kiya)
MainActivity.kt    : EXTRA_MAX_RESULTS = 1    ❌ ← MAIN MIC
```

**SUNO (v4.11.0) ka poora nizam "kai andazon mein se behtareen chuno" par khara hai —
aur main mic use sirf EK andaza deta hai.** Yani wo feature banaya to gaya, magar
**asal mic par kabhi chala hi nahi.**

*(Isi liye `"Funk Taka"` → `"اس لاوا فنک"` ho jata tha — sahih jawab shayad andaza #3 mein tha, magar hum ne maanga hi nahi.)*

---

## 7.2 🎤 Android ka apna **MIC ZOOM** (API 29+ · aap Android 14 par hain)

```kotlin
setPreferredMicrophoneFieldDimension(zoom: Float)
   -1.0  = wide angle     (poora kamra)
    0.0  = aam
   +1.0  = MAXIMUM ZOOM   (sirf qareeb ki awaaz — background RAD)   ← YE

setPreferredMicrophoneDirection(MIC_DIRECTION_TOWARDS_USER)
   phone ke us taraf ka mic jo AAP ki taraf hai
```

> Android ki apni dastavez: *"the desired field dimension of microphone capture.
> Range is from **-1 (wide angle)**, through 0, to **+1 (maximum zoom)**."*

**Ye lafz-ba-lafz wahi hai jo aap ne maanga: "qareebi awaaz par gaur karo, background ignore karo."**

---

## 7.3 🏗️ Ilaj — teen tehen

### 🎯 T1 — **JO SUNA, US MEIN SE BEHTAREEN CHUNO** *(foran, sab se sasta)*

| | Pehle | Ab |
|---|---|---|
| Main mic ke andaze | **1** | **6** |
| Confidence scores | parhe hi nahi jate | **har andaze ka score JS ko** |
| Chunne ka tareeqa | pehla, aankh band kar ke | **score × jaane-pehchane naam** *(SUNO ka lexicon)* |
| Kam yaqeen par | ghalat kaam kar deti thi | **"samajh nahi aayi, dobara bolo"** |

```kotlin
results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)   // abhi bilkul istemal nahi
```

> **Ye akela `"Funk Taka"` aur `"Monarch"` wale masle bohat kam kar dega** —
> aur is mein **koi khatra nahi**, sirf ek number 1 se 6 karna hai.

### 🎤 T2 — **MIC KA ZOOM** *(VAD ke sath, P8c)*

```kotlin
AudioRecord(AudioSource.VOICE_RECOGNITION, …)      // ASR ke liye bana source + AGC
   .setPreferredMicrophoneDirection(MIC_DIRECTION_TOWARDS_USER)
   .setPreferredMicrophoneFieldDimension(1.0f)     // 🔍 MAXIMUM ZOOM
NoiseSuppressor.create(sessionId)                  // shor kam
AutomaticGainControl.create(sessionId)             // awaaz barabar
AcousticEchoCanceler.create(sessionId)             // Maya ki apni awaaz na sune ⚠️
```

**⚠️ Imaandari:** ye **darkhwast** hain, hukm nahi — har device support nahi karta.
Har call `true`/`false` lautati hai. **Is liye 🩺 DOCTOR mein saaf likha jayega ke
aap ke TECNO par kaunsi chali aur kaunsi nahi.** *(Andaza nahi — saboot.)*

### 📏 T3 — **NAZDEEKI KA PAIMANA** *(asal "background ignore")*

Qareeb ki awaaz **buland** hoti hai, TV/doosra kamra **dheema**. To:

```
1. KHAMOSHI ka farsh naapo      → kamre ka aam shor kitna hai
2. Aap ki awaaz ka paimana seekho → "Boliye: Maya" (ek dafa, calibration)
3. Har awaaz par:
      awaaz − shor  =  SNR
      SNR kam?  →  ⛔ ye door ki awaaz hai. RAD.
      SNR theek? →  ✅ ye AAP ho. Suno.
```

> **Aur ye khud seekhta rahega** — jaise jaise aap bolte jayenge, paimana behtar hota jayega.

**Aur ek zaroori faida:** 🔇 **Maya apni hi awaaz nahi sunegi.** Jab wo bol rahi ho,
mic ka darwaza band *(+ echo canceler)* — warna wo apne hi jumle par jaag jati hai.

---

## 7.4 🎛️ Aur ye sab **aap ke haath mein**

```
🎤 MIC KA ZOOM
   wide ──────────●─── max
   (poora kamra)      (sirf qareeb)     Abhi: +0.8

📏 KITNI DOOR TAK SUNO
   sirf paas ●──────────── door tak     Abhi: qareeb

🔇 Maya apni awaaz na sune            [ ON ]
🧪 [ MIC TEST — 5 second boliye ]
```

**🧪 MIC TEST** kya batayega:
```
🎤 MIC TEST

  Kamre ka shor      : 38 dB   (khamosh)
  Aap ki awaaz       : 67 dB   (buland aur saaf)
  Farq (SNR)         : 29 dB   ✅ behtareen
  Mic zoom           : ✅ chala (+0.8)
  Noise suppressor   : ✅ chala
  Echo canceler      : ❌ is device par nahi
  Andaze mile        : 5   (behtareen: "Maya" — 0.94 yaqeen)

  👉 Aap ki awaaz saaf pohanch rahi hai.
```

---

## 7.5 📅 Naya naqsha

| | Kya | Kotlin? | Khatra | Faida |
|---|---|---|---|---|
| **P8a** | 🔒 SAKHT DARWAZA + 🚪 slider | ❌ **sirf JS** | 🟢 | aap ki farmaish |
| **P8b** | 🩺 KAAN DOCTOR + 🎯 recognizer ki seerhi | ✅ chhota | 🟢 | asal wajah |
| **P8b+** | 🎯 **T1 — 6 andaze + confidence** | ✅ **bohat chhota** | 🟢 | **SUNO zinda ho jayegi** |
| **P8c** | 🎧 VAD + 🎤 **T2 mic zoom** + 📏 T3 SNR | ✅ | 🟡 | background ignore |

> **T1 (6 andaze + confidence) ko main P8b ke sath hi kar dunga** — ye sirf ek number
> badalna hai aur **iska faida foran nazar aayega**.

## 7.6 🧪 Naye test (~20 aur, kul ~954)

har andaze ka confidence · kam yaqeen par "dobara bolo" · SNR se door ki awaaz rad ·
Maya apni awaaz par na jaage · mic zoom nakaam ho to bhi sab chale · MIC TEST ki report
