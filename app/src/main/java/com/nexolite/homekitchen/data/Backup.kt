package com.nexolite.homekitchen.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Converts the whole database to and from a single JSON document. */
object Backup {

    private const val FORMAT_VERSION = 1

    suspend fun export(db: AppDatabase): String {
        val dao = db.dao()
        val root = JSONObject()
        root.put("app", "HomeKitchen")
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("menuItems", JSONArray().apply {
            dao.allMenuItems().forEach {
                put(JSONObject()
                    .put("id", it.id).put("date", it.date).put("name", it.name)
                    .put("price", it.price).put("quantity", it.quantity).put("sortOrder", it.sortOrder))
            }
        })
        root.put("customers", JSONArray().apply {
            dao.allCustomers().forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone))
            }
        })
        root.put("orders", JSONArray().apply {
            dao.allOrders().forEach {
                put(JSONObject()
                    .put("id", it.id).put("date", it.date).put("orderNo", it.orderNo)
                    .put("customerId", it.customerId).put("fulfillment", it.fulfillment)
                    .put("note", it.note).put("status", it.status).put("total", it.total)
                    .put("paidAmount", it.paidAmount).put("createdAt", it.createdAt))
            }
        })
        root.put("orderItems", JSONArray().apply {
            dao.allOrderItems().forEach {
                put(JSONObject()
                    .put("id", it.id).put("orderId", it.orderId).put("menuItemId", it.menuItemId)
                    .put("name", it.name).put("price", it.price).put("qty", it.qty))
            }
        })
        root.put("payments", JSONArray().apply {
            dao.allPayments().forEach {
                put(JSONObject()
                    .put("id", it.id).put("customerId", it.customerId)
                    .put("orderId", it.orderId ?: JSONObject.NULL)
                    .put("amount", it.amount).put("date", it.date).put("createdAt", it.createdAt))
            }
        })
        return root.toString()
    }

    /** Replaces all data with the backup. Throws [IllegalArgumentException] for a file that is not a valid backup. */
    suspend fun restore(db: AppDatabase, json: String) {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("This file is not a Home Kitchen backup.")
        }
        require(root.optString("app") == "HomeKitchen") { "This file is not a Home Kitchen backup." }

        val menuItems = root.getJSONArray("menuItems").objects().map {
            MenuItem(it.getLong("id"), it.getLong("date"), it.getString("name"),
                it.getLong("price"), it.getInt("quantity"), it.optInt("sortOrder"))
        }
        val customers = root.getJSONArray("customers").objects().map {
            Customer(it.getLong("id"), it.getString("name"), it.optString("phone"))
        }
        val orders = root.getJSONArray("orders").objects().map {
            CustomerOrder(
                id = it.getLong("id"), date = it.getLong("date"), orderNo = it.getInt("orderNo"),
                customerId = it.getLong("customerId"), fulfillment = it.optString("fulfillment", Fulfillment.PICKUP),
                note = it.optString("note"), status = it.optString("status", OrderStatus.PENDING),
                total = it.getLong("total"), paidAmount = it.getLong("paidAmount"),
                createdAt = it.optLong("createdAt")
            )
        }
        val orderItems = root.getJSONArray("orderItems").objects().map {
            OrderItem(it.getLong("id"), it.getLong("orderId"), it.getLong("menuItemId"),
                it.getString("name"), it.getLong("price"), it.getInt("qty"))
        }
        val payments = root.getJSONArray("payments").objects().map {
            Payment(
                id = it.getLong("id"), customerId = it.getLong("customerId"),
                orderId = if (it.isNull("orderId")) null else it.getLong("orderId"),
                amount = it.getLong("amount"), date = it.getLong("date"), createdAt = it.optLong("createdAt")
            )
        }

        val dao = db.dao()
        db.withTransaction {
            dao.clearOrderItems()
            dao.clearOrders()
            dao.clearPayments()
            dao.clearCustomers()
            dao.clearMenuItems()
            dao.insertMenuItems(menuItems)
            dao.insertCustomers(customers)
            dao.insertOrders(orders)
            dao.insertOrderItems(orderItems)
            dao.insertPayments(payments)
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
