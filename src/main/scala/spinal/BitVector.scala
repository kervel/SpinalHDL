/*
 * SpinalHDL
 * Copyright (c) Dolu, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package spinal


abstract class BitVectorFactory[T <: BitVector] {
  def apply(): T
  def apply(value: BigInt): T = BitsLiteral(value, -1, this())
  def apply(width: BitCount): T = this().setWidth(width.value)
  def apply(value: BigInt, width: BitCount): T = BitsLiteral(value, width.value, this().setWidth(width.value))
}

abstract class BitVector extends BaseType {
  var fixedWidth = -1

  def isFixedWidth = fixedWidth != -1

  def setWidth(width: Int): this.type = {
    fixedWidth = width
    this
  }

  override def clone: this.type = {
    val res = super.clone
    res.fixedWidth = this.fixedWidth
    res
  }

  override def normalizeInputs: Unit ={
    InputNormalize.nodeWidth(this)
  }

  def resize(width: Int): this.type

  override def calcWidth: Int = {
    if (isFixedWidth) return fixedWidth
    if (inputs(0) == null) return -1
    return inputs(0).getWidth
  }


  def toBools: Vec[Bool] = {
    val vec = new Vec(new Bool())
    val bitCount = getWidth
    globalData.nodeWidthInferredCheck +=toBoolsCheck(bitCount)
    if(bitCount == -1) SpinalError("Can't convert to bools a Bits that has unspecified width value")
    for (i <- 0 until bitCount) vec.addElement(this(i))
    vec
  }

  def toBoolsCheck(wantedWidth : Int)() = {
    if(getWidth != wantedWidth){
      SpinalError(s"$this has changed width after a toBools call. It's not allowed")
    }
  }


  //extract bit
  def apply(bitId: Int): Bool = {
    val extract = new ExtractBoolFixed(s"extract($prefix,i)", this, bitId)
    val bool = new Bool()
    bool.setInput(extract)

    bool.compositeAssign = new Assignable {
      override def assignFrom(that: AnyRef, conservative: Boolean): Unit = {
        BitVector.this.assignFrom(new BitAssignmentFixed(BitVector.this,that.asInstanceOf[Bool],bitId),true)
      }
    }

    bool
  }

  //extract bit
  def apply(bitId: UInt): Bool = {
    val extract = new ExtractBoolFloating(s"extract($prefix,u)",this, bitId)
    val bool = new Bool()
    bool.setInput(extract)

    bool.compositeAssign = new Assignable {
      override def assignFrom(that: AnyRef, conservative: Boolean): Unit = {
        BitVector.this.assignFrom(new BitAssignmentFloating(BitVector.this,that.asInstanceOf[Bool],bitId),true)
      }
    }

    bool
  }

  //extract bits     that(8,2)
  def apply(hi: Int,lo: Int): this.type = {
    val ret = addTypeNodeFrom(new ExtractBitsVectorFixed(s"extract($prefix,i,i)",this, hi,lo))

    ret.compositeAssign = new Assignable {
      override def assignFrom(that: AnyRef, conservative: Boolean): Unit = {
        BitVector.this.assignFrom(new RangedAssignmentFixed(BitVector.this,that.asInstanceOf[Bits],hi,lo),true)
      }
    }

    ret
  }

  //extract bits     that(5,7 bit)
  def apply(offset: Int,bitCount: BitCount): this.type = this.apply(bitCount.value+offset-1, offset)



  override def addTypeNodeFrom(node: Node): this.type = {
    val typeNode = super.addTypeNodeFrom(node)
    typeNode.fixedWidth = -1
    typeNode
  }


  def prefix : String

}