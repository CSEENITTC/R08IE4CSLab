---
marp: true
theme: default
paginate: true
header: "第5回 60進カウンタの構造化設計"
footer: "© 2026 Hideaki YANAGISAWA, Dept. of Computer Science and Electronic Engineering, NIT, Tokuyama College"
author: "Hideaki YANAGISAWA"
size: 210mm x 297mm
style: |
  section {
    font-family: 'Helvetica Neue', Arial, 'Hiragino Kaku Gothic ProN', sans-serif;
    font-size: 14.5px;
    line-height: 1.55;
    padding: 28px 38px;
    justify-content: flex-start;
  }
  h1 { color: #1e3a8a; font-size: 23px; margin-bottom: 6px; border-bottom: 2px solid #1e3a8a; padding-bottom: 4px; }
  h2 { color: #1e40af; font-size: 17.5px; border-bottom: 1.5px solid #3b82f6; margin-top: 14px; margin-bottom: 6px; padding-bottom: 2px; }
  h3 { color: #2563eb; font-size: 14.5px; margin-top: 8px; margin-bottom: 4px; }
  pre { font-size: 12px; margin: 6px 0; padding: 8px 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 4px; }
  .box { background: #eff6ff; border-left: 4px solid #3b82f6; padding: 6px 12px; margin: 6px 0; font-size: 13px; }
  table { font-size: 12.5px; border-collapse: collapse; margin: 6px 0; width: 100%; }
  th, td { border: 1px solid #cbd5e1; padding: 4px 8px; }
  th { background: #f1f5f9; color: #1e293b; }
---

# 第5回: 60進カウンタの構造化設計 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. 階層化設計 (Hierarchical Design) の基礎とChisel記述作法

大規模なデジタル回路を1つの `Module` 内にフラットに記述すると、信号線の衝突や制御条件の複雑化により保守・検証が困難になります。機能ごとに独立した**サブモジュール（部品）**へ分割し、**トップモジュール（親）**でそれらをインスタンス化して結線する手法を**階層化設計**と呼びます。

### Chiselにおける階層化の3ステップ
1. **サブモジュールの定義**: `class SubModule(...) extends Module` として部品を定義する。
2. **インスタンス化（実体化）**: 親モジュール内で `val u = Module(new SubModule(...))` を呼び出す。
3. **ポート間の結線 (`:=`)**: `u.io. inputs := ...` で入力を与え、`... := u.io.outputs` で出力を受け取る（※ `clock` と `reset` はChiselが自動的に全サブモジュールへ同一の信号を配線します）。

---

## 2. カスケード接続における工学的原則（非同期リップルクロックの禁止）

1. **【最重要・厳禁】非同期リップルクロック (Ripple Clock) の追放**:
   - 前段（分周器や下位カウンタ）の出力を後段のクロック入力に繋ぐリップルクロックは、クロック遅延（スキュー）の累積やグリッチによる誤カウントを引き起こすため厳禁です。
2. **工学的標準: 単一クロックと完全同期イネーブル伝播 (`oneHzPulse` / `rollover` $\to$ `en`)**:
   - 全てのサブモジュールは**同一の単一システムクロック**で駆動します。
   - 1Hzパルスや桁上げパルス（1クロック幅のストローブ信号）を、次段サブモジュールの **`en`（イネーブル）端子**に接続することで、全ビットが同一クロックエッジで完全に同期して遷移します。

```
【同期イネーブル方式（工学的標準）】
clk -------------------------------------------------------------------> 全サブモジュール共通 (Single Clock)
io.en ---> [ OneHzGenerator ] --(oneHzPulse)--> en [ 10進カウンタ ] --(rollover)--> en [ 6進カウンタ ] --(rollover)--> io.cout
```

---

## 3. 60進カウンタ (`Counter60`) の3モジュール階層アーキテクチャ

`Counter60` は、以下の**3つのサブモジュール**を階層化して構成します：

1. **1Hzの`en`信号を出すモジュール (`OneHzGenerator(clkFreq)`)**:
   - システムクロックをカウントし、1秒に1回、1クロック幅だけ `true.B` になる `oneHzPulse` を出力。
2. **10進カウンタ (`ModuloNCounter(10, 4)`) — 1の位 (`secUnits`: 0〜9)**:
   - `u10.io.en := oneHz.io.oneHzPulse` を接続。1秒ごとにカウントアップし、値が `9` かつ `oneHzPulse` が High の1クロックサイクルだけ `u10.io.rollover` を High にする。
3. **6進カウンタ (`ModuloNCounter(6, 3)`) — 10の位 (`secTens`: 0〜5)**:
   - `u6.io.en := u10.io.rollover` を接続。1の位が 9 $\to$ 0 に戻る瞬間のクロックエッジでのみカウントアップする。
   - 59秒の最後の1クロックサイクルで `u6.io.rollover` が High になり、これが `io.cout` となる。

```scala
class Counter60(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val secUnits = Output(UInt(4.W)) // 1の位 (0〜9)
    val secTens  = Output(UInt(3.W)) // 10の位 (0〜5)
    val cout     = Output(Bool())    // 59秒完了時の1クロックパルス
  })
  val oneHz = Module(new OneHzGenerator(clkFreq))
  val u10   = Module(new ModuloNCounter(10, 4))
  val u6    = Module(new ModuloNCounter(6, 3))

  oneHz.io.en := io.en;              oneHz.io.clear := io.clear
  u10.io.en   := oneHz.io.oneHzPulse; u10.io.clear  := io.clear
  u6.io.en    := u10.io.rollover;     u6.io.clear   := io.clear

  io.secUnits := u10.io.count
  io.secTens  := u6.io.count
  io.cout     := u6.io.rollover
}
```

---

## 4. 本日の演習課題仕様とガイドライン (90分)

### 基本問題 (必須・目安45分 / 配点70点): 階層化 60進カウンタ (`Counter60`)
- **サブモジュール構成**: `OneHzGenerator(clkFreq)`、`ModuloNCounter(10, 4)`、`ModuloNCounter(6, 3)` の3モジュール構成
- **入出力**: 入力 `en`, `clear` / 出力 `secUnits` (4bit), `secTens` (3bit), `cout` (Bool)
- **検証**: テストベンチでは `new Counter60(clkFreq = 4)` として分周比を縮小し、00秒〜59秒の全遷移と `cout` を検証します。

### 確認課題 (推奨・目安35分 / 配点30点): 24時間デジタル時計コア (`DigitalClockCore`)
- **構成**: 秒(60進)、分(60進)、時(24進)の3段同期カスケード接続。23:59:59 $\to$ 00:00:00 の完全同期遷移を検証。
