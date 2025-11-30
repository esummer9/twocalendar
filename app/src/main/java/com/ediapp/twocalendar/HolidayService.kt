package com.ediapp.twocalendar

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Data models for XML parsing
@Root(name = "response", strict = false)
data class HolidayResponse(
    @field:Element(name = "body")
    var body: Body? = null
)

@Root(name = "body", strict = false)
data class Body(
    @field:Element(name = "items")
    var items: Items? = null
)

@Root(name = "items", strict = false)
data class Items(
    @field:ElementList(inline = true, name = "item")
    var itemList: List<HolidayItem>? = null
)

@Root(name = "item", strict = false)
data class HolidayItem(
    @field:Element(name = "dateName")
    var dateName: String? = null,
    @field:Element(name = "locdate")
    var locdate: String? = null
)

// Retrofit service interface
interface HolidayApiService {
    @GET("getHoliDeInfo")
    suspend fun getHolidays(
        @Query("serviceKey") serviceKey: String,
        @Query("solYear") year: Int = 2025,
        @Query("solMonth") month: String ="",
        @Query("numOfRows") numOfRows: Int = 100
    ): Response<HolidayResponse>
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

interface LunarApiService {
    @GET("getSpcifyLunCalInfo")
    suspend fun getLunarDate(
        @Query("serviceKey") serviceKey: String,
        @Query("fromSolYear") fromSolYear: String,
        @Query("toSolYear") toSolYear: String,
        @Query("lunMonth") lunMonth: String,
        @Query("lunDay") lunDay: String,
        @Query("leapMonth") leapMonth: String = "평"
    ): Response<LunarResponse>
}