# Prevent native methods from being renamed as long as they're used.
-keepclasseswithmembernames,includedescriptorclasses class io.zonarosa.libzonarosa.** {
    native <methods>;
}

# Keep members that the Rust library accesses directly on a variety of classes.
-keepclassmembers class io.zonarosa.libzonarosa.** {
    # Accessed by Rust code to retrieve a pointer to a wrapped Rust type.
    long unsafeHandle;
    # Called by Rust code to construct a type that wraps a Rust pointer.
    <init>(long);
}

## Handling for the @CalledFromNative annotation:
#
# The annotation can (should) be attached to anything that is accessed from
# native code. The simple case is methods and fields that are accessed directly
# via JNI.
-keepclassmembers,includedescriptorclasses class io.zonarosa.libzonarosa.** {
    @io.zonarosa.libzonarosa.internal.CalledFromNative *;
}

# Native code can access methods on objects whose classes are defined outside
# this library but that implement an interface in this library. Those methods
# need to be preserved since the call sites to them are invisible. We mark
# those methods for keeping, which in turn prevents their implementations
# on other classes from being stripped.
-keepclassmembers @io.zonarosa.libzonarosa.internal.CalledFromNative interface io.zonarosa.libzonarosa.** {
    *;
}

# Native code might construct instances of classes that are otherwise unused
# (like exceptions). Prevent these from being removed, but don't say anything
# about the methods that are called. The ones called by native code should be
# annotated separately.
-keep @io.zonarosa.libzonarosa.internal.CalledFromNative class io.zonarosa.libzonarosa.**

# As a convenience, enums with @CalledFromNative keep all their values.
-keep @io.zonarosa.libzonarosa.internal.CalledFromNative enum io.zonarosa.libzonarosa.** {
    <fields>;
}

# Keep constructors for all our exceptions.
# (This could be more fine-grained but doesn't really have to be.)
-keep,includedescriptorclasses class io.zonarosa.libzonarosa.** extends java.lang.Exception {
    <init>(...);
}

# Keep some types that the Rust library constructs unconditionally.
# (The constructors are covered by the above -keepclassmembers)
-keep class io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
-keep class io.zonarosa.libzonarosa.protocol.message.* implements io.zonarosa.libzonarosa.protocol.message.CiphertextMessage

# Keep names for store-related types, and the members used from the Rust library not covered above.
# (Thus, if you don't use a store, it won't be kept.)
-keepnames interface io.zonarosa.libzonarosa.**.*Store { *; }

-keepnames enum io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore$Direction { *; }
-keepnames class io.zonarosa.libzonarosa.**.*Record

# Keep rustls-platform-verifier classes
-keep, includedescriptorclasses class org.rustls.platformverifier.** { *; }

# Keep kotlin.Pair's constructor
-keep class kotlin.Pair {
    <init>(...);
}
