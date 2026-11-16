# עובדה ביום

אפליקציית אנדרואיד שמציגה עובדה מעניינת חדשה בכל בוקר:
- **על רקע מסך הנעילה**: תמונה עם העובדה. רקע מסך הבית לא משתנה.
- **בהתראה שקטה**: גלויה במלואה גם במסך הנעילה.

אין בה אינטרנט, פרסומות או איסוף מידע. העובדות נמצאות ב־`app/src/main/res/raw/facts.txt`, אחת בכל שורה.

## בנייה
כל דחיפה ל־`main` מריצה את GitHub Actions (`.github/workflows/build.yml`), ובסוף הריצה יש שני קבצים להורדה:
- `daily-fact-test-apk`: קובץ APK להתקנה ישירה בטלפון לבדיקה.
- `daily-fact-play-aab`: קובץ AAB להעלאה ל־Google Play. הוא חתום רק אם הוגדרו הסודות שבסעיף הבא.

## חתימה ל־Google Play
ב־Settings → Secrets and variables → Actions צריך להגדיר:
`KEYSTORE_BASE64` (קובץ המפתח בקידוד base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
אסור להעלות את קובץ המפתח לריפו.

## מדיניות פרטיות
`docs/index.html` מתפרסם דרך GitHub Pages (Settings → Pages → main, ‏/docs), והקישור אליו נכנס לטופס של Google Play.
