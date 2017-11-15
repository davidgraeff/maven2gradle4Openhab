# Migration Tools

This repository contains migration tools from maven/typcho to gradle/bnd

## Manifest dependencies to gradle dependencies

This tool reads an existing MANIFEST.MF file, extracts the dependencies and creates
a `dependencies.gradle` file per project in the root of each project.
If the tool is run in a multi-project directory, it will
also create an `allowed_dependencies.json` file, which lists all encountered project dependencies
in a single file in the following format:

```
[ "com.google.dagger:dagger-compiler:2.8": {},
  "another.group:artifactID:version": {}
]
```
