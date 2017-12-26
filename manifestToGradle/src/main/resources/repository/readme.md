## The build system

This repository contains multiple projects. Each project can be built separately.
The build tool is Gradle. If Gradle is not installed, just use `./gradlew` on Linux/MacOSX or
`gradlew.bat` on Windows instead of `gradle` for the following commands.

To build all projects, including executing the related tests, use 'gradle build' in the
root directory. To build a single project execute `gradle build :addons:binding:org.openhab.binding.airquality` for example, for navigate to the project directory and execute 'gradle build'.

### Quality checks

If you develop a binding for this project or if you want to contribute to existing code, it is necessary
to meet some quality standards. Next to the manually review process, there are some automated tools that make
sure that your code is maintainable and up to the coding standards.

Execute `gradle check` to execute all tests and quality checks. This needs to pass, before you should issue a
merge request for this repository.

If you want checks to be performed for a specific project only, you would use `gradle check :addons:binding:org.openhab.binding.airquality`.

Fine-grained specific checks and tests can be executed as well:

* `gradle LicenseCheck` : Checks for header consistency in the main source set
* `gradle LicenseFormat` : Applies the correct license header, where it is missing


### How to kickstart new binding development

The build system offers you to create a new binding directory, based on a template. The template includes a working
minimal handler implementation. A convenient way is the interactive mode: `gradle generate -i`. You will be asked
for the bindings name. You can also provide the name (and also optionally the group and version) directly:
`gradle generate -i -Dtarget=generated -Dname=my-awesome-plugin -Dgroup=org.openhab.binding -Dversion=1.0-SNAPSHOT`

Please be aware that your entered name is converted to lower case and whitespace is converted to dashes for the artifact ID, e.g. "My awesome plugin" becomes "my-awesome-plugin".

### Dependency management

In your project, you are only allowed to refer to the latest version of a dependency ("+" version modifier).
The actual resolved dependency version is determined by a dependency lock file (`dependencies.lock`).
This is because all dependencies and the specific used versions need an IP validation from the Eclipse foundation.
You can introduce a new dependency in your project, but you have to be prepared that the Eclipse foundation need to validate
the requested dependency version first and add it to the lock file.

The following tasks modify or alter the dependency lock file:

* `gradle generateLock saveLock`: Generate a **dependency.lock** file with the current Gradle dependency resolution. An existing file is ignored. Saves the lock file to the project directory.
* `gradle deleteLock`: Delete the lock file from the project directory.
* `gradlew updateLock -PdependencyLock.updateDependencies=com.example:foo,com.example:bar`: Updates listed dependencies. A **saveLock** task needs to be performed as well.

Refer to (Gradle Dependency Lock Plugin[https://github.com/nebula-plugins/gradle-dependency-lock-plugin/wiki/Usage] for further help.

### Publishing

To prepare the repository for publishing, the following Gradle tasks are available:

* `gradle generateFeatures`: Generate Karaf feature file in `/build/karaf/features`.
* `gradle generateKar`: Generate Karaf kar file in `/build/karaf/kar`.

<br>