---
marp: true
theme: default
paginate: true
header: "第7回 有限状態機械（FSM）"
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

# 第7回: 有限状態機械 (FSM) 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. 有限状態機械の工学的原則

1. **有限状態機械 (FSM: Finite State Machine)**:
   - システムの過去の履歴を有限個の「状態（State）」としてレジスタに記憶し、入力と現在の状態に基づいて「次状態」および「出力」を決定する逐次制御回路。

2. **Moore型 vs Mealy型 の本質的差異**:
   - **Mealy型**: 出力 $Y = f(\text{State}, \text{Input})$
     - 出力が現在の入力に直接依存するため、状態遷移を待たずに即時応答できるが、入力信号に含まれる**過渡的なノイズ（グリッチ）が出力へ直通（フィードスルー）**してしまう重大な欠点がある。
   - **Moore型（本講義推奨）**: 出力 $Y = f(\text{State})$
     - 出力は**状態レジスタの値のみから生成**される。
     - 入力側の組合せ論理と出力が完全にレジスタで遮断されるため、**グリッチフリーで極めて高い耐ノイズ性**を実現できる。

```
【Moore型 FSM (推奨: グリッチフリー)】
Input ---> [次状態論理] ---> [状態レジスタ] ---> [出力論理] ---> Output
                 ^                    |
                 +--------------------+
```

---

## 2. Chisel における型安全な状態モデリング (`ChiselEnum`)

状態を整数（0, 1, 2...）で直接扱うと、状態の重複や未定義値の代入などバグの原因になります。  
Chiselでは `ChiselEnum` を用いて型安全な列挙型状態を定義します。

```scala
object State extends ChiselEnum {
  val sIDLE, s1, s10, s101 = Value
}
import State._

val stateReg = RegInit(sIDLE) // 初期状態 sIDLE で同期リセット
val nextState = WireDefault(stateReg) // ラッチ防止のデフォルト値

switch(stateReg) {
  is(sIDLE) { when(io.in) { nextState := s1 } }
  is(s1)    { nextState := Mux(io.in, s1, s10) }
  is(s10)   { nextState := Mux(io.in, s101, sIDLE) }
  is(s101)  { nextState := Mux(io.in, s1, s10) } // 重複系列を考慮
}
stateReg := nextState

// Moore型出力生成（状態レジスタのみから生成）
io.detected := (stateReg === s101)
```

---

## 3. 重複検出 (Overlapping Sequence) の設計

入力ストリーム `1 -> 0 -> 1 -> 0 -> 1` が入力された場合：
- 3拍目の "101" で 1回目の検出パルスを出力。
- 5拍目の "101" は直前の "1" を先頭ビットとして再利用（共有）するため、2回目の検出パルスを出力。
- したがって、`s101` の状態で `io.in === false.B` が来たら、`sIDLE` ではなく `s10` へ遷移させる設計が不可欠です。

---

## 4. 本日の演習課題仕様 (Assignment Specifications)

### 基本問題: Moore型 "101" パターン検出器 (`SequenceDetector101`)
- **状態定義**: `ChiselEnum` による 4状態 (`sIDLE`, `s1`, `s10`, `s101`)
- **入力**: `in` (Bool) | **出力**: `detected` (Bool)
- **必須要件**:
  - Moore型設計（出力は `stateReg === s101` のみから生成）。
  - 重複系列（"10101" $\to$ 2回アサート）の正確な検出。

### 発展問題: 歩行者割り込み付き 交通信号機コントローラ (`TrafficLightController`)
- **入力**: `pedestrianButton` (Bool)
- **出力**: `mainLight` (UInt(2.W): 0=Green, 1=Yellow, 2=Red), `pedLight` (Bool: 0=Red, 1=Green)
- **論理仕様**:
  - 歩行者ボタン入力をラッチするリクエストフラグ。
  - タイマカウンタとの連動により、車道青 $\to$ 車道黄 $\to$ 車道赤/歩行者青 $\to$ 歩行者赤/車道青 へと安全な遷移時間を保証するFSM。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab07/` 配下の各課題ディレクトリに分かれて配置されています。

### 基本問題 (必須・目安45分 / 配点70点)
- `ChiselEnum` 状態定義と Moore型 FSM 状態遷移回路の実装 (40点)。
- 単一検出および "10101" 重複系列の正確なパルス出力 (30点)。

### 発展問題 (推奨・目安35分 / 配点30点)
- ボタン押下のラッチ回路と内部タイマとの連動 (15点)。
- 車道と歩行者の安全な状態遷移制御シーケンスの実装 (15点)。

### 採点ポイント
- `ChiselEnum` による型安全な記述がなされているか (20点)
- Moore型出力定義が徹底され、入力依存のグリッチが存在しないか (25点)
- 重複系列パターンに対する遷移論理の正確性 (25点)
- 交通信号コントローラのデッドロックのない安全設計 (30点)
