"""Per-scenario wire contracts for the L1 validator.

Data only. The checks live in validate_l1_logs.py and read this dict, which
stays one literal so stacked changes add an entry each.
"""

# What the wire must look like after a scenario ran. All endpoint names live
# here rather than in the checks, so the same checks serve this line's capture
# and the iOS one.
#
#   counts  endpoint -> exact number of requests. 0 forbids the endpoint.
#           An endpoint absent from counts is unconstrained.
#   order   (earlier, later) pairs. Relative, not adjacency: a request
#           between the two does not violate it.
#   fields  endpoint -> field -> exact number of that endpoint's requests
#           carrying the field. Same counting as `counts`, one level down;
#           0 forbids. Presence only, never a value comparison.
#           The endpoint may carry a position, "/v3/events/open[-1]": the rule
#           then judges only that one request of the endpoint (Python indexing,
#           so [0] is the first and [-1] the last) and the count is out of 1.
#           A position out of range is a contract error, whatever the expected
#           count, and so is any key with a "[" that is not a valid position.
#           Only for a rule that depends on which request carries the field;
#           the plain form stays the default.
#           It exists because an endpoint count cannot see a request changing
#           character: on 6.0.0-beta.0 the install is a /v3/events/open like
#           any other, so a first install and a launch on an installed device
#           put the same endpoints on the wire in the same order.
#
# Ported from the iOS line, where the same engine gates 4.0.0-beta.0. Kept
# byte-compatible on purpose: a contract that reads differently per platform
# is a parity gap wearing a helper's clothes.
SCENARIO_CONTRACTS = {
    # Every contract is derived from a real capture. organic_open's is less the
    # duplicate /v3/events/open that EMT-4136 removed.
    #
    # Every entry below is a test-plan scenario except link_generation, which
    # is the harness run that creates the link cold_firstInstall opens.

    # organic_open: a launch with no link. MainActivity.onCreate resolves
    # unconditionally, so a /v3/deeplink with no link precedes the open, the
    # nil-input resolve the beta design uses as the deferred link check.
    #
    # No `fields` rule. The property this scenario is really about is that the
    # open carries no link data, and the measurement that produced these shapes
    # reported the token rather than the link payload. The exact counts still
    # earn their place: they are what catches a second open reappearing.
    "organic_open": {
        "counts": {"/v3/deeplink": 1, "/v3/events/open": 1},
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {},
    },
    # cold_firstInstall: the link starts the app on a device with no prior
    # install. The install is a /v3/events/open like any other on 6.0, decided
    # by randomizedBundleToken == nil, so its missing token is what marks it.
    # /v1/url is 0 because the link is generated outside this capture.
    "cold_firstInstall": {
        "counts": {
            "/v3/deeplink": 1,
            "/v3/events/open": 1,
            "/v1/url": 0,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            "/v3/deeplink": {"android_app_link_url": 1},
            "/v3/events/open": {"randomized_bundle_token": 0},
        },
    },
    # warm_https_onNewIntent: the app alive and backgrounded when the link
    # arrives. This shape (process alive, activity stopped, link delivered through
    # onNewIntent to the existing task) is what Android's launch-time vocabulary and
    # `am start -W` report as HOT; EMT-4083 and the scenario names keep the Branch
    # meaning of warm, which is "app backgrounded".
    # Written from the capture, not from the ticket, which predicted one
    # /v3/deeplink and exactly one /v3/events/open. Measured on the CI emulator
    # (API 30, the passing Layer 1 run on 3eb17bc0): two and two. The
    # foreground open the process lifecycle observer used to send is gone by design
    # (EMT-4479: one open per requestDeepLinkData call), so each of the two launches
    # sends one open. The only thing this scenario does that cold_https does not is
    # background and foreground the app; that is a coincidence worth stating and not
    # a mapping this contract proves. What the ticket asked for and the capture
    # confirms is the absence of install, asserted at zero below.
    "warm_https_onNewIntent": {
        "counts": {
            "/v3/deeplink": 2,
            "/v3/events/open": 2,
            "/v1/url": 1,
            # Two, and not a property of the SDK: the TestBed logs one custom event
            # from MainActivity.onStart, so two means the activity went through
            # onStart twice, once per launch. The Kotlin driver asserts the stop
            # directly (WireScenarioDriver.assertBackgrounded); this count is the
            # capture-side witness the Python gate can see, kept as a second signal.
            # It cannot tell a stopped activity from a destroyed and recreated one,
            # and it moves with the TestBed, not the SDK. The cold contracts leave
            # it out for that reason.
            "/v3/events/custom": 2,
            "/v1/install": 0,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            # link_data is the attribution, and it belongs to the tapped link's
            # open, the last one: the scenario launches bare first, then taps. Judged
            # by position, a count of one of two would pass a capture where the bare
            # launch carried it and the tap's open lost it (the deeplink request
            # failed, or its reply was not a click). warm_uriScheme must not get this
            # rule: a bare scheme link matches nothing, so its opens have none.
            "/v3/events/open": {"randomized_bundle_token": 2},
            "/v3/events/open[-1]": {"link_data": 1},
            "/v1/url": {"hardware_id": 0},
            # The entry point, asserted in both directions across the two warm
            # scenarios. An https link now rides both fields, a scheme link only
            # external_intent_uri, so android_app_link_url is what separates them.
            # Without it warm_uriScheme says nothing warm_https_onNewIntent does not.
            # What each field carries is a mapping, and that is tested on the JVM
            # in RequestDeepLinkUriMappingTest, not here.
            "/v3/deeplink": {"android_app_link_url": 1, "external_intent_uri": 1},
        },
    },
    # warm_uriScheme: warm_https_onNewIntent's launch state entered through
    # branchtest:// instead of https. Same counts and order, measured in the same
    # run, and deliberately so: what it adds is not a different wire shape but the proof
    # that the manifest's branchtest filter matches and the OS hands a scheme
    # intent to a backgrounded app. Neither is reachable from a JVM test.
    "warm_uriScheme": {
        "counts": {
            "/v3/deeplink": 2,
            "/v3/events/open": 2,
            "/v1/url": 1,
            # Two for the same reason as warm_https_onNewIntent: one TestBed
            # onStart per launch. The driver asserts the stop directly; this is the
            # capture-side witness of it, kept as a second signal.
            "/v3/events/custom": 2,
            "/v1/install": 0,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            "/v3/events/open": {"randomized_bundle_token": 2},
            "/v1/url": {"hardware_id": 0},
            "/v3/deeplink": {"android_app_link_url": 0, "external_intent_uri": 1},
        },
    },
    # cold_https: the link starts the app on a device that already has it.
    # The open carries the token, which is what separates it from
    # cold_firstInstall.
    # /v3/events/custom is not counted in either: the TestBed logs one from
    # onStart, and whether it reaches the wire depends on init timing.
    "cold_https": {
        "counts": {
            "/v3/deeplink": 1,
            "/v3/events/open": 1,
            "/v1/url": 0,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            "/v3/deeplink": {"android_app_link_url": 1},
            "/v3/events/open": {"randomized_bundle_token": 1},
        },
    },
    # link_generation: the generation run that precedes
    # cold_firstInstall, judged on its own capture. It holds the /v1/url of the
    # cold line (the warm contracts count one of their own), so it carries the
    # EMT-4199 rule that /v1/url sends no hardware_id.
    "link_generation": {
        "counts": {"/v3/deeplink": 1, "/v3/events/open": 1, "/v1/url": 1},
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {"/v1/url": {"hardware_id": 0}},
    },
}
