package com.piania.app.data.remote

import com.piania.app.data.model.request.ChatMessageRequestDTO
import com.piania.app.data.model.request.ChatReadStateUpsertRequestDTO
import com.piania.app.data.model.request.ClassEnrollmentRequestDTO
import com.piania.app.data.model.request.LoginRequestDTO
import com.piania.app.data.model.request.RegistroRequestDTO
import com.piania.app.data.model.request.PracticeSessionUpdateRequestDTO
import com.piania.app.data.model.request.ShareLinkCreateRequestDTO
import com.piania.app.data.model.request.SheetMusicCreateRequestDTO
import com.piania.app.data.model.request.UserSettingsRequestDTO
import com.piania.app.data.model.response.AnalisisResponseDTO
import com.piania.app.data.model.response.ChatMessagePageResponseDTO
import com.piania.app.data.model.response.ChatReadStateResponseDTO
import com.piania.app.data.model.response.ChatMessageResponseDTO
import com.piania.app.data.model.response.AnnouncementPageResponseDTO
import com.piania.app.data.model.response.AnnouncementResponseDTO
import com.piania.app.data.model.response.ClassEnrollmentPageResponseDTO
import com.piania.app.data.model.response.ClassEnrollmentResponseDTO
import com.piania.app.data.model.response.FeedbackResponseDTO
import com.piania.app.data.model.response.AuthResponseDTO
import com.piania.app.data.model.response.FileUploadResponseDTO
import com.piania.app.data.model.response.PartituraResponseDTO
import com.piania.app.data.model.response.PracticeFeedbackResponseDTO
import com.piania.app.data.model.response.PracticeSessionPageResponseDTO
import com.piania.app.data.model.response.PracticeSessionResponseDTO
import com.piania.app.data.model.response.PracticaResponseDTO
import com.piania.app.data.model.response.SheetMusicPageResponseDTO
import com.piania.app.data.model.response.SheetMusicResponseDTO
import com.piania.app.data.model.response.ShareLinkPageResponseDTO
import com.piania.app.data.model.response.ShareLinkResponseDTO
import com.piania.app.data.model.response.TeacherInvitationResponseDTO
import com.piania.app.data.model.response.TeacherStudentRelationDTO
import com.piania.app.data.model.response.TeacherStudentRelationPageResponseDTO
import com.piania.app.data.model.response.UserSettingsResponseDTO
import com.piania.app.data.model.response.VirtualClassDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.Query


interface PianiaApiService {

    // --- AUTH (auth-service) ---
    @POST("/piania/auth/register")
    suspend fun register(@Body registroRequest: RegistroRequestDTO): Response<AuthResponseDTO>

    @POST("/piania/auth/login")
    suspend fun login(@Body loginRequestDTO: LoginRequestDTO): Response<AuthResponseDTO>

    @GET("/piania/auth/me")
    suspend fun me(): Response<com.piania.app.data.model.response.UserProfileResponseDTO>

    @PUT("/piania/auth/me/avatar/{avatar}")
    suspend fun updateMyAvatar(@Path("avatar") avatar: String): Response<com.piania.app.data.model.response.UserProfileResponseDTO>

    // --- CORE (core-service) ---
    // API Gateway enruta /piania/core/** al core-service (ver api-gateway application.yml)
    // Por tanto, todos los endpoints del core van prefijados con /piania/core

    // User Settings (idioma, notificaciones, modo oscuro, etc.)
    @GET("/piania/core/settings")
    suspend fun getUserSettings(): Response<UserSettingsResponseDTO>

    @PUT("/piania/core/settings")
    suspend fun upsertUserSettings(@Body body: UserSettingsRequestDTO): Response<UserSettingsResponseDTO>

