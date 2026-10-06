---
marp: true
theme: default
paginate: true
header: "第2回: Muxとデコーダの回路"
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

# 第2回: Muxとデコーダの回路
### 〜条件付き代入の回路合成と透過ラッチの防止〜

<br>

- **資料作成者**: Hideaki YANAGISAWA
- **講義時間**: 60分
- **対象**: デジタル論理回路基礎習得者
- **本日のアジェンダ**:
  1. 前回の復習（組合せ論理とChisel型システム）
  2. マルチプレクサ (MUX) の原理とデータパス制御
  3. 2-to-1 MUX (`Mux`) から多入力MUXへのツリー構成
  4. 条件付き代入 (`when / elsewhen / otherwise`) と優先度マルチプレクサ
  5. 直交デコーダ回路と `switch / is`
  6. **最重要概念**: 未完全記述による意図しない透過ラッチの発生メカニズム
  7. `WireDefault` による透過ラッチの完全撲滅手法
  8. 本日の演習課題（基本: 4-to-1 MUX & 7セグ / 発展: 優先度エンコーダ）

<!-- Note:
第2回の講義へようこそ。
前回はブール代数に基づき、基本的な論理ゲートと加算器の型安全な記述を学びました。
第2回では、プロセッサや制御回路の根幹をなす「データパスの切り替え（マルチプレクサ）」と「条件デコード」を扱います。
特に、ソフトウェアとハードウェアで最も考え方が異なる「未完全代入とラッチの発生」という重大な工学的罠について、Chiselがどのようにそれを防止するかを深く学びます。
-->

---

## 1. 前回のおさらいと本日の接続

### 第1回の重要コンセプト
- 組合せ回路は無記憶・非同期であり、**有向非巡回グラフ (DAG)** で構成される。
- 信号線は `Wire` で実体化し、接続演算子 `:=` で配線する。
- 桁あふれを保持する加算には拡張加算 `+&` を使用する。

### 本日の問い
> 「プログラム言語の `if-else` や `switch-case` は、ハードウェアとしてどのような物理回路に合成されるのか？」
> 「条件に合致しなかったとき、ハードウェアの信号線はどう振る舞うべきか？」

<!-- Note:
前回の要点を振り返りつつ、本日の核心的な問いを提示します。
C言語やPythonなどのソフトウェアでは、if文の条件が偽なら「何もしない（メモリの値がそのまま維持される）」が当たり前です。
しかし、クロックを持たない組合せ論理回路で「何もしない」を実行しようとすると、回路は前回の値を記憶しようとして予期せぬラッチを生成してしまいます。この違いを本講義で解き明かします。
-->

---

## 2. マルチプレクサ (Multiplexer: MUX) の基礎

### 2-to-1 マルチプレクサの原理
セレクト信号 $sel$（1ビット）によって、2つの入力 $in0, in1$ のいずれか一方を選択して出力する。

<div style="display: flex; justify-content: space-between;">
<div style="width: 48%;">

<p align="center">
  <img src="figures/lecture02_mux2to1.svg" width="300">
</p>

</div>
<div style="width: 48%;">

### 論理式
$$out = (\overline{sel} \cdot in0) + (sel \cdot in1)$$

</div>
</div>

- $sel = 0$ のとき $out = in0$
- $sel = 1$ のとき $out = in1$
- **ハードウェアの意味**: 物理的な配線スイッチ。データパスの分岐・選択を行う基本ビルディングブロック。

<!-- Note:
マルチプレクサの基本です。
2入力1出力のMUXは、AND-ORゲート、あるいはCMOSトランスミッションゲートで構成されます。
プロセッサのレジスタファイルからの読み出しデータ選択や、ALUの演算結果選択など、あらゆる場所で使用される最重要コンポーネントです。
-->

---

## 3. Chiselにおける2-to-1 MUX (`Mux`)

Chiselには、2-to-1 MUXを直接生成するプリミティブ `Mux` が用意されています。

```scala
// Mux(cond, trueValue, falseValue)
val out = Mux(io.sel, io.in1, io.in0)
```

