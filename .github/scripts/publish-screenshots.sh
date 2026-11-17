#!/usr/bin/env bash
# Publishes the Roborazzi screenshots of a pull request to the `ci-screenshots`
# branch and posts (or updates) one PR comment that shows them inline, with a
# link to the run for the APK. Needs: GH_TOKEN, REPO, PR, SHA, RUN_URL.
set -euo pipefail

SRC="${GITHUB_WORKSPACE:-$PWD}/app/build/outputs/roborazzi"
BRANCH=ci-screenshots
MARKER="<!-- ci-screenshots -->"

shopt -s nullglob
shots=("$SRC"/*.png)
if [ ${#shots[@]} -eq 0 ]; then
  echo "No screenshots found in $SRC"
  exit 0
fi

short_sha="${SHA:0:7}"
dest="pr-$PR/$short_sha"
work="$(mktemp -d)"
cd "$work"
git init -q
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git remote add origin "https://x-access-token:${GH_TOKEN}@github.com/${REPO}.git"

for attempt in 1 2 3; do
  if git fetch -q --depth 1 origin "$BRANCH" 2>/dev/null; then
    git checkout -q -B "$BRANCH" FETCH_HEAD
  else
    git checkout -q --orphan "$BRANCH"
    git rm -rq --cached . 2>/dev/null || true
  fi
  mkdir -p "$dest"
  cp "${shots[@]}" "$dest/"
  git add "$dest"
  git commit -q -m "Screenshots for PR #$PR at $short_sha" || true
  if git push -q origin "$BRANCH"; then
    break
  fi
  echo "Push failed (attempt $attempt), retrying"
  sleep $((attempt * 3))
done

raw="https://raw.githubusercontent.com/${REPO}/${BRANCH}/${dest}"
body="$MARKER
### Screenshots at \`$short_sha\`
"
for f in "${shots[@]}"; do
  name="$(basename "$f" .png)"
  body+="
**$name**

![$name]($raw/$name.png)
"
done
body+="
APK: download \`rowerdisplay-debug-apk\` from the [CI run]($RUN_URL)."

existing="$(gh api "repos/$REPO/issues/$PR/comments" --paginate \
  --jq ".[] | select(.body | startswith(\"$MARKER\")) | .id" | head -n 1)"
if [ -n "$existing" ]; then
  gh api -X PATCH "repos/$REPO/issues/comments/$existing" -f body="$body" > /dev/null
else
  gh api "repos/$REPO/issues/$PR/comments" -f body="$body" > /dev/null
fi
echo "Published ${#shots[@]} screenshots to $raw"
