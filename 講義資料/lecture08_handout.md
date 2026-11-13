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

## 2. 256バイトメモリの宣言と初期値のロード (`loadMemoryFromFileInline`)

Chiselで256バイト（256ワード $\times$ 8ビット）の非同期読み出しメモリを宣言し、外部の16進ファイル（`mem_init.hex`）から初期値を持たせるには、`Mem(256, UInt(8.W))` と `loadMemoryFromFileInline` を組み合わせます。

```scala
import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline

class Memory256(val initHexFile: String = "mem_init.hex") extends Module {
  val io = IO(new Bundle {
    val addr  = Input(UInt(8.W))  // 8ビット アドレス (0〜255)
    val wen   = Input(Bool())     // 書き込みイネーブル
    val wdata = Input(UInt(8.W))  // 8ビット 書き込みデータ
    val rdata = Output(UInt(8.W)) // 8ビット 非同期読み出しデータ
  })

  // 1. 256バイト (256ワード × 8ビット) の非同期読み出しメモリを宣言
  val mem = Mem(256, UInt(8.W))

  // 2. 外部16進ファイル ("mem_init.hex") から初期値をロード
  if (initHexFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, initHexFile)
  }

  // 3. クロック同期書き込み & 4. 非同期読み出し
  when(io.wen) { mem(io.addr) := io.wdata }
  io.rdata := mem(io.addr)
}
```

---

## 3. RAW (Read-After-Write) ハザードとバイパス回路

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

## 4. 本日の演習課題仕様 (`Lab08/assignment1`〜`assignment3`)

### 1. 基本問題1 (`assignment1`): 初期値付き 256バイト 非同期読み出しメモリ (`Memory256` & `MemoryIO1`)
- **モジュール構成**:
  - `Memory256` (`Memory256.scala`): `Mem(256, UInt(8.W))` で256バイトを宣言し、`loadMemoryFromFileInline(mem, initHexFile)` で `mem_init.hex`（`00`〜`FF` の256行）の初期値をロードする。
  - `MemoryIO` (`MemoryIO1.scala`, `MemoryIO1.ucf`): TeCボードのスイッチ（`DATA_SW[7:0]`, `SETA_SW` `P21`, `INCA_SW` `P22`, `DECA_SW` `P23`, `WRITE_SW` `P24`, `RESET_SW` `P120`）で8ビットアドレス `addrReg` の操作とメモリ書き込みを行い、`ADDR_LED[7:0]` (`P94..P83`) と `DATA_LED[7:0]` (`P81..P58`) に負論理反転して出力する。

### 2. 基本問題2 (`assignment2`): 2R1W 非同期読み出しレジスタファイル (`RegisterFile8x8`)
- **構成**: 8ワード $\times$ 8ビット (`Mem(8, UInt(8.W))`)
- **入力**: `raddr1` (UInt(3.W)), `raddr2` (UInt(3.W)), `wen` (Bool), `waddr` (UInt(3.W)), `wdata` (UInt(8.W))
- **出力**: `rdata1` (UInt(8.W)), `rdata2` (UInt(8.W))
- **論理仕様**:
  - 同期書き込み: `when(io.wen) { mem(io.waddr) := io.wdata }`
  - 非同期読み出し: `io.rdata1 := mem(io.raddr1)`, `io.rdata2 := mem(io.raddr2)` (無遅延読み出し)

### 3. 発展問題 (`assignment3`): RAWバイパス回路付き レジスタファイル (`BypassedRegisterFile`)
- **入力・出力**: `RegisterFile8x8` と同一
- **論理仕様**:
  - `wen` が有効で、かつ `waddr === raddr1` の場合、`rdata1` にはメモリの値ではなく `wdata` を即座に出力。
  - `rdata2` に対しても同様のバイパス判定論理を実装。

---

## 5. 本日の演習課題ガイドライン (90分)

演習コードは `Lab08/` 配下の各課題ディレクトリ（`assignment1`〜`assignment3`）に配置されています。

1. **基本問題1 (`assignment1`: `Memory256.scala` + `MemoryIO1.scala` + `mem_init.hex`)**:
   - `Memory256.scala` で `Mem(256, UInt(8.W))` と `loadMemoryFromFileInline` を実装して `sbt test` で検証したのち、`MemoryIO1.scala` を完成させて `sbt run` でSystemVerilogを生成し、`MemoryIO1.ucf` を用いてTeCボード実機で動作確認する。
2. **基本問題2 (`assignment2`: `RegisterFile8x8.scala`)**:
   - `Mem(8, UInt(8.W))` を用いた 2R1W レジスタファイルを実装し、`sbt test` で検証する。
3. **発展問題 (`assignment3`: `BypassedRegisterFile.scala`)**:
   - 同一アドレス同時書き込み・読み出し時のRAWバイパス回路を実装し、`sbt test` で検証する。
