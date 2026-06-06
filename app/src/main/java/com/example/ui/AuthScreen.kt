package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// Screen navigation enum
enum class AppScreen {
    Splash,
    Login,
    StudentDashboard,
    StaffDashboard,
    AdminDashboard,
    ApprovalPending
}

@Composable
fun AuthMainContent(
    repository: CollegeRepository,
    sessionManager: SessionManager,
    currentScreen: MutableState<AppScreen>,
    currentUserId: MutableState<Long>,
    onInitComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    when (currentScreen.value) {
        AppScreen.Splash -> {
            SplashScreen(
                repository = repository,
                sessionManager = sessionManager,
                currentScreen = currentScreen,
                currentUserId = currentUserId,
                onInitComplete = onInitComplete
            )
        }
        AppScreen.Login -> {
            LoginScreen(
                repository = repository,
                sessionManager = sessionManager,
                currentScreen = currentScreen,
                currentUserId = currentUserId
            )
        }
        AppScreen.ApprovalPending -> {
            ApprovalPendingScreen(
                repository = repository,
                sessionManager = sessionManager,
                currentScreen = currentScreen,
                currentUserId = currentUserId
            )
        }
        else -> {
            // Dashboards are handled from the parent wrapper in MainActivity
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================

@Composable
fun SplashScreen(
    repository: CollegeRepository,
    sessionManager: SessionManager,
    currentScreen: MutableState<AppScreen>,
    currentUserId: MutableState<Long>,
    onInitComplete: () -> Unit
) {
    var animateStart by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        animateStart = true
        // Check database schema seeding
        repository.checkForSeedData()
        onInitComplete()

        delay(1500) // Beautiful delay
        if (sessionManager.isLoggedIn()) {
            val uid = sessionManager.getUserId()
            currentUserId.value = uid
            // Verify status in DB
            val user = repository.getUserById(uid).firstOrNull()
            if (user != null) {
                if (user.status == "active") {
                    when (user.role) {
                        "Student" -> currentScreen.value = AppScreen.StudentDashboard
                        "Staff" -> currentScreen.value = AppScreen.StaffDashboard
                        "Admin" -> currentScreen.value = AppScreen.AdminDashboard
                        else -> currentScreen.value = AppScreen.Login
                    }
                } else if (user.status == "pending") {
                    currentScreen.value = AppScreen.ApprovalPending
                } else {
                    sessionManager.clearSession()
                    currentScreen.value = AppScreen.Login
                }
            } else {
                sessionManager.clearSession()
                currentScreen.value = AppScreen.Login
            }
        } else {
            currentScreen.value = AppScreen.Login
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.DeepBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = animateStart,
                enter = fadeIn() + expandVertically()
            ) {
                Surface(
                    color = com.example.ui.theme.Amber,
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Shield",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ICP COLLEGE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Text(
                text = "INDIRA COLLEGE OF PHARMACY",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = com.example.ui.theme.Amber,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ==========================================
// 2. LOGIN AND REGISTRATION SCREEN
// ==========================================

@Composable
fun LoginScreen(
    repository: CollegeRepository,
    sessionManager: SessionManager,
    currentScreen: MutableState<AppScreen>,
    currentUserId: MutableState<Long>
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var registerRole by remember { mutableStateOf("Student") } // "Student" or "Staff"

    // Form states
    var selectedRoleTab by remember { mutableStateOf("Student") } // "Student", "Staff", "Admin"
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showLoading by remember { mutableStateOf(false) }

    // Registration Form Inputs
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regAadhar by remember { mutableStateOf("") }

    // Student specific
    var regRollNo by remember { mutableStateOf("") }
    var regCourse by remember { mutableStateOf("B.Pharm") } // B.Pharm, D.Pharm, M.Pharm, PharmD
    var regYear by remember { mutableStateOf("1st Year") }
    var regDivision by remember { mutableStateOf("A") }
    var regParentName by remember { mutableStateOf("") }
    var regAddress by remember { mutableStateOf("") }

    // Staff specific
    var regDesignation by remember { mutableStateOf("") }
    var regDepartment by remember { mutableStateOf("") }
    var regQualifications by remember { mutableStateOf("") }
    var regDOJ by remember { mutableStateOf("") }

    // Verification check Helper
    fun validateLoginInputs(): Boolean {
        if (emailInput.trim().isEmpty() || passwordInput.isEmpty()) {
            errorMessage = "Please enter both Email and Password"
            return false
        }
        return true
    }

    fun validateRegInputs(): Boolean {
        if (regName.trim().isEmpty() || regEmail.trim().isEmpty() || regPassword.isEmpty() || regPhone.trim().isEmpty() || regAadhar.trim().isEmpty()) {
            errorMessage = "Please fill all standard fields"
            return false
        }
        if (registerRole == "Student" && (regRollNo.trim().isEmpty() || regParentName.trim().isEmpty())) {
            errorMessage = "Please fill student fields (Roll, Parent name)"
            return false
        }
        if (registerRole == "Staff" && (regDesignation.trim().isEmpty() || regDepartment.trim().isEmpty())) {
            errorMessage = "Please fill staff fields (Designation, Department)"
            return false
        }
        return true
    }

    Scaffold(
        topBar = {
            HeaderBar(title = if (isRegisterMode) "Create Account" else "College Portal Lock")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            if (!isRegisterMode) {
                // LOGIN SCREEN CONTAINER
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Login to Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.DeepBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select your role below first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Roles segment selector
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Student", "Staff", "Admin").forEach { r ->
                                val active = selectedRoleTab == r
                                Button(
                                    onClick = { selectedRoleTab = r },
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) com.example.ui.theme.DeepBlue else Color.LightGray.copy(alpha = 0.4f),
                                        contentColor = if (active) Color.White else Color.DarkGray
                                    ),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(r, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email ID") },
                            placeholder = { Text("e.g. student1@college.com") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, "Email") },
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, "Pass") },
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Toast Feedback Message inside card
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = com.example.ui.theme.ErrorRed,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        if (successMessage != null) {
                            Text(
                                text = successMessage ?: "",
                                color = com.example.ui.theme.SuccessGreen,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                successMessage = null
                                if (validateLoginInputs()) {
                                    showLoading = true
                                    coroutineScope.launch {
                                        val matchedUser = repository.authenticateUser(emailInput, selectedRoleTab)
                                        showLoading = false
                                        if (matchedUser != null && matchedUser.passwordHash == passwordInput) {
                                            if (matchedUser.status == "active") {
                                                // Save Session manager
                                                sessionManager.saveSession(
                                                    userId = matchedUser.id,
                                                    name = matchedUser.name,
                                                    email = matchedUser.email,
                                                    role = matchedUser.role,
                                                    token = "demo_session_token_${matchedUser.id}"
                                                )
                                                currentUserId.value = matchedUser.id

                                                // Route safely
                                                when (matchedUser.role) {
                                                    "Student" -> currentScreen.value = AppScreen.StudentDashboard
                                                    "Staff" -> currentScreen.value = AppScreen.StaffDashboard
                                                    "Admin" -> currentScreen.value = AppScreen.AdminDashboard
                                                }
                                            } else if (matchedUser.status == "pending") {
                                                sessionManager.saveSession(
                                                    userId = matchedUser.id,
                                                    name = matchedUser.name,
                                                    email = matchedUser.email,
                                                    role = matchedUser.role,
                                                    token = ""
                                                )
                                                currentUserId.value = matchedUser.id
                                                currentScreen.value = AppScreen.ApprovalPending
                                            } else {
                                                errorMessage = "Your registration has been Rejected by the College Administrations."
                                            }
                                        } else {
                                            errorMessage = "Invalid login credentials for $selectedRoleTab role!"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DeepBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (showLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Log In Represented Role", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }

                // REGISTER TOGGLE LINK
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Don't have an approved account yet?", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                isRegisterMode = true
                                errorMessage = null
                                successMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.Amber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Request New Account registration", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // HIGHLY CONVENIENT DEMO FILLERS Container
                Text(
                    text = "👇 QUICK PREVIEW DEMO USERS",
                    style = MaterialTheme.typography.titleSmall,
                    color = com.example.ui.theme.DeepBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    listOf(
                        Triple("student1@college.com", "student", "Student"),
                        Triple("staff1@college.com", "staff", "Staff"),
                        Triple("admin@college.com", "admin", "Admin")
                    ).forEach { fill ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    emailInput = fill.first
                                    passwordInput = fill.second
                                    selectedRoleTab = fill.third
                                }
                                .background(Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Auto-Fill ${fill.third} Role",
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.DeepBlue,
                                fontSize = 13.sp
                            )
                            Icon(Icons.Default.FlashOn, "Fast fill", tint = com.example.ui.theme.Amber, modifier = Modifier.size(16.dp))
                        }
                    }
                }

            } else {
                // REGISTRATION MODE CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Register as $registerRole",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.DeepBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Choose register role
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            listOf("Student", "Staff").forEach { r ->
                                val active = registerRole == r
                                Button(
                                    onClick = { registerRole = r },
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) com.example.ui.theme.Amber else Color.LightGray.copy(alpha = 0.4f),
                                        contentColor = if (active) Color.White else Color.DarkGray
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(r, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // STANDARD STANDARD TEXT FIELDS
                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Email ID *") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Password *") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("Phone Number *") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = regAadhar,
                            onValueChange = { regAadhar = it },
                            label = { Text("Aadhar Card Number *") },
                            placeholder = { Text("XXXX-XXXX-1234") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )

                        if (registerRole == "Student") {
                            // Student Profile Fields
                            OutlinedTextField(
                                value = regRollNo,
                                onValueChange = { regRollNo = it },
                                label = { Text("Roll Number *") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )

                            // Course Dropdown Select simulation
                            var showCourseDrop by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                OutlinedTextField(
                                    value = regCourse,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Course *") },
                                    modifier = Modifier.fillMaxWidth().clickable { showCourseDrop = true },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "down") }
                                )
                                DropdownMenu(
                                    expanded = showCourseDrop,
                                    onDismissRequest = { showCourseDrop = false }
                                ) {
                                    listOf("B.Pharm", "D.Pharm", "M.Pharm", "PharmD").forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c) },
                                            onClick = { regCourse = c; showCourseDrop = false }
                                        )
                                    }
                                }
                            }

                            // Year Dropdown Select simulation
                            var showYearDrop by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                OutlinedTextField(
                                    value = regYear,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Year *") },
                                    modifier = Modifier.fillMaxWidth().clickable { showYearDrop = true },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "down") }
                                )
                                DropdownMenu(
                                    expanded = showYearDrop,
                                    onDismissRequest = { showYearDrop = false }
                                ) {
                                    val yearsList = if (regCourse == "PharmD") {
                                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "6th Year")
                                    } else {
                                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
                                    }
                                    yearsList.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y) },
                                            onClick = { regYear = y; showYearDrop = false }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = regDivision,
                                onValueChange = { regDivision = it },
                                label = { Text("Division (e.g. A, B, C)") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            OutlinedTextField(
                                value = regParentName,
                                onValueChange = { regParentName = it },
                                label = { Text("Guardian Parent Name *") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            OutlinedTextField(
                                value = regAddress,
                                onValueChange = { regAddress = it },
                                label = { Text("Address") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        } else {
                            // Staff Profile Fields
                            OutlinedTextField(
                                value = regDesignation,
                                onValueChange = { regDesignation = it },
                                label = { Text("Designation *") },
                                placeholder = { Text("e.g. Associate Assistant Professor") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            OutlinedTextField(
                                value = regDepartment,
                                onValueChange = { regDepartment = it },
                                label = { Text("Department *") },
                                placeholder = { Text("e.g. Pharmaceutics Department") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            OutlinedTextField(
                                value = regQualifications,
                                onValueChange = { regQualifications = it },
                                label = { Text("Qualifications") },
                                placeholder = { Text("M.Pharm, Ph.D.") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            OutlinedTextField(
                                value = regDOJ,
                                onValueChange = { regDOJ = it },
                                label = { Text("Date of Joining (e.g. 01-Jun-2025)") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }

                        // Error panel
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = com.example.ui.theme.ErrorRed,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                if (validateRegInputs()) {
                                    showLoading = true
                                    coroutineScope.launch {
                                        // Insert user
                                        val userId = repository.registerUser(
                                            User(
                                                name = regName.trim(),
                                                email = regEmail.trim(),
                                                passwordHash = regPassword,
                                                role = registerRole,
                                                status = "pending" // Admin must approve!
                                            )
                                        )

                                        if (registerRole == "Student") {
                                            repository.insertStudentProfile(
                                                StudentProfile(
                                                    userId = userId,
                                                    rollNo = regRollNo.trim(),
                                                    course = regCourse,
                                                    year = regYear,
                                                    division = regDivision,
                                                    phone = regPhone.trim(),
                                                    parentName = regParentName.trim(),
                                                    address = regAddress.trim(),
                                                    aadharNo = regAadhar.trim(),
                                                    photoUrl = ""
                                                )
                                            )
                                        } else {
                                            repository.insertStaffProfile(
                                                StaffProfile(
                                                    userId = userId,
                                                    designation = regDesignation.trim(),
                                                    department = regDepartment.trim(),
                                                    qualifications = regQualifications.trim(),
                                                    aadharNo = regAadhar.trim(),
                                                    phone = regPhone.trim(),
                                                    dateOfJoining = if (regDOJ.isEmpty()) "06-Jun-2026" else regDOJ,
                                                    photoUrl = ""
                                                )
                                            )
                                        }

                                        showLoading = false
                                        isRegisterMode = false
                                        successMessage = "Account submitted! Status is Pending Approval."
                                        emailInput = regEmail
                                        passwordInput = regPassword
                                        selectedRoleTab = registerRole

                                        // Clear fields
                                        regName = ""; regEmail = ""; regPassword = ""; regPhone = ""; regAadhar = ""
                                        regRollNo = ""; regParentName = ""; regAddress = ""
                                        regDesignation = ""; regDepartment = ""; regQualifications = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.Amber)
                        ) {
                            if (showLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Submit for Admin Approval", fontWeight = FontWeight.Black)
                            }
                        }

                        TextButton(
                            onClick = {
                                isRegisterMode = false
                                errorMessage = null
                                successMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Already have an account? Log In", color = com.example.ui.theme.DeepBlue)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. APPROVAL PENDING STATUS SCREEN
// ==========================================

@Composable
fun ApprovalPendingScreen(
    repository: CollegeRepository,
    sessionManager: SessionManager,
    currentScreen: MutableState<AppScreen>,
    currentUserId: MutableState<Long>
) {
    val coroutineScope = rememberCoroutineScope()
    val userName = sessionManager.getName()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.SoftGrey),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = com.example.ui.theme.Amber.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Review Pending",
                        tint = com.example.ui.theme.Amber,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Hello, $userName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Registration Status: PENDING",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.WarningOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Thank you for requesting portal access. Your profile application is currently under review by the College Administrative Office. Once an admin approves your profile, your account will become active immediately.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val user = repository.getUserById(sessionManager.getUserId()).firstOrNull()
                        if (user != null && user.status == "active") {
                            // Approved! Update session role and route
                            sessionManager.clearSession()
                            sessionManager.saveSession(
                                userId = user.id,
                                name = user.name,
                                email = user.email,
                                role = user.role,
                                token = "demo_session_token_${user.id}"
                            )
                            when (user.role) {
                                "Student" -> currentScreen.value = AppScreen.StudentDashboard
                                "Staff" -> currentScreen.value = AppScreen.StaffDashboard
                                "Admin" -> currentScreen.value = AppScreen.AdminDashboard
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DeepBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Refresh Application status", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    sessionManager.clearSession()
                    currentScreen.value = AppScreen.Login
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, "out", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Return to Login locks", fontWeight = FontWeight.Bold, color = com.example.ui.theme.DeepBlue)
                }
            }
        }
    }
}
