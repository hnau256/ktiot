package hnau.ktiot.coordinator.property

import hnau.ktiot.scheme.PropertyType

fun <T, P : PropertyType<T>> PropertyPrototype<T, P>.calculated(): Property<T, P, Property.Direction.Calculated> = Property(
    topic = topic,
    type = type,
    direction = Property.Direction.Calculated,
)

fun <T, P : PropertyType<T>> PropertyPrototype<T, P>.`in`(
    origin: InPropertyOrigin,
): Property<T, P, Property.Direction.In> = Property(
    topic = topic,
    type = type,
    direction = Property.Direction.In(
        origin = origin,
    ),
)

fun <T, P : PropertyType<T>> PropertyPrototype<T, P>.manual(): Property<T, P, Property.Direction.In> =
    `in`(origin = InPropertyOrigin.Manual)

fun <T, P : PropertyType<T>> PropertyPrototype<T, P>.hardware(): Property<T, P, Property.Direction.In> =
    `in`(origin = InPropertyOrigin.Hardware)