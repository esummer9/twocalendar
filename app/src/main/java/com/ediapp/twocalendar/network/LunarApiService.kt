package com.ediapp.twocalendar.network

import android.util.Log
import com.ediapp.twocalendar.Constants
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface LunarApiService {
    @GET("getLunCalInfo")
    suspend fun getLunarDate(
        @Query("serviceKey", encoded = true) serviceKey: String,
        @Query("solYear") fromSolYear: String,
        @Query( "solMonth") toSolYear: String,
        @Query("solDay") lunMonth: String,
        @Query("lunDay") lunDay: String
    ): retrofit2.Response<LunarResponse>
}

data class LunarResponse(
    val body: Body
)

data class Body(
    val items: Items
)

data class Items(
    @org.simpleframework.xml.ElementList(inline = true)
    val itemList: List<LunarItem>?
)

data class LunarItem(
    val solYear: Int,
    val solMonth: Int,
    val solDay: Int
)

object LunarApi {
    private val apiConfig = Constants.API_CONFIGS["LUNAR"]

    private val retrofit = Retrofit.Builder()
        .baseUrl(apiConfig!!.baseUrl)
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .build()

    val service: LunarApiService = retrofit.create(LunarApiService::class.java)

    suspend fun convertToSolar(year: Int, month: Int, day: Int): java.time.LocalDate? {
        try {
            val response = apiConfig?.let {

                Log.d("convertToSolar", "serviceKey: ${it.serviceKey} $year-$month-$day")
                Log.d("convertToSolar", "baseUrl: ${it.baseUrl} $year-$month-$day")

                service.getLunarDate(
                    serviceKey = it.serviceKey,
                    fromSolYear = year.toString(),
                    toSolYear = year.toString(),
                    lunMonth = String.format("%02d", month),
                    lunDay = String.format("%02d", day)
                )
            }

            if (response?.isSuccessful ?: false) {
                val lunarItem = response.body()?.body?.items?.itemList?.firstOrNull()
                if (lunarItem != null) {
                    return java.time.LocalDate.of(lunarItem.solYear, lunarItem.solMonth, lunarItem.solDay)
                }
            } else {
                android.util.Log.e("LunarApi", "API Call Failed: ${response?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("LunarApi", "API Call Failed: ${e.message}")
        }
        return null
    }
}