package moe.shizuku.manager.core.extensions

inline val <reified T> T.TAG: String
    get() =
        when {
            T::class.java.isAnonymousClass -> T::class.java.name
            else -> T::class.java.simpleName
        }
