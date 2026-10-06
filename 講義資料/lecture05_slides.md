---
marp: true
theme: default
paginate: true
header: "第5回: 構造化設計"
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

# 第5回: 構造化設計
### 〜モジュールの階層化・ポートバインドとALUの統合〜

<br>

- **資料作成者**: Hideaki YANAGISAWA
- **講義時間**: 60分
- **対象**: デジタル論理回路基礎習得者
- **本日のアジェンダ**:
  1. ハードウェアの大規模化と分割統治 (Divide and Conquer)
  2. Chiselにおける階層モジュールの定義 (`class ... extends Module`)
  3. サブモジュールのインスタンス化 (`Module(new SubModule)`)
  4. 階層間ポートの配線作法（個別バインドとバルク接続 `<>`）
  5. 算術論理演算ユニット (ALU) のアーキテクチャ
  6. 算術部と論理部の分離設計と統合
  7. 発展トピック: ステータスフラグ生成と出力段パイプライン化
  8. 本日の演習課題（基本: 構造化ALU / 発展: パイプラインフラグALU）

<!-- Note:
第5回の講義へようこそ。
本講義シリーズもいよいよ後半に入ります。
これまで作ってきた加算器、MUX、デコーダ、カウンタなどの単体モジュールを組み合わせて、より大規模で実用的なデジタルシステムを構築するための「構造化設計（階層化設計）」を学びます。
ソフトウェアのクラス分割やカプセル化と同様に、ハードウェアでも適切なサブモジュール分割と明確なインターフェース定義が、システムの品質と開発効率を劇的に高めます。
-->

---

## 1. 大規模ハードウェア設計における課題

### フラット設計（単一巨大モジュール）の崩壊
すべての回路を1つの `Module` 内にフラットに記述すると：
- 信号線名が衝突し、可読性が著しく低下する。
- 同じ回路ブロック（加算器や比較器など）の再利用が困難になる。
- バグの局所化・単体テスト（Unit Test）が不可能になる。

### 構造化設計 (Structured Hierarchical Design)
回路を明確な機能境界を持つ独立した「サブモジュール」に分割し、最上位（トップモジュール）でそれらを結線する。

<p align="center"><img src="figures/lecture05_hierarchy_concept.svg" width="660"></p>

<!-- Note:
構造化設計の必要性です。
数百万ゲートにも及ぶ現代のSoCやCPUは、決して1つのファイルに書かれているわけではありません。
レジスタファイル、ALU、キャッシュ、バスインターフェースといった機能単位でモジュールに分割され、階層構造を形成しています。
モジュールごとに独立して設計・検証できることが、大規模開発の絶対条件です。
-->

---

## 2. Chiselにおけるモジュール階層の定義

Chiselでは、回路ブロックはすべて `Module` を継承した Scala クラスとして定義します。

```scala
// サブモジュールの定義
class SubModuleA extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(8.W))
    val out = Output(UInt(8.W))
  })
  io.out := io.in + 1.U
}
```

- **`io` フィールド**: 外部との入出力インターフェースを `IO(new Bundle { ... })` で束ねる。
- **カプセル化**: 内部の信号線（WireやReg）は外部から直接アクセスできず、必ず `io` ポートを介して通信する。

<!-- Note:
モジュールの定義方法です。
これまでの演習でも書いてきた `class Name extends Module` ですが、これがそのまま他のモジュールから呼び出される「部品」になります。
`val io = IO(new Bundle { ... })` がICチップのパッケージの足（ピン）に相当します。
-->

---

## 3. サブモジュールのインスタンス化 (`Module(...)`)

親モジュールの中で子モジュールを実体化（配置）するには、**`Module(new ...)`** を使用します。

```scala
class TopModule extends Module {
  val io = IO(new Bundle {
    val topIn  = Input(UInt(8.W))
    val topOut = Output(UInt(8.W))
  })

  // サブモジュールをインスタンス化
  val sub = Module(new SubModuleA)

  // ポートの結線
  sub.io.in := io.topIn
  io.topOut := sub.io.out
}
```

<div class="box">
<b>重要ルール: <code>new</code> だけでなく <code>Module(...)</code> で囲むこと</b><br>
Scalaのインスタンス化 <code>new SubModuleA</code> だけではChiselの回路ツリーに登録されません。必ず <code>Module(new ...)</code> でラップしてハードウェアとして実体化させてください。
</div>

