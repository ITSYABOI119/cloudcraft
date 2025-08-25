# Contributing to CloudCraft Engine

We love your input! We want to make contributing to CloudCraft Engine as easy and transparent as possible.

## Development Process

We use GitHub to host code, to track issues and feature requests, as well as accept pull requests.

1. Fork the repo and create your branch from `main`
2. If you've added code that should be tested, add tests
3. If you've changed APIs, update the documentation
4. Ensure the test suite passes
5. Make sure your code follows our coding standards
6. Issue that pull request!

## Pull Request Process

1. Update the README.md with details of changes if needed
2. Update the CHANGELOG.md with a note describing your changes
3. The PR will be merged once you have the sign-off of the maintainers

## Coding Standards

- Use Java 21 features appropriately
- Follow standard Java naming conventions
- Include comprehensive comments
- Write unit tests for new features
- Maintain thread safety in all components
- Document performance implications

## Commit Message Format

We follow a strict commit message format:
```
Type(scope): Subject

Body (optional)
```

Types:
- Feat: New feature
- Fix: Bug fix
- Docs: Documentation only
- Style: Code style/formatting
- Refactor: Code refactoring
- Test: Adding tests
- Chore: Maintenance tasks

Example:
```
Feat(threading): Add region-based entity processing

- Implement 16x16 chunk regions
- Add thread pool management
- Include performance metrics
```

## Performance Considerations

When contributing, always consider:
- Thread safety
- Memory usage
- CPU utilization
- Compatibility with vanilla behavior
- Impact on plugin ecosystem

## Setting Up Development Environment

1. Install Java 21
2. Clone the repository
3. Import as Gradle project
4. Run `./gradlew build` to verify setup

## Running Tests

```bash
./gradlew test        # Run unit tests
./gradlew stressTest  # Run performance tests
```

## Need Help?

Join our Discord server for development discussions and support.
