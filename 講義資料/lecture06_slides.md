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
### 〜算術論理演算ユニットの機能分離・パイプライン化とステータスフラグ〜

<br>

- **資料作成者**: Hideaki YANAGISAWA
- **講義時間**: 60分
- **対象**: デジタル論理回路基礎習得者
- **本日のアジェンダ**:
  1. プロセッサの計算中枢: 算術論理演算ユニット (ALU)
  2. 構造化設計の適用: 算術部と論理部の機能分離
  3. サブモジュール1: 算術ユニット (`ArithmeticUnit`) の設計
  4. サブモジュール2: 論理ユニット (`LogicUnit`) の設計
  5. トップモジュール: 構造化ALU (`StructuredALU`) の完全実装
  6. ALUの動作検証とマルチプレクサ制御
  7. クリティカルパスと動作周波数: パイプライン化の必要性
  8. `RegNext` によるパイプラインレジスタの挿入
  9. プロセッサの条件分岐を支えるステータスフラグ (Zero, Negative, Carry)
  10. 本日の演習課題（基本問題: 構造化ALU / 確認課題: パイプラインフラグALU）

<!-- Note:
第6回の講義へようこそ。
前回はモジュール階層化の基本と60進カウンタを通じて、同期回路の重要原則を学びました。
今回はその構造化設計の本格的な応用として、あらゆるプロセッサのコア（計算中枢）である「算術論理演算ユニット（ALU: Arithmetic Logic Unit）」を設計します。
加減算を行う算術ユニットとビット演算を行う論理ユニットを独立したサブモジュールとして切り分け、上位で綺麗に統合する手法を習得します。
さらに後半では、動作周波数を向上させるパイプラインレジスタの挿入や、条件分岐に不可欠な各種ステータスフラグの生成回路にも挑戦します。
-->

---

## 1. 算術論理演算ユニット (ALU) の工学的役割

### プロセッサの計算実行エンジン (Execution Unit)
コンピュータが実行するあらゆる機械語命令（加減算、ビット演算、比較等）は、すべてALUの中で物理的な論理演算として実行されます。

<p align="center"><img src="figures/lecture05_alu_arch.svg" width="620"></p>

- **入力データ**: 8ビットオペランド $A$ と $B$。
- **演算選択 (`aluSel`)**: どの演算を実行するかを指定する制御コード。
- **出力データ**: 演算結果 $Y$、および桁上げ等の状態フラグ。

<!-- Note:
ALUの役割です。
CPUのブロック図を見ると、必ず中心にV字型のALUシンボルが描かれています。
プログラムが「足し算せよ」「AND演算せよ」と命令を出したとき、実際にその演算を担当するのがこの回路です。
今日は8ビット幅の本格的なALUを設計します。
-->

---

## 2. 構造化設計によるALUの機能分離

### 1つのモジュールにすべて詰め込むとどうなるか？
加算、減算、AND、OR、XOR、NOTを巨大な1つの `switch` 構文にベタ書きすると、回路が肥大化し、タイミング解析や個別機能の検証が極めて困難になります。

### 算術部と論理部の分離 (Separation of Concerns)
演算の性質が根本的に異なる2つのユニットに階層分離します：

```
                ┌──────────────────┐
     a, b ─────>│  ArithmeticUnit  │──> arithOut, cout
                └──────────────────┘          │
                                              ├─> [ MUX ] ──> ALU出力
                ┌──────────────────┐          │    (aluSel[2])
     a, b ─────>│    LogicUnit     │──> logicOut
                └──────────────────┘
```

- **算術部**: キャリー伝播を伴う重い演算（加算・減算）。
- **論理部**: ビット間の独立した軽量並列演算（AND・OR・XOR・NOT）。

