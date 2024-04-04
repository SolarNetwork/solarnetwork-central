# SolarNet Cloud Applications

This directory contains the SolarNet applications that make up the cloud portion of SolarNetwork.

The main application projects are:

 * [SolarIn](./solarin/)
 * [SolarJobs](./solarjobs/)
 * [SolarOCPP](./solarocpp/)
 * [SolarQuery](./solarquery/)
 * [SolarUser](./solaruser/)

Additional application projects are:

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
