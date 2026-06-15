.PHONY: dev watch test build ci clean metals help

SBT ?= sbt --client

METALS_MCP ?= $(HOME)/Library/Application\ Support/Coursier/bin/metals-mcp
METALS_PORT ?= 52632

# ── Development ────────────────────────────────────────────────────────────

dev:
	sbt --batch dev

metals:
	$(METALS_MCP) --workspace . --port $(METALS_PORT) --transport http

watch:
	$(SBT) '~fastLinkJS'

# ── Quality ─────────────────────────────────────────────────────────────────

test:
	$(SBT) test

# ── Production build ────────────────────────────────────────────────────────

build:
	$(SBT) fullLinkJS
	mkdir -p public
	cp index.html public/
	@cp "$$(find target -name "main.js" -path "*text-maps-opt*" | head -1)" public/main.js
	touch public/.nojekyll
	@echo "Built → public/"

ci: test build

# ── Housekeeping ─────────────────────────────────────────────────────────────

clean:
	$(SBT) clean
	rm -rf public/

help:
	@echo "Usage: make <target> [SBT=sbt]"
	@echo ""
	@echo "  dev      compile + watch + serve on :8080 with live reload"
	@echo "  watch    continuous recompile only (no server)"
	@echo "  metals   start Metals MCP server on :52632 for Claude Code"
	@echo "  test     run unit tests"
	@echo "  build    production build → public/"
	@echo "  ci       test + build (use SBT=sbt in CI)"
	@echo "  clean    remove all build outputs"
