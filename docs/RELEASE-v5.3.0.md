# 🛡️ v5.3.0 — P3: IJAZAT · LEDGER · UNDO · TRACE

> **AMAL-WORKFLOW.md ka usool:** *"P3 (LAGAAM) P4/P6 (zyada taqat) se PEHLE."*
>
> Ab tak Maya har tool **chup-chaap** chala deti thi — brightness bhi, aur kisi ko
> **CALL** bhi. Brightness ghalat ho to koi baat nahi. Magar Maya "Ali" samajh kar
> raat 3 baje **"Ammi" ko call** laga de — **wo wapas nahi ho sakta.**

---

## 🚦 TEEN DARJE — har tool ka apna

| | Matlab | Kya hota hai | Tools |
|---|---|---|---|
| 🟢 **SABZ** | sirf phone ki setting | **foran, bina poochhe** | torch · brightness · volume · timer · mausam · battery · namaz · hisab · search *(15)* |
| 🟡 **ZARD** | phone ki halat / aap ka data | **kar do, magar bata kar** | alarm · reminder · app kholna · YouTube · diary · memory · files *(12)* |
| 🔴 **SURKH** | **phone se BAHAR** — wapas nahi hota | **pehle SAAF IJAZAT** | call · SMS · WhatsApp · reply · file share *(6)* |

**🔑 Test ka taala:** har tool ka darja tay hona **lazmi** hai. Naya tool banao aur
darja bhoolo → **test fail**. *(33/33 abhi tay hain.)*

### Ijazat ka card
Surkh kaam par chat mein card aata hai:
```
🔴 IJAZAT
📞 Ammi ko CALL lagani hai
[ ✅ HAAN, KARO ]   [ ❌ NAHI ]
15 second mein jawab na mila to khud-ba-khud NAHI.
```
**Jawab na aaye → NAHI.** *(Workflow ka usool: timeout par mana.)*
Aur ijazat na milne par Maya ko saaf hukm hai: *"ye bilkul theek hai — **zid mat karo**."*

### ⚡ TRUST MODE
Aap chaho to **surkh bhi zard** ban jaye — sab kuch bina poochhe. **Faisla hamesha aap ka.**

---

## 🛡️ RAILS — do pehredaar

**1. 🔐 OTP / password kabhi bahar nahi jate**
```
"Ali ko bhejo: mera OTP 483920 hai"
→ ❌ "Is message mein OTP/password jaisi cheez lag rahi hai —
      main aise paighaam KABHI nahi bhejti. Aap khud bhejein."
```
Ye **sab se bara khatra** tha. Ab band.

**2. ⏱️ Surkh tools par rate limit** — 45 second mein doosri call/SMS **ruk** jati hai.
*(Loop mein 50 call lag jayen — ye asli khatra hai.)*
Aam message aur sabz tools par **koi rok nahi**.

---

## 📜 LEDGER — "aaj kya kya kiya?"

Har amal darj: **kya · kab · kaunse args · kaamyab ya nahi · kaunsa darja**

```
📜 AAJ ke kaam — 7 amal

  05:12  ✅ 🟢 SABZ brightness_control  (done)
  05:14  ✅ 🟡 ZARD open_app            (started)
  05:20  ❌ 🔴 SURKH call_contact
```
Bolo *"aaj kya kya kiya"* — ya Settings mein **📜 button**.

## ⟲ UNDO — "wo wapas karo"

Amal se **pehle** ki halat mehfooz hoti hai. Bolo **"wapas karo"** / **"undo"** /
**"واپس کرو"** → aakhri wapas-hone-laiq kaam **palat** jata hai.

**Aur jhoota waada nahi:** call wapas nahi ho sakti → Maya **saaf keh deti hai**.
Purani halat maloom na ho → *"mujhe nahi pata is se pehle kya halat thi"* —
**andaza laga kar ghalat value set nahi karti.**

---

## 📊 TRACE — nazar aane wala saboot

Har jawab ke neeche patli patti:
```
🧠 Mistral 340ms   🔧 brightness_control · done   🗣️ fish
```
Nakaam tool par ⚠️. **`<think>` wala bug ab FEATURE ban gaya** — bakwas soch nahi,
**asli amal ka naqsha**.

---

## 🧪 LAB
```
📊 NAAP [ON]  🧹 SAAF [ON]  👑 MALIK [ON]  ⚡ AMAL [ON]
🎙️ SUNO [ON]  🤝 SACH [ON]  🗣️ BOLI [ON]
🛡️ IJAZAT [OFF]  ← YE NAYA        ⚡ TRUST MODE [OFF]
📜 AAJ KYA KYA KIYA — poora roznamcha
```

## 🧪 Saboot
```
✅ CSS PASS · Settings 72 · AWAAZ 294 · DIMAAG 155 · 🧪 LAB 253
──────────────────────────────────────────────────────────────
                                              774 / 774
```
Section 20 mein **36 naye** — har darja, TRUST MODE, OTP guard, rate limit,
roznamcha, undo (aur **jhoota undo na karna**), trace, aur switch-OFF ka rawaiya.

---

## 🗺️ Naqsha

```
✅ P0  NAAP-TOL       ✅ P1  SAAF ZUBAAN      ✅ P2  AMAL (tools + sach + loop)
✅ 🎙️ SUNO            ✅ 🗣️ BOLI              ✅ 🔒 AWAAZ MEHFOOZ
✅ P3  IJAZAT + TRACE   ← 🎉 LAGAAM LAG GAYI

👉 P4  BIJLI + 👁️ AANKHEIN   50ms amal (offline bhi) · Maya DEKH sakegi
   P6  KHUD-MUKHTAR         agar→to triggers · routines · khud-sikh router
   P5  ZINDA                streaming + barge-in (Kotlin — aakhir mein)
```

> **Ab taqat dena mehfooz hai** — kyunke ijazat, undo, rails aur roznamcha maujood hain.

---

## ▶️ Aap ke liye

1. Release → tag **`v5.3.0`** (versionCode 63)
2. LAB → **🛡️ IJAZAT ON**
3. Try karo:
   - *"brightness 100 karo"* → **foran** (sabz)
   - *"Monarch ko WhatsApp par hi bhejo"* → **🔴 ijazat ka card** aayega
   - *"wapas karo"* → brightness palat jayegi
   - *"aaj kya kya kiya"* → poora roznamcha
   - *"Ali ko bhejo mera OTP 1234 hai"* → **rok** diya jayega 🛡️
4. Har jawab ke neeche **📊 trace** dekho
