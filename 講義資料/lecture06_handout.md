# 第6回 講義配布資料（ハンドアウト）
## テーマ：構造化設計とALU（算術・論理・シフトユニットと階層統合）

---

### 1. 構造化（階層化）設計とALUの全体構成
複雑な単一モジュール（フラット設計）はバグの温床となる。プロセッサの演算器（ALU）を設計する際は、機能ごとに独立したサブモジュールへ分割（Divide and Conquer）し、単体テストで検証した上で上位モジュールにて統合する。

<p align="center">
  <img src="figures/lecture05_structured_alu.svg" width="620">
</p>

- **インスタンス化構文**: `val sub = Module(new SubModule())`
- **ポート配線**: `sub.io.in := io.in`（入力供給）、`io.out := sub.io.out`（出力回収）

---

### 2. ALUを構成する3つの基本演算サブモジュール（本日の演習課題）

#### ① 算術ユニット (`ArithmeticUnit` — `assignment1`)
8ビットの加算 (`ADD`) と減算 (`SUB`) を実行し、8ビット結果 `res` とキャリー／ボロー `cout` を出力する。
通常の `+` や `-` では最上位からの桁上げが切り捨てられるため、**ビット幅拡張演算子 `+&` / `-&`** を用いて9ビットワイヤ `ext` に受け、下位8ビット `ext(7, 0)` と第8ビット `ext(8)` に分離する。

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
  when(!io.op) {
    ext := io.a +& io.b
  }.otherwise {
    ext := io.a -& io.b
  }

  io.res  := ext(7, 0)
  io.cout := ext(8)
}
```

#### ② 論理ユニット (`LogicUnit` — `assignment2`)
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

#### ③ シフトユニット (`ShiftUnit` — `assignment3`)
8ビット入力 `a` を、3ビットのシフト量 `shamt`（0〜7ビット）だけ論理左シフト (`<<`) または論理右シフト (`>>`) するバレルシフタ回路。

<p align="center">
  <img src="figures/lecture06_shift_unit.svg" width="600">
</p>

- **動的左シフトのビット幅に注意**: Chiselの `io.a << io.shamt` は最大シフト時にも上位ビットが失われないよう、結果幅が $8 + (2^3 - 1) = 15$ ビットへ自動拡張される。8ビット出力 `res` に合わせるため、**`(io.a << io.shamt)(7, 0)`** で下位8ビットをスライスする。

```scala
class ShiftUnit extends Module {
  val io = IO(new Bundle {
    val a     = Input(UInt(8.W))
    val shamt = Input(UInt(3.W)) // シフト量 (0〜7ビット)
    val op    = Input(Bool())    // 0 (false.B): 論理左シフト (<<), 1 (true.B): 論理右シフト (>>)
    val res   = Output(UInt(8.W))
  })

  val outW = WireDefault(0.U(8.W))
  when(!io.op) {
    outW := (io.a << io.shamt)(7, 0)
  }.otherwise {
    outW := io.a >> io.shamt
  }
  io.res := outW
}
```

---

### 3. トップモジュール (`StructuredALU`) の階層統合とステータスフラグ

各サブモジュールを `Module(new ...)` で実体化し、上位ビットで出力をマルチプレクスする。

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

#### ステータスフラグ（Zero, Negative, Carry）の生成
プロセッサの条件分岐命令で使用されるステータスフラグは、ALUの出力結果 `out`（8ビット）および `carryOut` から組合せ回路として直接生成できる。

<p align="center">
  <img src="figures/lecture05_status_flags.svg" width="560">
</p>

- **Zeroフラグ (`Z`)**: `val zero = (io.out === 0.U)` （全ビットが0のとき `true.B`）
- **Negativeフラグ (`N`)**: `val negative = io.out(7)` （最上位ビットMSBが1のとき `true.B`）
- **Carryフラグ (`C`)**: `val carry = io.carryOut` （算術演算時の桁上げ／ボロー）

---

### 4. 演習課題チェックリスト（90分）
- [ ] **課題1 (`assignment1` / 必須・目安30分)**: `ArithmeticUnit.scala` を実装し、`sbt test` で加算・減算・キャリー／ボローの全テストにパスしたか？
- [ ] **課題2 (`assignment2` / 必須・目安25分)**: `LogicUnit.scala` を実装し、`sbt test` で AND, OR, XOR, NOT の全テストにパスしたか？
- [ ] **課題3 (`assignment3` / 発展・目安30分)**: `ShiftUnit.scala` を実装し、`sbt test` で論理左シフト (`<<`)・論理右シフト (`>>`) の全テストにパスしたか？
