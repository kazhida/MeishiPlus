# MeishiPlus

名刺を作成・編集・共有する Kotlin Multiplatform アプリです。Compose Multiplatform を使い、Android / Desktop / iOS 向けの UI と共通ロジックを `shared` モジュールにまとめています。

## 主な機能

- Google アカウントでのログイン
- Firestore を使ったユーザー・名刺データの保存
- 名刺の入力、レイアウト編集、プレビュー
- QR コードによる名刺交換
- 交換済みパートナー名刺の一覧表示
- PDF 出力と印刷用レイアウト
- SNS アカウント連携
  - GitHub
  - X
  - Qiita
  - Facebook
  - Instagram
- Android アプリ内課金によるカード追加
  - 商品 ID: `card_add_100yen`

## プロジェクト構成

```text
.
├── androidApp/   # Android アプリ本体
├── desktopApp/   # Desktop アプリ本体
├── iosApp/       # iOS アプリ本体
├── shared/       # 共通 UI・ドメインロジック・データ層
├── gradle/       # Version Catalog
└── README.md
```

## 必要な環境

- JDK 11 以上
- Android Studio
- Android SDK
- Xcode
- Firebase プロジェクト
- Google Play Console のアプリ内課金設定

## セットアップ

### Android

`androidApp/google-services.json` を配置してください。

このファイルが存在する場合のみ `com.google.gms.google-services` plugin が適用されます。

```kotlin
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
```

Google ログインでは `default_web_client_id` を利用します。Firebase Authentication の Google ログインを有効化し、Android アプリの package name が `com.abplus.meishiplus` と一致していることを確認してください。

### iOS

`iosApp/iosApp/GoogleService-Info.plist` と `iosApp/Configuration/Config.xcconfig` を配置・更新してください。

### アプリ内課金

Google Play Console で、次の 1 回限りの商品を有効化してください。

```text
Product ID: card_add_100yen
Product type: One-time product
```

Android 側では Billing Library 9 を使い、`BillingClient.ProductType.INAPP` として問い合わせます。商品が取得できない場合は Logcat の `CardAddBilling` タグに `unfetchedProductList` の内容が出力されます。

## 実行

### Android

```sh
./gradlew :androidApp:installDebug
```

または Android Studio から `androidApp` を実行してください。

### Desktop

```sh
./gradlew :desktopApp:run
```

### iOS

Xcode で `iosApp/iosApp.xcodeproj` を開いて実行してください。

## ビルド

### Android Debug APK

```sh
./gradlew :androidApp:assembleDebug
```

### Desktop 配布物

```sh
./gradlew :desktopApp:packageDistributionForCurrentOS
```

## テスト

```sh
./gradlew check
```

Android 側だけをコンパイル確認する場合:

```sh
./gradlew :androidApp:compileDebugKotlin
```

## ディープリンク

SNS 認証のリダイレクト用に、Android では次の scheme/host を受け取ります。

```text
mspls://github
mspls://x
mspls://qiita
mspls://facebook
mspls://instagram
```

## 技術スタック

- Kotlin Multiplatform
- Compose Multiplatform
- Material 3
- AndroidX Navigation
- Firebase Authentication
- Cloud Firestore
- Google Play Billing Library 9
- Ktor Client
- kotlinx.serialization
- Coil
- QRose / KScan
