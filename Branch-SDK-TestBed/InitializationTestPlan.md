# Test plan for different (re)initialization scenarios

This is a test plan enlisting possible initialization scenarios. As CSMs
find bugs arising from unforeseen initialization scenarios, this list should 
be updated. Unless noted, initialization is expected to work in all scenarios.


## Assumptions:
* User followed [Android documentation](https://docs.branch.io/apps/android/)
* Application can have multiple activities (launcher activity is called
`SplashActivity` and all others are `NextActivity`)
* Latest Branch SDK version is being used


## Scenario #1:
* `initSession` is only called in `SplashActivity.onStart`.


### 1. Application in background
```
Test by opening the app via:
* launcher icon
* recent apps tray (not available in cold start)
* push notification
```
### Cold start:
* Repro: Close app and remove it from the recent apps list
(not 100% guarantee that this will work because manufacturers determine
if application is killed when removed from recents list). Alternatively,
call `adb shell pm clear my.app.package.id` from the command line (this 
will also clear cache and stored data). Open app.
* Initialization entry point = `SplashActivity.onCreate`


### Warm start:
* Repro: Close app via the back button. Open app.
* Initialization entry point = `SplashActivity.onCreate`

### Hot start:
* Repro: Close app via the home button. Open app.
* Initialization entry point is either `SplashActivity.onStart` or 
`BranchActivityLifecycleObserver.onResume` depending on whether `SplashActivity` 
or `NextActivity` was in foreground last. However, if app is opened via 
a push notification, the Initialization entry point is guaranteed to be 
`SplashActivity.onStart`.


### 2. Application in foreground
```
Test by opening the app via:
* push notification
```
### Burning hot start:
* Repro: have the app open
* Initialization entry point = either `SplashActivity.onNewIntent` 
or `SplashActivity.onStart` depending if user is currently on `SplashActivity`
or `NextActivity` respectively. There is one exception, if the user is on
`NextActivity` but `SplashActivity` is still partially visible, then the 
entry point will be `SplashActivity.onNewIntent`.

There are four possible scenarios the users can find themselves in, 
`reInitSession` is either called or not and the user is currently in 
`SplashActivity` or in `NextActivity`. Note that documentation gives 
instructions to use `reInitSession`, however it's a new instruction, so 
it may be overlooked by existing users. When the latter happens, we may 
expect an initialization failure:
* `reInitSession` is not used (i.e. not following documentation)
    * User is in `SplashActivity` = Branch initialization is guaranteed to FAIL
    * User is in `NextActivity` = Branch initialization is expected work _unless_ 
    `SplashActivity` is still partially visible (e.g. `NextActivity` is
    semi-transparent or is not using the full screen)


## Scenario #2:
* `initSession` is called in both `SplashActivity` and `NextActivity`

### 1. Application in foreground
```
Test by opening the app via push notification launching `NextActivity`:
* when `NextActivity` is currently in foreground
* when `NextActivity` is in backstack (e.g. has been launched before 
but user is currently on another `NextActivity`)
* when `NextActivity` is NOT in backstack (e.g. this particular 
`NextActivity` has NOT been launched before)
```

### Burning hot start:
* Repro: have the app open

Again, there are expected failures when `reInitSession` is NOT used in `NextActivity`
* user is currently in `NextActivity` = guaranteed initialization failure
* `NextActivity` is in backstack BUT partially visible