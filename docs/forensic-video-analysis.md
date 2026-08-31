# 🔬 FORENSIC ANALYSIS — "Maya AI Android Full Setup Hindi" (GMqckXxK46c)

> **Target video:** https://youtu.be/GMqckXxK46c
> **Channel:** HunterIsLive (@TheHunter-AI) — Maya AI ka developer khud
> **Length:** 1:10:42 · **Uploaded:** 2026-08-30 · **App version in video:** 4.8.3
> **Sources used:** Full video transcript (every segment), chapter timestamps, getmaya.online/android official spec, AppBrain/Play listing
> **Purpose:** Video ke har segment ka study → hamare MAYA rebuild ka blueprint

---

## 0️⃣ BUSINESS MODEL (jo video se confirm hua)

| Element | Detail |
|---|---|
| Free tier | Roz **10 min** voice chat, **saare tools LOCKED** (sirf demo baat cheet) |
| License | **₹699 one-time, lifetime** (1 license = 1 phone, dashboard se unlink/re-activate) |
| Payment | UPI (QR / UPI ID), out-of-country ke liye Binance + WhatsApp proof |
| License server | Website dashboard par license key + linked device list; app me "ACTIVATE" me key paste → 30–40s first-time activation |
| Updates | In-app: "Check for updates" button → naya version direct install (4.8.3 chal raha tha) |
| Play Store | **NAHI hai** — accessibility permission ki wajah se; Play Protect disable karna padta hai install se pehle |

---

## 1️⃣ SEGMENT-BY-SEGMENT FORENSIC TIMELINE

### 🎬 0:00–3:38 — Overview + GF persona demo
- Video shuru hoti hai **live conversation demo** se: user YouTube stats poochta hai → Maya jawab: *"1540 subscribers, 1.3M views, congratulations babu!"* (YouTube Data API live integration + emotional GF tone)
- **Conversation Mode ON** karke dikhaya — voice "thoda emotional, real" lagti hai (6–7s delay)
- **Sub-agents background me kaam kar rahe hain** — Maya khud bolti hai: *"Jaanu sub-agents background me kaam kar rahe hain, kisi mission ka status dekhna hai?"*
- **Memory test:** favorite song poocha → *"yaad nahi aa raha, dobara bata do, is baar pakka yaad rakhungi"* → user ne *"Violento"* bataya → play kiya YouTube par → **aage khud yaad se refer karti hai** (memory update flow)
- **Automation demo:** "screenshot lo aur Maya user ko WhatsApp pe bhejo" → screenshot liya → contact nahi mila → **khud clarify poocha** ("sahi naam batao ya number confirm karwa doon") → "Mantosh Singh" → retry → sent ✅ *(yeh agentic error-recovery pattern hai — bahut important design detail)*

### 📥 3:38–7:06 — Download aur license process
- getmaya.online → Google login → 3-dot menu → "Maya AI for Android"
- Free vs Buy section → ₹699 → phone number → payment page (UPI QR/ID, Binance option)
- **Dashboard:** license key + linked device (UNLINK button) — ek waqt me 1 device
- License key copy → APK download from site

### 📱 7:06–10:04 — APK installation
- **Play Protect → "Scan apps with Play Protect" DISABLE** (accessibility permission ki wajah se)
- Install → Open → **FREE MODE: 10 min/day limit screen**
- ACTIVATE → license paste → 30–40s loading (first time)
- **In-app update system:** current version 4.8.3, "Check for updates" → direct install

### 🔑 10:04–12:51 — Free Gemini API key setup
- Mic dabaya → **genuine popup: "Get Free API Key"** (API key ke bina baat nahi karta)
- Google AI Studio → login → agreement → **project create** (kai logon ka project nahi banta — yeh common error) → Gemini API key → copy
- App → Settings → "Get Gemini Key" input → paste → SAVE → Maya live
- **Key insight:** ~64,000 tokens/min free, "hamare use case ke liye kaafi"

### 🔐 12:51–17:24 — Required permissions setup
Settings → **Advanced → Permissions** section (screen lock ke neeche):
1. **Default assistant** — Google Assistant ko REPLACE kar ke Maya (long-press home / power)
2. Camera
3. Phone calls (+ call answer)
4. Location
5. Contacts
6. Gallery & files
7. Bluetooth
8. **Notification access** — "App was denied access" error aata hai → App info → 3-dot → **"Allow restricted settings"** → phir allow
9. **Accessibility service** — touch/click/screen-read automation ke liye (OPTIONAL — bina iske sirf baat-cheet chalegi, automation nahi)
10. **Battery exception** — "Allow background activity" (nahi to background me Maya band ho jati hai — common complaint)
11. **Screen share permission** — har session me DObara poocha jata hai **by design** (privacy: Maya hamesha screen na dekhe) — "yeh bug nahi hai, aise hi design hai"

