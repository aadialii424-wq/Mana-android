# 🧩 FIX — MAYA v4.0.2 Settings UI (old WebView) — Recipe

> **Kya hai:** MAYA ki **Settings** page par form elements (sliders, switches,
> labels) purane Android System WebView mein render nahi ho rahe the — actual
> control ki jagah **patli invisible horizontal lakeerein** dikhti thin. Sath hi
> Settings scroll nahi hoti thi, is liye neeche ke sections (AWAAZ sliders,
> switches, THEME options) tak pahunchna hi mumkin nahi tha.
> **Target:** version `4.0.2` (versionCode `48`).

---

## 1. PROBLEM (root cause)

| Symptom | Root cause |
|---|---|
| Sliders ki jagah patli lakeer, thumb ghayab | `accent-color` (Chrome 93+) + `color-mix()` (Chrome 111+) purane WebView mein **silently drop** — track/thumb ka koi background hi nahi bachta |
| Switches invisible / 0-height | `.switch i` ka background `color-mix()` se aa raha tha + `inset:0` (Chrome 87+) → box ka size zero |
| Labels aur inputs invisible | background/border `color-mix()`/alpha par depend karte the → declaration drop → transparent field |
| Sirf `<select>` aur `<input type="text">` theek | Inki styling simple thi (plain hex + border) |
| Settings ke neeche ke sections tak pahunch nahi | `.tab` `position:absolute` tha + flex chain (`#app > main > .tab`) par `min-height:0` nahi tha → `main` content jitna lamba, aur `body{overflow:hidden}` ne baaki sab clip kar diya |
| Nav ke peeche content chhup jata | `padding-bottom:calc(8px + env(...))` — `env()` (Chrome 69+) na chale to **poori padding** drop |
| Rows/chips chipke hue | flexbox `gap` (Chrome 84+) drop |

> **Asool:** CSS mein ek bhi na-samjh aane wala token poori declaration ko
> invalid kar deta hai — browser use chup-chaap phenk deta hai. Is liye har
> "fancy" value ke sath ek plain fallback declaration chahiye (ya `@supports`).

---

## 2. FIX INVENTORY

### 2.1 `public/index.html` + `app/src/main/assets/web/index.html` (dono IDENTICAL)

1. **Scroll chain** (`main` rule):
   `main{... min-height:0; height:auto}` aur
   `.tab{... flex:1 1 auto; min-height:0; max-height:100%; overflow-y:auto;
   -webkit-overflow-scrolling:touch}`.
   Purane compat block se `.tab{top:0;left:0;right:0;bottom:0}` **hata diya**
   (ab `.tab` absolute nahi, flex child hai).

2. **Naya block `v4.0.2 — SETTINGS FORM CONTROLS · OLD-WEBVIEW HARD FALLBACK`**
   (stylesheet ke bilkul aakhir mein, `</style>` se pehle) — sab kuch
   `var(--token, #hex)` par, koi `color-mix` / `accent-color` / `gap` / `inset` nahi:
   - `.setgrp`, `.setgrp-head`, `.setgrp-body`, `.card` → solid bg + border
   - `.f` → `min-height:44px` + bg + border + `box-sizing:border-box`;
     `label.f` → apna bg/border/`padding` (invisible label khatam)
   - `input[type=range]` → `height:36px`, pill background, `-webkit-appearance:none`
     + `::-webkit-slider-runnable-track` (6px) + `::-webkit-slider-thumb`
     (22px, `margin-top:-8px`) + `-moz-` equivalents
   - `.switch` → 48×26px; `.switch input` poore switch par invisible overlay;
     `.switch i` → `top/left` + explicit `width/height` (koi `inset` nahi);
     `.switch i::before` → 20px knob, `transform` ki jagah `left:2px → left:24px`
   - `.switchrow > span{margin-right:12px}` → flex `gap` ka fallback
   - `#themeGrid{grid-gap:8px;gap:8px}`, `#accentRow .acc{margin:0 10px 10px 0}`
   - `nav{padding:8px 10px 10px}` + `@supports(padding-bottom:env(...))` se safe-area
   - `#tab-set{padding-bottom:130px}` — aakhri button nav ke peeche na chhupe

3. **Boot-guard feature detect** (pehla `<script>`):
   `window.__cssOK(prop,val)` → `CSS.supports` wrapper;
   `__noColorMix`, `__noAccentCol`, `__noFlexGap`;
   `window.__compatClass` = `" nocm noacc nogap oldwv"` (jo lagoo ho) →
   `document.documentElement.className += __compatClass`.

4. **Theme apply** (`applyTheme`): `document.documentElement.className =
   "t-" + theme + (window.__compatClass || "")` — warna theme lagate hi compat
   classes mit jati thin.

5. **Doctor report**: `CSS COMPAT: color-mix ... • accent-color ... • flex-gap ...`
   aur `SETTINGS SCROLL: scrollable ✅ (scrollHeight/clientHeight)` lines.

6. Version strings `v4.0.1` → `v4.0.2` (splash `.ver`, drawer `.dfoot`,
   `#subTitle`, `#hudBuild`, `.vtag`, `#dbgBadge`, doctor report, boot logs).

### 2.2 `public/sw.js` + `app/src/main/assets/web/sw.js`
- `CACHE = 'maya-v4.0.2'` (purani cache invalidate — warna purana CSS chipka rahega)

### 2.3 `app/build.gradle`
- `versionCode 47 → 48`, `versionName "4.0.1" → "4.0.2"`

### 2.4 `app/src/main/java/com/maya/ai/MainActivity.kt`
- `appVersion()` → `"4.0.2-native"`, launch toast → `"MAYA v4.0.2 • Settings UI + WebView compat fix install hua hai"`

### 2.5 `README.md`
- Version line + `## 🆕 v4.0.2` section

---

## 3. VERIFY (push se pehle)
```bash
cmp app/src/main/assets/web/index.html public/index.html   # identical
cmp app/src/main/assets/web/sw.js public/sw.js
# settings block mein koi modern-only property na bache
awk '/SETTINGS FORM CONTROLS/,/<\/style>/' public/index.html | grep -n 'color-mix\|accent-color\|inset:\|[^-]gap:' 
grep -n 'versionCode 48' app/build.gradle
npx serve public -l 3000    # browser mein Settings kholo, neeche tak scroll karo
```

Device par: Settings → har group kholo → sliders drag karo → switches tap karo →
neeche `💾 SAVE KARO` tak scroll → Doctor chalao aur `CSS COMPAT` / `SETTINGS SCROLL`
lines dekho.

## 4. RELEASE
```bash
git add -A && git commit -m "v4.0.2: Settings UI fix (sliders/switches/labels/scroll) for old WebView"
git push origin arena/01a05845-mana-android
gh workflow run "Release MAYA APK" --ref arena/01a05845-mana-android -f tag=v4.0.2
gh run watch --exit-status
gh release view v4.0.2
```
