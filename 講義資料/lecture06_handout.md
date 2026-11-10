---
marp: true
theme: default
paginate: true
header: "第6回 構造化設計とALU"
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

# 第6回: 構造化設計とALU 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. 構造化（階層化）設計とALUの工学的原則

1. **分割統治 (Divide and Conquer)**:
   - 複雑な単一モジュール（フラット設計）はバグと保守性低下の温床となる。プロセッサの演算器（ALU）を設計する際は、機能ごとに独立したサブモジュール（算術・論理・シフト等）へ分割し、単体テストで検証した上で上位モジュールにて統合する。
2. **サブモジュールのインスタンス化と配線**:
   - インスタンス化: `val sub = Module(new SubModule())`
   - ポート配線: `sub.io.in := io.in`（入力供給）、`io.out := sub.io.out`（出力回収）

<p align="center">
  <img src="figures/lecture05_structured_alu.svg" width="540">
</p>

### ステータスフラグ（Zero, Negative, Carry）の生成
- **Zeroフラグ (`Z`)**: `val zero = (io.out === 0.U)` （全ビットが `0` のとき `true.B`）
- **Negativeフラグ (`N`)**: `val negative = io.out(7)` （最上位ビットMSBが `1` のとき `true.B`）
- **Carryフラグ (`C`)**: `val carry = io.carryOut` （算術演算時の桁上げ／ボロー）

---

## 2. `assignment0`: TeCボード入出力回路 (`TeCIO`) の基礎確認

TeCボード上の8ビットデータ入力スイッチ（`DATA_SW[7:0]`）は1組しか存在しないため、2つの入力 `a`, `b` を必要とする演算器を実機で動作させるには、内部に2つのレジスタ **`regA`** と **`regB`** を設け、書き込みイネーブル信号（`weA`: `INC_SW`, `weB`: `DEC_SW`）で順番にデータを保持させる。

<p align="center">
  <img src="figures/lecture06_tecio_assign0.svg" width="560">
</p>

```scala
class TeCIO extends Module {
  val io = IO(new Bundle {
    val wd  = Input(UInt(8.W)) // DATA_SW[7:0] (書き込みデータ)
    val op  = Input(Bool())    // STEP_SW (0: regA出力, 1: regB出力)
    val weA = Input(Bool())    // INC_SW (regA 書き込みイネーブル)
    val weB = Input(Bool())    // DEC_SW (regB 書き込みイネーブル)
    val rd  = Output(UInt(8.W)) // DATA_LED[7:0] (負論理反転出力)
  })
  val regA = RegInit(0.U(8.W))
  val regB = RegInit(0.U(8.W))

  when(io.weA) { regA := io.wd }
  when(io.weB) { regB := io.wd }

  io.rd := ~Mux(io.op, regB, regA)
}
```

---

## 3. `assignment1`〜`assignment3`: 算術・論理・シフトユニットの設計

各課題ではまず演算サブモジュールを実装して **`sbt test`（シミュレーション）で単体検証**し、続いて **`TeCIO.scala` でサブモジュールをインスタンス化して FPGAボード（TeCボード）上で実機動作を確認**する。

### ① `assignment1`: 算術ユニット (`ArithmeticUnit.scala` & `TeCIO.scala`)
8ビットの加算 (`ADD`) と減算 (`SUB`) を実行し、8ビット結果 `res` とキャリー／ボロー `cout` を出力する。**ビット幅拡張演算子 `+&` / `-&`** を用いて9ビットワイヤ `ext` に受け、下位8ビット `ext(7, 0)` と第8ビット `ext(8)` に分離する。

```scala
class ArithmeticUnit extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(8.W))
    val b    = Input(UInt(8.W))
    val op   = Input(Bool()) // 0 (false.B): ADD, 1 (true.B): SUB
    val res  = Output(UInt(8.W))
    val cout = Output(Bool())
  })
  val ext = WireDefault(0.U(9.W))
  when(!io.op) { ext := io.a +& io.b }.otherwise { ext := io.a -& io.b }
  io.res  := ext(7, 0)
  io.cout := ext(8)
}
```
- **`assignment1/TeCIO.scala`**: `val au = Module(new ArithmeticUnit)` を接続し、`io.rd := ~au.io.res` を `DATA_LED[7:0]` へ、`io.cout := ~au.io.cout` を **`C_LED` (`P46`)** へ出力する。

### ② `assignment2`: 論理ユニット (`LogicUnit.scala` & `TeCIO.scala`)
ビット単位（Bitwise）の `AND`, `OR`, `XOR`, `NOT` を2ビット選択信号 `op` で切り替える。透過ラッチを防ぐため、`WireDefault(0.U(8.W))` で初期化したワイヤに対して `switch / is` で割り当てる。

```scala
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 0: AND, 1: OR, 2: XOR, 3: NOT a
    val res = Output(UInt(8.W))
  })
  val outW = WireDefault(0.U(8.W))
  switch(io.op) {
    is(0.U) { outW := io.a & io.b }
    is(1.U) { outW := io.a | io.b }
    is(2.U) { outW := io.a ^ io.b }
    is(3.U) { outW := ~io.a }
  }
  io.res := outW
}
```
- **`assignment2/TeCIO.scala`**: `val lu = Module(new LogicUnit())` を接続し、`BREAK_SW` (`op[1]`) と `STEP_SW` (`op[0]`) の2ビットで演算を選択して `io.res := ~lu.io.res` を `DATA_LED[7:0]` へ出力する。

