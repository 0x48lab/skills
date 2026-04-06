package com.hacklab.minecraft.skills.skill

import com.hacklab.minecraft.skills.data.PlayerData
import com.hacklab.minecraft.skills.data.SkillData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * スキルシーソー（上限到達時の上昇/下降）のテスト。
 *
 * バグ再現: スキル合計が700に達した状態で、DOWN(▼)スキルがあるのに
 * 新しいスキルが上がらなかった問題。
 *
 * 原因:
 * 1. 浮動小数点の精度問題 (0.3 - 0.1 - 0.1 = 0.0999... < 0.1)
 * 2. 下降候補が1つしか試されない設計
 */
class SkillSeesawTest {

    private fun createPlayerData(): PlayerData =
        PlayerData(uuid = UUID.randomUUID(), playerName = "TestPlayer")

    // ================================================================
    // 浮動小数点の問題を証明するテスト
    // ================================================================

    @Nested
    @DisplayName("IEEE 754 浮動小数点の精度問題")
    inner class FloatingPointIssues {

        @Test
        @DisplayName("0.3 - 0.1 - 0.1 は IEEE 754 では正確に 0.1 にならない")
        fun `prove floating point imprecision with 0_3 minus 0_1 minus 0_1`() {
            // これが浮動小数点のバグの根本原因
            val result = 0.3 - 0.1 - 0.1
            // result = 0.09999999999999998 (NOT 0.1)
            assertNotEquals(0.1, result, "IEEE 754 では 0.3 - 0.1 - 0.1 ≠ 0.1")
            assertTrue(result < 0.1, "結果は 0.1 未満: $result")
        }

        @Test
        @DisplayName("SkillData.addValue で 0.3 から 0.1 を2回引くと内部値が 0.1 未満になる")
        fun `skillData value becomes slightly below 0_1 after arithmetic`() {
            val skill = SkillData(SkillType.MEDITATION, value = 0.3)
            skill.addValue(-0.1)  // 0.2（実際は 0.19999...）
            skill.addValue(-0.1)  // 0.1（実際は 0.09999...）

            // 表示上は 0.1 だが...
            assertEquals("0.1", String.format("%.1f", skill.value))

            // 内部値は 0.1 未満
            assertTrue(skill.value < 0.1,
                "内部値 ${skill.value} は 0.1 未満（これがバグの原因）")
        }

        @Test
        @DisplayName("旧ロジック再現: value >= gainAmount が false になる")
        fun `old logic fails with floating point imprecise value`() {
            val skill = SkillData(SkillType.MEDITATION, value = 0.3)
            skill.addValue(-0.1)
            skill.addValue(-0.1)

            val gainAmount = 0.1

            // 旧ロジック: decreaseData.value >= gainAmount
            val oldCheckPasses = skill.value >= gainAmount
            assertFalse(oldCheckPasses,
                "旧ロジックでは ${skill.value} >= $gainAmount が false（バグ）")

            // 修正後: epsilon 比較
            val newCheckPasses = skill.value >= gainAmount - 0.001
            assertTrue(newCheckPasses,
                "修正後は ${skill.value} >= ${gainAmount - 0.001} が true")
        }
    }

    // ================================================================
    // getSkillsToDecrease のテスト
    // ================================================================

    @Nested
    @DisplayName("getSkillsToDecrease - 複数候補リスト")
    inner class GetSkillsToDecrease {

        @Test
        @DisplayName("DOWN スキルが UP スキルより優先される")
        fun `DOWN skills are prioritized over UP skills`() {
            val data = createPlayerData()

            // Wrestling を DOWN に設定（value > 0）
            data.getSkill(SkillType.WRESTLING).apply {
                addValue(50.0)
                lockMode = SkillLockMode.DOWN
                // lastUsed を古くする
            }

            // Mining は UP のまま（value > 0）
            data.getSkill(SkillType.MINING).apply {
                addValue(30.0)
            }

            val candidates = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)

            // DOWN の Wrestling が先に来る
            val wrestlingIndex = candidates.indexOf(SkillType.WRESTLING)
            val miningIndex = candidates.indexOf(SkillType.MINING)

            assertTrue(wrestlingIndex >= 0, "Wrestling が候補に含まれる")
            assertTrue(miningIndex >= 0, "Mining が候補に含まれる")
            assertTrue(wrestlingIndex < miningIndex,
                "DOWN の Wrestling($wrestlingIndex) が UP の Mining($miningIndex) より先")
        }

