---
marp: true
theme: default
paginate: true
header: "第7回: 有限状態機械 (FSM)"
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

# 第7回: 有限状態機械 (Finite State Machines: FSM)
### 〜Moore型FSMとChiselEnumによる型安全な制御論理設計〜

<br>

- **資料作成者**: Hideaki YANAGISAWA
- **講義時間**: 60分
- **対象**: デジタル論理回路基礎習得者
- **本日のアジェンダ**:
  1. 順序回路の最高峰: 有限状態機械 (FSM) の工学的役割
  2. FSMの3大構成要素（状態レジスタ、次状態論理、出力論理）
  3. **Moore型 vs Mealy型の本質的相違**（出力のタイミングとグリッチ耐性）
  4. なぜ高信頼システムではMoore型FSMが強く推奨されるのか？
  5. Chiselにおける型安全な状態定義: `ChiselEnum`
  6. 3プロセス分離記述スタイル（状態保持・遷移判定・出力デコード）
  7. 実践例: 連続ビットパターン "101" シーケンス検出器
  8. 本日の演習課題（基本: 101検出器 / 発展: 交通信号機コントローラ）

<!-- Note:
第7回の講義へようこそ。
これまでに学んだ組合せ回路と順序回路の集大成として、デジタルシステムの「頭脳（制御部）」を司る有限状態機械（FSM: Finite State Machine）を学びます。
プロセッサの制御ユニット、通信プロトコルのハンドシェイク、自動販売機や信号機など、あらゆる制御回路はFSMとして設計されます。
特に、実務で最重要となる「Moore型とMealy型の違い」および「ChiselEnumによる型安全な記述手法」をマスターします。
-->

---

## 1. 有限状態機械 (FSM) の工学的役割

### デジタルシステムを構成する2大要素
1. **データパス (Datapath)**: ALU、レジスタファイル、乗算器など、実際にデータを加工・演算・転送する回路網。
2. **制御パス (Control Unit / Controller)**: データパスの各部に対して「いつ、どの演算を行い、どこへ書き込むか」のイネーブル信号やMUXセレクタを指示する回路網。

$$\implies \mathbf{制御パスの本質こそが\ FSM\ である！}$$

<p align="center">
  <img src="figures/lecture07_fsm_datapath.svg" width="680">
</p>

<!-- Note:
FSMの役割についての解説です。
コンピュータや専用LSIは、計算を行う「データパス」と、それを指揮する「制御部」に分かれます。
データパスが筋肉だとすれば、FSMはまさに「脳」です。
現在の状況（状態）を記憶し、外部からの入力やデータパスからのフラグを見て、次の行動を決定して制御信号を発行します。
-->

---

## 2. FSMの基本アーキテクチャ

すべてのFSMは、数学的に以下の3つのブロックで厳密に構成されます。

<p align="center">
  <img src="figures/lecture07_fsm_architecture.svg" width="680">
</p>

1. **状態レジスタ (State Register)**: 現在の内部状態 $S_t$ を保持するクロック同期D-FF群。
2. **次状態論理 (Next-State Logic)**: 現在状態 $S_t$ と入力 $X_t$ から、次のクロックでの状態 $S_{t+1}$ を決定する組合せ論理回路。
3. **出力論理 (Output Logic)**: 現在の状態（および入力）から外部への制御信号 $Y_t$ を生成する組合せ論理回路。

<!-- Note:
FSMの構成ブロック図です。
この3つの要素への分解は極めて重要です。
状態レジスタだけが同期回路であり、次状態論理と出力論理は純粋な組合せ論理回路です。
コードを書く際も、この3つのブロックを意識して綺麗に分離して記述することで、バグのない美しいハードウェアになります。
-->

---

## 3. 【最重要比較】Moore型 vs Mealy型

FSMには、出力論理の構造の違いによって2つの流派が存在します。

<p align="center"><img src="figures/lecture07_moore_vs_mealy.svg" width="680"></p>