- **第一引数 `cond`**: 1ビットの `Bool` 型条件信号。
- **第二引数 `trueValue`**: 条件が `true.B` の時に出力される信号。
- **第三引数 `falseValue`**: 条件が `false.B` の時に出力される信号。

<div class="box">
<b>型安全性の保証</b><br>
<code>trueValue</code> と <code>falseValue</code> は全く同一の型かつビット幅でなければなりません。幅が異なる信号を渡すと、Chiselコンパイラがコンパイル時に型エラーを報告します。
</div>

<!-- Note:
Chiselの組み込みプリミティブ Mux の説明です。
書式は三項演算子 `cond ? trueVal : falseVal` に似ていますが、ソフトウェアの条件分岐ではなく、物理的な2-to-1マルチプレクサをインスタンス化しています。
Chiselの型チェッカーにより、両方の入力のビット幅が完全に一致していることが検証されます。
-->

---

## 4. 多入力MUXのツリー合成 (木構造展開)

4-to-1 MUXは、3つの2-to-1 MUXを木構造（トーナメント表形式）に結合することで合成できます。

<p align="center">
  <img src="figures/lecture02_mux4_tree.svg" width="560">
</p>

```scala
val muxLow  = Mux(io.sel(0), io.in1, io.in0)
val muxHigh = Mux(io.sel(0), io.in3, io.in2)
val finalOut = Mux(io.sel(1), muxHigh, muxLow)
```

- 下位ビット $sel(0)$ でペアごとに絞り込み、上位ビット $sel(1)$ で最終段を決定。
- 伝搬遅延は $\log_2 N$ 段のゲート遅延。

<!-- Note:
多入力MUXの構成法です。
4入力MUXを作る場合、このように2段の2-to-1 MUXツリーを組むのが教科書的なアプローチです。
1段目で下位ビット sel(0) を使って in0/in1 と in2/in3 を同時に選択し、2段目で上位ビット sel(1) を使ってどちらのグループの結果を採用するかを決めます。
遅延は O(log N) となり、高速に動作します。
-->

---

## 5. 条件付き代入: `when / elsewhen / otherwise`

Chiselでは、手続き型プログラミングのように条件分岐を記述する構文が提供されています。

```scala
val out = WireDefault(0.U(8.W))

when(io.condA) {
  out := io.dataA
}.elsewhen(io.condB) {
  out := io.dataB
}.otherwise {
  out := io.dataDefault
}
```

### ハードウェアとしての実体
これはソフトウェアの逐次実行ブロックではなく、**優先度付きマルチプレクサの連鎖 (Priority Chain)** として合成されます。

<p align="center">
  <img src="figures/lecture02_priority_chain.svg" width="560">
</p>

<!-- Note:
Chiselの when 構文のハードウェア的な意味を解説します。
C言語の if-else と見た目は同じですが、ハードウェアでは「優先度付きマルチプレクサの直列接続」になります。
先頭の condA が true ならば、背後の condB の真偽に関わらず dataA が出力されます。
したがって、後ろの条件ほどゲート遅延が深くなります。
-->

---

## 6. 直交選択とデコーダ構文: `switch / is`

全条件が互いに排他（直交: Orthogonal）である場合は、優先度連鎖ではなく並列デコーダを合成できる `switch / is` を使用します。

```scala
import chisel3.util.{switch, is}

val out = WireDefault(0.U(8.W))

switch(io.sel) {
  is(0.U) { out := io.in0 }
  is(1.U) { out := io.in1 }
  is(2.U) { out := io.in2 }
  is(3.U) { out := io.in3 }
}
```

- **特徴**: 各条件は優先度を持たず並列に評価される。
- **用途**: 状態機械 (FSM) の状態デコード、命令デコーダ、表示用セグメントデコーダなど。

<!-- Note:
switch / is 構文です。chisel3.util からインポートして使用します。
when との違いは「優先度を持たないこと」です。
入力値に対する完全な直交デコーダ（1-hotデコーダと並列MUX）が合成されるため、すべての条件で伝搬遅延が均一になります。
7セグメントデコーダの作成にはこの構文が最適です。
-->

