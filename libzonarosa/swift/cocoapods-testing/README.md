This directory exists for the Slow Tests to generate a fake project that depends on LibZonaRosaClient, very similar to what `pod lib lint` does. By doing it manually, we can ensure that we build for device as well as simulator.

```shell
% rm -rf Pods Testing.xcworkspace
% pod install
% xcodebuild -scheme LibZonaRosaClient -sdk iphoneos build-for-testing
```
