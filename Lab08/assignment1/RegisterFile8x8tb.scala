import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class RegisterFile8x8tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "write synchronously and read asynchronously through 2 read ports" in {
    test(new RegisterFile8x8).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // アドレス 2 に 0xAA を書き込み
      dut.io.wen.poke(true.B)
      dut.io.waddr.poke(2.U)
      dut.io.wdata.poke("hAA".U)
      dut.clock.step(1)

      // アドレス 5 に 0x55 を書き込み
      dut.io.waddr.poke(5.U)
      dut.io.wdata.poke("h55".U)
      dut.clock.step(1)

      // 書き込み無効化して非同期同時読み出し
      dut.io.wen.poke(false.B)
      dut.io.raddr1.poke(2.U)
      dut.io.raddr2.poke(5.U)
      // クロックを進めずに即座に読み出せることを確認 (非同期読み出し)
      dut.io.rdata1.expect("hAA".U)
      dut.io.rdata2.expect("h55".U)
      println(s"Async read port1=${dut.io.rdata1.peekInt().toString(16)}, port2=${dut.io.rdata2.peekInt().toString(16)}")
    }
  }
}
