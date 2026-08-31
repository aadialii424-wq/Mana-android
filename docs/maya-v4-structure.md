# 🏗️ MAYA v4.0 — COMPLETE STRUCTURE / BLUEPRINT
> Video (GMqckXxK46c) ke forensic analysis par based — hamare `Mana-android` repo me implement hone wala design.
> Companion doc: `docs/forensic-video-analysis.md`

---

## 1. APP ARCHITECTURE (hamara version)

```
┌────────────────────────────────────────────────────────┐
│                    MAYA APP (APK)                      │
│                                                        │
│  ┌──────────────────────┐   ┌───────────────────────┐  │
│  │   UI LAYER (WebView) │   │  NATIVE LAYER (Kotlin)│  │
│  │  single index.html   │◄──┤  MayaBridge (JS API)  │  │
│  │  • Home (orb+chat)   │   │  • STT (SpeechRecogn.) │  │
│  │  • Chat / Memories   │   │  • TTS + persona voices│  │
│  │  • Market / Docs     │   │  • Wake word service   │  │
│  │  • Whiteboard        │   │  • Accessibility svcs  │  │
│  │  • Settings (full    │   │  • Notif listener      │  │
│  │    tree — neeche)    │   │  • Echo guard / audio  │  │
│  │  • Theme engine      │   │  • Screen lock engine  │  │
│  └──────────────────────┘   │  • Camera capture      │  │
│                              │  • Voice Guardian (ML) │  │
│                              │  • Touch Guard service │  │
│                              │  • Siren / torch / SOS │  │
│                              └───────────────────────┘  │
│         ▲                            ▲                  │
│         │ localStorage (settings, memories, chat, rules)│
│         ▼                            ▼                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │              BRAIN LAYER (JS)                     │   │
│  │  • Gemini function-calling loop (already hai)     │   │
│  │  • Multi-provider failover (OpenRouter/NVIDIA/    │   │
│  │    Groq/OpenAI/Claude/Grok/DeepSeek/Mistral)      │   │
│  │  • TOOL REGISTRY — 17 suites / 150+ actions       │   │
│  │  • Sub-agents (background missions + status)      │   │
│  │  • Memory engine (facts + auto-update + summary)  │   │
│  │  • Personas (Maya/Friday/Venom × voices × prompts)│   │
│  │  • Rules engine (user-defined behaviour)          │   │
│  │  • Skills loader (GitHub-hosted skill packs)      │   │
│  └──────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

**Faisla:** WebView-hybrid hi rakhenge (video wali app bhi effectively yehi pattern hai —
updates ke liye, aur hamara bridge already strong hai). Native layer expand hogi.

---

## 2. SCREENS / NAVIGATION (video jaisa)

```
HOME (main screen)
├── Center floating ORB (tap = mute/unmute, glow = listening state)
├── Voice input bar (voice replies) + 📷 image attach + 📎 file upload
├── Live transcript overlay (Maya + user bubbles)
├── Edge glow (optional, theme accent color)
├── 2D live character (optional, replaces/augments orb)
└── Drawer (☰) →
    ├── 💬 Chat        (text chat — TEXT replies, history)
    ├── 🧠 Memories    (facts list: add/edit/delete + backup)
    ├── 🧩 Skills      (store: install/uninstall/refresh)
    ├── 🤖 Sub-agents  (missions, status, models/providers)
    ├── 📈 Market      (stock watchlist, charts, research)
    ├── 📄 Documents   (Maya-generated files)
    ├── 🎓 Whiteboard  (study mode: draw + write + tutor)
    ├── 📋 Maya Rules  (custom rules CRUD)
    ├── 🏠 Maya Home   (smart home dashboard — Phase 3)
    ├── 🔗 PC Link     (Phase 3)
    └── ⚙️ Settings    (poora tree neeche)
