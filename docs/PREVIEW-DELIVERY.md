# Preview delivery policy

Mark requested faster installable APKs on 15 September 2026 and prefers to test new features on his Shield while development continues.

## Default: preview

The Preview workflow defaults to `preview` on personal-branch pushes and manual dispatch. It builds **one development/debug APK**, with the existing isolated Preview package and pinned signing identity. It skips the unit suite, lint and release optimisation. It still verifies compilation, packaging, signing, installed-app launch with Preview enabled, and a same-APK reinstall/restart preserving that preference. This is not a previous-version migration test.

These APKs may be larger and have development logging. Their performance and release-shrinker behaviour are not equivalent to a fully optimised release. Keep normal version-code increments for future app changes so Mark can update his installation.

## Milestones: full

Select `validation: full` manually when a feature milestone is ready, before calling a build feature-complete/stable, or when changes to playback, storage/deletion, migrations, signing, dependencies or startup warrant broader checks. This retains the existing selected unit suite, lint, classic and Preview startup/restart, optimised release build and diagnostic-to-release upgrade/restart checks. Add focused tests for the changed risky behaviour where the existing suite does not cover it.

The retained suite is the project's selected regression suite, not every possible test. Physical Shield acceptance is separate and must be reported accurately.

## Avoid repeated work

- Finish each coherent feature or UI batch before starting a delivery build. Do not rebuild for every small cosmetic adjustment.
- Use existing test results and source inspection where appropriate; reserve populated screenshot review for milestones or concrete layout faults.
- A newer commit cancels a superseded Preview workflow. Documentation-only pushes do not build an APK.
- The legacy non-Preview workflow no longer duplicates personal-branch push/PR builds.
- Deliver the successfully checked APK promptly with concise changes, known gaps, validation mode and checksum. Full release optimisation is not a prerequisite for development delivery.
- Preserve the cumulative release notes. The previous 3.5 release was fully checked; this policy applies to subsequent development builds.
- No speed target has been measured for this new workflow yet.
