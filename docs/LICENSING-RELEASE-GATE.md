# Native Runtime Licensing — Release Gate

**Status: unresolved; do not treat the native runtime as cleared for public binary distribution.**

## PRoot fork grant

The bundled PRoot source is the Termux fork at `v5.1.107.102`. The Termux package recipe pinned by the build script declares `GPL-2.0`, while the upstream PRoot README states `GPL-2.0-or-later`. The fork's bundled `COPYING` is the GPL version 2 text, but the license text itself does not establish whether every copyright holder granted “or later” rights.

Sources:
- Termux package recipe: https://github.com/termux/termux-packages/blob/bb4dce3e28fa5fbcca7d91fee5910373e559ed41/packages/proot/build.sh
- PRoot upstream license declaration: https://github.com/proot-me/proot
- Pinned source: https://github.com/termux/proot/tree/v5.1.107.102

Do not silently convert the Termux recipe's `GPL-2.0` metadata into `GPL-2.0-or-later`. Obtain evidence from the pinned fork's maintainers/copyright grant or choose a source revision whose grant is clear.

## talloc static-link question

The pinned talloc 2.5.0 source identifies the library as LGPL-3.0-or-later. FORGE-TS statically links talloc into the PRoot executable. LGPLv3 static-link distribution has relinking and corresponding-source obligations. More importantly, if the pinned PRoot fork is GPL-2.0-only, its combination with an LGPL-3.0-or-later library may be license-incompatible. If PRoot is validly GPL-2.0-or-later, a GPLv3 path may be available, but the exact source grant and distribution structure still require review.

Do not claim this combination is cleared based only on a package recipe or a copied license text.

## Current repository evidence

- PRoot source archive and SHA-256 are pinned in `scripts/native/build-proot-runtime.sh`.
- talloc source archive and SHA-256 are pinned in the same build script.
- PRoot and talloc license texts are stored under `app/src/main/assets/licenses/proot-runtime/`.
- The build script is available to reproduce the native build.
- The repository does not yet package a complete, release-ready relinking kit with all required object files/application code materials.

## Required before public release

1. Confirm the exact PRoot license grant for the pinned Termux fork.
2. Have the PRoot/talloc static-link combination reviewed for compatibility.
3. If distributing the static-linked executable under LGPLv3, prepare and ship the required relinking materials and corresponding source.
4. Record the final decision and exact license identifiers in `ATTRIBUTIONS.md` and the release bundle.
5. Until these items are resolved, mark PRoot-enabled APK distribution as not cleared.
