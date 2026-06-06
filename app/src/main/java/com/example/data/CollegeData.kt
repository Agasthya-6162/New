package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==========================================
// 1. DATABASE ENTITIES
// ==========================================

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String, // Plain-text or hashed password check
    val role: String, // "Student", "Staff", "Admin"
    val status: String // "active", "pending", "rejected"
)

@Entity(tableName = "student_profiles")
data class StudentProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val rollNo: String,
    val course: String, // "B.Pharm", "D.Pharm", "M.Pharm", "PharmD"
    val year: String, // "1st Year", "2nd Year", "3rd Year", "4th Year"
    val division: String, // "A", "B", "C"
    val phone: String,
    val parentName: String,
    val address: String,
    val aadharNo: String,
    val photoUrl: String
)

@Entity(tableName = "staff_profiles")
data class StaffProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val designation: String,
    val department: String,
    val qualifications: String,
    val aadharNo: String,
    val phone: String,
    val dateOfJoining: String,
    val photoUrl: String
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val course: String,
    val year: String,
    val staffId: Long // Points to the user ID of the staff member
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long, // Points to student user ID
    val subjectId: Long,
    val staffId: Long, // Points to staff user ID
    val date: String, // "dd-MM-yyyy"
    val status: String, // "Present", "Absent"
    val submittedAt: Long = System.currentTimeMillis(),
    val approvalStatus: String = "Pending" // "Pending", "Approved", "Rejected"
)

@Entity(tableName = "leave_applications")
data class LeaveApplication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long, // Points to staff user ID
    val leaveType: String, // "CL" (Casual), "ML" (Medical), "EL" (Earned)
    val fromDate: String,
    val toDate: String,
    val reason: String,
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    val adminComment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "General", "Event", "Exam", "Scholarship"
    val targetRole: String, // "Student", "Staff", "All"
    val targetCourse: String = "All",
    val targetYear: String = "All",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val attachmentUri: String? = null,
    val attachmentName: String? = null
)

@Entity(tableName = "events")
data class CollegeEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val eventDate: String,
    val venue: String,
    val imageUrl: String = ""
)

@Entity(tableName = "exam_schedule")
data class ExamSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val course: String,
    val year: String,
    val subject: String,
    val examDate: String,
    val examDay: String,
    val time: String,
    val roomNo: String
)

@Entity(tableName = "scholarships")
data class Scholarship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val eligibility: String,
    val amount: Double,
    val lastDate: String
)

@Entity(tableName = "gallery")
data class GalleryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUrl: String,
    val caption: String,
    val eventName: String
)

@Entity(tableName = "teaching_plans")
data class TeachingPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val subjectId: Long,
    val course: String,
    val year: String,
    val division: String,
    val scheduledDay: String,
    val scheduledTime: String,
    val status: String = "Draft", // "Draft", "Pending", "Approved", "Rejected"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timetables")
data class Timetable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val course: String,
    val year: String, // Semester for B.Pharm/M.Pharm, Year for others
    val fileName: String,
    val fileUri: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

// ==========================================
// 2. CONSOLIDATED DAO INTERFACE
// ==========================================

@Dao
interface CollegeDao {
    // --- USER QUERIES ---
    @Query("SELECT * FROM users WHERE email = :email AND role = :role LIMIT 1")
    suspend fun getUserByEmailAndRole(email: String, role: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Long): Flow<User?>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    // --- STUDENT PROFILE QUERIES ---
    @Query("SELECT * FROM student_profiles WHERE userId = :userId LIMIT 1")
    fun getStudentProfileFlow(userId: Long): Flow<StudentProfile?>

