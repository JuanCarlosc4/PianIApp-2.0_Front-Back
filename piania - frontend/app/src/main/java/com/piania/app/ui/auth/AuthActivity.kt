package com.piania.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.piania.app.R
import com.piania.app.data.SessionManager
import com.piania.app.data.model.request.RegistroRequestDTO
import com.piania.app.ui.base.BaseLocalizedActivity
import com.piania.app.ui.main.MainActivity
import com.piania.app.ui.theme.PianIAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : BaseLocalizedActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val registroViewModel: RegistroViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Si ya hay token, saltamos directamente
        if (!sessionManager.fetchAuthToken().isNullOrEmpty()) {
            irAMenu()
            return
        }

        // Por si el LoginViewModel reactiva la sesión
        loginViewModel.isSessionActive.observe(this) { isActive ->
            if (isActive) irAMenu()
        }

        setContent {
            PianIAppTheme {
                AuthTabs(
                    loginViewModel = loginViewModel,
                    registroViewModel = registroViewModel,
                    onLoginSuccess = { irAMenu() },
                    onFinishAfterRegister = {
                        // Tras registro, volvemos a la tab de login
                    }
                )
            }
        }
    }

    private fun irAMenu() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTabs(
    loginViewModel: LoginViewModel,
    registroViewModel: RegistroViewModel,
    onLoginSuccess: () -> Unit,
    onFinishAfterRegister: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Login, 1=Registro
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_login)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_register)) }
                )
            }

            when (selectedTab) {
                0 -> LoginContent(
                    viewModel = loginViewModel,
                    onLoginSuccess = onLoginSuccess,
                    onNavigateToRegister = { selectedTab = 1 }
                )

                1 -> RegistroContent(
                    viewModel = registroViewModel,
                    onRegisterSuccess = {
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.register_success_login),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                        selectedTab = 0
                        onFinishAfterRegister()
                    },
                    onNavigateBackToLogin = { selectedTab = 0 }
                )
            }
        }
    }
}

@Composable
private fun LoginContent(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val isLoading by viewModel.isLoading.observeAsState(false)
    val loginResult by viewModel.loginResult.observeAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val emailValido = isValidEmail(email)

    LaunchedEffect(loginResult) {
        loginResult?.onSuccess {
            Toast.makeText(context, context.getString(R.string.welcome), Toast.LENGTH_LONG).show()
            onLoginSuccess()
        }
        loginResult?.onFailure {
            val msg = it.message ?: context.getString(R.string.auth_error)
            Toast.makeText(
                context,
                context.getString(R.string.error_prefix, msg),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.iniciar_sesion),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { input ->
                email = input.filterNot { it.isWhitespace() }
            },
            label = { Text(stringResource(R.string.correo_electronico)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            isError = email.isNotBlank() && !emailValido,
            supportingText = {
                if (email.isNotBlank() && !emailValido) {
                    Text(stringResource(R.string.correo_valido))
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.contrasenya)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (!isValidEmail(email)) {
                    Toast.makeText(context, "Introduce un correo válido", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.login(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && emailValido && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(22.dp))
            } else {
                Text(stringResource(R.string.entrar))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RegistroContent(
    viewModel: RegistroViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateBackToLogin: () -> Unit
) {
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.observeAsState(false)
    val registerResult by viewModel.registroResult.observeAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // USER (normal), TEACHER (profesor)
    var selectedAccountType by remember { mutableStateOf("USER") }

    val emailValido = isValidEmail(email)

    LaunchedEffect(registerResult) {
        registerResult?.onSuccess {
            onRegisterSuccess()
        }?.onFailure { exception ->
            val msg = exception.message ?: context.getString(R.string.auth_error)
            Toast.makeText(
                context,
                context.getString(R.string.error_prefix, msg),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(stringResource(R.string.full_name)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.account_type),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AccountTypeCard(
                title = stringResource(R.string.account_type_user_title),
                description = stringResource(R.string.account_type_user_desc),
                selected = selectedAccountType == "USER",
                onClick = { selectedAccountType = "USER" },
                enabled = !isLoading
            )
            AccountTypeCard(
                title = stringResource(R.string.account_type_teacher_title),
                description = stringResource(R.string.account_type_teacher_desc),
                selected = selectedAccountType == "TEACHER",
                onClick = { selectedAccountType = "TEACHER" },
                enabled = !isLoading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { input ->
                email = input.filterNot { it.isWhitespace() }
            },
            label = { Text(stringResource(R.string.correo_electronico)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = email.isNotBlank() && !emailValido,
            supportingText = {
                if (email.isNotBlank() && !emailValido) {
                    Text("Introduce un correo válido")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.contrasenya)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!isValidEmail(email)) {
                    Toast.makeText(context, "Introduce un correo válido", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val accountTypeToSend = if (selectedAccountType == "TEACHER") "TEACHER" else "USER"
                val request = RegistroRequestDTO(
                    email = email,
                    password = password,
                    fullName = fullName,
                    accountType = accountTypeToSend
                )
                viewModel.register(request)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading &&
                    fullName.isNotBlank() &&
                    emailValido &&
                    password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.register))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBackToLogin) {
            Text(stringResource(R.string.back_to_login))
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}