    // Announcements (core-service)
    // Backend nuevo expone /piania/announcements (ver AnnouncementController).
    // El API Gateway enruta /piania/core/** al core-service, así que:
    //  - desde el móvil llamamos a /piania/core/announcements -> core-service /piania/announcements
    @GET("/piania/core/announcements")
    suspend fun listAnnouncements(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<AnnouncementPageResponseDTO>

    // ADMIN: crear anuncio
    @POST("/piania/core/announcements")
    suspend fun createAnnouncement(
        @Body body: com.piania.app.data.model.request.AnnouncementCreateRequestDTO
    ): Response<AnnouncementResponseDTO>

    // ADMIN: desactivar anuncio
    @DELETE("/piania/core/announcements/{id}")
    suspend fun deactivateAnnouncement(
        @Path("id") id: Long
    ): Response<AnnouncementResponseDTO>

    // Uploads (core-service)
    // OJO: el controller del backend está mapeado a /piania/uploads (ver FileUploadController)
    // y el API Gateway enruta /piania/core/** al core-service.
    // Por tanto el endpoint real expuesto via gateway es: /piania/core/uploads -> /piania/uploads
    @Multipart
    @POST("/piania/core/uploads")
    suspend fun uploadFileToCore(
        @Part file: MultipartBody.Part
    ): Response<FileUploadResponseDTO>

    // Sheet Music (core-service)
    @GET("/piania/core/sheet-music")
    suspend fun listSheetMusic(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<SheetMusicPageResponseDTO>

    @POST("/piania/core/sheet-music")
    suspend fun createSheetMusic(@Body body: SheetMusicCreateRequestDTO): Response<SheetMusicResponseDTO>

    @GET("/piania/core/sheet-music/{id}/analysis")
    suspend fun getSheetMusicAnalysis(@Path("id") id: Long): Response<com.piania.app.data.model.response.SheetMusicAnalysisResponseDTO>

    // Practices (core-service)
    @GET("/piania/core/practices")
    suspend fun listPractices(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PracticeSessionPageResponseDTO>

    @POST("/piania/core/practices")
    suspend fun createPractice(@Body body: Any): Response<PracticeSessionResponseDTO>

    @GET("/piania/core/practices/{id}")
    suspend fun getPractice(@Path("id") id: Long): Response<PracticeSessionResponseDTO>

    @GET("/piania/core/practices/by-sheet-music/{sheetMusicId}")
    suspend fun listPracticesBySheetMusic(
        @Path("sheetMusicId") sheetMusicId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PracticeSessionPageResponseDTO>

    @PATCH("/piania/core/practices/{id}")
    suspend fun updatePracticeNotes(
        @Path("id") id: Long,
        @Body body: PracticeSessionUpdateRequestDTO
    ): Response<PracticeSessionResponseDTO>

    @GET("/piania/core/practices/{id}/feedback")
    suspend fun getPracticeFeedback(
        @Path("id") id: Long
    ): Response<PracticeFeedbackResponseDTO>

    // --- TEACHER / CLASSES / SHARING (core-service) ---
    @POST("/piania/core/classes")
    suspend fun createClass(@Query("name") name: String): Response<VirtualClassDTO>

    @GET("/piania/core/classes/my")
    suspend fun listMyClasses(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<com.piania.app.data.model.response.VirtualClassPageResponseDTO>

    @GET("/piania/core/classes/enrolled")
    suspend fun listEnrolledClasses(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<com.piania.app.data.model.response.VirtualClassPageResponseDTO>

    @DELETE("/piania/core/classes/{id}")
    suspend fun deleteClass(@Path("id") id: Long): Response<Unit>

    @PUT("/piania/core/classes/{id}")
    suspend fun updateClass(
        @Path("id") id: Long,
        @Query("name") name: String? = null,
        @Query("groupAvatar") groupAvatar: String? = null
    ): Response<VirtualClassDTO>

    @POST("/piania/core/teacher-relations")
    suspend fun linkToTeacher(@Query("teacherEmail") teacherEmail: String): Response<Unit>

    @GET("/piania/core/teacher-relations/my-students")
    suspend fun listMyStudents(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200
    ): Response<TeacherStudentRelationPageResponseDTO>

    @POST("/piania/core/class-enrollments")
    suspend fun addStudentToClass(@Body body: ClassEnrollmentRequestDTO): Response<ClassEnrollmentResponseDTO>

    // --- CLASS INVITATIONS (core-service) ---
    @POST("/piania/core/class-invitations")
    suspend fun createClassInvitation(
        @Query("classId") classId: Long,
        @Query("hoursValid") hoursValid: Int = 168
    ): Response<com.piania.app.data.model.response.ClassInvitationResponseDTO>

    @POST("/piania/core/class-invitations/{token}/accept")
    suspend fun acceptClassInvitation(
        @Path("token") token: String
    ): Response<Unit>

    @GET("/piania/core/class-enrollments/{classId}")
    suspend fun listClassStudents(
        @Path("classId") classId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200
    ): Response<ClassEnrollmentPageResponseDTO>

    @DELETE("/piania/core/class-enrollments/{enrollmentId}")
    suspend fun removeStudentFromClass(@Path("enrollmentId") enrollmentId: Long): Response<Unit>

    @POST("/piania/core/share-links")
    suspend fun createShareLink(@Body body: ShareLinkCreateRequestDTO): Response<ShareLinkResponseDTO>

    @GET("/piania/core/share-links/me")
    suspend fun listMyShareLinks(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200
    ): Response<ShareLinkPageResponseDTO>

    @POST("/piania/core/share-links/import/{token}")
    suspend fun importSharedSheet(
        @Path("token") token: String
    ): Response<SheetMusicResponseDTO>

    @DELETE("/piania/core/share-links/{id}")
    suspend fun revokeShareLink(@Path("id") id: Long): Response<Unit>

    // --- CHAT (core-service) ---
    @GET("/piania/core/classes/{classId}/chat")
    suspend fun listChatMessages(
        @Path("classId") classId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ChatMessagePageResponseDTO>

    @POST("/piania/core/classes/{classId}/chat")
    suspend fun sendChatMessage(
        @Path("classId") classId: Long,
        @Body body: ChatMessageRequestDTO
    ): Response<ChatMessageResponseDTO>

    @PATCH("/piania/core/classes/{classId}/chat/{messageId}/pin")
    suspend fun pinChatMessage(
        @Path("classId") classId: Long,
        @Path("messageId") messageId: Long
    ): Response<Unit>

    @PATCH("/piania/core/classes/{classId}/chat/{messageId}/unpin")
    suspend fun unpinChatMessage(
        @Path("classId") classId: Long,
        @Path("messageId") messageId: Long
    ): Response<Unit>

    @DELETE("/piania/core/classes/{classId}/chat/{messageId}")
    suspend fun deleteChatMessage(
        @Path("classId") classId: Long,
        @Path("messageId") messageId: Long
    ): Response<Unit>

    // --- CHAT READ STATE (core-service) ---
    @GET("/piania/core/chat-read-state/me/{virtualClassId}")
    suspend fun getMyChatReadState(
        @Path("virtualClassId") virtualClassId: Long
    ): Response<ChatReadStateResponseDTO>

    @POST("/piania/core/chat-read-state/me")
    suspend fun upsertMyChatReadState(
        @Body body: ChatReadStateUpsertRequestDTO
    ): Response<ChatReadStateResponseDTO>
}