- **Moore（ムーア）型 FSM 【推奨】**: 出力が**「現在の状態レジスタのみ」**に依存 ($Y_t = g(S_t)$)。入力信号のグリッチが出力へ突き抜けず、極めて安定。
- **Mealy（ミーリー）型 FSM 【要注意】**: 出力が**「状態レジスタと入力の両方」**に直接依存 ($Y_t = g(S_t, X_t)$)。入力のノイズや過渡状態が出力ピンへ直通するリスクあり。

<!-- Note:
Moore型とMealy型の違いです。
教科書では「Mealy型の方が状態数を1つ減らせることがある」と書かれますが、実際のハードウェア設計現場では圧倒的にMoore型が推奨されます。
なぜなら、Mealy型は入力ピンと出力ピンの間に直接の組合せパスが存在するため、外部入力のノイズやヒゲ（グリッチ）がそのままシステム全体の制御出力に筒抜けになってしまうからです。
-->

---

## 4. なぜ高信頼システムではMoore型が推奨されるのか？

### 1. 入出力アイソレーション（グリッチフリー出力）
Moore型では、入力信号は必ず「状態レジスタ」という同期壁で一旦遮断されます。
- 入力信号にどんな激しいグリッチが乗っていても、クロックのセットアップ時間を満たしていれば、出力にグリッチが現れることは物理的にありません。

### 2. タイミング設計（クロック周波数向上）の容易性
- **Mealy型**: 前段の回路遅延 ＋ 次状態・出力論理遅延 ＋ 後段の回路遅延が1サイクル内で合算され、クリティカルパスが長大化しやすい。
- **Moore型**: 出力パスがレジスタから始まるため、遅延の見積もりと最適化が極めて容易。

<div class="box">
<b>設計推奨</b><br>
本講義および実務のシステム設計では、原則として<b>Moore型FSM</b>を採用すること！
</div>

<!-- Note:
Moore型を推奨する工学的理由です。
Mealy型を多段に接続すると、Aの組合せ回路、Bの組合せ回路、Cの組合せ回路がレジスタを介さずに直列に繋がってしまい、クロック周波数がガタ落ちします。
一方、Moore型であればレジスタが出力をガードしているため、タイミング収束が極めて綺麗に行えます。
-->

---

## 5. Chiselにおける型安全な状態定義: `ChiselEnum`

従来は整数定数（0, 1, 2...）で状態を番号付けしていましたが、番号の重複や範囲外代入の危険がありました。
Chiselでは、**`ChiselEnum`** を用いて状態を厳格に型付けします。

```scala
import chisel3.util.ChiselEnum

// 状態列挙型の定義
object State extends ChiselEnum {
  val sIDLE, sACTIVE, sDONE, sERROR = Value
}

// 状態レジスタの生成 (初期値: sIDLE)
val stateReg = RegInit(State.sIDLE)
```

- **メリット**:
  - `stateReg := 99.U` のような不正な代入はコンパイルエラーとして即座に検出。
  - 最適なビット幅（4状態なら自動的に2ビット）がコンパイラにより推論される。
  - シミュレーション波形上で、数値ではなく `sIDLE` や `sACTIVE` などの名前で直接波形表示できる！

<!-- Note:
ChiselEnum の紹介です。
ハードウェア記述において列挙型が使えることの恩恵は絶大です。
C言語のenumと同様に、状態に名前をつけることができます。
間違えて関係のないUIntの値を代入しようとすると、コンパイラが型不一致エラーを出してくれます。
また波形ビューア（GTKWave等）でも、0や1の数字ではなく「sIDLE」「sACTIVE」というシンボル名で表示されるため、デバッグ効率が何倍にも跳ね上がります。
-->

---

## 6. FSMの3プロセス分離記述イディオム

Chiselで推奨される最も美しくバグの出ないFSM記述スタイルです。

```scala
// 1. 状態レジスタの保持 (同期部)
val stateReg = RegInit(State.sIDLE)

// 2. 次状態論理 (組合せ部) - switch / is で網羅的に記述
switch(stateReg) {
  is(State.sIDLE) {
    when(io.start) { stateReg := State.sACTIVE }
  }
  is(State.sACTIVE) {
    when(io.finish) { stateReg := State.sDONE }
  }
  is(State.sDONE) {
    stateReg := State.sIDLE
  }
}

// 3. 出力論理 (Moore型: 状態レジスタのみからデコード)
io.busy := stateReg === State.sACTIVE
io.done := stateReg === State.sDONE
```

