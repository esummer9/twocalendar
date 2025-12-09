package com.ediapp.twocalendar.network

import android.util.Log
import com.ediapp.twocalendar.Constants
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface LunarApiService {
    @GET("getSpcifyLunCalInfo")
    suspend fun getLunarDate(
        @Query("serviceKey") serviceKey: String,
        @Query("fromSolYear") fromSolYear: String,
        @Query("toSolYear") toSolYear: String,
        @Query("lunMonth") lunMonth: String,
        @Query("lunDay") lunDay: String,
        @Query("leapMonth") leapMonth: String = "평"
    ): retrofit2.Response<LunarResponse>
}

@Root(name = "response", strict = false)
data class LunarResponse(
    @field:Element(name = "body")
    var body: LunarBody? = null
)

@Root(name = "body", strict = false)
data class LunarBody(
    @field:Element(name = "items")
    var items: LunarItems? = null
)

@Root(name = "items", strict = false)
data class LunarItems(
    @field:ElementList(inline = true, name = "item")
    var itemList: List<LunarItem>? = null
)

@Root(name = "item", strict = false)
data class LunarItem(
    @field:Element(name = "solYear")
    var solYear: Int = 0,
    @field:Element(name = "solMonth")
    var solMonth: Int = 0,
    @field:Element(name = "solDay")
    var solDay: Int = 0
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
                android.util.Log.e("convertToSolar LunarApi", "API Call Failed: ${response?.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("convertToSolar LunarApi", "API Call Failed: ${e.message}")
        }
        return null
    }
}