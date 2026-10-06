# 全8回 演習課題仕様書 (Assignment Specifications)

- **作成担当**: `04_課題作成担当`
- **授業名**: 「Computer System Laboratory」
- **演習時間**: 各回 90分（基本問題 45〜50分 + 発展問題 30〜35分 + バッファ 5〜10分）
- **構成**: 全8回の「基本問題（必須）」および「発展問題（推奨）」の詳細入出力・機能仕様

---

## 第1回: 組合せ回路の基礎 (Combinational Logic)

### 基本問題 (必須・目安45分): 1ビット半加算器 & 1ビット全加算器
- **モジュール名**: `HalfAdder` (目安20分)
  - **入力**: `a` (Bool), `b` (Bool)
  - **出力**: `sum` (Bool), `cout` (Bool)
  - **論理仕様**:
    - 和: $sum = a \oplus b$
    - 桁上げ: $cout = a \cdot b$
  - **学習目標**: Chiselにおける最も基礎的な単一モジュール定義、`IO(new Bundle { ... })` による入出力ポート定義、基本論理演算子 (`^`, `&`) の使い方を習得する。
- **モジュール名**: `FullAdder` (目安25分)
  - **入力**: `a` (Bool), `b` (Bool), `cin` (Bool)
  - **出力**: `sum` (Bool), `cout` (Bool)
  - **論理仕様**:
    - 和: $sum = a \oplus b \oplus cin$
    - 桁上げ: $cout = (a \cdot b) + (cin \cdot (a \oplus b))$
  - **学習目標**: 3入力の組合せ論理回路の実装、真理値表のChiselモデル化、論理式の適切な記述を習得する。

### 発展問題 (推奨・目安35分): 4ビット加算・パリティ検出器
- **モジュール名**: `AdderWithParity4`
  - **入力**: `a` (UInt(4.W)), `b` (UInt(4.W)), `cin` (Bool)
  - **出力**: `sum` (UInt(4.W)), `cout` (Bool), `parity` (Bool)
  - **論理仕様**:
    - サブモジュールは使用せず、単一モジュール内で実装する。
    - 講義で解説した**拡張加算 (`+&`)** を用いて、最下位キャリーイン `cin` を含む加算を行い、桁あふれを失わない5ビットの中間結果 `sumExt` を生成する。
      - `val sumExt = io.a +& io.b + io.cin.asUInt`
    - 加算結果の低位4ビットを和出力 `sum`（`sumExt(3, 0)`）、最上位5ビット目をキャリー出力 `cout`（`sumExt(4)`）として出力する。
    - 加算結果 `sum` の各ビットに対して奇数パリティを計算し、`parity` として出力する（縮約XOR演算子 `sum.xorR` を使用）。
  - **学習目標**: `UInt` 同士の多ビット演算、`+&` によるキャリー保持、ビットスライス抽出、および縮約演算子の実践的応用を習得する。

---

## 第2回: Muxとデコーダの回路 (Mux & Decoders)

### 基本問題 (必須・目安45分): 4-to-1 マルチプレクサ & 7セグメントデコーダ
- **モジュール名**: `Mux4to1`
  - **入力**: `in0` (UInt(8.W)), `in1` (UInt(8.W)), `in2` (UInt(8.W)), `in3` (UInt(8.W)), `sel` (UInt(2.W))
  - **出力**: `out` (UInt(8.W))
  - **論理仕様**: `sel` の値 (0〜3) に応じて対応する入力を選択出力。
- **モジュール名**: `SevenSegDecoder`
  - **入力**: `bcd` (UInt(4.W))
  - **出力**: `seg` (UInt(7.W))  ※ ビット配置: `seg(6)` = a, `seg(5)` = b, ..., `seg(0)` = g
  - **論理仕様**: `switch / is` を使用。`WireDefault` で透過ラッチを完全防止。0〜9のフォントパターンを点灯(1)/消灯(0)で出力。未定義値 (10〜15) は全消灯 (`0.U(7.W)`)。

