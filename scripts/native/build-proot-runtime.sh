#!/usr/bin/env bash
set -euo pipefail

# Reproducible ARM64 Android PRoot build for FORGE-TS.
# Sources and hashes follow the official Termux package recipes.
# libandroid-shmem is intentionally disabled: FORGE-TS does not require
# Termux shared-memory interoperability in the initial standalone runtime.

PROOT_VERSION="5.1.107.102"
PROOT_SHA256="30d12a6e2cc4147123756ff58edea262d3083bbc238c12d0ab9b39e3d1c40852"
TALLOC_VERSION="2.5.0"
TALLOC_SHA256="912afa237510ae542a7733998eb18a12bcda35ab6729c8e2ddb43e8d0ebab007"
ANDROID_API="${ANDROID_API:-29}"
ABI="arm64-v8a"
TARGET="aarch64-linux-android${ANDROID_API}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="${PROOT_OUTPUT:-$ROOT/build/proot-native}"
WORK="$OUT/work"
DOWNLOADS="$OUT/downloads"
STAGE="$OUT/artifact"
NDK="${ANDROID_NDK_HOME:-}"

if [[ -z "$NDK" && -n "${ANDROID_HOME:-}" ]]; then
  NDK="$ANDROID_HOME/ndk/27.2.12479018"
fi
if [[ ! -x "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang" ]]; then
  echo "Android NDK r27c not found. Set ANDROID_NDK_HOME." >&2
  exit 2
fi

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
CC="$TOOLCHAIN/$TARGET-clang"
AR="$TOOLCHAIN/llvm-ar"
RANLIB="$TOOLCHAIN/llvm-ranlib"
STRIP="$TOOLCHAIN/llvm-strip"
READELF="$TOOLCHAIN/llvm-readelf"
OBJCOPY="$TOOLCHAIN/llvm-objcopy"
OBJDUMP="$TOOLCHAIN/llvm-objdump"
for tool in "$CC" "$AR" "$RANLIB" "$STRIP" "$READELF" "$OBJCOPY" "$OBJDUMP"; do
  [[ -x "$tool" ]] || { echo "Missing NDK tool: $tool" >&2; exit 2; }
done

rm -rf "$WORK" "$STAGE"
mkdir -p "$WORK" "$DOWNLOADS" "$STAGE/$ABI" "$STAGE/licenses"
fetch_checked() {
  local url="$1" expected="$2" destination="$3"
  if [[ ! -f "$destination" ]]; then
    curl --fail --location --retry 3 --retry-all-errors "$url" -o "$destination"
  fi
  echo "$expected  $destination" | sha256sum --check --status || {
    echo "SHA-256 mismatch: $destination" >&2
    rm -f "$destination"
    exit 3
  }
}

PROOT_ZIP="$DOWNLOADS/proot-$PROOT_VERSION.zip"
TALLOC_TAR="$DOWNLOADS/talloc-$TALLOC_VERSION.tar.gz"
fetch_checked "https://github.com/termux/proot/archive/v$PROOT_VERSION.zip" "$PROOT_SHA256" "$PROOT_ZIP"
fetch_checked "https://www.samba.org/ftp/talloc/talloc-$TALLOC_VERSION.tar.gz" "$TALLOC_SHA256" "$TALLOC_TAR"

mkdir -p "$WORK/proot" "$WORK/talloc"
unzip -q "$PROOT_ZIP" -d "$WORK"
mv "$WORK/proot-$PROOT_VERSION" "$WORK/proot-src"
tar -xzf "$TALLOC_TAR" --strip-components=1 -C "$WORK/talloc"

# The pinned Android ashmem/memfd source uses strcmp/memset without string.h.
# Add the declaration header before compiling; no runtime logic is modified.
ASHMEM_SOURCE="$WORK/proot-src/src/extension/ashmem_memfd/ashmem_memfd.c"
sed -i '1i #include <string.h>' "$ASHMEM_SOURCE"
grep -q 'include <string.h>' "$ASHMEM_SOURCE"

# Build talloc using the same cross-answer profile used by the Termux package.
cat > "$WORK/talloc/cross-answers.txt" <<'ANSWERS'
Checking uname sysname type: "Linux"
Checking uname machine type: "dontcare"
Checking uname release type: "dontcare"
Checking uname version type: "dontcare"
Checking simple C program: OK
building library support: OK
Checking for large file support: OK
Checking for -D_FILE_OFFSET_BITS=64: OK
Checking for WORDS_BIGENDIAN: OK
Checking for C99 vsnprintf: OK
Checking for HAVE_SECURE_MKSTEMP: OK
rpath library support: OK
-Wl,--version-script support: FAIL
Checking correct behavior of strtoll: OK
Checking correct behavior of strptime: OK
Checking for HAVE_IFACE_GETIFADDRS: OK
Checking for HAVE_IFACE_IFCONF: OK
Checking for HAVE_IFACE_IFREQ: OK
Checking getconf LFS_CFLAGS: OK
Checking for large file support without additional flags: OK
Checking for working strptime: OK
Checking for HAVE_SHARED_MMAP: OK
Checking for HAVE_MREMAP: OK
Checking for HAVE_INCOHERENT_MMAP: OK
Checking getconf large file support flags work: OK
ANSWERS

