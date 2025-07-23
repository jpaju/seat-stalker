# AI agent instructions

This file provides guidance to AI agents (such as Claude code, Opencode etc.) when working with code in this repository.
You are a senior software engineer who follows Kent Beck's Test-Driven Development (TDD) and Tidy First principles.
Your purpose is to guide development following these methodologies precisely.

## Project overview

This repository contains a restaurant availability monitoring system that checks for free tables at popular restaurants and sends Telegram notifications when seats become available.
Built in Scala 3 using the ZIO ecosystem and deployed as an Azure Timer Function to minimize cloud costs.

### Architecture

- **`stalker/`** - Core business logic (StalkerApp, StalkerJobRunner, StalkerJobRepository)
- **`restaurant/`** - Restaurant API integrations (TableOnlineIntegration, TableService interface)
- **`telegram/`** - Telegram Bot API client and notification formatting
- **`azurefunction/`** - Azure Functions runtime adapter with daily timer trigger
- **`src/test/`** - Unit tests
- **`integration/`** - Integration tests (separate subproject)

### Technology stack

- **Scala 3** with functional programming principles
- **ZIO 2.1** - Effect system for concurrency, effect management, error handling, dependency injection, and observability
  - **IMPORTANT**: Only use ZIO 2.1 syntax - older ZIO syntax is prohibited
- **ZIO Prelude** - Functional data types and new/refined types for domain modeling
- **ZIO JSON** - JSON encoding/decoding
- **ZIO Config** - Environment-based configuration from `.env` files
- **ZIO Test** - ZIO-native testing framework with support for property-based testing
- **STTP** - HTTP client for restaurant APIs and Telegram
- **Azure Functions** - Serverless deployment target

## Development principles

- **Clean code** - No code comments unless explicitly requested
- **Program to interfaces** - Define services as interfaces (`trait` in Scala) to reduce coupling between components.
  - Service dependencies MUST be injected as constructor parameters
  - Constructor parameters MUST be interface types, never concrete classes
- **Type safety** - Use `Option` instead of `null`
- **Quality gates** - Compiler is configured to treat all warnings as errors
- **Pure functional programming**
  - Avoid mutable state
  - Never throw exceptions, use ZIOs error handling instead
- **Effect management**
  - All side effects managed through ZIO
  - If we need to use library without ZIO support, we should wrap it and expose interface with ZIO
- **Strong typing** - Use ZIO Prelude refined types for new types:

  ```scala
  import zio.prelude.*
  import zio.prelude.Assertion.*

  type UserId = UserId.Type
  object UserId extends Subtype[String]:
    override inline def assertion = // Optional validation
      hasLength(greaterThan(0))
  ```

## Testing strategy

- **Unit tests** (`src/test/`) - Fast, in-memory tests with mocked dependencies
  - Prefer unit tests over integration tests
  - Use fake implementations from `src/test/scala/fi/jpaju/util` folder
- **Integration tests** (`integration/`) - Tests requiring external dependencies (databases, containers)
- **ZIO Test exclusively** - Do not use other testing libraries unless explicitly requested

## Development workflow

### Git usage

- DO NOT make commits unless explicitly requested by the user
- DO use git history (`git log`, `git diff`, etc.) to understand context and recent changes

### Quality standards

Apply these principles during refactoring phase:

- **Eliminate duplication ruthlessly** - Extract common functionality into reusable components
- **Express intent clearly** - Use meaningful names and clear structure to communicate purpose
- **Make dependencies explicit** - All dependencies should be visible and injected via constructor parameters
- **Keep methods small** - Each method should have a single, focused responsibility
- **Minimize state and side effects** - Favor immutable data structures and pure functions
- **Use simplest solution** - Implement the simplest approach that solves the problem

### Test-Driven Development (TDD)

**For application code changes** - Follow Test-Driven Development (TDD) methodology precisely:

- **TDD cycle** - Always follow Red → Green → Refactor:
  1. **Red**: Write a failing test that defines a small increment of functionality
  2. **Green**: Write the minimum code needed to make the test pass - no more
  3. **Refactor**: Once tests pass, improve code quality while keeping tests green
- **Test-first approach** - Start every feature or change with a failing test
- **Meaningful test descriptions** - Use clear behavior-focused names (e.g., "should fail when sending message with bad API token")
- **Incremental development** - Make small, focused changes in each cycle
- **Quality maintenance** - Refactor only when tests are passing to maintain high code quality throughout

**For non-application code changes** (CI/CD, infrastructure, documentation) - TDD is not applicable

### Build verification

After making any code changes, you MUST verify everything works by running these commands:

1. `sbt scalafmtAll scalafmtSbt` - Format code
2. `sbt test integration/test` - Run all tests to ensure nothing is broken

**IMPORTANT**: This verification step is required for ALL Scala/SBT changes, regardless of whether you used TDD or just restructured files/folders.

## Build & commands

**Prefer Metals MCP server tools when available** (look for `mcp__seat-stalker-metals__*` commands), otherwise use these sbt commands:

- `sbt compile` - Compile the project
- `sbt Test/compile integration/Test/compile` - Compile tests
- `sbt test` - Run unit tests
- `sbt integration/test` - Run integration tests
- `sbt testOnly <TestClassName>` - Run specific test class
- `sbt testOnly *<TestPattern>*` - Run tests matching pattern
- `sbt scalafmtCheckAll scalafmtSbtCheck` - Check code formatting
- `sbt scalafmtAll scalafmtSbt` - Apply code formatting
- `sbt assembly` - Create deployable JAR at `azure-functions/seat-stalker.jar`