<!-- Note:
FSMの標準コーディングテンプレートです。
1. レジスタ宣言、2. switch(stateReg) による次状態遷移の記述、3. 状態レジスタに基づいた出力の割り当て、という3ステップで書きます。
switch文の中で各状態における行き先を明確にし、出力は状態との比較式で書きます。
このスタイルを守るだけで、未完全代入のラッチが発生せず、100%綺麗なMoore型FSMが組み上がります。
-->

---

## 7. 題材: "101" パターンシーケンス検出器

入力ピン `in` からシリアルに流れてくるビット列を監視し、直近の3ビットが `"1-0-1"` と合致した瞬間に `detected` フラグを立てる。

### 入出力波形例
```
CLK      : ──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──
             └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘  
in       : ───[ 1 ]─[ 0 ]─[ 1 ]─[ 0 ]─[ 1 ]─[ 1 ]───────
state    : ──X IDLE X  s1  X s10  X s101 X s10  X s101 X
detected : ───────────────────────|‾‾‾‾‾‾|──────|‾‾‾‾‾‾| (2回検出)
```

- **オーバーラップ（重複）検出**: `"1-0-1-0-1"` のように、直前の "1" を次のパターンの先頭として再利用する動作をサポートしなければならない。

<!-- Note:
本日の基本問題「101シーケンス検出器」の仕様です。
シリアル通信のプリアンブル検出やパケット開始同期で必須となる回路です。
ポイントは「オーバーラップ検出」です。
101が検出された後、次のビットが0、その次が1と来たら、最初の101の末尾の1が次の101の先頭を兼ねるため、もう一度検出パルスを出さなければなりません。
状態遷移図を正しく描けるかが勝負です。
-->

---

## 8. "101" 検出器の状態遷移図 (State Transition Diagram)

4つの状態を定義します。

<p align="center">
  <img src="figures/lecture07_fsm_state_diagram.svg" width="680">
</p>

- **`sIDLE`**: 初期状態。何も検出していない。
- **`s1`**: 直前のビットが "1"。
- **`s10`**: 直前の2ビットが "10"。
- **`s101`**: 直前の3ビットが "101"（合致！ 出力=1）。

<!-- Note:
状態遷移図の詳細です。
各状態で、入力が0のときどこへ行くか、1のときどこへ行くかを必ず両方定義します。
特に s101 の状態から、次に1が来たときは s1 へ戻り、0が来たときは直前が "10" だったとみなして s10 へ戻ります。
この遷移を漏れなく埋めることで、重複シーケンスが完璧に検出されます。
-->

---

## 9. "101" シーケンス検出器の実装コード

```scala
class SequenceDetector101 extends Module {
  val io = IO(new Bundle {
    val in       = Input(Bool())
    val detected = Output(Bool())
  })

  object State extends ChiselEnum {
    val sIDLE, s1, s10, s101 = Value
  }

  val stateReg = RegInit(State.sIDLE)

  switch(stateReg) {
    is(State.sIDLE) {
      when(io.in) { stateReg := State.s1 }
    }
    is(State.s1) {
      when(!io.in) { stateReg := State.s10 }
    }
    is(State.s10) {
      when(io.in) { stateReg := State.s101 }.otherwise { stateReg := State.sIDLE }
    }
    is(State.s101) {
      when(io.in) { stateReg := State.s1 }.otherwise { stateReg := State.s10 }
    }
  }

  // Moore型出力 (現在の状態のみから生成)
  io.detected := stateReg === State.s101
}
```

<!-- Note:
完全な実装コードです。
ChiselEnum で4つの状態を定義し、switch / is 文で状態遷移を記述しています。
そして最後の行、`io.detected := stateReg === State.s101` に注目してください。
出力は状態レジスタ stateReg が s101 かどうかだけを見ています。入力 io.in は出力式に直接現れません。
これが真のMoore型FSMであり、グリッチフリーな検出パルスを約束します。
-->

---

## 10. 発展課題: 交通信号機コントローラ (Traffic Light Controller)

