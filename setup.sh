#!/usr/bin/env bash
# setup.sh - install the dependencies of the project, then check that the unit tests pass.
# stejar-transfers-java. Training material. Invented bank.
#
# Run it in Terminal, from the repository folder:
#   bash setup.sh
#
# What a developer does on a new project: Maven packages, Playwright browser, virtual environment of the cycle 6 agent,
# the superpowers plugin of Claude Code (switched off), then the unit tests.
# Run check.sh first: the tools must be installed.
# It needs network access. Safe to run again: it keeps what is installed.

# Work in the repository folder, wherever the script is called from.
cd "$(dirname "$0")" || exit 1
# Keep the PATH of your terminal, then add the usual install folders, so that a tool
# installed a minute ago is found even before you open a new terminal.
ORIG_PATH=$PATH
for d in /opt/homebrew/bin /usr/local/bin "$HOME/.local/bin" "$HOME/.dotnet"; do
  [ -d "$d" ] || continue
  case ":$PATH:" in *":$d:"*) ;; *) PATH="$PATH:$d" ;; esac
done
export PATH
# Colours only in a terminal.
if [ -t 1 ]; then
  red=$'\033[31m'; green=$'\033[32m'; yellow=$'\033[33m'; cyan=$'\033[36m'; grey=$'\033[90m'; reset=$'\033[0m'
else
  red=''; green=''; yellow=''; cyan=''; grey=''; reset=''
fi

# Print the title of a step.
step() { printf '\n%s==> %s%s\n' "$cyan" "$1" "$reset"; }
# Print an error and stop the script.
fail() { printf '%sFAILED: %s%s\n' "$red" "$1" "$reset"; exit 1; }
# Run a program, and stop the script if it fails.
run() { "$@" || fail "exit code $? for: $*"; }

