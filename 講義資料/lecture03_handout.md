---
marp: true
theme: default
paginate: true
header: "第3回 順序回路と基本カウンタ"
footer: "© 2026 Hideaki YANAGISAWA, Dept. of Computer Science and Electronic Engineering, NIT, Tokuyama College"
author: "Hideaki YANAGISAWA"
size: 210mm x 297mm
style: |
  section {
    font-family: 'Helvetica Neue', Arial, 'Hiragino Kaku Gothic ProN', sans-serif;
    font-size: 15px;
    line-height: 1.6;
    padding: 30px 40px;
    justify-content: flex-start;
  }
  h1 { color: #1e3a8a; font-size: 24px; margin-bottom: 8px; border-bottom: 2px solid #1e3a8a; padding-bottom: 4px; }
  h2 { color: #1e40af; font-size: 18px; border-bottom: 1.5px solid #3b82f6; margin-top: 16px; margin-bottom: 8px; padding-bottom: 2px; }
  h3 { color: #2563eb; font-size: 15px; margin-top: 10px; margin-bottom: 4px; }
  pre { font-size: 12.5px; margin: 6px 0; padding: 8px 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 4px; }
  .box { background: #eff6ff; border-left: 4px solid #3b82f6; padding: 6px 12px; margin: 8px 0; font-size: 13.5px; }
  table { font-size: 13px; border-collapse: collapse; margin: 8px 0; width: 100%; }
  th, td { border: 1px solid #cbd5e1; padding: 4px 8px; }
  th { background: #f1f5f9; color: #1e293b; }
---

# 第3回: 順序回路と基本カウンタ 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. クロック同期順序回路の工学的原則

1. **同期設計の基礎 (Synchronous Design)**:
   - 全ての記憶素子（D型フリップフロップ: D-FF）が共通の単一クロック信号のエッジ（立ち上がり）で一斉に状態を更新する設計手法。
   - 組合せ回路で発生する過渡的なヒゲ状ノイズ（グリッチ）が、次のクロック立ち上がりまでに整定（セトリング）することで吸収され、誤動作を完全に排除できる。

2. **タイミング制約 (Setup / Hold Time)**:
   - **セットアップ時間 ($t_{setup}$)**: クロック立ち上がり前にデータ入力が安定していなければならない最小時間。
   - **ホールド時間 ($t_{hold}$)**: クロック立ち上がり後にデータ入力が保持されていなければならない最小時間。
   - 最大遅延制約: $T_{clk} \ge t_{c-q} + t_{comb(max)} + t_{setup}$

3. **同期リセットの優位性**:
   - クロックのエッジに同期して既知の初期状態へ遷移するリセット方式。
   - リセット解除時のメタステーブル（準安定状態）リスクがなく、静的タイミング解析 (STA) が極めて容易。

---

## 2. Chisel レジスタ構文リファレンス

| 構文 | 説明 | ハードウェア表現 |
| :--- | :--- | :--- |
| `Reg(UInt(w.W))` | 初期値なしのレジスタ | リセット端子のないD-FF |
| `RegInit(initVal)` | **同期リセット付きレジスタ（推奨）** | リセット時に指定値で初期化されるD-FF |
| `RegNext(nextVal)` | 1サイクル遅延レジスタ | 入力信号を1クロック遅延させるD-FF |
| `RegEnable(nextVal, en)` | イネーブル付きレジスタ | `en` が `true.B` の時のみ更新されるD-FF |

### 同期カウンタの基本コード例
```scala
val count = RegInit(0.U(4.W)) // 初期値0で同期リセット
when(io.en) {
  count := count + 1.U        // イネーブル時のみ毎クロックインクリメント
}
```

---

## 3. 任意進数カウンタのアーキテクチャ設計

### 制御入力の優先度仕様 (`clear` > `en`)
産業用制御回路では、複数の制御信号が同時にアサートされた際の挙動を明確に階層化（優先度付け）します。
$$\text{clear (最優先: 0強制リセット)} > \text{en (通常動作: インクリメント/保持)}$$

複雑な `load` 信号を排し、安全最優先の同期クリア（`clear`）と通常カウント許可（`en`）の2段階に絞り込むことで、マルチプレクサの多段接続を解消し、クリティカルパスの短縮と堅牢な動作を両立させます。

```scala
class ModuloNCounter(val n: Int = 10, val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())         // カウントイネーブル (1Hzパルス等を接続)
    val clear    = Input(Bool())         // 同期クリア (最優先で 0 にリセット)
    val count    = Output(UInt(width.W)) // 現在のカウント値出力
    val rollover = Output(Bool())        // 周期満了パルス (count == n-1 かつ en 有効時)
  })

  val cntReg = RegInit(0.U(width.W))

  // 1. カウンタ更新論理 (clear > en の優先度)
  when(io.clear) {
    cntReg := 0.U
  }.elsewhen(io.en) {
    when(cntReg === (n - 1).U) {
      cntReg := 0.U
    }.otherwise {
      cntReg := cntReg + 1.U
    }
  }

  // 2. 出力論理 (カウンタ更新論理と明確に分離)
  io.count    := cntReg
  io.rollover := io.en && (cntReg === (n - 1).U)
}
```

### 1Hzパルスとの連携による1秒時計カウンタの実現
`OneHzGenerator` で生成した 1クロック幅のストローブパルス `oneHzPulse` を `ModuloNCounter` の `io.en` 入力に接続します。
- **完全同期設計**: 両モジュールは同一の9MHzシステムクロックで駆動されます。
- **リップルクロックの排除**: 非同期クロックを一切使用せず、単一クロック領域内でストローブパルスを同期イネーブルとして渡すことで、クロックスキューやメタスタビリティの懸念を完全に排除します。
- **1秒ごとの動作**: 普段は `en` が Low（カウント保持）。1秒に1回だけ `oneHzPulse` が High になる瞬間に、カウント値が $0 \to 1 \to \dots \to n-1 \to 0$ と 1 つ進みます。

### 桁上げパルス (`rollover`) の生成
- `rollover` は、カウンタが最大値 $n-1$ に達し、かつ `io.en` が有効な時に**1クロック幅だけ `true.B`** を出力します。
- レジスタ段を挟まない組合せ論理として生成することで、次段カウンタの同期イネーブル信号として即座に伝達できます（第5回で扱う多段カウンタ同期カスケードのキー信号）。

---

## 4. 実機応用: 9MHz クロック分周による 1Hz 生成と 0.5秒 LED 点滅

FPGA実機（例: 9MHzクロック）において、人間の目で認識できる1秒周期の動作を作るには、カウンタによるクロック分周を行います。

```scala
val halfPeriod = clkFreq / 2 // 4,500,000 (0.5秒)
val fullPeriod = clkFreq     // 9,000,000 (1.0秒)
val cntWidth   = log2Ceil(fullPeriod) // 24ビット
```

- **0.5秒 LED 点滅 (`ledBlink`)**: `cntReg` が半周期および全周期に達するごとに `ledReg` を反転（トグル）し、0.5秒点灯 / 0.5秒消灯の1Hz点滅を生成。
- **同期1秒パルス (`oneHzPulse`)**: 全周期満了時に1クロック幅のみ `true.B` をアサートし、後段の秒カウンタの同期イネーブル信号として利用。

```scala
// カウンタとLED点滅レジスタを独立して記述する設計パターン
when(io.clear) {
  cntReg := 0.U
}.elsewhen(io.en) {
  when(cntReg === (fullPeriod - 1).U) { cntReg := 0.U }
  .otherwise { cntReg := cntReg + 1.U }
}

when(io.clear) {
  ledReg := false.B
}.elsewhen(io.en) {
  when(cntReg === (halfPeriod - 1).U || cntReg === (fullPeriod - 1).U) {
    ledReg := !ledReg
  }
}
```

---

## 5. デューティ比可変 PWM パルス発生器の原理

パルス幅変調 (PWM: Pulse Width Modulation) は、一定周期の中でハイレベルの期間比率（デューティ比）をデジタル制御する技術です。

```
クロック    __|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__
周期 cnt    | 0 | 1 | 2 | 3 | 0 | 1 | 2 | 3 | 0 | 1 | ...
duty = 2    |------ < duty ------|
pwmOut     ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾________________‾‾‾‾‾‾‾‾‾... (50%)
```

- **周期カウンタ**: `cnt` が `0` から `period` までカウントし、満了時に `0` に復帰。
- **比較器**: `cnt < duty` の時に `pwmOut := true.B`、それ以外は `false.B`。
- `en === false.B` の時はカウンタを停止し、出力は安全のため `false.B` を維持。

---

## 6. 本日の演習課題ガイドライン (90分)

演習コードは `Lab03/` 配下の各課題ディレクトリに分かれて配置されています。

### 基本問題 (必須・目安50分 / 配点70点)
1. **`OneHzGenerator(clkFreq: Int = 9000000)`** (配点35点 / 目安25分)
   - パラメータ化されたクロック周波数分周カウンタ（デフォルト9MHz）。
   - 0.5秒ごとに `ledBlink` を反転させるトグル動作。
   - 1秒（全周期満了）時に 1クロック幅の `oneHzPulse` パルス出力。
   - カウンタ記述とLEDレジスタ記述の独立化。
2. **`ModuloNCounter(n: Int, width: Int)`** (配点35点 / 目安25分)
   - 1秒ごとにカウントアップする同期クリア付きモジュロ $n$ カウンタの設計。
   - `clear` (最優先リセット) > `en` (カウント動作) の優先度制御。
   - カウンタ更新論理と出力論理（`count`, `rollover`）の明確な分離。
   - 最大値 $n-1$ かつ `en` 有効時における 1クロック幅 `rollover` パルスの出力。

### 確認課題 (目安30分 / 配点30点)
3. **`PwmGenerator(periodMax: Int = 255)`** (配点30点)
   - 内部周期カウンタと比較器による PWM パルス発生器。
   - `duty = 0` (0%デューティ), `duty > period` (100%デューティ) を含む境界条件の正確な処理。

### 採点ポイント
- 同期リセット (`RegInit`) が正しく適用されているか (20点)
- 9MHz 分周カウンタの半周期/全周期判定と LED 反転・パルス生成が正確か (25点)
- `clear > en` の優先度制御とカウンタ更新/出力論理の分離が正確か (25点)
- `rollover` パルスが最大値かつイネーブル時のみ1クロック出力されるか (15点)
- PWM のデューティ比比較とイネーブル停止処理が正確か (15点)
