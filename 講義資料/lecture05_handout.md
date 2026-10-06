---
marp: true
theme: default
paginate: true
header: "第5回 構造化設計"
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

# 第5回: 構造化設計 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. 構造化・階層化設計の工学的原則

1. **関心の分離 (Separation of Concerns)**:
   - 単一の巨大なモジュール（モノリシック回路）は、デバッグ困難、タイミング収束難、再利用性欠如を招く。
   - 回路を機能単位（算術演算、論理演算、制御など）の独立したサブモジュールへ分割し、疎結合に設計する。

2. **モジュールのインスタンス化とポートバインド**:
   - Chiselでは `val subMod = Module(new SubModule)` でハードウェアインスタンスを生成。
   - 親モジュール内で、サブモジュールの入力ポートに入力信号を接続 (`subMod.io.in := ...`) し、出力ポートから結果を受け取る (`io.out := subMod.io.out`)。

3. **パイプライン化の概念 (Pipelining)**:
   - 長大な組合せ回路パスの途中に同期レジスタ（パイプラインレジスタ）を挿入することで、最悪伝搬遅延 $t_{pd}$ を短縮し、回路の動作周波数（スループット）を劇的に向上させる。

---

## 2. 階層型 ALU のアーキテクチャ

```
            +-------------------+
            |   StructuredALU   |
            |                   |
io.a ----+----> [ArithmeticUnit]----+---> (res, cout) ---+
io.b --+-|                                               |
       | +----> [ LogicUnit    ]----+---> (res) ---------+--> Mux(aluSel[2]) -> io.out
       |                                                 |
io.aluSel -----------------------------------------------+
```

### 演算選択コード (`aluSel[2:0]`)
- **最上位 bit[2] = 0**: 算術演算ユニット (`ArithmeticUnit`)
  - `000`: ADD ($a + b$)
  - `001`: SUB ($a - b$)
- **最上位 bit[2] = 1**: 論理演算ユニット (`LogicUnit`)
  - `100`: AND ($a \ \& \ b$)
  - `101`: OR ($a \ | \ b$)
  - `110`: XOR ($a \ \oplus \ b$)
  - `111`: NOT ($\sim a$)

---

## 3. ステータスフラグ生成とパイプライン出力段

プロセッサのALUでは、演算結果と同時に条件分岐の判定根拠となるフラグを生成します。

| フラグ | 論理定義 | 説明 |
|:---:|:---|:---|
| **Zero (Z)** | `out === 0.U` | 演算結果がゼロ |
| **Negative (N)** | `out(7) === 1.B` | 最上位ビットが1（2の補数での負数） |
| **Carry (C)** | `arithUnit.io.cout` | 算術加算時の桁あふれ・減算時のボロー |

### パイプラインレジスタの挿入
入力の有効フラグ `validIn` と同期して、1クロック遅延させた出力を生成：
```scala
val outReg   = RegEnable(aluCore.io.out, io.validIn)
val validReg = RegNext(io.validIn, false.B)
io.out      := outReg
io.validOut := validReg
```

---

## 4. 本日の演習課題仕様 (Assignment Specifications)

### 基本問題: 階層化 ALU (`StructuredALU`)
1. **`ArithmeticUnit`**: 加算・減算（キャリーアウト出力付き）を行うサブモジュール。
2. **`LogicUnit`**: AND, OR, XOR, NOT を行うサブモジュール。
3. **`StructuredALU`**: 上記2つのサブモジュールをインスタンス化し、`aluSel(2)` で演算結果をMUX選択して出力するトップモジュール。

### 発展問題: パイプライン化 & ステータスフラグ付き ALU (`PipelinedALUWithFlags`)
- `StructuredALU` の出力段に同期レジスタを挿入。
- `zero`, `negative`, `carry` フラグを生成。
- 入力有効信号 `validIn` を1クロック遅延させた `validOut` とデータ同期。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab05/` 配下の各課題ディレクトリに分かれて配置されています。

### 基本問題 (必須・目安45分 / 配点70点)
- サブモジュール分離（算術20点、論理20点）と階層結合（親モジュール30点）。

### 発展問題 (推奨・目安35分 / 配点30点)
- パイプラインレジスタ挿入とデータ・valid同期 (15点)。
- Z/N/C ステータスフラグの正確な生成 (15点)。

### 採点ポイント
- モジュール階層化 (`Module(new SubModule)`) が適切に行われているか (25点)
- ポートの結線 (`:=`) に型・ビット幅の不一致がないか (25点)
- 算術・論理全演算の真理値一致 (25点)
- パイプライン化に伴う1クロック遅延とフラグ整合性 (25点)
