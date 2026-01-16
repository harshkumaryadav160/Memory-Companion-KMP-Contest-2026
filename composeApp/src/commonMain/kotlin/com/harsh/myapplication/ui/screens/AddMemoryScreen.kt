package com.harsh.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome // Professional AI Icon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harsh.myapplication.data.model.AiAnalysisResult
import com.harsh.myapplication.data.model.Person
import com.harsh.myapplication.ui.components.VoiceInputButton
import com.harsh.myapplication.ui.viewmodel.AddMemoryUiState
import com.harsh.myapplication.ui.viewmodel.AddMemoryViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    onNavigateBack: () -> Unit = {},
    preSelectedPersonId: String? = null,
    viewModel: AddMemoryViewModel = koinInject()
) {
    val persons by viewModel.persons.collectAsState()
    val selectedPerson by viewModel.selectedPerson.collectAsState()
    val memoryText by viewModel.memoryText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()

    LaunchedEffect(preSelectedPersonId) {
        if (preSelectedPersonId != null) {
            viewModel.selectPersonById(preSelectedPersonId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AddMemoryUiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Memory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is AddMemoryUiState.ReviewingAI -> {
                    aiAnalysis?.let { analysis ->
                        AiReviewScreen(
                            analysis = analysis,
                            memoryText = memoryText,
                            onApprove = { approved -> viewModel.saveMemoryWithAI(approved) },
                            onDiscard = { viewModel.discardAiAnalysis() },
                            onEdit = { updated -> viewModel.updateAiAnalysis(updated) }
                        )
                    }
                }
                else -> {
                    MemoryInputScreen(
                        persons = persons,
                        selectedPerson = selectedPerson,
                        memoryText = memoryText,
                        uiState = uiState,
                        viewModel = viewModel,
                        onPersonSelected = { viewModel.selectPerson(it) },
                        onMemoryTextChange = { viewModel.updateMemoryText(it) },
                        onAnalyze = { viewModel.analyzeMemory() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryInputScreen(
    persons: List<Person>,
    selectedPerson: Person?,
    memoryText: String,
    uiState: AddMemoryUiState,
    viewModel: AddMemoryViewModel,
    onPersonSelected: (Person) -> Unit,
    onMemoryTextChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    val isListening by viewModel.isListening.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Person Selector
        PersonSelector(
            persons = persons,
            selectedPerson = selectedPerson,
            onPersonSelected = onPersonSelected
        )

        // Rounded Input Box
        OutlinedTextField(
            value = memoryText,
            onValueChange = onMemoryTextChange,
            label = { Text("What do you want to remember?") },
            placeholder = { Text("Start typing or use voice...") },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 200.dp),
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            supportingText = { Text("${memoryText.length} characters") }
        )

        // Voice Button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VoiceInputButton(
                isListening = isListening,
                onStartListening = { viewModel.startVoiceInput() },
                onStopListening = { viewModel.stopVoiceInput() }
            )
        }

        // Analyze Button
        Button(
            onClick = onAnalyze,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = uiState !is AddMemoryUiState.AnalyzingWithAI
        ) {
            if (uiState is AddMemoryUiState.AnalyzingWithAI) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Analyzing...")
            } else {
                Icon(Icons.Default.AutoAwesome, null) // Add Sparkles Icon
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze & Save", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Error Card
        if (uiState is AddMemoryUiState.Error) {
            val errorMsg = (uiState as AddMemoryUiState.Error).message
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * AI Review Screen
 */
@Composable
private fun AiReviewScreen(
    analysis: AiAnalysisResult,
    memoryText: String,
    onApprove: (AiAnalysisResult) -> Unit,
    onDiscard: () -> Unit,
    onEdit: (AiAnalysisResult) -> Unit
) {
    // We create a local state copy so user can edit fields before saving
    var editedAnalysis by remember(analysis) { mutableStateOf(analysis) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "AI Analysis Review",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Review the AI's understanding before saving.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Original Memory (ReadOnly)
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Original Memory:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = memoryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }


        // 1. TOPIC
        OutlinedTextField(
            value = editedAnalysis.topic,
            onValueChange = { editedAnalysis = editedAnalysis.copy(topic = it) },
            label = { Text("Topic") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        // 2. EMOTION
        OutlinedTextField(
            value = editedAnalysis.emotion ?: "",
            onValueChange = {
                editedAnalysis = editedAnalysis.copy(emotion = it.ifBlank { null })
            },
            label = { Text("Emotion (optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        // 3. SUMMARY
        OutlinedTextField(
            value = editedAnalysis.summary,
            onValueChange = { editedAnalysis = editedAnalysis.copy(summary = it) },
            label = { Text("Summary") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3, // Taller box for summary
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ACTION BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Discard Button
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Discard")
            }

            // Save Button
            Button(
                onClick = { onApprove(editedAnalysis) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }

        // Privacy Note
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            shape = MaterialTheme.shapes.medium,
            border = null // No border for cleaner look
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You can edit any field above. You remain in control of your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonSelector(
    persons: List<Person>,
    selectedPerson: Person?,
    onPersonSelected: (Person) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedPerson?.name ?: "Select Person",
            onValueChange = {},
            readOnly = true,
            label = { Text("Person") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            persons.forEach { person ->
                DropdownMenuItem(
                    text = { Text(person.name) },
                    onClick = {
                        onPersonSelected(person)
                        expanded = false
                    }
                )
            }
        }
    }
}