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
$$\text{clear (最優先: 0強制リセット)} > \text{modEn (通常動作: 1秒毎インクリメント/保持)}$$

複雑な `load` 信号を排し、安全最優先の同期クリア（`clear`）と通常カウント許可（`en`）の2段階に絞り込むことで、マルチプレクサの多段接続を解消し、クリティカルパスの短縮と堅牢な動作を両立させます。

```scala
class ModuloNCounter(val n: Int = 10, val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())         // カウントイネーブル
    val clear    = Input(Bool())         // 同期クリア (最優先で 0 にリセット)
    val count    = Output(UInt(width.W)) // カウント出力 (実機LED向け負論理)
    val runLED   = Output(Bool())        // 動作確認用LED (0.5秒反転、負論理)
  })

  // 1. 9MHz分周カウンタ更新論理（0 〜 fullPeriod-1 を周期カウント）
  when(io.clear) { cntReg := 0.U }.elsewhen(io.en) {
    when(cntReg === (clkFreq - 1).U) { cntReg := 0.U }.otherwise { cntReg := cntReg + 1.U }
  }
  modEn := io.en && (cntReg === (fullPeriod - 1).U) // 1秒ストローブ

  // 2. モジュロN更新論理 (clear > modEn の優先度で 0 〜 n-1 をカウント)
  when(io.clear) { modReg := 0.U }.elsewhen(modEn) {
    when(modReg === (n - 1).U) { modReg := 0.U }.otherwise { modReg := modReg + 1.U }
  }

  // 3. LED点滅更新論理 & 4. 出力論理（負論理で実機LEDへ出力）
  io.runLED := ~ledReg
  io.count  := ~modReg
}
```

### 1秒ストローブパルスと完全同期設計
- 回路全体が単一の 9MHz システムクロックで駆動されます。
- リップルクロックを完全排除し、分周カウンタ `cntReg` が満了した瞬間の 1 クロック幅パルス `modEn` をトリガーとしてモジュロレジスタ `modReg` を更新します。
- ボード上のアクティブLow LEDに合わせて、出力段で反転（`~`）を適用します。

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

## 5. デューティ比可変 PWM パルス発生器 (`PwmGenerator`) の原理と入出力仕様

パルス幅変調 (PWM: Pulse Width Modulation) は、一定周期の中で出力を High (ON) にする期間比率（デューティ比）をデジタル制御し、LEDの明るさ調整やモータの速度制御を行う技術です。

### 入出力ポートとパラメータの意味
| 信号・引数名 | 型・ビット幅 | 意味・役割 | 具体例 (`period = 4`, `duty = 2`) |
| :--- | :--- | :--- | :--- |
| `periodMax` | `Int` (引数) | カウンタのハードウェア最大上限値（デフォルト `255`） | `cntReg >= periodMax.U` でも `0` に戻す安全上限 |
| `io.period` | `Input(UInt(8.W))` | **周期カウンタの上限値**。`cntReg` は `0` 〜 `period` を巡回するため、**1周期は `period + 1` クロック**となる | `period = 4` $\implies$ `0, 1, 2, 3, 4` の **5クロック周期** |
| `io.duty` | `Input(UInt(8.W))` | **1周期内で High を出力するクロック数（比較閾値）**。`cntReg < duty` の期間だけ High になる | `duty = 2` $\implies$ `cntReg = 0, 1` の **2クロック間 High**（デューティ比 $2/5 = 40\%$） |
| `io.en` | `Input(Bool())` | **動作イネーブル**。`true.B` でカウント＆PWM出力、`false.B` でカウンタ0リセット＆出力 Low 固定 | `en = false.B` のときは即座に `pwmOut = false.B` |
| `io.pwmOut` | `Output(Bool())` | **PWMパルス出力信号** | `io.en && (cntReg < io.duty) && (io.duty > 0.U)` |

```
【動作波形例: period = 4 (1周期 = 5クロック), duty = 2 (2クロック間 High) の場合】
クロック    __|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__|‾|__
cntReg      | 0 | 1 | 2 | 3 | 4 | 0 | 1 | 2 | 3 | 4 | 0 | 1 | ...  (0〜period の 5クロックで1周期)
cnt < duty  |<-- High (2clk) -->|<--- Low (3clk) --->|<-- High ...
pwmOut     ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾_____________________‾‾‾‾‾‾‾‾‾...  (デューティ比 = 2 / (4 + 1) = 40%)
```

- **周期カウンタ (`cntReg`)**: `io.en` が `true.B` のとき、`0` から `io.period` まで毎クロック `+1` し、`cntReg >= io.period`（または `cntReg >= periodMax.U`）に達したら次のクロックで `0` に復帰します。`io.en` が `false.B` のときは `0` にリセットします。
- **比較器と境界ケース**:
  - `duty == 0`: `cntReg < 0` は成り立たないため、常時 `false.B`（デューティ比 0%）。
  - `duty > period`（例: `period = 4`, `duty = 5`）: `cntReg`（0〜4）は常に `duty` 未満となるため、常時 `true.B`（デューティ比 100%）。

