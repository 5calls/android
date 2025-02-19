# android
The Android App version of 5calls.org.

## License
This project is released open source under the MIT License. See [LICENSE](https://github.com/5calls/android/blob/master/LICENSE.txt) for more details.

## Basic structure
FiveCallsApi is used to request a list of issues from the server in MainActivity. When an issue is clicked, IssueActivity is launched for that issue, and FiveCallsApi is again used to record information back to the server. Location is set in LocationActivity and saved in SharedPreferences. A SQLite database (in DatabaseHelper) is used to store user stats, which are displayed in StatsActivity.

## Development
To run the app locally, you'll need to generate your own `google-services.json` file from the [Google website](https://developers.google.com/mobile/add).  It doesn't matter what services you enable because you won't be talking to the prod Google account, but you should include Analytics.

To test the app, set `TESTING=true` in `FiveCallsApi.java`. This makes sure that calls logged to the server are marked as test calls.

### Notifications

To test snoozing notifications, change `FREQUENT_NOTIFICATION_DEBUG_MODE` to `true` in `NotificationUtils.java`. Note that notifications
do not use exact alarms, so they may arrive a few minutes later than scheduled.

### Releasing

Steps to create a release:
* Ensure all strings are in strings.xml and translations are in strings-es.xml.
* Run the automated tests
* Manual testing (Note issue #130 to add more automated testing)
  * Testing anything that's been touched since the last release
  * If the release includes any settings or database changes:
    * Do your settings and database info persist across an upgrade? (Check out the previous release branch & flash, then the lastest and flash)
    * Do all pages lay out as expected in portrait and landscape? Does rotation cause crashes in any activity?
  * If the release includes any UI changes:
    * Accessibility testing of new UI, including:
      * Use the Accessibility Scanner tool to check for contrast, touch target size, etc
      * Enable TalkBack and ensure all parts of the UI can be accessed using swiping to navigate and double-tap to click.
  * If the release includes any API changes:
    * Do filter and search functionality still work?
    * Does the app behave OK as airplane mode is toggled?
* Rev the version number and code in the app `build.gradle`.
* Push to the release branch
* Build a bundle in Android Studio and upload to Play store

## TODOs and Issues
Issues marked "[up for grabs](https://github.com/5calls/android/labels/up%20for%20grabs)" are available for contributors. Please add a comment to the issue when you start working on it to avoid conflicts. Feel free to add new issues to the list too -- even better if new issues have some justification or background information.

## Download
The app is published on Google Play and has a Beta channnel for testing. If you are interested in getting Beta releases and helping with user testing, reach out to @dektar to be added!

[![Get it on Google Play](https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=org.a5calls.android.a5calls&rdid=org.a5calls.android.a5calls)

The app is also available on the [Amazon App Store](https://www.amazon.com/5-Calls-Civic-Action/dp/B06Y128HV6).

## Contributors
 - [Katie Dektar](https://github.com/dektar)
 - [Greg Lee](https://github.com/gregliest)
 - [Bryan Sills](https://github.com/bryansills)
 - [Ben Rericha](https://github.com/brericha)
 - [All contributors](https://github.com/5calls/android/graphs/contributors)

## Acknowledgments
Thanks to [Nick O'Neill](https://github.com/nickoneill) for organizing the 5calls project.
