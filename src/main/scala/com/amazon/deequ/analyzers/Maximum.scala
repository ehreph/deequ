/**
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License
 * is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package com.amazon.deequ.analyzers

import java.math.BigDecimal

import com.amazon.deequ.analyzers.Preconditions.{hasColumn, isDecimalType, isNumeric}
import org.apache.spark.sql.{Column, Row}
import org.apache.spark.sql.functions.{max, min}
import org.apache.spark.sql.types.{DoubleType, StructType}
import Analyzers._
import com.amazon.deequ.metrics.FullColumn

case class MaxState(maxValue: Double, override val fullColumn: Option[Column] = None)
  extends DoubleValuedState[MaxState] with FullColumn {

  override def sum(other: MaxState): MaxState = {
    MaxState(math.max(maxValue, other.maxValue), sum(fullColumn, other.fullColumn))
  }

  override def metricValue(): Double = {
    maxValue
  }
}

case class Maximum(column: String, where: Option[String] = None)
  extends StandardScanShareableAnalyzer[MaxState]("Maximum", column)
  with FilterableAnalyzer {

  override def aggregationFunctions(): Seq[Column] = {
    max(conditionalSelection(column, where)).cast(DoubleType) :: Nil
  }

  override def fromAggregationResult(result: Row, offset: Int): Option[MaxState] = {

    ifNoNullsIn(result, offset) { _ =>
      MaxState(result.getDouble(offset))
    }
  }

  override protected def additionalPreconditions(): Seq[StructType => Unit] = {
    hasColumn(column) :: isNumeric(column) :: Nil
  }

  override def filterCondition: Option[String] = where
}

case class MaxBigDecimalState(minValue: BigDecimal)
  extends BigDecimalValuedState[MaxBigDecimalState] {

  override def sum(other: MaxBigDecimalState): MaxBigDecimalState = {
    MaxBigDecimalState(minValue.max(other.minValue))
  }

  override def metricValue(): BigDecimal = {
    minValue
  }
}

case class MaximumBigDecimal(column: String, where: Option[String] = None)
  extends BigDecimalScanShareableAnalyzer[MaxBigDecimalState]("Maximum BigDecimal", column)
    with FilterableAnalyzer {

  override def aggregationFunctions(): Seq[Column] = {
    max(conditionalSelection(column, where)) :: Nil
  }


  override def fromAggregationResult(result: Row, offset: Int): Option[MaxBigDecimalState] = {
    ifNoNullsIn(result, offset) { _ =>
      MaxBigDecimalState(result.getDecimal(offset))
    }
  }

  override protected def additionalPreconditions(): Seq[StructType => Unit] = {
    hasColumn(column) :: isDecimalType(column) :: Nil
  }

  override def filterCondition: Option[String] = where
}
