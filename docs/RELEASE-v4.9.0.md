# 🧪 v4.9.0 — SAAF ZUBAAN · NAAP-TOL · 👑 MALIK

> **P0 + P1 + MALIK** — AMAL-WORKFLOW.md ke Qanoon ke tehat pehla release.
> Sab kuch **switch ke peeche** hai. Kuch bura lage → LAB se OFF → Maya foran
> purane tareeqe par. **Nayi APK ka intezar nahi.**

---

## Aap ke screenshot se seedha muqabla

| Aap ne dekha tha | Ab |
|---|---|
| Poora `<think>` bubble mein | ❌ **ghayab** |
| Poora `<think>` **bol kar** sunaya gaya | ❌ **ghayab** |
| `<think>` band hi nahi hua (kata hua) | ❌ **ghayab** — aur token 320 → **1400** |
| "(soch rahi hoon)" par atakna | ✅ khatam |
| `brightness_control(level=100)` **likha** gaya | 🧹 chhup gaya *(chalega P2 mein)* |
| Devanagari jab aap Roman Urdu bolte ho | ✅ **theek** |
| *"tumhe kis ne banaya?"* → koi jawab nahi | 👑 **ta'aruf** |

---

## 🧹 P1 — SAAF ZUBAAN

`SAAF()` — 10 shaklein, **bubble aur AWAAZ dono se pehle**:

| # | Shakl | Kis model ki |
|---|---|---|
| 1 | `<think>…</think>` · `<thinking>` · `<reasoning>` · `<analysis>` | qwen3, deepseek-r1 |
| 2 | `<\|channel\|>analysis…<\|message\|>` | gpt-oss (harmony) |
| 3 | **band na hua** `<think>` — aakhir tak | **aap ka screenshot** |
| 4 | akela `</think>` | mukhtalif |
| 5 | `‹AMAL›…‹/AMAL›` | hamara protocol (P2) |
| 6 | `<tool_call>…</tool_call>` | XML tool call |
| 7 | `Here's a thinking process:` · `Let me think` | saada matn |
| 8 | `**Analyze User Input:**` (numbered bold) | **aap ka screenshot** |
| 9 | `Check constraints:` · `Response draft:` | **aap ka screenshot** |
| 10 | akeli line `brightness_control(level=100)` | tool-less dimaag |

**Sab se ahem qanoon:** saaf hone ke baad kuch na bache → jawab **EMPTY** →
**AGLA DIMAAG**. Khali bubble ya adhoora jumla **kabhi nahi**.

### 🪙 Token budget
```
reasoning models (gpt-oss · qwen3 · deepseek-r1 · qwq)  →  1400
baqi sab                                                →   400
purana: sab ke liye 320  ← soch usi mein khatam ho jati thi
```

### 🗣️ Zubaan ka tazad khatam
Pehle **do mutazad hukm** the — `RULE(3)` *"user ki script mirror karo"* aur
`LANGUAGE RULE` *"Reply in Hindi (Devanagari)"*. Model ne baad wala maana.

Ab: **MIRROR pehle** — jo aap bolo wohi script. Setting sirf **fallback** hai
jab script ka pata na chale.

---

## 📊 P0 — NAAP-TOL (Maya ka rawaiya BILKUL nahi badalta)

| | |
|---|---|
| **NAAP** | har turn ka waqt: dimaag · tool · awaaz · kul — **p50 aur p90** |
| **BASELINE** | *"aaj kitna hai"* — taake kal *"teiz hua"* **sabit** ho sake |
| **📋 DIAGNOSTIC COPY** | ek tap: version · WebView · engines · dimaag · settings · aakhri 60 log |
| **🗃️ HARVEST** | aap ki apni 300 baaton se test set — aap ki zubaan mein |
| **🔖 SCHEMA** | localStorage ki version (12 keys thin, aur aa rahi hain) |

**🔒 Privacy:** diagnostic aur harvest dono mein **API keys · phone number ·
email khud-ba-khud chhup** jate hain. Harvest ka data **phone se bahar nahi jata** —
sirf hisab dikhta hai.

---

## 👑 MALIK — Maya ko apne banane wale ka pata

Ab *"tumhe kis ne banaya?"* / *"who made you?"* ka jawab **dimaag se poochhe
baghair** aata hai:

- **Chhota** — aam sawal
- **Poora** — "poora batao"
- **☠️ Flex** — jab **Adil khud** puche
- **English** — English sawal par

**Har jumla SACH hai** — naapa hua ya code mein maujood. `prompt` mein saaf likha
hai: *"SIRF SACH — kuch apni taraf se mat joro."* Aur ek test khokhli tareef
(*best/genius/legend*) par **fail** kar deta hai.

Prompt par bojh: **529 harf** — aur `facts` ki tarah **katta nahi**.

---

## 🧪 LAB — Settings mein naya khana

```
📊 NAAP        [ON ]   ← default ON (bay-zarar)
🧹 SAAF ZUBAAN [OFF]   ← aap chalu karenge
👑 MALIK       [OFF]   ← aap chalu karenge

📊 BASELINE DEKHO      🗃️ MERI BAATON SE TEST SET      📋 DIAGNOSTIC COPY
```

---

## 🔒 Qanoon 2 — purana raasta zinda

Har hook `typeof … !== "undefined"` aur `try/catch` ke peeche hai. **LAB block
poora gayab ho jaye to bhi Maya waise hi chalti rahegi** — 6 test isay pakarte hain.

Aur ek naya **DHANCHE KA TAALA**: 12 namespace maujood aur sahi tarteeb mein hon,
warna test fail — kyunke test harness code **naam se dhoond kar** nikalta hai.

---

## 🧪 Saboot

```
✅ CSS CHECK PASS
✅ SETTINGS     72 / 72
✅ AWAAZ       293 / 293
✅ DIMAAG      155 / 155
✅ MAYA LAB     92 / 92     ← naya
───────────────────────────
                612 / 612    (520 → 612)   3 baar lagataar
```

**Sab se ahem test:** SAAF ke test **asli screenshot ke matn** par chalte hain —
wohi jo aap ne bheja tha. **Wo bug dobara nikal hi nahi sakta.**

---

## ▶️ Aap ke liye 3 qadam

1. Release chalao → tag **`v4.9.0`** (versionCode 57)
2. Settings → 🧪 **MAYA LAB** → pehle **📊 BASELINE DEKHO** *(kuch baatein karne ke baad)*
3. Phir **🧹 SAAF ZUBAAN** aur **👑 MALIK** ON karo → tasdeeq karo

Kuch bhi ajeeb lage → **switch OFF** → Maya foran purane tareeqe par. 😌

**Agla:** P2 — AMAL ENGINE (tools har dimaag ko, aur `brightness_control` sach much chalega).
