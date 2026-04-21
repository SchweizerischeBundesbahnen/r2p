# Contributing

We appreciate all kinds of contributions. The following is a set of guidelines for contributing to this repository on GitHub. 
These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

#### Table Of Contents

* [Code of Conduct](#code-of-conduct)
* [I just have a question!](#i-just-have-a-question)
* [What should I know before I get started?](#what-should-i-know-before-i-get-started)
  * [Tools and Packages](#tools-and-packages)
  * [Design Decisions](#design-decisions)
* [Found an Issue?](#found-an-issue)
* [Want a Feature?](#want-a-feature)
  * [Submitting an Issue](#submitting-an-issue)
  * [Submitting a Pull Request (PR)](#submitting-a-pull-request-pr)
* [Coding Rules](#coding-rules)
* [Commit Message Guidelines](#commit-message-guidelines)
* [Attribution](#attribution)

## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). 
By participating, you are expected to uphold this code.

## I just have a question!

Please ask the questions in the discussions page.

* [Github Discussions, the official message board](https://github.com/SchweizerischeBundesbahnen/r2p/discussions)

## What should I know before I get started?

### Tools and Packages
See [README.md](README.md)

### Design Decisions
See [ADRs](docs/adr/)

## Found an Issue?
If you find a bug in the source code or a mistake in the documentation, you can help us by
[submitting an issue](#submitting-an-issue) to our [GitHub Repository](https://github.com/SchweizerischeBundesbahnen/r2p/issues/new). Including an issue
reproduction (via StackBlitz, JsBin, Plunkr, etc.) is the absolute best way to help the team quickly
diagnose the problem. Screenshots are also helpful.

You can help the team even more and [submit a Pull Request](#submitting-a-pull-request-pr) with a fix.


## Want a Feature?
You can *request* a new feature by [submitting an issue](#submitting-an-issue)
to our [GitHub Repository](https://github.com/SchweizerischeBundesbahnen/r2p/issues/new).
If you would like to *implement* a new feature, please submit an issue with
a proposal for your work first, to be sure that we can use it.
Please consider what kind of change it is:

* For a **Major Feature**, first open an issue and outline your proposal so that it can be
  discussed. This will also allow us to better coordinate our efforts, prevent duplication of work,
  and help you to craft the change so that it is successfully accepted into the project.
* **Small Features** can be crafted and directly [submitted as a Pull Request](#submitting-a-pull-request-pr).


### Submitting an Issue
If your issue appears to be a bug, and hasn't been reported, open a new issue.
Providing the following information will increase the
chances of your issue being dealt with quickly:

* **Overview of the Issue** - if an error is being thrown a non-minified stack trace helps
* **Toolchain and Environment Details** - which versions of libraries, toolchain, platform etc 
* **Motivation for or Use Case** - explain what are you trying to do and why the current behavior
  is a bug for you
* **Browsers and Operating System** - is this a problem with all browsers?
* **Reproduce the Error** - provide a live example (using StackBlitz or similar) or a unambiguous set of steps
* **Screenshots** - myybe screenshots can help the team
  triage issues far more quickly than a text description.
* **Related Issues** - has a similar issue been reported before?
* **Suggest a Fix** - if you can't fix the bug yourself, perhaps you can point to what might be
  causing the problem (line of code or commit)

You can file new issues by providing the above information [here](https://github.com/SchweizerischeBundesbahnen/r2p/issues/new).


### Submitting a Pull Request (PR)
Before you submit your Pull Request (PR) consider the following guidelines:

* Make your changes in a new git branch:

     ```shell
     git checkout -b my-fix-branch main
     ```

* Create your patch, **including appropriate test cases**.
* Follow our [Coding Rules](#coding-rules).
* Test your changes with our supported browsers and screen readers.
* Run tests and ensure that all tests pass.
* Commit your changes using a descriptive commit message that follows our
  [commit message conventions](#commit-message-guidelines). Adherence to these conventions
  is necessary because release notes are automatically generated from these messages.

     ```shell
     git commit -a
     ```
  Note: the optional commit `-a` command line option will automatically "add" and "rm" edited files.

* Push your branch to GitHub:

    ```shell
    git push my-fork my-fix-branch
    ```

* In GitHub, send a pull request to `sbb-your-project:main`.
  The PR title and message should as well conform to the [Commit Message Guidelines](#commit-message-guidelines).

## Coding Rules
To ensure consistency throughout the source code, keep these rules in mind as you are working:

* All features or bug fixes **must be tested** by one or more specs (unit-tests).
* All public API methods **must be documented**.
* Also see [CODING_STANDARDS](CODING_STANDARDS.md)

## Commit Message Guidelines

This project uses [Conventional Commits](https://www.conventionalcommits.org/) to generate the changelog.
Use the following prefixes when committing:

- `feat:` - New features (bumps MINOR version)
- `fix:` - Bug fixes (bumps PATCH version)
- `docs:` - Documentation changes
- `style:` - Code formatting (no functional changes)
- `refactor:` - Code refactoring
- `perf:` - Performance improvements
- `test:` - Adding or modifying tests
- `build:` - Build system or dependencies
- `ci:` - CI/CD changes
- `chore:` - Other changes

**Breaking Changes:** Add `BREAKING CHANGE:` in the commit body or use `!` after the type (e.g., `feat!:`) to bump the MAJOR version.

Examples:
```bash
git commit -m "feat: Add new API endpoint for train data"
git commit -m "fix: Correct authentication issue"
git commit -m "feat!: API change - removed old format"
```

You can also use scopes for better organization:
```bash
git commit -m "feat(api): Add new train data endpoint"
git commit -m "fix(auth): Correct JWT token validation"
```

For breaking changes with detailed explanation:
```bash
git commit -m "feat!: Change API response format

BREAKING CHANGE: The API now returns data in a new format.
Old clients will need to update their integration."
```

This is checked by a github action and by Husky pre-commit hooks.
To enable the pre-commit hook, run:
```bash
npm install
```

## Attribution

This CONTRIBUTING guideline is adapted from the [sbb-design-systems/sbb-angular](https://github.com/sbb-design-systems/sbb-angular)
