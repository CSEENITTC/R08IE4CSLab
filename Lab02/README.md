# Lab02: Muxとデコーダの回路 (Mux & Decoders)

第2回講義に対応する演習課題です。

## 課題一覧

### 基本問題（必須 / 目安45〜50分）
1. **`Mux4to1.scala`**
   - 4入力1出力のマルチプレクサ（セレクタ `sel` の値 0〜3 に応じて `in0`〜`in3` を選択出力）
   - ヒント: `Mux` の入れ子（木構造）や `when`、`MuxCase` などを活用して実装します。
2. **`SevenSegDecoder.scala`**
   - 4ビットBCD入力を7セグメントLEDパターン（a〜g）に変換するデコーダ
   - ヒント: `switch / is` と `WireDefault` を用いて、未完全記述による意図しない透過ラッチを防止します。0〜9のパターンを点灯(1)/消灯(0)で出力し、未定義値（10〜15）は全消灯とします。

### 発展問題（推奨 / 目安30〜35分）
3. **`PriorityEncoder4.scala`**
   - 4ビット入力の最高優先度アクティブビット位置を出力する優先度付きエンコーダ
   - `in(3)` が最高優先度、`in(0)` が最低優先度です。
   - `valid` 信号は縮約OR（`orR`）等を用いて生成します。

---

## 実行・テスト方法

### 1. テストの実行 (`sbt test`)

全てのテストを実行する場合:
```bash
sbt test
```

個別にテストを実行する場合:
```bash
sbt "testOnly Mux4to1tb"
sbt "testOnly SevenSegDecodertb"
sbt "testOnly PriorityEncoder4tb"
```

### 2. Verilog HDLの生成 (`sbt run`)

回路のVerilogファイルを `generated/` フォルダに出力します:
```bash
sbt "runMain Mux4to1Generator"
sbt "runMain SevenSegDecoderGenerator"
sbt "runMain PriorityEncoder4Generator"
```

### 3. 波形確認 (Surfer)

テストを実行すると `test_run_dir/` 配下に VCD 波形ファイルが出力されます。
```bash
surfer &
```
Surfer で `.vcd` ファイルを開くことで、各信号のタイミングチャートを確認できます。
