# R8 rules for the release build.
#
# Empty on purpose, and worth keeping rather than deleting: both build types
# name this file in app/build.gradle.kts, and for a long time it did not exist.
# Gradle does not complain about that, so the release build was running on
# AGP's defaults plus whatever rules each library ships inside itself, and
# nobody could tell the difference between "no rules are needed" and "the rules
# file went missing".
#
# As of 2026-10-01 no app-specific rule is needed, and that is a measurement
# rather than an assumption: a minified release build was installed on a device
# and put through the paths R8 is most likely to break. It launched (Hilt's
# generated injectors and Room's generated DAOs survived), wrote and read a
# plant, took a photo through FileProvider, and produced a backup whose JSON
# had all eleven table keys with every field present. That last one is the real
# test, because kotlinx.serialization fails quietly under R8: a stripped
# serializer gives you a bundle with fields silently missing rather than a
# crash.
#
# What to do when a release-only crash appears, which is the situation this
# file exists for. Debug builds are not minified, so "works in debug, dies in
# release" means R8 removed or renamed something reached by reflection. The
# stack trace will name an obfuscated class; map it with
# app/build/outputs/mapping/release/mapping.txt. Then add the narrowest keep
# that fixes it, here, with a comment saying which library and which symptom.
#
# Narrow matters. A broad -keep class dev.dheirav.** silences the problem and
# gives up most of what minification is for, which on this app is 18.3 MB of
# debug build becoming 4.3 MB.
