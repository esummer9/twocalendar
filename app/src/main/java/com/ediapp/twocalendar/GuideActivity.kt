package com.ediapp.twocalendar

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import androidx.activity.OnBackPressedCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.MobileAds
import android.util.Log
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api

class GuideActivity : ComponentActivity() {
    private var mInterstitialAd: InterstitialAd? = null
    private val tag = "GuideActivity"

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.initialize(this) {}

        loadInterstitialAd()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showInterstitialAndFinish()
            }
        })

        setContent {
            TwocalendarTheme {
                GuideScreen(onNavigateUp = { showInterstitialAndFinish() })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val viewGuideCount = sharedPreferences.getInt("view_guide_count", 0)
        sharedPreferences.edit().putInt("view_guide_count", viewGuideCount + 1).apply()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this, Constants.AD_UNIT_ID_INTERSTITIAL, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(tag, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(tag, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun showInterstitialAndFinish() {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val viewGuideCount = sharedPreferences.getInt("view_guide_count", 0)
        if (viewGuideCount % 5 == 4) {
            if (mInterstitialAd != null) {
                Log.d(tag, "Attempting to show interstitial ad.")
                mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(tag, "Ad was dismissed.")
                        finish()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.d(tag, "Ad failed to show.")
                        finish()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(tag, "Ad showed fullscreen content.")
                        mInterstitialAd = null
                    }
                }
                mInterstitialAd?.show(this@GuideActivity)
            } else {
                Log.d(tag, "The interstitial ad wasn't ready yet or failed to load. Finishing activity.")
                finish()
            }
        } else {
            finish()
        }
    }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onNavigateUp: () -> Unit) { // 람다 파라미터 추가
    val activity = LocalContext.current as Activity
    val images = listOf(
        R.drawable.guide_1,
        R.drawable.guide_2,
        R.drawable.guide_3,
        R.drawable.guide_3_1,
        R.drawable.guide_3_2
    )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { images.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "가이드") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) { // 여기서 람다 호출
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                Image(
                    painter = painterResource(id = images[page]),
                    contentDescription = "Guide Image ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            Row(
                Modifier
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                images.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(10.dp)
                            .background(
                                color = if (index == pagerState.currentPage) Color.Magenta else Color.DarkGray,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGuideScreen() {
    TwocalendarTheme {
        GuideScreen(onNavigateUp = {}) // Preview에서는 빈 람다 전달
    }
}
