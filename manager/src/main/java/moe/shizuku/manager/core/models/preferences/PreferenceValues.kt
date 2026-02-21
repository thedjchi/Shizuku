package moe.shizuku.manager.core.models.preferences

import kotlin.reflect.KClass

interface IntEnum {
    val value: Int
    val default: IntEnum
}

// Define IntEnum::class.defaultValue property
inline val <reified T> KClass<T>.defaultValue
    where T : Enum<T>, T : IntEnum
    get() = enumValues<T>().first().default.value

enum class StartMode(override val value: Int): IntEnum {
    PC(0),
    WADB(1),
    ROOT(2);

    override val default: IntEnum
        get() = PC
}

enum class Theme(override val value: Int): IntEnum {
    LIGHT(1),
    DARK(2),
    SYSTEM(-1);

    override val default: IntEnum
        get() = SYSTEM
}

enum class UpdateChannel(override val value: Int): IntEnum {
    STABLE(0),
    BETA(1);

    override val default: IntEnum
        get() = STABLE
}