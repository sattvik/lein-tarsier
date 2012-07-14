# What’s in in lein-tarsier?

This page will list all of the latest changes in lein-tarsier.

## 0.9.3

### New features

* Now supports trampoline with Leiningen 2.

### Bug fixes

* Fix problem with `leiningen.core.project/merge-profile` introduced in recent
  Leiningen 2 preview releases (thanks to Jeremy Holland).
  [Issue 4](https://github.com/sattvik/lein-tarsier/issues/4)

## 0.9.2

### New features

* lein-tarsier’s REPL is now based on the REPL that ships with Leiningen.  This
  means that it should now respect all of the same project and user/profile
  settings as `lein repl`.  This also means that Leiningen 2.x users get
  [REPL-y][1] with their VimClojure.

[1]: https://github.com/trptcolin/reply

### Bug fixes

* Fix problem with console I/O when running the REPL. [Issue 1](https://github.com/sattvik/lein-tarsier/issues/1)

### Known issues

* The code for the REPL is for Leiningen 2 is based off of Leiningen
  2.0.0-preview3.  This means it does not support trampoline and may not work
  with the latest development snapshot.
* A stack trace is sometimes emitted when exiting the REPL with Leiningen 2.
  It is innocuous.

<!-- vim:set ft=markdown: -->
