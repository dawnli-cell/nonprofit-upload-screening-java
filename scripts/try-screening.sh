#!/usr/bin/env sh
set -eu

: "${IMAGE_PATH:?Set IMAGE_PATH to a nonprofit submission image}"

curl --fail-with-body --request POST "http://localhost:8080/submissions/screen" \
  --form "submissionId=receipt-1042" \
  --form "caption=Receipt from the school supply drive" \
  --form "image=@${IMAGE_PATH}"
