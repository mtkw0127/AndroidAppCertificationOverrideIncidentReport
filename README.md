# App Certification Override Incident

Android における **カスタムパーミッションの同名宣言** と **署名ローテーション (APK Signature Scheme v3)** の組み合わせによって発生するセキュリティインシデントを再現するサンプルプロジェクトです。

## 概要

`signature` レベルのカスタムパーミッションは、**同一証明書で署名されたアプリにのみ付与される**という前提で設計されています。しかし、次の条件が重なったとき、その前提が崩れます。

- 2 つのアプリが **同一名のカスタムパーミッション** を宣言している
- 後からインストールされるアプリが、先にインストールされたアプリの証明書を **祖先に持つ署名ローテーション** を使っている

このリポジトリはその状況を最小構成で再現し、挙動を観察できるようにしたものです。

## 署名の構成

```
keystore_ancestor.jks  (ancestorkey)
        │
        │  apksigner rotate
        ▼
keystore2.jks          (app2key)   ← lineage_app2.bin に祖先→新鍵の系譜を記録
```

| アプリ | パッケージ名 | 署名鍵 | 備考 |
|--------|-------------|--------|------|
| App1 | `io.github.mtkw0127.app1` | `ancestorkey` (祖先鍵) | V3 署名のみ |
| App2 | `io.github.mtkw0127.app2` | `app2key` (ローテーション後) | V3 + lineage 埋め込み |

## インシデントの仕組み

1. **App1 を先にインストール** → `CUSTOM_PERMISSION` (signature) の宣言オーナーになる
2. **App2 をインストール** → 同名の `CUSTOM_PERMISSION` を宣言するが、App2 の APK には「祖先鍵 → app2key」の系譜 (lineage) が埋め込まれている
3. Android は系譜を参照し、App2 を App1 の証明書の正当な後継と判断する
4. 結果として、**異なる鍵で署名されているにもかかわらず** App2 に `CUSTOM_PERMISSION` が付与される

## プロジェクト構成

```
.
├── app1/                      # 祖先鍵で署名されるアプリ
│   └── src/main/
│       ├── AndroidManifest.xml   # CUSTOM_PERMISSION を宣言・要求
│       └── java/.../
│           ├── MainActivity.kt           # App2 の Receiver / Provider を叩く
│           ├── feature/sync/AuthBroadcastReceiver.kt
│           └── feature/sync/AuthContentProvider.kt
├── app2/                      # ローテーション鍵 + lineage で署名されるアプリ
│   └── src/main/
│       ├── AndroidManifest.xml   # 同名の CUSTOM_PERMISSION を宣言・要求
│       └── java/.../
│           ├── MainActivity.kt           # App1 の Receiver / Provider を叩く
│           ├── feature/sync/AuthBroadcastReceiver.kt
│           └── feature/sync/AuthContentProvider.kt
├── keystore_ancestor.jks      # 祖先鍵 (app1 署名用)
├── keystore2.jks              # ローテーション後の鍵 (app2 署名用)
├── lineage_app2.bin           # apksigner が生成する署名系譜ファイル
└── setup_signing.sh           # 上記 3 ファイルを一括生成するセットアップスクリプト
```

## セットアップ

### 1. SDK パスの設定

`local.properties` に Android SDK のパスを記述します。

```properties
sdk.dir=/Users/<your-username>/Library/Android/sdk
```

### 2. 署名ファイルの生成 (初回のみ)

鍵ストアと lineage ファイルがまだない場合は、セットアップスクリプトを実行します。
すでにリポジトリに含まれている場合はスキップしてください。

```bash
chmod +x setup_signing.sh
./setup_signing.sh
```

生成されるファイル:

| ファイル | 内容 |
|----------|------|
| `keystore_ancestor.jks` | 祖先鍵 (alias: `ancestorkey`, pass: `ancestorpass`) |
| `keystore2.jks` | ローテーション後の鍵 (alias: `app2key`, pass: `app2storepass`) |
| `lineage_app2.bin` | 祖先 → app2key の署名系譜 |

## インストール

**App1 → App2 の順でインストール**することがインシデント再現の前提です。

```bash
# App1 をインストール・起動
./gradlew :app1:installDebug

# App2 をインストール・起動 (lineage 埋め込みの再署名が自動実行される)
./gradlew :app2:installDebug
```

App2 のビルドでは `installDebug` の前に `signDebugWithLineage` タスクが自動実行され、`apksigner` によって lineage が APK に埋め込まれます。

## 動作確認

両アプリを起動すると、画面に **BroadcastReceiver** と **ContentProvider** 経由の通信結果が表示されます。

- `CUSTOM_PERMISSION` が付与されている場合: メッセージが正常に届く
- 付与されていない場合: `SecurityException` が表示される

## 関連する Android の仕様

- [APK Signature Scheme v3 (key rotation)](https://source.android.com/docs/security/features/apksigning/v3)
- [`signature` protectionLevel](https://developer.android.com/reference/android/R.attr#protectionLevel)
- [カスタムパーミッションのベストプラクティス](https://developer.android.com/privacy-and-security/risks/custom-permissions)
