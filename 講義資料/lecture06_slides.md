---
marp: true
theme: default
paginate: true
header: "第6回: 構造化設計とALU"
footer: "© 2026 Hideaki YANAGISAWA, Dept. of Computer Science and Electronic Engineering, NIT, Tokuyama College"
author: "Hideaki YANAGISAWA"
style: |
  section {
    font-family: 'Helvetica Neue', Arial, 'Hiragino Kaku Gothic ProN', sans-serif;
    font-size: 22px;
    padding: 32px 40px;
  }
  h1 { color: #1e3a8a; font-size: 34px; }
  h2 { color: #1e40af; border-bottom: 2px solid #3b82f6; font-size: 26px; }
  pre { font-size: 16px; margin: 8px 0; }
  .box { background: #eff6ff; border-left: 5px solid #3b82f6; padding: 8px 14px; margin: 8px 0; }
  .badge { background: #dbeafe; color: #1e40af; padding: 2px 6px; border-radius: 4px; font-weight: bold; font-size: 16px; }
---

# 第6回: 構造化設計とALU
### 〜機能別ユニットの分割設計・階層統合とステータスフラグ〜

<br>

- **資料作成者**: Hideaki YANAGISAWA
- **講義時間**: 60分
- **対象**: デジタル論理回路基礎習得者
- **本日のアジェンダ**:
  1. 前回の復習（反復的階層化から機能分割による並列階層化へ）
  2. フラット設計の限界と分割統治（Divide and Conquer）
  3. モジュール階層化とインスタンス化構文 (`Module(new ...)`)
  4. 構造化ALU (`StructuredALU`) の全体アーキテクチャ
  5. 算術ユニット (`ArithmeticUnit`: 加減算と `+&` / `-&` によるキャリー抽出)
  6. 論理ユニット (`LogicUnit`: `WireDefault` と `switch/is` による4種論理演算)
  7. シフトユニット (`ShiftUnit`: 動的左シフト `<<`・論理右シフト `>>` とビット幅仕様)
  8. トップモジュール `StructuredALU` の統合とステータスフラグ（Zero, Negative, Carry）
  9. 本日の演習課題（課題1: 算術ユニット / 課題2: 論理ユニット / 課題3: シフトユニット）

<!-- Note:
みなさんこんにちは。「Computer System Laboratory」第6回の講義を始めます。
前回（第5回）は、同一のモジュロNカウンタを複数インスタンス化して直列に繋ぐ「60進カウンタ」を学びました。
本日の第6回は、プロセッサの中核である「算術論理演算装置（ALU: Arithmetic Logic Unit）」をテーマに、異なる機能を持つサブモジュール（算術演算・論理演算・シフト演算）を分割設計し、階層的に統合する構造化設計手法を学びます。
-->

---

## 1. 前回（第5回）の復習と本日の位置づけ

### 第5回で学んだこと：反復的な直列階層化
- 同じクラス（`ModuloNCounter`）に異なるパラメータ（10進・6進）を与えて複数インスタンス化し、`rollover` $\to$ `en` で直列（カスケード）に接続した。

### 第6回で学ぶこと：機能分割による並列階層化（Divide and Conquer）
- プロセッサのデータパス（ALUなど）では、**役割の異なる複数の専用演算器**を並列に配置し、**マルチプレクサ（セレクタ）で出力を統合**する。

<div class="box">
<b>構造化設計の核心</b>: 「算術演算」「論理演算」「シフト演算」「トップレベルの出力選択」をそれぞれ独立したモジュールに分けることで、<b>単体テスト・デバッグ・機能拡張</b>が劇的に容易になる。
</div>

<!-- Note:
前回の60進カウンタは「同じカウンタを横に並べて桁上げを繋ぐ」という直列型の階層化でした。
一方、今回設計するALUは「算術演算器」「論理演算器」「シフト演算器」という全く中身が違う専門ユニットを並列に並べ、最後にセレクタで結果を選ぶ「並列・機能分割型」の階層化です。
-->

---

## 2. なぜフラット設計（単一巨大モジュール）は破綻するのか？

1つの `Module` 内に加算・減算・AND・OR・XOR・NOT・左右シフト・フラグ生成をすべて詰め込むと何が起きるか？

| 設計手法 | 構造の特徴 | 開発・検証上の影響 |
| :--- | :--- | :--- |
| **フラット設計**<br>（単一巨大モジュール） | 数十個の `when` / `switch` が1つのファイル内で複雑に絡み合う | ・バグが算術部・シフト部・セレクタのどこにあるか切り分け困難<br>・複数人での分担開発や部分再利用が不可能 |
| **構造化（階層化）設計**<br>（機能別モジュール分割） | 算術・論理・シフト・統合を独立した `Module` として分離 | ・**関心の分離**: 各ユニットの論理だけに集中できる<br>・**単体検証**: 各ユニットを個別のテストベンチで100%検証可能<br>・**再利用性**: 検証済みユニットを別の設計へそのまま流用可能 |

<!-- Note:
もし1つの巨大なモジュールの中にすべての演算とフラグ回路を書いたらどうなるでしょうか。
コードが数百行に膨れ上がり、どこかでビット幅やラッチのミスが起きたときに原因を特定するのが非常に困難になります。
これに対し、機能ごとにモジュールを分割すれば、それぞれのユニットを単独でテスト（単体テスト）して完璧に動くことを確認してから組み立てることができます。本日の演習でも、この単体モジュール実装とテストを体験します。
-->

---

## 3. モジュール階層化とインスタンス化の構文

子モジュールを親モジュール内部で実体化するには **`Module(new 子クラス名)`** を用いる。

```scala
class TopModule extends Module {
  val io = IO(new Bundle { ... })

  // 1. サブモジュールのインスタンス化 (必ず Module(...) でラップする)
  val subA = Module(new SubModuleA())
  val subB = Module(new SubModuleB())

  // 2. 入力の供給: 親の入力を子の入力へ接続 (子.io.in := 親.io.in)
  subA.io.in := io.inA
  subB.io.in := subA.io.out // 子から子への内部接続

  // 3. 出力の回収: 子の出力を親の出力へ接続 (親.io.out := 子.io.out)
  io.out := subB.io.out
}
```

- **`clock` と `reset` は自動配線**: Chiselでは親モジュールの `clock` / `reset` がすべてのサブモジュールへ自動的に分配される。

<!-- Note:
前回も登場した Module(new ...) の構文を改めて整理しましょう。
ただの `new SubModuleA()` ではハードウェアとして実体化されません。必ず `Module(new SubModuleA())` で包みます。
そして、親から子へ信号を渡すときは `subA.io.in := io.inA`、子から親へ結果を受け取るときは `io.out := subB.io.out` のように、`io` バンドルを介して `:=` で結線します。
-->

---

## 4. 構造化ALU (`StructuredALU`) の全体アーキテクチャ

トップモジュール `StructuredALU` は、独立した演算サブモジュールを束ね、上位制御信号 `aluSel` で出力を選択する。

<p align="center">
  <img src="figures/lecture05_structured_alu.svg" width="660">
</p>

- **機能分割**: 算術演算 (`ArithmeticUnit`) と 論理演算 (`LogicUnit`)（さらに拡張時はシフト演算 `ShiftUnit`）を並列に配置。
- **制御信号の階層分配**: 下位ビットを各ユニット内の演算選択 (`op`) に渡し、上位ビットをトップレベルのマルチプレクサ選択信号に使う。

<!-- Note:
構造化ALUの全体ブロック図です。
8ビット入力 a と b は、内部の各演算ユニットへ並列に分配されます。
各ユニットは同時に計算を行い、最後にトップレベルのマルチプレクサ（ALU MUX）が制御信号 aluSel に従って、どのユニットの計算結果を採用するかを選択して出力します。
-->

---

## 5. サブモジュール①：算術ユニット (`ArithmeticUnit`) — 課題1

8ビットの加算 (`ADD`) と減算 (`SUB`) を行い、**8ビット結果 `res`** と **キャリー／ボロー出力 `cout`** を生成する。

```scala
// 課題1 (assignment1): 算術ユニット
class ArithmeticUnit extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(8.W))
    val b    = Input(UInt(8.W))
    val op   = Input(Bool()) // 0 (false.B): ADD, 1 (true.B): SUB
    val res  = Output(UInt(8.W))
    val cout = Output(Bool())
  })

  // 9ビット幅拡張演算 (+& / -&) により桁上げ(cout)と8ビット結果(res)を同時に取得
  val ext = WireDefault(0.U(9.W))
  when(!io.op) {
    ext := io.a +& io.b
  }.otherwise {
    ext := io.a -& io.b
  }
  io.res  := ext(7, 0) // 下位8ビット: 演算結果
  io.cout := ext(8)    // 最上位ビット(第8ビット): キャリー／ボロー出力
}
```

<!-- Note:
本日の演習の課題1（assignment1）となる算術ユニット `ArithmeticUnit` です。
ここでの最大のポイントは、第1回で学んだビット幅拡張演算子 `+&` と `-&` の活用です。
通常の `+` や `-` を使うと結果が8ビットに切り捨てられてしまい、8ビット目からあふれたキャリー（または減算時のボロー）が消えてしまいます。
そこで 9ビットのワイヤ `ext = WireDefault(0.U(9.W))` を用意し、`+&` / `-&` で9ビット結果を受け取ってから、下位8ビット `ext(7, 0)` を `io.res` に、最上位ビット `ext(8)` を `io.cout` にスライスして出力します。
-->

---

## 6. `+&` / `-&` におけるキャリーとボローの動作

なぜ `ext := io.a +& io.b` と `ext := io.a -& io.b` で `ext(8)` がキャリー／ボローになるのか？

| 演算モード (`op`) | 入力例 (`a`, `b`) | 9ビット拡張計算 (`ext`) | `res = ext(7, 0)` | `cout = ext(8)` | 意味 |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **`false.B` (ADD)** | `10`, `20` | `0_0001_1110` (30) | `30` | `false.B` (`0`) | 桁上げなし |
| **`false.B` (ADD)** | `200`, `100` | `1_0010_1100` (300) | `44` | `true.B` (`1`) | **桁上げ（Carry）発生** |
| **`true.B` (SUB)** | `50`, `20` | `0_0001_1110` (30) | `30` | `false.B` (`0`) | 桁借りなし ($a \ge b$) |
| **`true.B` (SUB)** | `10`, `20` | `1_1111_0110` (-10) | `246` | `true.B` (`1`) | **桁借り（Borrow）発生 ($a < b$)** |

- **ポイント**: 無符号減算 $a - b$ において $a < b$ のとき、9ビット減算結果のMSB `ext(8)` は符号拡張により `1` となるため、そのまま**ボロー（桁借り）フラグ**として利用できる。

<!-- Note:
加算と減算の具体例を見てみましょう。
200 + 100 = 300 は8ビット（最大255）を超えるため、下位8ビットの `res` は 300 - 256 = 44 となり、9ビット目の `ext(8)` が `1`（true.B）になります。
また減算で 10 - 20 を計算すると、2の補数表現で 256 - 10 = 246 が下位8ビットに出力され、上位から借りてきたことを示す9ビット目の `ext(8)` が `1`（true.B）になります。この4パターンが課題1のテストベンチで検証されます。
-->

---

## 7. サブモジュール②：論理ユニット (`LogicUnit`) — 課題2

4種類のビット単位（Bitwise）論理演算を `op`（2ビット）で切り替える組合せ回路。

```scala
// 課題2 (assignment2): 論理ユニット (AND, OR, XOR, NOT)
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 0: AND, 1: OR, 2: XOR, 3: NOT a
    val res = Output(UInt(8.W))
  })

  // 透過ラッチ防止のため WireDefault で初期化し、switch/is で分岐
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

<!-- Note:
続いて課題2（assignment2）となる論理ユニット `LogicUnit` です。
第2回で学んだ組合せ回路の鉄則通り、まず `val outW = WireDefault(0.U(8.W))` でデフォルト値を設定して透過ラッチを防止します。
その上で `switch(io.op)` を使い、`0.U` ならビットAND (`&`)、`1.U` ならビットOR (`|`)、`2.U` ならビットXOR (`^`)、`3.U` なら入力 `a` のビット反転 (`~io.a`) を出力します。
-->

---

## 8. 論理ユニット (`LogicUnit`) の真理値とビットマスク応用

各ビット $i \in \{0, \dots, 7\}$ が独立・並列に演算される（キャリー伝播がないため極めて高速）。

| `op` | 演算子 | 入力 `a = 0xF0` (`1111_0000`) / `b = 0x3C` (`0011_1100`) | 結果 `res` | 主なプロセッサ応用 |
| :---: | :---: | :--- | :---: | :--- |
| **`0.U`** | `a & b` | `1111_0000 & 0011_1100` $\to$ `0011_0000` | **`0x30`** | 特定ビットの抽出・ゼロクリア（マスク処理） |
| **`1.U`** | `a \| b` | `1111_0000 \| 0011_1100` $\to$ `1111_1100` | **`0xFC`** | 特定ビットの強制セット（フラグ立て） |
| **`2.U`** | `a ^ b` | `1111_0000 ^ 0011_1100` $\to$ `1100_1100` | **`0xCC`** | 特定ビットの反転（トグル）・一致比較 |
| **`3.U`** | `~a` | `~1111_0000` $\to$ `0000_1111` | **`0x0F`** | 全ビット反転（1の補数生成） |

<!-- Note:
論理ユニットで行われる4つのビット演算の具体例と用途です。
算術演算と違い、隣のビットへの桁上げ（キャリー伝播）が一切存在しないため、ゲート1段分の遅延だけで瞬時に結果が確定します。
-->

---

## 9. サブモジュール③：シフトユニット (`ShiftUnit`) — 課題3

データを指定ビット数（`shamt`: Shift Amount）だけ左右へ移動させる**バレルシフタ（Barrel Shifter）**回路。

<p align="center">
  <img src="figures/lecture06_shift_unit.svg" width="640">
</p>

- **なぜ `shamt` は3ビット (`UInt(3.W)`) なのか？**
  - 8ビットデータ (`UInt(8.W)`) に対する有効なシフト量は **0〜7ビットの8通り**（$2^3 = 8$）であるため、`shamt` は3ビット幅で指定する。
- **シフト演算の用途**: $2^k$ 倍の高速乗除算（`a << k` $= a \times 2^k$、`a >> k` $= \lfloor a / 2^k \rfloor$）、ビットフィールド位置合わせ。

<!-- Note:
そして課題3（assignment3）で実装するのが、シフトユニット `ShiftUnit` です。
第4回のカウンタでは1クロックごとに1ビットずつずらす「シフトレジスタ（順序回路）」を学びましたが、ALU内部のシフタは、0〜7ビットの任意のシフト量を1サイクル（組合せ回路）で一気にずらす「バレルシフタ」です。
入力データが8ビットなので、シフト量 `shamt`（Shift Amount）は0〜7を表す3ビット（`UInt(3.W)`）となります。
-->

---

## 10. Chiselにおける動的シフト演算の注意点と実装 (`ShiftUnit.scala`)

Chiselの動的左シフト `io.a << io.shamt` は、**ビット落ちを防ぐため結果のビット幅が最大幅へ自動拡張**される仕様を持つ。

<div class="box">
<b>動的左シフトのビット幅拡張</b>:<br>
<code>io.a</code> (<code>8.W</code>) <code>&lt;&lt;</code> <code>io.shamt</code> (<code>3.W</code>: 最大7シフト) の結果幅は <b>8 + (2<sup>3</sup> - 1) = 15ビット (<code>15.W</code>)</b> に拡張される！<br>
8ビットALUの出力として得るには、<b><code>(...)(7, 0)</code> で下位8ビットを明示的にスライス</b>する。
</div>

```scala
// 課題3 (assignment3): シフトユニット (論理左シフト・論理右シフト)
class ShiftUnit extends Module {
  val io = IO(new Bundle {
    val a     = Input(UInt(8.W))
    val shamt = Input(UInt(3.W)) // シフト量 (0〜7ビット)
    val op    = Input(Bool())    // 0 (false.B): 論理左シフト (<<), 1 (true.B): 論理右シフト (>>)
    val res   = Output(UInt(8.W))
  })
  val outW = WireDefault(0.U(8.W))
  when(!io.op) {
    outW := (io.a << io.shamt)(7, 0) // 15ビット拡張結果から下位8ビットを取得
  }.otherwise {
    outW := io.a >> io.shamt       // 論理右シフト (上位には0が補充される)
  }
  io.res := outW
}
```

<!-- Note:
Chiselでシフトユニットを実装する際、非常に重要な仕様があります。
Chiselでは `io.a << io.shamt` のように変数（UInt）で左シフトを行うと、最大の7ビット左シフトが起きても上位ビットが消えないよう、自動的に 8 + 7 = 15ビット幅のワイヤを生成します。
今回のシフトユニットは8ビット演算器ですので、あふれた上位ビットを切り捨てて下位8ビットだけを取り出すために `(io.a << io.shamt)(7, 0)` とスライスします。
一方、論理右シフト `io.a >> io.shamt` は上位に0が詰められ、8ビット幅以下に収まるためそのまま代入できます。
-->

---

## 11. シフト演算の動作例（テストベンチ検証ケース）

`ShiftUnit` の動作を2進数・16進数で確認しよう。

| 演算モード (`op`) | 入力 `a` | `shamt` | シフト前の2進数 (`a`) | シフト後の下位8ビット (`res`) | 16進結果 |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **`false.B` (左 `<<`)** | `0x05` | `2.U` | `0000_0101` | `0001_0100` (下位に `00` 挿入) | **`0x14`** |
| **`false.B` (左 `<<`)** | `0xB4` | `3.U` | `1011_0100` | `1010_0000` (上位3bit切り捨て) | **`0xA0`** |
| **`true.B` (右 `>>`)** | `0xB4` | `2.U` | `1011_0100` | `0010_1101` (上位に `00` 挿入) | **`0x2D`** |
| **`true.B` (右 `>>`)** | `0xA5` | `0.U` | `1010_0101` | `1010_0101` (変化なし) | **`0xA5`** |

<!-- Note:
課題3のテストベンチ（ShiftUnittb.scala）でチェックする4つのケースです。
特に2つ目のケース `0xB4 << 3` に注目してください。
2進数 `1011_0100` を左に3ビットずらすと、上位の `101` が8ビット枠から外れて切り捨てられ、残った `1_0100` の右側に `000` が入って `1010_0000`（`0xA0`）になります。
また3つ目の `0xB4 >> 2` では、論理右シフトなので右側の `00` が押し出され、左側から `00` が補充されて `0010_1101`（`0x2D`）になります。
-->

---

## 12. トップモジュール：構造化ALU (`StructuredALU`) の統合実装

分割設計したサブモジュールを `Module(new ...)` でインスタンス化し、上位モジュールで統合する。

```scala
class StructuredALU extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W)) // bit[2]: 0=算術, 1=論理
    val out      = Output(UInt(8.W))
    val carryOut = Output(Bool())
  })
  // 1. 各サブモジュールのインスタンス化
  val arithUnit = Module(new ArithmeticUnit)
  val logicUnit = Module(new LogicUnit)

  // 2. 入力信号と下位制御ビットの配線
  arithUnit.io.a := io.a; arithUnit.io.b := io.b; arithUnit.io.op := io.aluSel(0)
  logicUnit.io.a := io.a; logicUnit.io.b := io.b; logicUnit.io.op := io.aluSel(1, 0)

  // 3. 最上位ビット aluSel(2) による出力選択 (MUX)
  io.out      := Mux(io.aluSel(2) === 0.U, arithUnit.io.res,  logicUnit.io.res)
  io.carryOut := Mux(io.aluSel(2) === 0.U, arithUnit.io.cout, false.B)
}
```

<!-- Note:
各演算ユニットが完成したら、トップモジュール `StructuredALU` の実装は驚くほどシンプルになります。
`Module(new ArithmeticUnit)` と `Module(new LogicUnit)` でインスタンスを作り、入力 `a`, `b` と選択信号の下位ビットを繋ぎます。
そして出力は `io.aluSel(2)`（最上位ビット）を見て、`0` なら算術ユニットの結果、`1` なら論理ユニットの結果を `Mux` で選ぶだけです。
論理演算が選ばれているときはキャリーは意味を持たないため、`io.carryOut` には `false.B` を出力します。
-->

---

## 13. ステータスフラグの生成回路（Zero, Negative, Carry）

CPUが条件分岐命令（`BEQ`, `BLT`, `BGEU` など）を実行するために、ALUは演算結果の性質を表す**ステータスフラグ**を出力する。

<p align="center">
  <img src="figures/lecture05_status_flags.svg" width="600">
</p>

| フラグ名 | 記号 | 論理条件（8ビット出力 `out` の場合） | Chisel記述 |
| :--- | :---: | :--- | :--- |
| **Zero (ゼロ)** | **Z** | 全ビットが `0` のとき `true.B` | `io.zero := (out === 0.U)` |
| **Negative (負)** | **N** | 最上位ビット(MSB)が `1` のとき `true.B` | `io.negative := out(7)` |
| **Carry (桁上げ)** | **C** | 算術演算で第8ビットへの桁上げ／ボロー発生 | `io.carry := rawCout` |

<!-- Note:
プロセッサのALUには、計算結果のデータだけでなく「ステータスフラグ」という1ビットの出力群が備わっています。
例えばプログラムの `if (a == b)` を判定するとき、CPUはALUで `a - b` を計算し、結果が0になったことを示す **Zeroフラグ (Z)** が `1` かどうかでジャンプ先を決めます。
Chiselでは、8ビット結果 `out` に対して `out === 0.U` でZeroフラグ、最上位ビット `out(7)` の抽出で2の補数表現におけるNegative（負数）フラグ、そして算術ユニットの `cout`（論理演算時は `false.B` にマスク）でCarryフラグを、それぞれ1行の組合せ回路として生成できます。
-->

---

## 14. 階層化ALU設計のベストプラクティスまとめ

<div class="box">
<b>構造化データパス設計 4つの鉄則</b><br>
1. <b>1モジュール・1責務</b>: 算術 (<code>ArithmeticUnit</code>)、論理 (<code>LogicUnit</code>)、シフト (<code>ShiftUnit</code>)、統合 (<code>StructuredALU</code>) を明確に分離する。<br>
2. <b>ビット幅境界の明示</b>: 加減算の桁上げ抽出には <code>+&amp;</code> / <code>-&amp;</code> と9ビットワイヤを用い、動的左シフト <code>&lt;&lt;</code> では拡張されたビット幅から <code>(7, 0)</code> で下位8ビットをスライスする。<br>
3. <b>透過ラッチの完全防止</b>: <code>switch</code> や <code>when</code> で分岐する組合せ回路の内部ワイヤには必ず <code>WireDefault(0.U(...))</code> を適用する。<br>
4. <b>不要フラグのマスク</b>: 算術演算以外（論理演算・シフト演算）が選択されている時は、<code>carryOut</code> を <code>false.B</code> にマスクして誤動作を防ぐ。
</div>

<!-- Note:
本日の設計原則のまとめです。
特に2番の「ビット幅境界の明示」はChiselならではの重要ポイントです。
算術ユニットでは `+&` / `-&` で9ビットに広げてから `ext(7, 0)` と `ext(8)` に分けること、シフトユニットでは `(io.a << io.shamt)(7, 0)` で8ビット幅に切り出すこと、この2つをしっかり押さえておきましょう。
-->

---

## 15. 本日の演習課題 (90分)

本日の演習では、ALUを構成する3つの基本演算ユニットをそれぞれ独立したモジュールとして実装し、単体テスト (`sbt test`) で検証します。

### 基本問題（必須 / 目安50分）
1. **課題1 (`assignment1`): 算術ユニット `ArithmeticUnit`**
   - 入力: `a(8.W)`, `b(8.W)`, `op(Bool)` ／ 出力: `res(8.W)`, `cout(Bool)`
   - `op = false.B`: 加算 (`io.a +& io.b`)、`op = true.B`: 減算 (`io.a -& io.b`)
2. **課題2 (`assignment2`): 論理ユニット `LogicUnit`**
   - 入力: `a(8.W)`, `b(8.W)`, `op(2.W)` ／ 出力: `res(8.W)`
   - `WireDefault` + `switch(io.op)` による `AND` (`0.U`), `OR` (`1.U`), `XOR` (`2.U`), `NOT a` (`3.U`)

### 発展問題（推奨 / 目安35分）
3. **課題3 (`assignment3`): シフトユニット `ShiftUnit`**
   - 入力: `a(8.W)`, `shamt(3.W)`, `op(Bool)` ／ 出力: `res(8.W)`
   - `op = false.B`: 論理左シフト `(io.a << io.shamt)(7, 0)`、`op = true.B`: 論理右シフト `io.a >> io.shamt`

<!-- Note:
本日の演習課題は3つに分かれています。
assignment1 は算術ユニット `ArithmeticUnit`、assignment2 は論理ユニット `LogicUnit`、assignment3 はシフトユニット `ShiftUnit` です。
それぞれのフォルダにテストベンチ（ArithmeticUnittb.scala, LogicUnittb.scala, ShiftUnittb.scala）が用意されていますので、各フォルダで `sbt test` を実行して全テストケースにパスすることを確認してください。
-->

---

## 16. 第6回 まとめ & 次回予告

### 本日の重要ポイント
1. 大規模な組合せ回路（ALU）は、**算術・論理・シフト**の各機能サブモジュールに分割して設計・単体検証する。
2. 算術演算では **`+&` / `-&`** で9ビット拡張し、下位8ビット (`res`) とキャリー (`cout`) を抽出する。
3. 論理演算では **`WireDefault` と `switch/is`** で透過ラッチを防ぎつつ4演算を切り替える。
4. 動的左シフトではビット幅拡張に対応するため **`(io.a << io.shamt)(7, 0)`** で下位8ビットをスライスする。
5. 演算結果から **Zero (`out === 0.U`)・Negative (`out(7)`)・Carry** のステータスフラグを生成できる。

### 次回予告: 第7回「有限状態機械（FSM）の設計」
- `ChiselEnum` を用いた型安全な状態定義と Moore型FSM による制御回路設計

<!-- Note:
第6回の講義は以上となります。
データパスの花形であるALUの内部構造と3つの演算ユニットの作り方が理解できたと思います。
次回第7回は、こうしたデータパスを順番に制御する頭脳となる「有限状態機械（FSM）」を学びます。
それでは演習に移ります。Lab06の課題ディレクトリを開いて作業を開始してください。
-->
