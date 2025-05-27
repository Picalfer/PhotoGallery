package com.android.photogallery.api

import com.android.photogallery.models.OpenverseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenverseApiService {
    @GET("images/")
    suspend fun searchImages(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): Response<OpenverseResponse>
}