---

## 6. 実機FPGA (TeCボード) 配線仕様（課題1 & 課題2）

生成された回路を実機TeCボードに書き込んで動作確認を行います。
**本回路の出力は負論理（0で点灯）のままで接続可能です。**

### 課題1: 1Hz生成・LED点滅回路 (`OneHzGenerator`) 配線
- **ピン接続仕様**:
  - `clock` $\to$ `CLK_IN` (`P55`): 9MHz オンボードシステムクロック
  - 入力 `en` $\to$ `RUN_SW` (`P16` | PULLUP): カウントイネーブル
  - 入力 `clear` $\to$ `RESET_SW` (`P120` | PULLUP): 同期クリア
  - 出力 `ledBlink` $\to$ `RUN_LED` (`P43`): 0.5秒点灯/0.5秒消灯の1Hz点滅
  - 出力 `oneHzPulse` $\to$ `C_LED` (`P46`): 1秒ストローブパルス（1秒に1回パルス点灯）
- **動作確認手順**:
  1. `RUN_SW` をオンにするとカウント動作を開始し、`RUN_LED` が0.5秒間隔で点滅します。
  2. 1秒周期満了ごとに `C_LED` が1クロック幅だけ一瞬点灯します。
  3. `RESET_SW` を押下すると、カウンタが即座に0クリアされ、LEDが消灯初期化されます。

### 課題2: 1秒カウントアップ・モジュロNカウンタ (`ModuloNCounter`) 配線
- **ピン接続仕様**:
  - `clock` $\to$ `CLK_IN` (`P55`): 9MHz オンボードシステムクロック
  - 入力 `en` $\to$ `RUN_SW` (`P16` | PULLUP): カウントイネーブル
  - 入力 `clear` $\to$ `RESET_SW` (`P120` | PULLUP): 同期クリア
  - 出力 `count[3:0]` $\to$ `DATA_LED[3:0]` (`P62, P61, P59, P58`): 4ビットバイナリ値表示
  - 出力 `runLED` $\to$ `RUN_LED` (`P43`): 動作状態インジケータ（1Hz点滅）
- **動作確認手順**:
  1. `RUN_SW` をオンにすると `RUN_LED` が点滅を開始し、`DATA_LED[3:0]` が1秒ごとに $0 \to 1 \to \dots \to 9 \to 0$ とカウントアップします。
  2. カウント値出力は内部で負論理反転（`~modReg`）されているため、TeCボードのLED（Lowで点灯）で正しい2進数値として点灯表示されます。
  3. `RESET_SW` を押すとカウント値が0クリアされ、全桁消灯（値0）に戻ります。

---

## 7. 本日の演習課題ガイドライン (90分)

演習コードは `Lab03/` 配下の各課題ディレクトリに分かれて配置されています。

### 基本問題 (必須・目安50分 / 配点70点)
1. **課題1: `OneHzGenerator(clkFreq: Int = 9000000)`** (配点35点 / 目安25分)
   - パラメータ化されたクロック周波数分周カウンタ（デフォルト9MHz）。
   - 0.5秒ごとに `ledBlink` を反転させるトグル動作。
   - 1秒（全周期満了）時に 1クロック幅の `oneHzPulse` パルス出力。
   - カウンタ更新とLED反転論理の明確な分離。
   - TeCボード実機配線（`RUN_SW`, `RESET_SW`, `RUN_LED`, `C_LED`）と動作確認。
2. **課題2: `ModuloNCounter(n: Int = 10, width: Int = 4, clkFreq: Int = 9000000)`** (配点35点 / 目安25分)
   - 1秒ごとにカウントアップする同期クリア付きモジュロ $n$ カウンタの設計。
   - `clear` (最優先リセット) > `en` (カウント動作) の優先度制御。
   - 内部9MHz分周による1秒ストローブ `modEn` の生成。
   - カウンタ更新論理と出力論理の明確な分離、および実機LEDに合わせた負論理出力（`runLED`, `count`）。
   - TeCボード実機配線（`RUN_SW`, `RESET_SW`, `RUN_LED`, `DATA_LED`）と動作確認。

### 確認課題 (目安30分 / 配点30点)
3. **`PwmGenerator(periodMax: Int = 255)`** (配点30点)
   - 内部周期カウンタと比較器による PWM パルス発生器。
   - `duty = 0` (0%デューティ), `duty > period` (100%デューティ) を含む境界条件の正確な処理。

### 採点ポイント
- 同期リセット (`RegInit`) が正しく適用されているか (15点)
- 9MHz 分周カウンタの半周期/全周期判定と LED 反転・パルス生成が正確か (20点)
- `clear > en` の優先度制御とカウンタ更新/出力論理の分離が正確か (20点)
- 1秒ストローブパルスによるモジュロカウントと負論理LED出力が正常か (15点)
- 実機TeCボードへのUCF配線が正しく定義され、実機上で正常に動作するか (15点)
- PWM のデューティ比比較とイネーブル停止処理が正確か (15点)
