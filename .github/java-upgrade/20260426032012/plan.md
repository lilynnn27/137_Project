# Upgrade Plan: project137-game (20260426032012)

- **Generated**: 2026-04-26 03:20:12
- **HEAD Branch**: main
- **HEAD Commit ID**: d6270b5499d15f8526f2686407a1037be6f37de0

## Available Tools

**JDKs**
- JDK 17.0.12: C:\Program Files\Java\jdk-17\bin (current baseline JDK)
- JDK 21.0.5: C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin (target runtime)

**Build Tools**
- Maven 3.9.6: c:\Users\tiamz\OneDrive\Desktop\CMSC 137\137_Project\apache-maven-3.9.6\bin\mvn.cmd
- No Maven wrapper detected; use local Maven distribution in the repository.

## Guidelines

- Upgrade the Java runtime from Java 17 to the latest LTS version (Java 21).
- Preserve existing project structure and keep dependency changes minimal.
- Run tests before and after the upgrade.

## Options

- Working branch: appmod/java-upgrade-20260426032012
- Run tests before and after the upgrade: true

## Upgrade Goals

- Upgrade Java from 17 to 21

### Technology Stack

| Technology/Dependency | Current | Min Compatible | Why Incompatible |
| --------------------- | ------- | -------------- | ---------------- |
| Java | 17 | 21 | User requested latest LTS runtime upgrade |
| Maven | 3.9.6 (local) | 3.9.6 | Already compatible with Java 21 |
| maven-compiler-plugin | 3.11.0 | 3.11.0 | Already compatible with Java 21 |
| org.openjfx:javafx-controls | 17.0.6 | 21.0.0 | JavaFX 17 is older than the target Java 21 runtime; align major versions for compatibility |
| org.openjfx:javafx-fxml | 17.0.6 | 21.0.0 | Same runtime compatibility concern as javafx-controls |
| org.openjfx:javafx-maven-plugin | 0.0.8 | 0.0.8 | Compatible with Maven 3.9.6 and Java 21; no upgrade required unless build issues appear |

### Derived Upgrades

- Upgrade JavaFX from 17.0.6 to 21.0.0 because the project is moving to Java 21 and JavaFX should align with the runtime.
- Use JDK 21 for final validation; this JDK is already installed on the machine.
- No Maven wrapper upgrade is required because the local Maven 3.9.6 distribution already supports Java 21.

## Upgrade Steps

- **Step 1: Setup Environment**
  - **Rationale**: Confirm the target JDK and local Maven distribution are available and usable.
  - **Changes to Make**:
    - [ ] Confirm JDK 21 is installed and accessible at `C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin`.
    - [ ] Confirm local Maven 3.9.6 is available at `apache-maven-3.9.6/bin/mvn.cmd`.
    - [ ] Verify the build command can run with the correct JDK and Maven path.
  - **Verification**:
    - Command: `"apache-maven-3.9.6/bin/mvn.cmd" -version`
    - Expected: Maven 3.9.6 runs and reports Java 21.

- **Step 2: Setup Baseline**
  - **Rationale**: Establish the current compile/test baseline on Java 17 before changing runtime configuration.
  - **Changes to Make**:
    - [ ] Run baseline compilation using JDK 17.
    - [ ] Run baseline tests with JDK 17.
    - [ ] Record current build status and any failures.
  - **Verification**:
    - Command: `"apache-maven-3.9.6/bin/mvn.cmd" clean test -q`
    - JDK: `C:\Program Files\Java\jdk-17\bin`
    - Expected: Baseline compile/test result documented.

- **Step 3: Upgrade Java runtime and JavaFX to Java 21**
  - **Rationale**: Align project build configuration and dependencies with the target Java 21 runtime.
  - **Changes to Make**:
    - [ ] Update `maven.compiler.source` and `maven.compiler.target` to `21` in `pom.xml`.
    - [ ] Update `<source>` and `<target>` in `maven-compiler-plugin` config to `21`.
    - [ ] Update `javafx.version` from `17.0.6` to `21.0.0`.
    - [ ] Keep `maven-compiler-plugin` at `3.11.0` because it supports Java 21.
  - **Verification**:
    - Command: `"apache-maven-3.9.6/bin/mvn.cmd" clean test-compile -q`
    - JDK: `C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin`
    - Expected: Compilation succeeds with Java 21.

- **Step 4: Final Validation**
  - **Rationale**: Verify the full upgrade, ensure the project compiles and tests pass with Java 21.
  - **Changes to Make**:
    - [ ] Run full Maven test lifecycle with Java 21.
    - [ ] Resolve any remaining compilation or test failures.
    - [ ] Confirm no temporary or fallback changes remain.
  - **Verification**:
    - Command: `"apache-maven-3.9.6/bin/mvn.cmd" clean test -q`
    - JDK: `C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot\bin`
    - Expected: Compilation success and 100% tests pass.

## Key Challenges

- **JavaFX and JDK alignment**
  - **Challenge**: JavaFX versions should match or be compatible with the target JDK.
  - **Strategy**: Upgrade `javafx.version` to `21.0.0` at the same time as the JDK upgrade to ensure runtime compatibility.

- **No Maven wrapper present**
  - **Challenge**: The project relies on a repository-local Maven installation instead of a wrapper.
  - **Strategy**: Use the committed local Maven distribution at `apache-maven-3.9.6/bin/mvn.cmd` for consistent build execution.

- **Baseline cleanup**
  - **Challenge**: Existing compiled artifacts in `target/` were present and stashed.
  - **Strategy**: Use `mvn clean` during verification and keep the working tree clean via git stash/branch operations.