### 発展問題 (推奨・目安35分): 4入力優先度付きエンコーダ
- **モジュール名**: `PriorityEncoder4`
  - **入力**: `in` (UInt(4.W))  ※ in(3) が最高優先度、in(0) が最低優先度
  - **出力**: `pos` (UInt(2.W)), `valid` (Bool)
  - **論理仕様**:
    - `valid`: いずれかのビットが `1` のとき `true.B`、全ビット `0` のとき `false.B`。
    - `pos`: 最も上位で立っているビットのインデックス (0〜3) を出力。`when / elsewhen / otherwise` または `PriorityMux` を活用。

---

## 第3回: 順序回路と基本カウンタ (Sequential Logic & Counters)

### 基本問題1 (必須・目安25分): クロック分周による 1Hz 信号生成・0.5秒 LED 点滅カウンタ
- **モジュール名**: `OneHzGenerator(clkFreq: Int = 9000000)`
  - **入力**: `en` (Bool), `clear` (Bool)
  - **出力**: `ledBlink` (Bool: 0.5秒点灯/0.5秒消灯の1Hz点滅), `oneHzPulse` (Bool: 1秒に1回、1クロック幅のストローブパルス)
  - **論理仕様**:
    - デフォルトクロック周波数は 9MHz（$9,000,000\text{Hz}$）。
    - 優先度: `clear` (最優先) $\to$ `en`。
    - `clear`: 次クロックでカウンタを 0、`ledBlink` を `false.B` に初期化。
    - `en`: カウント動作。
      - 半周期（`clkFreq / 2`）および全周期（`clkFreq`）満了時に `ledReg` を反転（トグル）。
      - 全周期満了（`cntReg === (clkFreq - 1).U`）かつ `en` 有効時に、`oneHzPulse` を 1 クロックだけ `true.B` にアサート。

### 基本問題2 (必須・目安25分): ロード・イネーブル・同期クリア付き 任意進数カウンタ
- **モジュール名**: `ModuloNCounter(n: Int, width: Int)`
  - **入力**: `en` (Bool), `clear` (Bool), `load` (Bool), `loadData` (UInt(width.W))
  - **出力**: `count` (UInt(width.W)), `rollover` (Bool)
  - **論理仕様**:
    - 優先度: `clear` (最優先) $\to$ `load` $\to$ `en`。
    - `clear`: 次クロックで `count` を 0 にリセット。
    - `load`: 次クロックで `loadData` をレジスタに格納（ただし $n$ 未満にクリップ）。
    - `en`: カウント動作。`count === (n - 1).U` の時、`rollover` を 1 クロックだけ `true.B` にし、次クロックで 0 へ復帰。それ以外は `count + 1.U`。

### 確認課題 (目安30分): デューティ比可変 PWM パルス発生器
- **モジュール名**: `PwmGenerator(periodMax: Int = 255)`
  - **入力**: `period` (UInt(8.W)), `duty` (UInt(8.W)), `en` (Bool)
  - **出力**: `pwmOut` (Bool)
  - **論理仕様**:
    - 周期カウンタ (`cnt`) が `0` から `period` までカウントアップ。
    - `cnt < duty` の期間中、`pwmOut` は `true.B` を出力し、`cnt >= duty` の期間は `false.B` を出力。
    - `en === false.B` の時はカウンタを停止し、出力は `false.B`。

---

## 第4回: RingカウンタとJohnsonカウンタ (Ring & Johnson Counters)

### 基本問題 (必須・目安45分): 自己復帰回路付き 4ビットRing & Johnsonカウンタ
- **モジュール名**: `SelfCorrectingRingCounter(width: Int = 4)`
  - **入力**: `en` (Bool)
  - **出力**: `out` (UInt(width.W))
  - **論理仕様**:
    - リセット時: `1.U(width.W)`（0001_2: 1-hot）。
    - 自己復帰論理: レジスタ値が1-hotパターン以外（PopCount $\neq$ 1、または0）の場合、次クロックで自動的に `1.U` へ強制復帰。正常時は左循環シフト。
