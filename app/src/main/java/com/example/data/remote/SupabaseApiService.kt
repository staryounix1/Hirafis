package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body body: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body body: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body profile: SupabaseProfileDto
    ): Response<Unit>

    @GET("rest/v1/profiles?select=id,lat,lng")
    suspend fun getProfileLocation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String
    ): Response<List<SupabaseProfileLocationDto>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/profiles")
    suspend fun patchProfileLocation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseProfileLocationPatchDto
    ): Response<Unit>

    @GET("rest/v1/service_requests?select=*&order=created_at.desc")
    suspend fun getServiceRequests(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String
    ): Response<List<SupabaseServiceRequestDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/service_requests")
    suspend fun createServiceRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseServiceRequestDto
    ): Response<List<SupabaseServiceRequestDto>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/service_requests")
    suspend fun patchServiceRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseRequestPatchDto
    ): Response<Unit>

    @GET("rest/v1/service_offers?select=*&order=created_at.desc")
    suspend fun getOffersForRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("request_id") requestIdFilter: String
    ): Response<List<SupabaseServiceOfferDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/service_offers")
    suspend fun createOffer(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseServiceOfferDto
    ): Response<List<SupabaseServiceOfferDto>>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/service_offers")
    suspend fun patchOffer(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("id") idFilter: String,
        @Body patch: SupabaseOfferPatchDto
    ): Response<Unit>

    @GET("rest/v1/chat_messages?select=*&order=created_at.asc")
    suspend fun getChatMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Query("request_id") requestIdFilter: String
    ): Response<List<SupabaseChatMessageDto>>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/chat_messages")
    suspend fun postChatMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseChatMessageDto
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/reviews")
    suspend fun postReview(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String,
        @Body body: SupabaseReviewDto
    ): Response<Unit>
}