---

## 7. 【最重要課題】未完全記述と透過ラッチの発生

### 透過ラッチ (Transparent Latch) とは
クロック信号ではなく、イネーブル信号のレベル（High/Low）によって入力データを素通し・保持する記憶回路。

<div class="box">
<b>組合せ回路における透過ラッチは重大なバグ</b><br>
デジタルシステム設計において、意図しないラッチの混入はタイミング解析ツール（STA）の破綻、予期せぬグリッチのラッチ、レースコンディションの原因となります。
</div>

### ラッチが生まれるメカニズム（未完全記述）
もし `when` の条件が `false` であり、かつ `otherwise`（デフォルト値）が定義されていない場合：
> 「条件が偽のとき、この信号線は何を出力すべきか？」  
> $\implies$ 「前の値をそのまま維持し続けなければならない！」  
> $\implies$ **物理的なラッチ（記憶回路）が意図せず合成されてしまう！**

<!-- Note:
デジタル回路設計で最も恐ろしいバグの一つが、この「未完全記述による透過ラッチ」です。
プログラミング初心者が「条件に当てはまらないときは何もしない」つもりで else を省略すると、合成ツールは「値を記憶せよ」と解釈し、ラッチ回路を勝手に生成してしまいます。
クロック同期回路の中に非同期ラッチが紛れ込むと、タイミング検証が不可能になり、実機で極めて不安定な挙動を示します。
-->

---

## 8. Chiselにおけるラッチ防止策: `WireDefault`

Chiselでは、宣言時に**デフォルト値**を与えることで、未完全代入による意図しない記憶動作をコンパイルレベルで根絶します。

### 安全な記述イディオム
```scala
// 宣言と同時に初期値（デフォルト接続）を与える
val result = WireDefault(0.U(8.W))

when(io.en) {
  when(io.mode === 1.U) {
    result := io.dataA
  }
  // mode != 1 の場合でも、result には自動的に 0.U が配線される！
}
```

<div class="box">
<b>設計鉄則</b><br>
組合せ回路で使用する全ての <code>Wire</code> は、原則として <code>WireDefault(...)</code> で初期値を明示すること！
</div>

<!-- Note:
Chiselの強力な武器が WireDefault です。
Wire(UInt(8.W)) と宣言して後から条件代入するのではなく、WireDefault(0.U(8.W)) と書く習慣をつけてください。
これにより、たとえ複雑に入り組んだ when や switch の分岐の網の目をすり抜けた場合でも、必ず 0.U が出力される純粋な組合せ回路となり、透過ラッチが物理的に発生し得なくなります。
-->

---

## 9. 7セグメントLEDデコーダの構造

4ビット2進数 ($0 \sim 9$) を受け取り、7つのLEDセグメント ($a \sim g$) の点灯パターンへ変換する。

<p align="center"><img src="figures/lecture02_seven_segment.svg" width="380"></p>

### セグメント配置と真理値表の例
| 数字 | 点灯セグメント ($a \sim g$) | 7ビット値 (`abcdefg_2`) |
| :---: | :--- | :---: |
| **0** | a, b, c, d, e, f | `1111110` |
| **1** | b, c | `0110000` |
| **2** | a, b, d, e, g | `1101101` |
| **3** | a, b, c, d, g | `1111001` |
| **8** | 全セグメント点灯 | `1111111` |

<!-- Note:
本日の基本問題で設計する7セグメントLEDデコーダです。
aからgまでの7つのLED素子を点灯(1)させるか消灯(0)させるかの真理値表を作ります。
入力は4ビット（0〜15）ですが、10進表示なので10〜15の未定義値が入力された場合は、安全のために全消灯（0000000）を出力するように設計します。
-->

---

## 10. 7セグメントデコーダのChisel実装パターン

