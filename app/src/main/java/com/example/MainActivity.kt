package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLanguage
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.FellahWorkspaceScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FellahViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var preferredLanguage by rememberSaveable { mutableStateOf(AppLanguage.AR) }

  val user = currentUser
  if (user == null) {
    LoginScreen(
      initialLanguage = preferredLanguage,
      onLanguageChanged = { lang -> preferredLanguage = lang },
    )
  } else {
    val fellahViewModel: FellahViewModel =
      viewModel(
        key = user.uid,
        factory = FellahViewModel.provideFactory(user.uid),
      )
    LaunchedEffect(user.uid, preferredLanguage) {
      fellahViewModel.setLanguage(preferredLanguage)
    }
    FellahWorkspaceScreen(
      viewModel = fellahViewModel,
      currentUser = user,
      onSignOut = {
        signOutUser(
          credentialManager = credentialManager,
          onSignOutComplete = {},
          scope = scope,
        )
      },
    )
  }
}
