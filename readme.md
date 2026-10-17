# ktv-casting-for-android

---

使用教程见[飞书文档](https://idol-zju.feishu.cn/wiki/Ytt1wNh88i6E9jkEndhcxNmYnBd)

---

本项目是 [ktv-casting](https://github.com/KARAOKE-MASTER-ZJU/ktv-casting) 的安卓前端

## DLNA 搜索诊断

搜索失败或未获取到设备时，点击错误提示中的「查看运行日志」，也可从设置页打开日志。日志页支持复制、分享和保存。

一次搜索会记录 App/Android 版本、系统默认网络的 Wi-Fi/VPN 状态、调用耗时和返回设备数。默认网络信息不代表 SSDP 实际使用的接口。配套的新 Rust 引擎还会记录搜索目标、发现结果项数、重复/过滤/错误数，以及手动连接时的地址解析、设备描述获取/解析和超时错误；结果项数不是原始 SSDP 数据包数。

提交问题时请附上从「诊断环境」到搜索结束的完整日志，并说明其他 DLNA 客户端在同一设备和 Wi-Fi 下是否能发现目标。空列表不等于设备离线。设备名称和局域网地址可能出现在日志中。

Debug CI 从固定的 Rust 子模块构建并运行 JVM 单元测试；正式 Release 仍使用 `rust_libs_version` 指定的已发布原生库。发布包含完整原生诊断的 APK 前，需要先发布对应 Rust 版本并更新该配置。

## 环境配置

如果你有本地构建发布的需求，复制 `app/local.properties.example` 为 `app/local.properties`，填写你的仓库信息：

```properties
repo_owner=你的GitHub用户名
repo_name=你的仓库名
```

未配置时默认使用 `KARAOKE-MASTER-ZJU/ktv-casting-android-app`。如果你使用 GitHub Actions 构建，CI 会自动注入当前仓库的 owner/name，无需手动配置 `local.properties`。

应用内更新检查读取 `https://<repo_owner>.github.io/<repo_name>/release.json`，需在仓库 Settings 中启用 GitHub Pages（source 选 `gh-pages` 分支），并配置 CI（见 `.github/workflows/build-and-release.yml` 顶部注释）。

## 环境准备

---

```shell
# 克隆 android-app分支的rust源码
git clone -b android-app --single-branch https://github.com/KARAOKE-MASTER-ZJU/ktv-casting.git

cd ktv-casting

rustup target add aarch64-linux-android
cargo ndk -t arm64-v8a build --lib --release
# 产物输出在 ./target/aarch64-linux-android/release/libktv_casting_lib.so

# 下面是更多版本

# rustup target add armv7-linux-androideabi
# cargo ndk -t armeabi-v7a build --lib --release

# rustup target add i686-linux-android
# cargo ndk -t x86 build --lib --release

# rustup target add x86_64-linux-android
# cargo ndk -t x86_64 build --lib --release

cd ..
```
这样就得到了`libktv_casting_lib.so`

接下来克隆本项目
```shell
# 主仓库
git clone https://github.com/KARAOKE-MASTER-ZJU/ktv-casting-android-app.git

# 如果你 fork 了项目，改为你自己的仓库地址，并按上面的 Fork 配置步骤设置 local.properties

cd ktv-casting-android-app
```

打开 [Android Studio](https://developer.android.google.cn/studio?hl=zh-cn) 将文件夹打开为项目

把上一步得到的编译产物放到`app/src/main/jniLibs/arm64-v8a/libktv_casting_lib.so`(这里的架构版本根据上面-t参数选择)

直接在android studio运行调试即可
