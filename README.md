# lein-vimclojure

A fully-featured Leiningen plugin to run a VimClojure server for Leiningen
projects.

## Why another VimClojure plug-in?

There are already a number of VimClojure plug-ins out there, some of them
called lein-nailgun and others called lein-vimclojure.  However, most of them
tend to be fairly minimal.  In particular, most of them lacked two key features:

1. Support for both Leiningen 1.x and Leiningen 2.x projects, and
2. The ability to run a standalone REPL in the same process as the server.

## Installation

While you can install this plug-in by adding it to the `:plugins` vector of
your `project.clj`, it probably makes more sense to install it as a user-level
plug-in.

### Leiningen 2

Add `[com.sattvik/lein-vimclojure "0.9.0"]` to the `plugins` vector of your `:user`
profile located in `~/.lein/profiles.clj`.  For example:

```clj
    {:user {:plugins [[com.sattvik/lein-vimclojure "0.9.0"]]}}
```

### Leiningen 1

Just run:

    lein plugin install com.sattvik/lein-vimclojure 0.9.0

## Use

Once the plug-in is installed, running it is as easy as:

    lein vimclojure

The plug-in accepts some arguments, described in the next section.

## Configuration

This plug-in supports the following configuration options:

* `:host`, the host name to use for the VimClojure server, defaults to
  `"127.0.0.1"`
* `:port`, the port the VimClojure server will listen on, defaults to `2113`
* `:repl`, a boolean value determining whether the plug-in should launch a
  REPL, defaults to `false`

You may set these options as follows.

### Profile-based configuration (Leiningen 2 only)

You can set these options using the profile feature of Leiningen 2.  For
example:

```clj
    {:user {:plugins [[com.sattvik/lein-vimclojure "0.9.0"]]
            :vimclojure-opts {:repl true
	                      :port 42}}}
```

### Project-based configuration

Additionally, you can modify your `project.clj` and add a `:vimclojure-opts`
mapping, for example:

```clj
    (defprojct foo "1.0.0"
      :vimclojure-opts {:repl true
                        :port 42})
```

### Command line override

Finally, you can override any profile- or project-based settings at the command line:

    lein vimclojure :repl true :port 42

## TODO

There are a number of features that may be added to the plug-in:

* The ability to run an initialisation script when launching the server
* Adding more features the REPL, such as ClojureDocs support
* The ability to hook onto other Leiningen commands

## License

Copyright © 2012 Sattvik Software & Technology Resources, Ltd. Co.
All rights reserved.

Distributed under the Eclipse Public License, the same as Clojure.
