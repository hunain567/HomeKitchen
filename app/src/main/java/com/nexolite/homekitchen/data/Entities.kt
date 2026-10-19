package com.nexolite.homekitchen.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A dish offered on a specific day. [date] is stored as epoch day. Amounts are whole rupees. */
@Entity(tableName = "menu_items", indices = [Index("date")])
data class MenuItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val name: String,
    val price: Long,
    val quantity: Int,
    val sortOrder: Int = 0
)

@Entity(tableName = "customers", indices = [Index("name")])
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = ""
)

object OrderStatus {
    const val PENDING = "Pending"
    const val PACKED = "Packed"
    const val DELIVERED = "Delivered"
}

object Fulfillment {
    const val PICKUP = "Pickup"
    const val DELIVERY = "Delivery"
}

@Entity(
    tableName = "orders",
    indices = [Index("date"), Index("customerId")]
)
data class CustomerOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val orderNo: Int,
    val customerId: Long,
    val fulfillment: String = Fulfillment.PICKUP,
    val note: String = "",
    val status: String = OrderStatus.PENDING,
    val total: Long,
    val paidAmount: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

fun CustomerOrder.balance(): Long = (total - paidAmount).coerceAtLeast(0)

fun CustomerOrder.isPaid(): Boolean = paidAmount >= total

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = CustomerOrder::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId"), Index("menuItemId")]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val menuItemId: Long,
    val name: String,
    val price: Long,
    val qty: Int
)

/** Every rupee received. [orderId] is set when the payment was taken against one order. */
@Entity(tableName = "payments", indices = [Index("date"), Index("customerId")])
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val orderId: Long? = null,
    val amount: Long,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderWithDetails(
    @Embedded val order: CustomerOrder,
    @Relation(parentColumn = "customerId", entityColumn = "id")
    val customer: Customer?,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val items: List<OrderItem>
)

data class BookedQty(val menuItemId: Long, val qty: Int)

data class DishSuggestion(val name: String, val price: Long, val quantity: Int)

data class CustomerDue(
    val id: Long,
    val name: String,
    val phone: String,
    val balance: Long,
    val unpaidCount: Int,
    val lastDate: Long
)

data class DaySale(val day: Long, val amount: Long)

data class DishSale(val name: String, val qty: Int, val amount: Long)
