CHANGELOG
---------

#### Next Release
- Updated dependencies:
  - Updated Dagger to 2.9.
  - Updated RxJava to 1.2.6.
  - Updated Moshi to 1.4.0.
- Features:
  - Enabled large heap.

#### v1.18.0 (2017-02-01)
- Updated dependencies:
  - Updated Android Gradle plugin to 2.3.0-beta3.
  - Updated Gradle to 3.3.
  - Updated support library to 25.1.1.
  - Updated RxJava to 1.2.5.
  - Updated ButterKnife to 8.5.1.
  - Updated okhttp to 3.6.0.
  - Updated Stetho to 1.4.2.
- Features:
  - Supported bookmarks & notes in simple reading mode.
- Fixes:
  - Checked if Play Services is available before showing ads.

#### v1.17.1 (2017-01-07)
- Fixed reading progress syncing across devices.

#### v1.17.0 (2016-12-30)
- Added setting & analytics for daily verse.
- Added link to Facebook page.
- Added notes to Firebase App Indexing.
- Fixed issue that it scrolls to wrong position when changing translations.
- Moved "search menu" to FAB.
- Fixed continuous reading days while syncing.
- Enabled travis build.

#### v1.16.3 (2016-12-26)
- Fixed cursor jumping in notes due to synchronization.

#### v1.16.2 (2016-12-21)
- Updated UI immediately when data is synced from server.
- Improved search speed (by first returing 20 results, and then the rest).
- Fixed handling of reading progress tracking.

#### v1.16.1 (2016-12-19)
- Updated deps:
  - Updated Android Gradle plugin to 2.3.0-beta1.
  - Updated build tool to 25.0.2.
  - Updated support library to 25.1.0.
  - Updated RxJava to 1.2.4.
- Supported syncing user data (reading progress).

#### v1.16.0 (2016-12-08)
- Updated deps:
  - Updated Gradle to 3.2.1.
  - Updated Android Gradle plugin to 2.2.3.
  - Updated build tool to 25.0.1.
  - Updated support library to 25.0.1.
  - Updated Dagger to 2.8.
  - Updated RxJava to 1.2.3.
  - Updated okhttp to 3.5.0.
  - Updated Play Services and Firebase to 10.0.1.
  - Updated Stetho to 1.4.1.
- Used Firebase for analytics and crash reporting.
- Supported Firebase auth (with Google account).
- Supported syncing user data (bookmarks, notes).

#### v1.15.3 (2016-11-18)
- Updated deps:
  - Updated compile SDK to 25.
  - Updated Gradle to 3.2.
- Fixed book / chapter comparison when showing / hiding bookmarks / notes.
- Improved performance of search.

#### v1.15.2 (2016-11-14)
- Supported query of multiple keywords.

#### v1.15.1 (2016-11-07)
- Updated deps:
  - Reverted Play Services and Firebase to 9.4.0.
  - Updated RxJava to 1.2.2.
- Fixed scrolling when selecting parallel translations.

#### v1.15.0 (2016-11-02)
- Updated deps:
  - Updated Android Gradle plugin to 2.2.2.
  - Updated build tool to 25.
  - Updated target SDK version to 25.
  - Updated support library to 25.0.0.
  - Updated Play Services and Firebase to 9.8.0.
  - Updated Moshi to 1.3.1.
- Highlighted query in search results.
- Fixed "consecutive reading days", for real.
- Enabled navigate to prev / next book.

#### v1.14.6 (2016-10-18)
- Downgraded Moshi to 1.2.0.
- Improved analytics for failed download.

#### v1.14.5 (2016-10-16)
- Updated Moshi to 1.3.0.
- Reverted Play Services and Firebase to 9.4.0.

#### v1.14.4 (2016-10-12)
- Updated build tools and 3rd-party libs:
  - Updated Gradle to 3.1.
  - Updated Android Gradle plugin to 2.2.1.
  - Updated build tool to 24.0.3.
  - Updated support library to 24.2.1.
  - Updated Play Services and Firebase to 9.6.1.
  - Updated Dagger to 2.7.
  - Updated RxJava to 1.2.1.
  - Updated ButterKnife to 8.4.0.
  - Updated okio to 1.11.0.
  - Updated okhttp to 3.4.1.
  - Updated Moshi to 1.2.0.
- Updated analytics tracking.
- Used Firebase messaging for "new translation" notification.
- Supported parallel translations when copying / sharing verses.
- Fixed "continuous reading" calculation.

#### v1.14.3 (2016-07-13)
- Updated build tools and 3rd-party libs:
  - Updated Android Gradle plugin to 2.2.0-alpha5.
  - Updated build tool to 24.
  - Updated Google Play Services and Firebase to 9.2.1.
  - Updated ButterKnife to 8.2.0.
  - Updated RxJava to 1.1.7.
