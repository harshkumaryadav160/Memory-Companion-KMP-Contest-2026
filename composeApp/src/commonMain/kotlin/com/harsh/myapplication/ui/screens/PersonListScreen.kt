package com.harsh.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harsh.myapplication.data.model.Person
import com.harsh.myapplication.ui.components.BackHandler
import com.harsh.myapplication.ui.components.PersonCard
import com.harsh.myapplication.ui.viewmodel.PersonListUiState
import com.harsh.myapplication.ui.viewmodel.PersonListViewModel
import com.harsh.myapplication.ui.viewmodel.SortOption
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    onNavigateToAddMemory: () -> Unit = {},
    onNavigateToPersonDetail: (String) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    viewModel: PersonListViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val memoryCounts by viewModel.memoryCounts.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showDialog by viewModel.showAddDialog.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var personToDelete by remember { mutableStateOf<Person?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(sortOption) { listState.scrollToItem(0) }

    BackHandler(enabled = isSearchActive || searchQuery.isNotEmpty()) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search people...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close Search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Clear")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Memory Companion", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Latest") },
                                    onClick = {
                                        viewModel.updateSortOption(SortOption.LATEST)
                                        showSortMenu = false
                                    },
                                    trailingIcon = { if (sortOption == SortOption.LATEST) Icon(Icons.Default.Check, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Alphabetical (A-Z)") },
                                    onClick = {
                                        viewModel.updateSortOption(SortOption.ALPHABETICAL)
                                        showSortMenu = false
                                    },
                                    trailingIcon = { if (sortOption == SortOption.ALPHABETICAL) Icon(Icons.Default.Check, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Most Memories") },
                                    onClick = {
                                        viewModel.updateSortOption(SortOption.MOST_MEMORIES)
                                        showSortMenu = false
                                    },
                                    trailingIcon = { if (sortOption == SortOption.MOST_MEMORIES) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = onNavigateToChat,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.Face, "Chat with AI")
                }

                FloatingActionButton(onClick = { viewModel.showAddPersonDialog() }) {
                    Icon(Icons.Default.Add, "Add Person")
                }

                SmallFloatingActionButton(
                    onClick = onNavigateToAddMemory,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Edit, "Quick Memory")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PersonListUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is PersonListUiState.Empty -> EmptyState(modifier = Modifier.align(Alignment.Center))
                is PersonListUiState.Success -> {
                    val filteredPersons = state.persons.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredPersons.isEmpty()) {
                        Text(
                            "No matches found",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            // 👇 FIXED: Increased bottom padding to 240.dp
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 240.dp // Enough space for 3 buttons!
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = filteredPersons, key = { it.id }) { person ->
                                PersonCard(
                                    person = person,
                                    memoryCount = memoryCounts[person.id] ?: 0,
                                    onClick = { onNavigateToPersonDetail(person.id) },
                                    onDelete = { personToDelete = person }
                                )
                            }
                        }
                    }
                }
                is PersonListUiState.Error -> Text("Error: ${state.message}", modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showDialog) {
        AddPersonDialog(
            onDismiss = { viewModel.hideAddPersonDialog() },
            onConfirm = { name -> viewModel.createPerson(name) }
        )
    }
    if (personToDelete != null) {
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("Delete Person?") },
            text = { Text("Are you sure? This will delete ${personToDelete?.name} and all their memories.") },
            confirmButton = {
                Button(
                    onClick = {
                        personToDelete?.let { viewModel.deletePerson(it) }
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { personToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No people yet",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Add someone to start remembering",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddPersonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Person") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}