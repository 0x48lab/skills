package com.hacklab.minecraft.skills.vengeful

/**
 * VengefulMobs風の攻撃モード
 * 受動的Mobがどのように反応するかを定義
 */
enum class AggressionMode {
    /**
     * 攻撃されたら反撃し続ける
     */
    RETALIATE,

    /**
     * 攻撃されたら1回だけ反撃（誤クリック対策）
     */
    RETALIATE_ONCE,

    /**
     * 攻撃されたら周囲の同種Mobも一緒に反撃
     */
    RETALIATE_WITH_SUPPORT,

    /**
     * 常にプレイヤーを攻撃（敵対的）
     */
    HOSTILE,

    /**
     * 全てのエンティティを攻撃
     */
    MURDER_ALL,

    /**
     * 同種以外のエンティティを攻撃
     */
    MURDER_OTHERS;

    companion object {
        fun fromString(value: String): AggressionMode? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
