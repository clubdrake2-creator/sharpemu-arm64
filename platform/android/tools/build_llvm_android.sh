#!/usr/bin/env bash
# Compila o LLVM (estatico, target AArch64 APENAS) para Android arm64-v8a, para o
# x64-arm64-backend (src/core/cpu/jit/x64_arm64/ — ver README.md, regra R1).
#
# Pre-requisito: llvm-tblgen + llvm-min-tblgen nativos do host ja compilados
# (TBLGEN_DIR abaixo). O resultado e instalado em externals/llvm-android/install,
# onde o CMakeLists.txt raiz o encontra e define SHADPS4_X64ARM64_HAS_LLVM.
set -euo pipefail

REPO=E:/shadPS4
NDK="C:/Users/mathu/AppData/Local/Android/Sdk/ndk/29.0.13113456"
TBLGEN_DIR=E:/tmp/llvm-tblgen-host/bin
BUILD_DIR=E:/tmp/llvm-android-build
INSTALL_DIR=$REPO/externals/llvm-android/install

cmake -G Ninja \
    -S "$REPO/externals/llvm-project/llvm" \
    -B "$BUILD_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-29 \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX="$INSTALL_DIR" \
    -DLLVM_HOST_TRIPLE=aarch64-unknown-linux-android29 \
    -DLLVM_DEFAULT_TARGET_TRIPLE=aarch64-unknown-linux-android29 \
    -DLLVM_TARGETS_TO_BUILD=AArch64 \
    -DLLVM_TARGET_ARCH=AArch64 \
    -DLLVM_TABLEGEN="$TBLGEN_DIR/llvm-tblgen.exe" \
    -DLLVM_NATIVE_TOOL_DIR="$TBLGEN_DIR" \
    -DLLVM_ENABLE_RTTI=ON \
    -DLLVM_ENABLE_THREADS=ON \
    -DLLVM_ENABLE_ZLIB=OFF \
    -DLLVM_ENABLE_ZSTD=OFF \
    -DLLVM_ENABLE_LIBXML2=OFF \
    -DLLVM_ENABLE_TERMINFO=OFF \
    -DLLVM_ENABLE_LIBEDIT=OFF \
    -DLLVM_ENABLE_BINDINGS=OFF \
    -DLLVM_BUILD_TOOLS=OFF \
    -DLLVM_INCLUDE_TOOLS=ON \
    -DLLVM_INCLUDE_TESTS=OFF \
    -DLLVM_INCLUDE_EXAMPLES=OFF \
    -DLLVM_INCLUDE_BENCHMARKS=OFF \
    -DLLVM_INCLUDE_DOCS=OFF \
    -DLLVM_INCLUDE_UTILS=OFF \
    -DLLVM_ENABLE_ASSERTIONS=OFF \
    -DLLVM_BUILD_LLVM_DYLIB=OFF

ninja -C "$BUILD_DIR" install
