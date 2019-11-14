# Test plan for different initialization scenarios

This is a test plan enlisting possible initialization scenarios. As CSMs
find bugs arising from unforeseen initialization scenarios, this list should be updated.


### Assumptions:
* User followed [Android documentation](https://docs.branch.io/apps/android/), 
so `initSession` is only called in the `RoutingActivity` (aka `LaunchActivity`).
* Application can have multiple activities (all non-routing activities 
will be called `RoutedActivity`)
* Latest Branch SDK version is being used


## Scenarios

##### Application in background

Test by opening the app via:
* launcher icon
* recent apps tray (not available in cold start)
* push notification

Cold start: entry point = `RoutingActivity.onCreate`
* Repro: Close app and remove it from the recent apps list
(not 100% guarantee that this will work because manufacturers determine
if application is killed when removed from recents list). Alternatively,
call `adb shell pm clear my.app.package.id` from the command line (this 
will also clear cache and stored data). Open app.


Warm start: entry point = `RoutingActivity.onCreate`
* Repro: Close app via the back button. Open app.

Hot start: entry point is either `RoutingActivity.onStart` or `RoutedActivity.onStart` 
depending which activity the user was last on. However, if app is opened 
via a push notification, the entry point is guaranteed to be `RoutingActivity.onStart`.
* Repro: Close app via the home button. Open app.


##### Application in foreground

Test by opening the app via:
* push notification

Burning hot start: entry point = either `RoutingActivity.onNewIntent` 
or `RoutingActivity.onStart` depending if user is currently on `RoutingActivity`
or `RoutedActivity` respectively. There is one exception, if the user is on
`RoutedActivity` but `RoutingActivity` is still partially visible, then the 
entry point will be `RoutingActivity.onNewIntent`.

There are four possible scenarios the users can find themselves in, 
`reInitSession` is either called or not and the user may may currently be 
in either `RoutingActivity` or `RoutedActivity`. Note that documentation 
gives instructions to use `reInitSession`, however it's a new instruction, 
so it may be overlooked by existing users:
* `reInitSession` is not used
    * User is in `RoutingActivity` = Branch initialization is expected to FAIL
    * User is in `RoutedActivity` = Branch initialization is expected work _unless_ 
    `RoutingActivity` is still partially visible (e.g. `RoutedActivity` is
    semi-transparent or is not using the full screen)
* `reInitSession` is used
    * User is in `RoutedActivity` = Branch initialization is expected work
    * User is in `RoutingActivity` = Branch initialization is expected work