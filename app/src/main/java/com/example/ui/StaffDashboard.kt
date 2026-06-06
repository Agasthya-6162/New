package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StaffDashboardScreen(
    repository: CollegeRepository,
    sessionManager: SessionManager,
    currentScreen: MutableState<AppScreen>,
    userId: Long
) {
    val coroutineScope = rememberCoroutineScope()

    // Staff current display state
    var selectedTab by remember { mutableStateOf("Dashboard") } // "Dashboard", "Profile", "Mark Attendance", "Schedule", "Leave App"

    // Fetch staff stats
    val staffProfileState = repository.getStaffProfile(userId).collectAsState(initial = null)
    val userState = repository.getUserById(userId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            HeaderBar(
                title = if (selectedTab == "Dashboard") "Staff Faculty central" else selectedTab,
                onBack = if (selectedTab != "Dashboard") { { selectedTab = "Dashboard" } } else null,
                onLogout = {
                    sessionManager.clearSession()
                    currentScreen.value = AppScreen.Login
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(SoftGrey)
        ) {
            when (selectedTab) {
                "Dashboard" -> {
                    StaffHubDashboard(
                        user = userState.value,
                        profile = staffProfileState.value,
                        onTabSelect = { selectedTab = it }
                    )
                }
                "Profile" -> {
                    StaffProfileView(user = userState.value, profile = staffProfileState.value)
                }
                "Mark Attendance" -> {
                    MarkAttendanceView(repository = repository, userId = userId)
                }
                "Schedule" -> {
                    StaffScheduleView(repository = repository, userId = userId)
                }
                "Leave App" -> {
                    StaffLeaveView(repository = repository, userId = userId)
                }
                "Manage Events" -> {
                    StaffEventsDashboardView(repository = repository, userId = userId)
                }
            }
        }
    }
}

// ==========================================
// 1. HUB DASHBOARD HOME
// ==========================================

@Composable
fun StaffHubDashboard(
    user: User?,
    profile: StaffProfile?,
    onTabSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Staff Profile mini-card header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Amber,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp),
                    shadowElevation = 2.dp
                ) {
                    if (profile != null && profile.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Staff Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user?.name ?: "P").take(1).uppercase(),
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user?.name ?: "Prof. Anil Sharma",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = profile?.designation ?: "Professor Faculty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Department: ${profile?.department ?: "ICP"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Text(
            text = "FACULTY SECTIONS MENU",
            style = MaterialTheme.typography.titleSmall,
            color = DeepBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Segment grid blocks
        val menuItems = listOf(
            Triple("Profile", Icons.Default.Person, "My details"),
            Triple("Mark Attendance", Icons.Default.CheckCircle, "Submit daily roll"),
            Triple("Schedule", Icons.Default.Schedule, "Time slot grid"),
            Triple("Leave App", Icons.Default.EventNote, "Casual / medical logs"),
            Triple("Manage Events", Icons.Default.Event, "Academic fests & events")
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp),
            userScrollEnabled = false
        ) {
            items(menuItems) { item ->
                Card(
                    modifier = Modifier
                        .padding(6.dp)
                        .fillMaxSize()
                        .clickable { onTabSelect(item.first) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = Amber.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(item.second, "grid icon", tint = Amber, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(item.first, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Text(item.third, style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. STAFF PROFILE (READ-ONLY)
// ==========================================

@Composable
fun StaffProfileView(user: User?, profile: StaffProfile?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                                    text = (user?.name ?: "P").take(1).uppercase(),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user?.name ?: "Prof. Anil Sharma",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Text(
                    text = profile?.designation ?: "HOD Pharmacology",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                val labels = listOf(
                    "Primary Department" to (profile?.department ?: "ICP Center"),
                    "Educational Certs" to (profile?.qualifications ?: "M.Pharm, Ph.D."),
                    "Joined College On" to (profile?.dateOfJoining ?: "12-Aug-2018"),
                    "Aadhar ID Card" to (if (profile?.aadharNo?.isNotEmpty() == true) {
                        "XXXX-XXXX-${profile.aadharNo.takeLast(4)}"
                    } else "XXXX-XXXX-4321"),
                    "Contact Phone" to (profile?.phone ?: "9876543210"),
                    "Official Email" to (user?.email ?: "staff@college.com")
                )

                labels.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.first,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f).padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. MARK ATTENDANCE FLOW SCREEN
// ==========================================

@Composable
fun MarkAttendanceView(repository: CollegeRepository, userId: Long) {
    val coroutineScope = rememberCoroutineScope()

    // 1. Fetch Reactively all students, subjects, attendance to calculate daily rules
    val studentProfilesState = repository.allStudentProfiles.collectAsState(initial = emptyList())
    val usersState = repository.allUsers.collectAsState(initial = emptyList())
    val staffSubjectsState = repository.getSubjectsForStaff(userId).collectAsState(initial = emptyList())
    val allAttendanceState = repository.allAttendance.collectAsState(initial = emptyList())
    val teachingPlansState = repository.getTeachingPlansForStaff(userId).collectAsState(initial = emptyList())

    val allStudents = studentProfilesState.value
    val allUsers = usersState.value
    val staffSubjects = staffSubjectsState.value
    val allAttendance = allAttendanceState.value
    val approvedSchedules = remember(teachingPlansState.value) { teachingPlansState.value.filter { it.status == "Approved" } }

    // Flow Selections
    var selectedCourse by remember { mutableStateOf("B.Pharm") }
    var selectedYear by remember { mutableStateOf("4th Year") }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }

    // Student Toggle present/absent map [studentUserId -> isPresent]
    val rollStateMap = remember { mutableStateMapOf<Long, Boolean>() }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(false) }

    // Populate default selection for Subject if subjects load
    LaunchedEffect(staffSubjects) {
        if (staffSubjects.isNotEmpty() && selectedSubjectId == null) {
            selectedSubjectId = staffSubjects.first().id
        }
    }

    // Auto load current student profile toggles when category course/year shifts
    val sortedStudents = remember(allStudents, selectedCourse, selectedYear) {
        allStudents.filter { it.course == selectedCourse && it.year == selectedYear }
    }

    LaunchedEffect(sortedStudents) {
        rollStateMap.clear()
        sortedStudents.forEach { sProfile ->
            rollStateMap[sProfile.userId] = true // Defaults to Present!
        }
    }

    // Verify double submissions for today
    val todayString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    val currentSubject = staffSubjects.firstOrNull { it.id == selectedSubjectId }

    val hasAlreadySubmittedToday = remember(allAttendance, selectedSubjectId, todayString, userId) {
        allAttendance.any {
            it.subjectId == selectedSubjectId &&
            it.date == todayString &&
            it.staffId == userId
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (approvedSchedules.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, DeepBlue.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, "verified", tint = DeepBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "APPROVED PLANS (QUICK FILL)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select an approved lecture schedule below to auto pre-fill subject, course, and year classes.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            approvedSchedules.forEach { sPlan ->
                                val subName = staffSubjects.firstOrNull { it.id == sPlan.subjectId }?.name ?: "Subject"
                                val isSelected = selectedSubjectId == sPlan.subjectId && selectedCourse == sPlan.course && selectedYear == sPlan.year
                                
                                Surface(
                                    modifier = Modifier.clickable {
                                        selectedCourse = sPlan.course
                                        selectedYear = sPlan.year
                                        selectedSubjectId = sPlan.subjectId
                                    },
                                    color = if (isSelected) DeepBlue else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) DeepBlue else Color.LightGray),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = subName,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else DeepBlue,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${sPlan.course} - ${sPlan.year}",
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.DarkGray,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "Time: ${sPlan.scheduledDay}, ${sPlan.scheduledTime}",
                                            color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Attendance Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 1 & 2: Course & Year Segment Selectors
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                            val active = selectedCourse == c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .background(
                                        if (active) DeepBlue else Color.LightGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCourse = c }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(c, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.DarkGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        val selectYears = if (selectedCourse == "PharmD") {
                            listOf("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "6th Year")
                        } else {
                            listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
                        }
                        selectYears.forEach { y ->
                            val active = selectedYear == y
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .background(
                                        if (active) DeepBlue else Color.LightGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedYear = y }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(y, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.DarkGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 3: Subject Dropdown select
                    Text(
                        "Assigned Subject Dropdown *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (staffSubjects.isEmpty()) {
                        Text("No subjects assigned. Contact Admin.", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    } else {
                        var showSubjectDrops by remember { mutableStateOf(false) }
                        val activeSubName = staffSubjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "Select Subject"

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showSubjectDrops = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(activeSubName, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Icon(Icons.Default.ArrowDropDown, "drop")
                                }
                            }
                            DropdownMenu(
                                expanded = showSubjectDrops,
                                onDismissRequest = { showSubjectDrops = false }
                            ) {
                                staffSubjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubjectId = sub.id
                                            showSubjectDrops = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Attendance Block Guard: Already Submitted Warning check!
        if (hasAlreadySubmittedToday && currentSubject != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, "warn", tint = WarningOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Warning: Attendance for '${currentSubject.name}' has ALREADY been submitted today. Re-submitting will overwrite today's logs.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "STUDENTS LIST FOR CLASS (${sortedStudents.size} found)",
                style = MaterialTheme.typography.titleSmall,
                color = DeepBlue,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (sortedStudents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No students enrolled in $selectedCourse $selectedYear", color = Color.Gray)
                }
            }
        } else {
            // Student list entries
            items(sortedStudents) { sProfile ->
                val sUser = allUsers.firstOrNull { it.id == sProfile.userId }
                val isPresent = rollStateMap[sProfile.userId] ?: true

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                sUser?.name ?: "Student Name",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Roll: ${sProfile.rollNo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        // PRESENT / ABSENT Slider Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPresent) "PRESENT" else "ABSENT",
                                color = if (isPresent) SuccessGreen else ErrorRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Switch(
                                checked = isPresent,
                                onCheckedChange = { rollStateMap[sProfile.userId] = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = ErrorRed
                                )
                            )
                        }
                    }
                }
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(16.dp))

                if (feedbackMessage != null) {
                    Text(
                        text = feedbackMessage ?: "",
                        color = if (feedbackSuccess) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        feedbackMessage = null
                        if (selectedSubjectId == null) {
                            feedbackMessage = "Assigned Subject not selected."
                            feedbackSuccess = false
                            return@Button
                        }
                        coroutineScope.launch {
                            val attendanceLogs = sortedStudents.map { s ->
                                Attendance(
                                    studentId = s.userId,
                                    subjectId = selectedSubjectId!!,
                                    staffId = userId,
                                    date = todayString,
                                    status = if (rollStateMap[s.userId] == true) "Present" else "Absent",
                                    submittedAt = System.currentTimeMillis(),
                                    approvalStatus = "Pending" // Submitted goes to admin for approval!
                                )
                            }
                            repository.submitAttendanceBatch(attendanceLogs)
                            feedbackSuccess = true
                            feedbackMessage = "Attendance submitted! Awaiting Admin Approval."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasAlreadySubmittedToday) WarningOrange else SuccessGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (hasAlreadySubmittedToday) "Overwrite Today's Roll List" else "Submit To Admin For Approval",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// 4. TIMETABLE SCHEDULE EDITING
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScheduleView(repository: CollegeRepository, userId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val teachingPlansState = repository.getTeachingPlansForStaff(userId).collectAsState(initial = emptyList())
    val staffSubjectsState = repository.getSubjectsForStaff(userId).collectAsState(initial = emptyList())

    val teachingPlans = teachingPlansState.value
    val staffSubjects = staffSubjectsState.value

    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedCourse by remember { mutableStateOf("B.Pharm") }
    var selectedYear by remember { mutableStateOf("1st Year") }
    var selectedDivision by remember { mutableStateOf("A") }
    var selectedDay by remember { mutableStateOf("Monday") }
    var selectedTime by remember { mutableStateOf("09:00 AM - 10:00 AM") }

    var expandedSubject by remember { mutableStateOf(false) }

    LaunchedEffect(staffSubjects) {
        if (staffSubjects.isNotEmpty() && selectedSubjectId == null) {
            selectedSubjectId = staffSubjects.first().id
            selectedCourse = staffSubjects.first().course
            selectedYear = staffSubjects.first().year
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LECTURE TIMETABLE PLANNER",
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Schedule standard weekly teaching lectures. Propose plan details below to send to College Administrator for approval. Locked on approval.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Propose New Timetable Schedule",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text("Select Assigned Subject", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    val subjectName = staffSubjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "Select Assigned Subject"
                    OutlinedButton(
                        onClick = { expandedSubject = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = subjectName, color = Color.Black, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, "down", tint = Color.Gray)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedSubject,
                        onDismissRequest = { expandedSubject = false }
                    ) {
                        staffSubjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    selectedSubjectId = sub.id
                                    selectedCourse = sub.course
                                    selectedYear = sub.year
                                    expandedSubject = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                        Text("Linked Course", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = selectedCourse,
                            onValueChange = { selectedCourse = it },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text("Linked Year", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = selectedYear,
                            onValueChange = { selectedYear = it },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Select Scheduled Day", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday").forEach { day ->
                        val active = selectedDay == day
                        Box(
                            modifier = Modifier
                                .clickable { selectedDay = day }
                                .background(
                                    if (active) DeepBlue else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (active) DeepBlue else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(day, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.DarkGray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Select Scheduled Time Slot", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("09:00 AM - 10:00 AM", "10:00 AM - 11:00 AM", "11:00 AM - 12:00 PM", "01:30 PM - 02:30 PM", "02:30 PM - 03:30 PM", "03:30 PM - 04:30 PM").forEach { time ->
                        val active = selectedTime == time
                        Box(
                            modifier = Modifier
                                .clickable { selectedTime = time }
                                .background(
                                    if (active) DeepBlue else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (active) DeepBlue else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(time, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.DarkGray, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val subId = selectedSubjectId
                        if (subId == null) {
                            Toast.makeText(context, "No assigned subject chosen.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val newPlan = TeachingPlan(
                                staffId = userId,
                                subjectId = subId,
                                course = selectedCourse,
                                year = selectedYear,
                                division = selectedDivision,
                                scheduledDay = selectedDay,
                                scheduledTime = selectedTime,
                                status = "Pending"
                            )
                            repository.insertTeachingPlan(newPlan)
                            Toast.makeText(context, "Lecture plan submitted to Admin for approval!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Send, "send", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send to Admin for Approval", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "PROPOSED COMPLETED LECTURE PLANS",
            style = MaterialTheme.typography.titleSmall,
            color = DeepBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp, top = 8.dp)
        )

        if (teachingPlans.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No proposed timetable schedules created yet.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            teachingPlans.forEach { plan ->
                val subjectName = staffSubjects.firstOrNull { it.id == plan.subjectId }?.name ?: "Subject #${plan.subjectId}"
                val isApproved = plan.status == "Approved"
                val isPending = plan.status == "Pending"
                val isRejected = plan.status == "Rejected"

                val statusColor = when {
                    isApproved -> SuccessGreen
                    isPending -> WarningOrange
                    isRejected -> ErrorRed
                    else -> Color.Gray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subjectName,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBlue,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${plan.course} ${plan.year}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Surface(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isApproved) {
                                        Icon(Icons.Default.Lock, "locked", tint = SuccessGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = plan.status,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, "date", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${plan.scheduledDay} at ${plan.scheduledTime}", fontSize = 11.sp, color = Color.DarkGray)
                            }

                            if (isApproved) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, "lock", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approved / Locked", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteTeachingPlan(plan)
                                                Toast.makeText(context, "Proposed schedule plan deleted.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                                    ) {
                                        Icon(Icons.Default.Delete, "delete", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Delete", fontSize = 11.sp)
                                    }

                                    if (isRejected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    repository.insertTeachingPlan(plan.copy(status = "Pending"))
                                                    Toast.makeText(context, "Resubmitted for Admin Approval!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Resubmit", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CAMPUS PUBLISHED PDF TIMETABLES",
            style = MaterialTheme.typography.titleSmall,
            color = DeepBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val publishedTimetables by repository.allTimetables.collectAsState(initial = emptyList())

        if (publishedTimetables.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No official PDF timetables published by the college yet.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            publishedTimetables.forEach { timetable ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "pdf",
                            tint = ErrorRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = timetable.fileName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                            Text(
                                text = "Course: ${timetable.course} | Sem/Year: ${timetable.year}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Button(
                            onClick = {
                                Toast.makeText(context, "Downloading/Viewing PDF: ${timetable.fileName}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Open", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. LEAVE APPLICATION & LOG
// ==========================================

@Composable
fun StaffLeaveView(repository: CollegeRepository, userId: Long) {
    val coroutineScope = rememberCoroutineScope()
    val pastLeavesState = repository.getLeavesForStaff(userId).collectAsState(initial = emptyList())
    val pastLeaves = pastLeavesState.value

    var leaveType by remember { mutableStateOf("CL") } // CL, ML, EL
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }

    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Request Leave Application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Leave type
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("CL" to "Casual", "ML" to "Medical", "EL" to "Earned").forEach { type ->
                            val active = leaveType == type.first
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        if (active) Amber else Color.LightGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { leaveType = type.first }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${type.second} (${type.first})", fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fromDate,
                        onValueChange = { fromDate = it },
                        label = { Text("From Date (e.g. 10-Jun-2026) *") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = toDate,
                        onValueChange = { toDate = it },
                        label = { Text("To Date (e.g. 12-Jun-2026) *") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason for leave input *") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (feedbackMsg != null) {
                        Text(
                            text = feedbackMsg ?: "",
                            color = if (feedbackSuccess) SuccessGreen else ErrorRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Button(
                        onClick = {
                            feedbackMsg = null
                            if (fromDate.isEmpty() || toDate.isEmpty() || reasonText.isEmpty()) {
                                feedbackMsg = "Please fill in dates and reason fields"
                                feedbackSuccess = false
                                return@Button
                            }
                            coroutineScope.launch {
                                repository.submitLeaveApplication(
                                    LeaveApplication(
                                        staffId = userId,
                                        leaveType = leaveType,
                                        fromDate = fromDate,
                                        toDate = toDate,
                                        reason = reasonText,
                                        status = "Pending",
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                feedbackSuccess = true
                                feedbackMsg = "Application request submitted successfully!"
                                fromDate = ""; toDate = ""; reasonText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit Leave Form", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Past applications
        item {
            Text("PAST APPLICATIONS STATUS LOG", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = DeepBlue, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (pastLeaves.isEmpty()) {
            item {
                Text("No past requests submitted yet.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(pastLeaves) { item ->
                val badgeColor = when (item.status) {
                    "Approved" -> SuccessGreen
                    "Rejected" -> ErrorRed
                    else -> WarningOrange
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Type: ${item.leaveType} Leave", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = DeepBlue)
                            Surface(color = badgeColor, shape = RoundedCornerShape(4.dp)) {
                                Text(item.status, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Duration: ${item.fromDate} to ${item.toDate}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                        Text("Reason: ${item.reason}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        if (item.adminComment?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)).padding(6.dp)) {
                                Text("Admin Comment: ${item.adminComment}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. EVENT MANAGEMENT DASHBOARD (CALENDAR VIEW)
// ==========================================

@Composable
fun StaffEventsDashboardView(repository: CollegeRepository, userId: Long) {
    val coroutineScope = rememberCoroutineScope()
    val allEventsState = repository.allEvents.collectAsState(initial = emptyList())
    val allEvents = allEventsState.value

    val todayCalendar = remember { Calendar.getInstance() }
    
    // Calendar navigation states
    var currentMonth by remember { mutableStateOf(todayCalendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(todayCalendar.get(Calendar.YEAR)) }
    
    // Selected day state
    var selectedDay by remember { 
        mutableStateOf(
            CalendarDay(
                dayNum = todayCalendar.get(Calendar.DAY_OF_MONTH),
                month = todayCalendar.get(Calendar.MONTH),
                year = todayCalendar.get(Calendar.YEAR),
                isCurrentMonth = true
            )
        )
    }

    // Modal forms
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CollegeEvent?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CollegeEvent?>(null) }

    // Dialog state
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var venueInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Overlapping months calculation
    val monthCalendar = remember(currentMonth, currentYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) // 1 (Sunday) to 7 (Saturday)
    val maxDays = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val prevMonthCalendar = remember(currentMonth, currentYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1)
        }
    }
    val maxDaysPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val daysList = remember(currentMonth, currentYear, firstDayOfWeek, maxDays, maxDaysPrevMonth) {
        val list = mutableListOf<CalendarDay>()
        val prefixCount = firstDayOfWeek - 1
        for (i in (maxDaysPrevMonth - prefixCount + 1)..maxDaysPrevMonth) {
            val y = if (currentMonth == 0) currentYear - 1 else currentYear
            val m = if (currentMonth == 0) 11 else currentMonth - 1
            list.add(CalendarDay(dayNum = i, month = m, year = y, isCurrentMonth = false))
        }
        for (i in 1..maxDays) {
            list.add(CalendarDay(dayNum = i, month = currentMonth, year = currentYear, isCurrentMonth = true))
        }
        val totalCells = 42
        val suffixCount = totalCells - list.size
        for (i in 1..suffixCount) {
            val y = if (currentMonth == 11) currentYear + 1 else currentYear
            val m = if (currentMonth == 11) 0 else currentMonth + 1
            list.add(CalendarDay(dayNum = i, month = m, year = y, isCurrentMonth = false))
        }
        list
    }

    val selectedDateDbString = remember(selectedDay) { formatToDatabaseDate(selectedDay) }
    val selectedDateDisplayString = remember(selectedDay) { formatToDisplayDate(selectedDay) }

    // Filter events occurring on the selected date
    val selectedDayEvents = remember(allEvents, selectedDay) {
        allEvents.filter { event ->
            eventMatchesCalendarDay(event, selectedDay)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- 1. CALENDAR CONTROLS & MONTH TITLE ----
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Month & year selector header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear--
                            } else {
                                currentMonth--
                            }
                        }) {
                            Icon(Icons.Default.ChevronLeft, "prev month", tint = DeepBlue)
                        }

                        Text(
                            text = getMonthName(currentMonth) + " $currentYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepBlue
                        )

                        IconButton(onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear++
                            } else {
                                currentMonth++
                            }
                        }) {
                            Icon(Icons.Default.ChevronRight, "next month", tint = DeepBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Days of week header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val dOWs = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        dOWs.forEach { dow ->
                            Text(
                                text = dow,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6-week Days grid rows (6 rows x 7 cols)
                    daysList.chunked(7).forEach { weekRow ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            weekRow.forEach { day ->
                                val isSelected = selectedDay.dayNum == day.dayNum &&
                                                 selectedDay.month == day.month &&
                                                 selectedDay.year == day.year
                                
                                val hasEvents = remember(allEvents, day) {
                                    allEvents.any { event -> eventMatchesCalendarDay(event, day) }
                                }

                                val textAlpha = if (day.isCurrentMonth) 1.0f else 0.40f
                                val textColor = if (isSelected) Color.White else Color.Black.copy(alpha = textAlpha)
                                val cellBgColor = if (isSelected) DeepBlue else Color.Transparent

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.1f)
                                        .padding(1.dp)
                                        .background(cellBgColor, shape = CircleShape)
                                        .clickable {
                                            selectedDay = day
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = textColor
                                        )
                                        if (hasEvents) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(Amber, shape = CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- 2. EVENTS DETAILED DIALOG / ACTIONS ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SCHEDULING ON DATE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = selectedDateDisplayString,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = DeepBlue
                    )
                }

                Button(
                    onClick = {
                        editingEvent = null
                        titleInput = ""
                        descInput = ""
                        venueInput = ""
                        dateInput = selectedDateDbString
                        validationError = null
                        showAddEditDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, "add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Event", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---- 3. SELECTION SPECIFIC EVENTS LIST ----
        if (selectedDayEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Event,
                                "empty events",
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No academic fests or events mapped on this date.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(selectedDayEvents) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(onClick = {
                                    editingEvent = event
                                    titleInput = event.title
                                    descInput = event.description
                                    venueInput = event.venue
                                    dateInput = event.eventDate
                                    validationError = null
                                    showAddEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = Amber, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = {
                                    showDeleteConfirm = event
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, "venue", tint = Amber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Venue: ${event.venue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        // ---- 4. GENERAL MASTER LIST OF ALL EVENTS (PROACTIVE UX OVERVIEW) ----
        item {
            Text(
                text = "ALL UPCOMING EVENTS DIRECTORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (allEvents.isEmpty()) {
            item {
                Text("No database events created yet.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(allEvents) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Amber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Event, "fest", tint = Amber, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = DeepBlue)
                            Text("Scheduled: ${event.eventDate} | Venue: ${event.venue}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        IconButton(onClick = {
                            editingEvent = event
                            titleInput = event.title
                            descInput = event.description
                            venueInput = event.venue
                            dateInput = event.eventDate
                            validationError = null
                            showAddEditDialog = true
                        }) {
                            Icon(Icons.Default.Edit, "edit inline", tint = Amber, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Add Edit Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (editingEvent == null) "Create Academic Event" else "Edit Academic Event",
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Event Title *") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Event Description *") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = venueInput,
                        onValueChange = { venueInput = it },
                        label = { Text("Event Venue *") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Event Date (e.g. 12-Jun-2026) *") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(validationError ?: "", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.trim().isEmpty() || descInput.trim().isEmpty() || venueInput.trim().isEmpty() || dateInput.trim().isEmpty()) {
                            validationError = "Please complete all fields marked with *"
                            return@Button
                        }
                        if (parseEventDate(dateInput) == null) {
                            validationError = "Invalid Date format. Use dd-MMM-yyyy like '12-Jun-2026'"
                            return@Button
                        }
                        coroutineScope.launch {
                            val newEvent = CollegeEvent(
                                id = editingEvent?.id ?: 0,
                                title = titleInput.trim(),
                                description = descInput.trim(),
                                venue = venueInput.trim(),
                                eventDate = dateInput.trim()
                            )
                            repository.insertEvent(newEvent)
                            showAddEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancel", color = Color.DarkGray)
                }
            }
        )
    }

    // Modal Delete Confirmation
    if (showDeleteConfirm != null) {
        val target = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Academic Event", fontWeight = FontWeight.Bold, color = ErrorRed) },
            text = { Text("Are you absolutely sure you want to permanently delete \"${target.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteEvent(target)
                            showDeleteConfirm = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = Color.DarkGray)
                }
            }
        )
    }
}

// Helpers
data class CalendarDay(
    val dayNum: Int,
    val month: Int, // 0-based
    val year: Int,
    val isCurrentMonth: Boolean
)

fun eventMatchesCalendarDay(event: CollegeEvent, day: CalendarDay): Boolean {
    val cal = parseEventDate(event.eventDate) ?: return false
    return cal.get(Calendar.YEAR) == day.year &&
           cal.get(Calendar.MONTH) == day.month &&
           cal.get(Calendar.DAY_OF_MONTH) == day.dayNum
}

fun parseEventDate(dateStr: String): Calendar? {
    val formats = listOf("dd-MMM-yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "d-MMM-yyyy", "dd/MM/yyyy", "yyyy/MM/dd")
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
            sdf.isLenient = false
            val date = sdf.parse(dateStr.trim())
            if (date != null) {
                return Calendar.getInstance().apply { time = date }
            }
        } catch (e: Exception) {}
    }
    return null
}

fun formatToDatabaseDate(day: CalendarDay): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, day.year)
        set(Calendar.MONTH, day.month)
        set(Calendar.DAY_OF_MONTH, day.dayNum)
    }
    val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
    return formatter.format(cal.time)
}

fun formatToDisplayDate(day: CalendarDay): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, day.year)
        set(Calendar.MONTH, day.month)
        set(Calendar.DAY_OF_MONTH, day.dayNum)
    }
    val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH)
    return formatter.format(cal.time)
}

fun getMonthName(monthIndex: Int): String {
    val names = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    return if (monthIndex in 0..11) names[monthIndex] else ""
}
