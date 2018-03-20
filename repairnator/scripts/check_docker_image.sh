#!/usr/bin/env bash

# This script intends to get the digest of the last built image of the indicated repo/tag
REPO="surli/librepair"
TAG="latest"
TOKEN=$(curl -s "https://auth.docker.io/token?service=registry.docker.io&scope=repository:$REPO:pull" | jq -r .token)
curl -I -H "Authorization: Bearer $TOKEN" "https://index.docker.io/v2/$REPO/manifests/$TAG"