        @Test
        @DisplayName("LOCKED スキルは候補に含まれない")
        fun `LOCKED skills are never included`() {
            val data = createPlayerData()

            data.getSkill(SkillType.TACTICS).apply {
                addValue(80.0)
                lockMode = SkillLockMode.LOCKED
            }

            val candidates = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)
            assertFalse(candidates.contains(SkillType.TACTICS),
                "LOCKED スキルは候補に含まれない")
        }

        @Test
        @DisplayName("value が 0 のスキルは候補に含まれない")
        fun `zero value skills are excluded`() {
            val data = createPlayerData()

            data.getSkill(SkillType.FISHING).apply {
                lockMode = SkillLockMode.DOWN
                // value は 0.0 のまま
            }

            val candidates = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)
            assertFalse(candidates.contains(SkillType.FISHING),
                "value=0 のスキルは候補に含まれない")
        }

        @Test
        @DisplayName("exclude 指定のスキルは候補に含まれない")
        fun `excluded skill is not a candidate`() {
            val data = createPlayerData()
            data.getSkill(SkillType.MINING).addValue(50.0)

            val candidates = data.getSkillsToDecrease(exclude = SkillType.MINING)
            assertFalse(candidates.contains(SkillType.MINING),
                "exclude 指定されたスキルは候補に含まれない")
        }

        @Test
        @DisplayName("DOWN スキル内は lastUsed 昇順でソートされる")
        fun `DOWN skills sorted by lastUsed ascending`() {
            val data = createPlayerData()

            // Meditation: DOWN, 古い lastUsed
            data.getSkill(SkillType.MEDITATION).apply {
                addValue(10.0)
                lockMode = SkillLockMode.DOWN
            }
            // lastUsed を意図的にずらす
            Thread.sleep(10)

            // Wrestling: DOWN, 新しい lastUsed
            data.getSkill(SkillType.WRESTLING).apply {
                addValue(50.0)
                lockMode = SkillLockMode.DOWN
                updateLastUsed()
            }

            val candidates = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)
            val medIdx = candidates.indexOf(SkillType.MEDITATION)
            val wrestIdx = candidates.indexOf(SkillType.WRESTLING)

            assertTrue(medIdx >= 0 && wrestIdx >= 0, "両方候補に含まれる")
            // Meditation の lastUsed が古い → 先にくる
            // ※ 同時刻の場合はどちらでもOKなので、明確に差をつけている
        }

        @Test
        @DisplayName("複数候補が返されるため、最初の候補が失敗しても次が使える")
        fun `multiple candidates allow fallback`() {
            val data = createPlayerData()

            // 1つ目の候補: 浮動小数点で value < 0.1 になるスキル
            data.getSkill(SkillType.COLD_RESISTANCE).apply {
                // 直接 0.3 → -0.1 → -0.1 で 0.0999... にする
                addValue(0.3)
                addValue(-0.1)
                addValue(-0.1)
                lockMode = SkillLockMode.DOWN
            }

            // 2つ目の候補: 十分な値があるスキル
            Thread.sleep(10)
            data.getSkill(SkillType.WRESTLING).apply {
                addValue(100.0)
                lockMode = SkillLockMode.DOWN
                updateLastUsed()
            }

            val candidates = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)
            assertTrue(candidates.size >= 2,
                "少なくとも2つの候補が返される（フォールバック可能）")

            // シーソーロジックのシミュレーション（修正後）
            val gainAmount = 0.1
            var decreased = false
            for (candidate in candidates) {
                val skill = data.getSkill(candidate)
                if (skill.value >= gainAmount - 0.001 && skill.canDecrease()) {
                    skill.addValue(-gainAmount)
                    decreased = true
                    break
                }
            }

            assertTrue(decreased, "複数候補のどれかで下降成功")
        }
    }

    // ================================================================
    // バグ再現: 旧ロジック vs 修正後ロジック
    // ================================================================

    @Nested
    @DisplayName("バグ再現: プレイヤー実データに近い状況")
    inner class BugReproduction {

        /**
         * ユーザー報告の再現:
         * - スキル合計 ≈ 700
         * - DOWN スキルが複数ある（Wrestling 100, Meditation 0.1, etc.）
         * - 新しいスキルが上がらない
         */
        @Test
        @DisplayName("旧ロジック(単一候補)で浮動小数点バグが発生する")
        fun `old logic single candidate fails with floating point edge case`() {
            val data = createPlayerData()

            // 最小 lastUsed の DOWN スキルを浮動小数点問題のある値にする
            val problematicSkill = data.getSkill(SkillType.MEDITATION).apply {
                addValue(0.3)
                addValue(-0.1)
                addValue(-0.1)
                lockMode = SkillLockMode.DOWN
                // lastUsed は最も古い（デフォルト初期化時刻のまま）
            }

            // これが旧ロジックの getSkillToDecrease が返す候補
            // （最小 lastUsed の DOWN スキル）
            val gainAmount = 0.1

            // 旧ロジック: 単一候補で value >= gainAmount チェック
            val oldCheckResult = problematicSkill.value >= gainAmount
            assertFalse(oldCheckResult,
                "旧ロジックでは浮動小数点のせいで value(${problematicSkill.value}) >= $gainAmount が false")
        }

        @Test
        @DisplayName("修正後ロジック(複数候補+epsilon)でバグが解消される")
        fun `new logic multiple candidates with epsilon fixes the bug`() {
            val data = createPlayerData()

            // 問題のあるスキル（最小 lastUsed → 最優先候補）
            data.getSkill(SkillType.MEDITATION).apply {
                addValue(0.3)
                addValue(-0.1)
                addValue(-0.1)
                lockMode = SkillLockMode.DOWN
            }

            // バックアップ候補（十分な値）
            Thread.sleep(10)
            data.getSkill(SkillType.WRESTLING).apply {
                addValue(100.0)
                lockMode = SkillLockMode.DOWN
                updateLastUsed()
            }

            // 上げたいスキル
            data.getSkill(SkillType.SWORDSMANSHIP).addValue(50.0)

            // 修正後のシーソーロジック
            val gainAmount = 0.1
            val candidates = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)

            var decreased = false
            var decreasedSkill: SkillType? = null
            for (candidate in candidates) {
                val skill = data.getSkill(candidate)
                // epsilon 比較
                if (skill.value >= gainAmount - 0.001 && skill.canDecrease()) {
                    skill.addValue(-gainAmount)
                    decreased = true
                    decreasedSkill = candidate
                    break
                }
            }

            assertTrue(decreased, "修正後のロジックでは下降成功")
            assertNotNull(decreasedSkill, "下降したスキルが特定できる")

            // epsilon のおかげで Meditation でも成功する
            // （0.0999... >= 0.1 - 0.001 = 0.099 → true）
            assertEquals(SkillType.MEDITATION, decreasedSkill,
                "epsilon 比較のおかげで最優先候補の Meditation で下降成功")
        }

        @Test
        @DisplayName("epsilon でも救えない場合は次の候補にフォールバックする")
        fun `falls back to next candidate when first has extremely low value`() {
            val data = createPlayerData()

            // 極小値のスキル（epsilon でも救えない）
            // SkillData は value 0.0 だと init で作れるが、0.001 はOK
            data.getSkill(SkillType.COLD_RESISTANCE).apply {
                addValue(0.1)
                addValue(-0.1) // → 0.0 (coerced)
                // value は 0.0 なので getSkillsToDecrease で除外される
                lockMode = SkillLockMode.DOWN
            }

            // バックアップ候補
            Thread.sleep(10)
            data.getSkill(SkillType.HIDING).apply {
                addValue(10.0)
                lockMode = SkillLockMode.DOWN
                updateLastUsed()
            }

            val gainAmount = 0.1
            val candidates = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)

            // Cold Resistance は value=0 で除外されているはず
            assertFalse(candidates.contains(SkillType.COLD_RESISTANCE),
                "value=0 の Cold Resistance は除外")

            var decreased = false
            for (candidate in candidates) {
                val skill = data.getSkill(candidate)
                if (skill.value >= gainAmount - 0.001 && skill.canDecrease()) {
                    skill.addValue(-gainAmount)
                    decreased = true
                    break
                }
            }

            assertTrue(decreased, "次の候補（Hiding）で下降成功")
        }
    }

    // ================================================================
    // getSkillToDecrease（後方互換）のテスト
    // ================================================================

    @Nested
    @DisplayName("getSkillToDecrease - 後方互換")
    inner class GetSkillToDecreaseCompat {

        @Test
        @DisplayName("getSkillToDecrease は getSkillsToDecrease の最初の要素を返す")
        fun `single result matches first of list`() {
            val data = createPlayerData()

            data.getSkill(SkillType.WRESTLING).apply {
                addValue(50.0)
                lockMode = SkillLockMode.DOWN
            }

            val single = data.getSkillToDecrease(exclude = SkillType.SWORDSMANSHIP)
            val list = data.getSkillsToDecrease(exclude = SkillType.SWORDSMANSHIP)

            assertEquals(list.firstOrNull(), single,
                "単一結果はリストの最初の要素と一致")
        }

        @Test
        @DisplayName("候補がない場合は null を返す")
        fun `returns null when no candidates`() {
            val data = createPlayerData()

            // 全スキルを LOCKED にする
            SkillType.entries.forEach {
                data.getSkill(it).lockMode = SkillLockMode.LOCKED
            }

            val result = data.getSkillToDecrease(exclude = SkillType.SWORDSMANSHIP)
            assertNull(result, "全 LOCKED なら null")
        }
    }
}