```scala
class SevenSegDecoder extends Module {
  val io = IO(new Bundle {
    val bcd = Input(UInt(4.W))
    val seg = Output(UInt(7.W))
  })

  // 1. WireDefault で未定義値（10〜15）に対する消灯パターンを定義
  val pattern = WireDefault(0.U(7.W))

  // 2. switch / is で直交パターンを展開
  switch(io.bcd) {
    is(0.U) { pattern := "b1111110".U }
    is(1.U) { pattern := "b0110000".U }
    is(2.U) { pattern := "b1101101".U }
    is(3.U) { pattern := "b1111001".U }
    // ... 4〜8 ...
    is(9.U) { pattern := "b1111011".U }
  }

  io.seg := pattern
}
```

- 意図しない透過ラッチを100%排除した安全なデコーダ記述。

<!-- Note:
実装例です。
最初に `val pattern = WireDefault(0.U(7.W))` と記述することで、bcdが10〜15のときは自動的に 0.U（消灯）が出力されます。
そして switch(io.bcd) の中で 0.U から 9.U までの点灯パターンを記述します。
この構造は非常に読みやすく、合成後も無駄なラッチを含まない高速な組合せ回路になります。
-->

---

## 11. 優先度付きエンコーダ (Priority Encoder)

### エンコーダとは
複数の入力要求線のうち、アクティブな線の番号を2進コードに変換して出力する回路。

### 優先度付き（Priority）の必要性
複数の入力線が**同時にアクティブ**になった場合、どの要求を優先すべきか？
$\implies$ 最も高い優先度（例: 最上位ビット MSB）を持つ入力を選択する。

<p align="center"><img src="figures/lecture02_priority_encoder.svg" width="580"></p>

- **用途**: プロセッサの割り込みコントローラ、バスのアービター（調停回路）。

<!-- Note:
発展問題で取り組む優先度付きエンコーダです。
普通のエンコーダは「常に入力のうち1本だけが1である」ことを前提としますが、現実のシステムでは複数のデバイスから同時に割り込み要求が来ることがあります。
その際、最優先の入力ポート番号を即座に特定してCPUに通知するのが優先度付きエンコーダの役割です。
-->

---

## 12. 4入力優先度付きエンコーダの真理値表

入力 `in[3:0]` に対して、上位ビットを優先評価する。

| $in[3]$ | $in[2]$ | $in[1]$ | $in[0]$ | $pos[1:0]$ | $valid$ | 備考 |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **1** | $\times$ | $\times$ | $\times$ | `11` | **1** | **in(3) が最優先** |
| **0** | **1** | $\times$ | $\times$ | `10` | **1** | in(2) を優先 |
| **0** | **0** | **1** | $\times$ | `01` | **1** | in(1) を優先 |
| **0** | **0** | **0** | **1** | `00` | **1** | in(0) のみアクティブ |
| **0** | **0** | **0** | **0** | `00` (任意) | **0** | リクエストなし |

- $\times$ はドントケア（0でも1でも結果に影響しない）。
- `valid` は「少なくとも1つのビットが1であるか」を示す有効フラグ。

<!-- Note:
真理値表を確認してください。
in(3) が 1 であれば、in(2)〜in(0) が何であろうと pos は 3 (11_2) になります。
全ビットが 0 のときは要求が存在しないため、valid フラグを 0 にして後段に通知します。
この階層構造をChiselの構文でどのようにモデル化するかを考えます。
-->

---

## 13. 優先度付きエンコーダのChisel記述テクニック

`when / elsewhen` の自然な優先順位を利用して直感的に記述できます。

```scala
class PriorityEncoder4 extends Module {
  val io = IO(new Bundle {
    val in    = Input(UInt(4.W))
    val pos   = Output(UInt(2.W))
    val valid = Output(Bool())
  })

  // 1. 全ビットORによる valid フラグ生成
  io.valid := io.in.orR

  // 2. WireDefault でデフォルト値を確定
  val posWire = WireDefault(0.U(2.W))

  // 3. 最上位ビットから順に優先評価
  when(io.in(3)) {
    posWire := 3.U
  }.elsewhen(io.in(2)) {
    posWire := 2.U
  }.elsewhen(io.in(1)) {
    posWire := 1.U
  }.otherwise {
    posWire := 0.U
  }

  io.pos := posWire
}
```

