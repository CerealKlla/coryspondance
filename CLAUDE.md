# Coryspondance

In-game mail mod for a modular Minecraft project (Minecraft Java Edition). Part of a suite of
intercommunicating mods that expose public APIs so other mods — the user's own and third parties' —
can integrate.

## Context directory — read this first

`context/` is a **separate private repo** (https://github.com/CerealKlla/coryspondance-context), not
part of this one — it's listed in `.gitignore` here and must never be committed to this repo. It's
cloned as a subdirectory at `context/` for local convenience. If this directory is missing (e.g. a
fresh clone of just this repo), restore it with:

```
git clone https://github.com/CerealKlla/coryspondance-context.git context
```

Before searching source for architecture, ownership boundaries, API shape, or "why does this work
this way," check `context/` first:

- `context/design-document.md` — the authoritative design spec. Start here for anything about
  intended shape or scope.
- `context/decisions.md` — dated log of decisions made during implementation that extend or
  override the design document, with rationale.
- `context/classes/` — one short markdown file per implemented class.

**Keep this system current as you work** — same discipline as every other mod in this suite: update
design-document.md/decisions.md when a decision is made, update classes/ when a class's public
surface changes, commit+push context/ separately from this repo's own commits.

## Status

Scaffolded 2026-10-12 — standalone mod, mirroring Kyt's own project shape:
- Loader: **NeoForge**, Minecraft **26.1.2**, Java **25**
- Group ID: `com.github.cerealklla.coryspondance` / Mod ID: `coryspondance`
- No required sibling-mod dependencies. Other mods (starting with Myssions' bug-report reward
  feature) depend on Coryspondance via the suite's usual `compileOnly` sibling-jar pattern.
- **Explicit user spec**: "endpoints for other mods to call... facilitates a 'mailing' mechanic...
  mostly to ensure that the player is online to receive something." `api.Coryspondance` is the real
  reason this exists as its own mod rather than living inside whichever mod needed it first.

**First slice built 2026-10-12** (see [context/decisions.md](context/decisions.md)) — the full spec
in one pass: a `mail.Letter` record (to/from, message, up to 9 item attachments, `hideSender`/
`allowReply`/`deleteWhenFullyClaimed` sender options), a per-player `PlayerMail` synced attachment,
offline delivery via the `FakePlayer`/`PlayerDataStorage` pattern (mirrors Lyfe's own
`api.Lyfe#addXp(MinecraftServer, UUID, ...)` precedent exactly), the public `api.Coryspondance`
facade, and a full player-facing loop: `N` keybind opens `client.MailboxScreen` (a synced-attachment
list/detail pane, same shape as Myssions' Quest Log), "Claim Items" opens a real 9-slot
`mail.LetterMenu` (a `ChestMenu` subclass with a Take All button, same shape as Yconomics'
`bag.LootBagMenu`), and "Write Letter"/"Reply" open `client.ComposeMailScreen` (a 9-slot
`mail.ComposeMailMenu` the player drags their own items into, plus a recipient name box and message
box) that sends via `net.SendLetterPayload`. `/coryspondance givetest` exercises the whole pipeline
without needing another mod to call the API yet.

`./gradlew build` green, whole-suite (9 mods) rebuild + `runServer` boot-smoke-test clean, plus a
real `runClient` boot (reached the dev world, rendering chunks, zero exceptions) confirming no
client-side registration crash. **Never actually opened/clicked through in a live client** — the
keybind, Mailbox list/detail rendering, claim flow, and compose flow are all unverified in practice
beyond "it boots."

**Known v1 gaps, flagged not forgotten** (see [context/decisions.md](context/decisions.md)):
- Compose can only address an online recipient by exact name — no offline-player name→UUID lookup
  exists anywhere in this suite yet.
- No expiration/auto-return-to-sender for long-unclaimed mail.
- No "Sent" folder or delivery confirmation for the sender.
- Reply only pre-fills the recipient name in Compose — no real threading.

See [context/classes/](context/classes/) for per-class reference.
