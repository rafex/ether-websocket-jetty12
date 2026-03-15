# Set the directory for this project so make deploy need not receive PROJECT_DIR
PROJECT_DIR         := ether-websocket-jetty12
PROJECT_GROUP_ID    := dev.rafex.ether.websocket
PROJECT_ARTIFACT_ID := ether-websocket-jetty12
DEPENDENCY_COORDS   := ether-websocket-core.version=dev.rafex.ether.websocket:ether-websocket-core

# Include shared build logic
include ../build-helpers/common.mk
include ../build-helpers/git.mk
