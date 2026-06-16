#!/usr/bin/env sh
set -e
GIT_HOOKS_DIR=".git/hooks"
HOOK_SRC="scripts/pre-commit"

if [ ! -d ".git" ]; then
  echo ".git directory not found; skipping hook installation."
  exit 0
fi

mkdir -p "$GIT_HOOKS_DIR"
cp "$HOOK_SRC" "$GIT_HOOKS_DIR/pre-commit"
chmod +x "$GIT_HOOKS_DIR/pre-commit"
echo "Installed git pre-commit hook."
