#!/usr/bin/env bash

export DISPLAY=:20
export PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
#installCmd="npm install && npx playwright@1.24.0 install"
# SSH_CONNECTION variable contains four space-separated values: client IP(f1),
# client port(f2), server IP(f3), and server port(f4). e.g. 172.19.172.208 39758 172.19.166.137 22
export UNIQUE_ID="$(cut -d' ' -f2 <<<"$SSH_CONNECTION")"-"$(cut -d' ' -f3 <<<"$SSH_CONNECTION")"
echo "UNIQUE_ID: $UNIQUE_ID"
export HEADLESS=true
export CI=true
export NODE_OPTIONS="--max-old-space-size=4096"
echo "NODE_OPTIONS: $NODE_OPTIONS"
versionCmd="npx playwright --version"
testCmd="npx playwright test"
grep=""

# Assumes running in blink directory
cd ../blink-playwright
echo -n "Running in directory:"
pwd

POSITIONAL_ARGS=()
while [[ $# -gt 0 ]]; do
    case $1 in
        --baseUrl=*)
            export BASE_URL="${1#*=}"
            echo "BASE_URL (env): $BASE_URL"
            shift
            ;;
        --username=*)
            export TS_USERNAME="${1#*=}"
            echo "TS_USERNAME: $TS_USERNAME"
            shift
            ;;
        --password=*)
            export TS_PASSWORD="${1#*=}"
            echo "TS_PASSWORD: $TS_PASSWORD"
            shift
            ;;
        --url-flags=*)
            export URL_FLAGS="${1#*=}"
            echo "URL_FLAGS: $URL_FLAGS"
            shift
            ;;
        --spec-file=*)
            cmd+=" ${1#*=}"
            spec="${1#*=}"
            shift
            ;;
        --embed-mode=*)
            export EMBED_MODE="${1#*=}"
            echo "EMBED_MODE: $EMBED_MODE"
            shift
            ;;
        --generate-tia-artifacts)
            export GENERATE_TIA_ARTIFACTS=true
            echo "GENERATE_TIA_ARTIFACTS: $GENERATE_TIA_ARTIFACTS"
            shift
            ;;
        --spec-name=*)
            grep="${1#*=}"
            shift
            ;;
        --navigation-timeout=*)
            export NAVIGATION_TIMEOUT="${1#*=}"
            echo "NAVIGATION_TIMEOUT (env): $NAVIGATION_TIMEOUT"
            shift
            ;;
        --action-timeout=*)
            export ACTION_TIMEOUT="${1#*=}"
            echo "ACTION_TIMEOUT (env): $ACTION_TIMEOUT"
            shift
            ;;
        --test-timeout=*)
            export TEST_TIMEOUT="${1#*=}"
            echo "TEST_TIMEOUT (env): $TEST_TIMEOUT"
            shift
            ;;
        #TODO: Remove this variable once the embed pipeline is made to support the latest react version.
        --blink-version=*)
            export BLINK_VERSION="${1#*=}"
            echo "BLINK_VERSION (env): $BLINK_VERSION"
            shift
            ;;
        *)
            POSITIONAL_ARGS+=("$1") # save positional arg
            shift
            ;;
    esac
done

echo "\n\nIgnoring parameters: $cmd ${POSITIONAL_ARGS[@]}"
#echo "\nRunning install cmd: $installCmd"
#$installCmd
echo "\nPlaywright Version: $versionCmd"
$versionCmd
# Escape regex special characters using sed
escapedGrep=$(
    echo "$grep" | sed 's/[][()\.+*?{}\\]/\\&/g')
# Print the escaped grep pattern
echo "Escaped grep pattern: $escapedGrep"

echo "\nRunning test cmd: $testCmd \"$spec\" --grep \"$escapedGrep\""
$testCmd "$spec" --grep "$escapedGrep"
