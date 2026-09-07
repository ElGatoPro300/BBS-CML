# UI redesign branch — migration plan

Planning rules for the local branch that **redesigns CML UI from a BBS FS ~1.10 baseline**, then reintroduces and optimizes current CML features.

**Companion:** structural patterns in [`code-organization.md`](code-organization.md).  
**Do not** auto-port commits between version branches unless explicitly asked (see `.cursor/rules/PROJECT-STRUCTURE.mdc` → Porting).

---

## 1. Goal of the redesign branch

| Phase | Focus | Not the focus |
|-------|--------|----------------|
| **A — Foundation** | Clean UI shell, dock/layout, panel orchestration, shared framework | Feature parity with current CML `master` |
| **B — Infrastructure** | Values, keybinds, registration events, film controller split, undo | Every CML clip/toolbar quirk |
| **C — Feature migration** | Reintroduce CML features **adapted** to the new structure | Paste old god-files unchanged |
| **D — Parity / polish** | Gates off, docs, in-game pass | Drive-by refactors unrelated to the migrated feature |

**North star:** the redesigned tree stays as maintainable as (or better than) the FS-inspired base **after** CML features land — not only before.

---

## 2. Sources of truth

| Source | Role |
|--------|------|
| Redesign branch (FS ~1.10–based UI) | Target architecture and layout UX |
| Current CML (`master` / production branch) | Feature behavior, data model, addons, gates to preserve |
| [`code-organization.md`](code-organization.md) | How to place new code |
| Area deep-dives (`forms.md`, `actor-mode.md`, …) | Semantics before changing that subsystem |
| `CONTRIBUTING.md` | Style (`this.`, braces, `/* */`, method length, no pure AI slop) |

When FS baseline and CML disagree on **product** behavior, prefer CML’s intended behavior unless the redesign explicitly changes UX. When they disagree on **structure**, prefer the redesign’s orchestration patterns.

---

## 3. Migration order

Do not migrate by “largest file first.” Prefer dependency order:

1. **Shared UI framework** — `UIElement`, overlays, `UIDockLayout` / `ILayoutSource` (or redesign equivalent), theme/localization hooks.
2. **Registration & settings** — keep event-based register APIs; `BBSSettings` / layout settings; `BBSFeatures` gates.
3. **Film shell** — `UIFilmPanel` as orchestrator only; preview, recorder, selection/home as separate types.
4. **Replays editor** — list/properties panels; utils for sheets/gizmos/picking; batch processors as pure logic.
5. **Camera / action clips UI** — clip panels + renderers registry; then CML-only clip types.
6. **Forms / ModelForm / renderers** — data + editor panels; split render passes when porting glow/soft-limb/etc.
7. **Actor Mode, onion, puppeteer** — read `actor-mode.md` first; stub vs entity must not collapse.
8. **Timeline toolbar & home/cross-world** — CML product layers as **packages**, not inline film-panel layout.
9. **Remaining CML packages** — addons UI, triggers, news, simulation, video/screen clips, etc.
10. **Hardening** — Iris/shadow passes, export/FPS (`FILM_FRAME_TIMELINE.md`), localization sweep.

Finish each step’s “definition of done” (below) before stacking the next large feature set on a half-migrated shell.

---

## 4. Definition of done (per feature)

A feature counts as migrated only if **all** applicable items hold:

### Structure

- [ ] Editor panel is an orchestrator; no new parallel dock/splitter implementation.
- [ ] Non-UI logic lives in utils / passes / processors (see `code-organization.md`).
- [ ] No method left grossly over ~150 LOC without a concrete reason.
- [ ] Client/server split respected (`src/client` vs `src/main`).

### Product contracts

- [ ] User-facing strings use `IKey` / `UIKeys` (no hardcoded UI copy).
- [ ] Persistent fields use the `Value` system.
- [ ] Registration goes through existing events / addon hooks where CML already has them.
- [ ] Unfinished UI is behind `BBSFeatures` (or redesign equivalent), default off.

### Correctness

