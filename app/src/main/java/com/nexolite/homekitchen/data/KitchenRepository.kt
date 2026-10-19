package com.nexolite.homekitchen.data

import androidx.room.withTransaction

data class OrderLine(val menuItem: MenuItem, val qty: Int)

data class OrderInput(
    val editingId: Long?,
    val date: Long,
    val customerName: String,
    val phone: String,
    val lines: List<OrderLine>,
    val fulfillment: String,
    val note: String,
    val paid: Boolean
)

class KitchenRepository(private val db: AppDatabase) {

    val dao: KitchenDao = db.dao()

    // ---------- Menu ----------

    suspend fun addMenuItem(date: Long, name: String, price: Long, quantity: Int) {
        dao.insertMenuItem(
            MenuItem(
                date = date,
                name = name.trim(),
                price = price,
                quantity = quantity,
                sortOrder = dao.nextSortOrder(date)
            )
        )
    }

    suspend fun updateMenuItem(item: MenuItem) = dao.updateMenuItem(item.copy(name = item.name.trim()))

    /** Returns false when the dish already has orders and therefore cannot be removed. */
    suspend fun deleteMenuItem(item: MenuItem): Boolean {
        if (dao.countOrdersForMenuItem(item.id) > 0) return false
        dao.deleteMenuItem(item)
        return true
    }

    /** Copies the most recent earlier menu onto [date]. Returns the number of dishes copied. */
    suspend fun copyPreviousMenu(date: Long): Int = db.withTransaction {
        val source = dao.lastMenuDateBefore(date) ?: return@withTransaction 0
        val existing = dao.getMenu(date).map { it.name.lowercase() }.toSet()
        var order = dao.nextSortOrder(date)
        var copied = 0
        dao.getMenu(source).forEach { item ->
            if (item.name.lowercase() !in existing) {
                dao.insertMenuItem(item.copy(id = 0, date = date, sortOrder = order++))
                copied++
            }
        }
        copied
    }

    // ---------- Orders ----------

    suspend fun saveOrder(input: OrderInput): Long = db.withTransaction {
        val customerId = resolveCustomer(input.customerName, input.phone)
        val lines = input.lines.filter { it.qty > 0 }
        val total = lines.sumOf { it.menuItem.price * it.qty }
        val previous = input.editingId?.let { dao.getOrder(it) }

        val orderId: Long
        val oldPaid: Long
        val wasFullyPaid: Boolean
        if (previous == null) {
            oldPaid = 0
            wasFullyPaid = false
            orderId = dao.insertOrder(
                CustomerOrder(
                    date = input.date,
                    orderNo = dao.nextOrderNo(input.date),
                    customerId = customerId,
                    fulfillment = input.fulfillment,
                    note = input.note.trim(),
                    total = total
                )
            )
        } else {
            oldPaid = previous.paidAmount
            wasFullyPaid = previous.isPaid()
            orderId = previous.id
            dao.updateOrder(
                previous.copy(
                    customerId = customerId,
                    fulfillment = input.fulfillment,
                    note = input.note.trim(),
                    total = total
                )
            )
            dao.deleteOrderItems(orderId)
        }

        dao.insertOrderItems(
            lines.map {
                OrderItem(
                    orderId = orderId,
                    menuItemId = it.menuItem.id,
                    name = it.menuItem.name,
                    price = it.menuItem.price,
                    qty = it.qty
                )
            }
        )

        val newPaid = when {
            input.paid -> total
            wasFullyPaid -> 0L
            else -> minOf(oldPaid, total)
        }
        val delta = newPaid - oldPaid
        if (delta != 0L) {
            // A negative entry records money returned, so "Received" in reports stays correct.
            dao.insertPayment(Payment(customerId = customerId, orderId = orderId, amount = delta, date = input.date))
        }
        val saved = dao.getOrder(orderId)!!
        dao.updateOrder(saved.copy(paidAmount = newPaid))
        orderId
    }

    private suspend fun resolveCustomer(name: String, phone: String): Long {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val existing = dao.findCustomerByName(cleanName)
        return if (existing != null) {
            if (cleanPhone.isNotEmpty() && cleanPhone != existing.phone) {
                dao.updateCustomer(existing.copy(phone = cleanPhone))
            }
            existing.id
        } else {
            dao.insertCustomer(Customer(name = cleanName, phone = cleanPhone))
        }
    }

    suspend fun deleteOrder(orderId: Long) = db.withTransaction {
        val order = dao.getOrder(orderId) ?: return@withTransaction
        dao.deletePaymentsForOrder(orderId)
        dao.deleteOrder(order)
    }

    suspend fun advanceStatus(order: CustomerOrder) {
        val next = when (order.status) {
            OrderStatus.PENDING -> OrderStatus.PACKED
            OrderStatus.PACKED -> OrderStatus.DELIVERED
            else -> return
        }
        dao.setStatus(order.id, next)
    }

    suspend fun setStatus(orderId: Long, status: String) = dao.setStatus(orderId, status)

    suspend fun markPaid(orderId: Long, today: Long) = db.withTransaction {
        val order = dao.getOrder(orderId) ?: return@withTransaction
        val due = order.balance()
        if (due <= 0) return@withTransaction
        dao.insertPayment(Payment(customerId = order.customerId, orderId = order.id, amount = due, date = today))
        dao.updateOrder(order.copy(paidAmount = order.total))
    }

    /** Records a payment from a customer and settles their oldest unpaid orders first. */
    suspend fun receivePayment(customerId: Long, amount: Long, today: Long) = db.withTransaction {
        if (amount <= 0) return@withTransaction
        dao.insertPayment(Payment(customerId = customerId, amount = amount, date = today))
        var remaining = amount
        for (order in dao.unpaidOrders(customerId)) {
            if (remaining <= 0) break
            val applied = minOf(order.balance(), remaining)
            dao.updateOrder(order.copy(paidAmount = order.paidAmount + applied))
            remaining -= applied
        }
    }
}