(
  cd "$WORK/talloc"
  PATH="$TOOLCHAIN:$PATH" CC="$CC" AR="$AR" RANLIB="$RANLIB" \
    ./configure --prefix="$WORK/talloc-install" --disable-rpath --disable-python \
      --cross-compile --cross-answers=cross-answers.txt
  PATH="$TOOLCHAIN:$PATH" make -j"$(getconf _NPROCESSORS_ONLN)"
  mkdir -p "$WORK/talloc-install/lib" "$WORK/talloc-install/include"
  cd bin/default
  "$AR" rcsD "$WORK/talloc-install/lib/libtalloc.a" talloc*.o
  "$RANLIB" "$WORK/talloc-install/lib/libtalloc.a"
  test -s "$WORK/talloc-install/lib/libtalloc.a"
  cp "$WORK/talloc/talloc.h" "$WORK/talloc-install/include/"
)

# Build from the pinned, unmodified Termux PRoot source. The official
# GNUmakefile creates a separate, freestanding ELF loader when
# PROOT_UNBUNDLE_LOADER is set. No optional shmem library is linked.
(
  cd "$WORK/proot-src/src"
  CPPFLAGS="-DARG_MAX=131072 -DVERSION=\\\"$PROOT_VERSION\\\" -I$WORK/talloc-install/include" \
  CFLAGS="-O2 -fPIE -ffile-prefix-map=$ROOT=." \
  LDFLAGS="-L$WORK/talloc-install/lib -Wl,-z,noexecstack -Wl,-z,max-page-size=16384" \
  make -j"$(getconf _NPROCESSORS_ONLN)" proot loader/loader \
    CC="$CC" LD="$CC" STRIP="$STRIP" OBJCOPY="$OBJCOPY" OBJDUMP="$OBJDUMP" \
    GIT=false PROOT_UNBUNDLE_LOADER="/forge-ts/proot-loader"
  "$STRIP" --strip-unneeded -o "$STAGE/$ABI/libproot.so" proot
  "$STRIP" --strip-unneeded -o "$STAGE/$ABI/libproot_loader.so" loader/loader
)

chmod 0755 "$STAGE/$ABI/libproot.so" "$STAGE/$ABI/libproot_loader.so"
"$READELF" -h "$STAGE/$ABI/libproot.so" > "$STAGE/proot-elf-header.txt"
"$READELF" -l "$STAGE/$ABI/libproot.so" > "$STAGE/proot-elf-program-headers.txt"
"$READELF" -d "$STAGE/$ABI/libproot.so" > "$STAGE/proot-elf-dynamic.txt"
"$READELF" -h "$STAGE/$ABI/libproot_loader.so" > "$STAGE/loader-elf-header.txt"
"$READELF" -l "$STAGE/$ABI/libproot_loader.so" > "$STAGE/loader-elf-program-headers.txt"
grep -q "AArch64" "$STAGE/proot-elf-header.txt"
grep -q "AArch64" "$STAGE/loader-elf-header.txt"
grep -q "0x4000" "$STAGE/loader-elf-program-headers.txt" || {
  echo "PRoot loader is missing the expected 16 KB maximum page alignment." >&2
  exit 4
}
grep -q "0x4000" "$STAGE/proot-elf-program-headers.txt" || {
  echo "PRoot ELF is missing the expected 16 KB maximum page alignment." >&2
  exit 4
}
if grep -q "libtalloc\\.so" "$STAGE/proot-elf-dynamic.txt"; then
  echo "Unexpected dynamic libtalloc dependency." >&2
  exit 5
fi

cp "$WORK/proot-src/COPYING" "$STAGE/licenses/PRoot-COPYING"
if [[ -f "$WORK/talloc/COPYING" ]]; then
  cp "$WORK/talloc/COPYING" "$STAGE/licenses/talloc-COPYING"
elif [[ -f "$WORK/talloc/COPYING.LESSER" ]]; then
  cp "$WORK/talloc/COPYING.LESSER" "$STAGE/licenses/talloc-COPYING.LESSER"
fi
cat > "$STAGE/build-manifest.txt" <<MANIFEST
FORGE-TS PRoot native build
ABI=$ABI
ANDROID_API=$ANDROID_API
NDK_REVISION=27.2.12479018
PRoot_SOURCE=https://github.com/termux/proot/archive/v$PROOT_VERSION.zip
PRoot_SHA256=$PROOT_SHA256
PRoot_LICENSE=GPL-2.0 (Termux package recipe declaration; inspect bundled COPYING)
TALLOC_SOURCE=https://www.samba.org/ftp/talloc/talloc-$TALLOC_VERSION.tar.gz
TALLOC_SHA256=$TALLOC_SHA256
TALLOC_LICENSE=Bundled upstream COPYING file (Termux package recipe declares GPL-3.0)
LIBANDROID_SHMEM=disabled
TALLOC_LINKAGE=static
ELF_MAX_PAGE_SIZE=16384
PROOT_LOADER_MODE=external
SOURCE_PATCHES=ashmem_memfd-add-string.h
MANIFEST
(
  cd "$STAGE"
  find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS
)

echo "Built ARM64 PRoot payloads:"
ls -lh "$STAGE/$ABI"
echo "Artifact directory: $STAGE"
