package com.nexolite.homekitchen.ui

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexolite.homekitchen.data.AppDatabase
import com.nexolite.homekitchen.data.Backup
import com.nexolite.homekitchen.data.Customer
import com.nexolite.homekitchen.data.CustomerDue
import com.nexolite.homekitchen.data.CustomerOrder
import com.nexolite.homekitchen.data.DishSale
import com.nexolite.homekitchen.data.DishSuggestion
import com.nexolite.homekitchen.data.KitchenRepository
import com.nexolite.homekitchen.data.MenuItem
import com.nexolite.homekitchen.data.OrderInput
import com.nexolite.homekitchen.data.OrderWithDetails
import com.nexolite.homekitchen.data.Prefs
import com.nexolite.homekitchen.data.Settings
import com.nexolite.homekitchen.util.monthEnd
import com.nexolite.homekitchen.util.monthStart
import com.nexolite.homekitchen.util.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MenuRow(val item: MenuItem, val booked: Int) {
    val left: Int get() = (item.quantity - booked).coerceAtLeast(0)
    val soldOut: Boolean get() = item.quantity > 0 && booked >= item.quantity
}

enum class ReportPeriod { TODAY, MONTH }

data class ReportData(
    val sale: Long = 0,
    val orders: Int = 0,
    val received: Long = 0,
    val unpaid: Long = 0,
    val lastSevenDays: List<Pair<Long, Long>> = emptyList(),
    val topDishes: List<DishSale> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = KitchenRepository(db)
    private val dao = repo.dao
    private val prefs = Prefs(app)

    private val _date = MutableStateFlow(today())
    val date: StateFlow<Long> = _date.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val settings: StateFlow<Settings> = prefs.settings

    val menu: StateFlow<List<MenuRow>> = _date.flatMapLatest { day ->
        combine(dao.observeMenu(day), dao.observeBooked(day)) { items, booked ->
            val byId = booked.associate { it.menuItemId to it.qty }
            items.map { MenuRow(it, byId[it.id] ?: 0) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suggestions: StateFlow<List<DishSuggestion>> = dao.observeDishSuggestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val orders: StateFlow<List<OrderWithDetails>> = _date.flatMapLatest { dao.observeOrders(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<Customer>> = dao.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dues: StateFlow<List<CustomerDue>> = dao.observeDues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _period = MutableStateFlow(ReportPeriod.TODAY)
    val period: StateFlow<ReportPeriod> = _period.asStateFlow()

    val report: StateFlow<ReportData> = _period.flatMapLatest { p -> reportFlow(p) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportData())

    private fun reportFlow(p: ReportPeriod): Flow<ReportData> {
        val now = today()
        val from = if (p == ReportPeriod.TODAY) now else monthStart(now)
        val to = if (p == ReportPeriod.TODAY) now else monthEnd(now)
        val weekFrom = now - 6
        val totals = combine(
            dao.observeSale(from, to),
            dao.observeOrderCount(from, to),
            dao.observeReceived(from, to),
            dao.observeUnpaid(from, to)
        ) { sale, count, received, unpaid -> ReportData(sale, count, received, unpaid) }
        return combine(totals, dao.observeDailySales(weekFrom, now), dao.observeTopDishes(from, to)) { t, daily, top ->
            val byDay = daily.associate { it.day to it.amount }
            t.copy(
                lastSevenDays = (weekFrom..now).map { it to (byDay[it] ?: 0L) },
                topDishes = top
            )
        }
    }

    // ---------- Date ----------

    fun setDate(day: Long) { _date.value = day }
    fun shiftDate(days: Long) { _date.value += days }
    fun setPeriod(p: ReportPeriod) { _period.value = p }

    // ---------- Menu ----------

    fun addDish(name: String, price: Long, quantity: Int) = launchTask {
        repo.addMenuItem(_date.value, name, price, quantity)
    }

    fun updateDish(item: MenuItem) = launchTask { repo.updateMenuItem(item) }

    fun deleteDish(item: MenuItem) = launchTask {
        if (!repo.deleteMenuItem(item)) {
            _messages.emit("This dish already has orders, so it cannot be removed. Set its quantity instead.")
        }
    }

    fun copyPreviousMenu() = launchTask {
        val copied = repo.copyPreviousMenu(_date.value)
        _messages.emit(if (copied == 0) "No earlier menu to copy." else "$copied dishes copied from the last menu.")
    }

    // ---------- Orders ----------

    suspend fun loadOrder(id: Long): OrderWithDetails? = dao.getOrderDetails(id)

    fun observeCustomerOrderCount(customerId: Long): Flow<Int> = dao.observeCustomerOrderCount(customerId)

    fun saveOrder(input: OrderInput, onSaved: () -> Unit) = launchTask {
        repo.saveOrder(input)
        _messages.emit(if (input.editingId == null) "Order saved." else "Order updated.")
        onSaved()
    }

    fun deleteOrder(id: Long, onDone: () -> Unit) = launchTask {
        repo.deleteOrder(id)
        _messages.emit("Order deleted.")
        onDone()
    }

    fun advanceStatus(order: CustomerOrder) = launchTask { repo.advanceStatus(order) }

    fun setStatus(orderId: Long, status: String) = launchTask { repo.setStatus(orderId, status) }

    fun markPaid(order: CustomerOrder) = launchTask {
        repo.markPaid(order.id, today())
        _messages.emit("Payment received.")
    }

    fun receivePayment(customerId: Long, amount: Long) = launchTask {
        repo.receivePayment(customerId, amount, today())
        _messages.emit("Payment received.")
    }

    // ---------- Settings & backup ----------

    fun updateSettings(transform: (Settings) -> Settings) = prefs.update(transform)

    fun backupFileName(): String =
        "HomeKitchen-backup-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".json"

    /** Writes a backup to a cache file and returns a shareable content Uri. */
    suspend fun createShareableBackup(): Uri? = runCatching {
        val app = getApplication<Application>()
        val json = Backup.export(db)
        val file = withContext(Dispatchers.IO) {
            val dir = File(app.cacheDir, "backups").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            File(dir, backupFileName()).apply { writeText(json) }
        }
        prefs.update { it.copy(lastBackupAt = System.currentTimeMillis()) }
        FileProvider.getUriForFile(app, app.packageName + ".fileprovider", file)
    }.onFailure { _messages.emit("Backup failed: ${it.message}") }.getOrNull()

    fun saveBackupTo(uri: Uri) = launchTask {
        val app = getApplication<Application>()
        runCatching {
            val json = Backup.export(db)
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("Could not open the selected file.")
            }
        }.onSuccess {
            prefs.update { it.copy(lastBackupAt = System.currentTimeMillis()) }
            _messages.emit("Backup saved.")
        }.onFailure { _messages.emit("Backup failed: ${it.message}") }
    }

    fun restoreFrom(uri: Uri) = launchTask {
        val app = getApplication<Application>()
        runCatching {
            val json = withContext(Dispatchers.IO) {
                app.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    ?: error("Could not open the selected file.")
            }
            Backup.restore(db, json)
        }.onSuccess { _messages.emit("Backup restored successfully.") }
            .onFailure { _messages.emit("Restore failed: ${it.message}") }
    }

    private fun launchTask(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
