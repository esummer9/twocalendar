package com.ediapp.twocalendar

import android.content.Context
import android.provider.Settings

object Constants {

    const val my_sep = ";;;"

    const val AD_UNIT_ID_BANNER = "ca-app-pub-9901915016619662/1142552664"
    // 전면 ca-app-pub-9901915016619662/5677404488
    const val AD_UNIT_ID_INTERSTITIAL = "ca-app-pub-9901915016619662/5677404488"



    data class ApiConfig(
        val baseUrl: String,
        val serviceKey: String,
        val description: String
    )

    val API_CONFIGS: Map<String, ApiConfig> = mapOf(
        "24_DIVISIONS" to ApiConfig(
            baseUrl = "https://apis.data.go.kr/B090041/openapi/service/get24DivisionsInfo/",
            serviceKey = "MzfGSuOeRsHJxatg9rabhIC/8lw/f95wNLGSGakqMb40S/Orp+UrYhLEtTznRzBLWFePFHTsYARFSn1L2PQLGA==",
            description = "24절기 정보 조회"
        ),
        "NATIONAL_HOLIDAY" to ApiConfig(
            baseUrl = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/",
            serviceKey = "MzfGSuOeRsHJxatg9rabhIC/8lw/f95wNLGSGakqMb40S/Orp+UrYhLEtTznRzBLWFePFHTsYARFSn1L2PQLGA==",
            description = "국경일 정보 조회"
        ),
        "HOLIDAY" to ApiConfig(
            baseUrl = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/",
            serviceKey = "MzfGSuOeRsHJxatg9rabhIC/8lw/f95wNLGSGakqMb40S/Orp+UrYhLEtTznRzBLWFePFHTsYARFSn1L2PQLGA==",
            description = "공휴일 정보 조회"
        )
        // 여기에 다른 API 설정을 추가할 수 있습니다.
    )
}



fun getAndroidId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

