# Working agreements (master)

> Contributor-facing notes for work on this branch. This is not a public contributing guide.
> Last updated 2026-09-03, against `master` at 827655ea.

## Branches and commits

`master` is the base branch and is never committed to directly. Branch, push, open a PR.

There is no enforced branch-naming convention. History carries author-prefixed (`gdeluna/...`), ticket-key-first (`SDK-1234-...`), and type-prefixed (`fix/...`) names side by side. Pick one and move on.

The PR title must reference the ticket. This is a reviewer-checklist item in `.github/pull_request_template.md`, not a style preference. Use your own key, never a teammate's: Jira auto-links any key it sees, so naming their ticket attaches your work to their scope. When another ticket explains why a change is needed, reference the PR number in the body instead (`#1401`), which carries the same information without auto-linking.

Commit subject formats vary across the history and none is enforced. Commit message bodies wrap at 72 columns, because git renders them in a terminal.

## Pull requests

Fill in `.github/pull_request_template.md`: reference, description, testing instructions, risk assessment.

**Request a reviewer, and verify it took.** `.github/CODEOWNERS` is `* @BranchMetrics/sdk-maintainers`, but the request is not reliably created for you. An unrequested PR sits invisible while its author believes it is queued.

```bash
gh pr view <n> --json reviewRequests -q '[.reviewRequests[]?.name, .reviewRequests[]?.login] | map(select(.)) | join(",")'
gh pr edit <n> --add-reviewer BranchMetrics/sdk-maintainers
```

An empty result is the defect. Re-check after any base-branch change, since retargeting can drop the request.

Keep the PR body to the defect, the fix, the proof it works, and what changed after a review round.

### Reviewing someone else's PR

A review comment carries the defect and the fix, not the investigation. One or two sentences per line comment, and a `suggestion` block instead of prose whenever the fix is code. Say what the code does wrong, where, and what closes it.

## Code style and scope

- **AOSP style.** Match the surrounding file even when you would write it differently.
- **Comments in public API earn their place.** What the function does, and what the parameters mean. Nothing else. Public doc comments render into the generated docs, so anything a consumer of the SDK would not act on does not go there.
- **Surgical changes**, so the diff stays reviewable. Every changed line traces to the request. Do not refactor adjacent code, do not fix pre-existing lint in a file you happen to touch, do not rename existing symbols to fit new code. Note anything you spotted in passing in the PR body under "Out of scope".
- **Clean up only your own orphans.** Remove imports and locals *your* change made unused. Leave pre-existing dead code alone unless asked.

## Splitting work

When a change has two separable halves, ship two stacked PRs titled `(1 of 2)` and `(2 of 2)`. A small PR gets picked up in a gap between meetings; a large one waits for an uninterrupted slot that may not come, and while it waits it drifts and collects conflicts.

Split at a seam that already exists: measurement before assertion, contract before driver, driver before CI wiring, mechanism before adoption. The one hard constraint is that **each piece must be verifiable on its own**. A contract with no test, a helper with no caller, a migration with no green build are fragments, not deliverables. When a seam would produce an unverifiable piece, keep the halves together and say why in the PR body.

## Releases

- Version lives in `gradle.properties`: `VERSION_NAME` and `VERSION_CODE`.
- Every user-visible change gets a `ChangeLog.md` entry under the new version heading.
- Release branches are named `Release-*`, which is what triggers `gptdriverautomation.yaml`.
- Published to Maven Central as `io.branch.sdk.android:library`. `./gradlew publishToMavenLocal` to consume a build before it ships.

## Public surfaces

This is a public repository. Audit what a change *publishes*, not just what it changes. Internal ticket keys, tracker URLs, internal hostnames, and colleague names must not reach the README, the integration docs, CI step summaries (`$GITHUB_STEP_SUMMARY`), workflow logs, build artifacts, or badge links. An `echo` inside a workflow reads like code and behaves like publishing.

Ticket keys are fine in commit subjects, branch names, PR titles, and PR bodies. Those are the repo's convention. The line is drawn at rendered product surfaces.

Security issues go to security@branch.io, never a public issue.
