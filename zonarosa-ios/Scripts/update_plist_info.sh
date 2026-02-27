#!/bin/sh

set -e

# PROJECT_DIR will be set when run from xcode, else we infer it
if [ "${PROJECT_DIR}" = "" ]; then
    PROJECT_DIR=`git rev-parse --show-toplevel`
    echo "inferred ${PROJECT_DIR}"
fi

# Capture project hashes that we want to add to the Info.plist
cd $PROJECT_DIR
_git_commit_zonarosa=`git log --pretty=oneline --decorate=no | head -1`

# Remove existing .plist entry, if any.
/usr/libexec/PlistBuddy -c "Delete BuildDetails" ZonaRosa/ZonaRosa-Info.plist || true
# Add new .plist entry.
/usr/libexec/PlistBuddy -c "add BuildDetails dict" ZonaRosa/ZonaRosa-Info.plist

echo "CONFIGURATION: ${CONFIGURATION}"
if [ "${CONFIGURATION}" = "App Store Release" ]; then
    /usr/libexec/PlistBuddy -c "add :BuildDetails:XCodeVersion string '${XCODE_VERSION_MAJOR}.${XCODE_VERSION_MINOR}'" ZonaRosa/ZonaRosa-Info.plist
    /usr/libexec/PlistBuddy -c "add :BuildDetails:ZonaRosaCommit string '$_git_commit_zonarosa'" ZonaRosa/ZonaRosa-Info.plist

    # Use UTC
    _build_datetime=`date -u`
    /usr/libexec/PlistBuddy -c "add :BuildDetails:DateTime string '$_build_datetime'" ZonaRosa/ZonaRosa-Info.plist

    _build_timestamp=`date +%s`
    /usr/libexec/PlistBuddy -c "add :BuildDetails:Timestamp integer $_build_timestamp" ZonaRosa/ZonaRosa-Info.plist
fi