<!-- Note:
モジュール分割の戦略です。
算術演算は「桁上げ（キャリー）」が下位ビットから上位ビットへ連鎖するため遅延が大きく、一方で論理演算は各ビットが独立しているため非常に高速です。
このように回路の電気的・論理的性質が異なるものは別モジュールに分けるのが、ハードウェア設計の鉄則です。
-->

---

## 3. サブモジュール1: 算術ユニット (`ArithmeticUnit`)

### 仕様設計
加算（ADD: $A + B$）と減算（SUB: $A - B$）を実行し、8ビット結果とキャリーを出力。

```scala
class ArithmeticUnit extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(8.W))
    val b    = Input(UInt(8.W))
    val op   = Input(Bool())     // 0: ADD, 1: SUB
    val res  = Output(UInt(8.W))
    val cout = Output(Bool())
  })

  // 9ビットの拡張加算/減算による正確なキャリー検出
  val extRes = WireDefault(0.U(9.W))
  when(!io.op) {
    extRes := io.a +& io.b
  }.otherwise {
    extRes := io.a -& io.b
  }

  io.res  := extRes(7, 0)
  io.cout := extRes(8)
}
```

<!-- Note:
算術ユニットの実装コードです。
ここで第1回で学んだ拡張加算演算子 +& および 拡張減算 -& が活きてきます。
8ビット同士を足すと最大9ビットになるため、最上位ビット（8番ビット）を取り出すことで桁上げフラグ cout が1発で求まります。
WireDefault で未定義値を防ぐ習慣も忘れていません。
-->

---

## 4. サブモジュール2: 論理ユニット (`LogicUnit`)

### 仕様設計
4種類のビット単位論理演算（AND, OR, XOR, NOT）を実行。

```scala
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 0:AND, 1:OR, 2:XOR, 3:NOT
    val res = Output(UInt(8.W))
  })

  io.res := 0.U
  switch(io.op) {
    is(0.U) { io.res := io.a & io.b }
    is(1.U) { io.res := io.a | io.b }
    is(2.U) { io.res := io.a ^ io.b }
    is(3.U) { io.res := ~io.a }
  }
}
```

- 各ビットが独立に計算されるため、キャリー伝播遅延が一切存在しない高速論理。

<!-- Note:
論理ユニットの実装です。
第2回で学んだ switch 構文を使用しています。
op が 0ならAND、1ならOR、2ならXOR、3ならNOTを出力します。
桁上げ伝播がないため、非常に低遅延で合成されます。
-->

---

## 5. トップモジュール: 構造化ALU (`StructuredALU`)

親モジュールの中で `ArithmeticUnit` と `LogicUnit` をインスタンス化し、配線。

<p align="center"><img src="figures/lecture05_alu_top.svg" width="660"></p>

```scala
class StructuredALU extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W)) // bit[2]: 0=算術, 1=論理
    val out      = Output(UInt(8.W))
    val carryOut = Output(Bool())
  })

  val arith = Module(new ArithmeticUnit)
  val logic = Module(new LogicUnit)
  // ... 配線とセレクタ ...
}
```

<!-- Note:
トップモジュール StructuredALU の外観です。
内部に2つの子モジュールを配置し、外部ポート a と b を両方に分岐して供給します。
そして最後に、aluSel の最上位ビットを使って、どちらの計算結果を最終出力 out に流すかを切り替えます。
-->

---

## 6. ポート配線とマルチプレクサ制御の実装

```scala
  // 入力信号の分配
  arith.io.a := io.a
  arith.io.b := io.b
  arith.io.op := io.aluSel(0)

  logic.io.a := io.a
  logic.io.b := io.b
  logic.io.op := io.aluSel(1, 0)

  // 最上位ビット aluSel(2) による出力マルチプレクサ
  when(io.aluSel(2) === 0.U) {
    io.out      := arith.io.res
    io.carryOut := arith.io.cout
  }.otherwise {
    io.out      := logic.io.res
    io.carryOut := false.B // 論理演算時はキャリー無効
  }
```

