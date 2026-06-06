package com.example.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    repository: CollegeRepository,
    sessionManager: SessionManager,
    currentScreen: MutableState<AppScreen>,
    userId: Long
) {
    val coroutineScope = rememberCoroutineScope()

    // Screen tab inside Admin
    var selectedTab by remember { mutableStateOf("Hub") } // "Hub", "Students", "Staff", "Registrations", "Attendance Approvals", "Leaves", "Reports", "Circulars"

    Scaffold(
        topBar = {
            HeaderBar(
                title = if (selectedTab == "Hub") "ICP Admin Console" else selectedTab,
                onBack = if (selectedTab != "Hub") { { selectedTab = "Hub" } } else null,
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
                "Hub" -> {
                    AdminDashboardHub(
                        repository = repository,
                        onTabNav = { selectedTab = it }
                    )
                }
                "Students" -> ManageStudentsScreen(repository)
                "Staff" -> ManageStaffScreen(repository)
                "Subjects" -> ManageSubjectsScreen(repository)
                "Registrations" -> ManageRegistrationsScreen(repository)
                "Attendance Approvals" -> AttendanceApprovalsScreen(repository)
                "Leaves" -> LeaveApprovalsScreen(repository)
                "Reports" -> AttendanceReportsScreen(repository)
                "Circulars" -> BroadcastCircularsScreen(repository)
                "Teaching Plan Approvals" -> TeachingPlanApprovalsScreen(repository)
                "Timetables" -> ManageTimetablesScreen(repository)
            }
        }
    }
}

// ==========================================
// 1. ADMIN HUB - STATS CARD GRID
// ==========================================