### 👤 17:24–18:44 — Personal settings
- **Gender: Male/Female** — *Girlfriend mode sirf Male select karne par available* (female pe GF mode lock)
- Phone number
- **Music app:** Spotify / YouTube / YouTube Music
- Favorite song (settings me ya baat-cheet me batao — memory se permanent)
- **YouTube channel ID** (naam nahi, ID) — YouTube stats ke liye
- Data API key **developer ne pre-filled** rakhi hai (user ko daalne ki zaroorat nahi)

### 🗣️ 18:44–20:53 — Name, memory, voices, personalities
- **Assistant name** badal sakte ho (UI branding "Maya" hi rehta hai; wake/baat usi naam se)
- **Girlfriend mode** toggle
- **"Maya remembers"** toggle — conversation se important facts auto-extract + **update bhi karti hai** (naya fact bola to purana replace)
- **Voices:** per-persona multiple voice options, **preview play buttons** (sample line sunai deta hai)
- **3 personas: MAYA / FRIDAY / VENOM** (Venom = alien/funny: *"Hum Venom hain. Bolo insaan."* 😄)
- Voice se switch: *"Maya Venom ko bhejo"* — **temporary (session only)**; settings ka default persona har boot par active

### 💬 20:53–23:44 — Conversation mode, language, call alerts
- **Conversation Mode (beta):** emotional/real voice, **6–7s delay**; Normal mode: **~0.5s**
- **WhatsApp message alerts** on/off
- **Languages:** Hindi+English mix, Hindi, Bhojpuri, Haryanvi, Rajasthani, English, Punjabi, Bangla, Assamese, Nepali, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam
- **Auto start (boot)** toggle
- **Proactive Maya** — khud ba khud kuch bol ti hai (memory-based) — demo me dekha: *"khoye kya babu, ab to Violento bhi yaad ho gaya"* 😄
- **Call announcement** on/off + **"Keep my ringtone playing"** + ringtone volume reduce
- **Driving mode:** "Maya driving mode on" → call cut + **custom SMS with variables** `{name}` template system

### 🧩 23:44–24:39 — Skills
- Skills section: **YouTube upload, social posting, web designing** enabled
- **Skills GitHub se fetch hoti hain** — developer naya skill push kare → app update ke bina showcase hone lagti hain (install/uninstall/refresh buttons)
- Video ke waqt **8 skills available**

### 🤖 24:39–30:42 — Sub-agents & model/API setup
- **Coding models selector** — Gemini free models (3.1 flash / 2.5 flash / 3.5 flash) — **rate limiting ~15 requests** problem
- **Custom providers:** Add Provider → name + **base URL + model ID + API key** → ya **"Pick Preset"** (Claude, Grok, OpenRouter, OpenAI, DeepSeek, Mistral, Groq)
- Free me **NVIDIA NemoTron Ultra 550B** recommend kiya (acchi rate limits)
- **Multi-provider failover:** ek pe rate limit → automatic doosre pe switch → **kaam wahi se resume** (shuru se nahi)
- Sub-agent + coding agent same models use karte hain

### 📧 ~30:42–32:14 — Email setup
- Email ID + password → Maya mails **read/reply/delete** (spam delete bhi)
- Signature format (regards + name + contact)
- Custom **SMTP** (host + port) for professional emails

### 💬 ~32:14–33:30 — WhatsApp groups & reports
- **Sensitive groups blocklist** — in groups me Maya kabhi message nahi karegi
- **Report formats:** e.g. "Daily Production Report" — format name + **exact group name** + message template with **variables** `{date}`, `{shift}`, `{production}`, `{remarks}` → Maya values batao → auto-fill → auto-send group me

### 📱 ~33:30–35:10 — Social media automation
- Handle name (optional — captions me use hoga)
- Platforms: **Instagram + Facebook**
- **Caption tone:** funny / professional / simple / motivational / custom prompt
- **Daily story:** time set (8AM) → Maya **khud phone unlock karegi → Facebook kholegi → story upload → phone lock** (poora autonomous loop!)
- Auto-post toggle + **scheduled posts list**

### 🔌 35:10–37:23 — Connectors, backup, customization
- **Connectors:** GitHub (PR generate, issues read, code download), Google Drive (photos upload/download/read), Notion, Telegram, GitLab, Linear
- **Backup:** memories ka backup/restore (uninstall ya naya phone)
- **Customize:** Themes — **"Midnight Obsidian"** etc. + light themes; text size, text style, **corner radius (soft/sharp)**, reset to normal
- **Appearance:** overlay styles (home screen **2D live model character** vs **AR character**), colors (orange pick kiya), size
- **Behavior:** **floating orb** + **edge glow** (screen ke charon taraf gradient ring ghoomta hai — battery heavy, toggle)

