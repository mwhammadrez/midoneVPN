# Security

This document covers the security posture of **this repository** (a
collection of markdown skills/workflows and two shell scripts), not of any
consuming Android application.

## Threat model

`install.sh` and `update.sh` run with the installing developer's local
shell privileges and write files into a target repository. Even though
they are first-party scripts, treat them like any other supply-chain
script: read them before running them, and prefer running them from a
pinned tag rather than a floating branch.

**Guarantee:** these scripts only ever read this repository's own tracked
files and copy static text/markdown into the target repo. They never
execute, `eval`, or `curl | bash` anything — fetched or otherwise. If a
future change to either script needs to run code, that is a breaking
change to this guarantee and must be called out explicitly in
`CHANGELOG.md` under a MAJOR bump.

## No secrets, ever

Skill and workflow content must never contain real API keys, tokens,
certificates, internal hostnames, credentials, or customer/product data.
`android-security` documents *how* to handle secrets in an Android app;
it must never *contain* one. This is enforced two ways:

1. **Convention** — every contributor is responsible for not pasting real
   values into examples. Use obviously-fake placeholders
   (`sk_live_examplePlaceholder`, `https://api.example.internal`).
2. **CI check** — `scripts/validate-skills.sh` runs a best-effort regex
   scan for common secret shapes (AWS-style access keys, generic
   `api[_-]?key` assignments, PEM private-key headers, bearer-token-shaped
   strings) on every pull request. This is a cheap first line of defense,
   **not** a substitute for a real secret scanner (e.g. gitleaks or
   trufflehog) — adopting one of those for this repo is a reasonable
   future addition, deliberately left out of v0.1 to keep the CI surface
   small.

## Third-party skills, plugins, and MCP servers

Any external Agent Skill, editor plugin, or MCP server is an executable
supply-chain dependency, exactly like an npm package. Before a team adds
one — to this repository or to a consuming project — go through this
checklist:

- [ ] Read the source. Don't install from a description alone.
- [ ] Check what permissions/tool access it requests (filesystem, network,
      shell exec, credentials).
- [ ] Read any scripts it ships, especially install/post-install hooks.
- [ ] Check its network behavior where you can (what does it call out to,
      and why).
- [ ] Pin an exact version or commit — never track a floating branch.
- [ ] Refuse anything with an unpinned `curl | bash`-style install step.
- [ ] Decide and document how it will be updated (manual review each time,
      not auto-update).

This repository itself references two categories of optional third-party
content without vendoring or trusting them by default: generic
process-skill packs (e.g. Superpowers, Matt Pocock's skills) and
device-level Android testing/profiling plugins. See
`docs/authoring-skills.md` and §5 of the design history for the specific
reasoning. Nothing in this list is installed as part of `install.sh`.

## MCP: principle of least privilege

If a team connects an MCP server (GitHub, issue tracking, docs retrieval,
CI) to an agent using these workflows:

- Use scoped, non-production credentials. Never assume production
  credentials are available to an agent.
- Prefer read-only scopes unless a workflow specifically needs write
  access (e.g. opening a PR), and prefer a human-reviewed step before any
  write action lands.
- Never commit a token, `.env` file, or MCP server config containing a
  credential to this repository or to a consuming repository's
  `.agent-engineering/` directory.
- No MCP server configuration is bundled or committed by this repository
  in v0.1 (see `docs/architecture.md`) — connecting one is entirely a
  consuming team's choice, made in their own environment.

## Reporting a concern

If you find a security issue with this repository's content or scripts
(a secret that slipped through, an installer bug that could overwrite
unintended files, guidance that would lead to an insecure Android
pattern), open a GitHub issue, or a private security advisory if the
concern is sensitive enough that it shouldn't be public until fixed
(e.g. a real credential that slipped into history — see below).

## If a real secret ever slips into a commit

Removing it from the latest commit is not enough — it's still in git
history, and this is a public repository, so treat it as compromised
immediately: rotate the credential first, then clean the history
(`git filter-repo` or equivalent) as a follow-up. Don't wait on the
history cleanup before rotating.
