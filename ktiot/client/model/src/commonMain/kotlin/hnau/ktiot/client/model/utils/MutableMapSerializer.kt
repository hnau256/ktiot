package hnau.ktiot.client.model.utils

import arrow.core.identity
import hnau.common.kotlin.mapper.Mapper
import hnau.common.kotlin.serialization.MappingKSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer

class MutableMapSerializer<K, V>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
) : MappingKSerializer<Map<K, V>, MutableMap<K, V>>(
    base = MapSerializer(keySerializer, valueSerializer),
    mapper = Mapper(
        direct = Map<K, V>::toMutableMap,
        reverse = ::identity
    )
)