<!-- Note:
インスタンス化の構文です。
Chisel初学者が極めて頻繁にやるミスが、`val sub = new SubModuleA` と `Module(...)` を書き忘れることです。
new だけだとScalaの単なるメモリ上のオブジェクトになってしまい、回路ネットリストに登録されません。
必ず `Module(new SubModuleA)` と記述してください。
-->

---

## 4. 階層間ポートの接続作法

親と子のポートを結線する際の信号の方向（データフロー）に注意します。

```
 [ 親モジュール: Top ]
   io.topIn (Input)  ─────────────>  sub.io.in (Input)   [ 子モジュール: Sub ]
   io.topOut (Output) <────────────  sub.io.out (Output) [ 子モジュール: Sub ]
```

### 接続の向きのルール
1. **親の入力 $\to$ 子の入力**: `sub.io.in := io.topIn`
2. **子の出力 $\to$ 親の出力**: `io.topOut := sub.io.out`
3. **子Aの出力 $\to$ 子Bの入力**: `subB.io.in := subA.io.out`

- 入力ポート（左辺）に対して出力ポート（右辺）を接続する。

<!-- Note:
ポート接続の向きです。
信号の流れを頭の中でイメージしてください。
外部から親モジュールに入ってきた信号は、子モジュールの入力へ流し込みます。
子モジュールが計算した出力は、親モジュールの出力ピンへ送り出すか、別の兄弟子モジュールの入力へと配線します。
逆向きに代入しようとすると、Chiselコンパイラが「入力ポートに代入しようとしています」とエラーを出してくれます。
-->

---

## 5. バルク接続演算子 (`<>`)

同じ構造を持つ Bundle 同士を接続する場合、**バルク接続演算子 `<>`** を使うと全ポートを一括で双方向に配線できます。

```scala
class ChannelIO extends Bundle {
  val data  = Output(UInt(8.W))
  val valid = Output(Bool())
  val ready = Input(Bool()) // 逆方向のフロー制御
}

// 送信側と受信側のポートを一括接続
receiver.io.channel <> sender.io.channel
```

- **自動方向反転**: `Output` は相手の `Input` へ、`Input` は相手の `Output` へ自動でクロスマッピングされる。
- AXIバスやReady/Validハンドシェイクなど、ピン数の多いバスの配線に威力を発揮。

<!-- Note:
Chiselの強力な機能であるバルク接続演算子 `<>` です。
ソフトウェアの代入は一方向ですが、ハードウェアのバスはデータ（順方向）とレディ信号（逆方向）が混在します。
`<>` を使うと、Bundle内の各信号の方向を解釈して、自動的に適切な向きで一括結線してくれます。
-->

---

## 6. 算術論理演算ユニット (ALU: Arithmetic Logic Unit)

プロセッサの心臓部であり、算術演算（加減算）とビット論理演算（AND, OR, XOR等）を実行する統合モジュール。

<p align="center">
  <img src="figures/lecture05_structured_alu.svg" width="650">
</p>

<!-- Note:
本日の題材であるALUの内部ブロック図です。
ALUは、加算器と論理ゲートを別々に動かし、セレクタでどちらの結果を採用するかを選ぶ構造が一般的です。
入力aとbは両方のユニットに同時に並列入力され、それぞれの計算結果が出そろったところで、選択信号 aluSel によって最終結果を選び出します。
これを構造化設計の模範例として作成します。
-->

---

## 7. サブモジュール1: 算術ユニット (`ArithmeticUnit`)

加算および減算を実行し、結果とキャリーアウトを出力する。

```scala
class ArithmeticUnit extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(8.W))
    val b    = Input(UInt(8.W))
    val op   = Input(Bool()) // 0: ADD, 1: SUB
    val res  = Output(UInt(8.W))
    val cout = Output(Bool())
  })

  val sumExt = Wire(UInt(9.W))
  when(!io.op) {
    sumExt := io.a +& io.b
  }.otherwise {
    sumExt := io.a -& io.b
  }

  io.res  := sumExt(7, 0)
  io.cout := sumExt(8)
}
```

<!-- Note:
サブモジュールその1、算術ユニットです。
第1回で学んだ拡張加算 `+&` および拡張減算 `-&` を活用して9ビットの中間結果を作り、下位8ビットを和、最上位ビットをキャリーアウトとして取り出しています。
このモジュール単体で独立してシミュレーション検証が可能です。
-->

---

## 8. サブモジュール2: 論理ユニット (`LogicUnit`)

4種類の基本ビット論理演算（AND, OR, XOR, NOT）を実行する。

