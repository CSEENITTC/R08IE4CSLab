# 全8回 演習ヒント集 (Engineering Hints)

- **作成担当**: `04_課題作成担当`
- **授業名**: 「Computer System Laboratory」
- **構成**: 全8回の「基本問題」および「発展問題」に対する技術的ヒントと注意点

---

## 第1回: 組合せ回路の基礎

### 基本問題ヒント (`HalfAdder`, `FullAdder`)
1. **半加算器のブール論理式**: 和 $sum$ は排他的論理和 (`a ^ b`)、桁上げ $cout$ は論理積 (`a & b`) です。
2. **全加算器のブール論理式**: 和 $sum$ は3入力の排他的論理和 (`a ^ b ^ cin`)、桁上げ $cout$ は多数決論理 (`(a & b) | (cin & (a ^ b))`) です。
3. **信号接続の注意点**: 出力ポートへの接続はすべて `:=` 演算子を使用します（単なる代入 `=` ではなく物理配線の接続）。

### 発展問題ヒント (`AdderWithParity4`)
1. **多ビット加算とビット幅の拡張**: 4ビット同士を加算すると結果は最大5ビットになります。Chiselの拡張加算 `+&` を使うことで、最上位キャリーを切り捨てずに5ビット幅の `UInt` として保持できます。
   - `val sumExt = io.a +& io.b + io.cin.asUInt` （`cin` は `Bool` なので `.asUInt` で型を合わせます）
2. **ビットスライスによる和とキャリーの分離**:
   - 低位4ビット `sumExt(3, 0)` が加算結果の `sum` となります。
   - 最上位の5ビット目 `sumExt(4)` が桁上げ `cout`（`Bool`）となります。
3. **パリティ生成と縮約演算子 (`xorR`)**:
   - `sum` の全ビットのXORを取ることで、1の個数が奇数であるかを1行で判定できます（`val parity = io.sum.xorR`）。
   - サブモジュール化は行わず、単一モジュール内で直感的に記述できます。

---

## 第2回: Muxとデコーダの回路

### 基本問題ヒント (`Mux4to1`, `SevenSegDecoder`)
1. **透過ラッチの撲滅 (`WireDefault`)**: `SevenSegDecoder` では出力 `seg` のデフォルト値を必ず宣言時に `WireDefault(0.U(7.W))` で指定してください。これにより `switch / is` で記述されなかった入力値（10〜15等）に対しても明示的に値が定まり、意図しないラッチ生成を完全に防止できます。
2. **2-to-1 MUXの木構造**: `Mux4to1` は `Mux(sel(1), Mux(sel(0), in3, in2), Mux(sel(0), in1, in0))` のように2-to-1 MUXを階層的に組み合わせることで簡潔に記述できます。

### 発展問題ヒント (`PriorityEncoder4`)
1. **優先順位の表現**: `when / elsewhen / otherwise` は上から順に優先評価されるハードウェア（優先度マルチプレクサ連鎖）を生成します。最高位ビット `in(3)` から順に `when(in(3)) { pos := 3.U }` と判定していくことで、自然に優先度エンコーダが合成されます。
2. **有効信号 (`valid`)**: 入力の全ビット論理和 (`in.orR`) を用いると、1行で簡潔かつグリッチの少ないフラグ生成が可能です。

---

## 第3回: 順序回路と基本カウンタ

### 実機実装課題ヒント（課題1 & 課題2: TeCボード配線）
1. **負論理LED駆動**:
   - TeCボード上のLED（`RUN_LED`, `C_LED`, `DATA_LED`）はカソード側を駆動する**負論理仕様（0で点灯）**です。
   - 回路側の出力は負論理出力のまま配線して問題ありません（`ModuloNCounter` で記述した `~modReg` や `~ledReg` により、正論理の数値・状態がそのままLEDの点灯として直感的に表示されます）。
