# android
The Android App version of 5calls.org.

##License
This project is released open source under the MIT License. See [LICENSE](https://github.com/5calls/android/blob/master/LICENSE.txt) for more details.

##Basic structure
JsonController is used to request a list of issues from the server in MainActivity. When an issue is clicked, IssueActivity is launched for that issue, and JsonController is again used to record information back to the server. Location is set in LocationActivity and saved in SharedPreferences. A SQLite database (DatabaseHelper) is used to store user stats.

##Development
To run the app locally, you'll need to generate your own `google-services.json` file from the [Google website](https://developers.google.com/mobile/add).  It doesn't matter what services you enable, because you won't be talking to the prod Google account.  

##TODOs
TODOs are listed in Issues as well as in MainActivity.java and scattered throughout the code. Please (create and) assign yourself to an issue if you are working on something to avoid conflicts!

##Download
The app is published on Google Play and has a Beta channnel for testing. If you are interested in getting Beta releases and helping with user testing, reach out to a contributor to be added!

[![Get it on Google Play](https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=org.a5calls.android.a5calls&rdid=org.a5calls.android.a5calls)

##Contributors
 - [Katie Dektar](https://github.com/dektar)
 - [Greg Lee](https://github.com/gregliest)
 - [All contributors](https://github.com/5calls/android/graphs/contributors)

##Acknowledgments
Thanks to [Nick O'Neill](https://github.com/nickoneill) for organizing the 5calls project.