### 🔊 37:23–45:19 — Audio, screen recording, typing, VOICE GUARDIAN
- **Audio section:**
  - **Echo Guard** — jab tak Maya bol rahi hai mic block (nahi to Maya apni hi awaaz sun ke khud reply karne lagti hai)
  - **Screen recording mode** — screen recording me Maya ki awaaz capture ho (echo guard ON rakhna zaroori)
- **Startup on boot** toggle
- **Typing tool:** default = fast paste-all; ON karo = **character-by-character typing + speed setting** (cool lagta hai)
- **🎤 VOICE GUARDIAN (v4 upgraded):**
  - Enable → modes: **Everyone** / **Owner Only** (owner-only ke liye voice registration zaroori)
  - **Guard/Away mode:** doosra banda bole → warning: *"Boss ko bulao warna phone lock kar dungi"*
  - **Record Owner Voice — 3 steps:** (1) *"Hey Maya meri awaaz pehchano"* (~2.2s+ required), (2) *"Aaj ka mausam kaisa hai? Mujhe batao"*, (3) numbers ginta hua 1-7 → Save Voice
  - **Test My Voice** → **match % score** (silent room me 99%)
  - **Threshold slider** — 5 pe set kiya (noisy environment me score girta hai, threshold accordingly); full/3 pe = 75%+ match required
  - **+0.5s latency** — ML voice-comparison model (low-end device pe 0.5–1s)
- **🆘 Emergency/SOS section:** contacts pick ya manual (name + number) → **priority 1–5** → SOS emergency contact ya favorite list → *"Maya emergency mode on"* → **current location + message sab 5 numbers pe SMS → call try (2x per number) → priority order me agli number pe**

### 🛡️ 45:19–50:07 — TOUCH GUARD (naya in v4)
- **Arm the Guard:** *"Maya touch guard on"* → **12s** phone rakhne ka time → koi bhi touch kare → **7s SIREN** + door reh ne ka warning + **3 front-camera images** save
- **God Mode** (voice registration required): sirf **owner ki awaaz** se unlock/disable; har touch pe photo + siren + warning (1s gap pe 3 photos)
- Settings: **photos per touch 3–6**, siren duration, **first touch = warning, second = siren**, instant phone lock, **torch blink**, **unknown touch → SOS message with location**
- **Touch sensitivity:** low / medium / high (default medium)
- **Charger pull-out pe bhi trigger** (toggle)
- **Stealth mode:** koi siren/warning nahi — chupke photos + lock → baad me batati hai
- **"Who Touched It" gallery:** kisne kab chua — timestamps + photos history

### 🔒 50:07–~52:30 — Screen lock (voice lock/unlock)
- PIN ya **pattern** set karo (pattern 2x confirm) → enable
- *"Maya mera phone lock kar do" / "unlock kar do"* — dono kaam karte hain
- **Test button** (video me pass hua)
- **Fine-tune:** swipe-up length (screen size ke hisab se short/full), pattern **dots vertical/horizontal position + size** (mismatch fix)

### 💬 ~52:30–55:42 — WhatsApp auto-reply + event triggers
- **WhatsApp auto-reply:** Maya band ho tab bhi (chatbot mode) — notification listener se
- **Include groups** (default OFF — ladaai ho jayegi warna 😄)
- **Instructions** section (polite raho, is way me jawab do)
- **Offline fixed reply** ("busy hun, thodi der me karta hun")
- Reply label ("Maya auto reply"), **exclude numbers/names** (comma separated)
- **Reply history log** (kisko kab kya reply kiya)
- **Event Triggers (announcements):**
  - Maya jagti hui ho → **apni natural voice**; sleep me → **system TTS** (robotic — dono ka farak bataya)
  - Events: charger in/out, battery full/low/critical, headphone in/out, Bluetooth connect/disconnect, **WiFi connect/lost**, airplane mode, silent on/ringer back, **app install/uninstall**
- **Maps API key (optional, paid Places)** — bina key bhi navigation/nearby chalta hai
- **Web search API (optional)** — Tavily etc.; **built-in khud ka headless search engine** hai jo parse kar ke deta hai
- **Image generation:** **built-in FREE (Pollinations-based)** — custom generator bhi add ho (name + base URL + model ID + API key)

### 🏠 55:42–1:00:15 — Maya Home, memories, chat, files, camera, market, whiteboard
- **Maya Home:** physical **Home Box** (ESP32, local control) — lights/fan/motor, kahin se bhi
- **Memories:** list — add/edit/delete per fact
- **Chat section:** text chat = **text reply**; Home ka input = **voice reply** (alag systems!)
- Home input me **image attach** + **koi bhi file upload** (HTML, Python, MD, TXT, PDF, Excel, doc)
- **Scan:** camera (front/rear) se dekh ke explain; **bina camera UI khole video recording** (gallery me save)
- **Market:** stock watchlist (search: "Adani" → options), **chart view**, analysis; sub-agent se **deep research** → summary document
- **Documents:** Maya ki banai PDF/Excel files yahan list hoti hain
- **Whiteboard (Study mode):** full-screen board — Maya likhti hai, **draw karti hai**, examples — voice se tutor
- **Maya Rules:** custom rules banao (agla behaviour usi hisab se)
- **PC Link:** Maya PC se link → phone se PC ko command

