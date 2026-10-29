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
    font-size: 13.8px;
    line-height: 1.48;
    padding: 24px 34px;
    justify-content: flex-start;
  }
  h1 { color: #1e3a8a; font-size: 22px; margin-bottom: 6px; border-bottom: 2px solid #1e3a8a; padding-bottom: 4px; }
  h2 { color: #1e40af; font-size: 16.5px; border-bottom: 1.5px solid #3b82f6; margin-top: 10px; margin-bottom: 5px; padding-bottom: 2px; }
  h3 { color: #2563eb; font-size: 14px; margin-top: 6px; margin-bottom: 3px; }
  pre { font-size: 11.2px; margin: 4px 0; padding: 6px 10px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 4px; }
  .box { background: #eff6ff; border-left: 4px solid #3b82f6; padding: 5px 10px; margin: 5px 0; font-size: 12.5px; }
  table { font-size: 12px; border-collapse: collapse; margin: 5px 0; width: 100%; }
  th, td { border: 1px solid #cbd5e1; padding: 3px 6px; }
  th { background: #f1f5f9; color: #1e293b; }
---

# 第5回: 60進カウンタの構造化設計 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. 階層化設計 (Hierarchical Design) と単一クロック同期イネーブル伝播

機能ごとに独立した**サブモジュール（部品）**へ分割し、**トップモジュール（親）**内で `val u = Module(new SubModule(...))` として実体化・ポート結線（`:=`）する手法を**階層化設計**と呼びます。
- **【最重要・厳禁】非同期リップルクロックの追放**: 分周器や下位カウンタの出力を次段のクロック端子に繋ぐことは厳禁です。
- **完全同期イネーブル伝播**: 全サブモジュールを同一のシステムクロックで駆動し、1クロック幅のパルス（`oneHzPulse`, `rollover`）を次段の **`en` 端子**へ接続します。

---

## 2. 基本問題 (`assignment1`): 60進カウンタ (`Counter60`) とFPGAボード動作確認

`assignment1` は **`OneHzGenerator` + 10進カウンタ + 6進カウンタ** の3つのサブモジュールで `Counter60` を構成し、**FPGAボード（TeCボード）にプログラムして動作を確認**します。

```scala
class Counter60(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val secUnits = Output(UInt(4.W)) // 1の位 (0〜9)
    val secTens  = Output(UInt(3.W)) // 10の位 (0〜5)
    val runLED   = Output(Bool())    // 0.5秒反転LED
    val cout     = Output(Bool())    // 59秒の最後の1クロックでアサート
  })
  val oneHz = Module(new OneHzGenerator(clkFreq))
  val u10   = Module(new ModuloNCounter(10, 4)) // 10進カウンタ
  val u6    = Module(new ModuloNCounter(6, 3))  // 6進カウンタ

  oneHz.io.clear := io.clear; oneHz.io.en := io.en
  u10.io.clear   := io.clear; u10.io.en   := oneHz.io.oneHzPulse
  u6.io.clear    := io.clear; u6.io.en    := u10.io.rollover

  io.secUnits := u10.io.count; io.secTens := u6.io.count
  io.cout     := u6.io.rollover; io.runLED := oneHz.io.ledBlink
}
```

| 信号名 (`Counter60.ucf`) | `clock` | `io_en` / `io_clear` | `io_runLED` | `io_secUnits[3:0]` (1の位) | `io_secTens[2:0]` (10の位) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **TeCボード端子 (ピン)** | `CLK_IN` (`P55`) | `RUN_SW` (`P16`) / `RESET_SW` (`P120`) | `RUN_LED` (`P43`) | `DATA_LED[3:0]` (`P62,61,59,58`) | 拡張LED (`P85, P84, P83`) |

---

## 3. 確認課題 (`assignment2`): 24時間時計 (`DigitalClockCore`) と長時間シミュレーション検証

`assignment2` は 秒(60進)・分(60進)・時(24進) を3段同期カスケード接続し、**シミュレーション（`sbt test`）で動作を確認**します。

```scala
class DigitalClockCoretb extends AnyFlatSpec with ChiselScalatestTester {
  it should "cascade seconds to minutes to hours synchronously" in {
    test(new DigitalClockCore) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.enSec.poke(true.B)
      dut.clock.setTimeout(0) // サイクル数のタイムアウトを解除

      for (_ <- 0 until 999) {
        for (_ <- 0 until 999) { dut.clock.step(1) }
        println(s"Simulated time: ${dut.io.hour.peekInt()}:${dut.io.min.peekInt()}:${dut.io.sec.peekInt()}")
      }
    }
  }
}
```

- **`dut.clock.setTimeout(0)`**: デフォルトの1000サイクル制限によるタイムアウトを解除します。
- **`WriteVcdAnnotation` の削除**: 約99万サイクル（$999 \times 999$）の実行でメモリ不足にならないよう、長時間の波形保存（大量のメモリを消費）を行わずに実行します。
- **`Simulated time` 表示**: 実際の出力内容に合わせて `Simulated time: 時:分:秒` をコンソールに表示し、24時間時計の巡回動作を確認します。
