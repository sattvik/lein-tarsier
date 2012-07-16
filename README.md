# lein-tarsier

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

Add `[lein-tarsier "0.9.3"]` to the `plugins` vector of your `:user`
profile located in `~/.lein/profiles.clj`.  For example:

```clj
    {:user {:plugins [[lein-tarsier "0.9.3"]]}}
```

### Leiningen 1

Just run:

    lein plugin install lein-tarsier 0.9.3
### Vim Setup

Next you need to setup vim to take advantage of the vim clojure (nailgun) server you
just setup. In order to do that run: 

    cd ~/.vim/bundle
    git clone https://github.com/vim-scripts/VimClojure.git vimclojure

Next you need to create the nailgun client:
   
    curl http://kotka.de/projects/vimclojure/vimclojure-nailgun-client-2.3.0.zip > /tmp/vimclojure-nailgun-client-2.3.0.zip
    cd /tmp
    unzip vimclojure-nailgun-client-2.3.0.zip
    cd vimclojure-nailgun-client/
    make
    cp ng ~/bin/

Note that ~/bin must be on your path

Finally add the following lines to your .vimrc

    syntax on
    filetype plugin indent on
    let vimclojure#WantNailgun = 1

## Use

Once the plug-in is installed, running it is as easy as:

    lein vimclojure

The plug-in accepts some arguments, described in the next section.

### Use in Vim

All of the commands and configuration for the use of vim clojure in vim itself are handled
by the [VimClojure Script](https://github.com/vim-scripts/VimClojure). Here are a few commands
to get you started, but please see the script configuration for more details:

 * `<LocalLeader>sr` – Start a repl
 * `<LocalLeader>sR` – Start a repl in the current namespace
 * `<LocalLeader>eb` – Evaluate current visual block. There are several “eX” variations.
 * `<LocalLeader>lw` – Lookup docs for word under cursor. There are several “lX” lookup variations

Thanks to [Dave Ray](http://blog.darevay.com/2010/10/how-i-tamed-vimclojure/) for a great starting point.

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
    {:user {:plugins [[lein-tarsier "0.9.3"]]
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

## Overriding the VimClojure server version

This plug-in is configured to use version 2.3.3 of the VimClojure server by
default.  However, you can override this by manually specifying the dependency
in your project.

For example, to use version 2.2.0 of the server, you could add the following
dependency to your project:

    [vimclojure/server "2.2.0"]

## About the name

In order to minimise confusion between this plug-in and other `lein-nailgun`
and `lein-vimclojure` plug-ins, this plug-in is named `lein-tarsier`, in honour
of the animal that appears on the cover of _Learning the vi and Vim Editors_.

## TODO

There are a number of features that may be added to the plug-in:

* The ability to hook onto other Leiningen commands

## Contributors

* Jeremy Holland
* Katsunori Kanda

## License

Copyright © 2012 Sattvik Software & Technology Resources, Ltd. Co.
All rights reserved.

Distributed under the Eclipse Public License, the same as Clojure.