```

---

## 3. SETTINGS TREE (video se 1:1 mapped)

```
SETTINGS
├── 👤 Personal Settings
│   ├── Gender (Male/Female) — GF mode Male par unlock
│   ├── Phone number
│   ├── Music app (Spotify / YouTube / YT Music)
│   ├── Favorite song (ya baat-cheet se memory)
│   └── YouTube channel ID (+ pre-filled Data API key)
├── 🗣️ Maya Settings
│   ├── Assistant name (UI branding "Maya" fixed)
│   ├── Girlfriend mode toggle
│   ├── "Maya remembers" toggle (auto fact extraction)
│   ├── Persona: Maya / Friday / Venom (default)
│   ├── Voice picker (per-persona voices + ▶ preview)
│   ├── Conversation mode (beta — emotional voice, +6s)
│   ├── WhatsApp message alerts
│   ├── Language (16: Hindi-English mix, Hindi, Bhojpuri,
│   │   Haryanvi, Rajasthani, English, Punjabi, Bangla,
│   │   Assamese, Nepali, Tamil, Telugu, Marathi, Gujarati,
│   │   Kannada, Malayalam)
│   ├── Auto start on boot
│   ├── Proactive mode (memory-based self-talk)
│   ├── Call announcement + keep ringtone + ringtone volume
│   └── Driving mode (custom SMS template + {variables})
├── 🧩 Skills
├── 🤖 Sub-agents & Models
│   ├── Coding model selector (Gemini free models)
│   ├── Providers: name + base URL + model ID + API key
│   ├── Pick Preset (Claude/Grok/OpenRouter/OpenAI/DeepSeek/Mistral/Groq)
│   └── Auto failover (rate limit → next provider → RESUME)
├── 📧 Email (ID/pass, signature, SMTP host+port)
├── 💬 WhatsApp Groups & Reports
│   ├── Sensitive group blocklist
│   └── Report formats (name + group + {date}{shift}{prod}{remarks})
├── 📱 Social Media
│   ├── Handle, platforms (IG/FB), caption tone/prompt
│   ├── Daily story (time → unlock→post→lock loop)
│   └── Scheduled posts list
├── 🔌 Connectors (GitHub, GDrive, Notion, Telegram, GitLab, Linear)
├── 💾 Backup (memories export/import)
├── 🎨 Customize
│   ├── Themes (Midnight Obsidian default + light + more)
│   ├── Text size / style
│   └── Corner radius (soft↔sharp) + reset
├── ✨ Appearance (orb styles, colors, size, 2D/AR character)
├── 🎭 Behavior (floating orb, edge glow)
├── 🔊 Audio (Echo guard, Screen-recording mode)
├── ⌨️ Typing tool (paste-all vs typing + speed)
├── 🎤 Voice Guardian
│   ├── Enable + Everyone / Owner-Only
│   ├── Guard mode (warnings for unknown voice)
│   ├── Record owner voice (3-step wizard)
│   ├── Test my voice (match %) + threshold slider
│   └── Latency note (+0.5s)
├── 🆘 Emergency & SOS (5 priority contacts, location SMS + call ladder)
├── 🛡️ Touch Guard
│   ├── Arm the guard / God mode
│   ├── Photos 3–6, siren duration, warning→siren sequence
│   ├── Instant lock, torch blink, SOS on unknown touch
│   ├── Sensitivity (low/med/high), charger-pull trigger
│   ├── Stealth mode + "Who Touched It" gallery
├── 🔒 Screen Lock (PIN/pattern set, test, fine-tune swipe/dots)
├── 💬 WhatsApp Auto-Reply
│   ├── Works-when-off (chatbot), include groups (OFF default)
│   ├── Instructions, offline fixed reply, label, excludes
│   └── Reply history
├── ⚡ Event Triggers (charger/battery/headphone/BT/WiFi/airplane/
│   silent/app install — announce via Maya voice ya TTS)
├── 🗺️ Maps API key (optional)
├── 🔍 Web Search API (optional — built-in headless search default)
├── 🖼️ Image Generator (built-in free Pollinations + custom add)
└── 🔐 Advanced → Permissions (assistant, camera, calls, location,
    contacts, files, BT, notifications, accessibility, battery,
    screen-share per-session)
