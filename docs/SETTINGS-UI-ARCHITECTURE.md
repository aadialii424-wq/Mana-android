# 🏗️ MAYA — SETTINGS UI ARCHITECTURE (v4.1.0 "IRONCLAD")

> Ye document **pehle** likha gaya hai, code baad mein. Maqsad: patch-work band.
> Ab se Settings UI ka har control ek **tay-shuda contract** par banega jo
> Android 5 ke purane System WebView (Chrome 40) se le kar aaj ke Chrome tak
> **bilkul same** render hota hai.

---

## 0. Ab tak kya galat tha (3 dafa fix ke baad bhi)

| # | Galti | Nateeja |
|---|---|---|
| 1 | **Native `<input type=range>` par bharosa** — uska thumb/track sirf `::-webkit-slider-*` pseudo-elements se banta hai. Purane WebView build alag-alag tarah se render karte hain; ek bhi property drop ho to **0px ki lakeer** bach jati hai. | Slider ghayab |
| 2 | **CSS par bharosa ke layout ho hi jayega** — flexbox chain (`#app > main > .tab`) mein agar kisi ek link par sizing quirk ho to `overflow:hidden` sab kaat deta hai. | Scroll nahi |
| 3 | **Modern CSS values** (`color-mix`, `accent-color`, `inset`, `gap`, `env`) — parser poori declaration chup-chaap phenk deta hai. | Invisible box |
| 4 | **Fix ke baad koi sabot nahi** — sirf aankh se dekhna. Har dafa "ho gaya" keh kar wapas wahi masla. | Bharosa khatam |

**Asool jo ab lagoo hoga:** *Jo cheez browser ke rahm-o-karam par hai, us par
UI mat banao. Jo cheez har jagah chalti hai (div + px + hex + JS measurement),
usi par banao — aur phir machine se test karo.*

---

## 1. TECH CONTRACT — kya ALLOWED hai, kya BANNED

### ✅ ALLOWED (settings layer mein)
- `display: block / inline-block / none / table / table-cell`
- `position: relative / absolute` **hamesha explicit `top/right/bottom/left`** ke sath
- Explicit `width` / `height` / `min-height` px mein
- `float` + `clear`
- Colors: `#hex` ya `var(--token, #hex)` (fallback lazmi)
- `border`, `border-radius`, `margin`, `padding`, `overflow`, `text-align`,
  `vertical-align`, `line-height`, `box-sizing` (+ `-webkit-` prefix)
- `-webkit-transform` / `transform` (sirf cosmetic — layout ke liye nahi)
- `transition` (sirf cosmetic)
- `@media` queries
- `:checked`, `+`, `~`, `::before` (CSS2/3 — Chrome 1 se)

### ❌ BANNED (settings layer mein — bilkul nahi)
| Feature | Kyun |
|---|---|
| `color-mix()` | Chrome 111+ |
| `accent-color` | Chrome 93+ |
| `inset:` shorthand | Chrome 87+ |
| `gap` / `row-gap` / `column-gap` | flex mein Chrome 84+ |
| `env()` / `min()` / `max()` / `clamp()` | Chrome 69/79+ |
| `display:grid` | purane WebView mein adhoora |
| `:is()` / `:where()` / `aspect-ratio` / `backdrop-filter` | naye |
| **Flexbox** *layout ke liye* | 2011 vs 2012 spec ki quirks — sirf cosmetic jagah par |
| `::-webkit-slider-thumb` par **inhisaar** | render engine ke reham par |

> Ye list machine se enforce hoti hai: `tools/check-oldwebview-css.js`
> settings layer mein koi banned token mile to **CI fail**.

---

## 2. LAYOUT SKELETON — "Measured Shell"

CSS se ummeed nahi rakhenge ke wo height nikal lega. **JS naapega.**

```
┌─ #app  (position:absolute; top/left/right/bottom:0)
│  ├─ header   (flex:none — height JS naapta hai)
│  ├─ main     (height = innerHeight − headerH − navH   ← JS set karta hai, px mein)
│  │   └─ .tab.active  (height:100%; overflow-y:auto; -webkit-overflow-scrolling:touch)
│  └─ nav      (position:fixed; bottom:0 — height JS naapta hai)
```

**`MayaUI.layout()`** in mauqon par chalta hai:
`DOMContentLoaded`, `load`, `resize`, `orientationchange`, splash hatne ke baad,
har `showTab()` par, aur boot ke baad 300ms/1200ms par (font/emoji late load).

Is se scroll **guaranteed** hai — kyunki container ki height ek asli number hai,
kisi flex algorithm ka nateeja nahi.

**Kya NAHI karenge:** `.tab` ko `position:absolute` nahi banayenge (pichhli dafa
`inset:0` drop hone se size zero ho gaya tha). Height JS deta hai — offsets ki
zaroorat hi nahi.

---

## 3. COMPONENT CONTRACTS

Har control ka **state** native form element mein rehta hai (taake poora purana
JS — `$("#sPitch").value`, `$("#sGf").checked`, `input`/`change` events — bilkul
waise hi chalta rahe), aur **dikhawa** hamesha plain `<div>`/`<span>` se banta hai.

### 3.1 `ui-group` — collapsible card
```html
<div class="ui-group is-open">
  <div class="ui-head"><span class="ui-ic">👤</span><span class="ui-ttl">PERSONAL</span><span class="ui-chev">▼</span></div>
  <div class="ui-body"> ... fields ... </div>
</div>
```
- `.ui-head` = `display:table; width:100%` → cells (icon 26px, title auto, chev 20px).
- `.ui-body` = `display:none`; `.is-open > .ui-body{display:block}`.
- Height kabhi fix nahi — content jitni.

