# Skills Plugin for Minecraft

クラシックMMOにインスパイアされたスキルベースの成長システムをMinecraftに追加するプラグインです。

**📖 プレイヤードキュメント**: https://0x48lab.github.io/skills/

## 特徴

- **スキルベースの成長**: レベルではなく、個別スキルの熟練度で成長
- **700スキルキャップ**: 全スキルの合計が700を超えると、使用頻度の低いスキルが減少
- **8サークル魔法システム**: 触媒を消費して魔法を詠唱
- **クラフト品質システム**: スキルに応じて作成物の品質が変化
- **多言語対応**: 日本語・英語をサポート

## スキル一覧

### 戦闘スキル
| スキル | 説明 |
|--------|------|
| Swordsmanship | 剣術 |
| Macefighting | 鈍器 |
| Fencing | フェンシング |
| Archery | 弓術 |
| Wrestling | 素手戦闘 |
| Parrying | 盾防御 |
| Tactics | 戦術（ダメージボーナス） |
| Anatomy | 解剖学（クリティカル率） |

### 魔法スキル
| スキル | 説明 |
|--------|------|
| Magery | 魔法詠唱 |
| Evaluating Intelligence | 魔法ダメージ評価 |
| Meditation | マナ回復 |
| Resisting Spells | 魔法抵抗 |

### クラフトスキル
| スキル | 説明 |
|--------|------|
| Blacksmithy | 鍛冶 |
| Tailoring | 裁縫 |
| Carpentry | 大工 |
| Cooking | 料理 |
| Alchemy | 錬金術 |
| Inscription | 写本（スクロール作成） |

### 採集スキル
| スキル | 説明 |
|--------|------|
| Mining | 採掘 |
| Lumberjacking | 伐採 |
| Fishing | 釣り |

### シーフスキル
| スキル | 説明 |
|--------|------|
| Hiding | 隠れる |
| Stealth | ステルス移動 |
| Snooping | 覗き見 |
| Stealing | 窃盗 |
| Poisoning | 毒塗り |
| Detecting Hidden | 隠れた者を探知 |

### テイミングスキル
| スキル | 説明 |
|--------|------|
| Animal Taming | 動物調教 |
| Animal Lore | 動物知識 |
| Veterinary | 動物治療 |

### サバイバルスキル
| スキル | 説明 |
|--------|------|
| Athletics | 運動（落下ダメージ軽減） |
| Swimming | 水泳（水中移動） |
| Heat Resistance | 耐熱 |

## 魔法システム

### スペルブック
魔法を使うにはスペルブックが必要です。スクロールを使用してスペルブックに魔法を記録します。

```
/spellbook create - 空のスペルブックを作成
/spellbook full - 全魔法入りスペルブックを作成（管理者用）
```

### 魔法一覧

#### 第1サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Create Food | 小麦 | 合成肉を生成 |
| Magic Arrow | 糸 | 魔法の矢を発射 |
| Night Sight | グロウストーンダスト | 暗視 |
| Heal | 金のニンジン | HP回復 |

#### 第2サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Feather Fall | 羽根 | 落下速度低下 |
| Harm | スパイダーアイ | 直接ダメージ |
| Cure | 金のニンジン, 毒じゃがいも | 状態異常解除 |

#### 第3サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Water Breathing | フグ | 水中呼吸 |
| Fireball | ガンパウダー, スパイダーアイ | 火球 |
| Teleport | エンダーパール | 短距離テレポート |
| Bless | ネザーウォート | 攻撃力UP |

#### 第4サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Lightning | ガンパウダー, ブレイズパウダー | 雷撃 |
| Fire Wall | ブレイズパウダー x2, スパイダーアイ | 炎の壁 |
| Greater Heal | 金のニンジン x2 | 大回復 |

#### 第5サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Mark | エンダーパール, ブレイズパウダー | ルーンに場所を記録 |
| Recall | エンダーパール | ルーンの場所へ移動 |
| Paralyze | スパイダーアイ, 糸 | 麻痺 |
| Protection | ネザーウォート, 糸 | 防御力UP |

#### 第6サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Explosion | ガンパウダー, ブレイズパウダー | 範囲爆発 |
| Invisibility | ネザーウォート, ブレイズパウダー | 透明化 |

#### 第7サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Gate Travel | エンダーパール x2, ブレイズパウダー | 双方向ゲートを開く |

#### 第8サークル
| 魔法 | 触媒 | 効果 |
|------|------|------|
| Meteor Swarm | ガンパウダー, ブレイズパウダー x3 | 大範囲ダメージ |

### ルーンとゲート

1. `/skilladmin give <player> rune` でルーンを入手
2. ルーンを手に持って `Mark` を詠唱 → 現在地を記録
3. `Recall` で記録した場所にテレポート
4. `Gate Travel` で双方向ポータルを開く（他のプレイヤーも通過可能）

## コマンド

### プレイヤーコマンド
| コマンド | 説明 |
|----------|------|
| `/skills` | スキル一覧を表示 |
| `/stats` | ステータスを表示 |
| `/cast <魔法名>` | 魔法を詠唱 |
| `/hide` | 隠れる |
| `/detect` | 隠れた者を探知 |
| `/snoop` | 覗き見モード |
| `/tame` | テイムモード |
| `/lore` | 動物情報を見る |
| `/language <en/ja>` | 言語を変更 |

### 管理者コマンド
| コマンド | 説明 |
|----------|------|
| `/skilladmin setskill <player> <skill> <value>` | スキル値を設定 |
| `/skilladmin give <player> spellbook` | スペルブックを付与 |
| `/skilladmin give <player> scroll <魔法名/all>` | スクロールを付与 |
| `/skilladmin give <player> rune` | ルーンを付与 |
| `/skilladmin give <player> reagents` | 触媒セットを付与 |

## インストール

1. [Releases](https://github.com/0x48lab/skills/releases)から最新のJARをダウンロード
2. `plugins/` フォルダに配置
3. サーバーを再起動

## 動作環境

- Minecraft 1.21+
- Paper/Spigot サーバー

## ビルド

```bash
./gradlew build
```

生成物: `build/libs/skills-1.0-SNAPSHOT-all.jar`

## ライセンス

MIT License

## クレジット

クラシックMMOのスキルシステムにインスパイアされています。
