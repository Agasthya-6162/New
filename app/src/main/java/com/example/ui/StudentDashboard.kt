package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    repository: CollegeRepository,
    sessionManager: SessionManager,
    currentScreen: MutableState<AppScreen>,
    userId: Long
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Current navigation tab inside dashboard
    var selectedDrawerTab by remember { mutableStateOf("Attendance") } // "Profile", "Attendance", "Notifications", "Events", "Exams", "Scholarships", "Gallery"

    // Fetch student info
    val studentProfileState = repository.getStudentProfile(userId).collectAsState(initial = null)
    val userState = repository.getUserById(userId).collectAsState(initial = null)

    // Side Drawer items
    val drawerItems = listOf(
        NavigationRowItem("Attendance", Icons.Default.CheckCircle, "Present checks"),
        NavigationRowItem("Profile", Icons.Default.Person, "My details"),
        NavigationRowItem("Notifications", Icons.Default.Notifications, "Updates"),
        NavigationRowItem("Events", Icons.Default.Event, "Fests"),
        NavigationRowItem("Exams Schedule", Icons.Default.Assignment, "Timetable"),
        NavigationRowItem("Scholarships", Icons.Default.CardMembership, "Grants"),
        NavigationRowItem("Gallery", Icons.Default.Collections, "Campus life")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepBlue)
                        .padding(20.dp)
                ) {
                    val profile = studentProfileState.value
                    val user = userState.value

                    Surface(
                        color = Amber,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        if (profile != null && profile.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profile.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (user?.name ?: "S").take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user?.name ?: "Student Portal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Roll No: ${profile?.rollNo ?: "Pending Roll"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Surface(
                        color = Amber,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = "${profile?.course ?: "ICP"} - ${profile?.year ?: "Pharm"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Items
                drawerItems.forEach { item ->
                    val selected = selectedDrawerTab == item.label
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null, tint = if (selected) Color.White else DeepBlue) },
                        label = { Text(item.label, fontWeight = FontWeight.Bold) },
                        selected = selected,
                        onClick = {
                            selectedDrawerTab = item.label
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = DeepBlue,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .height(48.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Logout Bottom drawer link
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ErrorRed) },
                    label = { Text("Log Out", color = ErrorRed, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        sessionManager.clearSession()
                        currentScreen.value = AppScreen.Login
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(bottom = 16.dp)
                )
            }
        }
    ) {
        val pageTitle = when (selectedDrawerTab) {
            "Exams Schedule" -> "Examination Timetable"
            "Scholarships" -> "Scholarships & Grants"
            else -> selectedDrawerTab
        }

        Scaffold(
            topBar = {
                HeaderBar(
                    title = pageTitle,
                    onBack = { coroutineScope.launch { drawerState.open() } } // Use back button to trigger side drawer open!
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(SoftGrey)
            ) {
                // Navigation components switcher
                when (selectedDrawerTab) {
                    "Attendance" -> StudentAttendanceView(repository, userId)
                    "Profile" -> StudentProfileView(userState.value, studentProfileState.value)
                    "Notifications" -> StudentNotificationsView(repository)
                    "Events" -> StudentEventsView(repository)
                    "Exams Schedule" -> StudentExamsView(repository, studentProfileState.value)
                    "Scholarships" -> StudentScholarshipsView(repository)
                    "Gallery" -> StudentGalleryView(repository)
                }
            }
        }
    }
}

data class NavigationRowItem(val label: String, val icon: ImageVector, val desc: String)

// ==========================================
// 1. ATTENDANCE SUB-VIEW
// ==========================================