### 3.2 `ui-label` + `ui-input`
```html
<div class="ui-field">
  <div class="ui-label">AAP KA NAAM</div>
  <input class="ui-input" id="sName" type="text">
</div>
```
- `.ui-label` — apna background + border + `min-height:34px` (invisible label mumkin hi nahi).
- `.ui-input` — `min-height:46px`, solid bg, 1px border, `box-sizing:border-box`.
- `<select class="ui-input ui-select">` — native select (ye har WebView par theek
  chalta hai) + simple arrow background image.

### 3.3 `ui-slider` — **native range ki jagah custom** (asli fix)
```html
<div class="ui-field">
  <div class="ui-label">PITCH <b id="pitchVal">1.05</b></div>
  <input type="range" id="sPitch" min="0.7" max="1.4" step="0.05" value="1.05" class="ui-native">
  <!-- niche wala DOM MayaUI khud banata hai -->
  <div class="ui-slider">
    <button class="ui-step" data-d="-1">−</button>
    <div class="ui-track"><div class="ui-fill"></div><div class="ui-thumb"></div></div>
    <button class="ui-step" data-d="1">+</button>
  </div>
</div>
```
- Native `<input type=range>` DOM mein **rehta hai** (state + purana JS), magar
  `.ui-native` se off-screen (`position:absolute;left:-9999px`) — koi
  `::-webkit-slider-*` par inhisaar nahi.
- Visual: 3 div — track (`height:8px`), fill (`width:X%`), thumb
  (`left:X%; margin-left:-13px; width:26px;height:26px`). Sab px + hex.
- `−` / `+` buttons: 40×40 — purane touchscreen par bhi qabil-e-tap, aur agar
  drag kisi wajah se na chale to bhi value badal sakti hai (**do rastay**).
- Drag: `touchstart/move/end` + `mousedown/move/up`, `getBoundingClientRect()`.
- Har tabdeeli par: `input.value = v` → `input` + `change` events dispatch →
  purane listeners (`#pitchVal` update, `applyTheme()`) bilkul waise hi chalte hain.

### 3.4 `ui-switch` — native checkbox + div dikhawa
```html
<div class="ui-row">
  <div class="ui-row-txt">💖 Girlfriend Mode <small>(Male profile par)</small></div>
  <div class="ui-row-ctl">
    <label class="ui-sw"><input type="checkbox" id="sGf"><span class="ui-sw-t"></span><span class="ui-sw-k"></span></label>
  </div>
</div>
```
- `.ui-row` = `display:table; width:100%` → do `table-cell` (text auto, control 64px).
  **Flex gap ka masla khatam** kyunki flex hai hi nahi.
- `.ui-sw` = 54×30 `position:relative` label; `.ui-sw-t` track (absolute 0,0 54×30);
  `.ui-sw-k` knob (absolute `top:3px; left:3px; 24×24`), ON par `left:27px`.
- Checkbox `opacity:0` magar **poore switch par** (54×30) — tap area guaranteed.
- Rang: CSS `:checked ~` se **aur** JS `.is-on` class se (do rastay).

### 3.5 `ui-btn` — buttons
`display:block; width:100%; min-height:48px;` — solid bg + border, gradient sirf
`background-image` ke tor par upar se (fail ho to solid rang bacha rehta hai).

---

## 4. SELF-TEST — "keh nahi, dikha"

### 4.1 Device par: Settings → 🧪 **UI CHECK**
Button har control par `getBoundingClientRect()` + `getComputedStyle()` chala kar
report deta hai:
```
UI CHECK — 47 controls
✅ visible: 47   ❌ invisible: 0
slider sPitch: 44px h • thumb 26px • fill 43%
switch sGf: 54x30 • knob visible
scroll: tab-set 2840px / 520px viewport ✅
```
Koi control 0-height ya transparent mile to **naam ke sath** ❌ list mein aata hai.

### 4.2 Repo mein (CI / dev machine)
| Test | File | Kya karta hai |
|---|---|---|
| CSS lint | `tools/check-oldwebview-css.js` | Settings layer mein banned CSS mile to fail; purane WebView ka simulation kar ke har control ka bg/border/size check |
| DOM test | `tools/test-settings-ui.js` | **jsdom** mein poori index.html chala kar: slider enhance hua?, `+`/`−` se value badli?, switch toggle par `change` fira?, `saveSettings` ne padha?, layout() ne height set ki? |

Dono `npm test` se chalte hain. **Push se pehle dono green hone chahiye.**

---

## 5. KYA NAHI KARENGE (ye dobara nahi hoga)

1. ❌ Naya control add karte waqt `color-mix`/`gap`/`accent-color` — linter rok dega.
2. ❌ Sirf `public/index.html` badalna — dono copies (`app/src/main/assets/web/`)
   hamesha byte-for-byte same; test isko bhi check karta hai.
3. ❌ "Dekh lena theek ho gaya" — pehle `npm test` green, phir push.
4. ❌ Purane JS ke IDs ya events badalna — UI kit sirf **upar se** lagta hai
   (progressive enhancement), core logic ko haath nahi lagata.
5. ❌ Height ka andaza CSS par chhorna — `MayaUI.layout()` naapta hai.

---

## 6. ROLL-OUT

| Step | Kya |
|---|---|
| 1 | `MAYA UI KIT v5` CSS layer (legacy-safe subset) |
| 2 | Settings tab markup → `ui-group / ui-field / ui-row` |
| 3 | `MayaUI` JS module: `layout()`, `sliders()`, `switches()`, `groups()`, `selfTest()` |
| 4 | 🧪 UI CHECK button + Doctor report lines |
| 5 | `tools/` ke dono test green |
| 6 | version `4.1.0` (versionCode 49), sw cache `maya-v4.1.0`, release `v4.1.0` |
