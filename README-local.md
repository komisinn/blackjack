# ローカル開発環境

このプロジェクトは Java 25 / Spring Boot 4.1.1 / Gradle / MySQL 8.4 を使用します。
既存のThymeleaf・Spring MVC構成を維持しています。

## 初回：MySQLのDB・ユーザー作成

ターミナルで `mysql -u root -p` を実行し、MySQLのrootパスワードを入力します。
ログイン後、以下のパスワードを自分で決めたものに置き換えて実行します。

```sql
CREATE DATABASE IF NOT EXISTS blackjack_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'blackjack_dev'@'localhost' IDENTIFIED BY '自分で決めたパスワード';
GRANT ALL PRIVILEGES ON blackjack_dev.* TO 'blackjack_dev'@'localhost';
EXIT;
```

同名ユーザーが存在する場合は、既存のパスワード・権限を確認してください。既存データを削除する操作は必要ありません。

## 起動

PowerShellでプロジェクトフォルダーに移動し、実行します。

```powershell
cd C:\dev\blackjack
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-local.ps1
```

`blackjack_dev` ユーザーのパスワードを入力します。Springの起動完了メッセージを確認してください。
`http://localhost:8080/` を開き、`"status":"UP"` と `"database":"CONNECTED"` が表示されれば、アプリ起動とMySQLへのSQL実行を確認できています。
同じ確認は `http://localhost:8080/api/dev/health` でもできます。確認ページはlocalプロファイル限定です。ゲーム画面はまだ未実装です。
終了は Ctrl+C。

DB名・ユーザー名が異なる場合は、起動前に現在のPowerShellで設定します。

```powershell
$env:BLACKJACK_DB_NAME='既存のDB名'
$env:BLACKJACK_DB_USER='既存のユーザー名'
```

設定は `src/main/resources/application-local.properties`。
パスワードは起動時にのみ渡し、ファイルに記録しません。テーブル自動作成は無効です。

## テスト

```powershell
.\gradlew.bat test
```

テストは専用H2でSpring起動とSQL実行を確認します。MySQLの接続確認とは別です。

`Access denied` はDBユーザー・パスワード・権限、`Unknown database` はDB名・作成状況を確認してください。