```

---

## 4. DESIGN SYSTEM (CSS tokens)

```css
:root {
  /* Midnight Obsidian (default dark) */
  --bg-0: #050508;      /* app background */
  --bg-1: #0E0E16;      /* cards */
  --bg-2: #171722;      /* elevated */
  --accent: #FF7A1A;    /* orange — video wala pick */
  --accent-2: #FFB347;
  --text-0: #F5F5FA;
  --text-1: #A8A8B8;
  --ok: #3DD68C; --warn: #FFC24B; --danger: #FF5C5C;
  --radius: 18px;        /* slider: 4px↔24px (soft↔sharp setting) */
  --font-scale: 1.0;     /* text size setting */
  --glow: 0 0 24px color-mix(in srgb, var(--accent) 45%, transparent);
}
/* Light theme + 6 more themes theme-engine se swap */
```

**Components:** Orb (idle/listening/thinking/speaking states + pulse animation),
chat bubbles (user right / Maya left + persona badge), settings cards,
toggle rows, slider rows, voice-preview rows (▶), persona chips,
drawer nav, edge-glow overlay (CSS conic-gradient rotating),
"Who touched it" photo grid, whiteboard canvas.

---

## 5. NATIVE BRIDGE — NAYE METHODS (Kotlin)

| # | Bridge method | Kaam | Existing? |
|---|---|---|---|
| 1 | `stt / tts` | voice in/out | ✅ |
| 2 | `setAlarm / setTimer` | system clock | ✅ |
| 3 | `vibrate / battery / notify` | system | ✅ |
| 4 | `takeScreenshot` | screenshot (MediaProjection) | ❌ |
| 5 | `screenLock / screenUnlock(pinOrPattern)` | DeviceAdmin + accessibility swipe/dots | ❌ |
| 6 | `captureFrontCam(count)` | Touch Guard photos | ❌ |
| 7 | `playSiren(seconds)` | Touch Guard alarm | ❌ |
| 8 | `torchBlink` | warning | ❌ |
| 9 | `sosSend(numbers[], msg)` | SMS + call ladder + location | ❌ |
| 10 | `echoGuard(on)` | mic mute jab TTS bol raha | ❌ |
| 11 | `screenRecord(on)` | recording mode audio route | ❌ |
| 12 | `typeText(text, speed)` | char-by-char typing via accessibility | ⚠️ AutoSendService expand |
| 13 | `tap / swipe / scrollTo` | phone control | ⚠️ expand |
| 14 | `readScreen()` | accessibility tree dump → LLM | ❌ |
| 15 | `voiceEnroll / voiceVerify` | Voice Guardian ML | ❌ |
| 16 | `scanCamera(front/rear)` | vision | ❌ |
| 17 | `recordVideo()` | background camera | ❌ |
| 18 | `setMediaVolume / brightness / silent / hotspot` | system | ⚠️ |
| 19 | `speechRate / personaVoice` | persona TTS config | ⚠️ |
| 20 | `checkUpdate / installUpdate` | in-app updater | ❌ |

**Naye Kotlin components:** `TouchGuardService`, `SirenPlayer`,
`ScreenLockAdmin (DeviceAdmin)`, `VoiceGuardianEngine` (embedding compare —
TFLite model ya simple MFCC+cosine), `SosManager`, `EventTriggerReceiver`
(charger/BT/WiFi/battery/headphone/package receivers), `Updater`.

---

## 6. TOOL REGISTRY (LLM function-calling — JS side)

Existing pattern (TOOL_DECLS + execTool) extend kar ke suites:

```
calls: [call, answerCall, rejectCall, sms, readCallLog]
whatsapp: [waSend(contact, msg), waSendPhoto, waCall, waVideoCall, waReadChats]
music: [playSong(query, app), musicMemory(save/get), controlPlayback]
phone: [openApp, tap(x,y), swipe, type, scroll, home, back, recents]
vision: [lookAtScreen, scanCamera, recordVideo]
time: [setAlarm, setTimer, reminder, calendarEvent]
notifications: [readNotifications, dismissNotification]
files: [createDoc(type: pdf/docx/xlsx/ppt, name, content), shareFile, findFile]
coding: [writeCode, runSubAgent(coding), githubDeploy]
knowledge: [webSearch, news, weather, stocks, ytStats]
maps: [navigate(to), nearby(type)]
smarthome: [homeControl(device, action)]
memory: [remember, recall, updateFact, forget]
study: [whiteboard(explain, draw)]
macros: [saveMacro(steps), runMacro(name)]
persona: [switchPersona, sleep, wake]
system: [torch, volume, brightness, silent, lock, unlock, battery]
social: [makeQuoteImage(style), postStory(platform), postCaption]
agents: [launchMission(goal), missionStatus, findTool, runTool]
email: [readMail, replyMail, sendMail, deleteSpam]
```

---

## 7. BUILD PHASES

### 🅰️ Phase 1 — FOUNDATION (UI redesign + core UX) ← *pehle yeh*
1. **Naya design system** (Midnight Obsidian + orange accent + radius/font sliders + themes + theme engine)
2. **Home screen redesign:** orb + states + live transcript + voice input bar
3. **Drawer navigation** (Chat/Memories/Skills/Sub-agents/Settings)
4. **Settings tree restructure** (video jaisi categorization)
5. **Persona system:** Maya/Friday/Venom (prompts + voice variants + preview + switch by voice)
6. **GF mode + assistant name + proactive mode**
7. **Memory upgrade:** auto-extract + update + Memories screen CRUD
8. **Language system** (16 languages ka prompt/STT/TTS map)
9. **Chat vs Voice input separation** (text↔text, voice↔voice)
10. **Conversation mode toggle** (placeholder — TTS style switch)

### 🅱️ Phase 2 — SECURITY SUITE (video ka hero feature)
Voice Guardian (3-step enroll + threshold + test) · Touch Guard (arm/god/siren/
photos/stealth/who-touched) · SOS ladder · Screen lock/unlock (PIN+pattern
+fine-tune) · Event triggers · Echo guard

### 🅲 Phase 3 — AUTOMATION & POWER
Sub-agents + multi-provider failover · Skills loader (GitHub) · WhatsApp
auto-reply + reports · Social automation + quote-image generator · Connectors
· Documents generator · Market (stocks) · Whiteboard · Typing tool ·
Email/SMTP · Backup · In-app updater · Maps

*(Phase 3 items selectively — har cheez ek dagger nahi, priority list
milegi to us hisab se)*

---

## 8. HAMARE REPO ME KYA BADLEGA

| File | Change |
|---|---|
| `public/index.html` (+ assets copy) | **Complete redesign** — naya UI, drawer, settings tree, personas, themes |
| `app/src/main/java/com/maya/ai/MainActivity.kt` | Bridge expand (naye 15+ methods), echo guard, updater |
| **NEW** `TouchGuardService.kt` | touch detection + siren + camera + lock |
| **NEW** `SosManager.kt` | SOS SMS/call ladder |
| **NEW** `ScreenLockAdmin.kt` | DeviceAdmin lock/unlock |
| **NEW** `EventTriggerReceiver.kt` | system events → announcements |
| **NEW** `VoiceGuardianEngine.kt` | voice enrollment/matching |
| `AndroidManifest.xml` | naye permissions/receivers/services |
| `README.md` | naya feature list + version 4.0.0 |
