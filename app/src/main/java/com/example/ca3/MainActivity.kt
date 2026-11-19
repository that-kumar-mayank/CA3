package com.example.ca3

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShoppingScreen()
            }
        }
    }
}

@Entity
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val quantity: Int
)

@Dao
interface ShoppingDao {
    @Insert
    suspend fun insertItem(item: Item)
    @Query("SELECT * FROM Item")
    fun getAllItems(): Flow<List<Item>>
    @Query("SELECT COUNT(*) FROM Item")
    suspend fun countItems(): Int
}

@Database(
    entities = [Item::class],
    version = 1,
    exportSchema = false
)

abstract class ShoppingDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao
}

class ShoppingViewModel(application: Application) :
    AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        ShoppingDatabase::class.java,
        "shopping_db"
    ).build()
    private val dao = db.shoppingDao()
    val items = dao.getAllItems()
    var message by mutableStateOf("")
        private set
    fun addItem(name: String, quantity: String) {
        val q = quantity.toIntOrNull()
        if (name.isBlank() || q == null || q <= 0) {
            message = "Enter valid name and quantity"
            return
        }
        viewModelScope.launch {
            val added = db.withTransaction {
                if (dao.countItems() >= 3) {
                    false
                } else {
                    dao.insertItem(
                        Item(
                            name = name.trim(),
                            quantity = q
                        )
                    )
                    true
                }
            }
            message = if (added) {
                "Item Added Successfully"
            } else {
                "Limit Reached! Only 3 items allowed."
            }
        }
    }
}

@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    val shoppingItems by viewModel.items.collectAsState(
        initial = emptyList()
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Shopping List",
            style = MaterialTheme.typography.headlineMedium
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                viewModel.addItem(name, quantity)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Item")
        }
        Text(
            text = viewModel.message
        )
        Text(
            text = "Total Items: ${shoppingItems.size}/3"
        )
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(shoppingItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("ID: ${item.id}")
                        Text("Name: ${item.name}")
                        Text("Quantity: ${item.quantity}")
                    }
                }
            }
        }
    }
}
