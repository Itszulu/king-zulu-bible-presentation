package com.kingzulu.biblepresentation

import java.util.UUID

data class ServicePlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<ServiceItem> = emptyList()
) {
    fun add(item: ServiceItem) = copy(items = items + item)
    fun remove(id: String) = copy(items = items.filterNot { it.id == id })
    fun move(from: Int, to: Int): ServicePlan {
        if (from !in items.indices || to !in items.indices) return this
        val list = items.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        return copy(items = list)
    }
}
