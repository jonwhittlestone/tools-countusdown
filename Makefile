# Shortcuts for the common loops. Run `make` (or `make help`) to list them.
#
# Variables you can override on the command line:
#   make run DEVICE=emulator-5554          # pick one device when several are connected
#   make pair ADDR=192.168.0.73:41613      # wireless debugging pairing
#   make run-tv TV=adb-700601250001877     # a TV by mDNS name (see make wifi-devices)

# Per-machine settings, gitignored. Copy local.mk.example to local.mk.
-include local.mk

ANDROID_HOME ?= $(HOME)/Android/Sdk
ADB          := $(ANDROID_HOME)/platform-tools/adb
EMULATOR     := $(ANDROID_HOME)/emulator/emulator
GRADLE       := ./gradlew

APP_ID       := uk.howapped.countusdown
ACTIVITY     := $(APP_ID)/.MainActivity
APK          := app/build/outputs/apk/debug/app-debug.apk

# The Google TV emulator, for when the real TV isn't reachable.
AVD          ?= Television_1080p
EMU_PORT     ?= 5554

# Network TVs, by mDNS name (stable) or ip[:port]; usually set in local.mk.
TV           ?=
FRONT_TV     ?=
WIFI_DEVICE  := ADB=$(ADB) scripts/wifi-device.sh

# Both adb and Gradle's connected tasks respect ANDROID_SERIAL.
ifdef DEVICE
export ANDROID_SERIAL := $(DEVICE)
endif

.DEFAULT_GOAL := help

.PHONY: help
help: ## List the available targets
	@grep -hE '^[a-zA-Z_-]+:.*## |^##@' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*## "} \
			/^##@/ {printf "\n\033[1m%s\033[0m\n", substr($$0, 5); next} \
			{printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

##@ 1. Setup

.PHONY: doctor
doctor: ## Check the JDK, SDK tools, local.properties and local.mk
	@echo "JAVA_HOME = $${JAVA_HOME:-<unset>}"
	@if [ -x "$${JAVA_HOME}/bin/javac" ]; then "$${JAVA_HOME}/bin/javac" -version; \
		else echo "  !! no javac under JAVA_HOME: set it to a full JDK 17 (README)"; fi
	@for t in $(ADB) $(EMULATOR); do \
		if [ -x $$t ]; then echo "ok  $$t"; else echo "!!  missing $$t (README)"; fi; done
	@if [ -f local.properties ]; then echo "ok  local.properties"; \
		else echo "!!  missing local.properties: run 'make local-properties'"; fi
	@if [ -n "$(TV)" ]; then echo "ok  TV = $(TV)"; \
		else echo "--  no TV set: copy local.mk.example to local.mk"; fi

.PHONY: local-properties
local-properties: ## Write local.properties pointing Gradle at the SDK
	echo "sdk.dir=$(ANDROID_HOME)" > local.properties

##@ 2. Build and test

.PHONY: build
build: ## Build the debug APK
	$(GRADLE) assembleDebug
	@echo "APK: $(APK)"

.PHONY: test
test: ## Run the JVM unit tests (no device needed)
	$(GRADLE) testDebugUnitTest

.PHONY: test-device
test-device: ## Run instrumented tests on the connected emulator/TV
	$(GRADLE) connectedDebugAndroidTest

.PHONY: clean
clean: ## Delete build outputs
	$(GRADLE) clean

##@ 3. Emulator

.PHONY: emulator
emulator: ## Start the Google TV emulator (AVD=) and wait until it has booted
	nohup $(EMULATOR) -avd $(AVD) -port $(EMU_PORT) > /dev/null 2>&1 &
	$(ADB) -s emulator-$(EMU_PORT) wait-for-device
	@until [ "$$($(ADB) -s emulator-$(EMU_PORT) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
	@echo "$(AVD) booted as emulator-$(EMU_PORT)"

.PHONY: emulator-stop
emulator-stop: ## Shut down the emulator
	$(ADB) -s emulator-$(EMU_PORT) emu kill

##@ 4. Device

.PHONY: devices
devices: ## List connected devices
	$(ADB) devices -l

.PHONY: install
# adb install rather than `./gradlew installDebug`: Gradle's device-property
# queries time out over wireless debugging (26-app-01 design doc, 03.02).
install: build ## Build and install the debug APK
	$(ADB) install -r $(APK)

.PHONY: launch
launch: ## Start the app (already installed)
	$(ADB) shell am start -n $(ACTIVITY)

.PHONY: run
run: install launch ## Build, install and start the app

.PHONY: stop
stop: ## Force-stop the app
	$(ADB) shell am force-stop $(APP_ID)

.PHONY: logs
logs: ## Follow the app's logcat output (Ctrl-C to quit)
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(APP_ID))

.PHONY: screenshot
screenshot: ## Save a screenshot of the device to screenshot.png
	$(ADB) exec-out screencap -p > screenshot.png
	@echo "Saved screenshot.png"

.PHONY: uninstall
uninstall: ## Remove the app from the device
	$(ADB) uninstall $(APP_ID)

##@ 5. Android TV (over the network)

.PHONY: pair
pair: ## Wireless debugging: pair with ADDR=ip:port from the TV's pairing dialog
	@echo "$(ADDR)" | grep -qE '^[^:]+:[0-9]+$$' || { \
		echo "usage: make pair ADDR=<ip>:<port>  (from 'Pair device with pairing code')"; exit 1; }
	$(ADB) pair $(ADDR)

.PHONY: wifi-devices
wifi-devices: ## List devices advertising wireless debugging, with their mDNS names
	$(ADB) mdns services

.PHONY: tv-connect
tv-connect: ## Connect to the living-room TV (TV=) and print its serial
	@test -n "$(TV)" || { echo "Set TV=<mDNS name or ip>, e.g. in local.mk"; exit 1; }
	@$(WIFI_DEVICE) "$(TV)"

.PHONY: run-tv
run-tv: ## Connect to the living-room TV, then build, install and start the app on it
	@test -n "$(TV)" || { echo "Set TV=<mDNS name or ip>, e.g. in local.mk"; exit 1; }
	@serial=$$($(WIFI_DEVICE) "$(TV)") && $(MAKE) --no-print-directory run DEVICE=$$serial

.PHONY: run-front-tv
run-front-tv: ## Same as run-tv, for the front-room TV (FRONT_TV=)
	@test -n "$(FRONT_TV)" || { echo "Set FRONT_TV=<mDNS name or ip>, e.g. in local.mk"; exit 1; }
	@serial=$$($(WIFI_DEVICE) "$(FRONT_TV)") && $(MAKE) --no-print-directory run DEVICE=$$serial
