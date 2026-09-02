# MAYA v5.9.1 — 🗣️ doctor ka jhoota button ab ASAL hai

**versionCode 70 · chhoti hotfix release**

## Masla (user ne pakda: "button aisa kuch nahi hai")

🩺 KAAN DOCTOR ki report mein likha hota tha *"[ON-DEVICE] dabao"* — **magar aisa
button kahin THA HI NAHI.** Doctor sirf text banata hai; woh line sirf likhi
hui dawa-MAT dikhati thi. User (sahi tarike se) pareshan ho gaya.

## Ilaj

1. **ASLI button**: LAB panel mein naya **🗣️ ON-DEVICE LANGUAGE** — dabate hi:
   - Android ki zubaan-download screen kholta hai (try-chain: **Gboard → Voice
     typing** settings → voice-input picker → aam Settings — koi bhi chain na
     chale to agli)
   - aur **likha hua pura shajra** bhi saath dikhata hai (koi ajeeb screen khul
     jaye to bhi raasta bhoolte nahi)
2. Doctor ka text ab jhoota hawala nahi deta — seedha asli button ka rasta batata hai.
3. Manual rasta hamesha maujood: *Settings → System → Languages & input →
   On-screen keyboard → Gboard → Voice typing → Faster voice typing ON → Offline
   speech recognition → اردو + English (India) download.*

## Kyun yeh zaroori hai

Bina on-device pack ke Android 14 par "Maya" ONLINE Google ur-PK decoder se
sun'ta hai — jo "مایا" ki jagah "ہے" jaisa kuch parh leta hai (device par pack
ho to sab offline sahi chalta hai — wake decode kabhi nahi rukta).

## Saboot

`npm test` = **975/975** (lab 450 → 454, Section 26c ke 4 naye lock).
CI har push par APK banata hai.
