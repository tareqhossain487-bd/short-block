# ShortsBlocker (Android)

এই প্রজেক্ট YouTube Shorts এবং Facebook Reels স্ক্রিন ডিটেক্ট করে অটোমেটিক "back" প্রেস করে বন্ধ করে দেয় — নিজস্ব YouTube/Facebook অ্যাপের ভেতরেই কাজ করে।

## কীভাবে APK বানাবেন (ধাপে ধাপে)

1. **Android Studio ইনস্টল করুন**
   https://developer.android.com/studio থেকে ডাউনলোড করে ইনস্টল করুন (ফ্রি)।

2. **প্রজেক্ট ওপেন করুন**
   Android Studio খুলে `Open` → এই `ShortsBlocker` ফোল্ডারটা সিলেক্ট করুন।
   প্রথমবার খুলতে কিছুক্ষণ সময় লাগবে (Gradle sync হবে, ইন্টারনেট লাগবে)।

3. **APK বানান**
   মেনু থেকে: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   বিল্ড শেষ হলে নিচে একটা notification আসবে — `locate` ক্লিক করলে APK ফাইল পাবেন
   (সাধারণত `app/build/outputs/apk/debug/app-debug.apk`)।

4. **ফোনে ইনস্টল করুন**
   APK ফাইলটা ফোনে পাঠিয়ে ইনস্টল করুন (Settings → Security → "Install unknown apps" চালু করতে হতে পারে)।

5. **Accessibility Service চালু করুন**
   অ্যাপ ওপেন করে "Enable Accessibility Service" বাটনে চাপ দিন → Android Settings-এ গিয়ে
   `ShortsBlocker`-কে ON করুন। **এই ধাপটা ছাড়া অ্যাপ কিছুই ব্লক করবে না** — এটা Android-এর
   নিরাপত্তা নিয়ম, অ্যাপ নিজে নিজে এই permission চালু করতে পারে না।

## সতর্কতা / সীমাবদ্ধতা

- **Detection heuristic-based**: `ShortsBlockerService.kt`-এর `shortsKeywords` লিস্ট দিয়ে
  Shorts/Reels স্ক্রিন চেনা হয়। YouTube বা Facebook তাদের অ্যাপ আপডেট করলে এই id/description
  বদলে যেতে পারে, ফলে ব্লকিং কাজ না-ও করতে পারে। তখন নতুন id খুঁজে ওই লিস্টে যোগ করতে হবে।
- **Battery/permission**: Accessibility Service ক্রমাগত স্ক্রিন কনটেন্ট পড়ে, তাই কিছুটা ব্যাটারি
  খরচ হবে। এটা স্বাভাবিক — কিন্তু ভারী কিছু না।
- **Play Store**: এই ধরনের অ্যাপ নিজের ফোনে sideload করে ব্যবহারের জন্য ঠিক আছে, কিন্তু Play
  Store-এ পাবলিশ করতে চাইলে Google-এর accessibility policy অনুযায়ী রিভিউ পাস করতে হবে।
- এটা শুধুমাত্র **নিজের ব্যবহারের জন্য** বানানো একটা টুল — অন্য কারো ডেটা পড়ে না, কোনো
  নেটওয়ার্ক কল করে না, কোনো অ্যানালিটিক্স নেই।
