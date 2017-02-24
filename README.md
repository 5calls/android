# android
The Android App version of 5calls.org.

##License
This project is released open source under the MIT License. See [LICENSE](https://github.com/5calls/android/blob/master/LICENSE.txt) for more details.

##Basic structure
FiveCallsApi is used to request a list of issues from the server in MainActivity. When an issue is clicked, IssueActivity is launched for that issue, and FiveCallsApi is again used to record information back to the server. Location is set in LocationActivity and saved in SharedPreferences. A SQLite database (in DatabaseHelper) is used to store user stats, which are displayed in StatsActivity.

##Development
To run the app locally, you'll need to generate your own `google-services.json` file from the [Google website](https://developers.google.com/mobile/add).  It doesn't matter what services you enable because you won't be talking to the prod Google account, but you should include Analytics.

Run against a local version of the Go server to avoid having test calls counted: set `FiveCallsApi.USE_LOCAL_DEBUG_REPORT = true` and spin up a Go server from instructions at [github.com/5calls/5calls](https://github.com/5calls/5calls).

##TODOs and Issues
Issues marked "[up for grabs](https://github.com/5calls/android/labels/up%20for%20grabs)" are available for contributors. Please add a comment to the issue when you start working on it to avoid conflicts. Feel free to add new issues to the list too -- even better if new issues have some justification or background information.

##Download
The app is published on Google Play and has a Beta channnel for testing. If you are interested in getting Beta releases and helping with user testing, reach out to @dektar to be added!

[![Get it on Google Play](https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=org.a5calls.android.a5calls&rdid=org.a5calls.android.a5calls)

##Contributors
 - [Katie Dektar](https://github.com/dektar)
 - [Greg Lee](https://github.com/gregliest)
 - [All contributors](https://github.com/5calls/android/graphs/contributors)

##Acknowledgments
Thanks to [Nick O'Neill](https://github.com/nickoneill) for organizing the 5calls project.
