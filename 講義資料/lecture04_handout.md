---
marp: true
theme: default
paginate: true
header: "第4回 Ring/Johnsonカウンタ"
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

# 第4回: Ring/Johnsonカウンタ 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. シフトレジスタ応用カウンタの工学的原則

1. **シフトレジスタの基礎**:
   - D-FFを直列に接続し、クロック毎にビット列を隣接レジスタへ転送する回路。
   - 外部加算器（ロジック）を介さずに状態遷移できるため、極めて高速かつ省電力に動作可能。

2. **Ring カウンタ (1-hot 表現)**:
   - $N$ 個のFFを使用し、常に単一ビットのみが `1` である状態（1-hot）を循環。
   - デコード回路が不要（各ビット出力がそのまま個別イネーブル信号となる）なため、ステートマシンやCPUシーケンサの制御パルス生成に多用される。

3. **初期値設計の原則 (`RegInit`)**:
   - Ringカウンタは自身のビットをそのまま循環させるため、リセット初期値を誤って `0000_2` にすると永久に `0` のまま変化しない。必ず **`RegInit(1.U(width.W))`**（`0001_2`）等の1-hot初期値を与える。
   - Johnsonカウンタは最上位ビットを反転して帰還させるため、**`RegInit(0.U(width.W))`**（`0000_2`）から開始することで $2N$ 状態（4ビットで8状態）を巡回する。

---

## 2. カウンタ状態遷移表とビット操作

2進数のビット配列（左側が最上位ビット MSB `bit 3`、右側が最下位ビット LSB `bit 0`）において、**下位ビットから上位ビットへビットを移動（左シフト）**させ、最上位ビットを最下位ビットへ帰還させます。

### 4ビット Ring カウンタ (循環シフト)
最上位ビット `state(3)` を最下位ビット `state(0)` に帰還し、下位ビットから上位ビットへシフト（`Cat(state(2, 0), state(3))`）。
$$\text{State: } 0001_2 \to 0010_2 \to 0100_2 \to 1000_2 \to 0001_2 \quad (N=4 \text{ 状態})$$

### 4ビット Johnson カウンタ (反転フィードバック)
最上位ビットの反転 `~state(3)` を最下位ビット `state(0)` に帰還し、下位ビットから上位ビットへシフト（`Cat(state(2, 0), ~state(3))`）。
$$\text{State: } 0000 \to 0001 \to 0011 \to 0111 \to 1111 \to 1110 \to 1100 \to 1000 \to 0000 \quad (2N=8 \text{ 状態})$$
- わずか $N$ 個のFFで $2N$ 通りの状態を生成可能。
- 状態遷移時に**常に1ビットしか変化しない**ため、デコード時のハザード（グリッチ）が発生しない。

---

## 3. グレイコード (Gray Code) カウンタの原理

バイナリカウンタは `0111` (7) $\to$ `1000` (8) のように全ビットが同時に反転する瞬間があり、物理回路では過渡的に意図しない中間値（グリッチ）が出力されます。

グレイコードは、**隣接する任意の2状態間のハミング距離（変化ビット数）が常に厳密に 1** である符号体系です。

### バイナリからグレイコードへの変換論理
バイナリ値 $B$ からグレイコード $G$ への変換は、1ビット右シフトした値との排他的論理和（XOR）で即座に合成できます。
$$G = B \oplus (B \gg 1)$$

```scala
val binReg  = RegInit(0.U(4.W))
when(io.en) { binReg := binReg + 1.U }
io.bin  := binReg
io.gray := binReg ^ (binReg >> 1)
```

---

## 4. 本日の演習課題仕様 (Assignment Specifications)

### 基本問題1: 4ビット Ring カウンタ (`RingCounter`)
- 入力: `en` (Bool) | 出力: `out` (UInt(4.W))
- リセット時: `1.U(4.W)` (`0001_2`)
- 動作仕様: `en` 有効時に下位ビットから上位ビットへシフトし、最上位ビット `state(3)` を最下位ビット `state(0)` へ循環帰還 (`Cat(state(2, 0), state(3))`)。

### 基本問題2: 4ビット Johnson カウンタ (`JohnsonCounter`)
- 入力: `en` (Bool) | 出力: `out` (UInt(4.W))
- リセット時: `0.U(4.W)`
- 動作仕様: 最上位ビットの反転 `~state(3)` を最下位ビットに結合して下位から上位へシフト (`Cat(state(2, 0), ~state(3))`)。

### 発展問題: 4ビット・グレイコード・カウンタ (`GrayCodeCounter`)
- 入力: `en` (Bool), `clear` (Bool) | 出力: `gray` (UInt(4.W)), `bin` (UInt(4.W))
- 内部バイナリカウンタと高速XOR変換回路を統合。全遷移でハミング距離1を保証。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab04/` 配下の各課題ディレクトリに分かれて配置されています。

### 基本問題 (必須・目安45分 / 配点70点)
1. **`RingCounter`** (35点): 1-hot初期値設定と4状態の循環シフト。
2. **`JohnsonCounter`** (35点): 8状態の反転帰還シフト。

### 発展問題 (推奨・目安35分 / 配点30点)
3. **`GrayCodeCounter`** (30点): バイナリカウントとグレイコード変換、ハミング距離1検証。

### 採点ポイント
- リセット値が工学的に適切か（Ring: `1.U`, Johnson: `0.U`）(20点)
- Ringカウンタの1-hot循環シフト接続が正しいか (25点)
- Johnsonカウンタの反転フィードバック接続が正しいか (25点)
- グレイコードの隣接ビット差分（ハミング距離1）が成立しているか (30点)
