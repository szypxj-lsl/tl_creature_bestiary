生物图鉴拓展（tl_creature_bestiary）
Minecraft 1.20.1 / Forge 47.4.10 / Java 17
版本：1.0.0
硬依赖：tl_domesticate_more_creatures 1.0.0+

首次构建：
1. 先把随包提供的 TDMC API 支持覆盖包应用到 TDMC 当前源码。
2. 在 TDMC 工程执行 gradlew build。
3. 将 TDMC 的 build/libs/tl_domesticate_more_creatures-1.0.0.jar 复制到本工程 libs/。
4. Windows 执行 gradlew.bat build；Linux/macOS 执行 ./gradlew build。

当前自动化执行环境无法联网下载 Gradle/Forge 依赖，因此交付源码包不会伪装包含一个“新 API 已编译”的 TDMC JAR。必须使用你本机编译出的最新 TDMC JAR。
