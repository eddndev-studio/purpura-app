package com.eddndev.purpura.data.remote.api

import com.eddndev.purpura.data.remote.dto.ChangeStatusRequest
import com.eddndev.purpura.data.remote.dto.CreateEventRequest
import com.eddndev.purpura.data.remote.dto.EventDto
import com.eddndev.purpura.data.remote.dto.ExportDocumentDto
import com.eddndev.purpura.data.remote.dto.ImportRequestDto
import com.eddndev.purpura.data.remote.dto.ImportResultDto
import com.eddndev.purpura.data.remote.dto.PagedEventsResponse
import com.eddndev.purpura.data.remote.dto.UpdateEventRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

// Servicio Retrofit de /api/v1/events/* (contrato §5.5..§5.12). El base URL termina en
// /api/v1/, por eso las rutas son relativas.
interface EventApi {

    @POST("events")
    suspend fun create(@Body body: CreateEventRequest): EventDto

    @GET("events/{id}")
    suspend fun getById(@Path("id") id: String): EventDto

    @GET("events")
    suspend fun query(@QueryMap params: Map<String, String>): PagedEventsResponse

    @PATCH("events/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateEventRequest): EventDto

    @PATCH("events/{id}/status")
    suspend fun changeStatus(@Path("id") id: String, @Body body: ChangeStatusRequest): EventDto

    @DELETE("events/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @GET("events/export")
    suspend fun export(@QueryMap params: Map<String, String>): ExportDocumentDto

    @POST("events/import")
    suspend fun import(@Body body: ImportRequestDto): ImportResultDto
}
