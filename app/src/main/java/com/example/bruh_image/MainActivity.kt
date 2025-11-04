package com.example.bruh_image

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.bumptech.glide.Glide

// ----- Модель -----
data class Picture(
    val id: Int,
    val author: String,
    val url: String
)

// ----- Демо-данные -----
private fun generateSamplePictures(): List<Picture> = listOf(
    Picture(1, "Human 1", "https://i.pravatar.cc/300"),
    Picture(2, "Human 2", "https://i.pravatar.cc/306"),
    Picture(3, "Human 3", "https://i.pravatar.cc/302"),
    Picture(4, "Human 4", "https://i.pravatar.cc/303"),
    Picture(5, "Human 5", "https://i.pravatar.cc/308"),
)

private enum class LayoutMode { LIST, GRID }

// ----- Активити -----
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GalleryScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScreen() {
    val gallery = remember { mutableStateListOf<Picture>().apply { addAll(generateSamplePictures()) } }

    var searchText by remember { mutableStateOf("") }
    var layout by remember { mutableStateOf(LayoutMode.GRID) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(searchText, gallery) {
        if (searchText.isBlank()) gallery.toList()
        else gallery.filter { it.author.contains(searchText, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Галерея") },
                actions = {
                    TextButton(onClick = {
                        layout = if (layout == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID
                    }) { Text(if (layout == LayoutMode.GRID) "Список" else "Сетка") }

                    TextButton(onClick = { gallery.clear() }) { Text("Очистить всё") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Поиск по автору") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true
            )

            when (layout) {
                LayoutMode.LIST -> PictureList(
                    pictures = filtered,
                    onRemove = { pic -> gallery.removeAll { it.id == pic.id } }
                )
                LayoutMode.GRID -> PictureGrid(
                    pictures = filtered,
                    onRemove = { pic -> gallery.removeAll { it.id == pic.id } }
                )
            }
        }
    }

    if (showAddDialog) {
        AddPictureDialog(
            onCancel = { showAddDialog = false },
            onConfirm = { idStr, author, url, setError ->
                val id = idStr.toIntOrNull()
                if (id == null) { setError("ID должен быть целым числом"); return@AddPictureDialog }
                if (author.isBlank()) { setError("Автор не может быть пустым"); return@AddPictureDialog }
                if (url.isBlank()) { setError("URL не может быть пустым"); return@AddPictureDialog }

                val normalizedUrl = url.trim()
                val exists = gallery.any { it.id == id || it.url.equals(normalizedUrl, ignoreCase = true) }
                if (exists) { setError("Картинка с таким ID или URL уже существует"); return@AddPictureDialog }

                gallery.add(Picture(id = id, author = author.trim(), url = normalizedUrl))
                showAddDialog = false
            }
        )
    }
}

// ----- Список -----
@Composable
private fun PictureList(
    pictures: List<Picture>,
    onRemove: (Picture) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = pictures, key = { it.id }) { pic ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRemove(pic) }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlideNetworkImage(
                        url = pic.url,
                        modifier = Modifier
                            .size(96.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = pic.author, style = MaterialTheme.typography.titleMedium)
                        Text(text = "id: ${pic.id}", style = MaterialTheme.typography.bodySmall)
                        Text(text = pic.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ----- Сетка -----
@Composable
private fun PictureGrid(
    pictures: List<Picture>,
    onRemove: (Picture) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = pictures, key = { it.id }) { pic ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRemove(pic) }
            ) {
                Column {
                    GlideNetworkImage(
                        url = pic.url,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Column(Modifier.padding(8.dp)) {
                        Text(text = pic.author, style = MaterialTheme.typography.titleSmall)
                        Text(text = "id: ${pic.id}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// ----- Картинка через Glide core + AndroidView -----
@Composable
private fun GlideNetworkImage(
    url: String,
    modifier: Modifier = Modifier
) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val errorColor = MaterialTheme.colorScheme.errorContainer.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.LTGRAY)
            }
        },
        update = { imageView ->
            Glide.with(imageView.context)
                .load(url)
                .placeholder(ColorDrawable(placeholderColor))
                .error(ColorDrawable(errorColor))
                .into(imageView)
        }
    )
}

// ----- Диалог добавления -----
@Composable
private fun AddPictureDialog(
    onCancel: () -> Unit,
    onConfirm: (id: String, author: String, url: String, setError: (String?) -> Unit) -> Unit
) {
    var id by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = { onConfirm(id, author, url) { error = it } }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Отмена") } },
        title = { Text("Новая картинка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("ID (целое число)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Автор") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true
                )
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(text = "Проверяем уникальность по ID и URL (без учёта регистра).", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}
