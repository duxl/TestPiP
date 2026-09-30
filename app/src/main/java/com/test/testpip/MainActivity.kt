package com.test.testpip

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.PictureInPictureUiState
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.test.testpip.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val ACTION_PAUSE_TIME = "ACTION_PAUSE_TIME"
    private var isPaused = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("onNewIntent: ${intent.action}")
        if (intent.action == ACTION_PAUSE_TIME) {
            isPaused = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            while (isActive && !isPaused) {
                binding.tvInfo.text = LocalTime.now().toString()
                delay(1.seconds)
            }
        }


        setupPiP()

        // 主动进入画中画
        binding.btnEnterPip.setOnClickListener {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    private fun setupPiP() {
        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            action = ACTION_PAUSE_TIME
        }

        // 示例为了简单使用PendingIntent.getActivity，事件触发时会拉起activity而突出小窗模式
        // 如果按钮点击不需要影响小窗，建议使用 PendingIntent.getBroadcast()
        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val action = RemoteAction(
            Icon.createWithResource(
                this,
                android.R.drawable.ic_media_play
            ),
            "暂停",
            "暂停时间",
            pendingIntent
        )

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(9, 16))
            .setActions(listOf(action))
            .build()
        setPictureInPictureParams(params)
    }

    // 已经处于 PiP 时，PiP 窗口自身的 UI 状态变化
    override fun onPictureInPictureUiStateChanged(pipState: PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // isStashed 表示 PiP 是否被收起到屏幕边缘（拖动小窗到屏幕边缘）
            Log.d("PiP", "isStashed = ${pipState.isStashed}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // isTransitioningToPip 表示是否正在过渡到 PiP 状态 (正在执行“进入 PiP”的动画)
            // Android 官方就是建议利用这个状态，在 PiP 进入动画开始的时候就隐藏不需要出现在小窗中的 UI，这样可以减少动画过程中的闪烁。
            Log.d("PiP", "isTransitioningToPip = ${pipState.isTransitioningToPip}")
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(
            isInPictureInPictureMode,
            newConfig
        )

        if (isInPictureInPictureMode) {
            // 进入小窗
            binding.tvTitle.text = "画中画"
            binding.btnEnterPip.isVisible = false
        } else {
            // 退出小窗
            binding.tvTitle.text = "android 的画中画（Picture-in-Picture，PiP）"
            binding.btnEnterPip.isVisible = true
        }
    }
}