現実の組込み制御システムを模した実践的FSM。

```
 [ 車道側信号: mainLight ] ──> Green (青) / Yellow (黄) / Red (赤)
 [ 歩行者信号: pedLight  ] ──> Red (赤)   / Green (青)
 [ 歩行者ボタン: pedBtn  ] ──> 割り込み要求入力
```

### システム要件
1. **通常時**: 車道が青 (`Green`)、歩行者は赤 (`Red`) を維持。
2. **歩行者ボタン押下**: リクエストを内部レジスタに記憶（ラッチ）。
3. **安全確保タイマ**: 車道青が最低10サイクル継続した後に、車道黄 $\to$ 車道赤/歩行者青 へと遷移。
4. **歩行者横断**: 歩行者青を8サイクル維持した後、歩行者赤 $\to$ 車道青へ復帰。

<!-- Note:
発展課題の交通信号機コントローラです。
信号機の制御は、世界中の工学部でFSMの最高の実践例として教えられています。
ボタンが押されたという非同期なイベントをラッチし、内部のタイマカウンタと連動しながら、絶対に「車道も歩行者も同時に青」にならない安全な状態遷移を設計します。
-->

---

## 11. 信号機FSMの状態遷移設計

4つの安全状態を定義し、タイマカウンタ (`timerReg`) と連動させる。

<p align="center">
  <img src="figures/lecture07_traffic_light_fsm.svg" width="680">
</p>

<!-- Note:
信号機FSMの状態遷移図です。
状態遷移のトリガーとして、外部入力だけでなく「タイマが満了したか」を使います。
FSMの中に小さなカウンタを同居させ、状態が変わるたびにタイマをゼロクリアして時間を測るという、実務で頻出するデザインパターンを学びます。
-->

---

## 12. 信号機コントローラの実装コード骨格

```scala
class TrafficLightController extends Module {
  val io = IO(new Bundle {
    val pedestrianButton = Input(Bool())
    val mainLight        = Output(UInt(2.W)) // 0: Green, 1: Yellow, 2: Red
    val pedLight         = Output(Bool())    // 0: Red, 1: Green
  })

  object LightState extends ChiselEnum {
    val sVehGreen, sVehYellow, sVehRedPedGreen, sVehRedPedYellow = Value
  }

  val stateReg  = RegInit(LightState.sVehGreen)
  val timerReg  = RegInit(0.U(4.W))
  val pedReqReg = RegInit(false.B)

  // 1. ボタン押下要求のラッチ
  when(io.pedestrianButton) { pedReqReg := true.B }
  timerReg := timerReg + 1.U

  // 2. タイマ連動の状態遷移論理
  switch(stateReg) {
    is(LightState.sVehGreen) {
      when(pedReqReg && timerReg >= 10.U) {
        stateReg := LightState.sVehYellow; timerReg := 0.U
      }
    }
    is(LightState.sVehYellow) {
      when(timerReg >= 3.U) {
        stateReg := LightState.sVehRedPedGreen; timerReg := 0.U; pedReqReg := false.B
      }
    }
    // ... 後続遷移 ...
  }
}
```

<!-- Note:
信号機コントローラの骨格コードです。
ボタンが押されたら pedReqReg を true にし、歩行者青になったタイミングで pedReqReg をクリアしています。
タイマのリセット `timerReg := 0.U` も状態遷移と同時に行っています。
このように、FSMは単なる状態変化だけでなく、データパスやタイマと密接に対話しながらシステム全体をドライブします。
-->

---

## 13. FSMのエンコーディング方式（バイナリ vs 1-hot）

ハードウェア合成時、FSMの各状態はどのようなビット列にエンコードされるか？

| 方式 | 4状態の表現例 | 必要FF数 | 特徴と用途 |
| :--- | :---: | :---: | :--- |
| **Sequential (バイナリ)** | `00`, `01`, `10`, `11` | $\lceil \log_2 N \rceil$ | FF数を最小化。状態数が多い場合に有利。 |
| **One-Hot (ワンホット)** | `0001`, `0010`, `0100`, `1000` | $N$ | **次状態デコードが超高速**。FPGA設計の業界標準。 |
| **Gray Code** | `00`, `01`, `11`, `10` | $\lceil \log_2 N \rceil$ | 隣接状態遷移で1ビットのみ反転。低ノイズ。 |