2. **課題1 (`OneHzGenerator`) 配線の要点**:
   - 入力: `io_en` を `RUN_SW` (`P16` | PULLUP)、`io_clear` を `RESET_SW` (`P120` | PULLUP) に接続します。
   - 出力: `io_ledBlink` を `RUN_LED` (`P43`)、`io_oneHzPulse` を `C_LED` (`P46`) に接続します。
   - クロック: `clock` を `CLK_IN` (`P55`) に接続します。
   - `RUN_SW` をオンにすると `RUN_LED` が0.5秒点灯/0.5秒消灯の1Hz周期で点滅し、1秒に1回 `C_LED` が一瞬パルス点灯します。
3. **課題2 (`ModuloNCounter`) 配線の要点**:
   - 入力: `io_en` を `RUN_SW` (`P16` | PULLUP)、`io_clear` を `RESET_SW` (`P120` | PULLUP) に接続します。
   - 出力: `io_count[3:0]` を `DATA_LED[3:0]` (`P62, P61, P59, P58`)、`io_runLED` を `RUN_LED` (`P43`) に接続します。
   - クロック: `clock` を `CLK_IN` (`P55`) に接続します。
   - `RUN_SW` をオンにすると `RUN_LED` が点滅しながら、`DATA_LED` が1秒ごとに $0 \to 1 \to \dots \to 9$ とバイナリカウントアップ表示されます。

### 基本問題1ヒント (`OneHzGenerator`)
1. **クロック分周の考え方**: 9MHz（$9,000,000\text{Hz}$）から0.5秒（$4,500,000$ サイクル）および1秒（$9,000,000$ サイクル）を作るには、$\lceil \log_2(9,000,000) \rceil = 24$ ビットのカウンタを用意します。`chisel3.util.log2Ceil` を使うと自動計算できます。
2. **0.5秒トグルと1秒パルス**: カウンタ（`cntReg`）の周期巡回論理と、LEDレジスタ（`ledReg`）の反転論理を独立した `when` ブロックに分けて記述します。半周期（`halfPeriod - 1`）と全周期（`fullPeriod - 1`）で `ledReg := !ledReg` と反転させることで0.5秒点灯/0.5秒消灯の綺麗な1Hz点滅波形が得られます。また全周期満了の瞬間に1クロック幅だけ `oneHzPulse := io.en && (cntReg === (fullPeriod - 1).U)` をアサートします。

### 基本問題2ヒント (`ModuloNCounter`)
1. **優先度構造と安全設計**: 制御入力は安全最優先の `clear` と通常動作の `en` の2つに絞り込み、`when(io.clear) ... .elsewhen(modEn) ...` の順序で構成します。
2. **1秒ストローブパルスの生成**: `OneHzGenerator` と同様に、9MHzのシステムクロックをカウントする内部カウンタ `cntReg` を設け、全周期満了（`cntReg === (fullPeriod - 1).U`）かつ `io.en` 有効時に `modEn := true.B` となるパルス信号を作ります。
3. **端数検出とラップアラウンド**: $N$ 進カウンタ `modReg` は $0$ から $N-1$ までの $N$ 個の値を数えます。リセット条件は `modReg === (n - 1).U` です。
4. **カウンタ更新と出力論理の分離**: レジスタの更新条件（`cntReg`, `modReg`, `ledReg` の書き換え）と、出力ポートへのドライブ（`io.runLED := ~ledReg`, `io.count := ~modReg`）を明確に分けて記述してください。負論理 `~` の反転を忘れずに適用します。

### 確認課題ヒント (`PwmGenerator`)
1. **各入力信号の意味**:
   - `io.period`: カウンタ `cntReg` がどこまで数えたら `0` に戻るかを表す**カウント上限値**です。`0` から `period` まで数えるため、**1周期は `period + 1` クロック**になります（例: `period = 4` なら `0, 1, 2, 3, 4` の5クロック周期）。
   - `io.duty`: 1周期のうち**何クロック間だけ `pwmOut` を High (`true.B`) にするか**を表す閾値です。`cntReg < io.duty` の間だけ High になります（例: `duty = 2` なら `cntReg` が `0` と `1` の2クロック間だけ High）。
   - `io.en`: 回路全体の動作許可信号です。`false.B` のときはカウンタを `0.U` に戻し、出力 `pwmOut` も `false.B` に強制遮断します。
