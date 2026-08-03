cmake_minimum_required(VERSION 3.22)
project(sharpemu_shim LANGUAGES C)

# SharpEmu's emulator core is managed C#/.NET (SharpEmu.Core/.HLE/.Libs), not
# C++ — unlike shadPS4, from whose Android shell this project tree was copied
# and adapted, there is no native emulator core to build here. The only
# native code this module needs is a small shim exporting a single C symbol,
# "SDL_main", which SDL3's Android Java glue (SDLActivity.nativeRunMain)
# dlsym's and calls with argv built from GameActivity.getArguments() — the
# shim's job is to bounce that call into the managed SharpEmu.Android bridge
# (a JNI upcall) rather than run a native emulator loop itself.
#
# TODO(Phase 2): add sharpemu_shim.c and its add_library/target_link_libraries.
