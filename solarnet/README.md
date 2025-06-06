# SolarNet Cloud Applications

This directory contains the SolarNet applications that make up the cloud portion of SolarNetwork.

The main application projects are:

 * [SolarIn](./solarin/)
 * [SolarJobs](./solarjobs/)
 * [SolarOCPP](./solarocpp/)
 * [SolarQuery](./solarquery/)
 * [SolarUser](./solaruser/)

Additional application projects are:

 * [SolarDIN](./solardin/)
 * [SolarDNP3](./solardnp3/)
 * [SolarOSCP Flexibility Provider](./oscp-fp/)
 * [SolarOSCP Capacity Provider Simulator](./oscp-sim-cp/)

# Building

The build is managed by Gradle, and requires a Java Development Kit version 21+ to build (and run).
To build all applications:

```sh
# Unix-like OS
./gradlew build

# Build without running tests:
./gradlew build -x test

# Windows
./gradlew.bat build
```

## ErrorProne

Pass `-P errorProneDisabled=true` to the build command to disable ErrorProne warnings when compiling
sources, and `-P errorProneEnabledTest=true` to enable warnings when compiling test sources.

# Testing

You can run all subproject unit/integration tests and produce a single HTML report at
`./build/reports/all-tests/index.html` like this:

```sh
# Run all project tests, combine into build/reports/all-tests/index.html
./gradlew testReport
```
