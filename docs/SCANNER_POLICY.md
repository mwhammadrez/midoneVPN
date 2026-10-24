# Scanner policy

Status: **not implemented**. This file defines the contract the implementation must satisfy.

## Scope
Only endpoints the user explicitly provides (list or bounded range). No public-internet sweeping,
no CDN-range enumeration, no port scanning beyond the configured port.

## Consent
Scope and budgets are shown before any packet is sent. Scan never starts implicitly.

## Budgets
Targets ≤ 256, concurrency ≤ 8, per-target timeout ≤ 5 s, total duration ≤ 120 s, backoff on failure,
immediate cancellation, lifecycle-aware cleanup. Results cached with timestamp + network context + TTL.

## Test levels (reported separately, never merged)
1. TCP reachable
2. TLS handshake valid
3. Protocol handshake
4. Authenticated tunnel
5. Traffic through the tunnel (HTTPS)
6. Optional bounded throughput

## Ranking
Traffic success > stability > handshake time > latency > battery. Ping or TCP connect alone NEVER ranks.

## Safety
No credentials to unverified targets; no exploitation; no auth bypass; no unbounded redirect following.
