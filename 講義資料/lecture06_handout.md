---
marp: true
theme: default
paginate: true
header: "第6回 60進カウンタの構造化設計"
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

# 第6回: 60進カウンタの構造化設計 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. カスケード接続における工学的原則

1. **【最重要・厳禁】非同期リップルクロック (Ripple Clock) の追放**:
   - 初心者が陥りやすい最大の誤りは、「前段の桁上げ出力を後段のクロック入力に繋ぐ」リップルクロックです。
   - クロック遅延が段数に応じて累積し、全ビットが整定するまでに激しいグリッチが発生します。また、FPGAやASICの専用クロックツリー（低スキュー配線網）を活用できず、静的タイミング解析 (STA) が完全に破綻します。

2. **工学的標準: 完全同期イネーブル方式 (Synchronous Enable)**:
   - 全てのレジスタ・カウンタは**同一の単一システムクロック**で駆動します。
   - 前段の桁上げパルス `rollover` は、後段カウンタの**クロックイネーブル (`en`)** 端子に接続します。
   - これにより、全ビットが同じクロックエッジで完全に同期して遷移します。

```
【非同期リップル（厳禁）】
clk ---> [10進] ---cout---> clk [6進]  (クロック遅延が累積、危険)

【同期イネーブル方式（工学的標準）】
clk ----------------------> clk [10進]
clk ----------------------> clk [6進]
          en ---> [10進] ---rollover---> en [6進]  (完全同期遷移)
```

---

## 2. 60進カウンタ (`Counter60`) の構造化アーキテクチャ

60進カウンタは、**10進カウンタ (1の位: 0〜9)** と **6進カウンタ (10の位: 0〜5)** の2つのサブモジュールを同期カスケード結合して実現します。

### 同期結合論理
1. **10進部 (`secUnits`)**:
   - 外部 `io.en` が `true.B` のとき毎クロックカウントアップ。
   - 値が 9 のとき、`rollover10 := (secUnits === 9.U) && io.en` を出力。
2. **6進部 (`secTens`)**:
   - イネーブル入力として `rollover10` を接続。
   - つまり、1の位が 9 から 0 に戻るまさにその瞬間のクロックエッジでのみ、10の位がインクリメントされる。
3. **全体の桁上げ (`cout`)**:
   - 59秒（`secTens === 5.U && secUnits === 9.U`）かつ `io.en` 有効時に `true.B` を出力。

---

## 3. 24時間デジタル時計コアへの拡張 (`DigitalClockCore`)

秒・分・時を統合した3段同期カスケード回路：

```
io.enSec ---> [ 60進 秒カウンタ ] ---> secRollover
                  sec(5:0)
                     |
                     +---> en ---> [ 60進 分カウンタ ] ---> minRollover
                                       min(5:0)
                                          |
                                          +---> en ---> [ 24進 時カウンタ ]
                                                            hour(4:0)
```

- **秒の位**: 00〜59
- **分の位**: 00〜59（秒の桁上げパルスで進む）
- **時の位**: 00〜23（分の桁上げパルスで進む。23:59:59 の次で 00:00:00 へ完全同期リセット）

---

## 4. 本日の演習課題仕様 (Assignment Specifications)

### 基本問題: 同期カスケード 60進カウンタ (`Counter60`)
- **サブモジュール構成**: `ModuloNCounter(10, 4)` と `ModuloNCounter(6, 3)`
- **入力**: `en` (Bool), `clear` (Bool)
- **出力**: `secUnits` (UInt(4.W)), `secTens` (UInt(3.W)), `cout` (Bool)
- **必須要件**: 共通クロックで駆動し、リップルクロックを一切使用しないこと。

### 発展問題: デジタル時計コア (`DigitalClockCore`)
- **入力**: `enSec` (Bool: 1Hzストローブパルス), `clear` (Bool)
- **出力**: `sec` (UInt(6.W)), `min` (UInt(6.W)), `hour` (UInt(5.W))
- **論理仕様**: 3段カスケード接続。23:59:59 $\to$ 00:00:00 の完全同期境界遷移を検証。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab06/` 配下の各課題ディレクトリに分かれて配置されています。

### 基本問題 (必須・目安45分 / 配点70点)
- 10進・6進サブモジュールのインスタンス化と同期イネーブル結線 (40点)。
- 00〜59 の正確な巡回と59での `cout` アサート (30点)。

### 発展問題 (推奨・目安35分 / 配点30点)
- 秒・分・時の3段同期カスケード結合 (15点)。
- 24進リセット条件と 23:59:59 境界の同期遷移検証 (15点)。

### 採点ポイント
- リップルクロック配線がなく、完全同期イネーブル接続となっているか (35点)
- 10進桁上げと外部イネーブルのAND条件が正しく6進イネーブルに渡っているか (25点)
- サブモジュール間のポートバインドの正確性 (20点)
- デジタル時計コアの境界値（59分59秒、23時59分59秒）テスト合格 (20点)
