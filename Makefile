# Project Chimera Makefile
.PHONY: setup test lint clean

# Install dependencies and build the project (skip tests)
setup:
	./mvnw clean install -DskipTests

# Run all tests (including the failing TDD tests)
test:
	./mvnw test

# Run code quality checks (Checkstyle)
lint:
	./mvnw checkstyle:check

# Clean the project
clean:
	./mvnw clean

# Optional: Run a specific test class
test-class:
	./mvnw test -Dtest=$(CLASS)

# Run tests with verbose output
test-verbose:
	./mvnw test -X