@Composable
fun AdminDashboardHub(
    repository: CollegeRepository,
    onTabNav: (String) -> Unit
) {
    // 1. Fetch Reactively all stats to calculate badge badges counts!
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allLeaves by repository.allLeaves.collectAsState(initial = emptyList())
    val allAttendance by repository.allAttendance.collectAsState(initial = emptyList())
    val allTeachingPlans by repository.allTeachingPlans.collectAsState(initial = emptyList())

    val pendingRegCount = allUsers.count { it.status == "pending" }
    val pendingLeavesCount = allLeaves.count { it.status == "Pending" }
    val pendingPlansCount = allTeachingPlans.count { it.status == "Pending" }
    val pendingAttendanceCount = allAttendance.filter { it.approvalStatus == "Pending" }
        .groupBy { "${it.subjectId}_${it.date}_${it.staffId}" }.size // Group by custom class session block!

    val totalStudents = allUsers.count { it.role == "Student" && it.status == "active" }
    val totalStaff = allUsers.count { it.role == "Staff" && it.status == "active" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "ICP ADMINISTRATIVE METRICS",
            style = MaterialTheme.typography.titleSmall,
            color = DeepBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Stat Badges
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            StatHeaderCard(
                title = "Pending Registrations",
                count = "$pendingRegCount",
                badgeColor = if (pendingRegCount > 0) WarningOrange else SuccessGreen,
                icon = Icons.Default.HourglassEmpty,
                modifier = Modifier.weight(1f).clickable { onTabNav("Registrations") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatHeaderCard(
                title = "Pending Leaves Log",
                count = "$pendingLeavesCount",
                badgeColor = if (pendingLeavesCount > 0) WarningOrange else SuccessGreen,
                icon = Icons.Default.EventNote,
                modifier = Modifier.weight(1f).clickable { onTabNav("Leaves") }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            StatHeaderCard(
                title = "Attendance Submits",
                count = "$pendingAttendanceCount",
                badgeColor = if (pendingAttendanceCount > 0) WarningOrange else SuccessGreen,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f).clickable { onTabNav("Attendance Approvals") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatHeaderCard(
                title = "Teaching Plans",
                count = "$pendingPlansCount",
                badgeColor = if (pendingPlansCount > 0) WarningOrange else SuccessGreen,
                icon = Icons.Default.Schedule,
                modifier = Modifier.weight(1f).clickable { onTabNav("Teaching Plan Approvals") }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
                    Text("Total Enrolled", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, "student counts", tint = DeepBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Students: $totalStudents", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Staff: $totalStaff", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Text(
            text = "ADMINISTRATIVE SECTIONS MENU",
            style = MaterialTheme.typography.titleSmall,
            color = DeepBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val actions = listOf(
            Triple("Manage Students", Icons.Default.Group, "Students"),
            Triple("Manage Faculty Members", Icons.Default.SupervisedUserCircle, "Staff"),
            Triple("Manage Academic Subjects", Icons.Default.Book, "Subjects"),
            Triple("Attendance Reports & PDFs", Icons.Default.Assessment, "Reports"),
            Triple("Broadcast Circular Notif", Icons.Default.Campaign, "Circulars"),
            Triple("Teaching Plan Approvals", Icons.Default.Assignment, "Teaching Plan Approvals"),
            Triple("Manage Academic Timetables", Icons.Default.Schedule, "Timetables")
        )

        actions.forEach { act ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onTabNav(act.third) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Amber.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(act.second, "ic", tint = Amber, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(act.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(act.third + " administration control tools", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, "go", tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun StatHeaderCard(
    title: String,
    count: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = badgeColor, shape = CircleShape, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, "s_icon", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(count, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
        }
    }
}

// ==========================================
// 2. REGISTRATION APPROVAL CONTROL
// ==========================================

@Composable
fun ManageRegistrationsScreen(repository: CollegeRepository) {
    val coroutineScope = rememberCoroutineScope()
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allStudentProfiles by repository.allStudentProfiles.collectAsState(initial = emptyList())
    val allStaffProfiles by repository.allStaffProfiles.collectAsState(initial = emptyList())

    val pendingUsers = allUsers.filter { it.status == "pending" }

    if (pendingUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, "Null", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No pending registration signup approvals.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(pendingUsers) { u ->
                val detailsText = if (u.role == "Student") {
                    val sp = allStudentProfiles.firstOrNull { it.userId == u.id }
                    "Roll No: ${sp?.rollNo ?: "N/A"}  | Course: ${sp?.course ?: "N/A"} ${sp?.year ?: ""}"
                } else {
                    val sf = allStaffProfiles.firstOrNull { it.userId == u.id }
                    "Dept: ${sf?.department ?: "N/A"}  | Desg: ${sf?.designation ?: "N/A"}"
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(u.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Email: ${u.email}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(detailsText, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                            }
                            Surface(color = Amber, shape = RoundedCornerShape(4.dp)) {
                                Text(u.role, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.updateUser(u.copy(status = "rejected"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Reject", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.updateUser(u.copy(status = "active"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Approve active", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. STUDENT CRUD MANAGEMENT
// ==========================================

@Composable
fun ManageStudentsScreen(repository: CollegeRepository) {
    val coroutineScope = rememberCoroutineScope()
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allProfiles by repository.allStudentProfiles.collectAsState(initial = emptyList())

    val activeStudents = allUsers.filter { it.role == "Student" && it.status == "active" }

    var searchQuery by remember { mutableStateOf("") }
    var filterCourse by remember { mutableStateOf("All") }

    val filteredList = activeStudents.filter { sUser ->
        val sProfile = allProfiles.firstOrNull { it.userId == sUser.id }
        val matchesSearch = sUser.name.contains(searchQuery, ignoreCase = true) || sProfile?.rollNo?.contains(searchQuery, ignoreCase = true) == true
        val matchesCourse = filterCourse == "All" || sProfile?.course == filterCourse
        matchesSearch && matchesCourse
    }

    // Modal Add Form state
    var showFormDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<User?>(null) }
    var editProfile by remember { mutableStateOf<StudentProfile?>(null) }

    // Inputs
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var rollNoInput by remember { mutableStateOf("") }
    var courseInput by remember { mutableStateOf("B.Pharm") }
    var yearInput by remember { mutableStateOf("1st Year") }
    var divInput by remember { mutableStateOf("A") }
    var phoneInput by remember { mutableStateOf("") }
    var parentInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var aadharInput by remember { mutableStateOf("") }

    // CSV Bulk Input State
    var importCsvText by remember { mutableStateOf("") }
    var importStatusMsg by remember { mutableStateOf<String?>(null) }

    val launchAddForm = {
        editUser = null
        editProfile = null
        nameInput = ""; emailInput = ""; rollNoInput = ""; courseInput = "B.Pharm"; yearInput = "1st Year"
        divInput = "A"; phoneInput = ""; parentInput = ""; addressInput = ""; aadharInput = ""
        showFormDialog = true
    }

    val launchEditForm = { user: User, profile: StudentProfile ->
        editUser = user
        editProfile = profile
        nameInput = user.name; emailInput = user.email; rollNoInput = profile.rollNo
        courseInput = profile.course; yearInput = profile.year; divInput = profile.division
        phoneInput = profile.phone; parentInput = profile.parentName; addressInput = profile.address
        aadharInput = profile.aadharNo
        showFormDialog = true
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Student Name or Roll") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                leadingIcon = { Icon(Icons.Default.Search, "search") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = launchAddForm,
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, "add")
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = {
                    importCsvText = ""
                    importStatusMsg = null
                    showImportDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.CloudUpload, "import")
            }
        }

        // Filtering chips (Including PharmD)
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { course ->
                val active = filterCourse == course
                Box(
                    modifier = Modifier
                        .clickable { filterCourse = course }
                        .background(if (active) DeepBlue else Color.White, shape = RoundedCornerShape(6.dp))
                        .border(1.dp, Color.LightGray, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(course, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black, fontSize = 11.sp)
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredList) { studentUser ->
                val profile = allProfiles.firstOrNull { it.userId == studentUser.id } ?: return@items
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(studentUser.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Roll No: ${profile.rollNo} | Course: ${profile.course} | Year: ${profile.year}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Row {
                            IconButton(onClick = { launchEditForm(studentUser, profile) }) {
                                Icon(Icons.Default.Edit, "Edit", tint = DeepBlue)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    repository.updateUser(studentUser.copy(status = "rejected")) // Soft delete
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = { Text(if (editUser == null) "Add Student Profile" else "Modify Student Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = emailInput, onValueChange = { emailInput = it }, label = { Text("Email ID *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = rollNoInput, onValueChange = { rollNoInput = it }, label = { Text("Roll Number *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    
                    // Advanced selective course row
                    Text("Select Course *", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                            val active = courseInput == c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { courseInput = c }
                                    .background(if (active) DeepBlue else Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                    .border(1.dp, if (active) DeepBlue else Color.LightGray, shape = RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(c, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black)
                            }
                        }
                    }

                    // Dynamic selective year chips
                    Text("Select Year *", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    val applicableYears = if (courseInput == "PharmD") {
                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "6th Year")
                    } else if (courseInput == "D.Pharm" || courseInput == "M.Pharm") {
                        listOf("1st Year", "2nd Year")
                    } else {
                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
                    }

                    // Fallback
                    if (!applicableYears.contains(yearInput)) {
                        yearInput = applicableYears.firstOrNull() ?: "1st Year"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        applicableYears.chunked(3).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                chunk.forEach { y ->
                                    val active = yearInput == y
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { yearInput = y }
                                            .background(if (active) Amber else Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                            .border(1.dp, if (active) Amber else Color.LightGray, shape = RoundedCornerShape(6.dp))
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(y, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black)
                                    }
                                }
                                if (chunk.size < 3) {
                                    repeat(3 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = divInput, onValueChange = { divInput = it }, label = { Text("Division Unit") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = parentInput, onValueChange = { parentInput = it }, label = { Text("Parent Name") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = addressInput, onValueChange = { addressInput = it }, label = { Text("Residence Address") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = aadharInput, onValueChange = { aadharInput = it }, label = { Text("Aadhar Card") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isEmpty() || emailInput.isEmpty() || rollNoInput.isEmpty()) return@Button
                        coroutineScope.launch {
                            if (editUser == null) {
                                val newUserId = repository.registerUser(User(name = nameInput, email = emailInput, passwordHash = "student", role = "Student", status = "active"))
                                repository.insertStudentProfile(
                                    StudentProfile(
                                        userId = newUserId, rollNo = rollNoInput, course = courseInput, year = yearInput,
                                        division = divInput, phone = phoneInput, parentName = parentInput, address = addressInput, aadharNo = aadharInput, photoUrl = ""
                                    )
                                )
                            } else {
                                repository.updateUser(editUser!!.copy(name = nameInput, email = emailInput))
                                repository.updateStudentProfile(
                                    editProfile!!.copy(
                                        rollNo = rollNoInput, course = courseInput, year = yearInput, division = divInput,
                                        phone = phoneInput, parentName = parentInput, address = addressInput, aadharNo = aadharInput
                                    )
                                )
                            }
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) { Text("Cancel", color = Color.DarkGray) }
            }
        )
    }

    // ==========================================
    // CSV / MANIFEST IMPORT Dialog
    // ==========================================
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, "Cloud", tint = Amber, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bulk Import Students", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Paste comma-separated rows. Every imported student gets an active portal user account automatically created with the password set to 'student'.",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Surface(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color.LightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("CSV Column Order Structure:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                            Text("Name, Email, RollNo, Course, Year, Division, Phone, Parent, Address, Aadhar", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Example Row Entries:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                            Text("Amit Joshi, amit@college.com, BP-2026-601, PharmD, 1st Year, A, 9081726354, Ravi Joshi, Pune, 1122-3344-5566\nAnisha Sen, anisha@college.com, BP-2026-602, B.Pharm, 4th Year, B, 9876543210, Dev Sen, Mumbai, 9988-7766-5544", fontSize = 10.sp, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Paste CSV records here...") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (importStatusMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(importStatusMsg ?: "", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importCsvText.trim().isEmpty()) {
                            importStatusMsg = "Please enter data to import."
                            return@Button
                        }
                        coroutineScope.launch {
                            val lines = importCsvText.split("\n")
                            var importCount = 0
                            for (line in lines) {
                                val trimmed = line.trim()
                                if (trimmed.isEmpty()) continue
                                val parts = trimmed.split(",").map { it.trim() }
                                if (parts.size >= 3) {
                                    val name = parts[0]
                                    val email = parts[1]
                                    val rollNo = parts[2]
                                    val course = parts.getOrNull(3) ?: "B.Pharm"
                                    val year = parts.getOrNull(4) ?: "1st Year"
                                    val division = parts.getOrNull(5) ?: "A"
                                    val phone = parts.getOrNull(6) ?: ""
                                    val parentName = parts.getOrNull(7) ?: ""
                                    val address = parts.getOrNull(8) ?: ""
                                    val aadhar = parts.getOrNull(9) ?: ""

                                    val newUserId = repository.registerUser(
                                        User(
                                            name = name,
                                            email = email,
                                            passwordHash = "student",
                                            role = "Student",
                                            status = "active"
                                        )
                                    )
                                    repository.insertStudentProfile(
                                        StudentProfile(
                                            userId = newUserId,
                                            rollNo = rollNo,
                                            course = course,
                                            year = year,
                                            division = division,
                                            phone = phone,
                                            parentName = parentName,
                                            address = address,
                                            aadharNo = aadhar,
                                            photoUrl = ""
                                        )
                                    )
                                    importCount++
                                }
                            }
                            importStatusMsg = "Successfully imported $importCount students & created accounts!"
                            importCsvText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Process Import", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Close", color = Color.DarkGray) }
            }
        )
    }
}

// ==========================================
// 4. STAFF && SUBJECT ASSIGN CRUD
// ==========================================

@Composable
fun ManageStaffScreen(repository: CollegeRepository) {
    val coroutineScope = rememberCoroutineScope()
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allStaffProfiles by repository.allStaffProfiles.collectAsState(initial = emptyList())

    val activeStaff = allUsers.filter { it.role == "Staff" && it.status == "active" }

    var showFormDialog by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<User?>(null) }
    var editProfile by remember { mutableStateOf<StaffProfile?>(null) }

    // Subject mapping form
    var showSubjectAssignDialog by remember { mutableStateOf(false) }
    var assigningStaffUser by remember { mutableStateOf<User?>(null) }
    var subjectNameInput by remember { mutableStateOf("") }
    var subjectCourseInput by remember { mutableStateOf("B.Pharm") }
    var subjectYearInput by remember { mutableStateOf("4th Year") }

    // Inputs
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var designationInput by remember { mutableStateOf("") }
    var departmentInput by remember { mutableStateOf("") }
    var qualificationsInput by remember { mutableStateOf("") }
    var aadharInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    val launchAddForm = {
        editUser = null
        editProfile = null
        nameInput = ""; emailInput = ""; designationInput = ""; departmentInput = ""
        qualificationsInput = ""; aadharInput = ""; phoneInput = ""
        showFormDialog = true
    }

    val launchEditForm = { user: User, profile: StaffProfile ->
        editUser = user
        editProfile = profile
        nameInput = user.name; emailInput = user.email; designationInput = profile.designation
        departmentInput = profile.department; qualificationsInput = profile.qualifications
        aadharInput = profile.aadharNo; phoneInput = profile.phone
        showFormDialog = true
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.CenterEnd) {
            Button(
                onClick = launchAddForm,
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, "add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Faculty Profile", color = Color.White)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(activeStaff) { staffUser ->
                val profile = allStaffProfiles.firstOrNull { it.userId == staffUser.id } ?: return@items
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(staffUser.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Dept: ${profile.department} | ${profile.designation}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = { launchEditForm(staffUser, profile) }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = DeepBlue)
                                }
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        repository.updateUser(staffUser.copy(status = "rejected"))
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = {
                                    assigningStaffUser = staffUser
                                    subjectNameInput = ""
                                    showSubjectAssignDialog = true
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Book, "subject assign", tint = Amber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Assign Subject Class", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = { Text(if (editUser == null) "Add Faculty Profile" else "Modify Faculty Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = emailInput, onValueChange = { emailInput = it }, label = { Text("Email ID *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = designationInput, onValueChange = { designationInput = it }, label = { Text("Designation *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = departmentInput, onValueChange = { departmentInput = it }, label = { Text("Department *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = qualificationsInput, onValueChange = { qualificationsInput = it }, label = { Text("Qualifications Certs") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = aadharInput, onValueChange = { aadharInput = it }, label = { Text("Aadhar Card") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isEmpty() || emailInput.isEmpty() || designationInput.isEmpty()) return@Button
                        coroutineScope.launch {
                            if (editUser == null) {
                                val newUserId = repository.registerUser(User(name = nameInput, email = emailInput, passwordHash = "staff", role = "Staff", status = "active"))
                                repository.insertStaffProfile(
                                    StaffProfile(
                                        userId = newUserId, designation = designationInput, department = departmentInput,
                                        qualifications = qualificationsInput, aadharNo = aadharInput, phone = phoneInput, dateOfJoining = "06-Jun-2026", photoUrl = ""
                                    )
                                )
                            } else {
                                repository.updateUser(editUser!!.copy(name = nameInput, email = emailInput))
                                repository.updateStaffProfile(
                                    editProfile!!.copy(
                                        designation = designationInput, department = departmentInput,
                                        qualifications = qualificationsInput, aadharNo = aadharInput, phone = phoneInput
                                    )
                                )
                            }
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) { Text("Cancel", color = Color.DarkGray) }
            }
        )
    }

    if (showSubjectAssignDialog && assigningStaffUser != null) {
        AlertDialog(
            onDismissRequest = { showSubjectAssignDialog = false },
            title = { Text("Assign Lecture Subject", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Assign class mapping for ${assigningStaffUser!!.name}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = subjectNameInput, onValueChange = { subjectNameInput = it }, label = { Text("Subject Name *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    
                    Text("Select Course Target *", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                            val active = subjectCourseInput == c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { subjectCourseInput = c }
                                    .background(if (active) DeepBlue else Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                    .border(1.dp, if (active) DeepBlue else Color.LightGray, shape = RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(c, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black)
                            }
                        }
                    }

                    Text("Select Year Target *", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    val applicableYears = if (subjectCourseInput == "PharmD") {
                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "6th Year")
                    } else if (subjectCourseInput == "D.Pharm" || subjectCourseInput == "M.Pharm") {
                        listOf("1st Year", "2nd Year")
                    } else {
                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
                    }

                    if (!applicableYears.contains(subjectYearInput)) {
                        subjectYearInput = applicableYears.firstOrNull() ?: "1st Year"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        applicableYears.chunked(3).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                chunk.forEach { y ->
                                    val active = subjectYearInput == y
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { subjectYearInput = y }
                                            .background(if (active) Amber else Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                            .border(1.dp, if (active) Amber else Color.LightGray, shape = RoundedCornerShape(6.dp))
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(y, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black)
                                    }
                                }
                                if (chunk.size < 3) {
                                    repeat(3 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectNameInput.isEmpty()) return@Button
                        coroutineScope.launch {
                            repository.insertSubject(
                                Subject(
                                    name = subjectNameInput,
                                    course = subjectCourseInput,
                                    year = subjectYearInput,
                                    staffId = assigningStaffUser!!.id
                                )
                            )
                            showSubjectAssignDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Assign", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubjectAssignDialog = false }) { Text("Cancel", color = Color.DarkGray) }
            }
        )
    }
}

// ==========================================
// 5. ATTENDANCE APPROVALS SCREEN
// ==========================================

@Composable
fun AttendanceApprovalsScreen(repository: CollegeRepository) {
    val coroutineScope = rememberCoroutineScope()

    // Query elements
    val allAttendance by repository.allAttendance.collectAsState(initial = emptyList())
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allStudentProfiles by repository.allStudentProfiles.collectAsState(initial = emptyList())
    val allSubjects by repository.allSubjects.collectAsState(initial = emptyList())

    // Group raw attendances with PENDING status by batch ID: "subjectId_date_staffId"
    val pendingSessions = remember(allAttendance) {
        allAttendance.filter { it.approvalStatus == "Pending" }
            .groupBy { "${it.subjectId}_${it.date}_${it.staffId}" }
    }

    if (pendingSessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, "Null", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No submitted attendance batches pending approval.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(pendingSessions.keys.toList()) { key ->
                val records = pendingSessions[key] ?: return@items
                val firstRec = records.first()

                val subjectName = allSubjects.firstOrNull { it.id == firstRec.subjectId }?.name ?: "Unknown Subject"
                val staffName = allUsers.firstOrNull { it.id == firstRec.staffId }?.name ?: "Faculty member"

                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(subjectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = DeepBlue)
                                Text("Date: ${firstRec.date}  | Prof: $staffName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("Contains ${records.size} student marks inside", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "expand")
                            }
                        }

                        // Expanded view rosters list
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            records.forEach { item ->
                                val sUser = allUsers.firstOrNull { it.id == item.studentId }
                                val sProf = allStudentProfiles.firstOrNull { it.userId == item.studentId }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${sUser?.name ?: "Student"} (${sProf?.rollNo ?: "N/A"})", fontSize = 13.sp)
                                    Text(
                                        text = item.status,
                                        color = if (item.status == "Present") SuccessGreen else ErrorRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        records.forEach { rec ->
                                            repository.deleteAttendanceRecord(rec.id) // reject = delete/discard
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Reject", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        records.forEach { rec ->
                                            repository.updateAttendanceRecord(rec.copy(approvalStatus = "Approved"))
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Approve and Finalize", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. LEAVE APROVALS SCREEN
// ==========================================

@Composable
fun LeaveApprovalsScreen(repository: CollegeRepository) {
    val coroutineScope = rememberCoroutineScope()
    val allLeaves by repository.allLeaves.collectAsState(initial = emptyList())
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())

    val pendingLeaves = allLeaves.filter { it.status == "Pending" }

    var showCommentDialog by remember { mutableStateOf(false) }
    var actingLeave by remember { mutableStateOf<LeaveApplication?>(null) }
    var actionType by remember { mutableStateOf("Approve") } // Approve or Reject
    var commentText by remember { mutableStateOf("") }

    if (pendingLeaves.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HourglassEmpty, "Null", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No pending staff leave applications.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(pendingLeaves) { leave ->
                val profName = allUsers.firstOrNull { it.id == leave.staffId }?.name ?: "Faculty member"

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Applicant: $profName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Leave Category: ${leave.leaveType} Leave", color = Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Surface(color = WarningOrange, shape = RoundedCornerShape(4.dp)) {
                                Text("PENDING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Duration: ${leave.fromDate} to ${leave.toDate}", style = MaterialTheme.typography.bodyMedium)
                        Text("Reason: ${leave.reason}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    actingLeave = leave
                                    actionType = "Reject"
                                    commentText = ""
                                    showCommentDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Reject")
                            }
                            Button(
                                onClick = {
                                    actingLeave = leave
                                    actionType = "Approve"
                                    commentText = ""
                                    showCommentDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Approve")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCommentDialog && actingLeave != null) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("$actionType Leave Application", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Add your administrative remark / instructions comment below *", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Comment") },
                        placeholder = { Text("e.g. Approved. Assigned substitute Prof Sunil.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updatedStatus = if (actionType == "Approve") "Approved" else "Rejected"
                            repository.updateLeaveStatus(
                                actingLeave!!.copy(
                                    status = updatedStatus,
                                    adminComment = commentText
                                )
                            )
                            showCommentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) { Text("Cancel", color = Color.DarkGray) }
            }
        )
    }
}

// ==========================================
// 7. BROADCAST CLOUD CIRCULARS
// ==========================================

@Composable
fun BroadcastCircularsScreen(repository: CollegeRepository) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("Student") } // "Student", "Staff", "All"
    var typeInput by remember { mutableStateOf("General") } // "General", "Event", "Exam", "Scholarship"

    var selectedFileUri by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri.toString()
            selectedFileName = getFileName(context, uri) ?: "Attached Document"
        }
    }

    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compose New Hub Circular", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Circular Topic Title *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = messageInput, onValueChange = { messageInput = it }, label = { Text("Circular Body Description *") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), maxLines = 4)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Target Audience Segment", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        listOf("Student", "Staff", "All").forEach { role ->
                            val active = targetRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .background(if (active) DeepBlue else Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                    .clickable { targetRole = role }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(role, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Notif Topic Tag", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        listOf("General", "Event", "Exam", "Scholarship").forEach { tag ->
                            val active = typeInput == tag
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .background(if (active) Amber else Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                    .clickable { typeInput = tag }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(tag, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Attachment selection block
                    Text("Attachment Document / PDF (Optional)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DeepBlue)
                        ) {
                            Icon(Icons.Default.AttachFile, "attach_file", tint = DeepBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select PDF / Doc from Gallery", color = DeepBlue, fontSize = 11.sp)
                        }
                        if (selectedFileName != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedFileName ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                selectedFileUri = null
                                selectedFileName = null
                            }) {
                                Icon(Icons.Default.Close, "clear", tint = ErrorRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (feedbackMsg != null) {
                        Text(text = feedbackMsg ?: "", color = if (feedbackSuccess) SuccessGreen else ErrorRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6?.dp ?: 6.dp))
                    }

                    Button(
                        onClick = {
                            feedbackMsg = null
                            if (titleInput.isEmpty() || messageInput.isEmpty()) {
                                feedbackMsg = "Fill Circular topic title and description"
                                feedbackSuccess = false
                                return@Button
                            }
                            coroutineScope.launch {
                                repository.insertNotification(
                                    Notification(
                                        title = titleInput,
                                        message = messageInput,
                                        type = typeInput,
                                        targetRole = targetRole,
                                        createdAt = System.currentTimeMillis(),
                                        attachmentUri = selectedFileUri,
                                        attachmentName = selectedFileName
                                    )
                                )
                                feedbackSuccess = true
                                feedbackMsg = "Circular Broadcasted Successfully!"
                                titleInput = ""; messageInput = ""
                                selectedFileUri = null; selectedFileName = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Broadcast Circular", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. REPORTS & NATIVE SYSTEM PDF PRINTER
// ==========================================

@Composable
fun AttendanceReportsScreen(repository: CollegeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Query active data
    val allAttendance by repository.allAttendance.collectAsState(initial = emptyList())
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allStudentProfiles by repository.allStudentProfiles.collectAsState(initial = emptyList())
    val allSubjects by repository.allSubjects.collectAsState(initial = emptyList())
    val allTeachingPlans by repository.allTeachingPlans.collectAsState(initial = emptyList())

    // Tabs for screen structure
    var activeReportTab by remember { mutableStateOf("Ledger") } // "Ledger" or "ProfessorTracker"

    // Unified 6-Month dynamic filtering properties
    var selectedCourseFilter by remember { mutableStateOf("All") } // "All", "B.Pharm", "D.Pharm", "M.Pharm", "PharmD"
    var selectedYearFilter by remember { mutableStateOf("All") }
    var selectedSixMonthsFilter by remember { mutableStateOf("All") } // "All", "June 2026", "May 2026"
    var globalSearchFilter by remember { mutableStateOf("") }

    // --- State variables for Professor Tracker Tab ---
    val professorsList = remember(allUsers) {
        allUsers.filter { it.role == "Staff" && it.status == "active" }
    }
    var selectedProfId by remember { mutableStateOf<Long?>(null) }
    var selectedMonthFilter by remember { mutableStateOf("All") } // "All", "01".."12"
    var selectedDayFilter by remember { mutableStateOf("All") } // "All", "Monday".."Saturday"
    var selectedDateStr by remember { mutableStateOf("") } // Custom date search
    var studentNameSearch by remember { mutableStateOf("") }
    var rosterStatusFilter by remember { mutableStateOf("All") } // "All", "Present", "Absent"

    // Auto-select first professor if none selected
    LaunchedEffect(professorsList) {
        if (selectedProfId == null && professorsList.isNotEmpty()) {
            selectedProfId = professorsList.first().id
        }
    }

    var expandedProfDropdown by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Tab Row Switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "Ledger" to "Student Ledger & PDFs",
                "ProfessorTracker" to "Professor Tracker & Filters"
            )
            tabs.forEach { (tabId, tabName) ->
                val active = activeReportTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) DeepBlue else Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (active) DeepBlue else Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { activeReportTab = tabId }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        color = if (active) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 6-Month dynamic filters panel
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ACADEMIC LEDGER FILTERS (6 MONTHS JOURNAL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Course Selector dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        var expCourse by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expCourse = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedCourseFilter, fontSize = 11.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, "down", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                        DropdownMenu(expanded = expCourse, onDismissRequest = { expCourse = false }) {
                            listOf("All", "B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c, fontSize = 12.sp) },
                                    onClick = {
                                        selectedCourseFilter = c
                                        selectedYearFilter = "All"
                                        expCourse = false
                                    }
                                )
                            }
                        }
                    }

                    // Semester/Year Selector dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        var expYear by remember { mutableStateOf(false) }
                        val yearOptions = if (selectedCourseFilter == "All") {
                            listOf("All")
                        } else {
                            listOf("All") + getYearsOrSemestersForCourse(selectedCourseFilter)
                        }
                        OutlinedButton(
                            onClick = { expYear = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedYearFilter, fontSize = 11.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, "down", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                        DropdownMenu(expanded = expYear, onDismissRequest = { expYear = false }) {
                            yearOptions.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y, fontSize = 12.sp) },
                                    onClick = {
                                        selectedYearFilter = y
                                        expYear = false
                                    }
                                )
                            }
                        }
                    }

                    // Month Range Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        var expMonth by remember { mutableStateOf(false) }
                        val monthOptions = listOf(
                            "All" to "All 6 Months",
                            "June 2026" to "Jun 2026",
                            "May 2026" to "May 2026",
                            "April 2026" to "Apr 2026",
                            "March 2026" to "Mar 2026",
                            "February 2026" to "Feb 2026",
                            "January 2026" to "Jan 2026"
                        )
                        val activeLabel = monthOptions.firstOrNull { it.first == selectedSixMonthsFilter }?.second ?: "6 Months"
                        OutlinedButton(
                            onClick = { expMonth = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(activeLabel, fontSize = 11.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, "down", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                        DropdownMenu(expanded = expMonth, onDismissRequest = { expMonth = false }) {
                            monthOptions.forEach { (valStr, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp) },
                                    onClick = {
                                        selectedSixMonthsFilter = valStr
                                        expMonth = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = globalSearchFilter,
                    onValueChange = { globalSearchFilter = it },
                    placeholder = { Text("Search Students or Subjects...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (globalSearchFilter.isNotEmpty()) {
                            IconButton(onClick = { globalSearchFilter = "" }) {
                                Icon(Icons.Default.Close, "clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )
            }
        }

        if (activeReportTab == "Ledger") {
            // ==================== TAB 1: LEDGER PORTAL ====================
            val approvedRecs = allAttendance.filter { it.approvalStatus == "Approved" }

            val filteredRecs = remember(approvedRecs, selectedCourseFilter, selectedYearFilter, selectedSixMonthsFilter, allStudentProfiles) {
                var list = approvedRecs
                if (selectedCourseFilter != "All") {
                    list = list.filter {
                        val profile = allStudentProfiles.firstOrNull { p -> p.userId == it.studentId }
                        profile?.course == selectedCourseFilter
                    }
                }
                if (selectedYearFilter != "All") {
                    list = list.filter {
                        val profile = allStudentProfiles.firstOrNull { p -> p.userId == it.studentId }
                        profile?.year == selectedYearFilter
                    }
                }
                if (selectedSixMonthsFilter != "All") {
                    val suffix = when (selectedSixMonthsFilter) {
                        "June 2026" -> "06-2026"
                        "May 2026" -> "05-2026"
                        "April 2026" -> "04-2026"
                        "March 2026" -> "03-2026"
                        "February 2026" -> "02-2026"
                        "January 2026" -> "01-2026"
                        else -> ""
                    }
                    if (suffix.isNotEmpty()) {
                        list = list.filter { it.date.endsWith(suffix) }
                    }
                }
                list
            }

            // Generate dynamic calculated roster rows
            val reportRows = remember(filteredRecs, allUsers, allStudentProfiles, selectedCourseFilter, selectedYearFilter, globalSearchFilter) {
                var studentList = allUsers.filter { it.role == "Student" && it.status == "active" }
                if (globalSearchFilter.isNotEmpty()) {
                    val qName = globalSearchFilter.lowercase()
                    studentList = studentList.filter { it.name.lowercase().contains(qName) }
                }
                studentList.mapNotNull { sUser ->
                    val profile = allStudentProfiles.firstOrNull { it.userId == sUser.id }
                    if (selectedCourseFilter != "All" && profile?.course != selectedCourseFilter) {
                        return@mapNotNull null
                    }
                    if (selectedYearFilter != "All" && profile?.year != selectedYearFilter) {
                        return@mapNotNull null
                    }

                    val studentAtt = filteredRecs.filter { it.studentId == sUser.id }
                    val totalClasses = studentAtt.size
                    val presentCount = studentAtt.count { it.status == "Present" }
                    val absentCount = totalClasses - presentCount
                    val percentage = if (totalClasses > 0) {
                        (presentCount.toFloat() / totalClasses.toFloat()) * 100f
                    } else {
                        100f
                    }
                    AttendanceReportRow(
                        rollNo = profile?.rollNo ?: "N/A",
                        name = sUser.name,
                        course = profile?.course ?: "B.Pharm",
                        year = profile?.year ?: "1st Year",
                        total = totalClasses,
                        present = presentCount,
                        absent = absentCount,
                        percentage = percentage.toInt()
                    )
                }
            }

            // Dynamic HTML Builder for system print manager -> clean formatted PDF creation
            val generateHtmlReport = {
                val html = java.lang.StringBuilder()
                html.append("<html><head><style>")
                html.append("body { font-family: sans-serif; margin: 30px; color: #333; }")
                html.append(".header { text-align: center; border-bottom: 2px solid #1565C0; padding-bottom: 12px; }")
                html.append(".title { font-size: 24px; font-weight: bold; color: #1565C0; }")
                html.append(".subtitle { font-size: 14px; color: #666; margin-top: 4px; }")
                html.append("table { width: 100%; border-collapse: collapse; margin-top: 24px; }")
                html.append("th, td { border: 1px solid #ddd; padding: 10px; text-align: left; font-size: 13px; }")
                html.append("th { background-color: #1565C0; color: white; }")
                html.append("tr:nth-child(even) { background-color: #f9f9f9; }")
                html.append(".btn-badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; color: white; }")
                html.append(".green { background-color: #2E7D32; }")
                html.append(".orange { background-color: #F57F17; }")
                html.append(".red { background-color: #C62828; }")
                html.append("</style></head><body>")

                html.append("<div class='header'>")
                html.append("<div class='title'>INDIRA COLLEGE OF PHARMACY, NANDED</div>")
                html.append("<div class='subtitle'>Academic Attendance Ledger - ICP College App</div>")
                html.append("<div class='subtitle'>Generated on: 06-Jun-2026 | Logged by: Administrator</div>")
                html.append("</div>")

                html.append("<table>")
                html.append("<tr><th>Roll No</th><th>Student Name</th><th>Degree Class</th><th>Total Lectures</th><th>Present</th><th>Absent</th><th>Ratio %</th></tr>")

                reportRows.forEach { row ->
                    val colorClass = when {
                        row.percentage > 75 -> "green"
                        row.percentage in 60..75 -> "orange"
                        else -> "red"
                    }
                    html.append("<tr>")
                    html.append("<td>${row.rollNo}</td>")
                    html.append("<td><b>${row.name}</b></td>")
                    html.append("<td>${row.course} - ${row.year}</td>")
                    html.append("<td>${row.total}</td>")
                    html.append("<td>${row.present}</td>")
                    html.append("<td>${row.absent}</td>")
                    html.append("<td><span class='btn-badge $colorClass'>${row.percentage}%</span></td>")
                    html.append("</tr>")
                }

                html.append("</table>")
                html.append("</body></html>")
                html.toString()
            }

            // Android Prints PDF wrapper
            val triggerNativePrint = {
                val webView = WebView(context)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val printAdapter = webView.createPrintDocumentAdapter("ICP_Attendance_Report_2026")
                        val jobName = "ICP Attendance Report Pdf"
                        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                    }
                }
                webView.loadDataWithBaseURL(null, generateHtmlReport(), "text/html", "utf-8", null)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepBlue)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Print / Export Ledger Portal Description", color = Color.White, fontWeight = FontWeight.Black)
                                Text("Generates formatted printable tables utilizing Android native print services.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Button(
                                onClick = {
                                    if (reportRows.isEmpty()) {
                                        Toast.makeText(context, "No rows calculated to print ledger.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        triggerNativePrint()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Amber)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, "pdf")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", color = Color.White)
                            }
                        }
                    }
                }

                item {
                    Text(
                        "CAMPUS ATTENDANCE REGISTER",
                        style = MaterialTheme.typography.titleSmall,
                        color = DeepBlue,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (reportRows.isEmpty()) {
                    item {
                        Text("No attendance logs approved to register metrics.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(reportRows) { row ->
                        val ratioColor = when {
                            row.percentage > 75 -> SuccessGreen
                            row.percentage in 60..75 -> WarningOrange
                            else -> ErrorRed
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(row.name, fontWeight = FontWeight.Bold)
                                    Text("Roll: ${row.rollNo}  | Class: ${row.course} - ${row.year}", fontSize = 12.sp, color = Color.Gray)
                                    Text("Total: ${row.total}  | Present: ${row.present}  | Absent: ${row.absent}", fontSize = 12.sp, color = Color.DarkGray)
                                }
                                Surface(color = ratioColor, shape = RoundedCornerShape(6.dp)) {
                                    Text("${row.percentage}%", color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== TAB 2: PROFESSOR TRACKER & FILTERS ====================
            // Calculate matches list of Session blocks
            val sessionBlocks = remember(
                allAttendance, selectedProfId, selectedMonthFilter, selectedDayFilter,
                selectedDateStr, studentNameSearch, rosterStatusFilter
            ) {
                // Group raw records by subjectId_date_staffId
                val grouped = allAttendance.groupBy { "${it.subjectId}_${it.date}_${it.staffId}" }
                
                val filteredBlocks = mutableListOf<Triple<String, List<Attendance>, String>>() // Key, matching records, Day of Week name
                val dateSdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.ENGLISH)
                val dayNameSdf = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH)

                for ((key, records) in grouped) {
                    val firstRec = records.firstOrNull() ?: continue
                    
                    // 1. Professor Filter
                    if (selectedProfId != null && firstRec.staffId != selectedProfId) {
                        continue
                    }
                    
                    // 2. Month Filter matching
                    val dateParts = firstRec.date.split("-")
                    if (selectedMonthFilter != "All" && dateParts.size == 3) {
                        if (dateParts[1] != selectedMonthFilter) {
                            continue
                        }
                    }
                    
                    // Calculate Day Of Week dynamically
                    var dayOfWeekStr = "Unknown"
                    try {
                        val parsed = dateSdf.parse(firstRec.date)
                        if (parsed != null) {
                            dayOfWeekStr = dayNameSdf.format(parsed)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                    
                    // 3. Day of Week filter
                    if (selectedDayFilter != "All") {
                        if (!dayOfWeekStr.equals(selectedDayFilter, ignoreCase = true)) {
                            continue
                        }
                    }
                    
                    // 4. Specific manual Custom Date Search string
                    if (selectedDateStr.isNotEmpty() && !firstRec.date.contains(selectedDateStr)) {
                        continue
                    }
                    
                    // Filter the student roster inside the batch session on search matches & status toggles
                    val matchedRoster = records.filter { item ->
                        val sUser = allUsers.firstOrNull { it.id == item.studentId }
                        val sProfile = allStudentProfiles.firstOrNull { it.userId == item.studentId }
                        
                        val qName = studentNameSearch.lowercase()
                        val textFilterMatches = if (studentNameSearch.isEmpty()) {
                            true
                        } else {
                            (sUser?.name?.lowercase()?.contains(qName) == true) ||
                            (sProfile?.rollNo?.lowercase()?.contains(qName) == true)
                        }
                        
                        val statusFilterMatches = if (rosterStatusFilter == "All") {
                            true
                        } else {
                            item.status.equals(rosterStatusFilter, ignoreCase = true)
                        }
                        
                        textFilterMatches && statusFilterMatches
                    }
                    
                    // If anything matches (or search criteria are empty so keep all)
                    if (matchedRoster.isNotEmpty() || (studentNameSearch.isEmpty() && rosterStatusFilter == "All")) {
                        filteredBlocks.add(Triple(key, matchedRoster, dayOfWeekStr))
                    }
                }
                filteredBlocks
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Settings & Selection Filter Panel Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "STAFF TIMETABLE LIVE SCHEDULER INSPECTOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Professor select dropdown
                            Text("Select Target Professor", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                val currentProfName = professorsList.firstOrNull { it.id == selectedProfId }?.name ?: "Select Professor"
                                OutlinedButton(
                                    onClick = { expandedProfDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentProfName, fontSize = 13.sp)
                                        Icon(Icons.Default.ArrowDropDown, "down", tint = Color.Gray)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedProfDropdown,
                                    onDismissRequest = { expandedProfDropdown = false }
                                ) {
                                    professorsList.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.name) },
                                            onClick = {
                                                selectedProfId = p.id
                                                expandedProfDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal scroll selector for Months
                            Text("Filter by Month", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val mMonths = listOf(
                                    "All" to "All Months",
                                    "01" to "Jan", "02" to "Feb", "03" to "Mar",
                                    "04" to "Apr", "05" to "May", "06" to "Jun",
                                    "07" to "Jul", "08" to "Aug", "09" to "Sep",
                                    "10" to "Oct", "11" to "Nov", "12" to "Dec"
                                )
                                mMonths.forEach { (code, name) ->
                                    val isSel = selectedMonthFilter == code
                                    Box(
                                        modifier = Modifier
                                            .clickable { selectedMonthFilter = code }
                                            .background(
                                                if (isSel) DeepBlue else Color.LightGray.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Color.DarkGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Specific Day of the week selector
                            Text("Filter by Scheduled Weekly Day", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val mDays = listOf("All", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                                mDays.forEach { dayName ->
                                    val isSel = selectedDayFilter == dayName
                                    Box(
                                        modifier = Modifier
                                            .clickable { selectedDayFilter = dayName }
                                            .background(
                                                if (isSel) DeepBlue else Color.LightGray.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = dayName,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Color.DarkGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Specific Date custom Search & student search inputs
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1.2f).padding(end = 4.dp)) {
                                    Text("Specific Date (e.g. 15-05-2026)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = selectedDateStr,
                                        onValueChange = { selectedDateStr = it },
                                        placeholder = { Text("Search date...", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        leadingIcon = { Icon(Icons.Default.CalendarToday, "date", modifier = Modifier.size(16.dp), tint = Color.Gray) },
                                        trailingIcon = {
                                            if (selectedDateStr.isNotEmpty()) {
                                                Icon(
                                                    Icons.Default.Clear, "clear",
                                                    modifier = Modifier.size(16.dp).clickable { selectedDateStr = "" },
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                    )
                                }

                                Column(modifier = Modifier.weight(1.8f).padding(start = 4.dp)) {
                                    Text("Student Search (Name/Roll No)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = studentNameSearch,
                                        onValueChange = { studentNameSearch = it },
                                        placeholder = { Text("Enter student query...", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        leadingIcon = { Icon(Icons.Default.Search, "search student", modifier = Modifier.size(16.dp), tint = Color.Gray) },
                                        trailingIcon = {
                                            if (studentNameSearch.isNotEmpty()) {
                                                Icon(
                                                    Icons.Default.Clear, "clear",
                                                    modifier = Modifier.size(16.dp).clickable { studentNameSearch = "" },
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Student Status filter
                            Text("Roster Student Presence Filter", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("All", "Present", "Absent").forEach { opt ->
                                    val active = rosterStatusFilter == opt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (active) DeepBlue else Color.LightGray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { rosterStatusFilter = opt }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (opt == "All") "Both Status" else "$opt Only",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick helpful suggestion chips for logged dates
                item {
                    val sampleLoggedDates = remember(allAttendance, selectedProfId) {
                        allAttendance
                            .filter { selectedProfId == null || it.staffId == selectedProfId }
                            .map { it.date }
                            .distinct()
                            .take(5)
                    }
                    if (sampleLoggedDates.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Select Dates: ", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sampleLoggedDates.forEach { dt ->
                                    Surface(
                                        modifier = Modifier.clickable { selectedDateStr = dt },
                                        color = if (selectedDateStr == dt) DeepBlue.copy(alpha = 0.1f) else Color.White,
                                        border = BorderStroke(1.dp, if (selectedDateStr == dt) DeepBlue else Color.LightGray),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = dt,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = if (selectedDateStr == dt) DeepBlue else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (sessionBlocks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.HourglassEmpty, "None", tint = Color.LightGray, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No logs match the selected filter query.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(sessionBlocks) { (key, records, dayOfWeek) ->
                        val firstRec = records.first()
                        val subjectName = allSubjects.firstOrNull { it.id == firstRec.subjectId }?.name ?: "Subject #${firstRec.subjectId}"
                        val totalSessionSize = records.size
                        val presentCount = records.count { it.status == "Present" }
                        val absentCount = totalSessionSize - presentCount
                        val attendancePct = if (totalSessionSize > 0) {
                            (presentCount.toFloat() / totalSessionSize.toFloat() * 100).toInt()
                        } else 100

                        // Lookup the professor's TeachingPlan to verify "Scheduled Timetable Slot"
                        val matchingPlan = remember(allTeachingPlans, firstRec) {
                            allTeachingPlans.firstOrNull { plan ->
                                plan.staffId == firstRec.staffId &&
                                plan.subjectId == firstRec.subjectId &&
                                plan.status == "Approved" &&
                                plan.scheduledDay.equals(dayOfWeek, ignoreCase = true)
                            }
                        }

                        var showRosterList by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subjectName.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeepBlue
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Date: ${firstRec.date} ($dayOfWeek)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.DarkGray
                                        )
                                    }

                                    Surface(
                                        color = if (firstRec.approvalStatus == "Approved") SuccessGreen.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (firstRec.approvalStatus == "Approved") "Approved" else "Pending Approval",
                                            color = if (firstRec.approvalStatus == "Approved") SuccessGreen else WarningOrange,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.4f))

                                // Match timetable verification highlight
                                if (matchingPlan != null) {
                                    Surface(
                                        color = DeepBlue.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, DeepBlue.copy(alpha = 0.12f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Verified, "plan match", tint = DeepBlue, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Matched Timetable Slot: Scheduled on ${matchingPlan.scheduledDay}, ${matchingPlan.scheduledTime}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DeepBlue
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = WarningOrange.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.12f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Campaign, "plan extra", tint = WarningOrange, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Extra / Out of Schedule Timetable Register Class",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = WarningOrange
                                            )
                                        }
                                    }
                                }

                                // Class metrics stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("SESSION ATTENDANCE SUMMARY", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Ratio: $attendancePct% ($presentCount Present, $absentCount Absent)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { showRosterList = !showRosterList },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(if (showRosterList) "Hide Roster" else "View Roster", fontSize = 10.sp)
                                        Icon(
                                            imageVector = if (showRosterList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "exp",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Roster expanded slide down lists
                                if (showRosterList) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "STUDENT ROSTER LISTING (${records.size} matched)",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SoftGrey, shape = RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        records.forEach { att ->
                                            val sUser = allUsers.firstOrNull { it.id == att.studentId }
                                            val sProfile = allStudentProfiles.firstOrNull { it.userId == att.studentId }
                                            val rollNo = sProfile?.rollNo ?: "Roll Unknown"
                                            val courseClass = "${sProfile?.course ?: "Class"} ${sProfile?.year ?: "Yr"}"
                                            val isPres = att.status == "Present"

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = sUser?.name ?: "Student #${att.studentId}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color.Black
                                                    )
                                                    Text(
                                                        text = "Roll: $rollNo | $courseClass",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }

                                                Surface(
                                                    color = (if (isPres) SuccessGreen else ErrorRed).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = att.status,
                                                        color = if (isPres) SuccessGreen else ErrorRed,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            }
        }
    }
}

data class AttendanceReportRow(
    val rollNo: String,
    val name: String,
    val course: String,
    val year: String,
    val total: Int,
    val present: Int,
    val absent: Int,
    val percentage: Int
)

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1 && cut != null) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun TeachingPlanApprovalsScreen(repository: CollegeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val allTeachingPlans by repository.allTeachingPlans.collectAsState(initial = emptyList())
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allSubjects by repository.allSubjects.collectAsState(initial = emptyList())
    
    var activeTab by remember { mutableStateOf("Pending") } // "Pending", "History", "Simplified"
    
    val pendingPlans = allTeachingPlans.filter { it.status == "Pending" }
    val historyPlans = allTeachingPlans.filter { it.status == "Approved" || it.status == "Rejected" }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "LECTURE TEACHING PLANS",
            style = MaterialTheme.typography.titleMedium,
            color = DeepBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Sub-tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                "Pending" to "Pending",
                "History" to "History",
                "Simplified" to "Staff Calendars"
            )
            tabs.forEach { (tabId, tabTitle) ->
                val active = activeTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) DeepBlue else Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (active) DeepBlue else Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabTitle,
                        color = if (active) Color.White else Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        when (activeTab) {
            "Pending" -> {
            if (pendingPlans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, "None", tint = Color.LightGray, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No pending teaching schedule plans.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingPlans) { plan ->
                        val staffName = allUsers.firstOrNull { it.id == plan.staffId }?.name ?: "Prof. Unknown"
                        val subjectName = allSubjects.firstOrNull { it.id == plan.subjectId }?.name ?: "Subject #${plan.subjectId}"
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subjectName.uppercase(),
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeepBlue
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "By $staffName",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                    
                                    Surface(
                                        color = WarningOrange.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            text = plan.status,
                                            color = WarningOrange,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("COURSE / CLASS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("${plan.course} - ${plan.year}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("SCHEDULED TIMING", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("${plan.scheduledDay}, ${plan.scheduledTime}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.updateTeachingPlan(plan.copy(status = "Rejected"))
                                                Toast.makeText(context, "Teaching plan request rejected.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Reject", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.updateTeachingPlan(plan.copy(status = "Approved"))
                                                Toast.makeText(context, "Teaching plan request approved! Locked in database.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Approve", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            "History" -> {
                if (historyPlans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No past approvals/rejections found.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyPlans) { plan ->
                            val staffName = allUsers.firstOrNull { it.id == plan.staffId }?.name ?: "Prof. Unknown"
                            val subjectName = allSubjects.firstOrNull { it.id == plan.subjectId }?.name ?: "Subject #${plan.subjectId}"
                            val isApproved = plan.status == "Approved"
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = subjectName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = DeepBlue
                                            )
                                            Text(text = "Proposed by $staffName", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        
                                        Surface(
                                            color = (if (isApproved) SuccessGreen else ErrorRed).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isApproved) Icons.Default.Lock else Icons.Default.Cancel,
                                                    contentDescription = plan.status,
                                                    tint = if (isApproved) SuccessGreen else ErrorRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = plan.status,
                                                    color = if (isApproved) SuccessGreen else ErrorRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.3f))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${plan.course} ${plan.year}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                        
                                        Text(
                                            text = "${plan.scheduledDay}s at ${plan.scheduledTime}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DeepBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "Simplified" -> {
                val staffUsers = allUsers.filter { it.role == "Staff" && it.status == "active" }
                var selectedSimplifiedStaffId by remember { mutableStateOf<Long?>(null) }
                
                LaunchedEffect(staffUsers) {
                    if (selectedSimplifiedStaffId == null && staffUsers.isNotEmpty()) {
                        selectedSimplifiedStaffId = staffUsers.first().id
                    }
                }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Simplified Professor Schedule View",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    var expandedStaffSelect by remember { mutableStateOf(false) }
                    val currentStaff = staffUsers.firstOrNull { it.id == selectedSimplifiedStaffId }
                    
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        OutlinedButton(
                            onClick = { expandedStaffSelect = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = currentStaff?.name ?: "Select Professor...", color = Color.Black, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, "down", tint = Color.Gray)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedStaffSelect,
                            onDismissRequest = { expandedStaffSelect = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            staffUsers.forEach { staff ->
                                DropdownMenuItem(
                                    text = { Text(staff.name) },
                                    onClick = {
                                        selectedSimplifiedStaffId = staff.id
                                        expandedStaffSelect = false
                                    }
                                )
                            }
                        }
                    }
                    
                    val currentStaffPlans = allTeachingPlans.filter { it.staffId == selectedSimplifiedStaffId && it.status == "Approved" }
                    
                    if (currentStaffPlans.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No approved teaching plans found for this professor.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(daysOfWeek) { day ->
                                val dayPlans = currentStaffPlans.filter { it.scheduledDay.lowercase().trim() == day.lowercase() }
                                if (dayPlans.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = day, fontWeight = FontWeight.Bold, color = DeepBlue, fontSize = 14.sp)
                                                Surface(
                                                    color = SuccessGreen.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${dayPlans.size} Scheduled",
                                                        fontSize = 10.sp,
                                                        color = SuccessGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            
                                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
                                            
                                            dayPlans.forEachIndexed { idx, plan ->
                                                val subject = allSubjects.firstOrNull { it.id == plan.subjectId }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = subject?.name ?: "Subject Class", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
                                                        Text(text = "${plan.course} (${plan.year}) | Division ${plan.division}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                    Text(text = plan.scheduledTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                                }
                                                if (idx < dayPlans.size - 1) {
                                                    Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color.LightGray.copy(alpha = 0.15f))
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
        }
    }
}

@Composable
fun ManageTimetablesScreen(repository: CollegeRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allTimetables by repository.allTimetables.collectAsState(initial = emptyList())

    var courseInput by remember { mutableStateOf("B.Pharm") }
    var yearInput by remember { mutableStateOf("Semester 1") }

    var selectedFileUri by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri.toString()
            selectedFileName = getFileName(context, uri) ?: "Attached Timetable.pdf"
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Upload Academic Timetable PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Course *", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                            val active = courseInput == c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (active) DeepBlue else Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                    .clickable {
                                        courseInput = c
                                        val available = getYearsOrSemestersForCourse(c)
                                        yearInput = available.firstOrNull() ?: "1st Year"
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(c, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Select Semester / Year *", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    val applicableYears = getYearsOrSemestersForCourse(courseInput)
                    if (!applicableYears.contains(yearInput)) {
                        yearInput = applicableYears.firstOrNull() ?: "1st Year"
                    }

                    var expandedYearDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedButton(
                            onClick = { expandedYearDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = yearInput, color = Color.Black, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, "down", tint = Color.Gray)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedYearDropdown,
                            onDismissRequest = { expandedYearDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            applicableYears.forEach { semYear ->
                                DropdownMenuItem(
                                    text = { Text(semYear) },
                                    onClick = {
                                        yearInput = semYear
                                        expandedYearDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Timetable File Source *", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { filePickerLauncher.launch("application/pdf") },
                        colors = CardDefaults.cardColors(containerColor = SoftGrey),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = "pdf icon",
                                tint = ErrorRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedFileName ?: "Select Timetable PDF Document",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedFileName != null) Color.Black else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selectedFileName != null) "Ready to upload" else "Click to use device file manager",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val uri = selectedFileUri
                            val name = selectedFileName
                            if (uri == null || name == null) {
                                Toast.makeText(context, "Please click above to select a PDF timetable file first.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                val item = Timetable(
                                    course = courseInput,
                                    year = yearInput,
                                    fileName = name,
                                    fileUri = uri
                                )
                                repository.insertTimetable(item)
                                selectedFileUri = null
                                selectedFileName = null
                                Toast.makeText(context, "Time table successfully published for $courseInput - $yearInput!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Upload, "upload", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish New Timetable", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "CURRENT COMPRESSED PDF TIMETABLES",
                style = MaterialTheme.typography.titleSmall,
                color = DeepBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp, top = 8.dp)
            )
        }

        if (allTimetables.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No academic timetables are currently uploaded on the server.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(allTimetables) { t ->
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
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t.fileName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Surface(
                                    color = DeepBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = t.course,
                                        fontSize = 9.sp,
                                        color = DeepBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Amber.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = t.year,
                                        fontSize = 9.sp,
                                        color = Color(0xFFC26402),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    repository.deleteTimetable(t)
                                    Toast.makeText(context, "Timetable PDF entry removed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageSubjectsScreen(repository: CollegeRepository) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Query subjects and faculty/staff list
    val allSubjects by repository.allSubjects.collectAsState(initial = emptyList())
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val allStaffProfiles by repository.allStaffProfiles.collectAsState(initial = emptyList())

    val activeStaff = allUsers.filter { it.role == "Staff" && it.status == "active" }

    // Screen filters
    var filterCourse by remember { mutableStateOf("All") }
    var filterYear by remember { mutableStateOf("All") }

    val filteredSubjects = allSubjects.filter { sub ->
        val matchesCourse = filterCourse == "All" || sub.course == filterCourse
        val matchesYear = filterYear == "All" || sub.year == filterYear
        matchesCourse && matchesYear
    }

    // Dialog state for adding subject
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectNameInput by remember { mutableStateOf("") }
    var selectedCourseInput by remember { mutableStateOf("B.Pharm") }
    var selectedYearInput by remember { mutableStateOf("1st Year") }
    var assignedStaffId by remember { mutableStateOf<Long?>(null) }
    var staffDropdownExpanded by remember { mutableStateOf(false) }

    // Side effect to default assigned staff if active staff load
    LaunchedEffect(activeStaff) {
        if (activeStaff.isNotEmpty() && assignedStaffId == null) {
            assignedStaffId = activeStaff.first().id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Header info-card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Book, "subjects", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACADEMIC SUBJECTS REGISTRY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Amber
                    )
                    Text(
                        text = "Create and map curriculum subjects, choose courses, years, and declare the faculty assigned.",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action controls (Add & Filters)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CURRICULUM LIST (${filteredSubjects.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBlue
            )

            Button(
                onClick = {
                    subjectNameInput = ""
                    selectedCourseInput = "B.Pharm"
                    selectedYearInput = "1st Year"
                    assignedStaffId = activeStaff.firstOrNull()?.id
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, "add", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Subject", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Simple modern horizontal filters
        Text(
            text = "FILTER BY COURSE:",
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                val active = filterCourse == c
                Surface(
                    modifier = Modifier.clickable { filterCourse = c },
                    color = if (active) DeepBlue else Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (active) DeepBlue else Color.LightGray.copy(alpha = 0.8f))
                ) {
                    Text(
                        text = c,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Listing area
        if (filteredSubjects.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        "empty",
                        tint = Color.LightGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Subjects Registered Yet",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Tap the 'Add Subject' button to register curriculum classes.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSubjects) { subject ->
                    val professor = activeStaff.firstOrNull { it.id == subject.staffId }
                    val profile = allStaffProfiles.firstOrNull { it.userId == subject.staffId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subject.name,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBlue,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Amber.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${subject.course} • ${subject.year}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC26402),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        "user",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Faculty: ${professor?.name ?: "Prof. Unassigned"}${if (profile != null) " (Dept: ${profile.department})" else ""}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.deleteSubject(subject)
                                        Toast.makeText(context, "Subject deleted successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, "delete", tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal form dialog to register subjects
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Register New Subject",
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Specify academic details below to publish subject in the database.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = subjectNameInput,
                        onValueChange = { subjectNameInput = it },
                        label = { Text("Subject Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Course Target *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                            val active = selectedCourseInput == c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCourseInput = c }
                                    .background(
                                        if (active) DeepBlue else Color.LightGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (active) DeepBlue else Color.LightGray,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = c,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Select Target Year *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    val applicableYears = if (selectedCourseInput == "PharmD") {
                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "6th Year")
                    } else if (selectedCourseInput == "D.Pharm" || selectedCourseInput == "M.Pharm") {
                        listOf("1st Year", "2nd Year")
                    } else {
                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
                    }

                    if (!applicableYears.contains(selectedYearInput)) {
                        selectedYearInput = applicableYears.firstOrNull() ?: "1st Year"
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        applicableYears.chunked(3).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                chunk.forEach { y ->
                                    val active = selectedYearInput == y
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedYearInput = y }
                                            .background(
                                                if (active) Amber else Color.LightGray.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (active) Amber else Color.LightGray,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = y,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else Color.Black
                                        )
                                    }
                                }
                                if (chunk.size < 3) {
                                    repeat(3 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Assign Faculty *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (activeStaff.isEmpty()) {
                        Text(
                            text = "No active faculty registered in system to assign! Register staff first.",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val activeProfessorName = activeStaff.firstOrNull { it.id == assignedStaffId }?.name ?: "Tap to choose faculty member"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { staffDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = activeProfessorName, color = Color.Black, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, "expand", tint = Color.Gray)
                                }
                            }

                            DropdownMenu(
                                expanded = staffDropdownExpanded,
                                onDismissRequest = { staffDropdownExpanded = false }
                            ) {
                                activeStaff.forEach { staff ->
                                    val staffProf = allStaffProfiles.firstOrNull { it.userId == staff.id }
                                    DropdownMenuItem(
                                        text = { Text("${staff.name}${if (staffProf != null) " (${staffProf.department})" else ""}") },
                                        onClick = {
                                            assignedStaffId = staff.id
                                            staffDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val staffId = assignedStaffId
                        if (subjectNameInput.isEmpty() || staffId == null) {
                            Toast.makeText(context, "Specify subject name and assign a teacher", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            repository.insertSubject(
                                Subject(
                                    name = subjectNameInput,
                                    course = selectedCourseInput,
                                    year = selectedYearInput,
                                    staffId = staffId
                                )
                            )
                            Toast.makeText(context, "Subject added!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue)
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.DarkGray)
                }
            }
        )
    }
}


