Hypatia
=======

A high-performance hash based malware scanner. A basic port from the original Android version.

Use
---
- Compile: gradle assemble
- Download databases: sh update_databases.sh
- Run a one-off scan: java -jar HypatiaDesktop.jar $pathToDatabase $cacheFile $pathsToRecurse
 - Example output:
  - Hashed 758140 files, totalling 107224MB, 286208ms at 374MBps, skipped 398 files already in cache

Prebuilts
---------
- via CI: https://gitlab.com/divested/hypatia/-/jobs/artifacts/master/browse?job=build

TODO
----
- Monitoring
- Built-in database downloading/verifying

Donate
-------
- https://divested.dev/donate
