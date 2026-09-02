# 🚀 P10 — "NAYI MAYA, KHUD-BA-KHUD" — auto-update ka STRUCTURE

> 📌 **Status: sirf STRUCTURE hai. Code NAHI likha — user ke "ab banao" ka intezaar.**

## 🎯 ISSUE CARD

**Masla**: Har release par — Actions kholo → artifact dhoondo → APK download → file
manager → install → confirm. Bar bar yeh mehnat. User chahta hai:
*"hum yahan changes karein, Maya wahan khud latest ho jaye — perfectly,
accurately, bagair galati ke."*

**Qabil-e-qabool hone ke (acceptance):**
1. Phone **khud bataye** naya version aaya hai (kaya naya likha ho)
2. **Ek tap** = update → naya version waha chal raha ho
3. 🛡️ Ghalat/tuti file **kabhi** install na ho (checksum darwaza)
4. Sirf JS/bartan ki baat ho to **bina APK ke** update (90% changes JS mein hain)
5. Aankh band karne layak **rollback**: update bigar jaye to purana version wapas — phone kabhi brick nahi
6. Har cheez ka **saboot** (tests + doctor-report line + CI check)

**HAR jagah "kya ghusa / kya nahi":**
- GitHub public repo = kaafi (koi server, koi paisa nahi)
- Signing: `app/maya.keystore` committed hai (har build ka signature EK) ✅ — yeh aaj se hi fixed rehna chahiye

---

## 🅰️ OPTION A — GitHub Releases + 1-tap APK install

**Sanstha (machine):**
1. CI: `git tag vX.Y.Z` push → release workflow (abhi manual `workflow_dispatch`
   hai) **khud** trigger ho, APK banaye aur GitHub Release mein `MAYA-vX.Y.Z.apk` daale.
2. App boot par: `GET api.github.com/repos/adil-chandio/Mana-android/releases/latest`
   (public repo → keyless, ~200 bytes)
3. `tagName` ka version compare apne `appVersion()` se → naya ho to UI mein card:
   **"🚀 NAYI MAYA vX.Y.Z aayi hai — [kya naya: release notes]** | UPDATE KARO | BAAD MEIN"
4. Tap → APK download (notification mein % progress) → poora hone par Android ka
   install sheet (mastmola: signature same ➜ sidha upar likh dega; settings bachi).
5. Zyada kal: DownloadManager → file → `FileProvider` + `ACTION_INSTALL_PACKAGE`
   + `REQUEST_INSTALL_PACKAGES` permission (istemaal aksar sirf ek dafa poochhta hai).

**Taqqat**: poore v5.x native app ke liye kaam karta hua classic tree.
**Kamzori**: har JS-only change bhi poore APK download ko aama deta hai (15-20MB per release).

---

## 🅱️ OPTION B — HOT UPDATE: sirf web bundle (index.html), bina APK ⭐ (shahi raasta)

Sach: hum kaam **90% `index.html` mein** karte hain. To:

1. **CI par hot-release manifest**: har green push se `manifest.json` banega
   `{ versionCode, sha256, url }` aur ek **keyless jagah** par publish (GitHub
   release asset ya raw file `main` par — woh file khud release ke andar nahi,
   gh-pages/raw se bhi ho sakta hai).
2. **App boot**: manifest parho → naya versionCode aur **build hot-eligible"** stamp
   (CI certificates: 975/975 green + sha256) → file download → **sha256 tasdeeq**
   (mismatch = phenk do, as if kuch hua hi nahi) → internal storage.
3. **WebViewAssetLoader**: `/assets/web/` ke path handler mein — *ubdate hui
   copy maujood & valid* → wahan se serve; warna bundled copy (yeh Rs.iva change hai).
4. **Rollback qanooni**: update ki hui copy ke pehle boot mein boot-guard
   (jo pehle se hai — `markAlive`) await karke `hotUpdate.ok` nahi likha → agli baar
   bundled copy wapas + "hot update wapas li gayi" ka counter doctor mein likha.
   **KABHI nayi copy pahli bar brick nahi karegi.**
5. **Kotlin change aaye** (koi bhi `@JavascriptInterface` ya native) → us release ki
   manifest line **hot-eligible: false** ➜ Option A se APK update hi chalega.

**Khatre aur unka razkarz**:
| Khatra | Ilaaj |
|---|---|
| Buri JS phone par crash | sha256 + boot-guard rollback + CI ka poora suite hot se pehle |
| Aadha stepsa download (network toota) | temp file + atomic rename + checksum dono taraf |
| Kotlin/JS version milan (nayi JS purana Kotlin call kare) | manifest mein `minApkVc`; mismatch ho to hot update ROK do seedha |
| Security (koi aur file ghusa de) | sirf hamari repo ka release + sha256 jo CI ne keyless banaya; TLS only |

---

## 🅲️ OPTION C — Proper beta channel (Firebase App Distribution / Play Internal)

Theek magar: naya account/project/setup chahiye — "0-budget+self-hosted" ke khilaaf.
Abhi nahi; baad mein dikhte hain.

---

## 🏁 MERI TAJWEEZ (one-theme-one-release qaid ke andar)

> **Pehle B (hot update) — Value max, mehnat min, risk rollback se capped.
> Phir A (APK update) — jab doori Kotlin ka kaam aaye.**
> Dono ke liye doctor mein `── 🚀 UPDATE ──` report block: pichhli check, abhi ka
> version, kitne updates lagae, rollback count — andhe kabhi nahi rahenge.

### CONTRACT (jo "ab banao" par likhunga — jaaiza itefaq)
- [ ] CI mein `hot-release` manifest build step + sha256 + `minApkVc`
- [ ] Boot: manifest fetch + card "🚀 NAYI MAYA" (with kya-naya text)
- [ ] Download→sha256→atomic swap→rollback (boot-guard)
- [ ] openSetting-style diagnostic block + counters (`hot_ok/hot_rolled`)
- [ ] Tests: Section 27 — manifest parsing, checksum reject, rollback sim,
      milan-guard, UI wiring — (target ~990+ total)
- [ ] Docs: P10-UPDATE + RELEASE note; README line
- [ ] Device daakhila: "naya UI wala push karo → phone par 'update aaya' card
      aana chahiye bina hamne APK diye" ☜ **yahi final SABOOT hoga** 🫵
