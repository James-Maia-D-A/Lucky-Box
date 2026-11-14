package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.CashTransaction
import com.example.data.model.DebtItem
import com.example.data.model.FinancialGoal
import com.example.data.repository.FinanceRepository
import com.example.notifications.DailyReminderReceiver
import com.example.notifications.DailyReminderScheduler
import com.example.notifications.ReminderPreferences
import com.example.ui.auth.signOutUser
import com.example.ui.components.DebtFormDialog
import com.example.ui.components.DebtPaymentDialog
import com.example.ui.components.GoalFormDialog
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.components.TransactionFormDialog
import com.example.ui.screens.DebtsTab
import com.example.ui.screens.DivisionTab
import com.example.ui.screens.GoalsTab
import com.example.ui.screens.LedgerTab
import com.example.ui.screens.OverviewTab
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class DashboardTab(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  OVERVIEW(R.string.tab_overview, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
  LEDGER(
    R.string.tab_ledger,
    Icons.AutoMirrored.Filled.ReceiptLong,
    Icons.AutoMirrored.Outlined.ReceiptLong,
  ),
  DIVISION(R.string.tab_division, Icons.Filled.PieChart, Icons.Outlined.PieChart),
  GOALS(R.string.tab_goals, Icons.Filled.Flag, Icons.Outlined.Flag),
  DEBTS(R.string.tab_debts, Icons.Filled.CreditScore, Icons.Outlined.CreditScore),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(currentUserId: String) {
  val viewModel: FinanceViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing from CreationExtras" }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            FinanceViewModel(FinanceRepository(db), currentUserId)
          }
        },
    )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.OVERVIEW) }
  var showMenu by remember { mutableStateOf(false) }
  var showRemindersDialog by remember { mutableStateOf(false) }

  var remindersEnabled by remember { mutableStateOf(ReminderPreferences.isEnabled(context)) }
  var hasNotificationPermission by remember {
    mutableStateOf(DailyReminderReceiver.canPostNotifications(context))
  }
  var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotificationPermission = granted && DailyReminderReceiver.canPostNotifications(context)
      if (hasNotificationPermission && remindersEnabled) {
        DailyReminderScheduler.scheduleAll(context)
      }
    }

  LaunchedEffect(Unit) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !hasNotificationPermission &&
        !askedNotificationPermission
    ) {
      askedNotificationPermission = true
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else if (remindersEnabled) {
      DailyReminderScheduler.scheduleAll(context)
    }
  }

  LaunchedEffect(uiState) {
    val state = uiState
    if (state is FinanceUiState.Ready) {
      val activeGoals =
        state.summary.goalProjections.filter { !it.isAchieved }.map { it.goal.title }
      val activeDebts =
        state.summary.debtSuggestions.filter { it.remainingDebt > 0.0 }.map { it.debt.title }
      ReminderPreferences.syncContext(context, activeGoals, activeDebts)
    }
  }

  // Dialog states
  var showTxDialog by remember { mutableStateOf(false) }
  var editingTx by remember { mutableStateOf<CashTransaction?>(null) }

  var showGoalDialog by remember { mutableStateOf(false) }
  var editingGoal by remember { mutableStateOf<FinancialGoal?>(null) }

  var showDebtDialog by remember { mutableStateOf(false) }
  var editingDebt by remember { mutableStateOf<DebtItem?>(null) }

  var payingDebtPair by remember { mutableStateOf<Pair<DebtItem, Double>?>(null) }

  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearFeedback()
    }
  }

  if (selectedTab != DashboardTab.OVERVIEW) {
    BackHandler { selectedTab = DashboardTab.OVERVIEW }
  }

  // Modal dialogs
  if (showTxDialog || editingTx != null) {
    TransactionFormDialog(
      initial = editingTx,
      onDismiss = {
        showTxDialog = false
        editingTx = null
      },
      onSave = { type, amount, desc, cat, dateMillis ->
        val existing = editingTx
        if (existing == null) {
          viewModel.addTransaction(type, amount, desc, cat, dateMillis)
        } else {
          viewModel.updateTransaction(
            existing.copy(
              type = type.value,
              amount = amount,
              description = desc,
              category = cat,
              dateMillis = dateMillis,
            )
          )
        }
        showTxDialog = false
        editingTx = null
      },
    )
  }

  if (showGoalDialog || editingGoal != null) {
    GoalFormDialog(
      initial = editingGoal,
      onDismiss = {
        showGoalDialog = false
        editingGoal = null
      },
      onSave = { title, targetAmount, notes ->
        val existing = editingGoal
        if (existing == null) {
          viewModel.addGoal(title, targetAmount, notes)
        } else {
          viewModel.updateGoal(
            existing.copy(title = title, targetAmount = targetAmount, notes = notes)
          )
        }
        showGoalDialog = false
        editingGoal = null
      },
    )
  }

  if (showDebtDialog || editingDebt != null) {
    DebtFormDialog(
      initial = editingDebt,
      onDismiss = {
        showDebtDialog = false
        editingDebt = null
      },
      onSave = { title, creditor, totalAmount, paidAmount ->
        val existing = editingDebt
        if (existing == null) {
          viewModel.addDebt(title, creditor, totalAmount, paidAmount)
        } else {
          viewModel.updateDebt(
            existing.copy(
              title = title,
              creditor = creditor,
              totalAmount = totalAmount,
              paidAmount = paidAmount,
            )
          )
        }
        showDebtDialog = false
        editingDebt = null
      },
    )
  }

  payingDebtPair?.let { (debt, suggested) ->
    DebtPaymentDialog(
      debt = debt,
      suggestedInstallment = suggested,
      onDismiss = { payingDebtPair = null },
      onConfirmPayment = { payment ->
        viewModel.registerDebtPayment(debt, payment)
        payingDebtPair = null
      },
    )
  }

  if (showRemindersDialog) {
    ReminderSettingsDialog(
      remindersEnabled = remindersEnabled,
      hasNotificationPermission = hasNotificationPermission,
      onToggleEnabled = { enabled ->
        remindersEnabled = enabled
        ReminderPreferences.setEnabled(context, enabled)
        if (enabled) {
          if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
              !DailyReminderReceiver.canPostNotifications(context)
          ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            DailyReminderScheduler.scheduleAll(context)
          }
        } else {
          DailyReminderScheduler.cancelAll(context)
        }
      },
      onRequestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      },
      onSendTestNotification = {
        val posted = DailyReminderReceiver.showReminderNotification(context, notificationId = 1099)
        if (posted) {
          coroutineScope.launch {
            snackbarHostState.showSnackbar("Notificação de exemplo enviada!")
          }
        }
      },
      onDismiss = { showRemindersDialog = false },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = stringResource(selectedTab.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
          actions = {
            IconButton(
              onClick = {
                hasNotificationPermission = DailyReminderReceiver.canPostNotifications(context)
                showRemindersDialog = true
              },
              modifier = Modifier.testTag("btn_reminder_settings"),
            ) {
              Icon(
                imageVector =
                  if (remindersEnabled && hasNotificationPermission) {
                    Icons.Outlined.NotificationsActive
                  } else {
                    Icons.Outlined.NotificationsOff
                  },
                contentDescription = stringResource(R.string.reminders_icon_desc),
              )
            }
            Box {
              IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.testTag("btn_account_menu"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Logout,
                  contentDescription = stringResource(R.string.sign_out),
                )
              }
              DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.sign_out)) },
                  leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                  },
                  onClick = {
                    showMenu = false
                    ReminderPreferences.clearContext(context)
                    DailyReminderScheduler.cancelAll(context)
                    signOutUser(credentialManager, coroutineScope)
                  },
                )
              }
            }
          },
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            DashboardTab.entries.forEach { tab ->
              val selected = selectedTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { selectedTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
              )
            }
          }
        }
      },
      floatingActionButton = {
        when (selectedTab) {
          DashboardTab.OVERVIEW,
          DashboardTab.LEDGER,
          DashboardTab.DIVISION ->
            FloatingActionButton(
              onClick = { showTxDialog = true },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("fab_add_tx"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = "Nova movimentação")
            }
          DashboardTab.GOALS ->
            FloatingActionButton(
              onClick = { showGoalDialog = true },
              containerColor = MaterialTheme.colorScheme.secondary,
              contentColor = MaterialTheme.colorScheme.onSecondary,
              modifier = Modifier.testTag("fab_add_goal"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = "Novo objetivo")
            }
          DashboardTab.DEBTS ->
            FloatingActionButton(
              onClick = { showDebtDialog = true },
              containerColor = MaterialTheme.colorScheme.tertiary,
              contentColor = MaterialTheme.colorScheme.onTertiary,
              modifier = Modifier.testTag("fab_add_debt"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = "Nova dívida")
            }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier =
              Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.navigationBars),
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            DashboardTab.entries.forEach { tab ->
              val selected = selectedTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { selectedTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
              )
            }
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
            when (val state = uiState) {
              FinanceUiState.Loading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    CircularProgressIndicator()
                    Text(
                      text = "Carregando seu baú Lucky Box...",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

              is FinanceUiState.Error ->
                Box(
                  modifier = Modifier.fillMaxSize().padding(24.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                  )
                }

              is FinanceUiState.Ready ->
                when (selectedTab) {
                  DashboardTab.OVERVIEW ->
                    OverviewTab(
                      summary = state.summary,
                      transactionCount = state.transactions.size,
                      onNavigateToLedger = { selectedTab = DashboardTab.LEDGER },
                    )
                  DashboardTab.LEDGER ->
                    LedgerTab(
                      transactions = state.transactions,
                      onEditTransaction = { editingTx = it },
                      onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    )
                  DashboardTab.DIVISION -> DivisionTab(summary = state.summary)
                  DashboardTab.GOALS ->
                    GoalsTab(
                      summary = state.summary,
                      onEditGoal = { editingGoal = it },
                      onDeleteGoal = { viewModel.deleteGoal(it) },
                    )
                  DashboardTab.DEBTS ->
                    DebtsTab(
                      summary = state.summary,
                      onEditDebt = { editingDebt = it },
                      onPayDebt = { debt, suggested -> payingDebtPair = debt to suggested },
                      onDeleteDebt = { viewModel.deleteDebt(it) },
                    )
                }
            }
          }
        }
      }
    }
  }
}
