---
marp: true
theme: default
paginate: true
header: "第1回 組合せ回路の基礎"
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

# 第1回: 組合せ回路の基礎 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. 組合せ論理回路の工学的原則
1. **有向非巡回グラフ (DAG: Directed Acyclic Graph)**:
   - 組合せ回路網は、論理ゲートをノード、配線をエッジとするDAGで構成される。
   - レジスタ（フリップフロップ）を介さないフィードバック経路（閉ループ配線）は**発振・ラッチ動作の原因となるため厳禁**。
2. **無記憶性 & 非同期性**:
   - 出力は「現在の入力の組み合わせ」のみで一意に決定され、過去の状態を記憶しない。クロック信号には同期せず伝搬遅延 $t_{pd}$ を経て出力が確定する。

---

## 2. Chisel 型システム & ビット幅リファレンス

| 型 | ビット幅 | 表現データ | 記述例 |
| :--- | :---: | :--- | :--- |
| `Bool()` | 1 | 真偽フラグ、ストローブ | `val en = Wire(Bool())` |
| `UInt(w.W)` | $w$ | $w$ ビット符号なし整数 ($0 \sim 2^w-1$) | `val data = Wire(UInt(8.W))` |
| `SInt(w.W)` | $w$ | $w$ ビット2の補数符号付き整数 | `val offset = Wire(SInt(16.W))` |

### 定数リテラル & 信号配線規則
- **真偽値定数**: `true.B`, `false.B`
- **整数定数**: `0.U(8.W)`, `10.U`, `"hFF".U(8.W)` (16進), `"b1010".U(4.W)` (2進), `-5.S(8.W)`
- **`Wire` 宣言と接続 `:=`**:
  - `val w = Wire(UInt(8.W))` でハードウェアノードを実体化。
  - `w := inA + inB` で物理的な信号線を配線（代入 `=` と混同しないこと）。

---

## 3. 演算子 & バスマニピュレーション

### 論理演算子・縮約演算
- ビットワイズ演算: `&` (AND), `|` (OR), `^` (XOR), `~` (NOT)
- 縮約演算（リダクション）:
  - `bus.andR`: 全ビットAND（全1検出）
  - `bus.orR`: 全ビットOR（非ゼロ判定、有効フラグ生成）
  - `bus.xorR`: 全ビットXOR（パリティビット生成）

### ビット抽出 & ビット結合 (`Cat`, `##`)
```scala
val msb = bus(7)              // 7ビット目を抽出 (Bool)
val nibble = bus(3, 0)        // bit[3:0] の4ビットを抽出 (UInt(4.W))
val word1 = Cat(high, low)    // Cat関数: highを上位、lowを下位として結合
val word2 = high ## low       // ##演算子: 同様に結合（import不要）
```

### 【重要】加算におけるビット幅と拡張加算 (`+&`)
- 標準加算 `a + b` はオペランドの最大幅に切り詰められ、最上位キャリーは失われます。
- キャリーを保持するには **`+&`（拡張加算）** を使用します：
  ```scala
  val sumExt = a +& b          // 結果は (w + 1) ビット幅の UInt になる
  val cout   = sumExt(4)       // 最上位ビットがキャリーアウト
  val sum    = sumExt(3, 0)    // 下位4ビットが和
  ```

---

<!-- _class: lead -->

## 4. 半加算器・全加算器・多ビット加算の仕様

### 1ビット半加算器 (Half Adder: HA)
- **和**: $sum = a \oplus b$
- **桁上げ**: $cout = a \cdot b$

### 1ビット全加算器 (Full Adder: FA)
- **和**: $sum = a \oplus b \oplus cin$
- **桁上げ**: $cout = (a \cdot b) + (cin \cdot (a \oplus b))$

### 多ビット加算とパリティ検出の原理
- 拡張加算 `+&` によりビット幅を自動拡張して桁あふれを保持。
- 縮約XOR演算子 `xorR` によるパリティ計算（全ビットの排他的論理和：1の個数が奇数なら1）。

---

## 5. 本日の演習課題ガイドライン (90分)

### 基本問題 (必須・目安45分 / 配点70点)
1. **`HalfAdder`** (目安20分 / 30点): 1bit入力 `a`, `b` から論理式に基づき `sum`, `cout` を生成。
2. **`FullAdder`** (目安25分 / 40点): 1bit入力 `a`, `b`, `cin` から論理式に基づき `sum`, `cout` を生成。

### 発展問題 (推奨・目安35分 / 配点30点)
- **`AdderWithParity4`** (目安35分 / 30点):
  - 4bit入力 `a`, `b` と 1bitキャリー入力 `cin` を加算。
  - 4bitの和 `sum` と 1bitキャリー出力 `cout` を生成。
  - 和 `sum` の全ビットの排他的論理和による奇数パリティ検査ビット `parity` を出力（`xorR` を活用）。

### 採点ポイント
- 構文エラーがなく、全テストケースをパスするか (35点)
- 型とビット幅が `.W` で厳格に明示されているか (15点)
- 透過ラッチ・未代入がなく、健全な組合せ回路として合成されているか (20点)
- 発展仕様（4bit加算・キャリー出力・パリティ検出）が完全に動作するか (30点)
