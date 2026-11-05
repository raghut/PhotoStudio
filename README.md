# Shop Tools — everyday tools for the shop

Native Android app (Kotlin + Jetpack Compose + CameraX). English and Telugu.

The home screen has one big tile per tool:

| Tool | What it does |
|---|---|
| **Passport Photos** | Passport / 4×6 photos with exact-size printing (details below) |
| **Interest (Vaddi)** | Simple interest the village way — "₹2 for every ₹100 a month" |
| **Quick Bill** | Bill / receipt: send as a picture or WhatsApp message, or print |
| **Chit Calculator** | Chit auction: winner's amount, dividend, what each member pays, interest |
| Settings | Language, photo options |

More tools planned: EMI/loan, GST & discount, daily sales notebook, unit
conversion (acres/guntas, kg/quintals).

## Interest (Vaddi) calculator

Enter the way it's said at the shop — no percentages:

- **Amount given** (e.g. ₹10,000)
- **Interest for every ₹100, per month**: tap ₹1 / ₹1.5 / ₹2 / ₹2.5 / ₹3 / ₹5, or type any amount.
  The yearly % is shown underneath for reference (₹2 a month = 24% a year).
- **Money given on** and **money returned on**. Either type the date on the number keyboard —
  just the digits, the dashes appear by themselves (`05112026` → 05-11-2026) — or tap the
  calendar icon. Both fill the same box, so a picked date can still be corrected by typing.
  The return date starts as today; type over it, pick another, or tap **Today** to go back.
  An impossible date (31-02-2026) is marked in red.

It shows:

- Time: *10 months 12 days* (and total days)
- Interest per month = amount × rate ÷ 100
- Total interest, and **principal + interest** in large text
- **Month by month** list: each month's dates, that month's interest, and the total till then
- **Share** sends the summary as text (WhatsApp etc.)

How time is counted: calendar months from the day the money was given (5 Jan → 5 Feb is one month,
31 Jan → 28 Feb → 31 Mar stays on month-ends). Days left after the last full month are charged as
part of a 30-day month — 15 extra days = half a month's interest. The logic lives in
`tools/SimpleInterest.kt` with unit tests in `app/src/test/.../SimpleInterestTest.kt`.

## Quick bill

- **Shop name and address/phone** are typed once (tap the top card) and printed on every bill.
- **Bill number** goes up by one each time a bill is sent or printed; sending the same bill again
  keeps its number. **New bill** clears the screen for the next customer.
- **Items**: name × quantity × rate, with the amount worked out on each line. Items billed before
  show as one-tap chips with their last rate (last 12 kept).
- **Discount** and **received** are optional; with received filled in, the bill shows the balance due.
- **Send bill picture**: the receipt as an image — pick WhatsApp and the customer.
- **Send as message**: the bill as text. If the customer's 10-digit mobile is filled in, WhatsApp
  opens straight on that customer's chat with the bill typed in.
- **Print**: about 10 cm wide at the top of the page (A4 by default) — cut off the rest.
  The receipt is drawn as vectors, so it prints sharp.

Code: `tools/Bill.kt` (sums, WhatsApp text, tested), `bill/ReceiptRenderer.kt` (drawing),
`bill/BillSharing.kt` (picture, message, print), `ui/screens/BillScreen.kt`.

## Chit calculator

Same formula as the Tool Basket website (`handy-finance-tools`, `src/routes/chit-advanced/calculations.ts`).
Inputs: chit amount, members, months, commission %, auction month, auction amount (the bid — what the
winner gives up). Opens with the website's example (₹3,00,000 · 20 members · 20 months · 4% ·
month 14 · ₹20,000) so it shows a full answer; **Clear** empties it.

| Result | Formula | Example |
|---|---|---|
| Winner takes | chit − auction amount | ₹2,80,000 |
| Monthly share | chit ÷ members | ₹15,000 |
| Commission | chit × commission % | ₹12,000 |
| Dividend per member | (auction − commission) ÷ members | ₹400 |
| Each member pays | monthly share − dividend | ₹14,600 |
| Winner's interest | (auction − commission) ÷ (winner takes × months left ÷ 12) | 5.71% a year = ₹0.48 per ₹100 a month |

In the last month there are no months left, so no interest is shown. **Share** sends the summary as text.
Logic: `tools/ChitFund.kt`, tests: `ChitFundTest.kt`.

# Passport Photos

## What it does

Opened from the **Passport Photos** tile on the home screen:

```
Photo ┬─ Take Photo ─► Choose size ─┬─ Passport (3.5 × 4.5 cm) ─► Camera: passport frame + face oval
      │                             └─ 4×6 inch photo         ─► Camera: 4×6 frame
      │                                        │                  (flash: Off / Auto / On)
      │                                        ▼
      │                    Adjust: white background · brightness · contrast · colour · warmth
      │                                        │
      │                                        ▼ name the photo (optional) ─► saved
      │                    Print preview: 8 passport photos on one 4×6 sheet
      │                                   ─► Print  ·  Save to phone (Photo 4×6 / PDF)  ·  Share  ·  Edit
      ├─ Previous Photos ─► tap any photo ─► reprint / share / delete
      │                  └─ Edit ─────────► back to Adjust, sliders where you left them
      └─ Settings ─► Language (English / తెలుగు), white background default, back/front camera, cutting lines
```

- **Passport sheet**: 8 photos (4 × 2) on 4×6 paper with a 3.5 mm safe border and thin grey cutting lines.
- **Exact size printing**: the app never uses "fit to page". The sheet is drawn at true physical size,
  so each photo comes out 35 × 45 mm. If A4 is selected by mistake, it still prints true size in the middle.
