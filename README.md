Hypatia
=======

A high-performance hash based malware scanner. A basic port from the original Android version.

Use
---
- gradle assemble
- sh update_databases.sh
- java -jar HypatiaDesktop.jar $databases $pathsToRecurse
- Example output:
    - Hashed 5789841 files, totalling 1679630MB, 1761712ms at 953MBps

Prebuilts
---------
- via CI: https://gitlab.com/divested/hypatia/-/jobs/artifacts/master/browse?job=build

TODO
----
- User defined thread count
- Default thread count determined by storage medium

Donate
-------
- https://divested.dev/donate
