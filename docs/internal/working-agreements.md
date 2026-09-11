# Working agreements (6.0.0-beta.0)

> Contributor-facing notes for work on this branch. This is not a public contributing guide.
> Last updated 2026-09-03, against `6.0.0-beta.0` at 88b3bf28.

## Branches and commits

`6.0.0-beta.0` is the base branch for this line and is never committed to directly. Branch, push, open a PR against it. Do not target `master` by accident: the two lines have different architectures, so a cross-line merge is not a conflict, it is a regression.

There is no enforced branch-naming convention. History carries author-prefixed (`gdeluna/...`), ticket-key-first (`SDK-1234-...`), and type-prefixed (`fix/...`) names side by side. Pick one and move on.

The PR title must reference the ticket. This is a reviewer-checklist item in `.github/pull_request_template.md`, not a style preference. Use your own key, never a teammate's: Jira auto-links any key it sees, so naming their ticket attaches your work to their scope. When another ticket explains why a change is needed, reference the PR number in the body instead (`#1401`), which carries the same information without auto-linking.

Commit subject formats vary across the history and none is enforced. Commit message bodies wrap at 72 columns, because git renders them in a terminal.

## What this branch allows that `master` does not

This is an internal beta. Breaking changes and clean API surfaces are acceptable, and intentional public-API changes are expected. **Do not add backward-compatibility shims unless explicitly asked.** When a reviewer's suggestion trims a check you added, the reason is usually that the concept is being retired; apply it rather than defending the line.

## Pull requests

Fill in `.github/pull_request_template.md`: reference, description, testing instructions, risk assessment.

**Request a reviewer explicitly.** This branch has no `.github/CODEOWNERS`, so nothing requests one for you. An unrequested PR sits invisible while its author believes it is queued.

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
- **Comments in public API earn their place.** What the function does, and what the parameters mean. Nothing else. Public doc comments render into the generated docs.
- **Surgical changes**, so the diff stays reviewable. Every changed line traces to the request. Do not refactor adjacent code, do not fix pre-existing lint in a file you happen to touch, do not rename existing symbols to fit new code. Note anything you spotted in passing in the PR body under "Out of scope".
- **Clean up only your own orphans.** Remove imports and locals *your* change made unused. Leave pre-existing dead code alone unless asked.

## Splitting work

When a change has two separable halves, ship two stacked PRs titled `(1 of 2)` and `(2 of 2)`. A small PR gets picked up in a gap between meetings; a large one waits for an uninterrupted slot that may not come, and while it waits it drifts and collects conflicts.

Split at a seam that already exists: measurement before assertion, contract before driver, driver before CI wiring, mechanism before adoption. The one hard constraint is that **each piece must be verifiable on its own**. A contract with no test, a helper with no caller, a migration with no green build are fragments, not deliverables.

## Versioning

`gradle.properties` reads `VERSION_NAME=5.20.999`, a pre-6.0 placeholder that does not track this branch. Do not identify the branch by its version string, and do not treat a version bump here as a release. This line does not publish to Maven Central yet. `./gradlew publishToMavenLocal` to consume a local build.

## Public surfaces

This is a public repository. Audit what a change *publishes*, not just what it changes. Internal ticket keys, tracker URLs, internal hostnames, and colleague names must not reach the README, the integration docs, CI step summaries (`$GITHUB_STEP_SUMMARY`), workflow logs, build artifacts, or badge links.

Ticket keys are fine in commit subjects, branch names, PR titles, and PR bodies. The line is drawn at rendered product surfaces.

Security issues go to security@branch.io, never a public issue.