2. **周期カウンタと比較器の記述パターン**:
   - `when(io.en)` の中で、`cntReg >= io.period || cntReg >= periodMax.U` なら `cntReg := 0.U`、それ以外は `cntReg := cntReg + 1.U` と更新します（`otherwise` では `cntReg := 0.U`）。
   - 出力は `io.pwmOut := io.en && (cntReg < io.duty) && (io.duty > 0.U)` と記述することで、`duty === 0.U`（0%消灯）や `duty > period`（100%常時点灯）、`en === false.B`（安全停止）のすべての境界ケースを1行で正確に満たせます。

---

## 第4回: RingカウンタとJohnsonカウンタ

### 基本問題ヒント (`RingCounter`, `JohnsonCounter`)
1. **1秒ストローブパルスの生成**: 第3回と同様に、9MHzクロックを `cntReg` で数え、`enRingCounter := io.en && (cntReg === (fullPeriod - 1).U)`（Johnsonでは `enJohnsonCounter`）の1クロック幅パルスで状態レジスタ `state` を更新します（テスト時は `clkFreq = 4` が渡されます）。
2. **Ringカウンタの循環シフト**: 初期値および `io.clear` 時の値を `1.U(width.W)`（`0001_2`）とし、`enRingCounter` 有効時に下位ビット `state(width-2, 0)` を上位側へシフトしつつ、最上位ビット `state(width-1)` を最下位ビットへ帰還（`Cat(state(width-2, 0), state(width-1))`）させることで、`0001` $\to$ `0010` $\to$ `0100` $\to$ `1000` の4つの1-hot状態を巡回します。
3. **Johnsonカウンタの反転帰還**: 初期値および `io.clear` 時の値を `0.U(width.W)` とし、`enJohnsonCounter` 有効時に最上位ビットの反転 `~state(width-1)` を下位ビット `state(width-2, 0)` と結合（`Cat(state(width-2, 0), ~state(width-1))`）して8状態を巡回させます。

### 発展問題ヒント (`GrayCodeCounter`)
1. **変換式の簡潔性と負論理出力**: 1秒ストローブ `enGrayCodeCounter` で内部バイナリレジスタ `binReg` を `+1.U` し、バイナリからグレイコードへの変換 `val gray = binReg ^ (binReg >> 1)` を行います。出力は実機LEDに合わせて `io.gray := ~gray`, `io.bin := ~binReg`, `io.runLED := ~ledReg` とします。
2. **非同期ハザード耐性**: グレイコードは隣接する遷移で1ビットしか変化しない（ビット反転 `~` してもハミング距離は 1 のまま）ため、非同期回路との境界やセンサ読み取りでグリッチを生じない特長があります。

---

## 第5回: 60進カウンタの構造化設計

### 基本問題ヒント (`assignment1`: `Counter60` — FPGAボードで動作確認)
1. **3つのサブモジュールのインスタンス化と出力接続**:
   ```scala
   val oneHz = Module(new OneHzGenerator(clkFreq))
   val u10   = Module(new ModuloNCounter(10, 4))
   val u6    = Module(new ModuloNCounter(6, 3))
   ```
   `io.runLED := oneHz.io.ledBlink` を接続し、FPGAボード上の `RUN_LED` で0.5秒反転動作を確認できるようにします。
2. **絶対禁止事項（非同期クロック）**: `OneHzGenerator` の出力や 10進カウンタの桁上げ出力を、次段カウンタの `clock` 入力に接続してはいけません（クロックスキューやグリッチ誤動作の原因）。
3. **同期イネーブル伝播と実機確認**: `oneHz.io.oneHzPulse` を `u10.io.en` に接続し、`u10.io.rollover` を `u6.io.en` に接続します。`sbt run` でSystemVerilogを生成し、`Counter60.ucf` を用いてFPGAボード（TeCボード）にプログラムして実機動作を確認します。

### 確認課題ヒント (`assignment2`: `DigitalClockCore` — シミュレーションで動作確認)
1. **階層カスケードの連鎖**:
   - 秒の桁上げ: `io.enSec && secCounter.io.rollover` $\to$ 分カウンタのイネーブルへ
   - 分の桁上げ: `io.enSec && secCounter.io.rollover && minCounter.io.rollover` $\to$ 時カウンタのイネーブルへ