- **モジュール名**: `JohnsonCounter(width: Int = 4)`
  - **入力**: `en` (Bool)
  - **出力**: `out` (UInt(width.W))
  - **論理仕様**:
    - リセット時: `0.U(width.W)`。
    - 最上位ビットの反転 `~out(width - 1)` を最下位ビットに結合し、左シフト。$2 \times width$ 個のユニークな状態を巡回。

### 発展問題 (推奨・目安35分): 4ビット・グレイコード・カウンタ
- **モジュール名**: `GrayCodeCounter`
  - **入力**: `en` (Bool), `clear` (Bool)
  - **出力**: `gray` (UInt(4.W)), `bin` (UInt(4.W))
  - **論理仕様**:
    - 内部に標準バイナリカウンタを保持し、バイナリからグレイコードへの変換論理 $gray = bin \oplus (bin \gg 1)$ を実装。
    - 隣接するカウント遷移において、出力 `gray` のハミング距離（変化ビット数）が常に厳密に 1 であることを保証。

---

## 第5回: 構造化設計 (Structured Hierarchical Design)

### 基本問題 (必須・目安45分): 算術・論理分離型 階層ALU
- **モジュール名**: `ArithmeticUnit`
  - **入力**: `a` (UInt(8.W)), `b` (UInt(8.W)), `op` (Bool: 0=ADD, 1=SUB)
  - **出力**: `res` (UInt(8.W)), `cout` (Bool)
- **モジュール名**: `LogicUnit`
  - **入力**: `a` (UInt(8.W)), `b` (UInt(8.W)), `op` (UInt(2.W): 0=AND, 1=OR, 2=XOR, 3=NOT a)
  - **出力**: `res` (UInt(8.W))
- **モジュール名**: `StructuredALU`
  - **入力**: `a` (UInt(8.W)), `b` (UInt(8.W)), `aluSel` (UInt(3.W))  ※ bit[2]: 0=算術, 1=論理
  - **出力**: `out` (UInt(8.W)), `carryOut` (Bool)
  - **論理仕様**: `ArithmeticUnit` と `LogicUnit` をインスタンス化し、階層間ポートをバインド。最上位選択ビットで結果を選択。

### 発展問題 (推奨・目安35分): ステータスフラグ生成 & パイプライン出力段
- **モジュール名**: `PipelinedALUWithFlags`
  - **入力**: `a` (UInt(8.W)), `b` (UInt(8.W)), `aluSel` (UInt(3.W)), `validIn` (Bool)
  - **出力**: `out` (UInt(8.W)), `zero` (Bool), `negative` (Bool), `carry` (Bool), `validOut` (Bool)
  - **論理仕様**:
    - `StructuredALU` の出力段に同期レジスタを挿入（パイプライン化）。
    - フラグ生成: `zero` (`out === 0.U`), `negative` (`out(7) === 1.B`), `carry` (算術演算時の桁上げ)。

---

## 第6回: 60進カウンタの構造化設計 (Hierarchical 60-Counter)

### 基本問題 (必須・目安45分): 同期カスケード 60進カウンタ
- **モジュール名**: `Counter60`
  - **構成**: 10進カウンタ (`ModuloNCounter(10, 4)`) と 6進カウンタ (`ModuloNCounter(6, 3)`)
  - **入力**: `en` (Bool), `clear` (Bool)
  - **出力**: `secUnits` (UInt(4.W)), `secTens` (UInt(3.W)), `cout` (Bool)
  - **論理仕様**:
    - **非同期リップルクロックの厳禁**: 共通のクロックで駆動。
    - 10進カウンタの `rollover` と外部 `en` の論理積 (`rollover10 && en`) を 6進カウンタの `en` 入力に接続。
    - `cout` は 59 (秒の9かつ10の位の5) で `en` が有効なときに `true.B` をアサート。