- **`aluSel` デコード表**:
  - `000`: ADD / `001`: SUB
  - `100`: AND / `101`: OR / `110`: XOR / `111`: NOT

<!-- Note:
ポート配線の詳細です。
arith.io.op には下位1ビットを、logic.io.op には下位2ビットを渡しています。
そして aluSel(2) が 0 のときは算術結果を、1 のときは論理結果を出力します。
論理演算時にはキャリーは発生しないため、carryOut には false.B を安全に出力しています。
-->

---

## 7. クリティカルパスと動作周波数

### クリティカルパス (Critical Path) とは？
クロックエッジから次のクロックエッジまでの間に、信号が通過する**最も遅延時間の長い組合せ論理パス**。

<p align="center"><img src="figures/lecture05_critical_path.svg" width="640"></p>

$$f_{max} = \frac{1}{T_{clk}} \le \frac{1}{t_{cq} + t_{comb} + t_{setup}}$$

- 組合せ論理遅延 $t_{comb}$ が長大になると、最高動作周波数 $f_{max}$ が頭打ちになる。
- 解決策: 回路の途中にレジスタを挟んでパスを半分に分割する $\implies$ **パイプライン化！**

<!-- Note:
性能向上のための最重要概念、クリティカルパスです。
プロセッサの動作クロック（GHz）を上げるためには、1クロックの間に信号が通り抜けられるゲートの数を減らさなければなりません。
ALUのように演算が重い回路では、入力から出力までそのまま組合せ回路で繋ぐと遅延が累積してしまいます。
そこで登場するのがパイプラインレジスタです。
-->

---

## 8. `RegNext` によるパイプラインレジスタの挿入

Chiselでは、信号を1クロック遅延させるレジスタを **`RegNext(...)`** で極めて簡潔に記述できる。

```scala
// 1サイクルのパイプラインレジスタを挿入
val outReg   = RegNext(aluOut)
val carryReg = RegNext(aluCarry)

io.out      := outReg
io.carryOut := carryReg
```

- **効果**: ALU内部の演算遅延と、ALU後段のバス配線遅延がレジスタで物理的に分断される。
- **トレードオフ**: 結果が出るまでに 1 クロックのレイテンシ（遅延）が生じるが、**スループット（動作周波数）を劇的に向上**させることができる。

<!-- Note:
RegNext によるパイプライン化の実装です。
RegNext(aluOut) と書くだけで、D-FFのバンクが自動生成され、信号が1クロック分保持されます。
演算結果を1クロック遅れて受け取ることになりますが、その代わりにクロック周波数を2倍近くまで引き上げることが可能になります。
現代の高速CPUは、このように無数のパイプラインステージで構成されています。
-->

---

## 9. プロセッサ条件分岐を支えるステータスフラグ

プログラムの `if (a == b)` や `if (a < b)` は、ALUが生成する**ステータスフラグ**によって判定される。

<p align="center"><img src="figures/lecture05_flags.svg" width="660"></p>

| フラグ名 | 記号 | 判定条件 | 主な用途 |
| :---: | :---: | :--- | :--- |
| **Zero** | `Z` | 演算結果の全ビットが 0 (`out === 0.U`) | 一致判定 (`BEQ`, `BNE`) |
| **Negative** | `N` | 符号ビット (MSB) が 1 (`out(7) === 1.B`) | 負数判定 (`BLT`, `BGE`) |
| **Carry** | `C` | 算術演算で桁上げ/借りが発生 | 多倍長演算、符号なし比較 |

<!-- Note:
ステータスフラグの重要性です。
ソフトウェアの条件分岐 if 文は、ハードウェア的には「減算を行ってその結果のフラグを見る」ことで実現されています。
結果が0ならZeroフラグが立ち、引き算した結果がマイナスならNegativeフラグが立ちます。
CPUのプログラムカウンタは、このフラグを見てジャンプするかどうかを決定します。
-->

---

