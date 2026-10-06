---
marp: true
theme: default
paginate: true
header: "第8回 メモリ回路と非同期読み出し"
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

# 第8回: メモリと非同期読み出し 受講者配布資料
**授業名**: Computer System Laboratory | **資料作成者**: Hideaki YANAGISAWA | **講義**: 60分 / **演習**: 90分

---

## 1. オンチップメモリの工学的原則

1. **メモリ読み出し動作の分類**:
   - ハードウェアにおけるメモリの読み出し動作には、**非同期読み出し**と**同期読み出し**の2種類が存在し、要求される速度と容量によって厳密に使い分ける必要があります。

| 特性 | `Mem` (非同期読み出し) | `SyncReadMem` (同期読み出し) |
|:---|:---|:---|
| **読み出し遅延** | **0サイクル (同一サイクル内即時出力)** | **1サイクル (次クロックエッジで出力)** |
| **回路構造** | 組合せ論理回路 (マルチプレクサ) | クロック同期レジスタ付きSRAMマクロ |
| **ハードウェア実装** | レジスタ配列 / 分散RAM (Distributed RAM) | ブロックSRAMマクロ (Block RAM) |
| **主な用途** | **CPUレジスタファイル**、即時参照LUT | キャッシュメモリ、大容量バッファ |
| **スケーラビリティ** | 容量増大に伴い遅延・面積が急増 | 大容量でも高クロック動作が可能 |

2. **2R1W レジスタファイル (Register File)**:
   - RISCプロセッサ等で必須となる「2つのオペランドを同時に読み出し (2 Read)、1つの演算結果を書き込む (1 Write)」構成の小規模・超高速メモリ。
   - 命令実行ステージを滞らせないため、読み出しポートには非同期読み出しが不可欠。

---

## 2. RAW (Read-After-Write) ハザードとバイパス回路

同一クロックサイクル内で、同一アドレスに対して「書き込み」と「読み出し」が同時に発生した場合：
- 単純なメモリ配列では、書き込みデータが安定してセルに定着する前に読み出されるため、**未定義データや1クロック前の古いデータが読まれるリスク（RAW競合）**が発生します。

### RAW バイパス（フォワーディング）回路
書き込みアドレスと読み出しアドレスの一致を検出し、メモリセルをバイパスして入力 `wdata` を直接読み出しデータとして出力するマルチプレクサ回路を設けます。

```scala
val mem = Mem(8, UInt(8.W))

// 同期書き込み
when(io.wen) {
  mem(io.waddr) := io.wdata
}

// RAWバイパス付き非同期読み出しポート
val rawConflict1 = io.wen && (io.waddr === io.raddr1)
io.rdata1 := Mux(rawConflict1, io.wdata, mem(io.raddr1))
```

---

## 3. レジスタファイルの入出力アーキテクチャ

```
            +---------------------------------+
            |       RegisterFile8x8 (2R1W)    |
            |                                 |
io.wen -----+---> [Write Logic]               |
io.waddr ---+          |                      |
io.wdata ---+          v                      |
                 [ 8x8 Memory Array ]         |
                       |         |            |
io.raddr1 ------------+         |            |
io.raddr2 ----------------------+            |
                       v         v            |
                  io.rdata1   io.rdata2       |
            +---------------------------------+
```

- 同一サイクルで `rdata1 = mem(raddr1)` と `rdata2 = mem(raddr2)` を独立に取得。
- 書き込みは `wen === true.B` のときのみ、次のクロックエッジで `wdata` が `waddr` に格納される。

---

## 4. 本日の演習課題仕様 (Assignment Specifications)

### 基本問題: 2R1W 非同期読み出しレジスタファイル (`RegisterFile8x8`)
- **構成**: 8ワード $\times$ 8ビット (`Mem(8, UInt(8.W))`)
- **入力**: `raddr1` (UInt(3.W)), `raddr2` (UInt(3.W)), `wen` (Bool), `waddr` (UInt(3.W)), `wdata` (UInt(8.W))
- **出力**: `rdata1` (UInt(8.W)), `rdata2` (UInt(8.W))
- **論理仕様**:
  - 同期書き込み: `when(wen) { mem(waddr) := wdata }`
  - 非同期読み出し: `io.rdata1 := mem(io.raddr1)`, `io.rdata2 := mem(io.raddr2)` (無遅延読み出し)

### 発展問題: RAWバイパス回路付き レジスタファイル (`BypassedRegisterFile`)
- **入力・出力**: `RegisterFile8x8` と同一
- **論理仕様**:
  - `wen` が有効で、かつ `waddr === raddr1` の場合、`rdata1` にはメモリの値ではなく `wdata` を即座に出力。
  - `rdata2` に対しても同様のバイパス判定論理を実装。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab08/` 配下の各課題ディレクトリに分かれて配置されています。

### 基本問題 (必須・目安45分 / 配点70点)
- `Mem` を用いた 2R1W レジスタファイルの実装 (40点)。
- クロック同期書き込みと同一サイクルの非同期読み出し動作の検証 (30点)。

### 発展問題 (推奨・目安35分 / 配点30点)
- 同一アドレス同時書き込み・読み出し時のRAW競合検出論理 (15点)。
- バイパスマルチプレクサによる即時データフォワーディングの検証 (15点)。

### 採点ポイント
- `Mem` による非同期読み出しが正しく実現されているか (25点)
- 書き込みが `wen` 有効時かつクロック同期で行われているか (25点)
- 2つの読み出しポートが互いに干渉せず独立動作するか (20点)
- RAWバイパス回路が同一サイクル競合時に新データを正しくフォワードするか (30点)
