---
marp: true
theme: default
paginate: true
header: "第0回 ハードウェアシミュレーションと検証"
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

# 第0回: ハードウェアシミュレーションと検証 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. ハードウェア検証 (Verification) の工学的原則

1. **実機製造前のバグゼロ保証**:
   - 集積回路（ASIC）は製造後の修正が不可能であり、FPGAでも内部信号の物理プロービングは極めて困難です。そのため、全入力パターンやコーナーケースを論理シミュレーションで網羅的に検証することが不可欠です。
2. **DUTとテストベンチの分離**:
   - 回路本体（DUT: Device Under Test）にはテスト用の余分な論理を含めず、外部テスター（Testbench）からピンを介して刺激を与え、出力を自動検査します。

```
              ┌────────────────────────────────────────────────────────┐
              │ Testbench (chiseltest)                                 │
              │                                                        │
              │   [ Stimulus: poke ] ───> [ DUT: MyModule ]            │
              │                                  │                     │
              │   [ Clock: step(1) ] ────────────┤                     │
              │                                  ▼                     │
              │                        [ Check: expect / peek ]        │
              └────────────────────────────────────────────────────────┘
```

---

## 2. `chiseltest` API リファレンス

テストコード（`*tb.scala`）内で使用する主要メソッド一覧：

| API / メソッド | 引数 / 形式 | 機能説明 | 記述例 |
| :--- | :--- | :--- | :--- |
| **`test(new M)`** | `dut => ...` | テスト対象モジュールをシミュレータ上に実体化 | `test(new And) { dut => ... }` |
| **`withAnnotations`** | `Seq(WriteVcdAnnotation)` | 波形ファイル（VCD）の出力指示を付与 | `test(new And).withAnnotations(...)` |
| **`poke`** | `Data` (型一致) | 入力ポートに信号値をセット（印加） | `dut.io.a.poke(1.U)` / `dut.io.en.poke(true.B)` |
| **`step`** | `Int` (サイクル数) | 仮想クロックを $n$ 周期進める | `step(1)` / `dut.clock.step(10)` |
| **`expect`** | `Data` (期待値) | 出力ポートの値を検証（不一致時エラー中断） | `dut.io.out.expect(1.U)` |
| **`peekInt`** | なし | 信号の現在値を整数（`BigInt`）として取得 | `val v = dut.io.out.peekInt()` |

---

## 3. ターミナルでの開発・検証コマンド一覧

| 操作 | 実行コマンド | 備考 |
| :--- | :--- | :--- |
| **全テスト実行** | `sbt test` | プロジェクト内のすべてのテストベンチを一括実行 |
| **特定テスト単体実行** | `sbt "testOnly Andtb"` | 指定したテストクラスのみを高速実行（推奨） |
| **Verilogコード生成** | `sbt run` | 実機合成用の `.sv` / `.v` コードを `generated/` に出力 |
| **波形ファイル探索** | `find . -name "*.vcd"` | シミュレーションで生成された VCD ファイルを検索 |
| **波形ビュワー起動** | `surfer &` | 高速波形ビュワー Surfer をバックグラウンド起動 |

---

## 4. 波形ビュワー Surfer によるタイミング検証手順

1. **VCDファイルの読み込み**:
   - `sbt test` を実行後、`surfer test_run_dir/.../And.vcd &` を実行するか、起動した Surfer ウィンドウへ `.vcd` ファイルをドラッグ＆ドロップ。
2. **信号の観測**:
   - 画面左側のモジュール階層ツリーから、観測したい信号（`clock`, `io_a`, `io_b`, `io_out` 等）を選択し、タイムラインエリアへドラッグ＆ドロップ。
3. **時間軸操作**:
   - スクロールで拡大・縮小（ズームイン/アウト）、クリックでタイムカーソルを配置して各クロックサイクルでの値を直感的に確認。

---

## 5. 本日の演習ガイドライン (Lab00: 90分)

演習コードは `Lab00/` ディレクトリに配置されています。

### 必須課題
1. ターミナルで `Lab00` へ移動し、`sbt test` を実行して `All tests passed.` となることを確認。
2. 生成された `And.vcd` を `surfer` で開き、4つの入力パターンに対する出力波形を確認。

### 発展課題（推奨）
- `And.scala` の論理を故意に反転・誤記（例: `io.a | io.b`）させ、`sbt test` でエラーメッセージ（`did not equal expected=...`）の出力内容と該当行の特定を体験する。確認後は元の正しい論理に戻すこと。
