# Android 画中画（PiP）小窗播放

Android 的**画中画（Picture-in-Picture，PiP）**，本质上是让 Activity 进入一个系统管理的小窗中，同时用户可以离开当前 App，小窗继续显示视频等内容。

如果你是想做 **Android 视频播放的小窗**，核心流程可以概括成：

```
正常 Activity
	↓
用户点击 Home / 返回桌面 / 主动触发 PiP
	↓
Activity 进入 Picture-in-Picture
	↓
系统显示悬浮小窗
	↓
App 在后台继续播放视频
	↓
用户点击小窗
	↓
恢复 Activity

```
## 1. Manifest 声明

首先 Activity 需要声明支持 PiP：

``` xml
<activity
	android:name=".VideoActivity"
	android:supportsPictureInPicture="true"
	android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation">
</activity>

```
其中：

`android:supportsPictureInPicture="true"`

是关键。

**configChanges** 不是 PiP 必须项，但视频 Activity 通常建议根据自己的界面处理配置变化，避免进入/退出 PiP 时 Activity 被重新创建。

## 2. 主动进入 PiP

Java：

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	PictureInPictureParams params =
	new PictureInPictureParams.Builder()
	.setAspectRatio(new Rational(16, 9))
	.build();

    enterPictureInPictureMode(params);
}
```

Kotlin：

```Kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	val params = PictureInPictureParams.Builder()
	.setAspectRatio(Rational(16, 9))
	.build()

    enterPictureInPictureMode(params)
}
```

这里：

`Rational(16, 9)
`

表示 PiP 小窗的宽高比例。

例如：

```
16:9
4:3
1:1
9:16
```

不过系统会根据设备和当前 PiP 的规则限制最终尺寸。

## 3. 用户按 Home 时自动进入 PiP

如果你希望：

> 视频正在播放 → 用户按 Home → 自动进入小窗

可以使用：

```xml
<activity
	android:name=".VideoActivity"
	android:supportsPictureInPicture="true"
	android:autoEnterEnabled="true">
</activity>

```
**android:autoEnterEnabled** 是 Android 12（API 31）引入的。

这种情况下通常不需要自己监听 Home。

例如：
```

<activity
	android:name=".VideoActivity"
	android:supportsPictureInPicture="true"
	android:autoEnterEnabled="true"
	android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation" />
```

然后 Activity 中设置：

```
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	val params = PictureInPictureParams.Builder()
	.setAspectRatio(Rational(16, 9))
	.build()

    setPictureInPictureParams(params)
}
```

用户离开 Activity 时，系统就可以自动进入 PiP。

## 4. 判断当前是否处于 PiP
   ```
override fun onPictureInPictureModeChanged(
   	isInPictureInPictureMode: Boolean,
   	newConfig: Configuration
   ) {
   	super.onPictureInPictureModeChanged(
   	isInPictureInPictureMode,
   	newConfig)

   	if (isInPictureInPictureMode) {
   		// 进入小窗
   	} else {
   		// 退出小窗
   	}
   }

```
例如：

```
override fun onPictureInPictureModeChanged(
	isInPictureInPictureMode: Boolean,
	newConfig: Configuration
) {
	super.onPictureInPictureModeChanged(
	isInPictureInPictureMode,
	newConfig)

    if (isInPictureInPictureMode) {
        binding.videoTitle.visibility = View.GONE
        binding.videoController.visibility = View.GONE
    } else {
        binding.videoTitle.visibility = View.VISIBLE
        binding.videoController.visibility = View.VISIBLE
    }
}

```
PiP 模式下一般应该：

```
隐藏：
标题
评论
列表
按钮
复杂控制面板

保留：
VideoView / PlayerView
播放控制
```
## 5. PiP 小窗里的播放控制

PiP 支持通过 RemoteAction 添加按钮。

例如：
```
┌──────────────────────┐
│                      │
│       视频画面        │
│                      │
│    ▶       ⏭        │
└──────────────────────┘
```
可以添加：

```
播放 / 暂停
上一首
下一首
关闭
```

例如：

```
val intent = Intent(this,VideoActivity::class.java).apply {
	action = "ACTION_PLAY"
}

