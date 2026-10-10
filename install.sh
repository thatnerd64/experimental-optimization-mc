#!/bin/sh
# Install or update a prebuilt native server from this repo's GitHub Releases.
#
#   curl -fsSL https://raw.githubusercontent.com/thatnerd64/experimental-optimization-mc/main/install.sh | sh -s -- direwolf20
#   sh install.sh <pack> [dir]
#
# Packs: direwolf20, ftb-ultimate. The default dir is ./<pack>-server.
# Re-running it on an existing install updates the binary, libraries and mods, and keeps
# worlds, server.properties, config/ and anything else not shipped in the bundle.
# Then start the server with ./start.sh in that dir, exactly like `java -jar server.jar nogui`.
set -eu

REPO_URL=${REPO_URL:-https://github.com/thatnerd64/experimental-optimization-mc/releases/download}

usage() {
    echo "usage: install.sh <direwolf20|ftb-ultimate> [dir]" >&2
    exit 2
}

[ $# -ge 1 ] || usage
PACK=$1
case "$PACK" in
    direwolf20)
        TAG=direwolf20-5.3.2-graalvm-v1
        ASSET=dw20-native-linux-x86_64.tar.gz
        BIN=dw20-server ;;
    ftb-ultimate)
        TAG=ftb-ultimate-1.1.2-graalvm-v1
        ASSET=ultimate-native-linux-x86_64.tar.gz
        BIN=ultimate-server ;;
    *) usage ;;
esac
DIR=${2:-./$PACK-server}

case "$(uname -s)/$(uname -m)" in
    Linux/x86_64|Linux/amd64) ;;
    *) echo "These builds run on Linux x86_64 only (this is $(uname -s)/$(uname -m))." >&2; exit 1 ;;
esac
for tool in curl tar sha256sum; do
    command -v "$tool" >/dev/null 2>&1 || { echo "install.sh needs $tool" >&2; exit 1; }
done

mkdir -p "$DIR"
DIR=$(cd "$DIR" && pwd)
TMP=$(mktemp -d "$DIR/.install.XXXXXX")
trap 'rm -rf "$TMP"' EXIT INT TERM

echo "Downloading $ASSET ($TAG)..."
curl -fL --progress-bar "$REPO_URL/$TAG/$ASSET" -o "$TMP/$ASSET"
curl -fsSL "$REPO_URL/$TAG/$ASSET.sha256" -o "$TMP/$ASSET.sha256"
(cd "$TMP" && sha256sum -c "$ASSET.sha256" >/dev/null) || { echo "Checksum mismatch for $ASSET" >&2; exit 1; }

mkdir "$TMP/x"
tar -xzf "$TMP/$ASSET" -C "$TMP/x" --strip-components=1

# Switching an existing dir to another pack: its config/ belongs to the old mods, so replace it.
SWITCH=0
for other in dw20-server ultimate-server; do
    if [ "$other" != "$BIN" ] && [ -e "$DIR/$other" ]; then
        SWITCH=1
        rm -f "$DIR/$other"
    fi
done
[ "$SWITCH" = 0 ] || echo "Replacing another pack's install; its config/ is replaced and old worlds will not load cleanly."

# Move each bundled top-level entry into place. server.properties and config/ are the user's
# once they exist; everything else in the bundle is replaced so it matches the binary.
for src in "$TMP/x"/* "$TMP/x"/.[!.]*; do
    [ -e "$src" ] || continue
    name=${src##*/}
    case "$name" in
        server.properties|config)
            if [ -e "$DIR/$name" ] && { [ "$SWITCH" = 0 ] || [ "$name" = server.properties ]; }; then
                echo "Keeping existing $name"
                continue
            fi ;;
    esac
    rm -rf "$DIR/$name"
    mv "$src" "$DIR/$name"
done
chmod +x "$DIR/$BIN" "$DIR/start.sh"
# A pack-independent name for panels and service files.
ln -sf "$BIN" "$DIR/server"
echo "$TAG" > "$DIR/.installed-release"

echo
echo "Installed $TAG in $DIR"
echo "Start it with:  cd \"$DIR\" && ./start.sh"
