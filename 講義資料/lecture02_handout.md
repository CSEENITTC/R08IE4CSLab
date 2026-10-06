---
marp: true
theme: default
paginate: true
header: "第2回 Muxとデコーダの回路"
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

# 第2回: Muxとデコーダの回路 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. MUXとデコーダの工学的原則

1. **マルチプレクサ (MUX: Multiplexer)**:
   - 複数の入力信号から、セレクタ（選択制御信号）の値に応じて1つを選択して出力する調停回路網。
   - 2-to-1 MUXを基本構成要素とし、木構造（ツリー）に展開することで大規模なN-to-1 MUXを合成する。

2. **デコーダ (Decoder)**:
   - $n$ ビットの符号化されたコード（BCDなど）を解釈し、対応する特定の出力線（7セグメントLED等）を活性化する復号回路。

3. **【最重要】透過ラッチ (Transparent Latch) の危険性と完全防止**:
   - 組合せ論理回路において、条件分岐（`when` や `switch`）で**「値が代入されないケース（未完全代入）」が存在すると、合成ツールは前サイクル（過去）の値を保持するためのラッチ回路を勝手に生成**してしまう。
   - ラッチはクロック同期設計の基本原則（グリッチ耐性・静的タイミング解析 STA）を破壊し、誤動作の温床となる。
   - **撲滅の鉄則**: 分岐に入る前に、必ず `WireDefault` で全ての出力信号にデフォルト値を事前配線すること。

---

## 2. Chisel における選択・デコード構文リファレンス

| 構文 | 生成されるハードウェア | 推奨される用途 | 特徴・注意点 |
| :--- | :--- | :--- | :--- |
| `Mux(c, t, f)` | 単一の 2-to-1 MUX | 単純な2値選択 | 最も高速で簡潔。入れ子で木構造を構成可能 |
| `when / elsewhen` | 優先度付きMUX連鎖 | 明確な優先順位がある場合 | カスケード段数が深くなると伝搬遅延 $O(N)$ が増大 |
| `switch / is` | 並列直交デコーダ | BCDや命令デコード | 全ケースが等価。ツリー構造で遅延 $O(\log N)$ |
| `MuxCase(default, seq)` | 優先度付きMUX連鎖 | テーブル駆動の条件選択 | `when` 同様の優先度連鎖 |

### 透過ラッチ防止コードの定石 (`WireDefault`)
```scala
val outWire = WireDefault(0.U(7.W)) // 1. 必ず全消灯/デフォルト値で初期化
switch(io.in) {
  is(0.U) { outWire := "b1111110".U }
  is(1.U) { outWire := "b0110000".U }
  // 未定義の入力が来ても、必ず 0.U が出力され、ラッチは一切発生しない
}
io.out := outWire
```

---

## 3. 7セグメントLED点灯パターン真理値表 (アノードコモン/カソードコモン)

本演習では `1: 点灯`, `0: 消灯` とします。  
ビット配置: `seg(6)` = a, `seg(5)` = b, `seg(4)` = c, `seg(3)` = d, `seg(2)` = e, `seg(1)` = f, `seg(0)` = g

| BCD | 点灯セグメント | 2進表現 (abcdefg) | 16進 | 表示 |
| :---: | :--- | :---: | :---: | :---: |
| 0 | a, b, c, d, e, f | `"b1111110".U` | `0x7E` | **0** |
| 1 | b, c | `"b0110000".U` | `0x30` | **1** |
| 2 | a, b, d, e, g | `"b1101101".U` | `0x6D` | **2** |
| 3 | a, b, c, d, g | `"b1111001".U` | `0x79` | **3** |
| 4 | b, c, f, g | `"b0110011".U` | `0x33` | **4** |
| 5 | a, c, d, f, g | `"b1011011".U` | `0x5B` | **5** |
| 6 | a, c, d, e, f, g | `"b1011111".U` | `0x5F` | **6** |
| 7 | a, b, c | `"b1110000".U` | `0x70` | **7** |
| 8 | a, b, c, d, e, f, g | `"b1111111".U` | `0x7F` | **8** |
| 9 | a, b, c, d, f, g | `"b1111011".U` | `0x7B` | **9** |
| 10〜15 | 全消灯 (未定義) | `"b0000000".U` | `0x00` | 消灯 |

---

## 4. 優先度付きエンコーダの論理仕様

4ビット入力 `in[3:0]` のうち、`1` が立っている最高位のビット番号を出力します。

| 入力 `in[3:0]` | 出力 `valid` | 出力 `pos[1:0]` | 備考 |
| :---: | :---: | :---: | :--- |
| `1xxx` (`in(3) == 1`) | `true.B` | `3.U` | in(3) がアクティブ (最優先) |
| `01xx` (`in(2) == 1`) | `true.B` | `2.U` | in(3)=0 かつ in(2)=1 |
| `001x` (`in(1) == 1`) | `true.B` | `1.U` | in(3..2)=0 かつ in(1)=1 |
| `0001` (`in(0) == 1`) | `true.B` | `0.U` | in(3..1)=0 かつ in(0)=1 |
| `0000` (全ビット0) | `false.B` | `0.U` (任意) | 有効な入力なし (`valid := false.B`) |

- **縮約OR演算子**: `io.in.orR` で「いずれかのビットが1か」を1式で判定可能。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab02/` 配下の各課題ディレクトリ（`assignment1`, `assignment2`, `assignment3`）に分かれて配置されています。各ディレクトリに移動して `sbt test` を実行してください。

### 基本問題 (必須・目安45〜50分 / 配点70点)
1. **Assignment 1: `Mux4to1`** (`Lab02/assignment1/` / 目安20分 / 30点)
   - 4入力 `in0`〜`in3` (各8bit) と 2bitセレクタ `sel` から1出力を選択。
   - `cd assignment1 && sbt test` でテスト検証。
2. **Assignment 2: `SevenSegDecoder`** (`Lab02/assignment2/` / 目安25分 / 40点)
   - `switch / is` と `WireDefault` を用いた透過ラッチフリーな7セグメントデコーダ。
   - 0〜9のパターン点灯および 10〜15の全消灯を実装。
   - `cd assignment2 && sbt test` で全入力網羅テスト検証。

### 発展問題 (推奨・目安30〜35分 / 配点30点)
3. **Assignment 3: `PriorityEncoder4`** (`Lab02/assignment3/` / 目安30〜35分 / 30点)
   - 4bit入力の最高位アクティブビット位置 `pos` と有効フラグ `valid` を生成。
   - `cd assignment3 && sbt test` で全16パターンの優先度検証。

### 採点ポイント
- 構文エラーがなく、全テストケースをパスするか (35点)
- 透過ラッチが `WireDefault` により完全に防止されているか (25点)
- セレクタおよび優先度判定が論理仕様通り正しく動作するか (25点)
- 発展仕様（優先度エンコード & valid フラグ）が完全動作するか (15点)
