package id.stargan.intikasirfnb.licensing

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppRegApi {

    @POST("activation/challenge")
    suspend fun challenge(@Body body: ChallengeRequest): ChallengeResponse

    @POST("activation/activate")
    suspend fun activate(@Body body: ActivateRequest): ActivateResponse

    @POST("activation/reactivate")
    suspend fun reactivate(@Body body: ActivateRequest): ActivateResponse

    @GET("validate/{sn}")
    suspend fun validate(
        @Path("sn") sn: String,
        @Query("device_id") deviceId: String? = null,
    ): ValidationResponse
}
