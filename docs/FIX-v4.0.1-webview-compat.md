# 🧩 FIX — MAYA v4.0.1 WebView Compat (Recipe)

> **Kya hai:** v4.0.0 "OBSIDIAN" APK purane Android System WebView (Chrome < 87)
> par blank/broken layout de sakta hai + har launch par jhoota "WebView load
> NAHI hua" toast aata hai. Ye recipe woh sab theek karti hai.
> **Target:** version `4.0.1` (versionCode `47`), released via `Release MAYA APK`
> workflow tag `v4.0.1`.

---

## 1. PROBLEM (device-side ground truth)

| Symptom | Root cause |
|---|---|
| Splash/na screen nahi dikhti, layout toota | CSS `inset:0` (Chrome 87+) — purane WebView ignore → splash/tabs ka size zero |
| Background white/blank dikhta hai | `color-mix()` (Chrome 111+) body/splash `background:` shorthand mein → poori declaration drop |
| Har launch par "WebView load NAHI hua" toast | JS `markAlive()` bridge par exists **nahi** tha aur `onPageFinished` `webViewAlive` true nahi karta tha |
| WebViewAssetLoader is device par fail | Koi fallback nahi tha → blank screen |
| Service worker offline fail | `sw.js` mein `?.` optional chaining (Chrome 80+) — purane SW-capable WebView par parse fail |
| Purana WebView detect nahi hota | Na native na JS mein version check tha |

> **Note:** index.html ka main JS pehle se hi ES5-safe hai (var/function, koi
> `=>`/backtick/`?.` nahi) — JS syntax ko rewrite **nahi** karna. Asli masla
> CSS + sw.js + native bridge ka tha.

---

## 2. FIX INVENTORY

### 2.1 `app/src/main/java/com/maya/ai/MainActivity.kt`
1. **Settings** (`webView.settings.apply` mein add karo):
   `allowFileAccess`, `allowContentAccess`, `javaScriptCanOpenWindowsAutomatically`,
   `loadWithOverviewMode`, `useWideViewPort`, `setSupportZoom(false)`,
   `cacheMode = LOAD_DEFAULT`, `mixedContentMode = NEVER_ALLOW`, `textZoom = 100`.
2. **`markAlive()`** bridge mein add karo → `webViewAlive = true`
   (JS pehle se `window.MayaBridge.markAlive()` call karta hai).
3. **`onPageFinished`** override → agar URL `https://appassets...` ya
   `file:///android_asset` hai to `webViewAlive = true`.
4. **`onReceivedError`** override (main-frame, host = VIRTUAL_HOST) →
   `view.loadUrl("file:///android_asset/web/index.html")` (asset-loader fail-safe).
5. **`shouldOverrideUrlLoading`** → `url.scheme == "file"` bhi `return false`
   (fallback page WebView ke andar khule, external app mein na bheje).
6. **Old-WebView toast** (12s baad): `WebViewCompat.getCurrentWebViewPackage(this)`
   ka `versionName` major parse; 1..86 ho to "Play Store se Android System WebView
   update karo" long toast.
7. **`appVersion()`** → `"4.0.1-native"`; naya bridge `webViewVersion()` jo
   installed WebView package version deta hai (doctor report ke liye).
8. Launch toast → `"MAYA v4.0.1 • WebView compat fix install hua hai"`.

### 2.2 `app/src/main/assets/web/index.html` (aur `public/index.html` — dono IDENTICAL honi chahiye)
1. `body{...}` → `background-color:var(--bg);` **pehle** `background:` shorthand
   (color-mix fail par bhi dark bg).
2. `#splash{...}` → same `background-color:var(--bg);` fallback.
3. **OLD-WEBVIEW COMPAT block** main `</style>` se pehle:
   `#edgeGlow`, `#edgeGlow::before/after`, `#splash`, `.sring div/.r2/.r3`,
   `.tab`, `#orb::before/after`, `.switch i`, `.char-glow`, `#drawer`
   (`top:0;bottom:0;left:0;right:auto;width:78vw;max-width:300px`),
   `#drawerScrim` — sab par explicit `top/right/bottom/left` (modern par same
   value, harmless).
4. Boot-guard script (pehla `<script>`): WebView major detect —
   `window.__webviewMajor` (UA `Chrome/(\d+)`), `window.__oldWebview = major>0 && major<87`.
5. `finishBoot()`: old WebView par `pushLog` + 2.5s baad non-blocking toast:
   "⚠ Purana WebView (Chrome X) — Play Store se Android System WebView update karo".
6. Doctor report: `WEBVIEW: Chrome X (PURANA/OK)` line + `NATIVE WEBVIEW PKG`
   line (`window.MayaBridge.webViewVersion()`).
7. Version strings `v4.0.0` → `v4.0.1` (splash `.ver`, `#drawer .dfoot`,
   `#subTitle`, `#hudBuild`, `.vtag`, `#dbgBadge`, doctor report,
   `VERSION 4.0.1 — WEBVIEW COMPAT EDITION`, boot logs).

### 2.3 `app/src/main/assets/web/sw.js` + `public/sw.js`
- **Full ES5 rewrite:** `const`→`var`, arrows→`function`, `?.` wali line →
  `(req.headers.get('accept') || '').indexOf('text/html') !== -1`.
- `CACHE` → `'maya-v4.0.1'` (donon files; public wala v4.0.0 se bump hua).
- Header comment → `v4.0.1`.

### 2.4 `app/build.gradle`
- `versionCode 46 → 47`, `versionName "4.0.0" → "4.0.1"`.

### 2.5 `README.md`
- Version line + `## 🆕 v4.0.1` section (WebView compat notes).

---

## 3. VERIFY (push se pehle)
```bash
# dono copies identical honi chahiye
cmp app/src/main/assets/web/index.html public/index.html
cmp app/src/main/assets/web/sw.js public/sw.js
# koi optional chaining na bachein
grep -n '?\\.' app/src/main/assets/web/index.html public/sw.js
# sw.js ES5
grep -c '=>\|const\|let ' app/src/main/assets/web/sw.js  # 0 expected
# versions
grep -n '4.0.1' app/build.gradle README.md
```

## 4. RELEASE
```bash
git add -A && git commit -m "v4.0.1: WebView compat fix ..."
git push origin arena/01a0565b-mana-android
gh workflow run "Release MAYA APK" --ref arena/01a0565b-mana-android -f tag=v4.0.1
gh run watch --exit-status   # build ~1-5 min
gh release view v4.0.1       # phir download link check
```
Workflow APK: `MAYA-v4.0.1.apk` → release tag `v4.0.1`.
