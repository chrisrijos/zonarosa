SCHEME=ZonaRosa
PODS=Pods
BACKUP_TESTS=ZonaRosaServiceKit/tests/MessageBackup/ZonaRosa-Message-Backup-Tests

.PHONY: dependencies
dependencies: pod-setup backup-tests-setup fetch-ringrtc

.PHONY: pod-setup
pod-setup:
	git -C ${PODS} clean -xfd
	git -C ${PODS} reset --hard
	./Scripts/setup_private_pods
	git submodule update --init --progress ${PODS}

.PHONY: backup-tests-setup
backup-tests-setup:
	git submodule update --init --progress ${BACKUP_TESTS}

.PHONY: fetch-ringrtc
fetch-ringrtc:
	$(CURDIR)/Pods/ZonaRosaRingRTC/bin/set-up-for-cocoapods
