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
  <img src="figures/lecture05_structured_alu.svg" width="580">
</p>

---

## 2. ALUを構成する3つの基本演算サブモジュール

### ① 算術ユニット (`ArithmeticUnit`)
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

### ② 論理ユニット (`LogicUnit`)
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

### ③ シフトユニット (`ShiftUnit`)
8ビット入力 `a` を、3ビットのシフト量 `shamt`（0〜7ビット）だけ論理左シフト (`<<`) または論理右シフト (`>>`) するバレルシフタ回路。Chiselの動的左シフト `io.a << io.shamt` は最大シフト時にも上位ビットが失われないよう結果幅が $8 + (2^3 - 1) = 15$ ビットへ自動拡張されるため、**`(io.a << io.shamt)(7, 0)`** で下位8ビットをスライスする。

<p align="center">
  <img src="figures/lecture06_shift_unit.svg" width="560">
</p>

```scala
class ShiftUnit extends Module {
  val io = IO(new Bundle {
    val a     = Input(UInt(8.W))
    val shamt = Input(UInt(3.W)) // シフト量 (0〜7ビット)
    val op    = Input(Bool())    // 0 (false.B): 論理左シフト (<<), 1 (true.B): 論理右シフト (>>)
    val res   = Output(UInt(8.W))
  })
  val outW = WireDefault(0.U(8.W))
  when(!io.op) { outW := (io.a << io.shamt)(7, 0) }.otherwise { outW := io.a >> io.shamt }
  io.res := outW
}
```

---

## 3. トップモジュール (`StructuredALU`) の階層統合とステータスフラグ

```scala
class StructuredALU extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W)) // bit[2]: 0=算術, 1=論理
    val out      = Output(UInt(8.W))
    val carryOut = Output(Bool())
  })
  val arithUnit = Module(new ArithmeticUnit)
  val logicUnit = Module(new LogicUnit)

  arithUnit.io.a := io.a; arithUnit.io.b := io.b; arithUnit.io.op := io.aluSel(0)
  logicUnit.io.a := io.a; logicUnit.io.b := io.b; logicUnit.io.op := io.aluSel(1, 0)

  io.out      := Mux(io.aluSel(2) === 0.U, arithUnit.io.res,  logicUnit.io.res)
  io.carryOut := Mux(io.aluSel(2) === 0.U, arithUnit.io.cout, false.B)
}
```

### ステータスフラグ（Zero, Negative, Carry）の生成
<p align="center">
  <img src="figures/lecture05_status_flags.svg" width="520">
</p>

- **Zeroフラグ (`Z`)**: `val zero = (io.out === 0.U)` （全ビットが `0` のとき `true.B`）
- **Negativeフラグ (`N`)**: `val negative = io.out(7)` （最上位ビットMSBが `1` のとき `true.B`）
- **Carryフラグ (`C`)**: `val carry = io.carryOut` （算術演算時の桁上げ／ボロー）

---

## 4. 本日の演習課題仕様 (Assignment Specifications)

### 基本問題1 (`assignment1`): 算術ユニット (`ArithmeticUnit`)
- **入力**: `a` (UInt(8.W)), `b` (UInt(8.W)), `op` (Bool: `false.B`=ADD, `true.B`=SUB)
- **出力**: `res` (UInt(8.W)), `cout` (Bool)
- **論理仕様**: `+&` / `-&` による9ビット拡張演算を行い、`ext(7, 0)` を `res`、`ext(8)` を `cout` に出力。

### 基本問題2 (`assignment2`): 論理ユニット (`LogicUnit`)
- **入力**: `a` (UInt(8.W)), `b` (UInt(8.W)), `op` (UInt(2.W): `0`=AND, `1`=OR, `2`=XOR, `3`=NOT a)
- **出力**: `res` (UInt(8.W))
- **論理仕様**: `WireDefault(0.U(8.W))` と `switch / is` を用いて4種類のビット論理演算を出力。

### 発展問題 (`assignment3`): シフトユニット (`ShiftUnit`)
- **入力**: `a` (UInt(8.W)), `shamt` (UInt(3.W)), `op` (Bool: `false.B`=論理左シフト `<<`, `true.B`=論理右シフト `>>`)
- **出力**: `res` (UInt(8.W))
- **論理仕様**: `op = false.B` で `(io.a << io.shamt)(7, 0)`、`op = true.B` で `io.a >> io.shamt` を出力。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab06/` 配下の各課題ディレクトリ（`assignment1`〜`assignment3`）に配置されています。

### 基本問題 (必須・目安50分 / 配点70点)
1. **`ArithmeticUnit` (`assignment1`)** (目安25分 / 35点): 加算・減算とキャリー／ボロー出力の実装・検証。
2. **`LogicUnit` (`assignment2`)** (目安25分 / 35点): 4種ビット論理演算の実装・検証。

### 発展問題 (推奨・目安35分 / 配点30点)
- **`ShiftUnit` (`assignment3`)** (目安35分 / 30点): 論理左シフト・論理右シフトの実装・検証。

### 採点ポイント
- `ArithmeticUnit` で `+&` / `-&` を用いてキャリー・ボローが正しく抽出されているか (35点)
- `LogicUnit` で `WireDefault` により透過ラッチが防止され4演算が正しく動作するか (35点)
- `ShiftUnit` で動的左シフトのビット幅拡張に対して `(7, 0)` スライスが適切に行われているか (30点)
