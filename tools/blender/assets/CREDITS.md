# Third-party assets

All from [Poly Haven](https://polyhaven.com), all **CC0**: usable and
redistributable with no attribution condition. Listed anyway.

Fetched at 1k with `fetch_assets.py`. Materials are replaced on append
with the flat palette in `look.py`, so only alpha maps are kept.

- **potted_plant_01** — Poly Haven, CC0 — https://polyhaven.com/a/potted_plant_01
- **potted_plant_02** — Poly Haven, CC0 — https://polyhaven.com/a/potted_plant_02
- **potted_plant_04** — Poly Haven, CC0 — https://polyhaven.com/a/potted_plant_04
- **calathea_orbifolia_01** — Poly Haven, CC0 — https://polyhaven.com/a/calathea_orbifolia_01
- **pachira_aquatica_01** — Poly Haven, CC0 — https://polyhaven.com/a/pachira_aquatica_01
- **desk_lamp_arm_01** — Poly Haven, CC0 — https://polyhaven.com/a/desk_lamp_arm_01
- **root_cluster_01** — Poly Haven, CC0 — https://polyhaven.com/a/root_cluster_01
- **book_encyclopedia_set_01** — Poly Haven, CC0 — https://polyhaven.com/a/book_encyclopedia_set_01
- **tea_set_01** — Poly Haven, CC0 — https://polyhaven.com/a/tea_set_01
- **wooden_spoon** — Poly Haven, CC0 — https://polyhaven.com/a/wooden_spoon
- **planter_pot_clay** — Poly Haven, CC0 — https://polyhaven.com/a/planter_pot_clay
- **trowel_01** — Poly Haven, CC0 — https://polyhaven.com/a/trowel_01
- **small_empty_room_1** (HDRI, 1k) — Poly Haven, CC0 — https://polyhaven.com/a/small_empty_room_1

## Generated, not fetched

Made with [Meshy](https://meshy.ai) on the repo owner's own account, 2026-10-05.
These are **not CC0** and are not Poly Haven's: they are covered by Meshy's terms
for the account that generated them, so they are listed here for provenance
rather than offered for redistribution. Like everything else in this directory
they are gitignored, so a fresh clone has to regenerate or re-download them.

Each file is a whole small scene rather than one prop, and the parts are fused,
so they are cut up by script rather than by hand.

- **meshy-hand-pot.glb** — pot, plant, hand and forearm, a mug, a phone.
  The hand is the only part used. `extract_hand.py` cuts it out and writes
  `hand.glb`; that derived file is also gitignored, so run the script to rebuild
  it. Everything else in the scene is deliberately discarded, because the pots
  and plants are built procedurally and are identical across all seven shots,
  and the complaint that started this work was that they differed plate to plate.
- **meshy-watering.glb** — a watering can, a book stack and five pots. Unused.
- **meshy-roots.glb** — a dense root mass in four parts. Unused.
- **meshy-desk.glb** — a mug and a phone, the cleanest of the set at 10.7k verts.
  Unused.
- **meshy-plantcare.glb** — a mug with a handle, a phone, a pot on a tray. Unused.
- **meshy-tending.glb** — a second hand and two pots. Unused.
- **meshy-log.glb** — a large flat sheet and a small book stack. Unused.
