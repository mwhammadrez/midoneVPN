# sample-android-project

A deliberately minimal, from-scratch multi-module skeleton — not a real
product, not derived from any company codebase. It exists for two
reasons:

1. To give `docs/getting-started.md` and the skills something concrete to
   point at (the `feature/greeting` module is a working, if tiny,
   illustration of `android-architecture`'s `presentation`/`domain`/`data`
   split and `compose-development`'s state-hoisting pattern).
2. To act as a throwaway target for smoke-testing `install.sh`/`update.sh`
   (see the repository root's implementation notes).

This skeleton has not been verified against a real Android SDK/AGP build
in this environment (no Android SDK is installed here) — treat the Gradle
files as structurally representative, not as a guarantee they build
as-is on every AGP/Kotlin version. Point them at your team's actual
version catalog when using this as a real starting point.

## Structure

```
app/                          minimal application module, wires the feature in
feature/greeting/domain/      GreetingRepository interface, GetGreetingUseCase - no Android dependency
feature/greeting/data/        GreetingRepositoryImpl - in-memory, stands in for a real data source
feature/greeting/presentation/ GreetingViewModel + GreetingScreen (Compose)
```

`domain` has zero Android SDK imports — check it yourself:
`grep -r "import android" feature/greeting/domain/src` should return
nothing.