@Composable
fun AttendanceTrendsLineGraph(trendPoints: List<Pair<String, Float>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ATTENDANCE VELOCITY TRENDS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Current Semester Timeline Chart",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                }

                Surface(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        text = "Live Sync",
                        color = SuccessGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom high-fidelity canvas area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 90f
                    val paddingRight = 30f
                    val paddingTop = 20f
                    val paddingBottom = 40f

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // Draw static y-axis lines: 0%, 50%, 100%
                    val yTops = listOf(0f, 50f, 100f)
                    yTops.forEach { gridValue ->
                        val ratio = gridValue / 100f
                        val yPos = height - paddingBottom - (ratio * chartHeight)

                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = androidx.compose.ui.geometry.Offset(paddingLeft, yPos),
                            end = androidx.compose.ui.geometry.Offset(width - paddingRight, yPos),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )

                        // Draw Y value labels
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 26f
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                            drawText("${gridValue.toInt()}%", paddingLeft - 12f, yPos + 8f, paint)
                        }
                    }

                    if (trendPoints.isNotEmpty()) {
                        val numPoints = trendPoints.size
                        val stepX = if (numPoints > 1) chartWidth / (numPoints - 1) else chartWidth

                        val pixelOffsets = trendPoints.mapIndexed { index, pair ->
                            val x = paddingLeft + (index * stepX)
                            val yRatio = (pair.second / 100f).coerceIn(0f, 1f)
                            val y = height - paddingBottom - (yRatio * chartHeight)
                            androidx.compose.ui.geometry.Offset(x, y)
                        }

                        // Path calculation (clean smooth lines)
                        val linePath = Path().apply {
                            pixelOffsets.forEachIndexed { idx, point ->
                                if (idx == 0) {
                                    moveTo(point.x, point.y)
                                } else {
                                    val prev = pixelOffsets[idx - 1]
                                    cubicTo(
                                        (prev.x + point.x) / 2, prev.y,
                                        (prev.x + point.x) / 2, point.y,
                                        point.x, point.y
                                    )
                                }
                            }
                        }

                        // Semi-transparent gradient brush under path
                        val underGradientPath = Path().apply {
                            moveTo(paddingLeft, height - paddingBottom)
                            pixelOffsets.forEachIndexed { idx, point ->
                                if (idx == 0) {
                                    lineTo(point.x, point.y)
                                } else {
                                    val prev = pixelOffsets[idx - 1]
                                    cubicTo(
                                        (prev.x + point.x) / 2, prev.y,
                                        (prev.x + point.x) / 2, point.y,
                                        point.x, point.y
                                    )
                                }
                            }
                            lineTo(paddingLeft + chartWidth, height - paddingBottom)
                            close()
                        }

                        // Fill color
                        drawPath(
                            path = underGradientPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(DeepBlue.copy(alpha = 0.25f), Color.Transparent),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )

                        // Stroke outline
                        drawPath(
                            path = linePath,
                            color = DeepBlue,
                            style = Stroke(
                                width = 5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw interactive Node dots
                        pixelOffsets.forEachIndexed { idx, offset ->
                            drawCircle(
                                color = Color.White,
                                radius = 7f,
                                center = offset
                            )
                            drawCircle(
                                color = DeepBlue,
                                radius = 4f,
                                center = offset
                            )

                            // Show X labels
                            if (numPoints <= 8 || idx % (numPoints / 4).coerceAtLeast(1) == 0 || idx == numPoints - 1) {
                                val labelText = trendPoints[idx].first
                                drawContext.canvas.nativeCanvas.apply {
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 24f
                                        isAntiAlias = true
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    drawText(labelText, offset.x, height - 8f, paint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceView(repository: CollegeRepository, userId: Long) {
    // Dynamic Query Flow
    val attendanceListState = repository.getAttendanceForStudent(userId).collectAsState(initial = emptyList())
    val subjectsState = repository.allSubjects.collectAsState(initial = emptyList())

    val attendanceList = attendanceListState.value
    val subjects = subjectsState.value

    if (attendanceList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, "None", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No attendance records found yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    } else {
        val approvedAttendance = remember(attendanceList) {
            attendanceList.filter { it.approvalStatus == "Approved" }
        }

        val trendPoints = remember(approvedAttendance) {
            val sorted = approvedAttendance.sortedBy { att ->
                try {
                    val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ENGLISH)
                    sdf.parse(att.date.trim())?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
            var runningCount = 0
            var runningPresent = 0
            sorted.map { att ->
                runningCount++
                if (att.status == "Present") {
                    runningPresent++
                }
                val pct = if (runningCount > 0) {
                    (runningPresent.toFloat() / runningCount.toFloat()) * 100f
                } else {
                    100f
                }
                val shortDate = try {
                    val fullSdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ENGLISH)
                    val shortSdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.ENGLISH)
                    fullSdf.parse(att.date.trim())?.let { shortSdf.format(it) } ?: att.date
                } catch (e: Exception) {
                    att.date
                }
                Pair(shortDate, pct)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Dynamic overall calculation:
            val overallTotal = approvedAttendance.size
            val overallPresent = approvedAttendance.count { it.status == "Present" }
            val overallPercentage = if (overallTotal > 0) {
                (overallPresent.toFloat() / overallTotal.toFloat()) * 100f
            } else {
                100f
            }

            val badgeColor = when {
                overallPercentage > 75f -> SuccessGreen
                overallPercentage in 60f..75f -> WarningOrange
                else -> ErrorRed
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = badgeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "OVERALL PORTAL ATTENDANCE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${overallPercentage.toInt()}%",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Present classes: $overallPresent / $overallTotal sessions approved",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            if (trendPoints.isNotEmpty()) {
                item {
                    AttendanceTrendsLineGraph(trendPoints = trendPoints)
                }
            }

            item {
                Text(
                    text = "SUBJECT-WISE BREAKDOWN",
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Create circular component per subject
            if (subjects.isEmpty()) {
                item {
                    Text("No subject cards loaded.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(subjects) { subject ->
                    val subPresent = approvedAttendance.count { it.subjectId == subject.id && it.status == "Present" }
                    val subAbsent = approvedAttendance.count { it.subjectId == subject.id && it.status == "Absent" }
                    val hasDataForSubject = approvedAttendance.any { it.subjectId == subject.id }

                    if (hasDataForSubject) {
                        SubjectAttendanceCircularIndicator(
                            subjectName = subject.name,
                            presentCount = subPresent,
                            absentCount = subAbsent
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. PROFILE READ-ONLY SUB-VIEW
// ==========================================

@Composable
fun StudentProfileView(user: User?, profile: StudentProfile?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Circular Image
                Surface(
                    color = DeepBlue,
                    shape = CircleShape,
                    modifier = Modifier.size(96.dp)
                ) {
                    if (profile != null && profile.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user?.name ?: "S").take(1).uppercase(),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user?.name ?: "Agasthya Verma",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Text(
                    text = "ID: ${profile?.rollNo ?: "Roll No Pending"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Structured keys value
                val labels = listOf(
                    "Course Degree" to (profile?.course ?: "Not Assigned"),
                    "Current Year" to (profile?.year ?: "Not Assigned"),
                    "Division Unit" to (profile?.division ?: "Not Assigned"),
                    "Official Email" to (user?.email ?: "Not Assigned"),
                    "Contact Phone" to (profile?.phone ?: "Not Assigned"),
                    "Parent Name" to (profile?.parentName ?: "Not Assigned"),
                    "Residence Address" to (profile?.address ?: "Not Assigned"),
                    "Aadhar ID (Masked)" to (if (profile?.aadharNo?.isNotEmpty() == true) {
                        "XXXX-XXXX-${profile.aadharNo.takeLast(4)}"
                    } else "XXXX-XXXX-1234")
                )

                labels.forEach { field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = field.first,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = field.second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f).padding(paddingValuesLeftOffset())
                        )
                    }
                }
            }
        }
    }
}

private fun paddingValuesLeftOffset() = PaddingValues(start = 16.dp)

// ==========================================
// 3. NOTIFICATIONS LIST VIEW
// ==========================================

@Composable
fun StudentNotificationsView(repository: CollegeRepository) {
    val context = LocalContext.current
    val notificationListState = repository.allNotifications.collectAsState(initial = emptyList())
    val list = notificationListState.value

    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, "null", tint = Color.LightGray, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No active circular notifications found.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(list) { notif ->
                val cardIcon = when (notif.type) {
                    "Exam" -> Icons.Default.Assignment
                    "Event" -> Icons.Default.Event
                    "Scholarship" -> Icons.Default.CardMembership
                    else -> Icons.Default.Notifications
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Amber.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(cardIcon, "notif icon", tint = Amber, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notif.message, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(notif.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            if (!notif.attachmentUri.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Amber.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                        .border(1.dp, Amber.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse(notif.attachmentUri)
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open: No valid handler / application found for file", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, "Pdf doc", tint = Amber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = notif.attachmentName ?: "Attached Circular Document",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.OpenInNew, "open", tint = DeepBlue, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. EVENTS SCREEN
// ==========================================

@Composable
fun StudentEventsView(repository: CollegeRepository) {
    val eventsListState = repository.allEvents.collectAsState(initial = emptyList())
    val list = eventsListState.value

    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No upcoming events listed.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(list) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column {
                        // Event Banner Image representation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(DeepBlue.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, "edu", tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, "date", tint = Amber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(event.eventDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(Icons.Default.Place, "loc", tint = Amber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(event.venue, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(event.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. EXAMINATIONS VIEW
// ==========================================

@Composable
fun StudentExamsView(repository: CollegeRepository, profile: StudentProfile?) {
    val examListState = repository.allExams.collectAsState(initial = emptyList())
    val list = examListState.value

    val course = profile?.course ?: "B.Pharm"
    val year = profile?.year ?: "4th Year"

    // Filter exams appropriate for this student course/year!
    val relevantExams = list.filter { it.course == course && it.year == year }

    if (relevantExams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, "Empty", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No exam schedules for $course $year", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Amber)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Exam Eligibility Confirmed", color = Color.White, fontWeight = FontWeight.Black)
                        Text("You are cleared to appear for the Sem end exams in Hall A/B.", color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }

            items(relevantExams) { exam ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(exam.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Room No: ${exam.roomNo}", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Text("Time: ${exam.time}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(color = DeepBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text(exam.examDate, color = DeepBlue, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(exam.examDay, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SCHOLARSHIPS VIEW
// ==========================================

@Composable
fun StudentScholarshipsView(repository: CollegeRepository) {
    val scholarshipListState = repository.allScholarships.collectAsState(initial = emptyList())
    val list = scholarshipListState.value

    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active scholarships available.", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(list) { sch ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sch.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue, modifier = Modifier.weight(1f))
                            Surface(color = SuccessGreen, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "₹" + String.format("%,.0f", sch.amount),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(sch.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Eligibility: ${sch.eligibility}", style = MaterialTheme.typography.bodySmall, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text("Apply before: ${sch.lastDate}", style = MaterialTheme.typography.bodySmall, color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. GALLERY GRID VIEW
// ==========================================

@Composable
fun StudentGalleryView(repository: CollegeRepository) {
    val galleryListState = repository.allGalleryItems.collectAsState(initial = emptyList())
    val list = galleryListState.value

    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No gallery pictures uploaded yet.", color = Color.Gray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            items(list) { item ->
                Card(
                    modifier = Modifier.padding(6.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(item.caption, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.eventName, style = MaterialTheme.typography.labelSmall, color = Amber)
                        }
                    }
                }
            }
        }
    }
}
