# PhysxMC プラグイン - 問題と対策

## 重大な問題（2025-07-31）

### サーバーフリーズ/デッドロック
**問題**: 
```
[03:12:56 ERROR]: The server has not responded for 10 seconds! Creating thread dump
[03:12:59 WARN]: Can't keep up! Is the server overloaded? Running 12162ms or 243 ticks behind
```

**原因**: 
- DisplayedPhysxBox.getSurroundingChunks()でチャンクロード待機
- PhysxMc.java:164行目のメインスレッドタスク内でブロッキング処理
- チャンクローダーがデッドロック状態（chunk (-16,1) の読み込み待機）

**影響**: 
- サーバーが完全にフリーズ
- プレイヤーが操作不能
- 強制終了が必要

**緊急対策が必要**: 
- DisplayedBoxHolder.update()の無効化
- または非同期でのチャンク処理に変更

## 新たな問題（2025-07-31）

### 非同期エンティティ追加エラー
**問題**: 
```
[03:03:30 ERROR]: Thread Craft Scheduler Thread - 5 - PhysxMc failed main thread check: entity add
java.lang.IllegalStateException: Asynchronous entity add!
```

**原因**: 
- プッシャー/ランプのロード処理が非同期スレッドでエンティティを作成しようとしている
- DisplayedBoxHolder.createDisplay()がメインスレッド以外から呼ばれている
- PhysxCommand.java:492, 495行目の非同期タスク内での処理

**影響**: 
- プッシャーとランプの読み込みが失敗（0個読み込み）
- 保存されたデータが復元されない

**対策が必要**: 
- loadPushers()とloadRamps()をメインスレッドで実行するよう修正
- または、エンティティ作成部分のみメインスレッドで実行

## 解決済み問題

### サーバークラッシュ問題（2025-07-31修正）
**問題**: `/physxmc save` コマンド実行時にサーバーがクラッシュ（終了コード -1073740940）

**原因**: PhysicsObjectManager.saveAll()でPhysXネイティブオブジェクトへのアクセス時にメモリ違反

**対策**: 
- PhysicsObjectManager.saveAll()の呼び出しを無効化
- プッシャーとランプの保存のみ実行（安全に動作確認済み）
- ボックス・スフィアの保存は一時的に無効化

### コンパイルエラー修正
- PhysxCommand.java:447: 内部クラスから参照される変数のfinal化
- PhysicsObjectManager.java:179: double→float型変換エラー修正

## 現在の保存機能

### 保存コマンド
```
/physxmc save
```

### 保存されるオブジェクト
- ✅ **プッシャー**: 位置と動き状態（保存OK、読み込み失敗）
- ✅ **ランプ**: 位置、角度、サイズ、マテリアル（保存OK、読み込み失敗）
- ❌ **ボックス**: 一時的に無効化（ネイティブクラッシュ防止）
- ❌ **スフィア**: 保存対象外

### 使用方法
1. プッシャーやランプを配置
2. `/physxmc save` コマンドを実行
3. ⚠️ サーバー再起動後の自動復元は現在失敗中

## その他のエラー

### 起動時エラー（未解決）
```
[21:23:58 ERROR]: Error loading saved data: raids
java.io.EOFException: Unexpected end of ZLIB input stream
```
→ Minecraftの raids.dat ファイル破損（PhysxMCとは無関係）

### ランプ作成時のクラッシュ（未解決）
```
/physxmc ramp create 20 3 6 0.5 QUARTZ_BLOCK
```
→ 要調査（ネイティブメモリ問題の可能性）