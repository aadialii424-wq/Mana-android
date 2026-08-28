# 🤖 MAYA — Personal AI Assistant

**Version 2.12.1 — 0-Budget Build • Android APK + Web PWA**

MAYA = aap ka apna JARVIS — voice controlled AI assistant (Gemini brain),
ab **asli Android app** (APK) ki soorat mein, native superpowers ke saath:

- 🎤 **Native voice input** — Urdu (ur-PK) / Hindi / English (Google recognition)
- 🗣️ **Native voice output** — system TTS engine (behtar Urdu awaaz)
- ⏰ **REAL system alarm** — *"subah 6 baje alarm lagao"* → clock app mein set!
- ⏱️ **REAL system timer** — screen band ho to bhi notification + bajega
- 🔋 Battery status, vibration, notifications
- 🔁 Auto-listen mode (screen-on lock ke saath)
- 🧠 Gemini AI brain (FREE API key — app ke andar guide)
- 🧷 Memory bank — *"yaad rakhna ki ..."*

---

## 🛠️ APK KAISE BANAYEIN — Step by Step (FREE, PC ki zaroorat NAHI)

### STEP 1 — GitHub account (free)
1. **github.com** kholo → **Sign up** → account banao

### STEP 2 — Repository banao
1. GitHub par login kar ke **+** (top right) → **New repository**
2. Naam do: `maya-android` → **Create repository** (Public ya Private dono chalega)

### STEP 3 — Ye files upload karo
1. Repository page par **"uploading an existing file"** link par click karo
2. Ye poora folder drag & drop karo (sab kuch: `.github`, `app`, gradle files, etc.)
   - **Ahem:** chhupi hui folders (`.github`) browser upload mein nahi dikhte —
     is liye GitHub web par manually banao:
     - **Add file → Create new file** → naam likho:
       `.github/workflows/build-apk.yml` → is file ka content paste karo → Commit
     - Baqi files (`settings.gradle`, `build.gradle`, `gradle.properties`,
       `app/build.gradle`, `app/src/...` sab) bhi **Create new file** se sahi
       path ke saath banao aur content paste karo (ye zaroori hai kyunki
       `.github` folder zip se upload nahi hota)
   - Binary files (icons) upload page se normal drag & drop hongi
3. **Commit changes** dabao

### STEP 4 — APK build (automatic)
1. Repository mein **Actions** tab kholo
2. **"Build MAYA APK"** workflow dikhegi — pehli push par khud chal jayegi
   (nahi chale to **Run workflow** button dabao)
3. **3-6 minute** wait karo (pehli baar Gradle dependencies download hoti hain)

### STEP 5 — APK download + install
1. Actions tab mein complete hone wali run par click karo (✅ green)
2. Neeche **Artifacts** section mein **MAYA-APK** dikhega → click kar ke
   **MAYA-APK.zip** download karo
3. Zip extract karo → andar `app-debug.apk` milegi
4. Phone mein APK kholo → **"Install unknown apps / Install anyway"** allow karo
   (Play Protect warning aaye to **Install anyway** — apna hi code hai, 100% safe)
5. MAYA app khulo → **INITIALIZE** → mic + notification permission allow karo

### STEP 6 — Gemini brain (2 minute)
1. `aistudio.google.com` → **Get API key** → copy
2. MAYA app → **⚙️ Settings** → API key paste → **SAVE**
3. Ho gaya — kuch bhi poocho! 🚀

---

## 📱 App mein kya hai

| Tab | Kaam |
|---|---|
| 💬 CHAT | Voice (🎤) ya type se baat karo |
| 🎯 HUD | Live clock, battery, AI status, system log, quick actions |
| ⚡ COMMANDS | Saari commands + RUN buttons + setup guides |
| ⚙️ SETTINGS | Naam, API key, awaaz, languages, memory management |

## 🗣️ Voice Commands (misal)

- "maya, subah 6 baje alarm lagao" → **real alarm**
- "maya, paanch minute ka timer lagao" → **system timer**
- "maya, open whatsapp" / "open youtube"
- "maya, youtube pe [gaana] chalao"
- "maya, search karo [kuch bhi]"
- "maya, battery kitni hai" / "waqt kya hua"
- "maya, yaad rakhna ki kal doctor appointment hai"
- "maya, call karo 0300xxxxxxx"
- Aur kuch bhi — Gemini khud jawab degi (usi zubaan mein!)

---

## ❓ MASLA HO TO (Troubleshooting)

| Masla | Hal |
|---|---|
| Mic nahi chalta | Settings → Apps → MAYA → Permissions → Microphone allow |
| Awaz nahi aati | Settings → Accessibility → Text-to-speech → Google TTS install |
| Urdu sunai nahi deti | Google app update karo (voice recognition isi se aati hai) |
| AI jawab nahi deta | API key check karo + internet on karo |
| Alarm nahi lagta | Phone ka Clock app check karo (kuch phones SKIP_UI block karte hain) |
| Build fail (Actions) | Run ka log kholo → error dekho → file theek kar ke dubara push |

## 🔒 Privacy

- API key + chat + memory **sirf aap ke phone** mein (app data)
- AI sawal **direct Google Gemini** ko jata hai — koi doosra server nahi
- Code 100% open — khud dekh lo! 😄

## 🌐 Web Version (FREE Deploy)

MAYA ka web version bhi hai — phone ke browser mein chalta hai!

### GitHub Pages se deploy (FREE, 2 minute):
1. Repository push karo (ye files automatically deploy ho jayengi: `public/` folder)
2. GitHub par **Settings → Pages** kholo
3. **Source** mein "GitHub Actions" select karo
4. **Save** dabao — 1-2 minute mein live ho jayega!
5. Link milega: `https://tumhara-username.github.io/maya-android/`

### Phone mein install karo (PWA):
1. Upar ka link phone ke Chrome mein kholo
2. Menu (⋮) → **Add to Home screen** → **Install**
3. MAYA ka icon phone pe aa jayega — app jaisa chalega!

### Netlify se deploy (FREE, alternative):
1. **app.netlify.com/drop** kholo (free account)
2. `public/` folder drag & drop karo
3. Live link mil jayega!

> **Note:** Web version mein alarm, wake word, aur auto-send jaise native features nahi chalte.
> Voice chat, AI brain, aur memory bank full kaam karte hain!

---

## 🗺️ Roadmap

- ✅ Phase 1 — PWA Pro (web app)
- ✅ Phase 2 — Native APK (ye!)
- ⏳ Phase 3 — Smart Brain (AI function calling, routines)
- ⏳ Phase 4 — Boss Level (wake word, offline AI)

