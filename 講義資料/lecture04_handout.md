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

3. **【工学的核心】自己復帰回路 (Self-Correction Logic)**:
   - 4ビットRingカウンタの全16状態のうち、正常な1-hot状態は4つ（`0001`, `0010`, `0100`, `1000`）のみ。残りの12状態は異常（不正）状態。
   - 宇宙線や電源ノイズによってビット反転が生じ不正状態に陥ると、永久に正常循環に戻れなくなる（ラッチアップ）。
   - **自己復帰設計**: 不正状態を検知した瞬間、次のクロックで強制的に初期1-hotパターンへ復帰させる論理回路を必ず付加する。

---

## 2. カウンタ状態遷移表とビット操作

### 4ビット Ring カウンタ (循環シフト)
$$\text{State: } 0001_2 \to 0010_2 \to 0100_2 \to 1000_2 \to 0001_2 \quad (N=4 \text{ 状態})$$
- 異常状態の検出: ビット内の1の個数を数える `PopCount(out) =/= 1.U`

### 4ビット Johnson カウンタ (反転フィードバック)
最上位ビットの反転 $\sim out(3)$ を最下位ビットに帰還し、左シフト。
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

## 4. 実機FPGA (TeCボード) 配線仕様（課題1 & 課題2）

前回設計したカウンタ回路を実機TeCボードに書き込んで動作確認を行います。
**本回路の出力は負論理（0で点灯）のままで接続可能です。**

### 課題1: 1Hz生成・LED点滅回路 (`OneHzGenerator`) 配線
- **ピン接続仕様**:
  - `clock` $\to$ `CLK_IN` (`P55`): 9MHz オンボードシステムクロック
  - 入力 `en` $\to$ `RUN_SW` (`P16` | PULLUP): カウントイネーブル
  - 入力 `clear` $\to$ `RESET_SW` (`P120` | PULLUP): 同期クリア
  - 出力 `ledBlink` $\to$ `RUN_LED` (`P43`): 0.5秒点灯/0.5秒消灯の1Hz点滅
  - 出力 `oneHzPulse` $\to$ `C_LED` (`P46`): 1秒ストローブパルス（1秒に1回パルス点灯）
- **動作確認手順**:
  1. `RUN_SW` をオンにするとカウント動作を開始し、`RUN_LED` が0.5秒間隔で点滅します。
  2. 1秒周期満了ごとに `C_LED` が1クロック幅だけ一瞬点灯します。
  3. `RESET_SW` を押下すると、カウンタが即座に0クリアされ、LEDが消灯初期化されます。

### 課題2: 1秒カウントアップ・モジュロNカウンタ (`ModuloNCounter`) 配線
- **ピン接続仕様**:
  - `clock` $\to$ `CLK_IN` (`P55`): 9MHz オンボードシステムクロック
  - 入力 `en` $\to$ `RUN_SW` (`P16` | PULLUP): カウントイネーブル
  - 入力 `clear` $\to$ `RESET_SW` (`P120` | PULLUP): 同期クリア
  - 出力 `count[3:0]` $\to$ `DATA_LED[3:0]` (`P62, P61, P59, P58`): 4ビットバイナリ値表示
  - 出力 `runLED` $\to$ `RUN_LED` (`P43`): 動作状態インジケータ（1Hz点滅）
- **動作確認手順**:
  1. `RUN_SW` をオンにすると `RUN_LED` が点滅を開始し、`DATA_LED[3:0]` が1秒ごとに $0 \to 1 \to \dots \to 9 \to 0$ とカウントアップします。
  2. カウント値出力は内部で負論理反転（`~modReg`）されているため、TeCボードのLED（Lowで点灯）で正しい2進数値として点灯表示されます。
  3. `RESET_SW` を押すとカウント値が0クリアされ、全桁消灯（値0）に戻ります。

---

## 5. 本日の演習課題仕様 (Assignment Specifications)

### 基本問題1: 自己復帰型 4ビット Ring カウンタ (`SelfCorrectingRingCounter`)
- 入力: `en` (Bool) | 出力: `out` (UInt(4.W))
- リセット時: `1.U(4.W)` (`0001_2`)
- 動作仕様: `en` 有効時に左循環シフト。もし値が1-hotパターン以外なら、次クロックで自動的に `1.U` に復帰。

### 基本問題2: 4ビット Johnson カウンタ (`JohnsonCounter`)
- 入力: `en` (Bool) | 出力: `out` (UInt(4.W))
- リセット時: `0.U(4.W)`
- 動作仕様: 最上位ビットの反転 `~out(3)` を LSB に結合して左シフト (`Cat(out(2, 0), ~out(3))`)。

### 発展問題: 4ビット・グレイコード・カウンタ (`GrayCodeCounter`)
- 入力: `en` (Bool), `clear` (Bool) | 出力: `gray` (UInt(4.W)), `bin` (UInt(4.W))
- 内部バイナリカウンタと高速XOR変換回路を統合。全遷移でハミング距離1を保証。

---

## 6. 本日の演習課題ガイドライン (90分)

演習コードは `Lab03/`（実機確認）および `Lab04/`（設計課題）に分かれて配置されています。

### 実機動作確認 (目安30分 / 配点40点)
1. **課題1: `OneHzGenerator` のTeCボード動作確認** (20点): `RUN_SW`, `RESET_SW`, `RUN_LED`, `C_LED` の接続確認。
2. **課題2: `ModuloNCounter` のTeCボード動作確認** (20点): `DATA_LED` の1秒カウントアップ表示確認。

### 設計問題 (目安50分 / 配点60点)
3. **`SelfCorrectingRingCounter`** (20点): 1-hot循環および異常値からの1クロック自己復帰。
4. **`JohnsonCounter`** (20点): 8状態の反転巡回シフト。
5. **発展問題: `GrayCodeCounter`** (20点): バイナリカウントとグレイコード変換、ハミング距離1検証。

### バッファ (10分)
- 合成・配線レポートの確認、質疑応答

### 採点ポイント
- 実機TeCボードへのUCF配線が正しく定義され、負論理LEDで正常に点灯動作しているか (40点)
- Ringカウンタの自己復帰回路が異常値注入時に確実に機能するか (20点)
- Johnsonカウンタの反転フィードバック接続が正しいか (20点)
- グレイコードの隣接ビット差分（ハミング距離1）が成立しているか (20点)
