#!/usr/bin/env bash
# Run from repository root. Never publish raw reporters.
set -uo pipefail
jar=${AUDIT_JAR:-target/gerador-docs-tests-3.0.0.jar}
mkdir -p audit-output audit-artifacts
# A dedicated output folder avoids publishing outputs from unrelated runs.
run_dir=$(mktemp -d audit-output/ci-XXXXXXXX)
java_cmd=java
if [ -n "${JAVA_HOME:-}" ]; then java_cmd="$JAVA_HOME/bin/java"; fi
"$java_cmd" -jar "$jar" "$@" --output-dir "$run_dir"
audit_status=$?
find "$run_dir" -maxdepth 1 -type f \( -name '*.pdf' -o -name '*.audit-run.json' \) -exec cp -- '{}' audit-artifacts/ \;
publish_status=$?
if [ "$publish_status" -ne 0 ]; then audit_status=2; fi
printf '%s\n' "$audit_status" > audit-artifacts/exit-code.txt
exit "$audit_status"