### 発展問題 (推奨・目安35分): デジタル時計コア（時・分・秒 統合階層モジュール）
- **モジュール名**: `DigitalClockCore`
  - **入力**: `enSec` (Bool: 1Hzパルス), `clear` (Bool)
  - **出力**: `sec` (UInt(6.W)), `min` (UInt(6.W)), `hour` (UInt(5.W))
  - **論理仕様**:
    - 秒(60進) $\to$ 分(60進) $\to$ 時(24進: 0〜23) の3段カスケード接続。
    - 各桁上げ信号が同期イネーブルとして正しく伝播し、23時59分59秒の次で 00:00:00 に同期リセットされる回路。

---

## 第7回: 有限状態機械 (Finite State Machines: FSM)

### 基本問題 (必須・目安45分): Moore型 "101" パターン検出器
- **モジュール名**: `SequenceDetector101`
  - **状態定義**: `ChiselEnum` を使用 (`sIDLE`, `s1`, `s10`, `s101`)
  - **入力**: `in` (Bool)
  - **出力**: `detected` (Bool)
  - **論理仕様**:
    - クロック毎にシリアルビット `in` が入力され、直近の3ビットが "1-0-1" と合致した瞬間に `detected` を `true.B` にする。
    - 重複検出（例: "10101" $\to$ 2回検出）をサポート。
    - グリッチフリー設計のため、出力 `detected` は状態レジスタのみから生成（Moore型）。

### 発展問題 (推奨・目安35分): 歩行者割り込み付き 交通信号機コントローラ
- **モジュール名**: `TrafficLightController`
  - **入力**: `pedestrianButton` (Bool)
  - **出力**: `mainLight` (UInt(2.W): 0=Green, 1=Yellow, 2=Red), `pedLight` (Bool: 0=Red, 1=Green)
  - **論理仕様**:
    - 通常状態: 車道青 (`mainLight=Green`), 歩行者赤 (`pedLight=Red`)。
    - 歩行者ボタンが押されるとリクエストをラッチし、一定サイクル後に車道黄 $\to$ 車道赤/歩行者青 $\to$ 歩行者点滅/赤 $\to$ 車道青へと安全に遷移するFSM。

---

## 第8回: メモリ回路と非同期読み出し (Memory Circuits & Asynchronous Read)

### 基本問題 (必須・目安45分): 2R1W 非同期読み出しレジスタファイル (8ワード×8ビット)
- **モジュール名**: `RegisterFile8x8`
  - **入力**:
    - `raddr1` (UInt(3.W)), `raddr2` (UInt(3.W))
    - `wen` (Bool), `waddr` (UInt(3.W)), `wdata` (UInt(8.W))
  - **出力**:
    - `rdata1` (UInt(8.W)), `rdata2` (UInt(8.W))
  - **論理仕様**:
    - `Mem(8, UInt(8.W))` を使用。
    - 書き込みはクロック同期 (`when(wen) { mem(waddr) := wdata }`)。
    - 読み出しはアドレス入力から同一サイクル（無遅延）でデータが出力される非同期読み出し (`mem(raddr)`）。

### 発展問題 (推奨・目安35分): RAWバイパス（フォワーディング）回路付き レジスタファイル
- **モジュール名**: `BypassedRegisterFile`
  - **入力・出力**: `RegisterFile8x8` と同等
  - **論理仕様**:
    - 通常の非同期読み出しでは、同一サイクル・同一アドレスに対して「書き込み中データ」と「読み出しデータ」が競合した際に未定義や旧データが読まれるリスクがある。
    - 書き込みアドレスと読み出しアドレスが一致し、かつ `wen` が有効な場合、メモリ配列を通さずに `wdata` を直接読み出しポートへ転送（フォワーディング）するマルチプレクサ回路を追加。
