#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <current-tag> [output-dir]" >&2
  exit 1
fi

current_tag="$1"
output_dir="${2:-build/github}"

if ! git rev-parse --verify "${current_tag}^{tag}" >/dev/null 2>&1 && \
   ! git rev-parse --verify "${current_tag}^{commit}" >/dev/null 2>&1; then
  echo "Tag or commit '$current_tag' does not exist." >&2
  exit 1
fi

mkdir -p "$output_dir"

previous_tag=""
log_range="$current_tag"
if previous_tag="$(git describe --tags --abbrev=0 "${current_tag}^" 2>/dev/null)"; then
  log_range="${previous_tag}..${current_tag}"
fi

mapfile -t commit_subjects < <(git log --no-merges --pretty=format:%s "$log_range")

if [[ ${#commit_subjects[@]} -eq 0 ]]; then
  commit_subjects=("No user-facing changes.")
fi

escape_html() {
  local value="$1"
  value="${value//&/&amp;}"
  value="${value//</&lt;}"
  value="${value//>/&gt;}"
  printf '%s' "$value"
}

markdown_file="${output_dir}/release-notes.md"
{
  echo "## Changes"
  echo
  if [[ -n "$previous_tag" ]]; then
    echo "_Commit range: ${previous_tag}..${current_tag}_"
    echo
  fi
  for subject in "${commit_subjects[@]}"; do
    echo "- ${subject}"
  done
} >"$markdown_file"

html_file="${output_dir}/change-notes.html"
{
  echo "<h2>Changes</h2>"
  echo "<ul>"
  for subject in "${commit_subjects[@]}"; do
    escaped_subject="$(escape_html "$subject")"
    echo "  <li>${escaped_subject}</li>"
  done
  echo "</ul>"
} >"$html_file"

echo "Generated ${markdown_file} and ${html_file}"
