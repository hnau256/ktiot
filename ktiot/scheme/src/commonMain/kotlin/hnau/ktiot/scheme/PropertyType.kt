package hnau.ktiot.scheme

import arrow.core.NonEmptyList
import arrow.core.serialization.NonEmptyListSerializer
import hnau.common.kotlin.serialization.ClosedFloatingPointRangeSerializer
import hnau.common.model.color.RGBABytes
import hnau.common.model.color.gradient.Gradient
import hnau.common.model.color.gradient.create
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer

@Serializable
sealed interface PropertyType<T> {

    @Transient
    val serializer: KSerializer<T>

    @Serializable
    sealed interface State<T> : PropertyType<T> {

        @Serializable
        @SerialName("number")
        data class Number(
            val suffix: String = "",
        ) : State<Float> {

            override val serializer: KSerializer<Float>
                get() = Float.serializer()
        }

        @Serializable
        @SerialName("text")
        data object Text : State<String> {

            override val serializer: KSerializer<String>
                get() = String.serializer()
        }

        @Serializable
        @SerialName("flag")
        data object Flag : State<Boolean> {

            override val serializer: KSerializer<Boolean>
                get() = Boolean.serializer()
        }

        @Serializable
        @SerialName("rgb")
        data object RGB : State<RGBABytes> {

            override val serializer: KSerializer<RGBABytes>
                get() = RGBABytes.serializer()
        }

        @Serializable
        @SerialName("enum")
        data class Enum(
            @Serializable(NonEmptyListSerializer::class)
            val variants: NonEmptyList<Variant>,
        ) : State<String> {

            constructor(
                firstVariant: String,
                vararg otherVariants: String,
            ) : this(
                variants = NonEmptyList(
                    head = firstVariant,
                    tail = otherVariants.toList(),
                ).map { key ->
                    Variant(
                        key = key,
                    )
                }
            )

            @Serializable
            data class Variant(
                val key: String,
                val customTitle: String? = null,
            )

            override val serializer: KSerializer<String>
                get() = String.serializer()
        }

        @Serializable
        @SerialName("timestamp")
        data object Timestamp : State<Instant> {

            override val serializer: KSerializer<Instant>
                get() = Instant.serializer()
        }

        @Serializable
        @SerialName("fraction")
        data class Fraction(
            @Serializable(ClosedFloatingPointRangeSerializer.Float::class)
            val range: ClosedFloatingPointRange<Float> = 0f..1f,
            val display: Display = Display.Percent,
            val gradient: Gradient<RGBABytes> =
                Gradient.create(RGBABytes.Black, RGBABytes.White),
        ) : State<Float> {

            @Serializable
            sealed interface Display {

                @Serializable
                @SerialName("percent")
                data object Percent : Display

                @Serializable
                @SerialName("raw")
                data class Raw(
                    val suffix: String = "",
                    val decimalPlaces: Int = 0,
                ) : Display
            }

            override val serializer: KSerializer<Float>
                get() = Float.serializer()
        }
    }

    @Serializable
    sealed interface Events<T> : PropertyType<T> {

        @Serializable
        @SerialName("tic")
        data object Tic : Events<Unit> {

            override val serializer: KSerializer<Unit>
                get() = Unit.serializer()
        }
    }
}
