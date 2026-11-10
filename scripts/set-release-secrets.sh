#!/usr/bin/env bash
# Sets the GitHub Actions secrets that .github/workflows/publish.yaml needs. Same Central account
# as the Android SDK repo (bbnativeplayersdk-kotlin), but its GPG secret is NOT reusable: that one is stored single-line, setup-java needs a normal armored key.
#
#   MAVEN_CENTRAL_USERNAME          Central Portal user-token username
#   MAVEN_CENTRAL_PASSWORD          Central Portal user-token password
#   MAVEN_GPG_PRIVATE_KEY             ASCII-armored GPG private key
#   MAVEN_GPG_PASSPHRASE    passphrase of that key
#
# GitHub never returns a secret's value (API or UI), so they cannot be copied from the Android
# repo. Take them from where you keep them (password manager) or generate new ones:
#   - token:  central.sonatype.com > Account > Generate User Token
#   - key:    the GPG key whose public half is published for com.bluebillywig
#
# Values are read from the environment (same variable names) or prompted for without echo, and
# are piped to `gh secret set` on stdin: never on a command line, never printed, never written
# to disk by this script.
#
# Usage:
#   scripts/set-release-secrets.sh [--dry-run]
#   REPO=owner/name                    target repo (default bluebillywig/bb-sapi-java-sdk)
#   GPG_KEY_ID=ABCD1234...             export the signing key from your local gpg keyring
#                                      (gpg asks for the passphrase itself)
#   SIGNING_KEY_FILE=/path/key.asc     or read the armored private key from a file
#   (or MAVEN_GPG_PRIVATE_KEY=... in the environment; prompted for a file path if none is set)
#
# Needs: gh logged in with admin access to the target repo (`gh auth status`).

set -euo pipefail

REPO="${REPO:-bluebillywig/bb-sapi-java-sdk}"
DRY_RUN=0
[ "${1:-}" = "--dry-run" ] && DRY_RUN=1

command -v gh >/dev/null 2>&1 || { echo "gh is required" >&2; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "gh is not logged in (gh auth login)" >&2; exit 1; }
gh repo view "$REPO" >/dev/null 2>&1 || { echo "cannot access $REPO" >&2; exit 1; }

set_secret() { # name, value
  if [ "$DRY_RUN" = 1 ]; then
    echo "[dry-run] would set $1 on $REPO (${#2} chars)"
  else
    printf '%s' "$2" | gh secret set "$1" --repo "$REPO" >/dev/null
    echo "set $1 on $REPO"
  fi
}

value_of() { # name, prompt: env var if set, otherwise a silent prompt
  local current
  eval "current=\${$1:-}"
  if [ -n "$current" ]; then
    printf '%s' "$current"
    return
  fi
  local entered
  printf '%s: ' "$2" >&2
  IFS= read -r -s entered
  printf '\n' >&2
  [ -n "$entered" ] || { echo "$1 is empty" >&2; exit 1; }
  printf '%s' "$entered"
}

signing_key() {
  if [ -n "${MAVEN_GPG_PRIVATE_KEY:-}" ]; then
    printf '%s' "$MAVEN_GPG_PRIVATE_KEY"
  elif [ -n "${SIGNING_KEY_FILE:-}" ]; then
    cat "$SIGNING_KEY_FILE"
  elif [ -n "${GPG_KEY_ID:-}" ]; then
    gpg --armor --export-secret-keys "$GPG_KEY_ID"
  else
    local path
    printf 'Path to the armored GPG private key file: ' >&2
    IFS= read -r path
    cat "$path"
  fi
}

user="$(value_of MAVEN_CENTRAL_USERNAME 'Central Portal token username')"
pass="$(value_of MAVEN_CENTRAL_PASSWORD 'Central Portal token password')"
key="$(signing_key)"
case "$key" in
  *"BEGIN PGP PRIVATE KEY BLOCK"*) ;;
  *) echo "the signing key is not an armored PGP private key" >&2; exit 1 ;;
esac
keypass="$(value_of MAVEN_GPG_PASSPHRASE 'GPG key passphrase')"

set_secret MAVEN_CENTRAL_USERNAME "$user"
set_secret MAVEN_CENTRAL_PASSWORD "$pass"
set_secret MAVEN_GPG_PRIVATE_KEY "$key"
set_secret MAVEN_GPG_PASSPHRASE "$keypass"

echo
echo "Secrets now on $REPO (names and dates only):"
gh secret list --repo "$REPO"
