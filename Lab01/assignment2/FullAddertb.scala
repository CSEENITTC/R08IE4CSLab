import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FullAddertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly implement an And gate" in {
    test(new FullAdder).withAnnotations(Seq(WriteVcdAnnotation)) {dut => 
      // Test case 1: 0 AND 0 = 0
      println("Start test")
      dut.io.a.poke(0.U(1.W))
      dut.io.b.poke(0.U(1.W))
      step(1)
//      dut.io.out.expect(false.B)
//      dut.io.out.expect(true.B)
      println(s"a=${dut.io.a.peekInt()},b=${dut.io.b.peekInt()},sum=>${dut.io.sum.peekInt()},cout=>${dut.io.cout.peekInt()}")
      

      // Test case 2: 0 AND 1 = 0
      dut.io.a.poke(false.B)
      dut.io.b.poke(true.B)
      step(1)
    //   dut.io.out.expect(false.B)
      println(s"a=${dut.io.a.peekInt()},b=${dut.io.b.peekInt()},sum=>${dut.io.sum.peekInt()},cout=>${dut.io.cout.peekInt()}")

      // Test case 2: 0 AND 1 = 0
      dut.io.a.poke(true.B)
      dut.io.b.poke(true.B)
      step(1)
    //   dut.io.out.expect(false.B)
      println(s"a=${dut.io.a.peekInt()},b=${dut.io.b.peekInt()},sum=>${dut.io.sum.peekInt()},cout=>${dut.io.cout.peekInt()}")

 
        check(dut,true,false,true,false,true)
 
    }
  }

    def check(
        dut: FullAdder,
        a: Boolean,
        b: Boolean,
        cin: Boolean,
        sum: Boolean,
        cout: Boolean
    ): Unit = {
    dut.io.a.poke(a)
    dut.io.b.poke(b)
    dut.io.cin.poke(cin)
    dut.clock.step()

    dut.io.sum.expect(sum)
    dut.io.cout.expect(cout)

    // dut.io.a.peek() 定義された型を表示
    // dut.io.a.peekInt() Int型で表示
    println(s"a=${dut.io.a.peek()},b=${dut.io.b.peek()},cin=${dut.io.cin.peek()}," +
      s"sum=${dut.io.sum.peek()},}" +
      s"cout=${dut.io.cout.peek()},}"
      )
    }
}