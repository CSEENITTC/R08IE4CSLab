import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TeCIOtb extends AnyFlatSpec with ChiselScalatestTester {
  "ArithmeticUnit" should "pass" in {
    test(new TeCIO).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.wd.poke(10.U)
      dut.io.weA.poke(true.B)
      dut.io.weB.poke(false.B)
      dut.io.op.poke(false.B)
      dut.clock.step(1)
      dut.io.rd.expect(10.U)
      
      dut.io.wd.poke(200.U)
      dut.io.weB.poke(true.B)
      dut.io.weA.poke(false.B)
      dut.io.op.poke(true.B)
      dut.clock.step(1)
      dut.io.rd.expect(200.U)
      println(s"${dut.io.rd.peekInt()}")

      dut.io.op.poke(false.B)
      dut.clock.step(1)
      dut.io.rd.expect(10.U)

      dut.io.op.poke(true.B)
      dut.clock.step(1)
      println(s"${dut.io.rd.peekInt()}")

      dut.io.op.poke(false.B)
      dut.clock.step(1)
      println(s"${dut.io.rd.peekInt()}")

    }
  }
}
