# Test plan for different (re)initialization scenarios

This is a test plan enlisting possible initialization scenarios. As CSMs
find bugs arising from unforeseen initialization scenarios, this list should 
be updated. Unless noted, initialization is expected to work in all scenarios.


## Assumptions:
* User followed [Android documentation](https://docs.branch.io/apps/android/)
* Application can have multiple activities (all non-routing activities 
are called `RoutedActivity`)
* Latest Branch SDK version is being used


## Scenario #1:
* `initSession` is only called in `RoutingActivity.onStart`.


##### 1. Application in background
```
Test by opening the app via:
* launcher icon
* recent apps tray (not available in cold start)
* push notification
```
##### Cold start:
* Repro: Close app and remove it from the recent apps list
(not 100% guarantee that this will work because manufacturers determine
if application is killed when removed from recents list). Alternatively,
call `adb shell pm clear my.app.package.id` from the command line (this 
will also clear cache and stored data). Open app.
* Initialization entry point = `RoutingActivity.onCreate`


##### Warm start:
* Repro: Close app via the back button. Open app.
* Initialization entry point = `RoutingActivity.onCreate`

##### Hot start:
* Repro: Close app via the home button. Open app.
* Initialization entry point is either `RoutingActivity.onStart` or 
`BranchActivityLifecycleObserver.onResume` depending on whether `RoutingActivity` 
or `RoutedActivity` was in foreground last. However, if app is opened via 
a push notification, the Initialization entry point is guaranteed to be 
`RoutingActivity.onStart`.


##### 2. Application in foreground
```
Test by opening the app via:
* push notification
```
##### Burning hot start:
* Repro: have the app open
* Initialization entry point = either `RoutingActivity.onNewIntent` 
or `RoutingActivity.onStart` depending if user is currently on `RoutingActivity`
or `RoutedActivity` respectively. There is one exception, if the user is on
`RoutedActivity` but `RoutingActivity` is still partially visible, then the 
entry point will be `RoutingActivity.onNewIntent`.

There are four possible scenarios the users can find themselves in, 
`reInitSession` is either called or not and the user is currently in 
`RoutingActivity` or in `RoutedActivity`. Note that documentation gives 
instructions to use `reInitSession`, however it's a new instruction, so 
it may be overlooked by existing users. When the latter happens, we may 
expect an initialization failure:
* `reInitSession` is not used (i.e. not following documentation)
    * User is in `RoutingActivity` = Branch initialization is guaranteed to FAIL
    * User is in `RoutedActivity` = Branch initialization is expected work _unless_ 
    `RoutingActivity` is still partially visible (e.g. `RoutedActivity` is
    semi-transparent or is not using the full screen)


## Scenario #2:
* `initSession` is called in both `RoutingActivity` and `RoutedActivity`

##### 1. Application in foreground
```
Test by opening the app via push notification launching `RoutedActivity`:
* when `RoutedActivity` is currently in foreground
* when `RoutedActivity` is in backstack (e.g. has been launched before 
but user is currently on another `RoutedActivity`)
* when `RoutedActivity` is NOT in backstack (e.g. this particular 
`RoutedActivity` has NOT been launched before)
```

##### Burning hot start:
* Repro: have the app open

Again, there are expected failures when `reInitSession` is NOT used in `RoutedActivity`
* user is currently in `RoutedActivity` = guaranteed initialization failure
* `RoutedActivity` is in backstack BUT partially visible