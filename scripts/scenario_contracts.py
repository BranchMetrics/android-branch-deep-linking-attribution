"""Per-scenario wire contracts for the L1 validator.

Data only. The checks live in validate_l1_logs.py and read this dict, which
stays one literal so stacked changes add an entry each.
"""

# Fields a /v3/deeplink sent at attribution level NONE must not carry: device ids, and the install
# referrer's fields other than its click id, which goes as link_identifier.
NONE_DEEPLINK_ABSENT = dict.fromkeys((
    "advertising_ids", "google_advertising_id", "aaid", "hardware_id", "is_hardware_id_real", "anon_id",
    "local_ip", "identity", "randomized_device_token", "randomized_bundle_token",
    "link_click_id", "install_referrer_extras", "google_search_install_referrer", "clicked_referrer_ts",
    "clicked_referrer_server_ts", "install_begin_ts", "install_begin_server_ts", "app_store", "is_meta_ct",
    "first_install_time", "latest_install_time", "latest_update_time", "previous_update_time",
), 0)

# An install from a link at NONE, then consent: one /v3/deeplink with only the referrer's click id, then
# the open with the install's link and the install referrer's fields.
INSTALL_REFERRER_LINK_CONTRACT = {
    "counts": {"/v3/deeplink": 1, "/v3/events/open": 1, "/v1/url": 0},
    "order": (("/v3/deeplink", "/v3/events/open"),),
    "fields": {
        "/v3/deeplink": {**NONE_DEEPLINK_ABSENT, "link_identifier": 1, "tracking_disabled": 1},
        "/v3/events/open": {
            "link_data": 1, "link_identifier": 1, "link_click_id": 1,
            "install_referrer_extras": 1, "app_store": 1, "tracking_disabled": 0,
        },
    },
}

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
    # The open carries no link_data: api-open credits any link_data it gets, so
    # an organic open must not send one. The exact counts catch a second open.
    "organic_open": {
        "counts": {"/v3/deeplink": 1, "/v3/events/open": 1},
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {"/v3/events/open": {"link_data": 0}},
    },
    # attribution_none: design "Track Open after Deep Link", if CPP is NONE no
    # /v3/events/open is sent; the resolve goes out stripped and marked.
    "attribution_none": {
        "counts": {"/v3/deeplink": 1, "/v3/events/open": 0},
        "order": (),
        "fields": {"/v3/deeplink": {"tracking_disabled": 1, "randomized_device_token": 0,
                   "randomized_bundle_token": 0, "hardware_id": 0, "anon_id": 0}},
    },
    # cold_firstInstall: the link starts the app on a device with no prior
    # install. The install is a /v3/events/open like any other on 6.0, decided
    # by randomizedBundleToken == nil, so its missing token is what marks it.
    # Its link_data is what credits the install to the link.
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
            "/v3/events/open": {"randomized_bundle_token": 0, "link_data": 1},
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
    # cold_firstInstall, and the link_data that credits the open to the link.
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
            "/v3/events/open": {"randomized_bundle_token": 1, "link_data": 1},
        },
    },
    # hot_https_foreground: an App Link delivered via onNewIntent to a MainActivity that stayed
    # resumed. Same wire shape as cold_https, both tokens already known; hot-vs-warm is proven by
    # WireScenarioDriver.deliverHot (resumed, never stopped or destroyed), not by the wire.
    # An https link rides both android_app_link_url and external_intent_uri (the launch fields are
    # added at send time from the intent, as on master), and link_identifier appears only when the
    # URL carries a link_click_id. The link_identifier zero pins the resolved URL, not how the
    # intent was addressed; android_app_link_url is what separates this from hot_uriScheme.
    # link_data on the open is the attribution: without it the launch was reported organically.
    # When EMT-4395 lands, a warm delivery emits this same wire, and hot-vs-warm rests on the
    # driver's lifecycle asserts alone.
    "hot_https_foreground": {
        "counts": {
            "/v3/deeplink": 1,
            "/v3/events/open": 1,
            "/v1/url": 0,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            "/v3/deeplink": {
                "android_app_link_url": 1,
                "external_intent_uri": 1,
                "link_identifier": 0,
            },
            "/v3/events/open": {"randomized_bundle_token": 1, "link_data": 1},
        },
    },
    # link_generation: the generation run that precedes
    # cold_firstInstall, judged on its own capture. It holds the /v1/url of the
    # cold line (the warm contracts count one of their own), so it carries the
    # EMT-4199 rule that /v1/url sends no hardware_id. Its launch has no link,
    # so its open carries no link_data.
    "link_generation": {
        "counts": {"/v3/deeplink": 1, "/v3/events/open": 1, "/v1/url": 1},
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {"/v1/url": {"hardware_id": 0}, "/v3/events/open": {"link_data": 0}},
    },
    # hot_uriScheme: a scheme link delivered via onNewIntent while MainActivity is already
    # RESUMED, no backgrounding step. The counts come from a local run of
    # H2HotUriSchemeWireTest at 4652bf06 (2026-10-09, an API 30 emulator), judged on the
    # delivery alone: the driver moves the launch capture aside once the bare launch has
    # settled (WireScenarioDriver.setCaptureAside), so the log holds this scenario's own
    # requests, not the bare launch that precedes it. No CI emulator run of this scenario
    # exists yet (H2 is not in the CI gate), so the fixture is that local run, real
    # requests with placeholders.
    #
    # The delivery window must not restart the activity, which is what /v3/events/custom
    # at 0 holds: the warm scenarios send two because they background and foreground the
    # app. The open must be a returning device's (randomized_bundle_token) and must carry
    # the scheme link (external_intent_uri).
    #
    # An https link sends external_intent_uri as well as android_app_link_url, so
    # external_intent_uri alone cannot tell the two deliveries apart. A scheme link
    # carries only external_intent_uri, which is why android_app_link_url is held at 0.
    "hot_uriScheme": {
        "counts": {
            "/v3/deeplink": 1,
            "/v3/events/open": 1,
            "/v3/events/custom": 0,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            "/v3/deeplink": {"android_app_link_url": 0, "external_intent_uri": 1},
            "/v3/events/open": {"randomized_bundle_token": 1, "external_intent_uri": 1},
        },
    },
    # The install-referrer scenarios (scripts/run_l1_referrer.sh): a fresh install whose Play install
    # referrer holds a link click id, the app at attribution level NONE, the steps below, then consent.
    # Measured on an AOSP API 34 emulator with Branch-SDK-TestPlayStore.
    "none_referrer_install": INSTALL_REFERRER_LINK_CONTRACT,
    "none_referrer_install_reduced": INSTALL_REFERRER_LINK_CONTRACT,
    # A redirect into the app calls requestDeepLinkData with a non-Branch URI, and a relaunch after the
    # process ends calls it with no link: at NONE neither sends anything.
    "none_referrer_browser_return": INSTALL_REFERRER_LINK_CONTRACT,
    "none_referrer_background_death": INSTALL_REFERRER_LINK_CONTRACT,
    "none_referrer_foreground_death": INSTALL_REFERRER_LINK_CONTRACT,
    # The app passes its intent's data, a null Uri on a launcher start, so the relaunch reads the saved
    # click id: one /v3/deeplink in all means it was not sent again.
    "none_referrer_foreground_death_uri_call": INSTALL_REFERRER_LINK_CONTRACT,
    # The first launch is a URI carrying the referrer's click id, as an app can pass it itself.
    "none_referrer_app_passes_click_id": INSTALL_REFERRER_LINK_CONTRACT,
    # A second Branch link opens the installed app at NONE: its own /v3/deeplink, and the open carries it.
    "none_referrer_second_link": {
        "counts": {"/v3/deeplink": 2, "/v3/events/open": 1, "/v1/url": 0},
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            "/v3/deeplink": {
                **NONE_DEEPLINK_ABSENT, "link_identifier": 1, "android_app_link_url": 1, "tracking_disabled": 2,
            },
            "/v3/events/open": INSTALL_REFERRER_LINK_CONTRACT["fields"]["/v3/events/open"],
        },
    },
    # The referrer holds no click id: nothing goes out at NONE, and the open carries no link.
    "none_organic_install": {
        "counts": {"/v3/deeplink": 0, "/v3/events/open": 1, "/v1/url": 0},
        "order": (),
        "fields": {
            "/v3/events/open": {
                "link_data": 0, "link_identifier": 0, "link_click_id": 0,
                "install_referrer_extras": 1, "app_store": 1, "tracking_disabled": 0,
            },
        },
    },
    # cold_uriScheme: a branchtest:// link starts the app on a device that already has it. The
    # wire is asserted, not resolution: the server answers a scheme link_click_id with
    # +non_branch_link. android_app_link_url at 0 is what separates it from an https delivery.
    # /v3/events/custom is not counted, as in cold_https.
    "cold_uriScheme": {
        "counts": {
            "/v3/deeplink": 1,
            "/v3/events/open": 1,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {
            "/v3/deeplink": {
                "android_app_link_url": 0,
                "external_intent_uri": 1,
                "link_identifier": 1,
            },
        },
    },
}
