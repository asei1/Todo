package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TodoEntity
import com.example.ui.theme.*
import com.example.viewmodel.TodoViewModel
import com.example.util.HangulEngine
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import java.text.SimpleDateFormat
import java.util.*

fun isHanEngSwitchKey(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
    val nativeEvent = event.nativeKeyEvent
    val keyCode = nativeEvent.keyCode
    val isShiftSpace = keyCode == android.view.KeyEvent.KEYCODE_SPACE && nativeEvent.isShiftPressed
    
    return event.type == KeyEventType.KeyDown && (
        keyCode == android.view.KeyEvent.KEYCODE_ALT_RIGHT ||
        keyCode == android.view.KeyEvent.KEYCODE_LANGUAGE_SWITCH ||
        keyCode == 170 ||
        keyCode == 218 ||
        keyCode == 21 ||
        keyCode == 211 ||
        keyCode == 213 ||
        keyCode == 214 ||
        isShiftSpace
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val todoViewModel: TodoViewModel = viewModel()
            val themePref by todoViewModel.themeMode.collectAsState()

            val isDarkTheme = when (themePref) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoAppScreen(viewModel = todoViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoAppScreen(viewModel: TodoViewModel) {
    val context = LocalContext.current
    val todos by viewModel.filteredTodos.collectAsState()
    val allTodosList by viewModel.todos.collectAsState()

    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedPriorityFilter by viewModel.selectedPriorityFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var todoToEdit by remember { mutableStateOf<TodoEntity?>(null) }

    // Search Input states (with integrated English to Korean keyboard mapping)
    var searchKeyboardIsKorean by remember { mutableStateOf(true) }
    val searchHangulEngine = remember { HangulEngine() }

    // Dynamic notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "일정 알림을 받으려면 알림 권한 허용이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to check and launch permission
    val checkNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    checkNotificationPermission()
                    showAddDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_todo_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "할 일 추가")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Adaptive Container (restricted width for tablets/large screens)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp)
            ) {
                // Header Banner
                DashboardHeader(
                    allTodos = allTodosList,
                    onSettingsClick = { showSettingsDialog = true }
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        val processed = searchHangulEngine.processTextInput(searchQuery, newValue, searchKeyboardIsKorean)
                        viewModel.searchQuery.value = processed
                    },
                    placeholder = { 
                        Text(
                            text = if (searchKeyboardIsKorean) "할 일 검색... (한글)" else "할 일 검색... (English)", 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ) 
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.searchQuery.value = "" 
                                searchHangulEngine.clear()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "검색 초기화")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_bar")
                        .onKeyEvent { event ->
                            if (isHanEngSwitchKey(event)) {
                                searchKeyboardIsKorean = !searchKeyboardIsKorean
                                searchHangulEngine.clear()
                                searchHangulEngine.setInitialText(searchQuery)
                                true
                            } else {
                                false
                            }
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    singleLine = true
                )

                // Category & Priority Filters Row
                FilterChipsSection(
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelect = { viewModel.selectedCategoryFilter.value = it },
                    selectedPriority = selectedPriorityFilter,
                    onPrioritySelect = { viewModel.selectedPriorityFilter.value = it }
                )

                // Dynamic High Priority Alert Banner from "Professional Polish" design theme
                val uncompletedHighPriorityTasks = allTodosList.filter { !it.isCompleted && it.priority == "HIGH" }
                if (uncompletedHighPriorityTasks.isNotEmpty()) {
                    HighPriorityBanner(
                        highPriorityTasksCount = uncompletedHighPriorityTasks.size,
                        nextTaskTitle = uncompletedHighPriorityTasks.first().title
                    )
                }

                // Task List Section
                if (todos.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(todos, key = { it.id }) { item ->
                            TodoItemCard(
                                todo = item,
                                onToggleComplete = { viewModel.toggleComplete(item) },
                                onEdit = { todoToEdit = item },
                                onDelete = { viewModel.deleteTodo(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        AddEditTodoDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, cat, pri, due, periodic, repeat, rTime, rEnabled ->
                viewModel.addTodo(
                    title, desc, cat, pri, due, periodic, repeat, rTime, rEnabled
                )
                showAddDialog = false
            }
        )
    }

    // Edit Dialog
    todoToEdit?.let { todo ->
        AddEditTodoDialog(
            todoToEdit = todo,
            onDismiss = { todoToEdit = null },
            onSave = { title, desc, cat, pri, due, periodic, repeat, rTime, rEnabled ->
                viewModel.updateTodo(
                    todo.copy(
                        title = title,
                        description = desc,
                        category = cat,
                        priority = pri,
                        dueDate = due,
                        isPeriodic = periodic,
                        repeatMode = repeat,
                        reminderTime = rTime,
                        isReminderEnabled = rEnabled
                    )
                )
                todoToEdit = null
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentMode = viewModel.themeMode.collectAsState().value,
            onModeSelect = { viewModel.setThemeMode(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun DashboardHeader(
    allTodos: List<TodoEntity>,
    onSettingsClick: () -> Unit
) {
    val totalTasks = allTodos.size
    val completedTasks = allTodos.count { it.isCompleted }
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    val dayFormat = SimpleDateFormat("EEEE", Locale.KOREAN)
    val dateString = dateFormat.format(calendar.time)
    val dayString = dayFormat.format(calendar.time)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        // Image background with fallback
        Image(
            painter = painterResource(id = com.example.R.drawable.img_banner),
            contentDescription = "Header Banner Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Top Controls: App Name & Settings Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_todo_icon),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Todo List",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            IconButton(
                onClick = onSettingsClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = Color.White
                )
            }
        }

        // Dashboard Stats (Aligned Bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "$dateString ($dayString)",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = if (totalTasks == 0) {
                    "오늘의 할 일을 시작해보세요!"
                } else {
                    "총 ${totalTasks}개 중 ${completedTasks}개 완료"
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            if (totalTasks > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = SlateDarkPrimary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipsSection(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    selectedPriority: String,
    onPrioritySelect: (String) -> Unit
) {
    val categories = listOf("전체", "업무", "개인", "쇼핑", "건강", "학습", "기타")
    val priorities = listOf("전체", "높음", "보통", "낮음")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Category row
        Text(
            text = "📁 카테고리",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                InputChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(cat) },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Priority row
        Text(
            text = "⚡ 우선순위",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            priorities.forEach { pri ->
                val isSelected = selectedPriority == pri
                InputChip(
                    selected = isSelected,
                    onClick = { onPrioritySelect(pri) },
                    label = { Text(pri, fontSize = 12.sp) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun HighPriorityBanner(
    highPriorityTasksCount: Int,
    nextTaskTitle: String
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) Color(0xFF31111D) else Color(0xFFFFEBEE)
    val borderCol = if (isDark) Color(0xFF93000A) else Color(0xFFFFCDD2)
    val textCol = if (isDark) Color(0xFFFFB4AB) else Color(0xFFC62828)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "경고",
                tint = textCol,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = "중요도 높음 (남은 할 일: ${highPriorityTasksCount}개)",
                    fontSize = 11.sp,
                    color = textCol,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = nextTaskTitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "자세히 보기",
                tint = textCol.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TodoItemCard(
    todo: TodoEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (todo.category) {
        "업무" -> ColorWork
        "개인" -> ColorPersonal
        "쇼핑" -> ColorShopping
        "건강" -> ColorHealth
        "학습" -> ColorStudy
        else -> ColorOthers
    }

    val priorityColor = when (todo.priority) {
        "HIGH" -> PriorityHigh
        "MEDIUM" -> PriorityMedium
        else -> PriorityLow
    }

    val priorityLabel = when (todo.priority) {
        "HIGH" -> "높음"
        "MEDIUM" -> "보통"
        else -> "낮음"
    }

    val categoryIcon = when (todo.category) {
        "업무" -> Icons.Default.Work
        "개인" -> Icons.Default.Person
        "쇼핑" -> Icons.Default.ShoppingCart
        "건강" -> Icons.Default.Favorite
        "학습" -> Icons.Default.School
        else -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todo_item_${todo.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (todo.isCompleted) 0.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox Circle
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = if (todo.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "완료 토글",
                    tint = if (todo.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title
                    Text(
                        text = todo.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (todo.isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Priority indicator line
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                }

                if (todo.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todo.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (todo.isCompleted) 0.3f else 0.6f
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Details Row: Category, Priority, Date, Alarms
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = todo.category,
                                tint = categoryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = todo.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                        }
                    }

                    // Priority Label Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "우선순위: $priorityLabel",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }

                    // Due Date Badge (if exists)
                    todo.dueDate?.let { due ->
                        val sdf = SimpleDateFormat("MM/dd", Locale.KOREAN)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "마감일",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = sdf.format(Date(due)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Reminder Alarm Icon (if enabled)
                    if (todo.isReminderEnabled && !todo.reminderTime.isNullOrEmpty()) {
                        val repeatLabel = when (todo.repeatMode) {
                            "DAILY" -> "매일"
                            "WEEKLY" -> "매주"
                            "MONTHLY" -> "매월"
                            else -> "한번"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = "알림",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${todo.reminderTime} ($repeatLabel)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Column(
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "수정",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AssignmentTurnedIn,
            contentDescription = "할 일 없음",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "할 일이 비어있습니다!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "새로운 할 일을 추가하고 일정을 관리해보세요.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.clickable { /* No-op, just informative */ }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditTodoDialog(
    todoToEdit: TodoEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: Long?,
        isPeriodic: Boolean,
        repeatMode: String,
        reminderTime: String?,
        isReminderEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(todoToEdit?.title ?: "") }
    var description by remember { mutableStateOf(todoToEdit?.description ?: "") }
    var category by remember { mutableStateOf(todoToEdit?.category ?: "업무") }
    var priority by remember { mutableStateOf(todoToEdit?.priority ?: "MEDIUM") }
    var dueDate by remember { mutableStateOf(todoToEdit?.dueDate) }
    
    var isPeriodic by remember { mutableStateOf(todoToEdit?.isPeriodic ?: false) }
    var repeatMode by remember { mutableStateOf(todoToEdit?.repeatMode ?: "NONE") }
    var reminderTime by remember { mutableStateOf(todoToEdit?.reminderTime) }
    var isReminderEnabled by remember { mutableStateOf(todoToEdit?.isReminderEnabled ?: false) }

    // Dialog Input states with Hangul conversion engines
    var titleIsKorean by remember { mutableStateOf(true) }
    var descIsKorean by remember { mutableStateOf(true) }
    val titleHangulEngine = remember { HangulEngine() }
    val descHangulEngine = remember { HangulEngine() }

    LaunchedEffect(todoToEdit) {
        titleHangulEngine.setInitialText(todoToEdit?.title ?: "")
        descHangulEngine.setInitialText(todoToEdit?.description ?: "")
    }

    val categories = listOf("업무", "개인", "쇼핑", "건강", "학습", "기타")
    val repeatModes = listOf("NONE" to "한번", "DAILY" to "매일", "WEEKLY" to "매주", "MONTHLY" to "매월")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 500.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                item {
                    Text(
                        text = if (todoToEdit == null) "📝 새로운 할 일" else "✏️ 할 일 수정",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { newValue ->
                            title = titleHangulEngine.processTextInput(title, newValue, titleIsKorean)
                        },
                        label = { Text(if (titleIsKorean) "할 일 제목 (한글)" else "할 일 제목 (English)") },
                        placeholder = { Text("예: 리포트 작성하기") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("todo_title_input")
                            .onKeyEvent { event ->
                                if (isHanEngSwitchKey(event)) {
                                    titleIsKorean = !titleIsKorean
                                    titleHangulEngine.clear()
                                    titleHangulEngine.setInitialText(title)
                                    true
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Description Input
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { newValue ->
                            description = descHangulEngine.processTextInput(description, newValue, descIsKorean)
                        },
                        label = { Text(if (descIsKorean) "상세 메모 (선택 - 한글)" else "상세 메모 (선택 - English)") },
                        placeholder = { Text("상세 내용이나 체크리스트...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("todo_desc_input")
                            .onKeyEvent { event ->
                                if (isHanEngSwitchKey(event)) {
                                    descIsKorean = !descIsKorean
                                    descHangulEngine.clear()
                                    descHangulEngine.setInitialText(description)
                                    true
                                } else {
                                    false
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }

                // Category Grid Selector
                item {
                    Text(
                        text = "카테고리 선택",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            val color = when (cat) {
                                "업무" -> ColorWork
                                "개인" -> ColorPersonal
                                "쇼핑" -> ColorShopping
                                "건강" -> ColorHealth
                                "학습" -> ColorStudy
                                else -> ColorOthers
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Priority Selection
                item {
                    Text(
                        text = "우선순위",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HIGH" to "높음", "MEDIUM" to "보통", "LOW" to "낮음").forEach { (level, label) ->
                            val isSelected = priority == level
                            val color = when (level) {
                                "HIGH" -> PriorityHigh
                                "MEDIUM" -> PriorityMedium
                                else -> PriorityLow
                            }
                            Button(
                                onClick = { priority = level },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Date & Time Scheduling Selection
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "📅 일정 관리 및 알림 설정",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    // Due Date Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (dueDate == null) "마감 기한 없음" else "마감 기한: " + SimpleDateFormat("yyyy/MM/dd", Locale.KOREAN).format(Date(dueDate!!)),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row {
                            if (dueDate != null) {
                                IconButton(onClick = { dueDate = null }) {
                                    Icon(Icons.Default.Clear, contentDescription = "기한 초기화")
                                }
                            }
                            Button(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    val dpd = DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected = Calendar.getInstance().apply {
                                                set(year, month, day)
                                            }
                                            dueDate = selected.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    )
                                    dpd.show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("날짜 선택", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Reminder Alarm Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "⏰ 정시 푸시 알림 설정",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isReminderEnabled && reminderTime != null) {
                                Text(
                                    text = "설정 시간: $reminderTime",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { isReminderEnabled = it }
                        )
                    }
                }

                // If alarm enabled, show Time Picker & Repeat dropdown options
                if (isReminderEnabled) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Time Picker Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (reminderTime == null) "알림 시간을 설정하세요" else "알림 예정 시간: $reminderTime",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        val tpd = TimePickerDialog(
                                            context,
                                            { _, hour, min ->
                                                reminderTime = String.format("%02d:%02d", hour, min)
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true
                                        )
                                        tpd.show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("시간 선택", fontSize = 11.sp)
                                }
                            }

                            // Repeat Mode Select
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("주기 반복 설정", fontSize = 13.sp)
                                
                                // Simple Selector for repeat mode
                                var expandedDropdown by remember { mutableStateOf(false) }
                                val currentModeLabel = repeatModes.find { it.first == repeatMode }?.second ?: "설정 안 함"

                                Box {
                                    Button(
                                        onClick = { expandedDropdown = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(currentModeLabel, fontSize = 11.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(
                                        expanded = expandedDropdown,
                                        onDismissRequest = { expandedDropdown = false }
                                    ) {
                                        repeatModes.forEach { (mode, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    repeatMode = mode
                                                    isPeriodic = mode != "NONE"
                                                    expandedDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action controls: Cancel / Save
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("취소")
                        }

                        Button(
                            onClick = {
                                if (title.trim().isEmpty()) {
                                    Toast.makeText(context, "할 일 제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (isReminderEnabled && reminderTime.isNullOrEmpty()) {
                                    Toast.makeText(context, "알림 시간을 지정해주세요.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    category,
                                    priority,
                                    dueDate,
                                    isPeriodic,
                                    repeatMode,
                                    reminderTime,
                                    isReminderEnabled
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("todo_save_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("저장")
                        }
                    }
                }
            }

        }
    }
}
}

@Composable
fun SettingsDialog(
    currentMode: String,
    onModeSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚙️ 설정 및 테마",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "화면 테마 모드",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Theme selection list
                listOf(
                    "SYSTEM" to "시스템 기본값",
                    "LIGHT" to "라이트 모드",
                    "DARK" to "다크 모드"
                ).forEach { (mode, label) ->
                    val isSelected = currentMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModeSelect(mode) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (mode) {
                                    "LIGHT" -> Icons.Default.LightMode
                                    "DARK" -> Icons.Default.DarkMode
                                    else -> Icons.Default.SettingsSuggest
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onModeSelect(mode) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("닫기")
                }
            }
        }
    }
}