- [ ] Behavior matched against current CML (or intentional redesign delta written down in the PR/commit message).
- [ ] Actor Mode / Iris / timeline rules checked when the feature touches those areas.
- [ ] Smoke-tested in-game (`./gradlew runClient`) for the happy path + one regression risk.

### Docs

- [ ] If the feature invents a new pattern, update the relevant `docs/architecture/*.md` (or this file’s order/checklist).
- [ ] Do not leave “temporary” copies of old CML classes “until later” without a gate or ticket note.

---

## 5. Preserve from current CML

Bring these across **even if** FS 1.10 lacked them:

* Event-driven registration and addon surface (`docs/ADDONS.md`)
* `BBSFeatures` (or successor) for incomplete work
* Actor Mode semantics (`docs/architecture/actor-mode.md`)
* Timeline / export frame model (`docs/FILM_FRAME_TIMELINE.md`)
* CML-only product: timeline toolbar, home/document tabs, cross-world film browser, extra clips/effects — **as isolated modules**
* Localization discipline and architecture deep-dives

---

## 6. Do not copy blindly

| From | Avoid |
|------|--------|
| Current CML | Pasting `UIFilmPanel` / `UIReplaysEditor` / `ModelFormRenderer` as monolithic files |
| Current CML | Reintroducing inline film layout when the redesign already has dock layout |
| FS baseline | Dropping event registration / addon hooks to match FS’s thinner `BBSMod` |
| FS baseline | Replacing CML IK/limb/opacity stacks wholesale without an explicit decision |
| Either | “Feature parity” patches that bypass `Value`, `IKey`, or client/server split |
| Either | Porting unrelated version branches or gradle churn “while we’re here” |

---

## 7. Feature migration checklist (copy per feature)

Use in PRs or commit descriptions on the redesign branch:

```text
Feature:
Source reference (CML path / commit):
Target package(s):

[ ] Read area deep-dive(s):
[ ] Data / Value model ported or confirmed compatible
[ ] UI orchestrator + extracted helpers (no god-file dump)
[ ] Renderer / Iris notes checked (if any)
[ ] Network / server bits in src/main (if any)
[ ] Registration events / BBSFeatures gate
[ ] IKey strings added
[ ] In-game smoke test
[ ] Intentional behavior deltas noted
```

---

## 8. Anti-patterns

1. **Paste then “clean later”** — later rarely comes; extract on the way in.
2. **Second layout engine** — any new splitter/tab system beside the redesign dock is a reject.
3. **Utils as decoration** — extracting a class without moving call sites does not count.
4. **Parity theater** — matching pixels/LOC of old CML without matching structure goals.
5. **Silent product drops** — removing toolbar/home/actor behavior without documenting the decision.
6. **Gate-free WIP** — shipping half-wired panels visible to users.
7. **Cross-branch drive-by** — migrating features does not imply merging redesign back to `master` or porting to other MC versions unless asked.

---

## 9. Agent / contributor workflow

When the user asks to migrate or reimplement a CML feature onto the redesign branch:

1. Confirm the target is the redesign branch (do not invent ports to other version branches).
2. Read this file + [`code-organization.md`](code-organization.md) + the area deep-dive.
3. Prefer implementing against the new shell; use current CML as behavioral reference.
4. Refuse (or push back on) dumping old monolinks; propose the extraction boundary first if unclear.
5. Keep changes scoped to the feature; no gradle edits (`CONTRIBUTING.md`).

---

## Related

* [`code-organization.md`](code-organization.md)
* [`ui-framework.md`](ui-framework.md)
* [`how-to-ui-element.md`](how-to-ui-element.md), [`how-to-form.md`](how-to-form.md), [`how-to-clip.md`](how-to-clip.md)
* `.cursor/rules/ui-redesign-migration.mdc` — short agent reminder
* External reference fork: [Wemppy4/bbs-fs](https://github.com/Wemppy4/bbs-fs) (organization inspiration; baseline for the redesign is ~FS 1.10, not necessarily latest `master`)