- Chiselコンパイラ / 合成ツールがターゲット（FPGA / ASIC）に応じて最適なエンコーディングを自動選択する。

<!-- Note:
FSMの内部エンコーディングについての知識です。
私たちが ChiselEnum で抽象的に状態を書いた後、合成ツールはそれをバイナリにするか、ワンホットにするかを選択します。
FPGAにはFFが大量にあるため、デコード論理が軽くなって周波数が上がるワンホットエンコードが好まれます。
言語レベルで数値を直書きせず、ChiselEnum を使っておくことで、合成ツールが自由に最適なエンコードを選べるようになります。
-->

---

## 14. FSMデバッグの勘所: ハングアップ防止

FSM設計で最も警戒すべきバグは**「状態遷移のデッドロック（行き止まり）」**です。

<div class="box">
<b>FSMセーフティ設計チェックリスト</b><br>
1. <b>全条件の網羅</b>: <code>when</code> に合致しなかった場合、どこに留まるか（あるいは遷移するか）が明確か？<br>
2. <b>デフォルト遷移</b>: 想定外の事態に備え、未定義状態からは <code>sIDLE</code> へ戻る復帰パスが存在するか？<br>
3. <b>タイムアウト機構</b>: 外部からの応答（例: 通信Ack）を待つ状態で、永久待ちにならないタイマガードが備わっているか？
</div>

<!-- Note:
FSMデバッグの心得です。
実機でシステムが完全に固まる原因の9割は、FSMのデッドロックです。
外部からのハンドシェイク信号を待っている状態で、相手が故障して信号が来なくなると、FSMがその状態で永久に立ち往生してしまいます。
実務では、必ずタイマによるタイムアウト脱出経路を設けるのが鉄則です。
-->

---

## 15. 本日の演習課題 (90分)

演習は**基本問題**と**発展問題**の2段階構成です。

### 基本問題（必須 / 目安45〜50分）
- **`SequenceDetector101`**:
  - `ChiselEnum` を用いた4状態の定義
  - 重複パターン（"10101" 等）を正確に認識するMoore型遷移
  - グリッチのない `detected` パルス出力の検証

### 発展問題（推奨 / 目安30〜35分）
- **`TrafficLightController`**:
  - 歩行者割り込みボタンのラッチとタイマ連動遷移
  - 4つの信号状態（車道青 $\to$ 黄 $\to$ 赤/歩青 $\to$ 赤/歩黄 $\to$ 車青）の完全巡回

### バッファ (5〜10分)
- コード整形、テスト実行、波形確認

<!-- Note:
演習の案内です。
まずは基本問題の101検出器を完成させてください。シミュレーション波形で、10101が入ったときにパルスが2回ちゃんと出るかを確認します。
続いて発展問題の信号機コントローラに挑み、タイマと割り込みが連動した本格的なステートマシンを完成させましょう。
-->

---

## 16. 第7回 まとめ & 次回予告

### 本日の重要ポイント
1. FSMは「状態レジスタ」「次状態論理」「出力論理」の3要素からなる制御の中核。
2. **Moore型FSM**は出力が状態レジスタのみに依存するため、**グリッチフリーで高周波動作**に適する。
3. `ChiselEnum` を用いることで、型安全で波形視認性の高いステートマシンを記述できる。
4. タイマやフラグラッチと連携させることで、高度なシーケンス制御を実現できる。

### 次回予告: 第8回（最終回）「メモリ回路と非同期読み出し」
- ハードウェアにおける記憶素子の階層: レジスタ群 vs メモリ
- Chiselの `Mem`（非同期読み出し）と `SyncReadMem`（同期読み出し）
- プロセッサの要: レジスタファイル (Register File) の設計

<!-- Note:
第7回の講義は以上となります。
FSMを自由に設計できるようになれば、一人前のデジタル回路設計者と言えます。
次回はいよいよ最終回、メモリとレジスタファイルを扱います。
それでは演習に移ります。starter_kitのExercises.scalaのLab07を開いてください。
-->
