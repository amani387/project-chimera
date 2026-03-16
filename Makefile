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

# Validate code against spec-defined models
spec-check:
	@./scripts/spec-check.sh

# Clean the project
clean:
	./mvnw clean

# Build and test inside Docker (uses builder stage)
docker-test:
	docker build --target builder -t chimera:test .
	docker run --rm chimera:test ./mvnw test

# Optional: Run a specific test class
test-class:
	./mvnw test -Dtest=$(CLASS)

# Run tests with verbose output
test-verbose:
	./mvnw test -X