### ③ `assignment3`: シフトユニット (`ShiftUnit.scala` & `TeCIO.scala`)
8ビット入力 `a` を、**3ビットで宣言したシフト量 `shamt` (`Input(UInt(3.W))`: 0〜7ビット)** だけ論理左シフト (`<<`) または論理右シフト (`>>`) する。Chiselの動的左シフト `io.a << io.shamt` は最大シフト時にも上位ビットが失われないよう結果幅が $8 + (2^3 - 1) = 15$ ビットへ自動拡張されるため、**`(io.a << io.shamt)(7, 0)`** で下位8ビットをスライスする。

<p align="center">
  <img src="figures/lecture06_shift_unit.svg" width="500">
</p>

```scala
class ShiftUnit extends Module {
  val io = IO(new Bundle {
    val a     = Input(UInt(8.W))
    val shamt = Input(UInt(3.W)) // シフト量は3ビット (0〜7ビット) で宣言
    val op    = Input(Bool())    // 0 (false.B): 論理左シフト (<<), 1 (true.B): 論理右シフト (>>)
    val res   = Output(UInt(8.W))
  })
  val outW = WireDefault(0.U(8.W))
  when(!io.op) { outW := (io.a << io.shamt)(7, 0) }.otherwise { outW := io.a >> io.shamt }
  io.res := outW
}
```

- **`assignment3/TeCIO.scala` における3ビット `shamt` レジスタ (`regB`) の宣言**:
  `ShiftUnit` の `shamt` は3ビット幅であるため、`TeCIO` 内の `regB` も **3ビット幅 (`RegInit(0.U(3.W))`)** で宣言し、`weB` (`DEC_SW`) 押下時に `io.wd(2, 0)`（下位3ビット）を書き込む。
```scala
  val regA = RegInit(0.U(8.W))
  val regB = RegInit(0.U(3.W)) // shamt 用に 3ビットで宣言
  when(io.weA) { regA := io.wd }
  when(io.weB) { regB := io.wd(2, 0) } // DATA_SW の下位3ビットを格納

  val su = Module(new ShiftUnit)
  su.io.a     := regA
  su.io.shamt := regB
  su.io.op    := io.op
  io.rd       := ~su.io.res
```

---

## 4. 各課題のTeCボード配線仕様 (`TeCIO.ucf`) と実機確認手順

<p align="center">
  <img src="figures/lecture06_tecio_logic.svg" width="580">
</p>

| 共通／課題別ポート | TeCボード上のスイッチ・LED | FPGAピン番号 (`LOC`) | 各課題 (`assignment0`〜`3`) での役割 |
| :--- | :--- | :--- | :--- |
| `clock` / `reset` | `CLK_IN` / **`RESET_SW`** | `P55` / `P120` (`PULLUP`) | システムクロック / `regA`, `regB` のリセット |
| `io_wd[7:0]` | **`DATA_SW[7:0]`** | `P26,27,29,30,32,33,34,35` (`PULLUP`) | **書き込みデータの用意**（`assignment3` の `regB` は下位3ビット `wd[2:0]`） |
| `io_weA` / `io_weB` | **`INC_SW` / `DEC_SW`** | `P22` / `P23` (`PULLUP`) | **`INC_SW`**: `regA` へ書き込み / **`DEC_SW`**: `regB` (`shamt`) へ書き込み |
| `io_op` (1bit)<br>(`assignment0, 1, 3`) | **`STEP_SW`** | `P15` (`PULLUP`) | `assignment0`: `0`=`regA`, `1`=`regB`<br>`assignment1`: `0`=ADD, `1`=SUB / `assignment3`: `0`=`<<`, `1`=`>>` |
| `io_op[1:0]` (2bit)<br>(`assignment2`) | **`BREAK_SW` (`op[1]`)**<br>**`STEP_SW` (`op[0]`)** | `P14` (`op[1]`, `PULLUP`)<br>`P15` (`op[0]`, `PULLUP`) | `assignment2` 2ビット演算選択 (`00`:AND, `01`:OR, `10`:XOR, `11`:NOT a) |
| `io_rd[7:0]` / `io_res[7:0]` | **`DATA_LED[7:0]`** | `P81,80,79,78,62,61,59,58` | **演算結果を `DATA_LED` へ出力**（負論理反転 `~`） |
| `io_cout` (`assignment1`) | **`C_LED`** | `P46` | `assignment1` のキャリー／ボロー出力 (`~au.io.cout`) |

### 本日の演習ステップ (90分)
1. **`assignment0`**: `TeCIO.scala` を確認し、`sbt test` および `sbt run` $\to$ FPGAボード書き込みで `regA`, `regB` の読み書き動作を確認する。
2. **`assignment1` (`ArithmeticUnit` + `TeCIO`)**: 算術ユニットを実装して `sbt test` で検証後、`TeCIO.scala` を完成させて `sbt run` で合成し、`TeCIO.ucf` を用いて実機の `DATA_LED` と `C_LED` (`P46`) で加減算・キャリー動作を確認する。
3. **`assignment2` (`LogicUnit` + `TeCIO`)**: 論理ユニットを実装して `sbt test` で検証後、`TeCIO.scala` を完成させて `sbt run` で合成し、`TeCIO.ucf` を用いて `BREAK_SW`, `STEP_SW` による4種論理演算を実機確認する。
4. **`assignment3` (`ShiftUnit` + `TeCIO`)**: 3ビット `shamt` のシフトユニットを実装して `sbt test` で検証後、`TeCIO.scala`（`regB` を3ビット宣言）を完成させて `sbt run` で合成し、`TeCIO.ucf` を用いて左右シフト動作を実機確認する。
