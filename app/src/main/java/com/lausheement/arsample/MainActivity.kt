package com.lausheement.arsample

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class TodoItem(
    val id: Long,
    val text: String,
    val completed: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TodoApp(context = applicationContext)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoApp(context: Context) {
    val todos = remember {
        mutableStateListOf<TodoItem>().apply {
            addAll(TodoStorage.load(context))
        }
    }
    var taskText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("To-Do List") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Add a task") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        val trimmed = taskText.trim()
                        if (trimmed.isNotEmpty()) {
                            todos.add(TodoItem(id = System.currentTimeMillis(), text = trimmed, completed = false))
                            TodoStorage.save(context, todos)
                            taskText = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = todos, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = { checked ->
                                    val index = todos.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        todos[index] = item.copy(completed = checked)
                                        TodoStorage.save(context, todos)
                                    }
                                }
                            )
                            Text(
                                text = item.text,
                                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                        IconButton(
                            onClick = {
                                todos.removeAll { it.id == item.id }
                                TodoStorage.save(context, todos)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${item.text}")
                        }
                    }
                }
            }
        }
    }
}

private object TodoStorage {
    private const val PREFS_NAME = "todo_prefs"
    private const val TODOS_KEY = "todos"

    fun load(context: Context): List<TodoItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJson = prefs.getString(TODOS_KEY, null) ?: return emptyList()

        return runCatching {
            val array = JSONArray(savedJson)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val id = obj.optLong("id", -1L)
                    val text = obj.optString("text", "")
                    val completed = obj.optBoolean("completed", false)
                    if (id != -1L && text.isNotBlank()) {
                        add(TodoItem(id = id, text = text, completed = completed))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, todos: List<TodoItem>) {
        val jsonArray = JSONArray()
        todos.forEach { todo ->
            jsonArray.put(
                JSONObject()
                    .put("id", todo.id)
                    .put("text", todo.text)
                    .put("completed", todo.completed)
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TODOS_KEY, jsonArray.toString())
            .apply()
    }
}