2. **長時間シミュレーションの設定 (`DigitalClockCoretb.scala`)**:
   - `dut.clock.setTimeout(0)` でサイクル数のタイムアウトを解除します。
   - 約99万サイクル（$999 \times 999$）の実行でメモリ不足にならないよう、`WriteVcdAnnotation` を削除します（長時間の波形保存は大量のメモリを使います）。
   - コンソール出力は実際の内容に合わせて `Simulated time` と表示し、23時59分59秒から 0:0:0 への巡回を確認します。

---

## 第6回: 構造化設計とALU

### 基本問題ヒント (`StructuredALU`)
1. **サブモジュールのインスタンス化**:
   ```scala
   val arith = Module(new ArithmeticUnit)
   val logic = Module(new LogicUnit)
   ```
2. **ポート接続の作法**: 上位モジュールの入力を各サブモジュールの入力へ並列に分配し、各サブモジュールの出力をマルチプレクサ（セレクタ）で受けて上位出力 `io.out` へ接続します。

### 確認課題ヒント (`PipelinedALUWithFlags`)
1. **レジスタ挿入**: 組合せALUの出力に `val outReg = RegNext(aluOut)` を配置して1クロック遅延させることで、クリティカルパスを分断し動作周波数を向上させます。
2. **制御信号の整合性**: データパスを1クロック遅延（パイプライン化）させた場合、対応する `valid` 信号やステータスフラグも同じタイミングで1クロック遅延させる必要があります (`RegNext(validIn)`)。

---

## 第7回: 有限状態機械 (FSM)

### 基本問題ヒント (`SequenceDetector101`)
1. **ChiselEnumの定義**:
   ```scala
   object State extends ChiselEnum {
     val sIDLE, s1, s10, s101 = Value
   }
   ```
2. **状態遷移の網羅**: 各状態において入力 `in` が `0.B` の場合と `1.B` の場合の両方の行き先を明示的に記述します。特に `s101` の状態で `1` が来た場合、直前の "1" を次の "101" の先頭とみなして `s1` へ遷移させることで、重複シーケンスを漏れなく検出できます。
3. **Moore型出力**: 出力 `detected` は現在の状態レジスタ `state === State.s101` のみで判定し、入力 `in` を直接混ぜないことでグリッチを完全に防ぎます。

### 発展問題ヒント (`TrafficLightController`)
1. **タイマカウンタとの連携**: FSM内にサイクル数を数えるタイマレジスタ `val timer = RegInit(0.U(8.W))` を設け、「一定サイクル経過したら次状態へ自動遷移」するパターンを作ります。
2. **リクエストのラッチ**: 歩行者ボタン `pedestrianButton` はいつ押されるか分からないため、押されたら `val pedRequest = RegInit(false.B)` を `true.B` に保持し、歩行者青信号が終わった段階で `false.B` にクリアします。

---

## 第8回: メモリ回路と非同期読み出し

### 基本問題ヒント (`RegisterFile8x8`)
1. **非同期読み出しの記述**:
   ```scala
   val mem = Mem(8, UInt(8.W))
   when(io.wen) { mem(io.waddr) := io.wdata }
   io.rdata1 := mem(io.raddr1) // アドレス変化と同一サイクルに出力
   ```
2. **マルチリードポート**: `mem` インスタンスに対して異なるアドレス `io.raddr1` と `io.raddr2` で非同期インデックス参照を行うだけで、独立した2つの読み出しポートが合成されます。

### 発展問題ヒント (`BypassedRegisterFile`)
1. **フォワーディング比較条件**:
   - `val bypass1 = io.wen && (io.waddr === io.raddr1)`
   - `io.rdata1 := Mux(bypass1, io.wdata, mem(io.raddr1))`
2. **ハードウェアの意図**: メモリへの書き込みデータが確定するクロックエッジを待たずに、同じクロックサイクル内で書き込みデータを読み出しポートへバイパスさせることで、パイプラインハザードやストールを削減するプロセッサ設計の基本技術です。