# version_ge 3.12.1 3.11 is true when the first version is the same or newer.
version_ge() {
  local IFS=.
  local -a a b
  read -r -a a <<< "$1"
  read -r -a b <<< "$2"
  local i x y
  for i in 0 1 2; do
    x=${a[i]:-0}; y=${b[i]:-0}
    x=${x%%[!0-9]*}; y=${y%%[!0-9]*}
    x=${x:-0}; y=${y:-0}
    if (( 10#$x > 10#$y )); then return 0; fi
    if (( 10#$x < 10#$y )); then return 1; fi
  done
  return 0
}

# On macOS, /usr/bin/git and /usr/bin/python3 only open an install dialog when the
# Command Line Tools are missing: do not call them in that case.
is_stub() {
  [ "$(uname -s)" = Darwin ] || return 1
  case "$1" in /usr/bin/git|/usr/bin/python3) ;; *) return 1 ;; esac
  xcode-select -p >/dev/null 2>&1 && return 1
  return 0
}

# Find Python: the newest python3.x first, then python3 and python. Sets PY_EXE and
# PY_VERSION to the first one that is at least $1 (PY_OK=1), else to the first one
# found (PY_OK=0, too old). Returns 1 when there is no Python at all.
find_python() {
  PY_EXE=''; PY_VERSION=''; PY_OK=0
  local name path v
  for name in python3.14 python3.13 python3.12 python3.11 python3.10 python3 python; do
    path=$(command -v "$name" 2>/dev/null) || continue
    is_stub "$path" && continue
    v=$("$path" -c 'import sys; print(".".join(map(str, sys.version_info[:3])))' 2>/dev/null) || continue
    [ -n "$v" ] || continue
    if version_ge "$v" "$1"; then PY_EXE=$path; PY_VERSION=$v; PY_OK=1; return 0; fi
    if [ -z "$PY_EXE" ]; then PY_EXE=$path; PY_VERSION=$v; fi
  done
  [ -n "$PY_EXE" ]
}

# State of the superpowers plugin for your user: missing, enabled, disabled or unknown.
# Needs PY_EXE to read the JSON.
superpowers_state() {
  local json
  json=$(claude plugin list --json 2>/dev/null) || { echo unknown; return; }
  [ -n "$PY_EXE" ] || { echo unknown; return; }
  printf '%s' "$json" | "$PY_EXE" -c '
import json, sys
try:
    items = json.load(sys.stdin)
except Exception:
    print("unknown"); sys.exit(0)
sp = [p for p in items if str(p.get("id", "")).startswith("superpowers@") and p.get("scope") == "user"]
print("missing" if not sp else ("enabled" if sp[0].get("enabled") else "disabled"))'
}

# Find the java that the Maven wrapper uses: JAVA_HOME first, then the default JDK
# of macOS. Sets JAVA_EXE and JAVA_VERSION (the major version, 8 for "1.8").
find_java() {
  JAVA_EXE=''; JAVA_VERSION=''
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXE=$JAVA_HOME/bin/java
  elif [ -x /usr/libexec/java_home ] && /usr/libexec/java_home >/dev/null 2>&1; then
    JAVA_EXE="$(/usr/libexec/java_home)/bin/java"
  elif [ "$(uname -s)" != Darwin ] && command -v java >/dev/null 2>&1; then
    JAVA_EXE=$(command -v java)
  fi
  [ -n "$JAVA_EXE" ] || return 1
  JAVA_VERSION=$("$JAVA_EXE" -version 2>&1 | grep 'version "' | head -n 1 | sed 's/.*version "\([0-9][0-9._]*\)".*/\1/')
  case "$JAVA_VERSION" in 1.*) JAVA_VERSION=${JAVA_VERSION#1.} ;; esac
  JAVA_VERSION=${JAVA_VERSION%%_*}
  [ -n "$JAVA_VERSION" ]
}

step 'Git repository'
# The ZIP of a GitHub release has no .git folder: create the repository and its first commit.
if [ -d .git ]; then echo 'Already a git repository: kept.'
else
  [ -n "$(git config user.name)" ] && [ -n "$(git config user.email)" ] || fail 'git needs a name and an e-mail first: git config --global user.name "Your Name" and git config --global user.email "you@example.com"'
  run git init -q -b main
  run git -c core.safecrlf=false add -A
  run git commit -q -m 'Starter repository, training material'
  echo 'Created: a git repository with one commit.'
fi

step 'JDK 17 or newer'
find_java && version_ge "$JAVA_VERSION" 17 || fail 'JDK 17 or newer not found. Run bash check.sh, then bash install-tools.sh.'
echo "JDK $JAVA_VERSION: $JAVA_EXE"

step 'Python (cycle 6 agent), 3.10 or newer'
find_python 3.10 && [ "$PY_OK" = 1 ] || fail 'Python 3.10 or newer not found. Run bash check.sh, then bash install-tools.sh.'
echo "Python $PY_VERSION: $PY_EXE"

step 'Build, and download the Maven packages (a few minutes the first time)'
run sh ./mvnw -B test-compile

step 'Playwright browser, used in cycle 5'
# Downloads Chromium once. Exit code 0 means it is installed. It prints no BUILD SUCCESS line: this is normal.
run sh ./mvnw -B exec:java@playwright-install

step 'Virtual environment of the cycle 6 agent: privacy-agent/.venv'
# Create it only if it is not there yet, then install only what is missing.
if [ -x privacy-agent/.venv/bin/python ]; then echo 'Already there: kept.'
else run "$PY_EXE" -m venv privacy-agent/.venv
fi
run privacy-agent/.venv/bin/python -m pip install --disable-pip-version-check -r privacy-agent/requirements.txt

step 'Plugin superpowers, used in cycle 1: installed, then switched off'
sp_note=''
if ! command -v claude >/dev/null 2>&1; then sp_note='Claude Code not found: plugin skipped. Run bash check.sh.'
else
  # Install only when missing: installing again would switch it on.
  if [ "$(superpowers_state)" = missing ]; then
    if ! claude plugin install superpowers@claude-plugins-official; then
      # The marketplace of Anthropic is out of date or not registered: update or add it, then try again.
      claude plugin marketplace update claude-plugins-official || claude plugin marketplace add anthropics/claude-plugins-official
      claude plugin install superpowers@claude-plugins-official
    fi
  fi
  [ "$(superpowers_state)" = enabled ] && claude plugin disable superpowers@claude-plugins-official --scope user
  state=$(superpowers_state)
  if [ "$state" = disabled ]; then echo 'OK: installed, switched off.'
  else sp_note="superpowers is $state. P1e of cycle 1 (optional) needs it installed and switched off."
  fi
fi
[ -n "$sp_note" ] && printf '%sWARNING: %s%s\n' "$yellow" "$sp_note" "$reset"

step 'Unit tests (expected: Tests run: 40, BUILD SUCCESS)'
run sh ./mvnw -B test

step 'Ready for cycle 1'
[ -n "$sp_note" ] && printf '%sExcept: %s%s\n' "$yellow" "$sp_note" "$reset"
echo 'Next: start Claude Code in this folder with: claude'
