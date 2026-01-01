# Feature Specification: スタミナ消費バグ修正

## Overview

### Problem Statement
スタミナが低い状態で回復し始めるとき、ジャンプしてから走り出すとスタミナが減らなくなるバグが発生している。

現在の問題：
- スタミナ回復中にジャンプ→着地後に走るとスタミナ消費が発生しない
- プレイヤーが無限にダッシュできてしまう

### Goal
- ジャンプ後のダッシュでも正常にスタミナが消費されるようにする
- スタミナ不足時は走れないように制御する

### Success Criteria
- ジャンプ後に走り出してもスタミナが正常に消費される
- スタミナが閾値（50）未満の場合、ダッシュが開始できない
- ダッシュ中にスタミナが0になった場合、疲労状態になる

## Background

### 現在の実装
現在のスタミナシステム：
1. `PlayerToggleSprintEvent`でダッシュ開始時に`canSprint()`をチェック
2. `updateStamina()`で毎tick `player.isSprinting`を確認してスタミナを消費
3. スタミナが0になると疲労状態（exhausted）になる

### 問題の原因（推定）
1. プレイヤーがダッシュ中にジャンプ
2. 空中では`player.isSprinting`がfalseになる可能性がある
3. 着地時に`PlayerToggleSprintEvent`が発火せずにダッシュ状態が継続
4. `canSprint()`チェックがバイパスされる
5. `player.isSprinting`がtrueでもスタミナが消費されない状態が発生

## Functional Requirements

### FR-1: ダッシュ中のスタミナ確認強化
`updateStamina()`内で、プレイヤーがダッシュ中でもスタミナが閾値未満の場合は強制的にダッシュを停止する。

**条件**:
- `player.isSprinting == true`
- `canSprint(player) == false`（スタミナ不足または疲労状態）

**処理**:
- `player.isSprinting = false` でダッシュを強制停止

### FR-2: 疲労状態からの復帰後のチェック
疲労状態から回復した直後にダッシュ中だった場合、スタミナが閾値以上かを確認する。

### FR-3: ジャンプ中のスタミナ消費
ジャンプ中もダッシュキーを押している場合はスタミナを消費し続ける。

**判定方法**:
- `player.isSprinting`に加えて、プレイヤーの水平移動速度も確認
- 高速移動中はスタミナを消費

## User Scenarios & Testing

### Scenario 1: 通常ダッシュ
**Given**: プレイヤーがスタミナ100でダッシュを開始する
**When**: 5秒間ダッシュを続ける
**Then**: スタミナが約100消費される（100 - 20×5 = 0）

### Scenario 2: ジャンプ後のダッシュ（バグ修正確認）
**Given**: プレイヤーがスタミナ40（閾値未満）で立っている
**When**: ジャンプしてから走り出そうとする
**Then**: ダッシュが開始できない（スタミナ不足）

### Scenario 3: ダッシュ中のジャンプ
**Given**: プレイヤーがスタミナ80でダッシュ中
**When**: ジャンプして着地する
**Then**: 着地後もスタミナが正常に消費され続ける

### Scenario 4: スタミナ切れ直前のジャンプ
**Given**: プレイヤーがスタミナ10でダッシュ中
**When**: ジャンプする
**Then**: 空中または着地後にスタミナが0になり、疲労状態になる

### Scenario 5: 疲労状態でのジャンプ試行
**Given**: プレイヤーが疲労状態（スタミナ30）
**When**: ジャンプして走ろうとする
**Then**: ジャンプできず、ダッシュもできない

## Scope

### In Scope
- `updateStamina()`でのスタミナ/ダッシュ状態の整合性チェック
- ジャンプ中のスタミナ消費処理
- ダッシュ強制停止ロジック

### Out of Scope
- スタミナ消費量・回復量の調整
- Focus スキルの効果変更
- 疲労状態のエフェクト変更

## Assumptions
- `player.isSprinting`はBukkitが管理するダッシュ状態を正確に反映する
- `player.isSprinting = false`でダッシュを強制停止できる
- 毎tickの`updateStamina()`で状態チェックが可能

## Dependencies
- 既存のStaminaManager.kt
- 既存のPlayerListener.kt
- Bukkit Player API