## 10. フラグ生成回路のハードウェア記述

```scala
// Zeroフラグ: 8ビット全ビットが0であることを検出（NORツリー）
val zeroFlag = (aluOut === 0.U)

// Negativeフラグ: 最上位ビット（符号ビット）を抽出
val negativeFlag = aluOut(7)

// Carryフラグ: 算術演算時のみ有効
val carryFlag = aluCarry
```

- **回路規模の考察**:
  - `zeroFlag` は8入力NORゲート（またはANDリダクション）に相当し、極めて軽量。
  - `negativeFlag` は単なる配線の引き出し（ゲート数 0）。

<!-- Note:
フラグ生成の実装コードです。
Chiselでは直感的に書けます。
zeroFlag は aluOut === 0.U で判定します。ハードウェア的には8ビットの各ビットをORして反転するNOR回路になります。
negativeFlag は最上位ビット aluOut(7) を取り出すだけです。
キャリーフラグは算術ユニットのキャリー出力をそのまま利用します。
-->

---

## 11. 制御信号 (`validIn` / `validOut`) の同期整合性

データパスにパイプラインレジスタを入れた場合、**制御信号も全く同じサイクル数だけ遅延**させなければならない。

```
Cycle:       0          1          2
validIn:   ──‾‾‾‾‾‾‾‾\______________________
Data:      [  A, B  ]
outReg:    ─────────[ Result ]
validOut:  ─────────[  ‾‾‾‾‾‾‾‾\  ]  <── validIn を RegNext したもの！
```

```scala
// 制御信号の同期遅延
io.validOut := RegNext(io.validIn, init = false.B)
```

- これを怠ると、データが無効なサイクルで後段の回路が誤ってデータを取り込んでしまう！

<!-- Note:
パイプライン設計で最も重要な「制御の同期」です。
データだけを1クロック遅らせて、データの有効性を示す valid 信号をそのままにしておくと、後段の回路が「まだ計算が終わっていない無効な値」を正しいデータだと勘違いして処理してしまいます。
データが1クロック遅れるなら、valid 信号も必ず RegNext で1クロック遅らせる。
これがパイプライン制御の鉄則です。
-->

---

## 12. パイプライン化ALU (`PipelinedALUWithFlags`) の完全構造

```scala
class PipelinedALUWithFlags extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(8.W)), val b = Input(UInt(8.W)), val aluSel = Input(UInt(3.W))
    val validIn = Input(Bool())
    val out = Output(UInt(8.W)), val zero = Output(Bool())
    val negative = Output(Bool()), val carry = Output(Bool())
    val validOut = Output(Bool())
  })

  val alu = Module(new StructuredALU)
  alu.io.a := io.a; alu.io.b := io.b; alu.io.aluSel := io.aluSel

  // パイプラインレジスタとフラグの同期ラッチ
  io.out      := RegNext(alu.io.out)
  io.carry    := RegNext(alu.io.carryOut)
  io.zero     := RegNext(alu.io.out === 0.U)
  io.negative := RegNext(alu.io.out(7))
  io.validOut := RegNext(io.validIn, false.B)
}
```

<!-- Note:
確認課題である PipelinedALUWithFlags の完全実装例です。
StructuredALU をインスタンス化し、その出力をすべて RegNext で受けています。
フラグも出力データと完全に同じタイミングで1クロック遅れて同期出力されます。
リセット時には validOut が確実に false.B になるよう初期値を指定しています。
-->

---

## 13. モジュール分割の設計原則まとめ

良いハードウェア構造化のための3大原則：

1. **高凝集・低結合 (High Cohesion, Low Coupling)**:
   - 関連の強い論理（加算・減算）を同一モジュールに閉じ込め、外部との信号線数を最小化する。
2. **入出力方向の明確化**:
   - `Bundle` 内で `Input` と `Output` を厳格に宣言し、未接続ピンを残さない。
