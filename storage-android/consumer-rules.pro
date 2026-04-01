# -----------------------------------------------------------------------
# Consumer ProGuard / R8 rules for kt-abstract-storage-android
#
# These rules are merged into the consumer app's R8 build automatically
# when this library is included as a dependency.
# -----------------------------------------------------------------------

# Keep every public class in the library's namespace together with all of
# its public and protected members (constructors, methods, fields).
# This prevents R8 from trimming library API that the consumer might not
# reference directly in its own code but that is part of the public contract
# (e.g. open classes meant for subclassing, constructors used reflectively,
# or coroutine-suspend overloads whose call sites are generated at compile time).
-keep public class io.github.ktabstractstorage.** {
    public protected *;
}

