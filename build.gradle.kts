import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ajoberstar.grgit.Grgit

plugins {
    // Javaプラグインを適用
    java
    // ShadowJar(依存関係埋め込み)を使用するためのプラグイン
    id("com.github.johnrengelman.shadow") version "6.0.0"
    // Gitに応じた自動バージョニングを行うためのプラグイン
    id("org.ajoberstar.grgit") version "4.1.1"
}

// グループ定義
group = "com.quarri6343"
// バージョン定義
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Paperの依存リポジトリ
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

configurations {
    // PhysX関係を別の設定にする
    val includeLib by creating
    compileOnly.get().extendsFrom(includeLib)
}

dependencies {
    // PaperAPI
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")

    // IncludeLib
    val includeLib by configurations

    // java bindings
    includeLib("de.fabmax:physx-jni:2.0.6")

    // native libraries, you can add the one matching your system or all
    includeLib("de.fabmax:physx-jni:2.0.6:natives-windows")
    includeLib("de.fabmax:physx-jni:2.0.6:natives-linux")
    includeLib("de.fabmax:physx-jni:2.0.6:natives-macos")

    // JOML
    implementation("org.joml:joml:1.10.5")
}

tasks {
    jar {
        // -bukkitを除く
        archiveAppendix.set("")
        // 依存関係を埋め込んでいないjarは末尾に-originalを付与する
        archiveClassifier.set("original")
    }

    // ソースjarを生成する
    val includeLibZip by registering(ShadowJar::class) {
        archiveBaseName.set("libs")
        archiveVersion.set("")
        archiveClassifier.set("")
        archiveExtension.set("zip")
        destinationDirectory.set(layout.buildDirectory.dir("distributions"))
        val includeLib by project.configurations
        from(includeLib)
    }

    // リソースパックを生成する
    val resourcepack by registering(Zip::class) {
        archiveClassifier.set("resourcepack")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        from("resourcepack")
    }

    // fatJarを生成する
    shadowJar {
        // 依存関係を埋め込んだjarは末尾なし
        archiveClassifier.set("")
        // IncludeLibを埋め込む
        from(includeLibZip)
    }

    // アーティファクトを登録する
    artifacts {
        // 依存関係を埋め込んだjarをビルドする
        add("archives", shadowJar)
        // リソースパックを生成する
        add("archives", resourcepack)
    }

    // plugin.ymlの中にバージョンを埋め込む
    @Suppress("UnstableApiUsage")
    withType<ProcessResources> {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}