- **White background**: on-device person detection (Google ML Kit, works offline). Brightness/contrast
  only change the person; the background stays pure white.
- **Flash**: a button at the top of the camera cycles Off → Auto → On. It only appears when the
  chosen camera actually has a flash, so it stays hidden on front cameras that don't.
- **Names**: after adjusting, the app asks for a name (skippable — just leave it empty). The name
  shows on the Previous Photos card and leads the saved file name, e.g. `Ravi_passport_04-11-2026_1830.jpg`.
- **Edit again**: any photo can be reopened from Previous Photos (the **Edit** button on its card) or
  from the print preview. The sliders come back exactly where they were left, and editing works from
  the *untouched* capture — so changes never pile up on top of each other, however often it's edited.
  Editing replaces the photo in place; it does not create a second entry.
- **Share**: sends the single photo (JPEG, 600 dpi) to WhatsApp/Gmail, useful for online application forms.
- **Save to phone**: keeps the whole sheet as a file, so it can be printed later or sent on WhatsApp.
  Two formats, both exactly 4 × 6 inch:
  - **Photo (4×6)** — JPEG, 1200 × 1800 px (300 dpi), lands in *Gallery → Photo Studio*.
    Print it from the Gallery, or hand it to any photo lab as a normal 4×6 print.
  - **PDF** — one page the true physical size of the sheet, lands in *Downloads → Photo Studio*.
    Use this when the photo must come out at an exact size (the page carries the measurements, so
    no print dialog can shrink it).

  A message appears after saving with a **Share** button, in case it should go out straight away.
- All photos stay on the phone — in the app's private storage, plus Gallery/Downloads for the
  sheets you choose to save. Nothing is uploaded anywhere.

## Build and install (one time, on your Mac)

1. Install **Android Studio** (any 2024+ version).
2. *File → Open* → select this `PhotoStudio` folder. Let Gradle sync (first sync downloads ~1 GB).
   If Android Studio offers to upgrade the Android Gradle Plugin, it's safe to accept.
3. On your parents' phone: *Settings → About phone → tap "Build number" 7 times* → *Developer options → USB debugging ON*.
4. Connect the phone by USB, pick it in the device dropdown, press **Run ▶**.

To install without a cable: *Build → Build App Bundle(s) / APK(s) → Build APK(s)*, then send
`app/build/outputs/apk/debug/app-debug.apk` on WhatsApp and open it on the phone (allow "Install unknown apps").

Requirements: Android 7.0 or newer.

## Printer setup (Canon PIXMA G3770)

1. Connect the printer to the shop Wi-Fi (2.4 GHz only — the G3770 doesn't support 5 GHz).
2. On the phone install **Canon Print Service** from Play Store (or use the built-in Mopria/Default Print Service).
3. In the app press **Print** → choose the Canon printer → **Paper size 4×6 (10×15 cm)** → **Paper type: Photo paper (glossy)**
   → print quality High. Load 4×6 photo paper in the rear tray.
4. Print one test sheet and measure a photo with a ruler: it should be **3.5 cm wide × 4.5 cm tall**.
   If it's slightly smaller, look for a "Fit to page / Scale" option in the print dialog and turn it off.

Tip: for the 4×6 single photo, choose **Borderless** in the print options so the photo fills the paper edge to edge.

## Code map

| File | What it does |
|---|---|
| `model/PhotoSize.kt` | Photo sizes. Add e.g. `STAMP(20f, 25f, false)` and everything adapts. |
| `model/SheetLayout.kt` | Fits as many photos as possible on 4×6, with margins and cut gaps |
| `ui/screens/CameraScreen.kt` | Camera, flash, frame overlay, capture and crop to frame |
| `image/BackgroundTools.kt` | White background via ML Kit selfie segmentation |
| `image/ImageTools.kt` | Crop, colour adjust, sheet rendering at 300 dpi |
| `print/SheetPrinter.kt` | Android print framework, true-size PDF |
| `data/SheetExporter.kt` | "Save to phone": sheet as 4×6 JPEG (Gallery) or PDF (Downloads) |
| `data/PhotoStore.kt` | Saves photos (finished + original + name/sliders), history list, share |
| `tools/SimpleInterest.kt` | Vaddi calculation: months + leftover days, month-by-month rows |
| `tools/DateInput.kt` | Typed dates: digits ↔ date, auto dashes |
| `tools/ChitFund.kt` | Chit auction calculation |
| `ui/screens/ChitScreen.kt` | Chit calculator screen |
| `tools/Money.kt` | ₹1,00,000 Indian number format, dd-MM-yyyy dates |
| `ui/screens/InterestScreen.kt` | Interest calculator screen and share text |
| `ui/screens/HomeScreen.kt` | Tools home (one tile per tool) and the photo start screen |
| `res/values-te/strings.xml` | Telugu text — edit here if any wording sounds odd |

## How a photo is stored

```
files/jobs/<timestamp>/
  photo.jpg     the finished photo — what gets printed and shared
  original.jpg  the untouched capture — what Edit re-opens
  meta.txt      size, name, slider values, white-background on/off
```

Photos taken with the very first version have no `original.jpg`, so Edit falls back to `photo.jpg`
for those — their earlier adjustments stay baked in. Everything taken since edits cleanly.

## Ideas for v2

- Pick a photo from Gallery/WhatsApp and crop it (customers bring photos on their phone).
- More sizes: stamp (2 × 2.5 cm), US visa (2 × 2 in), 5×7.
- Choose how many copies on the sheet; mix two people on one sheet.
- Daily count of photos printed (simple earnings log).
