package com.piania.app.ui.teacher

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.piania.app.ui.base.BaseLocalizedActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.piania.app.R
import com.piania.app.ui.theme.PianIAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeacherAreaActivity : BaseLocalizedActivity() {

    private val viewModel: TeacherAreaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PianIAppTheme {
                TeacherAreaScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAreaScreen(viewModel: TeacherAreaViewModel) {
    val classes by viewModel.classes.observeAsState(emptyList())
    val students by viewModel.selectedClassStudents.observeAsState(emptyList())
    val myStudents by viewModel.myStudents.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState(null)

    var selectedTab by remember { mutableIntStateOf(0) }
    var newClassName by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf<Long?>(null) }
    var newStudentEmail by remember { mutableStateOf("") }
    var editingClassId by remember { mutableStateOf<Long?>(null) }
    var editingClassName by remember { mutableStateOf("") }
    var editingClassAvatar by remember { mutableStateOf("GROUP_AVATAR_1") }

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.teacher_area_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (error != null) {
                AssistChip(
                    onClick = { viewModel.clearError() },
                    label = { Text(error ?: "") },
                    modifier = Modifier.padding(16.dp)
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.classes)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.students)) })
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (selectedTab) {
                0 -> {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.teacher_area_classes_desc), style = MaterialTheme.typography.bodyMedium)

                        OutlinedTextField(
                            value = newClassName,
                            onValueChange = { newClassName = it },
                            label = { Text(stringResource(R.string.class_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val n = newClassName.trim()
                                if (n.isNotEmpty()) viewModel.createClass(n)
                                newClassName = ""
                            },
                            enabled = !isLoading
                        ) { Text(stringResource(R.string.create_class)) }

                        Divider()

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(classes) { c ->
                                Card(onClick = {
                                    selectedClassId = c.id
                                    viewModel.loadClassStudents(c.id)
                                }) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(c.name ?: "Clase #${c.id}", style = MaterialTheme.typography.titleMedium)
                                                Text("ID: ${c.id}", style = MaterialTheme.typography.bodySmall)
                                                Text("Avatar: ${c.groupAvatar ?: "GROUP_AVATAR_1"}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            TextButton(
                                                onClick = {
                                                    editingClassId = c.id
                                                    editingClassName = c.name ?: ""
                                                    editingClassAvatar = c.groupAvatar ?: "GROUP_AVATAR_1"
                                                },
                                                enabled = !isLoading
                                            ) { Text(stringResource(R.string.edit)) }
                                            TextButton(
                                                onClick = { viewModel.deleteClass(c.id) },
                                                enabled = !isLoading
                                            ) { Text(stringResource(R.string.delete)) }
                                        }
                                    }
                                }
                            }
                        }

                        if (editingClassId != null) {
                            Divider()
                            Text(stringResource(R.string.edit_class), style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = editingClassName,
                                onValueChange = { editingClassName = it },
                                label = { Text(stringResource(R.string.class_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                (1..4).forEach { index ->
                                    val value = "GROUP_AVATAR_$index"
                                    FilterChip(
                                        selected = editingClassAvatar == value,
                                        onClick = { editingClassAvatar = value },
                                        label = { Text(index.toString()) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val id = editingClassId ?: return@Button
                                        viewModel.updateClass(id, editingClassName.trim(), editingClassAvatar)
                                        editingClassId = null
                                    },
                                    enabled = !isLoading
                                ) { Text(stringResource(R.string.save)) }
                                OutlinedButton(onClick = { editingClassId = null }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        }

                        if (selectedClassId != null) {
                            Divider()
                            Text(stringResource(R.string.students_of_class_format, selectedClassId!!), style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = newStudentEmail,
                                onValueChange = { newStudentEmail = it },
                                label = { Text(stringResource(R.string.student_email)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val email = newStudentEmail.trim()
                                    val id = selectedClassId
                                    if (id != null && email.isNotEmpty()) viewModel.addStudentToClass(id, email)
                                    newStudentEmail = ""
                                },
                                enabled = !isLoading
                            ) { Text(stringResource(R.string.add_student)) }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(students) { s ->
                                    Card {
                                        Row(modifier = Modifier.padding(12.dp)) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(s.studentEmail ?: stringResource(R.string.student), style = MaterialTheme.typography.bodyLarge)
                                                Text("EnrollmentId: ${s.id}", style = MaterialTheme.typography.bodySmall)
                                                Text("Status: ${s.status ?: "-"}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            TextButton(
                                                onClick = {
                                                    val id = selectedClassId ?: return@TextButton
                                                    viewModel.removeStudentFromClass(id, s.id)
                                                },
                                                enabled = !isLoading
                                            ) { Text(stringResource(R.string.remove)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.loadMyStudents() }, enabled = !isLoading) { Text(stringResource(R.string.refresh)) }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(myStudents) { r ->
                                Card {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(r.studentEmail ?: stringResource(R.string.student), style = MaterialTheme.typography.bodyLarge)
                                        Text(stringResource(R.string.active_format, r.active), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
