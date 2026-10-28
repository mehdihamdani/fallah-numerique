package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.AiChatMessageRecord
import com.example.data.ai.AiConversation
import com.example.data.ai.FellahPlatformDataSnapshot
import com.example.data.ai.ProductionCostInput
import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.example.ui.viewmodel.FellahAiViewModel
import com.example.util.RoleStrings

/**
 * Non-intrusive modal sheet / drawer allowing operators and visitors to invoke **Fellah AI**
 * from any screen without losing their current task or form state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FellahAiModalSheet(
  aiViewModel: FellahAiViewModel,
  language: AppLanguage,
  activeRole: UserRole,
  platformSnapshot: FellahPlatformDataSnapshot,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.testTag("fellah_ai_modal_sheet"),
  ) {
    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.90f)) {
      FellahAiAssistantPanel(
        aiViewModel = aiViewModel,
        language = language,
        activeRole = activeRole,
        platformSnapshot = platformSnapshot,
        isOverlaySheet = true,
        onCloseOrBack = onDismiss,
      )
    }
  }
}

/**
 * Responsive **Fellah AI — Intelligent Agricultural Assistant** panel usable both
 * inside [FellahAiModalSheet] and as a full workspace section (`WorkspaceSection.FELLAH_AI`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FellahAiAssistantPanel(
  aiViewModel: FellahAiViewModel,
  language: AppLanguage,
  activeRole: UserRole,
  platformSnapshot: FellahPlatformDataSnapshot,
  isOverlaySheet: Boolean,
  onCloseOrBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOverlaySheet) {
    BackHandler(onBack = onCloseOrBack)
  }

  val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()
  val conversations by aiViewModel.conversationsState.collectAsStateWithLifecycle()
  val messages by aiViewModel.messagesState.collectAsStateWithLifecycle()
  val layoutDirection =
    if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
  val listState = rememberLazyListState()

  val starterQuestions =
    remember(activeRole, language, platformSnapshot.isAuthenticated) {
      aiViewModel.suggestedQuestions(activeRole, language, platformSnapshot.isAuthenticated)
    }

  LaunchedEffect(messages.size, uiState.isSending) {
    val totalItems = messages.size + (if (uiState.isSending) 1 else 0)
    if (totalItems > 0) {
      listState.animateScrollToItem(totalItems - 1)
    }
  }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier =
        modifier
          .fillMaxWidth()
          .let { if (isOverlaySheet) it.fillMaxSize() else it.heightIn(min = 560.dp, max = 780.dp) }
          .imePadding()
          .testTag("fellah_ai_panel_root"),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // 1. Assistant Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Column {
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "فلاح الذكي • Fellah AI",
                    fr = "Fellah AI • Assistant Agricole",
                    en = "Fellah AI • Agricultural Assistant",
                  ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              val contextSubtitle =
                if (platformSnapshot.isAuthenticated) {
                  "${activeRole.localizedTitle(language)} • ${
                    if (uiState.isProviderVerified) "Gemini 3.8 Flash"
                    else RoleStrings.tr(language, "غير مهيأ", "Non configuré", "Unconfigured")
                  }"
                } else {
                  RoleStrings.tr(
                    language,
                    ar = "وضع الزائر العام (بدون حفظ دائم للمحادثة)",
                    fr = "Mode Visiteur (session éphémère)",
                    en = "Public Visitor Mode (ephemeral session)",
                  )
                }
              Text(
                text = contextSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            if (platformSnapshot.isAuthenticated) {
              IconButton(
                onClick = { aiViewModel.toggleHistoryPanel() },
                modifier = Modifier.size(48.dp).testTag("fellah_ai_history_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.History,
                  contentDescription =
                    RoleStrings.tr(language, "سجل المحادثات", "Historique", "Conversation History"),
                )
              }
            }

            IconButton(
              onClick = { aiViewModel.startNewConversation() },
              modifier = Modifier.size(48.dp).testTag("fellah_ai_new_chat_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.AddComment,
                contentDescription =
                  RoleStrings.tr(language, "محادثة جديدة", "Nouvelle discussion", "New Chat"),
                tint = MaterialTheme.colorScheme.primary,
              )
            }

            IconButton(
              onClick = { aiViewModel.toggleCostCalculatorPanel() },
              modifier = Modifier.size(48.dp).testTag("fellah_ai_cost_calc_toggle"),
            ) {
              Icon(
                imageVector = Icons.Filled.Calculate,
                contentDescription =
                  RoleStrings.tr(
                    language,
                    ar = "حاسبة تكلفة الإنتاج",
                    fr = "Calculateur de coûts",
                    en = "Production Cost Calculator",
                  ),
              )
            }

            IconButton(
              onClick = { aiViewModel.setShowPrivacyInfoDialog(true) },
              modifier = Modifier.size(48.dp).testTag("fellah_ai_privacy_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription =
                  RoleStrings.tr(language, "الخصوصية وحفظ البيانات", "Confidentialité", "Privacy"),
              )
            }

            if (isOverlaySheet) {
              IconButton(
                onClick = onCloseOrBack,
                modifier = Modifier.size(48.dp).testTag("fellah_ai_close_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = RoleStrings.tr(language, "إغلاق", "Fermer", "Close"),
                )
              }
            }
          }
        }

        // 2. Trust & Distinction Banner (AI Guidance vs Verified Data)
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Verified,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "تُعرض بيانات السوق والحسابات الموثقة بشارة خضراء مؤرخة، بينما تُميّز النصائح الإرشادية كتوجيه ذكي غير بديل عن الفحص الميداني ومخابر INRAA.",
                  fr = "Les données vérifiées (prix horodatés, calculs) portent un badge vert distinct des explications IA générales.",
                  en = "Verified timestamped platform data & calculations carry a green badge distinct from general AI guidance.",
                ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // 3. Expandable Conversation History Drawer/List
        AnimatedVisibility(visible = uiState.showHistoryPanel && platformSnapshot.isAuthenticated) {
          ConversationHistoryCard(
            conversations = conversations,
            activeConversationId = uiState.activeConversationId,
            language = language,
            onSelect = aiViewModel::selectConversation,
            onDelete = aiViewModel::deleteConversation,
          )
        }

        // 4. Expandable Production Cost Calculator Card
        AnimatedVisibility(visible = uiState.showCostCalculatorPanel) {
          ProductionCostCalculatorCard(
            language = language,
            defaultAreaHa = platformSnapshot.ownActiveParcel?.areaHectares ?: 2.0,
            defaultYieldTonsHa = platformSnapshot.ownActiveParcel?.estimatedYieldTonsPerHa ?: 25.0,
            onSubmitCalculation = { costInput ->
              val autoQuestion =
                RoleStrings.tr(
                  language,
                  ar = "احسب تكلفة الإنتاج وسعر التعادل لمحصول ${costInput.cropName} على مساحة ${costInput.areaHectares} هكتار وقدم تحليلاً اقتصادياً.",
                  fr = "Calculez le coût de production et le seuil de rentabilité pour ${costInput.cropName} sur ${costInput.areaHectares} Ha.",
                  en = "Calculate total production costs and break-even price for ${costInput.cropName} across ${costInput.areaHectares} Ha.",
                )
              aiViewModel.sendMessage(
                messageText = autoQuestion,
                activeRole = activeRole,
                language = language,
                platformSnapshot = platformSnapshot,
                costInput = costInput,
              )
            },
          )
        }

        // 5. Error Banner with Retry Support
        AnimatedVisibility(visible = uiState.errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth().testTag("fellah_ai_error_banner"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                  text = uiState.errorMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
              if (uiState.lastFailedMessage != null && !uiState.isSending) {
                OutlinedButton(
                  onClick = {
                    aiViewModel.retryLastTurn(
                      activeRole = activeRole,
                      language = language,
                      platformSnapshot = platformSnapshot,
                    )
                  },
                  modifier = Modifier.height(48.dp).testTag("fellah_ai_retry_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(RoleStrings.tr(language, "إعادة المحاولة", "Réessayer", "Retry"))
                }
              }
            }
          }
        }

        // 6. Conversation Messages + Starter Prompts
        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f).fillMaxWidth().testTag("fellah_ai_messages_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (messages.isEmpty() && !uiState.isSending) {
            item {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text =
                    RoleStrings.tr(
                      language,
                      ar = "أسئلة مقترحة لنشاطك (${activeRole.localizedTitle(language)}):",
                      fr = "Questions suggérées pour votre rôle (${activeRole.localizedTitle(language)}) :",
                      en = "Suggested questions for your active role (${activeRole.localizedTitle(language)}):",
                    ),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )

                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  starterQuestions.forEachIndexed { idx, starter ->
                    SuggestionChip(
                      onClick = {
                        aiViewModel.sendMessage(
                          messageText = starter,
                          activeRole = activeRole,
                          language = language,
                          platformSnapshot = platformSnapshot,
                        )
                      },
                      label = {
                        Text(
                          text = starter,
                          style = MaterialTheme.typography.bodySmall,
                        )
                      },
                      modifier = Modifier.heightIn(min = 48.dp).testTag("fellah_ai_starter_chip_$idx"),
                    )
                  }
                }
              }
            }
          }

          items(messages, key = { it.id.ifBlank { "${it.sender}_${it.content.hashCode()}" } }) { msg ->
            ChatTurnBubble(message = msg, language = language)
          }

          if (uiState.isSending) {
            item {
              StreamingAssistantBubble(
                partialText = uiState.streamingReplyText,
                verifiedSummary = uiState.streamingVerifiedSummary,
                hasVerifiedData = uiState.streamingHasVerifiedData,
                language = language,
              )
            }
          }
        }

        // 7. Accessible Composer Row (Keyboard Enter/Send + RTL/LTR)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = uiState.draftInput,
            onValueChange = aiViewModel::updateDraftInput,
            enabled = !uiState.isSending,
            maxLines = 4,
            placeholder = {
              Text(
                RoleStrings.tr(
                  language,
                  ar = "اسأل بالعربية أو الدارجة أو الفرنسية أو الإنجليزية...",
                  fr = "Posez votre question agricole ou marché (AR, Darija, FR, EN)…",
                  en = "Ask about crops, listings, prices, transport, or costs…",
                ),
                style = MaterialTheme.typography.bodySmall,
              )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions =
              KeyboardActions(
                onSend = {
                  if (uiState.draftInput.isNotBlank() && !uiState.isSending) {
                    aiViewModel.sendMessage(
                      messageText = uiState.draftInput,
                      activeRole = activeRole,
                      language = language,
                      platformSnapshot = platformSnapshot,
                    )
                  }
                }
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).testTag("fellah_ai_input_field"),
          )

          Button(
            onClick = {
              aiViewModel.sendMessage(
                messageText = uiState.draftInput,
                activeRole = activeRole,
                language = language,
                platformSnapshot = platformSnapshot,
              )
            },
            enabled = !uiState.isSending,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(54.dp).testTag("fellah_ai_send_button"),
          ) {
            if (uiState.isSending) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription =
                  RoleStrings.tr(language, "إرسال السؤال", "Envoyer", "Send"),
              )
            }
          }
        }
      }
    }

    if (uiState.showPrivacyInfoDialog) {
      AlertDialog(
        onDismissRequest = { aiViewModel.setShowPrivacyInfoDialog(false) },
        title = {
          Text(
            RoleStrings.tr(
              language,
              ar = "الخصوصية وحفظ المحادثات في Fellah AI",
              fr = "Confidentialité & Conservation des Discussions",
              en = "Fellah AI Privacy & Conversation Retention",
            ),
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Text(
            RoleStrings.tr(
              language,
              ar = "• تُحفظ محادثات المستخدمين المسجلين بشكل خاص ومعزول في حسابك فقط لتمكينك من متابعتها لاحقاً، ويمكنك حذف أي محادثة نهائياً في أي وقت من زر سجل المحادثات.\n• في وضع الزائر، تكون المحادثة مؤقتة ولا تُحفظ على الخادم.\n• لا يشارك Fellah AI بياناتك الخاصة مع مستخدمين آخرين ولا ينفذ عمليات مالية أو طلبيات نيابة عنك.",
              fr = "• Vos conversations connectées sont isolées dans votre compte et peuvent être supprimées définitivement à tout moment.\n• En mode visiteur, aucune discussion n'est enregistrée sur le serveur.\n• Aucune donnée privée d'autres utilisateurs n'est accessible.",
              en = "• Signed-in conversations are privately scoped to your account and can be permanently deleted anytime from History.\n• Visitor sessions are ephemeral and never stored on the server.\n• Fellah AI never accesses other users' private records.",
            )
          )
        },
        confirmButton = {
          TextButton(
            onClick = { aiViewModel.setShowPrivacyInfoDialog(false) },
            modifier = Modifier.height(48.dp).testTag("fellah_ai_privacy_close_button"),
          ) {
            Text(RoleStrings.tr(language, "حسناً", "Fermer", "Got it"))
          }
        },
      )
    }
  }
}

@Composable
private fun ConversationHistoryCard(
  conversations: List<AiConversation>,
  activeConversationId: String?,
  language: AppLanguage,
  onSelect: (String) -> Unit,
  onDelete: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth().testTag("fellah_ai_history_panel"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text =
          RoleStrings.tr(
            language,
            ar = "سجل محادثاتك المحفوظة (${conversations.size})",
            fr = "Historique de vos conversations (${conversations.size})",
            en = "Saved Conversations (${conversations.size})",
          ),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )
      if (conversations.isEmpty()) {
        Text(
          text =
            RoleStrings.tr(
              language,
              ar = "لا توجد محادثات محفوظة بعد.",
              fr = "Aucune conversation enregistrée.",
              en = "No saved conversations yet.",
            ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        conversations.take(6).forEach { conv ->
          val selected = conv.id == activeConversationId
          Surface(
            shape = RoundedCornerShape(10.dp),
            color =
              if (selected) MaterialTheme.colorScheme.secondaryContainer
              else MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelect(conv.id) }
                .testTag("fellah_ai_conv_item_${conv.id}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = conv.title,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${conv.activeRole} • ${conv.messageCount} msg",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(
                onClick = { onDelete(conv.id) },
                modifier = Modifier.size(48.dp).testTag("fellah_ai_delete_conv_${conv.id}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.DeleteOutline,
                  contentDescription =
                    RoleStrings.tr(language, "حذف المحادثة", "Supprimer", "Delete conversation"),
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProductionCostCalculatorCard(
  language: AppLanguage,
  defaultAreaHa: Double,
  defaultYieldTonsHa: Double,
  onSubmitCalculation: (ProductionCostInput) -> Unit,
) {
  var cropName by rememberSaveable { mutableStateOf("بطاطا موسمية / دقلة نور") }
  var areaText by rememberSaveable { mutableStateOf(defaultAreaHa.toString()) }
  var seedsText by rememberSaveable { mutableStateOf("120000") }
  var fertText by rememberSaveable { mutableStateOf("85000") }
  var waterText by rememberSaveable { mutableStateOf("40000") }
  var laborText by rememberSaveable { mutableStateOf("95000") }
  var transportText by rememberSaveable { mutableStateOf("35000") }
  var yieldText by rememberSaveable { mutableStateOf(defaultYieldTonsHa.toString()) }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    modifier =
      Modifier.fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
        .testTag("fellah_ai_cost_calculator_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text =
          RoleStrings.tr(
            language,
            ar = "حاسبة تكلفة الإنتاج وسعر التعادل (بيانات مدخلة موثقة بالدينار د.ج)",
            fr = "Calculateur de Coût de Production & Seuil de Rentabilité (DZD)",
            en = "Production Cost & Break-Even Calculator (DZD)",
          ),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = cropName,
          onValueChange = { cropName = it },
          singleLine = true,
          label = { Text(RoleStrings.tr(language, "المحصول", "Culture", "Crop")) },
          modifier = Modifier.weight(1f).testTag("cost_calc_crop_input"),
        )
        OutlinedTextField(
          value = areaText,
          onValueChange = { areaText = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          label = { Text(RoleStrings.tr(language, "المساحة (Ha)", "Surface (Ha)", "Area (Ha)")) },
          modifier = Modifier.weight(1f).testTag("cost_calc_area_input"),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = seedsText,
          onValueChange = { seedsText = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          label = { Text(RoleStrings.tr(language, "البذور (د.ج)", "Semences", "Seeds DZD")) },
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = fertText,
          onValueChange = { fertText = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          label = { Text(RoleStrings.tr(language, "الأسمدة (د.ج)", "Engrais", "Fertilizer DZD")) },
          modifier = Modifier.weight(1f),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = waterText,
          onValueChange = { waterText = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          label = { Text(RoleStrings.tr(language, "السقي (د.ج)", "Irrigation", "Irrigation DZD")) },
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = laborText,
          onValueChange = { laborText = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          label = { Text(RoleStrings.tr(language, "العمالة والعتاد", "Main-d'œuvre", "Labor DZD")) },
          modifier = Modifier.weight(1f),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = transportText,
          onValueChange = { transportText = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          label = { Text(RoleStrings.tr(language, "النقل والتبريد", "Transport", "Logistics DZD")) },
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = yieldText,
          onValueChange = { yieldText = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          label = { Text(RoleStrings.tr(language, "المردود (طن/Ha)", "Rendement T/Ha", "Yield T/Ha")) },
          modifier = Modifier.weight(1f),
        )
      }

      Button(
        onClick = {
          onSubmitCalculation(
            ProductionCostInput(
              cropName = cropName,
              areaHectares = areaText.toDoubleOrNull() ?: 1.0,
              seedsCostDzd = seedsText.toDoubleOrNull() ?: 0.0,
              fertilizerCostDzd = fertText.toDoubleOrNull() ?: 0.0,
              irrigationWaterCostDzd = waterText.toDoubleOrNull() ?: 0.0,
              laborAndMachineryCostDzd = laborText.toDoubleOrNull() ?: 0.0,
              transportAndStorageCostDzd = transportText.toDoubleOrNull() ?: 0.0,
              expectedYieldTonsPerHa = yieldText.toDoubleOrNull() ?: 0.0,
            )
          )
        },
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cost_calc_submit_button"),
      ) {
        Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          RoleStrings.tr(
            language,
            ar = "حساب التكلفة الإجمالية وسعر التعادل",
            fr = "Calculer le coût total et le seuil de rentabilité",
            en = "Calculate Total Cost & Break-Even",
          )
        )
      }
    }
  }
}

@Composable
private fun ChatTurnBubble(
  message: AiChatMessageRecord,
  language: AppLanguage,
) {
  val isUser = message.sender == "user"
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (!isUser && message.hasVerifiedPlatformData && message.verifiedDataSummary.isNotBlank()) {
      VerifiedPlatformDataBadgeCard(
        summary = message.verifiedDataSummary,
        language = language,
      )
    }

    Surface(
      shape =
        RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (isUser) 16.dp else 4.dp,
          bottomEnd = if (isUser) 4.dp else 16.dp,
        ),
      color =
        if (isUser) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier =
        Modifier.widthIn(max = 560.dp)
          .testTag(if (isUser) "fellah_ai_user_bubble" else "fellah_ai_assistant_bubble"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (!isUser) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "توجيه ذكي مُولّد بواسطة Fellah AI",
                  fr = "Explication générée par Fellah AI",
                  en = "AI-Generated Explanation (Fellah AI)",
                ),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(vertical = 2.dp),
          )
        }

        Text(
          text = message.content,
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (isUser) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun StreamingAssistantBubble(
  partialText: String,
  verifiedSummary: String,
  hasVerifiedData: Boolean,
  language: AppLanguage,
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag("fellah_ai_loading_indicator"),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (hasVerifiedData && verifiedSummary.isNotBlank()) {
      VerifiedPlatformDataBadgeCard(summary = verifiedSummary, language = language)
    }

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.widthIn(max = 560.dp),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
        )
        Text(
          text =
            partialText.ifBlank {
              RoleStrings.tr(
                language,
                ar = "جارٍ التحقق من بيانات المنصة وصياغة الإجابة...",
                fr = "Vérification des données et génération de la réponse…",
                en = "Checking verified platform data and generating answer…",
              )
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun VerifiedPlatformDataBadgeCard(
  summary: String,
  language: AppLanguage,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
    modifier =
      Modifier.widthIn(max = 560.dp)
        .border(
          1.dp,
          MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
          RoundedCornerShape(12.dp),
        )
        .testTag("fellah_ai_verified_data_badge"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text =
            RoleStrings.tr(
              language,
              ar = "بيانات وحسابات موثقة من منصة فلاح نوميريك",
              fr = "Données & Calculs Vérifiés de la Plateforme",
              en = "Verified Platform Data & Calculations",
            ),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
          text = summary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}