3. **同期境界の明確化 (Register Slice)**:
   - モジュール境界や長大な演算器の出力段にレジスタを挟むことで、タイミング解析（STA）が容易になり、システム全体の最高動作周波数を向上できる。

<!-- Note:
構造化設計のまとめです。
ソフトウェアのクラス設計と同様、ハードウェアでも高凝集・低結合が設計品質を左右します。
また、モジュールの出口にレジスタを置く設計作法を「レジスタスライス」と呼び、FPGAやASICで周波数を上げるための標準的なテクニックです。
-->

---

## 14. 階層設計のデバッグ手法

階層化されたモジュールでバグが発生した場合の切り分け手順：

```
 [ トップモジュール不具合発生 ]
                │
                ├─> 1. 各サブモジュールの単体テスト (Unit Test) はパスしているか？
                │      └─> NO: サブモジュール内部の論理バグ（加減算やビット反転の誤り）
                │
                └─> 2. YES: 階層間のポート配線ミスを疑う
                       ├─> 信号線の左右取り違え (`arith.io.a := io.b` などの誤配線)
                       └─> 制御信号のビットスライスのズレ (`aluSel(2, 1)` と `(1, 0)`)
```

- 「部品が正しいか」と「配線が正しいか」を完全に分離して検証する。

<!-- Note:
デバッグの鉄則です。
構造化設計の最大の強みは「部品単体でテストできること」です。
システムが動かないときは、まず ArithmeticUnit 単体、LogicUnit 単体でテストベンチを走らせます。
両方が100%動いているなら、疑うべきはトップモジュールでのピンの配線ミス（左右のつなぎ間違いなど）に絞り込めます。
-->

---

## 15. 本日の演習課題 (90分)

演習は**基本問題**と**確認課題**の2段階構成です。

### 基本問題（必須 / 目安45〜50分）
- **`StructuredALU`**:
  - `ArithmeticUnit`（加減算・キャリー出力）の設計
  - `LogicUnit`（4種ビット論理演算）の設計
  - 上位モジュールでのインスタンス化と `aluSel` による結果選択

### 確認課題（推奨 / 目安30〜35分）
- **`PipelinedALUWithFlags`**:
  - `StructuredALU` のインスタンス化と出力段レジスタ (`RegNext`) 挿入
  - Zero, Negative, Carry フラグ生成および `validOut` 同期化

### バッファ (5〜10分)
- コード整形、テスト実行、波形確認

<!-- Note:
演習課題の確認です。
まずは基本問題で、算術ユニットと論理ユニットを別々に書き、StructuredALUで綺麗に結線してください。
完了した方は確認課題に進み、パイプラインレジスタとフラグ生成回路を追加して、プロセッサ品質のALUを完成させましょう。
-->

---

## 16. 第6回 まとめ & 次回予告

### 本日の重要ポイント
1. 階層化設計により、演算器を機能単位（算術部・論理部）に分割統治できる。
2. サブモジュールは `Module(new SubModule)` で実体化し、ピン同士を配線する。
3. `RegNext` によりパイプラインレジスタを挿入し、クリティカルパスを分断できる。
4. 演算結果からゼロ・負・キャリーの**ステータスフラグ**を生成し、制御信号と同期させる。

### 次回予告: 第7回「有限状態機械 (FSM)」
- 順序回路の最高峰: システムの自律制御を司るステートマシン
- **Moore型 vs Mealy型**: グリッチ耐性とタイミング設計の違い
- Chiselにおける列挙型 `ChiselEnum` を用いた型安全な状態遷移設計

<!-- Note:
第6回の講義は以上です。
モジュールを分割し、組み上げる構造化設計の醍醐味と、パイプライン化の威力を実感できたでしょうか。
次回はいよいよ、ハードウェア制御の最高峰である「有限状態機械（FSM）」を学びます。
それでは演習に移ります。starter_kitのExercises.scalaのLab06を開いてください。
-->