```scala
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 00:AND, 01:OR, 10:XOR, 11:NOT
    val res = Output(UInt(8.W))
  })

  val outWire = WireDefault(0.U(8.W))
  switch(io.op) {
    is(0.U) { outWire := io.a & io.b }
    is(1.U) { outWire := io.a | io.b }
    is(2.U) { outWire := io.a ^ io.b }
    is(3.U) { outWire := ~io.a }
  }
  io.res := outWire
}
```

<!-- Note:
サブモジュールその2、論理ユニットです。
第2回で学んだ `switch / is` と `WireDefault` を使用し、2ビットの op 信号に応じて4つの論理演算を直交デコードして選択しています。
こちらも独立したサブモジュールとして完全に完結しています。
-->

---

## 9. トップモジュール: 構造化ALU (`StructuredALU`)

2つのサブモジュールをインスタンス化し、階層間ポートをバインドする。

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

  // 1. サブモジュールへの入力分配
  arith.io.a  := io.a; arith.io.b := io.b; arith.io.op := io.aluSel(0)
  logic.io.a  := io.a; logic.io.b := io.b; logic.io.op := io.aluSel(1, 0)

  // 2. 出力マルチプレクサによる選択
  when(io.aluSel(2) === 0.U) {
    io.out := arith.io.res; io.carryOut := arith.io.cout
  }.otherwise {
    io.out := logic.io.res; io.carryOut := false.B
  }
}
```

<!-- Note:
トップモジュールでの統合です。
Module(new ArithmeticUnit) と Module(new LogicUnit) で実体化し、入力aとbを両者に配線します。
そして aluSel の最上位ビット bit[2] を見て、0なら算術ユニットの結果、1なら論理ユニットの結果を出力ピンに接続します。
これが構造化設計の基本形です。
-->

---

## 10. 発展課題: ステータスフラグ (Flags) の生成

プロセッサの条件分岐命令（BEQ, BNE, BLT等）を判定するため、ALU演算結果から各種ステータスフラグを生成する。

<div style="display: flex; justify-content: space-between;">
<div style="width: 48%;">

### 主要ステータスフラグ
1. **Zeroフラグ ($Z$)**:
   - 演算結果が全ビット `0` のとき `true`
   - `io.zero := (aluOut === 0.U)`
2. **Negativeフラグ ($N$)**:
   - 2の補数として結果が負（MSBが `1`）のとき `true`
   - `io.negative := aluOut(7) === 1.B`
3. **Carryフラグ ($C$)**:
   - 算術演算での桁上げ

</div>
<div style="width: 48%;">

<p align="center">
  <img src="figures/lecture05_status_flags.svg" width="300">
</p>

</div>
</div>

<!-- Note:
発展課題で追加するステータスフラグの解説です。
RISC-VやARMなどのCPUには、演算結果の状態を表すフラグがあります。
結果がゼロならZフラグ、最上位ビットが1ならマイナスを表すNフラグ、桁あふれならCフラグです。
これらはALUの出力信号線を分岐させて簡単な比較器やスライスを通すだけで生成できます。
-->

---

## 11. 発展課題: 出力段パイプラインレジスタの挿入

ALUの演算結果とフラグをレジスタでサンプリングし、1クロック後に同期出力する。

<p align="center">
  <img src="figures/lecture05_pipeline_stage.svg" width="660">
</p>

### パイプライン化の意義
- 組合せ論理の遅延が出力ピンの先へ伝搬するのを遮断。
- **データと制御信号の同期**: データパスが1サイクル遅延するため、有効性を示す `validIn` も `RegNext(validIn)` で1サイクル遅延させて `validOut` と揃える。

<!-- Note:
パイプライン化の重要性です。
ALUは加算器やセレクタなどゲート段数が多く、クリティカルパスになりやすいブロックです。
この出力に RegNext を1段挟むことで、後続の回路への遅延の連鎖を断ち切り、システム全体のクロック周波数を劇的に上げることができます。
このとき、valid 信号も一緒に遅らせる「データと制御の同期」を忘れてはなりません。
-->

---

## 12. パイプライン化ALUの実装コード

```scala
class PipelinedALUWithFlags extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W))
    val validIn  = Input(Bool())
    val out      = Output(UInt(8.W))
    val zero     = Output(Bool())
    val negative = Output(Bool())
    val carry    = Output(Bool())
    val validOut = Output(Bool())
  })

  // 1. 既存の StructuredALU をインスタンス化
  val alu = Module(new StructuredALU)
  alu.io.a := io.a; alu.io.b := io.b; alu.io.aluSel := io.aluSel

  // 2. パイプラインレジスタの挿入
  val outReg   = RegNext(alu.io.out, 0.U)
  val carryReg = RegNext(alu.io.carryOut, false.B)
  val validReg = RegNext(io.validIn, false.B)

  // 3. レジスタ出力からフラグとデータを出力
  io.out      := outReg
  io.carry    := carryReg
  io.zero     := outReg === 0.U
  io.negative := outReg(7) === 1.B
  io.validOut := validReg
}
```

<!-- Note:
発展問題の完全なコードです。
先ほど作った StructuredALU を丸ごとサブモジュールとしてインスタンス化し、その出力を RegNext で受け止めています。
モジュールの再利用と階層化の美しさがここに表れています。
-->

---

## 13. モジュール分割の設計原則

良いハードウェア構造化のための3大原則：

1. **高凝集・低結合 (High Cohesion, Low Coupling)**:
   - 強く関連する論理（加減算）を1つのモジュールにまとめ、モジュール間の配線本数（ピン数）を最小化する。
2. **入出力方向の明確化**:
   - `Bundle` 内で `Input` と `Output` を厳格に宣言し、双方向バス（inout）は極力避ける（トライステートはLSI内部では原則禁止）。
3. **同期境界の一貫性**:
   - モジュールの境界は、できるだけレジスタで区切る（Register Slice）。これによりタイミング収束が容易になる。

<!-- Note:
ハードウェアアーキテクチャの心得です。
ソフトウェアの設計原則である高凝集・低結合は、ハードウェアでも全く同じです。
ピン数が多いと配線が混雑してチップ面積を圧迫します。
また、モジュールの境目に出力レジスタを置く習慣をつけておくと、後で巨大なシステムに組み込んだときにタイミングエラーが出にくくなります。
-->

---

## 14. 階層設計のデバッグ手法

階層化されたモジュールでバグが発生した場合の切り分け手順：

```
 [ トップモジュール不具合発生 ]
                │
                ├─> 1. 各サブモジュールの単体テスト (Unit Test) はパスしているか？
                │      └─> NO: サブモジュール内部の論理バグ
                │
                └─> 2. YES: 階層間のポート配線ミスを疑う
                       ├─> 信号線の左右取り違え (`sub.io.a := io.b` などの誤配線)
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

