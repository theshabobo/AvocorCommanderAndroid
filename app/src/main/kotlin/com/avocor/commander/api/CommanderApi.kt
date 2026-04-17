package com.avocor.commander.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface CommanderApi {

    // ── Auth ──

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // ── Devices ──

    @GET("api/devices")
    suspend fun getDevices(): List<DeviceDto>

    @GET("api/status")
    suspend fun getStatus(): List<DeviceStatusDto>

    @POST("api/devices/{id}/command")
    suspend fun sendCommand(
        @Path("id") id: Int,
        @Body request: CommandRequest
    ): CommandResponse

    @POST("api/devices/{id}/wake")
    suspend fun wakeDevice(@Path("id") id: Int): WakeResponse

    @POST("api/devices/{id}/connect")
    suspend fun connectDevice(@Path("id") id: Int): SuccessResponse

    @POST("api/devices/{id}/disconnect")
    suspend fun disconnectDevice(@Path("id") id: Int): SuccessResponse

    // ── Groups ──

    @GET("api/groups")
    suspend fun getGroups(): List<GroupDto>

    @POST("api/groups/{id}/command")
    suspend fun sendGroupCommand(
        @Path("id") id: Int,
        @Body request: GroupCommandRequest
    ): List<CommandResponse>

    // ── Macros ──

    @GET("api/macros")
    suspend fun getMacros(): List<MacroDto>

    @POST("api/macros/{id}/run")
    suspend fun runMacro(
        @Path("id") id: Int,
        @Body request: MacroRunRequest
    ): SuccessResponse

    // ── Commands ──

    @GET("api/commands")
    suspend fun getCommands(@Query("series") series: String? = null): List<CommandDto>

    @GET("api/commands/series")
    suspend fun getCommandSeries(): List<String>

    companion object {
        fun create(baseUrl: String, tokenProvider: () -> String?): CommanderApi {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val token = tokenProvider()
                val request = if (token != null) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            return Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CommanderApi::class.java)
        }
    }
}
