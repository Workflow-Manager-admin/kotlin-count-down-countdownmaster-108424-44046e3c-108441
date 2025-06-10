#!/bin/bash
cd /home/kavia/workspace/code-generation/countdownmaster-108424-44046e3c/countdown_timer_app
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

