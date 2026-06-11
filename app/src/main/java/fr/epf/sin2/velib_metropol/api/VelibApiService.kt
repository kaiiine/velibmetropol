package fr.epf.sin2.velib_metropol.api

import fr.epf.sin2.velib_metropol.model.GbfsInformationResponse
import fr.epf.sin2.velib_metropol.model.GbfsStatusResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface VelibApiService {

    @GET("station_information.json")
    suspend fun getStationInformation(): GbfsInformationResponse

    @GET("station_status.json")
    suspend fun getStationStatus(): GbfsStatusResponse

    companion object {
        private const val BASE_URL =
            "https://velib-metropole-opendata.smovengo.cloud/opendata/Velib_Metropole/"

        val instance: VelibApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VelibApiService::class.java)
        }
    }
}