<!-- Note:
Chiselでのエレガントな実装パターンです。
io.in.orR を使えば、1行で「いずれかのビットが1か」を判定して valid を生成できます。
そして posWire に初期値を入れた上で、in(3) から順に when / elsewhen で判定します。
上位ビットの条件が優先されるハードウェアの特性を、プログラムコードの記述順序と美しく一致させることができます。
-->

---

## 14. 組合せ回路の静的タイミング検証と最適化

### MUXツリー vs 優先度エンコーダの遅延比較

<div style="display: flex; justify-content: space-between;">
<div style="width: 48%;">

### 平衡MUXツリー (`switch / is`)
- 段数: $O(\log_2 N)$
- 全入力に対して均一な伝搬遅延
- クリティカルパスが短く、高速クロック動作に適する

</div>
<div style="width: 48%;">

### 優先度連鎖 (`when / elsewhen`)
- 段数: $O(N)$
- 最下位条件ほどゲート段数が増大
- 条件数 $N$ が大きい場合、タイミング違反の原因になりやすい

</div>
</div>

<div class="box">
<b>設計指針</b><br>
優先順位が本質的に不要な直交選択には、必ず <code>switch / is</code> を用いて平衡ツリーを合成させること。
</div>

<!-- Note:
回路のタイミング性能についての工学的考察です。
when / elsewhen で条件を16個並べると、16段のMUXが数珠つなぎになり、最悪伝搬遅延が非常に大きくなってしまいます。
一方で switch / is を使えば、合成ツールが木構造（ツリー）を展開できるため、log2(16) = 4段の遅延で済みます。
機能的に動くだけでなく、合成されるハードウェアのトポロジーを意識することがプロの設計者への第一歩です。
-->

---

## 15. 本日の演習課題 (90分)

演習は**基本問題**と**発展問題**の2段階構成です。

### 基本問題（必須 / 目安45〜50分）
1. **`Mux4to1`**: 2-to-1 MUXの組み合わせ、またはセレクタによる4入力MUXの設計
2. **`SevenSegDecoder`**: `switch / is` と `WireDefault` を用いた透過ラッチフリーな7セグメントデコーダの設計

### 確認課題
- **`PriorityEncoder4`**:
  - 4ビット入力の最上位アクティブビット位置を出力
  - 縮約OR (`orR`) による `valid` フラグの生成

### バッファ (5〜10分)
- コード整形、テスト結果の確認、質疑応答

<!-- Note:
本日の90分演習の案内です。
まずは基本問題として、4-to-1 MUXと7セグメントデコーダを作成してください。特に7セグデコーダでは WireDefault の威力を体感してください。
完了した方は、優先度付きエンコーダの発展課題に進み、ハードウェアの優先度判定回路を完成させてください。
-->

---

## 16. 第2回 まとめ & 次回予告

### 本日の重要ポイント
1. 2-to-1 MUXはChiselの `Mux(cond, trueVal, falseVal)` で生成する。
2. `when / elsewhen` は**優先度付きMUX連鎖**を生成し、`switch / is` は**直交デコーダ**を生成する。
3. 条件分岐での未完全代入は**意図しない透過ラッチ**を引き起こす。
4. 全ての `Wire` に `WireDefault` を適用することで、ラッチの混入を完全に防止できる。

### 次回予告: 第3回「順序回路と基本カウンタ」
- 状態を記憶する回路: クロック同期設計とDフリップフロップ
- Chiselにおけるレジスタ生成 (`RegInit`) と同期リセット
- モジュロ$N$カウンタのラップアラウンド論理

<!-- Note:
第2回の講義は以上です。
組合せ回路の設計において「透過ラッチを作らない」というルールは、業界標準の最重要鉄則です。
それでは90分の演習時間に入ります。starter_kitのExercises.scalaのLab02に取り組んでください。
-->