- Added basic usage for Firebase analytics, and switched to Firebase Cloud Messaging, Ads, and Invites.

#### v1.14.2 (2016-06-17)
- Updated build tools and 3rd-party libraries:
  - Updated target API level to 24.
  - Updated build tool to 23.0.3.
  - Updated Gradle to 2.14.
  - Updated Android Gradle plugin to 2.1.2.
  - Updated Fabric Gradle plugin to 1.21.5.
  - Updated Crashlytics to 2.5.7.
  - Updated support library to 24.0.0.
  - Updated Google Play Services to 9.0.2.
  - Updated Dagger to 2.5.
  - Updated RxJava to 1.1.6.
  - Updated RxAndroid to 1.2.1.
  - Updated Retrofit to 2.1.0.
  - Updated ButterKnife to 8.1.0.
  - Updated Stetho to 1.3.1.

#### v1.14.1 (2016-02-14)
- Supported "simple reading" mode.

#### v1.14.0 (2016-02-07)
- Supported NFC for M and above.
- Allowed app to be moved to external storage.
- Supported loading multiple translations.
- Supported bookmarks and notes.
- Updated 3rd-party libraries:
  - Updated Retrofit to 2.0.0 Beta 4.
  - Updated Stetho to 1.3.0.

#### v1.13.2 (2015-12-26)
- Updated min SDK level to 15.
- Fixed ProGuard issues.
- UI fixes for Bible reading activity.

#### v1.13.1 (2015-12-26)
- Last release for SDK level 9 to 14.

#### v1.13.0 (2015-12-25)
- Major refactoring to use MVP and RxJava.
- Added app invite support.

#### v1.12.5 (2015-06-29)
- Used GCM 3.0 for push notification.
- Bug fixes.

#### v1.12.4 (2015-03-23)
- Supported search history removal.
- Stopped using FLAG_ACTIVITY_REORDER_TO_FRONT for KitKat and above.

#### v1.12.3 (2014-12-22)
- Fixed translation list grouping.

#### v1.12.2 (2014-12-20)
- Used Dagger for dependency injection and ButterKnife for view binding.
- Used proper icon for notifications on Lollipop and above.

#### v1.12.1 (2014-12-11)
- Bug fixes.

#### v1.12.0 (2014-12-09)
- Added push notification support.
- Used Crashlytics for crash tracking.

#### v1.11.4 (2014-11-28)
- Bug fixes.

#### v1.11.3 (2014-11-27)
- Bug fixes.

#### v1.11.2 (2014-11-22)
- Bug fixes.

#### v1.11.1 (2014-11-05)
- Bug fixes.

#### v1.11.0 (2014-11-04)
- Supported recent search history.
- Adapted to material design.

#### v1.10.4 (2014-10-17)
- Added activity transition animation.
- Bug fixes.

#### v1.10.3 (2014-08-21)
- Bug fixes.

#### v1.10.2 (2014-08-11)
- Bug fixes.

#### v1.10.1 (2014-08-01)
- Bug fixes.

#### v1.10.0 (2014-07-12)
- Supported app indexing.
- Improved network support.

#### v1.9.0 (2014-05-13)
- Supported reading progress tracking.
- Supported IAP to remove ads.

#### v1.8.2 (2014-04-14)
- Bug fixes.

#### v1.8.1 (2014-04-08)
- Bug fixes.

#### v1.8.0 (2014-04-08)
- Updated min SDK level to 9.
- Supported screen always on.
- Improved translation list sorting.

#### v1.7.1 (2013-09-02)
- Bug fixes.

#### v1.7.0 (2013-08-30)
- Adapted to Android Studio and Gradle build system.
- Used action bar from system.

#### v1.6.0 (2012-12-07)
- Added night mode support.
- Added front size support.

#### v1.5.3 (2012-11-07)
- Bug fixes.

#### v1.5.2 (2012-11-06)
- Bug fixes.

#### v1.5.1 (2012-11-05)
- Bug fixes.

#### v1.5.0 (2012-11-05)
- Supported verse searching.

#### v1.4.0 (2012-09-05)
- New UI.
- Supported verse copying.

#### v1.3.3 (2012-08-13)
- Bug fixes.

#### v1.3.2 (2012-08-05)
- Bug fixes.

#### v1.3.1 (2012-08-04)
- Bug fixes.

#### v1.3.0 (2012-07-14)
- Supported verse sharing.
- Supported adding shortcut to desktop.

#### v1.2.0 (2012-07-01)
- Supported translation downloading.
