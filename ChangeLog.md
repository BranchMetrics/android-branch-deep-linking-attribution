Branch Android SDK change log

- v1.5.8: Fix automated session management. Changes added to block multiple Activity life cycle listeners getting registered.

- v1.5.7: Added automated session management. Getting rid of session handling with each Activity's life cycle methods.

- v1.5.6: Add limit to queue retrieval to prevent legacy stack overflow ghosts.

- v1.5.5: Fix null server request tag bug.

- v1.5.4: Fix concurrent modification bug in ServerRequestQueue. Some code was added where there was rare possibility of modifying the ServerRequest body at the same time we were iterating through it.

- v1.5.3: Simplified branch_key setup by directly specifying it in Manifest.xml

- v1.5.2: Added retry count to all queries + a little better handling of retries

- v1.5.1: Added Javadocs for easy explanation of all methods

- v1.5.0: We have deprecated the use of `bnc_app_key` and are now using `branch_key`, which can be obtained in the settings page of your Dashboard. The replacement in the SDK should happen in the manifest and strings.xml (see README for details), as well as in `public static Branch getInstance(Context context, String branchKey)` if necessary.

- v1.4.5: Better handling of intent data for state transitions from Facebook's AppLinks

- v1.4.4: Fixed potential issue where update state was improperly set if no isReferrable specified

- v1.4.3: Added new update state to differentiate between update and open

- v1.4.2.2: Bullet proof prevention of duplicate callClose (reported StackOverflow error)

- v1.4.2.1: Catch OOM during parsing public dir

- v1.4.2: Added Unit tests

- v1.4.1: Fixed synchronization issue in persistence queue

- v1.4.0: Exposed duration in getShortUrl for tuning link click match duration

- v1.3.9: Added API Key to GET

- v1.3.8: Check memory when getting URI Scheme

- v1.3.7: Added app listing

- v1.3.6: Fix pre Honeycomb (3.0) bug

- v1.3.5: Added advertising id to init params (optional)

- v1.3.4: Added optional advertising id to install params

- v1.3.3: Fixed issue with null tags

- v1.3.2: Added API's to getShortURL synchronously

- v1.3.1: Enforce setting app key before any API call; Provided ability to turn off smart session

- v1.3.0: Added setDebug call to enable logging and use a random hardware ID (helpful for referral testings). Also, cacheing of links to save on network requests

- v1.2.9: Moved app key to strings.xml; Added constants for OG tags and redirect URLs

- v1.2.8: Fixed close issue due to rotation

- v1.2.7: Check if URI is hierarchical

- v1.2.6: Handle not init-ed case

- v1.2.5: Proper debug connection

- v1.2.4: Fixed rare race condition

- v1.2.3: Added BranchError to callbacks

- v1.2.2: Added Branch remote debug feature

- v1.2.1: Proper network callbacks; Added query limit

- v1.2.0: Cleanup semaphore issue

- v1.1.9: Fixed request before init issue

- v1.1.8: Added link alias