val pendingIntent = PendingIntent.getActivity(
	this,
	100,
	intent,
	PendingIntent.FLAG_UPDATE_CURRENT or
	PendingIntent.FLAG_IMMUTABLE
)

val action = RemoteAction(
	Icon.createWithResource(
		this,
		R.drawable.ic_play
	),
	"播放",
	"播放视频",
	pendingIntent
)

```
然后：

```
val params = PictureInPictureParams.Builder()
	.setAspectRatio(Rational(16, 9))
	.setActions(listOf(action))
	.build()

setPictureInPictureParams(params)
```

Activity 收到：

```
override fun onNewIntent(intent: Intent) {
super.onNewIntent(intent)

    when (intent.action) {
        "ACTION_PLAY" -> {
            // 播放
        }

        "ACTION_PAUSE" -> {
            // 暂停
        }
    }
}

```

## 6. Android 12+ 更推荐的方式

Android 12（API 31）以后，PiP 有一些新的参数。

例如：

```
val params = PictureInPictureParams.Builder()
	.setAspectRatio(Rational(16, 9))
	.setAutoEnterEnabled(true)
	.setSeamlessResizeEnabled(true)
	.build()

	setPictureInPictureParams(params)

```
其中：

`setAutoEnterEnabled(true)`

允许 Activity 在用户离开时自动进入 PiP。

`setSeamlessResizeEnabled(true)`

告诉系统希望 PiP 尺寸变化时尽可能平滑。

视频播放场景通常比较适合：

```
val params = PictureInPictureParams.Builder()
	.setAspectRatio(Rational(16, 9))
	.setAutoEnterEnabled(true)
	.setSeamlessResizeEnabled(true)
	.build()

setPictureInPictureParams(params)

```

## 7. 一个完整的视频 Activity

例如使用 PlayerView：

```
class VideoActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video)

        playerView = findViewById(R.id.playerView)

        setupPiP()
    }

    private fun setupPiP() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder
                .setAutoEnterEnabled(true)
                .setSeamlessResizeEnabled(true)
        }

        setPictureInPictureParams(builder.build())
    }

    fun enterPiP() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()

        enterPictureInPictureMode(params)
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
            // 小窗
            hideNormalUI()
        } else {
            // 全屏 Activity
            showNormalUI()
        }
    }

    private fun hideNormalUI() {
        // 隐藏标题、评论、列表等
    }

    private fun showNormalUI() {
        // 恢复正常 UI
    }
}
```

## 8. 一个非常重要的问题：PiP ≠ 后台播放

这两个概念容易混淆。

**PiP**

Activity

↓

进入 PiP

↓

Activity 仍然存在

↓

VideoView / PlayerView 继续显示


**后台播放**

Activity

↓

进入后台

↓

Service / MediaSession

↓

继续播放

如果你只是：

> 视频 Activity → Home → 小窗继续播放

那么 PiP 就够了。

但是如果你的需求是：

App 被杀掉 / Activity 被销毁 / 锁屏后仍然继续播放音频

那么应该结合：

```
Foreground Service
+
MediaSession
+
MediaPlayer / ExoPlayer / Media3
```

## 9. 如果是你现在这种“视频播放 App”

如果你的目标是类似：
```


┌─────────────────────────────┐
│          视频 Activity       │
│                             │
│        ┌────────────┐       │
│        │            │       │
│        │   Video    │       │
│        │            │       │
│        └────────────┘       │
│                             │
│      评论 / 推荐 / 列表      │
└─────────────────────────────┘
```

↓ Home
```

┌─────────────────┐
│                 │
│     Video       │
│                 │
│    ▶     ⏭      │
└─────────────────┘

```

那么推荐的架构是：
```


Activity
│
├── PlayerView
│      │
│      └── Media3 / ExoPlayer
│
└── PiP
│
├── AspectRatio
├── AutoEnter
├── RemoteAction
└── onPictureInPictureModeChanged()
```

而**不要为了 PiP 单独创建一个悬浮 Window** / TYPE_APPLICATION_OVERLAY。

PiP 是 Android 系统级能力，系统负责小窗的位置、大小、拖动、关闭以及和其他 App 的层级关系。