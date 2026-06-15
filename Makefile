.PHONY: dev test build native clean help

help:
	@echo "make dev     — compile + watch + dev server on :8082"
	@echo "make test    — run unit tests"
	@echo "make build   — production JS build → public/"
	@echo "make native  — compile native CLI binary"
	@echo "make clean   — remove build artifacts"

dev:
	sbt --batch dev

test:
	sbt --batch "coreJS/test"

build:
	sbt --batch "js/fullLinkJS"
	@mkdir -p public
	@cp -r js/target/scala-3.3.3/text-maps-js-opt public/ 2>/dev/null || true
	@cp index.html public/

native:
	sbt --batch "native/nativeLink"
	@echo "Binary: native/target/scala-3.3.3/text-maps-native"
	@echo "Usage:  echo 'map dungeon\\ngenerate dungeon rooms:8 seed:42' | native/target/scala-3.3.3/text-maps-native"

clean:
	sbt --batch clean
