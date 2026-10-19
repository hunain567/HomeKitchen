package com.nexolite.homekitchen.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KitchenDao {

    // ---------- Menu ----------

    @Query("SELECT * FROM menu_items WHERE date = :date ORDER BY sortOrder, id")
    fun observeMenu(date: Long): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE date = :date ORDER BY sortOrder, id")
    suspend fun getMenu(date: Long): List<MenuItem>

    @Query(
        "SELECT oi.menuItemId AS menuItemId, SUM(oi.qty) AS qty FROM order_items oi " +
            "JOIN orders o ON o.id = oi.orderId WHERE o.date = :date GROUP BY oi.menuItemId"
    )
    fun observeBooked(date: Long): Flow<List<BookedQty>>

    @Query(
        "SELECT name, price, quantity FROM menu_items WHERE id IN " +
            "(SELECT MAX(id) FROM menu_items GROUP BY LOWER(name)) ORDER BY name COLLATE NOCASE"
    )
    fun observeDishSuggestions(): Flow<List<DishSuggestion>>

    @Query("SELECT MAX(date) FROM menu_items WHERE date < :date")
    suspend fun lastMenuDateBefore(date: Long): Long?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM menu_items WHERE date = :date")
    suspend fun nextSortOrder(date: Long): Int

    @Query("SELECT COUNT(*) FROM order_items WHERE menuItemId = :menuItemId")
    suspend fun countOrdersForMenuItem(menuItemId: Long): Int

    @Insert
    suspend fun insertMenuItem(item: MenuItem): Long

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Delete
    suspend fun deleteMenuItem(item: MenuItem)

    // ---------- Customers ----------

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun observeCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findCustomerByName(name: String): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomer(id: Long): Customer?

    @Insert
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT COUNT(*) FROM orders WHERE customerId = :customerId")
    fun observeCustomerOrderCount(customerId: Long): Flow<Int>

    // ---------- Orders ----------

    @Transaction
    @Query("SELECT * FROM orders WHERE date = :date ORDER BY orderNo DESC")
    fun observeOrders(date: Long): Flow<List<OrderWithDetails>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderDetails(id: Long): OrderWithDetails?

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrder(id: Long): CustomerOrder?

    @Query("SELECT COALESCE(MAX(orderNo), 0) + 1 FROM orders WHERE date = :date")
    suspend fun nextOrderNo(date: Long): Int

    @Insert
    suspend fun insertOrder(order: CustomerOrder): Long

    @Update
    suspend fun updateOrder(order: CustomerOrder)

    @Delete
    suspend fun deleteOrder(order: CustomerOrder)

    @Insert
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Long)

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("SELECT * FROM orders WHERE customerId = :customerId AND total > paidAmount ORDER BY date, orderNo")
    suspend fun unpaidOrders(customerId: Long): List<CustomerOrder>

    // ---------- Payments ----------

    @Insert
    suspend fun insertPayment(payment: Payment): Long

    @Query("DELETE FROM payments WHERE orderId = :orderId")
    suspend fun deletePaymentsForOrder(orderId: Long)

    // ---------- Dues ----------

    @Query(
        "SELECT c.id AS id, c.name AS name, c.phone AS phone, " +
            "SUM(o.total - o.paidAmount) AS balance, COUNT(o.id) AS unpaidCount, MAX(o.date) AS lastDate " +
            "FROM customers c JOIN orders o ON o.customerId = c.id " +
            "WHERE o.total > o.paidAmount GROUP BY c.id ORDER BY balance DESC"
    )
    fun observeDues(): Flow<List<CustomerDue>>

    // ---------- Reports ----------

    @Query("SELECT COALESCE(SUM(total), 0) FROM orders WHERE date BETWEEN :from AND :to")
    fun observeSale(from: Long, to: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM orders WHERE date BETWEEN :from AND :to")
    fun observeOrderCount(from: Long, to: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE date BETWEEN :from AND :to")
    fun observeReceived(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(total - paidAmount), 0) FROM orders WHERE date BETWEEN :from AND :to AND total > paidAmount")
    fun observeUnpaid(from: Long, to: Long): Flow<Long>

    @Query("SELECT date AS day, SUM(total) AS amount FROM orders WHERE date BETWEEN :from AND :to GROUP BY date")
    fun observeDailySales(from: Long, to: Long): Flow<List<DaySale>>

    @Query(
        "SELECT oi.name AS name, SUM(oi.qty) AS qty, SUM(oi.qty * oi.price) AS amount FROM order_items oi " +
            "JOIN orders o ON o.id = oi.orderId WHERE o.date BETWEEN :from AND :to " +
            "GROUP BY LOWER(oi.name) ORDER BY qty DESC LIMIT 5"
    )
    fun observeTopDishes(from: Long, to: Long): Flow<List<DishSale>>

    // ---------- Backup ----------

    @Query("SELECT * FROM menu_items") suspend fun allMenuItems(): List<MenuItem>
    @Query("SELECT * FROM customers") suspend fun allCustomers(): List<Customer>
    @Query("SELECT * FROM orders") suspend fun allOrders(): List<CustomerOrder>
    @Query("SELECT * FROM order_items") suspend fun allOrderItems(): List<OrderItem>
    @Query("SELECT * FROM payments") suspend fun allPayments(): List<Payment>

    @Insert suspend fun insertMenuItems(items: List<MenuItem>)
    @Insert suspend fun insertCustomers(items: List<Customer>)
    @Insert suspend fun insertOrders(items: List<CustomerOrder>)
    @Insert suspend fun insertPayments(items: List<Payment>)

    @Query("DELETE FROM order_items") suspend fun clearOrderItems()
    @Query("DELETE FROM orders") suspend fun clearOrders()
    @Query("DELETE FROM payments") suspend fun clearPayments()
    @Query("DELETE FROM customers") suspend fun clearCustomers()
    @Query("DELETE FROM menu_items") suspend fun clearMenuItems()
}
