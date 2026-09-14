---
marp: true
theme: default
paginate: true
header: "総合テクニカルハンドアウト"
footer: "© 2026 Hideaki YANAGISAWA, Dept. of Computer Science and Electronic Engineering, NIT, Tokuyama College"
author: "Hideaki YANAGISAWA"
size: 210mm x 297mm
style: |
  section {
    font-family: 'Helvetica Neue', Arial, 'Hiragino Kaku Gothic ProN', sans-serif;
    font-size: 14px;
    line-height: 1.5;
    padding: 28px 36px;
    justify-content: flex-start;
  }
  h1 { color: #1e3a8a; font-size: 22px; margin-bottom: 6px; border-bottom: 2px solid #1e3a8a; padding-bottom: 4px; }
  h2 { color: #1e40af; font-size: 17px; border-bottom: 1.5px solid #3b82f6; margin-top: 14px; margin-bottom: 6px; padding-bottom: 2px; }
  h3 { color: #2563eb; font-size: 14px; margin-top: 8px; margin-bottom: 3px; }
  pre { font-size: 12px; margin: 4px 0; padding: 6px 10px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 4px; }
  .box { background: #eff6ff; border-left: 4px solid #3b82f6; padding: 5px 10px; margin: 6px 0; font-size: 13px; }
  table { font-size: 12.5px; border-collapse: collapse; margin: 6px 0; width: 100%; }
  th, td { border: 1px solid #cbd5e1; padding: 3px 6px; }
  th { background: #f1f5f9; color: #1e293b; }
---

# Computer System Laboratory テクニカルハンドアウト (Handout)

- **授業名**: 「Computer System Laboratory」全8回シリーズ
- **資料作成者**: Hideaki YANAGISAWA
- **対象読者**: 受講者全員（講義中および90分演習時の手元参照用リファレンス）
- **時間設定**: 各回 講義 60分 + 演習 90分

---

## 1. 信号型・演算子クイックリファレンス (第1回〜第2回)

| 構文 | 意味 | 物理回路 | 記述例 |
| :--- | :--- | :--- | :--- |
| `Bool()` | 1ビット二値信号 | 1本の配線 | `val en = Input(Bool())` |
| `UInt(w.W)` | $w$ ビット符号なし整数 | $w$ 本のバス | `val data = Wire(UInt(8.W))` |
| `SInt(w.W)` | $w$ ビット2の補数整数 | $w$ 本のバス | `val s = Wire(SInt(16.W))` |
| `+&` | 拡張加算（キャリー保持） | $w+1$ ビット加算器 | `val sumExt = a +& b` |
| `-&` | 拡張減算（ボロー保持） | $w+1$ ビット減算器 | `val subExt = a -& b` |
| `Cat(hi, lo)` / `hi ## lo` | バス結合（上位/下位） | 物理配線バンドル | `val word = highNibble ## lowNibble` |
| `x(hi, lo)` | ビットスライス抽出 | バスから部分配線 | `val slice = x(7, 4)` |
| `x.orR` | 縮約OR (Reduction OR) | ORツリー回路 | `val anyBit = x.orR` (非ゼロ判定) |
| `Mux(c, t, f)` | 2-to-1 マルチプレクサ | 物理MUXゲート | `val res = Mux(sel, in1, in0)` |
| `WireDefault(v)` | デフォルト接続付き配線 | **透過ラッチ完全防止** | `val out = WireDefault(0.U(8.W))` |
| `switch / is` | 直交デコーダ | 並列平衡デコーダ | `switch(op) { is(0.U) { ... } }` |

---

## 2. 順序回路と特殊カウンタ構文 (第3回〜第4回)

| 回路要素 | Chisel記述 | ハードウェア動作特性 |
| :--- | :--- | :--- |
| **初期値付きD-FF** | `val r = RegInit(0.U(8.W))` | 暗黙の同期リセットで初期化 |
| **パイプライン段** | `val pipe = RegNext(inSignal, 0.U)` | 1クロックサイクルの同期遅延 |
| **モジュロNラップ** | `when(cnt === (n-1).U) { cnt := 0.U }` | 終端検出による巡回リセット |
| **Ringカウンタ循環** | `Cat(state(w - 2, 0), state(w - 1))` | 1-hot左循環シフト |
| **Johnson反転帰還** | `Cat(state(w - 2, 0), ~state(w - 1))` | 最上位ビットの反転を最下位へ帰還 ($2N$ 状態) |
| **1-hot自己復帰判定** | `PopCount(state) === 1.U` | 立っているビット数が1以外の時に初期値へ強制復帰 |
| **グレイコード変換** | `val gray = bin ^ (bin >> 1)` | 隣接遷移で常に1ビットのみ反転する安全符号 |

---

## 3. 構造化設計と同期カスケード (第5回〜第6回)

### サブモジュールのインスタンス化と配線
```scala
class TopModule extends Module {
  val subA = Module(new SubModuleA)
  val subB = Module(new SubModuleB)

  // 1. 親の入力 -> 子の入力
  subA.io.in := io.topIn

  // 2. 子 -> 子 の完全同期カスケード (cout -> en)
  subB.io.en := io.en && subA.io.rollover

  // 3. 子の出力 -> 親の出力
  io.topOut := subB.io.out
}
```

### 同期カスケードの絶対原則
- **非同期リップルクロックの厳禁**: 下位モジュールの `cout` を上位モジュールの `clock` 端子に接続してはならない。
- すべてのモジュールに同一のシステムクロックを供給し、`cout` を上位の `en` 端子に接続する。

---

## 4. FSMとメモリ回路の構文 (第7回〜第8回)

### 有限状態機械 (`ChiselEnum` + Moore型)
```scala
object State extends ChiselEnum {
  val sIDLE, sACTIVE, sDONE = Value
}

val stateReg = RegInit(State.sIDLE)

// 次状態遷移論理
switch(stateReg) {
  is(State.sIDLE)   { when(io.start) { stateReg := State.sACTIVE } }
  is(State.sACTIVE) { when(io.finish) { stateReg := State.sDONE } }
  is(State.sDONE)   { stateReg := State.sIDLE }
}

// Moore型出力 (状態レジスタのみから生成 = グリッチフリー)
io.busy := stateReg === State.sACTIVE
```

### 非同期読み出しメモリ & RAWフォワーディング (`Mem`)
```scala
val mem = Mem(8, UInt(8.W))

// クロック同期書き込み
when(io.wen) { mem(io.waddr) := io.wdata }

// 非同期読み出し (同一サイクル無遅延)
val rawData = mem(io.raddr)

// RAWフォワーディング回路 (書き込み中の最新データを即時バイパス)
val bypass = io.wen && (io.waddr === io.raddr)
io.rdata := Mux(bypass, io.wdata, rawData)
```

---

## 5. 演習課題一覧とチェックリスト (90分演習)

| 回 | 基本問題（必須 / 45分） | 発展問題（推奨 / 35分） | 重点チェック項目 |
| :-: | :--- | :--- | :--- |
| **第1回** | `HalfAdder`, `FullAdder` | `AdderWithParity4` (加算・パリティ) | 拡張加算 `+&`、縮約XOR `xorR` |
| **第2回** | `Mux4to1`, `SevenSegDecoder` | `PriorityEncoder4` (`valid` 出力) | `WireDefault`、`io.in.orR` |
| **第3回** | `ModuloNCounter` (クリア/ロード/en) | `PwmGenerator` (周期/Duty比較) | $N-1$ ラップ、優先度 `clear > load > en` |
| **第4回** | `SelfCorrectingRingCounter`, `JohnsonCounter` | `GrayCodeCounter` (ハミング距離1) | `PopCount(state) === 1.U` 自己復帰 |
| **第5回** | `StructuredALU` (算術/論理統合) | `PipelinedALUWithFlags` (パイプライン) | `Module(new ...)`、`RegNext` 遅延同期 |
| **第6回** | `Counter60` (10進×6進カスケード) | `DigitalClockCore` (24時間時計) | **リップルクロック厳禁**、`cout` $\to$ `en` |
| **第7回** | `SequenceDetector101` (Moore型) | `TrafficLightController` (タイマ連動) | `ChiselEnum`、重複 "10101" 検出 |
| **第8回** | `RegisterFile8x8` (2R1W 非同期) | `BypassedRegisterFile` (RAWバイパス) | 同一サイクルでの `waddr === raddr` バイパス |
