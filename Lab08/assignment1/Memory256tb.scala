import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Memory256tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "initialize 256 bytes from mem_init.hex and support synchronous write and asynchronous read" in {
    test(new Memory256("mem_init.hex")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 1. 初期値ファイル (mem_init.hex: 番地 i に i が格納) の非同期読み出し確認
      dut.io.wen.poke(false.B)
      for (addr <- Seq(0, 1, 16, 127, 255)) {
        dut.io.addr.poke(addr.U)
        val rdata = dut.io.rdata.peekInt()
        println(f"Init read: addr=0x$addr%02X => rdata=0x$rdata%02X (expected: 0x$addr%02X)")
        dut.io.rdata.expect(addr.U)
      }

      // 2. 番地 0x10 に 0xA5、番地 0xFF に 0x3C を同期書き込み
      dut.io.wen.poke(true.B)
      dut.io.addr.poke("h10".U)
      dut.io.wdata.poke("hA5".U)
      dut.clock.step(1)

      dut.io.addr.poke("hFF".U)
      dut.io.wdata.poke("h3C".U)
      dut.clock.step(1)

      // 3. 書き込み後の非同期読み出し確認
      dut.io.wen.poke(false.B)
      dut.io.addr.poke("h10".U)
      dut.io.rdata.expect("hA5".U)

      dut.io.addr.poke("hFF".U)
      dut.io.rdata.expect("h3C".U)
      println(f"Write & Async read verified: mem[0x10]=0x${dut.io.rdata.peekInt()}%02X")
    }
  }
}
