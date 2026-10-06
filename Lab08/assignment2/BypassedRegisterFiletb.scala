import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class BypassedRegisterFiletb extends AnyFlatSpec with ChiselScalatestTester {
  it should "forward write data immediately when read and write collide on same address" in {
    test(new BypassedRegisterFile).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 同一サイクルでアドレス3に0xFEを書き込みながら、ポート1からアドレス3を読み出し
      dut.io.wen.poke(true.B)
      dut.io.waddr.poke(3.U)
      dut.io.wdata.poke("hFE".U)
      dut.io.raddr1.poke(3.U)
      dut.io.raddr2.poke(0.U)

      // クロックを進める前の同一サイクル内で、即座にフォワーディングされたデータが読めるか検証
      dut.io.rdata1.expect("hFE".U)
      println(s"RAW Bypass verified: rdata1=${dut.io.rdata1.peekInt().toString(16)} (expected: fe)")
    }
  }
}