### 🎯 1:00:15–1:10:42 — Live demos (forensic highlights)
- Voice lock/unlock ✅ · YouTube stats ✅ · screenshot→WhatsApp (contact-resolution retry) ✅
- **Mute/unmute:** home ke center orb pe tap (mute state indicator abhi nahi hai — "next update me daal dunga")
- Sub-agent research demo — **tool-discovery pattern:** *"mission agent tool list me nahi dikh raha... create document tool bhi nahi... FIND TOOL kar ke RUN TOOL chalati hun"* → "Aaj ki Khabrein" document bana ke khol diya (LLM apne tools khud dhoondhta hai!)
- **Facebook story automation:** *"motivational quote image bana, red-orange-yellow gradient, Facebook story pe daal"* → **offline built-in quote-image generator** (internet pe nahi jata) → seconds me upload ✅
- Outro: monthly updates, WhatsApp community

---

## 2️⃣ COMPLETE FEATURE MATRIX (17 suites, 150+ actions — official site se cross-verified)

| # | Suite | Example commands | Already in hamara repo? |
|---|---|---|---|
| 1 | Calls & SMS | "Call Mom", answer/reject by voice | ⚠️ Call intent hai, answer nahi |
| 2 | WhatsApp | "WhatsApp Riya: good morning", photos, voice/video call | ⚠️ Basic send hai |
| 3 | Music & Video | "Play some Arijit Singh" (Spotify/YT/YTMusic + memory) | ✅ YouTube (no Spotify/memory) |
| 4 | Phone control | "Open Instagram and scroll" — taps/types/swipes | ❌ (accessibility hai par limited) |
| 5 | Screen & camera vision | "Look at my screen", scan, record | ❌ |
| 6 | Alarms & reminders | alarm, timer, calendar, spoken reminders | ✅ |
| 7 | Notifications | "Read my notifications" | ✅ (listener hai) |
| 8 | Files & docs | "Make a resume in Word" (Word/Excel/PPT/PDF) | ❌ |
| 9 | Coding & websites | "Build a website for my cafe" → GitHub → deploy | ❌ |
| 10 | Knowledge & live info | news, weather, sports, stocks, YT stats | ⚠️ Weather/news partial |
| 11 | Location & maps | "Navigate to the airport", nearby ATMs | ❌ |
| 12 | Smart home | "Turn off the bedroom light" (Home Box/ESP32) | ❌ |
| 13 | Memory | "Remember I like dark-roast coffee" | ✅ (basic) |
| 14 | Study mode | Whiteboard tutor | ❌ |
| 15 | Task macros | "Run my morning routine" | ⚠️ Custom skills kaam ke qareeb |
| 16 | Personas & voice | Maya/Friday/Venom, GF mode, sleep/wake | ⚠️ Persona system nahi |
| 17 | System & settings | torch, volume, brightness, silent, lock | ⚠️ Partial |

**Plus non-voice modules:** Skills store (GitHub-loaded), Sub-agents (multi-provider failover), Connectors (GitHub/GDrive/Notion/Telegram/GitLab/Linear), Email (SMTP), WhatsApp reports, Social automation (IG/FB), Voice Guardian, Touch Guard, SOS, Screen lock (PIN/pattern), WhatsApp auto-reply, Event triggers, Market (stocks), Documents, Whiteboard, Rules, PC Link, Backup, Themes.

---

## 3️⃣ DESIGN SYSTEM (video se observed)

| Element | Observation |
|---|---|
| Theme | Dark default — **"Midnight Obsidian"** + light themes bhi |
| Accent | User ne **orange** pick kiya (orb/edge glow) — accent color system |
| Home | **Floating orb** (tap = mute/unmute) + optional **2D live character / AR character** |
| Edge glow | Screen ke 4 taraf **gradient ring** rotating (battery-heavy, toggle-able) |
| Cards | Corner radius setting: **soft ↔ sharp** slider + reset |
| Text | Size + style settings |
| Nav | 3-dot menu → Settings; sections drawer; Chat / Home input separated |
| Voice UX | Wake = "Hey Maya"; sleep = "Bye Maya" (offline recognized); barge-in (beech me tok sakte ho jab echo guard off) |
| Speed targets | Normal reply ~0.5s; conversation mode 6–7s (documented tradeoff) |
