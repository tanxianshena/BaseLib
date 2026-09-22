# mylibrary 接入说明

## DeepSeek 凭据

库不再内置服务密钥。原来的 `sendRequest(owner, inputText)` 方法保持不变，
宿主在发起请求前通过运行时提供器接入用户凭据：

```kotlin
LibNetWorkApi.deepSeekApiKeyProvider = { credentialStore.currentUserApiKey }
```

`credentialStore` 是宿主自己的凭据管理对象。提供器在订阅的 IO 线程执行，
应使用线程安全的凭据存储，不要捕获 Activity 或 Fragment。
退出登录时可将提供器设为 null；未配置或返回空值时，请求通过 onError 返回，
不会发出带有空凭据的网络请求。

生产环境共享的服务密钥应保留在服务端，通过宿主后端代理请求。
运行时提供器不等于服务端代理，也不能保护被打包进 APK 的常量。
此前提交过的密钥需要在服务商后台撤销/轮换；删除源码不会撤销密钥或清除 Git 历史。

## 日志

日志默认关闭。仅在库的 debug 构建中，可显式开启：

```kotlin
LogUtils.enabled = true
```

库的 release 构建始终关闭 LogUtils 输出。网络日志仅记录 HTTP 方法和状态码，
不记录 URL、请求头、请求正文或响应正文。

## 生命周期与列表

- Fragment 的 binding 仅在 onViewCreated 到 onDestroyView 之间有效。
  binding 使用 viewLifecycleOwner，销毁视图时 unbind 并清空引用。
  视图重建后，onLoadData 会在下次 onResume 再次调用。
- 定时器使用完后仍需调用 ObservableUtil.stopTimer(key)，例如在宿主 onDestroy 中。
  stopTimer 现在同时移除订阅和回调引用；相同 key 重启会先释放旧订阅。
- 列表增删通知已修正；列表替换接口和底部条目接口保持不变。

## 回归验证

```text
gradlew.bat :mylibrary:testDebugUnitTest :mylibrary:assembleRelease
```

新增测试覆盖请求参数保真、重复参数、默认请求头、列表插入/删除通知、
带底部条目的边界情况，以及定时器停止和重复启动。
## ImageTextView

- 支持 XML 和 ImageTextView(context) 创建；默认图片 24dp，间距 5dp，文字 14sp。
- XML 颜色支持直接色值、颜色资源和 ColorStateList selector。
- 原 setTextColor(Int) 仍接收颜色资源 ID；新增 setTextColorRes、setTextColorInt 和 setTextColors。
- setImage 更新默认图片，0 表示清空。选中图片未配置时回退到默认图片；
  只配置选中图片时，取消选中会清空图片。显式选中图片不会被 setImage 覆盖。
- 未配置选中文字颜色或染色时，使用默认配置；selector 跟随父控件的选中、按下、禁用状态。
- 自动切换发生在外部点击回调之前；onSelect 返回 true 仍可阻止变化。
  itvIsClick=false 关闭自动切换，但允许外部点击监听。重复设置同一选中状态不回调。
- setShowLocal 相同方向不重复布局；非法方向抛出 IllegalArgumentException，原布局保留。
  LEFT/RIGHT 的顺序及间距跟随 RTL 布局方向。
- Android 回归测试位于 src/androidTest/java/com/tzh/baselib/ImageTextViewTest.kt，
  可在连接设备后运行 gradlew.bat :mylibrary:connectedDebugAndroidTest。

## SearchView

- XML 和 SearchView(context) 都会初始化。binding 现在是非空只读属性，仍可读取内部控件，不能替换 binding。
- sv_text_size、sv_corners、sv_image_width 按 XML 尺寸解析后的 px 应用，不重复换算。
  sv_image_width 控制两个图标的视觉尺寸，清空按钮点击区域至少 48dp；固定高度也建议不少于 48dp。
- setHintText 立即生效，不改变空输入校验。未配置 sv_no_input_hint_text 时仍允许空搜索。
- sv_input_max_length 保留提交时校验语义，正值启用，非正值不限制；按 String.length 计数，不截断或裁剪输入。
- clear 仅在非空文本变为空时触发，包括删除、点击清空、setText("")；重复设置空值不回调。
- submitSearch 提供统一提交入口；支持 IME 搜索和实体键盘 Enter。search 返回 true 时关闭键盘，false 时保持。
- showKeyBord 保留旧名称，在控件附着并获得窗口焦点后请求键盘。移除控件会取消待执行请求。
- KeyBoardUtils.openKeyboard 现在只显示键盘，不再调用 toggleSoftInput。
- 提示和无障碍文本位于 res/values/search_strings.xml。
- 测试位于 src/androidTest/java/com/tzh/baselib/SearchViewTest.kt。

## XSmartRefreshLayout

- 组合监听、Lambda 和单独刷新/加载监听共用分页逻辑。请求未结束时重复触发会被忽略。
- pageCount <= 0 表示总页数未知，允许加载下一页；正值仍作为总页数边界。
  空结果请调用 loadNoData，不再用 pageCount=0 表示空数据。
- loadSuccess(adapter) 的默认行为改为按总页数判断末页；未知总页数默认允许继续加载。
  显式 loadSuccess(adapter, true) 仍表示没有更多，false 表示还有更多。
  无总页数的接口也可调用 loadPageSuccess(hasMore, adapter)。
- 末页通过 SmartRefreshLayout 的 noMoreData 状态控制，与 setEnableLoadMore 功能开关分离。
  无 adapter 同样生效；刷新不会覆盖调用方禁用加载更多的配置。
- loadNoData 可以结束刷新或加载。已传入的 adapter 用弱引用保存，用于刷新时清除末页提示、失败时恢复提示。
- 失败会回滚请求前的页码、总页数和末页状态。刷新请求保留 pageCount，服务端返回新总数时应更新它。
- dampingRefreshStyle 关闭加载更多、保留刷新，且关闭纯滚动模式。
- stopRefreshLoad 默认没有额外延迟，会使当前请求 ID 失效并回滚页码，但不取消宿主网络请求；控件移除时同样使 ID 失效。
  刷新头尾自身的收起动画仍正常执行。

异步请求建议在发起时保存 currentRequestId（以及 pageIndex、isRefresh），回到主线程后使用带 ID 的完成接口：

```kotlin
val requestId = checkNotNull(layout.currentRequestId)
// 网络成功后，在主线程中：
layout.completeRequest(requestId, hasMore, adapter) {
    // 在这里更新列表，以及服务端返回的 pageCount。
    // 已取消、已完成或上一轮请求的回调不会执行此代码块。
}
// 网络失败后，在主线程中：
layout.failRequest(requestId)
```

旧 loadSuccess/loadError 等无 ID 接口继续支持首次直接请求和既有调用；它们无法判断响应属于哪一轮请求。
需要过滤取消后晚到的响应时，必须使用带 ID 的接口，并把列表更新放在 completeRequest 的代码块内。
所有分页属性修改、监听器注册和完成操作都应在主线程执行。
测试位于 src/androidTest/java/com/tzh/baselib/XSmartRefreshLayoutTest.kt。
