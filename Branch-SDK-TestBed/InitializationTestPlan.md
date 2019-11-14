# Test plan for initialization scenarios

### Application in the background
Cold start: will work fine (entry point: app.onCreate)
Warm start: will also work fine (entry point: RoutingActivity.onCreate).
Hot start will: also work fine (entry point: RoutingActivity.onStart)

### Application in the foreground
Burning hot start: this will most likely work fine because the app is 
running, so the user is on some "RoutedActivity", that means that 
RoutingActivity is invisible, and therefore the entry point will again be (RoutingActivity.onStart). I say "most likely" because there is a chance that the RoutingActivity was never hidden away (e.g. the "RoutedActivity" takes up just part of the screen or it is semi-transparent, therefore partially showing the initial RoutingActivity, in which case the entry point will be RoutingActivity.onNewIntent instead onStart and the branch link will fail.