    @Query("SELECT * FROM student_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getStudentProfileByUserId(userId: Long): StudentProfile?

    @Query("SELECT * FROM student_profiles")
    fun getAllStudentProfilesFlow(): Flow<List<StudentProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentProfile(profile: StudentProfile): Long

    @Update
    suspend fun updateStudentProfile(profile: StudentProfile)

    @Delete
    suspend fun deleteStudentProfile(profile: StudentProfile)

    // --- STAFF PROFILE QUERIES ---
    @Query("SELECT * FROM staff_profiles WHERE userId = :userId LIMIT 1")
    fun getStaffProfileFlow(userId: Long): Flow<StaffProfile?>

    @Query("SELECT * FROM staff_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getStaffProfileByUserId(userId: Long): StaffProfile?

    @Query("SELECT * FROM staff_profiles")
    fun getAllStaffProfilesFlow(): Flow<List<StaffProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffProfile(profile: StaffProfile): Long

    @Update
    suspend fun updateStaffProfile(profile: StaffProfile)

    @Delete
    suspend fun deleteStaffProfile(profile: StaffProfile)

    // --- SUBJECT QUERIES ---
    @Query("SELECT * FROM subjects")
    fun getAllSubjectsFlow(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE staffId = :staffId")
    fun getSubjectsByStaffFlow(staffId: Long): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Delete
    suspend fun deleteSubject(subject: Subject)

    // --- ATTENDANCE QUERIES ---
    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    fun getAttendanceForStudentFlow(studentId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE approvalStatus = 'Pending'")
    fun getPendingAttendanceFlow(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendanceFlow(): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsertAttendance(list: List<Attendance>)

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendance(id: Long)

    // --- LEAVE APPLICATIONS ---
    @Query("SELECT * FROM leave_applications WHERE staffId = :staffId ORDER BY createdAt DESC")
    fun getLeavesForStaffFlow(staffId: Long): Flow<List<LeaveApplication>>

    @Query("SELECT * FROM leave_applications ORDER BY createdAt DESC")
    fun getAllLeavesFlow(): Flow<List<LeaveApplication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveApplication): Long

    @Update
    suspend fun updateLeave(leave: LeaveApplication)

    // --- NOTIFICATIONS ---
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotificationsFlow(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    // --- EVENTS ---
    @Query("SELECT * FROM events ORDER BY id DESC")
    fun getAllEventsFlow(): Flow<List<CollegeEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CollegeEvent): Long

    @Delete
    suspend fun deleteEvent(event: CollegeEvent)

    // --- EXAMS ---
    @Query("SELECT * FROM exam_schedule ORDER BY id DESC")
    fun getAllExamsFlow(): Flow<List<ExamSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamSchedule): Long

    @Delete
    suspend fun deleteExam(exam: ExamSchedule)

    // --- SCHOLARSHIPS ---
    @Query("SELECT * FROM scholarships ORDER BY id DESC")
    fun getAllScholarshipsFlow(): Flow<List<Scholarship>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScholarship(scholarship: Scholarship): Long

    @Delete
    suspend fun deleteScholarship(scholarship: Scholarship)

    // --- GALLERY ---
    @Query("SELECT * FROM gallery ORDER BY id DESC")
    fun getAllGalleryFlow(): Flow<List<GalleryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGalleryItem(item: GalleryItem): Long

    @Delete
    suspend fun deleteGalleryItem(item: GalleryItem)

    // --- TEACHING PLANS / LECTURE SCHEDULES ---
    @Query("SELECT * FROM teaching_plans ORDER BY id DESC")
    fun getAllTeachingPlansFlow(): Flow<List<TeachingPlan>>

    @Query("SELECT * FROM teaching_plans WHERE staffId = :staffId ORDER BY id DESC")
    fun getTeachingPlansForStaffFlow(staffId: Long): Flow<List<TeachingPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachingPlan(plan: TeachingPlan): Long

    @Update
    suspend fun updateTeachingPlan(plan: TeachingPlan)

    @Delete
    suspend fun deleteTeachingPlan(plan: TeachingPlan)

    // --- TIMETABLES ---
    @Query("SELECT * FROM timetables ORDER BY id DESC")
    fun getAllTimetablesFlow(): Flow<List<Timetable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(timetable: Timetable): Long

    @Delete
    suspend fun deleteTimetable(timetable: Timetable)
}

// ==========================================
// 3. DATABASE CLIENT
// ==========================================

@Database(
    entities = [
        User::class,
        StudentProfile::class,
        StaffProfile::class,
        Subject::class,
        Attendance::class,
        LeaveApplication::class,
        Notification::class,
        CollegeEvent::class,
        ExamSchedule::class,
        Scholarship::class,
        GalleryItem::class,
        TeachingPlan::class,
        Timetable::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CollegeDatabase : RoomDatabase() {
    abstract fun collegeDao(): CollegeDao

    companion object {
        @Volatile
        private var INSTANCE: CollegeDatabase? = null

        fun getDatabase(context: Context): CollegeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CollegeDatabase::class.java,
                    "college_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. REPOSITORY & PRE-SEED GENERATOR
// ==========================================

class CollegeRepository(private val dao: CollegeDao) {

    val allUsers: Flow<List<User>> = dao.getAllUsersFlow()
    val allStudentProfiles: Flow<List<StudentProfile>> = dao.getAllStudentProfilesFlow()
    val allStaffProfiles: Flow<List<StaffProfile>> = dao.getAllStaffProfilesFlow()
    val allSubjects: Flow<List<Subject>> = dao.getAllSubjectsFlow()
    val pendingAttendance: Flow<List<Attendance>> = dao.getPendingAttendanceFlow()
    val allAttendance: Flow<List<Attendance>> = dao.getAllAttendanceFlow()
    val allLeaves: Flow<List<LeaveApplication>> = dao.getAllLeavesFlow()
    val allNotifications: Flow<List<Notification>> = dao.getAllNotificationsFlow()
    val allEvents: Flow<List<CollegeEvent>> = dao.getAllEventsFlow()
    val allExams: Flow<List<ExamSchedule>> = dao.getAllExamsFlow()
    val allScholarships: Flow<List<Scholarship>> = dao.getAllScholarshipsFlow()
    val allGalleryItems: Flow<List<GalleryItem>> = dao.getAllGalleryFlow()
    val allTeachingPlans: Flow<List<TeachingPlan>> = dao.getAllTeachingPlansFlow()
    val allTimetables: Flow<List<Timetable>> = dao.getAllTimetablesFlow()

    fun getUserById(id: Long): Flow<User?> = dao.getUserByIdFlow(id)
    fun getSubjectsForStaff(staffId: Long): Flow<List<Subject>> = dao.getSubjectsByStaffFlow(staffId)
    fun getStudentProfile(userId: Long): Flow<StudentProfile?> = dao.getStudentProfileFlow(userId)
    fun getStaffProfile(userId: Long): Flow<StaffProfile?> = dao.getStaffProfileFlow(userId)
    fun getAttendanceForStudent(studentId: Long): Flow<List<Attendance>> = dao.getAttendanceForStudentFlow(studentId)
    fun getLeavesForStaff(staffId: Long): Flow<List<LeaveApplication>> = dao.getLeavesForStaffFlow(staffId)
    fun getTeachingPlansForStaff(staffId: Long): Flow<List<TeachingPlan>> = dao.getTeachingPlansForStaffFlow(staffId)

    suspend fun authenticateUser(email: String, role: String): User? {
        return dao.getUserByEmailAndRole(email.trim(), role)
    }

    suspend fun registerUser(user: User): Long {
        return dao.insertUser(user)
    }

    suspend fun insertStudentProfile(profile: StudentProfile) {
        dao.insertStudentProfile(profile)
    }

    suspend fun insertStaffProfile(profile: StaffProfile) {
        dao.insertStaffProfile(profile)
    }

    suspend fun updateUser(user: User) {
        dao.updateUser(user)
    }

    suspend fun updateStudentProfile(profile: StudentProfile) {
        dao.updateStudentProfile(profile)
    }

    suspend fun updateStaffProfile(profile: StaffProfile) {
        dao.updateStaffProfile(profile)
    }

    suspend fun submitLeaveApplication(leave: LeaveApplication) {
        dao.insertLeave(leave)
    }

    suspend fun updateLeaveStatus(leave: LeaveApplication) {
        dao.updateLeave(leave)
    }

    suspend fun submitAttendanceBatch(list: List<Attendance>) {
        dao.bulkInsertAttendance(list)
    }

    suspend fun insertSubject(subject: Subject) {
        dao.insertSubject(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        dao.deleteSubject(subject)
    }

    suspend fun updateAttendanceRecord(attendance: Attendance) {
        dao.updateAttendance(attendance)
    }

    suspend fun insertNotification(notification: Notification) {
        dao.insertNotification(notification)
    }

    suspend fun insertEvent(event: CollegeEvent) {
        dao.insertEvent(event)
    }

    suspend fun insertExam(exam: ExamSchedule) {
        dao.insertExam(exam)
    }

    suspend fun insertScholarship(scholarship: Scholarship) {
        dao.insertScholarship(scholarship)
    }

    suspend fun insertGalleryItem(item: GalleryItem) {
        dao.insertGalleryItem(item)
    }

    suspend fun deleteGalleryItem(item: GalleryItem) {
        dao.deleteGalleryItem(item)
    }

    suspend fun deleteExam(exam: ExamSchedule) {
        dao.deleteExam(exam)
    }

    suspend fun deleteEvent(event: CollegeEvent) {
        dao.deleteEvent(event)
    }

    suspend fun deleteScholarship(scholarship: Scholarship) {
        dao.deleteScholarship(scholarship)
    }

    suspend fun insertTeachingPlan(plan: TeachingPlan) {
        dao.insertTeachingPlan(plan)
    }

    suspend fun updateTeachingPlan(plan: TeachingPlan) {
        dao.updateTeachingPlan(plan)
    }

    suspend fun deleteTeachingPlan(plan: TeachingPlan) {
        dao.deleteTeachingPlan(plan)
    }

    suspend fun insertTimetable(timetable: Timetable): Long {
        return dao.insertTimetable(timetable)
    }

    suspend fun deleteTimetable(timetable: Timetable) {
        dao.deleteTimetable(timetable)
    }

    suspend fun getStudentProfileDirect(userId: Long): StudentProfile? {
        return dao.getStudentProfileByUserId(userId)
    }

    suspend fun getStaffProfileDirect(userId: Long): StaffProfile? {
        return dao.getStaffProfileByUserId(userId)
    }

    suspend fun deleteAttendanceRecord(id: Long) {
        dao.deleteAttendance(id)
    }

    // --- COHESIVE SEEDING ENGINE ---
    suspend fun checkForSeedData() {
        withContext(Dispatchers.IO) {
            val existingAdmin = dao.getUserByEmailAndRole("admin@college.com", "Admin")
            if (existingAdmin == null) {
                // Ground Level Database Populator
                // 1. ADMIN USER
                val adminId = dao.insertUser(User(name = "College Administrator", email = "admin@college.com", passwordHash = "admin", role = "Admin", status = "active"))

                // 2. STAFF USERS
                val s1Id = dao.insertUser(User(name = "Prof. Anil Sharma", email = "staff1@college.com", passwordHash = "staff", role = "Staff", status = "active"))
                val s2Id = dao.insertUser(User(name = "Dr. Sunil Patil", email = "staff2@college.com", passwordHash = "staff", role = "Staff", status = "active"))

                dao.insertStaffProfile(StaffProfile(userId = s1Id, designation = "HOD Pharmacology", department = "Pharmacology", qualifications = "M.Pharm, Ph.D. (IIT B)", aadharNo = "XXXX-XXXX-4321", phone = "9876543210", dateOfJoining = "12-Aug-2018", photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150"))
                dao.insertStaffProfile(StaffProfile(userId = s2Id, designation = "Assistant Professor", department = "Pharmaceutics", qualifications = "M.Pharm, Ph.D. (UDCT)", aadharNo = "XXXX-XXXX-9876", phone = "9876543211", dateOfJoining = "01-Jun-2020", photoUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150"))

                // 3. STUDENT USERS
                val st1Id = dao.insertUser(User(name = "Agasthya Verma", email = "student1@college.com", passwordHash = "student", role = "Student", status = "active"))
                val st2Id = dao.insertUser(User(name = "Rohan Mehta", email = "student2@college.com", passwordHash = "student", role = "Student", status = "active"))
                val st3Id = dao.insertUser(User(name = "Sara Khan", email = "student3@college.com", passwordHash = "student", role = "Student", status = "active"))
                val st4Id = dao.insertUser(User(name = "Pebbles D'Souza", email = "student4@college.com", passwordHash = "student", role = "Student", status = "active"))

                // Standby users requesting signup/approval test metrics
                val st5Id = dao.insertUser(User(name = "Sumit Rathi", email = "sumit@college.com", passwordHash = "student", role = "Student", status = "pending"))
                val st6Id = dao.insertUser(User(name = "Alisha Roy", email = "alisha@college.com", passwordHash = "student", role = "Student", status = "pending"))

                dao.insertStudentProfile(StudentProfile(userId = st1Id, rollNo = "BP-2026-001", course = "B.Pharm", year = "4th Year", division = "A", phone = "9123456780", parentName = "Vijay Verma", address = "Sector 12, Pune", aadharNo = "9000-8000-7000", photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"))
                dao.insertStudentProfile(StudentProfile(userId = st2Id, rollNo = "BP-2026-002", course = "B.Pharm", year = "4th Year", division = "A", phone = "9123456781", parentName = "Dev Mehta", address = "Viman Nagar, Pune", aadharNo = "9100-8100-7100", photoUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150"))
                dao.insertStudentProfile(StudentProfile(userId = st3Id, rollNo = "DP-2026-044", course = "D.Pharm", year = "2nd Year", division = "B", phone = "9123456782", parentName = "Asif Khan", address = "MG Road, Pune", aadharNo = "9200-8200-7200", photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150"))
                dao.insertStudentProfile(StudentProfile(userId = st4Id, rollNo = "MP-2026-101", course = "M.Pharm", year = "1st Year", division = "C", phone = "9123456783", parentName = "Leo D'Souza", address = "Aundh, Pune", aadharNo = "9300-8300-7300", photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"))
                dao.insertStudentProfile(StudentProfile(userId = st5Id, rollNo = "BP-2026-102", course = "B.Pharm", year = "1st Year", division = "A", phone = "9321156783", parentName = "Om Rathi", address = "Hadapsar, Pune", aadharNo = "9400-8400-7400", photoUrl = ""))
                dao.insertStudentProfile(StudentProfile(userId = st6Id, rollNo = "BP-2026-103", course = "B.Pharm", year = "1st Year", division = "A", phone = "9321156784", parentName = "Joy Roy", address = "Deccan, Pune", aadharNo = "9400-8400-7401", photoUrl = ""))

                // 4. SUBJECTS
                val sub1 = dao.insertSubject(Subject(name = "Pharmacology IV", course = "B.Pharm", year = "4th Year", staffId = s1Id))
                val sub2 = dao.insertSubject(Subject(name = "Pharmaceutics III", course = "B.Pharm", year = "4th Year", staffId = s2Id))
                val sub3 = dao.insertSubject(Subject(name = "Clinical Pharmacy", course = "B.Pharm", year = "4th Year", staffId = s1Id))
                val sub4 = dao.insertSubject(Subject(name = "Biopharmaceutics", course = "B.Pharm", year = "4th Year", staffId = s2Id))

                // 5. SAMPLE SCHOLARSHIPS
                dao.insertScholarship(Scholarship(name = "Sir Ratan Tata Pharmacy Scholarship", description = "Awarded to high merit Pharmacy students in B.Pharm & D.Pharm courses demonstrating extraordinary academic grades.", eligibility = "CGPA > 8.5 / Annual income < 3 Lakhs", amount = 50000.0, lastDate = "24-Jun-2026"))
                dao.insertScholarship(Scholarship(name = "Aditya Birla Pharmacy Aid", description = "Grant schemes focused on covering tuition costs for deserving students under professional clinical PharmD divisions.", eligibility = "All registered PharmD / M.Pharm", amount = 75000.0, lastDate = "30-Jul-2026"))

                // 6. EXAM SCHEDULES
                dao.insertExam(ExamSchedule(course = "B.Pharm", year = "4th Year", subject = "Pharmacology IV", examDate = "15-Jun-2026", examDay = "Monday", time = "10:00 AM - 01:00 PM", roomNo = "Auditorium Hall A"))
                dao.insertExam(ExamSchedule(course = "B.Pharm", year = "4th Year", subject = "Pharmaceutics III", examDate = "17-Jun-2026", examDay = "Wednesday", time = "10:00 AM - 01:00 PM", roomNo = "Main Faculty Room B"))
                dao.insertExam(ExamSchedule(course = "B.Pharm", year = "4th Year", subject = "Clinical Pharmacy", examDate = "19-Jun-2026", examDay = "Friday", time = "10:00 AM - 01:00 PM", roomNo = "Auditorium Hall A"))

                // 7. EVENTS
                dao.insertEvent(CollegeEvent(title = "Annual Science Exhibition 'PharmSci' ", description = "Inter-college showcase of drug delivery prototypes, medicine synthesis methods, and visual model presentations.", eventDate = "12-Jun-2026", venue = "ICP Central Court"))
                dao.insertEvent(CollegeEvent(title = "National Pharmacovigilance Seminar", description = "Guest panels containing leading clinical drug researchers covering global adverse event registries and safety workflows.", eventDate = "22-Jun-2026", venue = "Seminar Auditorium 1"))

                // 8. NOTIFICATIONS
                dao.insertNotification(Notification(title = "Final Examination Timetable Out", message = "Sem-VIII written trials details have been published. Verify subject entries in the Examination section.", type = "Exam", targetRole = "Student"))
                dao.insertNotification(Notification(title = "Leave App Approved Reminders", message = "All faculty requested to adjust substitute lectures prior to submitting custom ML/EL logs to admin team.", type = "General", targetRole = "Staff"))
                dao.insertNotification(Notification(title = "Scholarship Portal Open", message = "National scheme applications must be submitted before 20th Jun with verification receipts.", type = "Scholarship", targetRole = "Student"))

                // 9. GALLERY IMAGES
                dao.insertGalleryItem(GalleryItem(imageUrl = "https://images.unsplash.com/photo-1582719508461-905c673771fd?w=400", caption = "College Pharmacy Laboratory Inauguration", eventName = "Inauguration 2025"))
                dao.insertGalleryItem(GalleryItem(imageUrl = "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=400", caption = "Graduation Convocation ceremony inside Main Hall", eventName = "Convocation"))
                dao.insertGalleryItem(GalleryItem(imageUrl = "https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=400", caption = "College Sports relays running trophy distribution", eventName = "Sports Meet"))

                // 10. ATTENDANCE HISTORY
                // We'll feed a robust series of Present / Absent histories to make graphics (circular subject-bars) look authentic!
                // Student 1 (BP-2026-001) in Pharmacology IV: has 9 classes total (8 PRESENT, 1 ABSENT = 89% green!)
                // Student 1 in Pharmaceutics III: has 8 classes (7 PRESENT, 1 ABSENT = 87.5% green!)
                // Student 2 (BP-2026-002) in Pharmacology: (2 PRESENT, 6 ABSENT = 25% red danger!)
                // Student 2 in Pharmaceutics: (5 PRESENT, 3 ABSENT = 62.5% yellow watch!)

                val student1Id = st1Id
                val student2Id = st2Id

                // Student 1
                for (i in 1..8) {
                    dao.insertAttendance(Attendance(studentId = student1Id, subjectId = sub1, staffId = s1Id, date = "${10 + i}-05-2026", status = "Present", approvalStatus = "Approved"))
                }
                dao.insertAttendance(Attendance(studentId = student1Id, subjectId = sub1, staffId = s1Id, date = "19-05-2026", status = "Absent", approvalStatus = "Approved"))

                for (i in 1..7) {
                    dao.insertAttendance(Attendance(studentId = student1Id, subjectId = sub2, staffId = s2Id, date = "${10 + i}-05-2026", status = "Present", approvalStatus = "Approved"))
                }
                dao.insertAttendance(Attendance(studentId = student1Id, subjectId = sub2, staffId = s2Id, date = "18-05-2026", status = "Absent", approvalStatus = "Approved"))

                // Student 2
                for (i in 1..2) {
                    dao.insertAttendance(Attendance(studentId = student2Id, subjectId = sub1, staffId = s1Id, date = "${10 + i}-05-2026", status = "Present", approvalStatus = "Approved"))
                }
                for (i in 1..6) {
                    dao.insertAttendance(Attendance(studentId = student2Id, subjectId = sub1, staffId = s1Id, date = "${12 + i}-05-2026", status = "Absent", approvalStatus = "Approved"))
                }

                for (i in 1..5) {
                    dao.insertAttendance(Attendance(studentId = student2Id, subjectId = sub2, staffId = s2Id, date = "${10 + i}-05-2026", status = "Present", approvalStatus = "Approved"))
                }
                for (i in 1..3) {
                    dao.insertAttendance(Attendance(studentId = student2Id, subjectId = sub2, staffId = s2Id, date = "${15 + i}-05-2026", status = "Absent", approvalStatus = "Approved"))
                }

                // 11. LEAVE APPLICATIONS
                dao.insertLeave(LeaveApplication(staffId = s1Id, leaveType = "CL", fromDate = "10-Jun-2026", toDate = "11-Jun-2026", reason = "Admitting parent to local cardiac care unit", status = "Pending"))
                dao.insertLeave(LeaveApplication(staffId = s2Id, leaveType = "EL", fromDate = "02-May-2026", toDate = "05-May-2026", reason = "Attending drug patent methodology registry", status = "Approved", adminComment = "Approved. Ensure alternative lectures are assigned."))
            }
        }
    }
}

// Global course-semester mapping and compatibility helper functions
fun getYearsOrSemestersForCourse(course: String): List<String> {
    return when (course) {
        "B.Pharm" -> listOf("Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8")
        "M.Pharm" -> listOf("Semester 1", "Semester 2", "Semester 3", "Semester 4")
        "PharmD" -> listOf("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "6th Year")
        "D.Pharm" -> listOf("1st Year", "2nd Year")
        else -> listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
    }
}

fun isYearOrSemMatch(profileVal: String, filterVal: String): Boolean {
    val pVal = profileVal.lowercase().trim()
    val fVal = filterVal.lowercase().trim()
    if (pVal == fVal) return true
    
    // Map existing years or semesters interchangeably to bridge seeded 4th Year / Sem 7 & 8 data points
    if (pVal == "1st year" && (fVal == "semester 1" || fVal == "semester 2")) return true
    if (pVal == "2nd year" && (fVal == "semester 3" || fVal == "semester 4")) return true
    if (pVal == "3rd year" && (fVal == "semester 5" || fVal == "semester 6")) return true
    if (pVal == "4th year" && (fVal == "semester 7" || fVal == "semester 8")) return true
    
    if (fVal == "1st year" && (pVal == "semester 1" || pVal == "semester 2")) return true
    if (fVal == "2nd year" && (pVal == "semester 3" || pVal == "semester 4")) return true
    if (fVal == "3rd year" && (pVal == "semester 5" || pVal == "semester 6")) return true
    if (fVal == "4th year" && (pVal == "semester 7" || pVal == "semester 8")) return true
    
    return false
}

