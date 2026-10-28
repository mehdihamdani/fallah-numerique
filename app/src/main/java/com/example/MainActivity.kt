package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.example.data.model.WorkspaceSection
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.FellahWorkspaceScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RoleOnboardingScreen
import com.example.ui.screens.VisitorExploreScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FellahViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
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
  var isVisitorMode by rememberSaveable { mutableStateOf(false) }
  var pendingDestinationSection by rememberSaveable { mutableStateOf<WorkspaceSection?>(null) }
  var pendingSuggestedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }

  val user = currentUser
  if (user == null) {
    if (isVisitorMode) {
      VisitorExploreScreen(
        language = preferredLanguage,
        onLanguageSelected = { lang -> preferredLanguage = lang },
        onBackToLogin = { isVisitorMode = false },
        onRequireSignInForAction = { targetSection, suggestedRole ->
          pendingDestinationSection = targetSection
          pendingSuggestedRole = suggestedRole
          isVisitorMode = false
        },
      )
    } else {
      LoginScreen(
        initialLanguage = preferredLanguage,
        onLanguageChanged = { lang -> preferredLanguage = lang },
        onEnterVisitorMode = { isVisitorMode = true },
      )
    }
  } else {
    val fellahViewModel: FellahViewModel =
      viewModel(
        key = user.uid,
        factory = FellahViewModel.provideFactory(user.uid),
      )
    val editorState by fellahViewModel.editorState.collectAsStateWithLifecycle()
    val userAccountState by fellahViewModel.userAccountState.collectAsStateWithLifecycle()
    val roleProfiles by fellahViewModel.roleProfilesState.collectAsStateWithLifecycle()

    LaunchedEffect(user.uid, preferredLanguage) {
      fellahViewModel.setLanguage(preferredLanguage)
    }

    when (val accountUi = userAccountState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Success -> {
        val account = accountUi.data
        if (account == null || !account.onboardingCompleted) {
          RoleOnboardingScreen(
            existingAccount = account,
            existingRoleProfiles = roleProfiles,
            initialSuggestedRole = pendingSuggestedRole,
            language = editorState.language,
            userDisplayName = user.displayName ?: "Opérateur Fellah Numérique",
            userEmail = user.email.orEmpty(),
            isSaving = editorState.isOnboardingSaving,
            errorMessage = editorState.onboardingErrorMessage,
            onLanguageSelected = { lang ->
              preferredLanguage = lang
              fellahViewModel.setLanguage(lang)
            },
            onSaveProgressDraft = { roles, defaultRole, step ->
              fellahViewModel.saveOnboardingDraft(
                displayName = user.displayName ?: "Opérateur Fellah Numérique",
                email = user.email.orEmpty(),
                selectedRoles = roles,
                defaultRole = defaultRole,
                step = step,
              )
            },
            onCompleteOnboarding = { roles, defaultRole, profiles ->
              val dest = pendingDestinationSection
              pendingDestinationSection = null
              pendingSuggestedRole = null
              fellahViewModel.completeMultiRoleOnboarding(
                displayName = user.displayName ?: "Opérateur Fellah Numérique",
                email = user.email.orEmpty(),
                selectedRoles = roles,
                defaultRole = defaultRole,
                profilesToSave = profiles,
                intendedSectionAfterLogin = dest,
              )
            },
            onSignOut = {
              signOutUser(
                credentialManager = credentialManager,
                onSignOutComplete = {},
                scope = scope,
              )
            },
          )
        } else {
          LaunchedEffect(pendingDestinationSection) {
            val dest = pendingDestinationSection
            if (dest != null) {
              fellahViewModel.navigateToSection(dest)
              pendingDestinationSection = null
              pendingSuggestedRole = null
            }
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
      is UiState.Error -> {
        // Fallback to workspace if offline / account stream error so existing farmers are never blocked
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
  }
}