演習は**基本問題**と**発展問題**の2段階構成です。

### 基本問題（必須 / 目安45〜50分）
- **`StructuredALU`**:
  - `ArithmeticUnit`（加減算・キャリー出力）の設計
  - `LogicUnit`（4種ビット演算）の設計
  - 上位モジュールでのインスタンス化と `aluSel` による結果選択

### 確認課題
- **`PipelinedALUWithFlags`**:
  - `StructuredALU` のインスタンス化と出力段レジスタ (`RegNext`) 挿入
  - Zero, Negative, Carry フラグ生成および `validOut` 同期化

### バッファ (5〜10分)
- コード整形、テスト実行、質疑応答

<!-- Note:
演習課題の確認です。
まずは基本問題で、算術ユニットと論理ユニットを別々に書き、StructuredALUで綺麗に結線してください。
完了した方は発展課題に進み、パイプラインレジスタとフラグ生成回路を追加して、プロセッサ品質のALUを完成させましょう。
-->

---

## 16. 第5回 まとめ & 次回予告

### 本日の重要ポイント
1. 階層化設計により、大規模回路の可読性・再利用性・保守性を飛躍的に高める。
2. サブモジュールは `Module(new SubModule)` で実体化し、`io` ポートを配線する。
3. 演算器とセレクタを組み合わせることで汎用ALUを構築できる。
4. 出力段のレジスタ挿入（パイプライン化）により、クリティカルパスを分断できる。

### 次回予告: 第6回「60進カウンタの構造化設計」
- 第3回のカウンタと第5回の構造化設計を融合する時計回路の設計
- **工学的最重要原則**: 非同期リップルクロックの厳禁と、同期イネーブル伝播 (`cout` $\to$ `en`)
- 10進カウンタ × 6進カウンタのカスケード結合

<!-- Note:
第5回の講義は以上です。
モジュールを分割し、組み上げる構造化設計の面白さを実感していただけたでしょうか。
次回はこの技術を使って、時計の「秒・分」を刻む60進カウンタを作ります。
それでは演習に移ります。starter_kitのExercises.scalaのLab05に取り組んでください。
-->
