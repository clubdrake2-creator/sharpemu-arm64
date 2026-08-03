# SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
# SPDX-License-Identifier: GPL-2.0-or-later

set(SHADPS4_ANDROID_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/Android/app/src/main/cpp")

target_sources(shadps4 PRIVATE
    "${SHADPS4_ANDROID_CPP_DIR}/shadps4_android_jni.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/src/core/platform/android_app_host.cpp"
)

target_include_directories(shadps4 PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/src"
    "${SHADNET_PROTO_OUT}"
)

find_library(SHADPS4_ANDROID_LOG_LIB log)

if (SHADPS4PLUS_ROOT)
    file(TO_CMAKE_PATH "${SHADPS4PLUS_ROOT}" SHADPS4PLUS_ROOT_CMAKE)
else()
    set(SHADPS4PLUS_ROOT_CMAKE "")
endif()

if (SHADPS4PLUS_ROOT_CMAKE AND EXISTS "${SHADPS4PLUS_ROOT_CMAKE}/src/core/file_format/pkg.cpp")
    message(STATUS "Using shadPS4Plus PKG extractor from ${SHADPS4PLUS_ROOT_CMAKE}")

    set(SHADPS4PLUS_CRYPTOPP_DIR "${SHADPS4PLUS_ROOT_CMAKE}/externals/cryptopp")

    file(GLOB SHADPS4PLUS_CRYPTOPP_SOURCES
        "${SHADPS4PLUS_CRYPTOPP_DIR}/*.cpp"
    )
    list(FILTER SHADPS4PLUS_CRYPTOPP_SOURCES EXCLUDE REGEX ".*/(adhoc|bench|bench1|bench2|cryptest|datatest|dlltest|fipsalgt|fipstest|regtest|test|validat[0-9]?|validat[0-9]?\\_simd)\\.cpp$")

    add_library(shadps4plus_cryptopp STATIC
        ${SHADPS4PLUS_CRYPTOPP_SOURCES}
    )
    # Performance: PKG files are AES-encrypted, so install time is dominated by AES decryption.
    # CRYPTOPP_DISABLE_ASM previously forced the pure-software AES path (~10-20x slower than the CPU's
    # AES instructions). Enable Crypto++'s hardware AES: on the arm64 NDK -march=armv8-a+crypto unlocks
    # the ARMv8 Crypto extension (AES instructions) and defines __ARM_FEATURE_CRYPTO so
    # CRYPTOPP_ARM_AES_AVAILABLE turns on; on x86_64 the flag is rejected (probe below) and CryptoPP
    # uses SSE/AES-NI. Crypto++ still gates the hardware path at runtime (getauxval HWCAP_AES / CPUID),
    # so it falls back to software on a CPU without the extension.
    # +crc is needed too: Crypto++'s crc_simd.cpp uses the ARMv8 CRC32 instructions, which the base
    # armv8-a does not provide (only +crc does). The Snapdragon 855 (ARMv8.2) has both crypto and crc.
    include(CheckCXXCompilerFlag)
    check_cxx_compiler_flag("-march=armv8-a+crypto+crc" CRYPTOPP_HAS_ARMV8_CRYPTO)
    if (CRYPTOPP_HAS_ARMV8_CRYPTO)
        target_compile_options(shadps4plus_cryptopp PRIVATE -march=armv8-a+crypto+crc)
        message(STATUS "CRYPTOPP: enabled ARMv8 hardware AES (-march=armv8-a+crypto+crc)")
    else()
        message(STATUS "CRYPTOPP: ARMv8 crypto flag unavailable (abi=${ANDROID_ABI}); software AES")
    endif()
    target_include_directories(shadps4plus_cryptopp PRIVATE
        "${SHADPS4_ANDROID_CPP_DIR}/shadps4plus_shims"
        "${SHADPS4PLUS_CRYPTOPP_DIR}"
    )

    add_library(shadps4plus_pkg STATIC
        "${SHADPS4PLUS_ROOT_CMAKE}/src/core/file_format/pkg.cpp"
        "${SHADPS4PLUS_ROOT_CMAKE}/src/core/file_format/pkg_type.cpp"
        "${SHADPS4PLUS_ROOT_CMAKE}/src/core/crypto/crypto.cpp"
        "${SHADPS4PLUS_ROOT_CMAKE}/src/common/assert.cpp"
        "${SHADPS4PLUS_ROOT_CMAKE}/src/common/error.cpp"
        "${SHADPS4PLUS_ROOT_CMAKE}/src/common/io_file.cpp"
        "${SHADPS4PLUS_ROOT_CMAKE}/src/common/path_util.cpp"
        "${SHADPS4PLUS_ROOT_CMAKE}/src/common/string_util.cpp"
    )
    target_include_directories(shadps4plus_pkg BEFORE PUBLIC
        "${SHADPS4_ANDROID_CPP_DIR}/shadps4plus_shims"
        "${SHADPS4PLUS_ROOT_CMAKE}/src"
        "${SHADPS4PLUS_ROOT_CMAKE}/externals"
    )
    target_compile_definitions(shadps4plus_pkg PUBLIC
        SHADPS4_ANDROID_STANDALONE_PKG
    )
    target_link_libraries(shadps4plus_pkg PRIVATE
        shadps4plus_cryptopp
        ZLIB::ZLIB
    )

    target_compile_definitions(shadps4 PRIVATE
        SHADPS4_ANDROID_HAS_PKG_EXTRACTOR
    )
    target_include_directories(shadps4 PRIVATE
        "${SHADPS4_ANDROID_CPP_DIR}/shadps4plus_shims"
        "${SHADPS4PLUS_ROOT_CMAKE}/src"
        "${SHADPS4PLUS_ROOT_CMAKE}/externals"
    )
    target_link_libraries(shadps4 PRIVATE
        shadps4plus_pkg
    )
endif()

target_link_libraries(shadps4 PRIVATE
    ${SHADPS4_ANDROID_LOG_LIB}
    fmt::fmt
    spdlog::spdlog
    toml11::toml11
    SDL3::SDL3
    nlohmann_json::nlohmann_json
)
